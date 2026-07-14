package relationsapi;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import utilities.OperationWebserviceInDistributionSingleton;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Validates and creates entity relations with cycle detection.
 *
 * <p>Reference entities are shared entities that prefer PUBLISHED versions
 * when creating relations, unless operating in "ingestor" mode.</p>
 *
 * <p><strong>Performance Notes:</strong></p>
 * <ul>
 *   <li>Cycle detection uses ThreadLocal to track in-flight operations per request</li>
 *   <li>Reflection calls are cached to eliminate repeated method lookups</li>
 *   <li>Entity type normalization is done once and cached where possible</li>
 * </ul>
 */
public class RelationChecker {

    private static final Logger LOG = Logger.getLogger(RelationChecker.class.getName());

    private static final ThreadLocal<Set<String>> processingEntities = ThreadLocal.withInitial(() -> new HashSet<>(8));

    /*
     * Reference entities participate in cascade operations but should link
     * to existing versions rather than being duplicated.
     */
    private static final Set<String> REFERENCE_ENTITIES = Set.of(
            EntityNames.ATTRIBUTION.name(),
            EntityNames.PERSON.name(),
            EntityNames.MAPPING.name(),
            EntityNames.CATEGORY.name(),
            EntityNames.CATEGORYSCHEME.name(),
            EntityNames.FACILITY.name(),
            EntityNames.EQUIPMENT.name(),
            EntityNames.OPERATION.name(),
            EntityNames.WEBSERVICE.name(),
            EntityNames.DATAPRODUCT.name(),
            EntityNames.CONTACTPOINT.name(),
            EntityNames.DISTRIBUTION.name(),
            EntityNames.ORGANIZATION.name(),
            EntityNames.SOFTWARESOURCECODE.name(),
            EntityNames.SOFTWAREAPPLICATION.name(),
            EntityNames.ADDRESS.name(),
            EntityNames.ELEMENT.name(),
            EntityNames.LOCATION.name(),
            EntityNames.PERIODOFTIME.name(),
            EntityNames.IDENTIFIER.name(),
            EntityNames.QUANTITATIVEVALUE.name(),
            EntityNames.DOCUMENTATION.name(),
            EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name(),
            EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(),
            EntityNames.OUTPUTMAPPING.name(),
            EntityNames.PAYLOAD.name()
    );

    /*
     * Shared reference entities always prefer PUBLISHED versions
     * unless explicitly in ingestor mode.
     */
    private static final Set<String> SHARED_REFERENCE_ENTITIES = Set.of(
            EntityNames.CATEGORY.name(),
            EntityNames.CATEGORYSCHEME.name(),
            EntityNames.ORGANIZATION.name(),
            EntityNames.PERSON.name(),
            EntityNames.CONTACTPOINT.name()
    );

    // Pre-interned status string for hot-path comparisons
    private static final String STATUS_PUBLISHED = StatusType.PUBLISHED.toString();
    private static final String INGESTOR = "ingestor";

    // Reflection caches to eliminate repeated introspection overhead
    private static final ConcurrentHashMap<Class<?>, MethodHandle> VERSION_GETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, MethodHandle> STATUS_GETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> PROPERTY_GETTERS = new ConcurrentHashMap<>();
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private static boolean isSharedReferenceEntity(String entityType) {
        if (entityType == null) {
            return false;
        }
        return SHARED_REFERENCE_ENTITIES.contains(entityType.toUpperCase(Locale.ROOT));
    }

    /**
     * Determines whether to prefer PUBLISHED versions for this entity type.
     * Returns false in ingestor mode to allow normal cascade behavior.
     */
    private static boolean shouldUsePublishedVersion(String entityType, EPOSDataModelEntity mainEntity) {
        if (!isSharedReferenceEntity(entityType)) {
            return false;
        }
        if (mainEntity != null) {
            String editorId = mainEntity.getEditorId();
            if (editorId != null && INGESTOR.equalsIgnoreCase(editorId.trim())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds a unique key for cycle detection.
     * Prioritizes UID > MetaId > InstanceId for stability across versions.
     */
    private static String getEntityKey(LinkedEntity linkedEntity) {
        String entityType = linkedEntity.getEntityType();
        String uid = linkedEntity.getUid();
        if (uid != null && !uid.isBlank()) {
            return entityType + ":" + uid;
        }
        String metaId = linkedEntity.getMetaId();
        if (metaId != null && !metaId.isBlank()) {
            return entityType + ":" + metaId;
        }
        return entityType + ":" + linkedEntity.getInstanceId();
    }

    @SuppressWarnings("rawtypes")
    public static Object checkRelation(EPOSDataModelEntity mainEntity,
                                       EPOSDataModelEntity oldMainEntity,
                                       Class mainEntityClazz,
                                       LinkedEntity linkedEntity,
                                       StatusType overrideStatus,
                                       Class clazz,
                                       Boolean enableStore) {

        String entityKey = getEntityKey(linkedEntity);
        Set<String> processing = processingEntities.get();

        // Fast cycle detection before expensive operations
        if (processing.contains(entityKey)) {
            return handleCycleCase(linkedEntity, clazz);
        }

        try {
            processing.add(entityKey);
            Object result = processRelation(mainEntity, oldMainEntity, mainEntityClazz,
                    linkedEntity, overrideStatus, clazz, enableStore);
            if (LOG.isLoggable(Level.FINE)) {
                LOG.log(Level.FINE, "[RELATION CHECK] Resolved linkedEntity {0} to: {1}", 
                        new Object[]{entityKey, result != null ? result.getClass().getSimpleName() + " (instanceId=" + utilities.ReflectionCache.getInstanceId(result) + ")" : "null"});
            }
            if (result == null) {
                LOG.log(Level.WARNING, "[RELATION CHECK WARNING] Could not resolve linkedEntity {0} for mainEntity {1} ({2})", 
                        new Object[]{entityKey, mainEntity != null ? mainEntity.getInstanceId() : "null", mainEntity != null ? mainEntity.getClass().getSimpleName() : "null"});
            }
            return result;
        } finally {
            processing.remove(entityKey);
            if (processing.isEmpty()) {
                processingEntities.remove();
            }
        }
    }

    @SuppressWarnings("rawtypes")
    private static Object handleCycleCase(LinkedEntity linkedEntity, Class clazz) {
        List<Object> results = EposDataModelDAO.getInstance()
                .getOneFromDBByLinkedEntity(linkedEntity, clazz);
        return results.isEmpty() ? null : results.get(0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Object processRelation(EPOSDataModelEntity mainEntity,
                                          EPOSDataModelEntity oldMainEntity,
                                          Class mainEntityClazz,
                                          LinkedEntity linkedEntity,
                                          StatusType overrideStatus,
                                          Class clazz,
                                          Boolean enableStore) {

        if (mainEntityClazz == null) {
            mainEntityClazz = mainEntity.getClass();
        }

        // Cache the entity type uppercase for multiple uses
        String mainEntityTypeName = mainEntityClazz.getSimpleName().toUpperCase(Locale.ROOT);
        String linkedEntityType = linkedEntity.getEntityType();
        String linkedEntityTypeUpper = linkedEntityType != null ? linkedEntityType.toUpperCase(Locale.ROOT) : null;

        LinkedEntity newLinkedEntityMainEntity = null;
        if (mainEntity != null) {
            AbstractAPI mainApi = AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityTypeName).name());
            if (mainApi != null) {
                newLinkedEntityMainEntity = mainApi.retrieveLinkedEntity(mainEntity.getInstanceId());
            }
        }

        LinkedEntity oldLinkedEntityMainEntity = null;
        if (oldMainEntity != null) {
            AbstractAPI mainApi = AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityTypeName).name());
            if (mainApi != null) {
                oldLinkedEntityMainEntity = mainApi.retrieveLinkedEntity(oldMainEntity.getInstanceId());
            }
        }

        EPOSDataModelEntity relationEntity = (EPOSDataModelEntity)
                LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);

        LinkedEntity obj = null;

        // Priority path: For shared reference entities, find PUBLISHED version first
        if (shouldUsePublishedVersion(linkedEntityType, mainEntity)) {
            Object publishedVersion = findPublishedVersionOfEntity(linkedEntity, clazz);
            if (publishedVersion != null) {
                LOG.log(Level.FINE, "Reference entity PUBLISHED version found: {0} uid={1}",
                        new Object[]{linkedEntityType, linkedEntity.getUid()});
                return publishedVersion;
            }
        }

        if (relationEntity != null) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus :
                    (mainEntity != null ? mainEntity.getStatus() : null);

            if (targetStatus != null) {
                String relationUid = relationEntity.getUid();
                List<Object> allVersions = EposDataModelDAO.getInstance()
                        .getOneFromDBByUIDNoCache(relationUid, clazz);

                // For shared reference entities, prioritize PUBLISHED
                if (shouldUsePublishedVersion(linkedEntityType, mainEntity)) {
                    for (Object v : allVersions) {
                        String statusStr = getModelVersionStatus(v);
                        if (STATUS_PUBLISHED.equals(statusStr)) {
                            return v;
                        }
                    }
                }

                // Standard behavior: find matching status version
                String targetStatusStr = targetStatus.toString();
                for (Object v : allVersions) {
                    String statusStr = getModelVersionStatus(v);
                    if (targetStatusStr.equals(statusStr)) {
                        obj = buildLinkedEntity(v, linkedEntityType);
                        break;
                    }
                }
            }
        }

        if (obj == null) {
            if (relationEntity != null && newLinkedEntityMainEntity != null) {
                boolean statusMismatch = mainEntity.getStatus() != null
                        && relationEntity.getStatus() != null
                        && !mainEntity.getStatus().equals(relationEntity.getStatus());

                boolean isReference = linkedEntityTypeUpper != null && REFERENCE_ENTITIES.contains(linkedEntityTypeUpper);
                boolean isSharedReference = isSharedReferenceEntity(linkedEntityType);

                // For shared reference entities, don't propagate status changes
                if (statusMismatch && isReference && !isSharedReference) {
                    relationEntity.setEditorId(mainEntity.getEditorId());
                    relationEntity.setStatus(mainEntity.getStatus());

                    if (Boolean.TRUE.equals(enableStore)) {
                        obj = OperationWebserviceInDistributionSingleton.getInstance()
                                .getTarget(linkedEntity);
                    }

                    if (obj == null) {
                        String apiName = EntityNames.valueOf(linkedEntityTypeUpper).name();
                        obj = AbstractAPI.retrieveAPI(apiName)
                                .create(relationEntity, overrideStatus, oldLinkedEntityMainEntity, newLinkedEntityMainEntity);

                        if (Boolean.TRUE.equals(enableStore)) {
                            OperationWebserviceInDistributionSingleton.getInstance()
                                    .createRelation(linkedEntity, obj);
                        }
                    }
                } else {
                    obj = linkedEntity;
                }

            } else {
                List<Object> results = EposDataModelDAO.getInstance()
                        .getOneFromDBByLinkedEntity(linkedEntity, clazz);

                // Try direct instanceId lookup if linked entity search fails
                if (results.isEmpty() && linkedEntity.getInstanceId() != null) {
                    results = EposDataModelDAO.getInstance()
                            .getOneFromDBByInstanceIdNoCache(linkedEntity.getInstanceId(), clazz);
                }

                if (!results.isEmpty()) {
                    // For shared reference entities, prefer PUBLISHED from results
                    if (shouldUsePublishedVersion(linkedEntityType, mainEntity)) {
                        for (Object result : results) {
                            String status = getModelVersionStatus(result);
                            if (STATUS_PUBLISHED.equals(status)) {
                                return result;
                            }
                        }
                    }
                    obj = linkedEntity;
                } else {
                    // Try UID-based lookup
                    String linkedUid = linkedEntity.getUid();
                    if (linkedUid != null) {
                        List<Object> byUid = EposDataModelDAO.getInstance()
                                .getOneFromDBByUIDNoCache(linkedUid, clazz);

                        if (!byUid.isEmpty()) {
                            StatusType preferredStatus = shouldUsePublishedVersion(linkedEntityType, mainEntity)
                                    ? StatusType.PUBLISHED
                                    : (overrideStatus != null ? overrideStatus :
                                    (mainEntity != null ? mainEntity.getStatus() : null));

                            Object existing = findBestMatchingVersion(byUid, preferredStatus,
                                    mainEntity != null ? mainEntity.getEditorId() : null);

                            if (existing != null) {
                                obj = buildLinkedEntity(existing, linkedEntityType);
                            }
                        }
                    }

                    // Last resort: create stub entity if enabled.
                    // WebService links are handled better as pending relations because
                    // the stub path does not reliably materialize the join table entry.
                    if (obj == null && Boolean.TRUE.equals(enableStore) && linkedEntityType != null
                            && !EntityNames.WEBSERVICE.name().equals(linkedEntityTypeUpper)) {
                        obj = createStubEntity(linkedEntity, mainEntity, overrideStatus);
                    }
                }
            }
        }

        if (obj == null) {
            return null;
        }

        List<Object> results = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(obj, clazz);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Creates a stub entity when no existing version is found.
     * Reference entities are created as PUBLISHED; others follow the main entity status.
     */
    private static LinkedEntity createStubEntity(LinkedEntity linkedEntity,
                                                 EPOSDataModelEntity mainEntity,
                                                 StatusType overrideStatus) {
        try {
            String entityType = linkedEntity.getEntityType();
            String apiName = EntityNames.valueOf(entityType.toUpperCase(Locale.ROOT)).name();
            AbstractAPI api = AbstractAPI.retrieveAPI(apiName);

            if (api == null) {
                return null;
            }

            Class<?> dtoClass = findDtoClassForApi(api);
            if (dtoClass == null) {
                return null;
            }

            EPOSDataModelEntity newDto = (EPOSDataModelEntity) dtoClass.getDeclaredConstructor().newInstance();
            newDto.setUid(linkedEntity.getUid());
            newDto.setInstanceId(UUID.randomUUID().toString());
            newDto.setMetaId(UUID.randomUUID().toString());
            if (mainEntity != null && mainEntity.getEditorId() != null) {
                newDto.setEditorId(mainEntity.getEditorId());
            }

            // Reference entities always create as PUBLISHED
            StatusType createStatus;
            if (shouldUsePublishedVersion(entityType, mainEntity)) {
                createStatus = StatusType.PUBLISHED;
                LOG.log(Level.FINE, "Reference entity: creating {0} as PUBLISHED", entityType);
            } else {
                createStatus = overrideStatus != null ? overrideStatus :
                        (mainEntity != null ? mainEntity.getStatus() : StatusType.DRAFT);
            }
            newDto.setStatus(createStatus);

            return api.create(newDto, createStatus, null, null);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Stub entity creation failed: {0}", e.getMessage());
        }
        return null;
    }

    /**
     * Finds the DTO class for an API's create method via reflection.
     * Result could be cached if this becomes a bottleneck.
     */
    private static Class<?> findDtoClassForApi(AbstractAPI api) {
        for (Method m : api.getClass().getMethods()) {
            if ("create".equals(m.getName()) && m.getParameterCount() == 4) {
                Class<?> paramType = m.getParameterTypes()[0];
                if (EPOSDataModelEntity.class.isAssignableFrom(paramType)) {
                    return paramType;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    private static Object findPublishedVersionOfEntity(LinkedEntity linkedEntity, Class clazz) {
        String uid = linkedEntity.getUid();
        if (uid != null) {
            List<Object> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, clazz);
            for (Object v : allVersions) {
                String status = getModelVersionStatus(v);
                if (STATUS_PUBLISHED.equals(status)) {
                    return v;
                }
            }
        }

        // Fallback: resolve via instanceId if UID search fails
        String instanceId = linkedEntity.getInstanceId();
        if (instanceId != null) {
            List<Object> byInstanceId = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(instanceId, clazz);
            if (!byInstanceId.isEmpty()) {
                Object found = byInstanceId.get(0);
                String foundUid = getModelStrProperty(found, "getUid");
                if (foundUid != null) {
                    List<Object> allVersions = EposDataModelDAO.getInstance()
                            .getOneFromDBByUIDNoCache(foundUid, clazz);
                    for (Object v : allVersions) {
                        String status = getModelVersionStatus(v);
                        if (STATUS_PUBLISHED.equals(status)) {
                            return v;
                        }
                    }
                }
            }
        }

        return null;
    }

    private static LinkedEntity buildLinkedEntity(Object model, String entityType) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(getModelStrProperty(model, "getInstanceId"));
        le.setMetaId(getModelStrProperty(model, "getMetaId"));
        le.setUid(getModelStrProperty(model, "getUid"));
        le.setEntityType(entityType);
        return le;
    }

    private static Object findBestMatchingVersion(List<Object> versions, StatusType targetStatus, String editorId) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }
        if (targetStatus == null) {
            return versions.get(0);
        }

        String targetStatusStr = targetStatus.toString();
        if (StatusType.DRAFT.equals(targetStatus) && editorId != null) {
            for (Object v : versions) {
                if (StatusType.DRAFT.toString().equals(getModelVersionStatus(v)) && sameEditor(editorId, getModelStrProperty(v, "getEditorId"))) {
                    return v;
                }
            }
            for (Object v : versions) {
                if (STATUS_PUBLISHED.equals(getModelVersionStatus(v))) {
                    return v;
                }
            }
            return null;
        }

        for (Object v : versions) {
            String status = getModelVersionStatus(v);
            if (targetStatusStr.equals(status)) {
                return v;
            }
        }

        return versions.get(0);
    }

    private static boolean sameEditor(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.trim().equalsIgnoreCase(right.trim());
    }

    /**
     * Extracts the version status from a model entity using cached reflection.
     */
    private static String getModelVersionStatus(Object modelEntity) {
        try {
            Object versionObj = invokeGetter(modelEntity, VERSION_GETTERS, "getVersion");
            if (versionObj != null) {
                Object statusObj = invokeGetter(versionObj, STATUS_GETTERS, "getStatus");
                return statusObj != null ? statusObj.toString() : null;
            }
        } catch (Exception ignored) {
            // Method not found or invocation failed
        }
        return null;
    }

    private static String getModelStrProperty(Object modelEntity, String methodName) {
        try {
            String cacheKey = modelEntity.getClass().getName() + "#" + methodName;
            Method method = PROPERTY_GETTERS.computeIfAbsent(cacheKey, k -> {
                try {
                    return modelEntity.getClass().getMethod(methodName);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            if (method != null) {
                Object result = method.invoke(modelEntity);
                return result != null ? result.toString() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Invokes a getter using cached MethodHandles for optimal performance.
     */
    private static Object invokeGetter(Object target, ConcurrentHashMap<Class<?>, MethodHandle> cache,
                                       String methodName) {
        Class<?> clazz = target.getClass();
        MethodHandle handle = cache.get(clazz);

        if (handle == null) {
            try {
                Method method = clazz.getMethod(methodName);
                handle = LOOKUP.unreflect(method);
                cache.put(clazz, handle);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                return null;
            }
        }

        try {
            return handle.invoke(target);
        } catch (Throwable t) {
            // MethodHandle.invoke declares throws Throwable; propagate errors, suppress exceptions
            if (t instanceof Error) {
                throw (Error) t;
            }
            return null;
        }
    }

    /**
     * Explicitly cleans up ThreadLocal state to prevent memory leaks in thread-pooled environments.
     * Should be called in a finally block at the top-level entry point of operations
     * that use relation checking functionality.
     */
    public static void cleanupThreadLocals() {
        processingEntities.remove();
    }
}
