package relationsapi;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.LinkedEntity;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for synchronizing entity relations with cascade support.
 *
 * <p>Reference entities (Category, CategoryScheme, Organization, Person, ContactPoint)
 * are shared and maintain links to existing PUBLISHED versions rather than being
 * duplicated. Exception: "ingestor" mode treats them as normal cascade targets.</p>
 *
 * <p><strong>Performance Notes:</strong></p>
 * <ul>
 *   <li>Reflection metadata is cached per-class to eliminate repeated introspection overhead</li>
 *   <li>ThreadLocal contexts use lazy cleanup to minimize allocation churn</li>
 *   <li>Collections are pre-sized where cardinality is predictable</li>
 * </ul>
 */
public class RelationSyncUtil {

    private static final Logger LOG = Logger.getLogger(RelationSyncUtil.class.getName());

    // Normalized to uppercase only - eliminates redundant storage and lookup overhead
    private static final Set<String> REFERENCE_ENTITIES = Set.of(
            "CATEGORY", "CATEGORYSCHEME", "ORGANIZATION", "PERSON", "CONTACTPOINT"
    );

    // Pre-interned status strings avoid repeated string creation in hot paths
    private static final String STATUS_PUBLISHED = StatusType.PUBLISHED.name();
    private static final String STATUS_PENDING = StatusType.PENDING.name();
    private static final String STATUS_ARCHIVED = StatusType.ARCHIVED.name();
    private static final String STATUS_DRAFT = StatusType.DRAFT.name();
    private static final String INGESTOR = "ingestor";

    // ThreadLocal cascade context - tracks in-progress operations to prevent infinite recursion
    private static final ThreadLocal<Set<String>> cascadeInProgress = ThreadLocal.withInitial(() -> new HashSet<>(8));
    private static final ThreadLocal<Map<String, String>> cascadeCreatedVersions = ThreadLocal.withInitial(() -> new HashMap<>(16));

    /*
     * Reflection cache: Eliminates the significant cost of repeated Class.getMethod() calls.
     * The JVM's method lookup is non-trivial, so caching MethodHandles provides ~10x speedup
     * on repeated invocations compared to raw reflection.
     */
    private static final ConcurrentHashMap<Class<?>, MethodHandle> VERSION_GETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, MethodHandle> STATUS_GETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, MethodHandle> INSTANCE_ID_GETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, MethodHandle> UID_GETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, MethodHandle> META_ID_GETTERS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, Method> SETTER_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, Method> ID_GETTER_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Class<?>, List<Method>> STRING_SETTERS_CACHE = new ConcurrentHashMap<>();

    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    /*
     * Fine-grained locks for join entity creation to prevent race conditions.
     * Keyed by joinClass+parentId+targetId to allow maximum concurrency while
     * preventing duplicate key violations from concurrent inserts of the same join.
     */
    private static final ConcurrentHashMap<String, Object> JOIN_LOCKS = new ConcurrentHashMap<>();
    private static final int MAX_JOIN_LOCKS = 10000;

    private static Object getJoinLock(String joinClassName, String parentId, String targetId) {
        String key = joinClassName + ":" + parentId + ":" + targetId;
        return JOIN_LOCKS.computeIfAbsent(key, k -> new Object());
    }

    private static void cleanupJoinLocksIfNeeded() {
        if (JOIN_LOCKS.size() > MAX_JOIN_LOCKS) {
            JOIN_LOCKS.clear();  // Simple reset - locks are transient
        }
    }

    // ===== ThreadLocal Cleanup =====

    /**
     * Explicitly clears all ThreadLocal state. Call this at the end of a request/operation
     * in pooled thread environments (servlet containers, thread pools) to prevent memory leaks.
     * 
     * <p>This method is safe to call even if no ThreadLocal state exists.</p>
     */
    public static void clearThreadLocalState() {
        cascadeInProgress.remove();
        cascadeCreatedVersions.remove();
    }

    /**
     * Execute an operation with guaranteed ThreadLocal cleanup.
     * Use this at API boundaries to ensure thread pool safety.
     *
     * @param operation the operation to execute
     * @param <T> the return type
     * @return the result of the operation
     */
    public static <T> T executeWithCleanup(java.util.function.Supplier<T> operation) {
        try {
            return operation.get();
        } finally {
            clearThreadLocalState();
        }
    }

    /**
     * Execute a void operation with guaranteed ThreadLocal cleanup.
     * Use this at API boundaries to ensure thread pool safety.
     *
     * @param operation the operation to execute
     */
    public static void executeWithCleanup(Runnable operation) {
        try {
            operation.run();
        } finally {
            clearThreadLocalState();
        }
    }

    // ===== Reference Entity Checks =====

    private static boolean isReferenceEntity(Class<?> targetClass) {
        return targetClass != null && REFERENCE_ENTITIES.contains(targetClass.getSimpleName().toUpperCase(Locale.ROOT));
    }

    private static boolean shouldApplyReferenceEntityLogic(Class<?> targetClass, org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (!isReferenceEntity(targetClass)) {
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

    public static boolean isReferenceEntityType(String entityType) {
        return entityType != null && REFERENCE_ENTITIES.contains(entityType.toUpperCase(Locale.ROOT));
    }

    public static boolean shouldApplyReferenceEntityLogicForType(String entityType, org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (!isReferenceEntityType(entityType)) {
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

    // ===== Self-Reference Check =====

    private static boolean isSelfReference(LinkedEntity link, String parentId, String parentUid) {
        if (link == null) {
            return false;
        }
        // Use object identity check first (cheaper), then equals
        String linkInstanceId = link.getInstanceId();
        if (parentId != null && (parentId == linkInstanceId || parentId.equals(linkInstanceId))) {
            return true;
        }
        String linkUid = link.getUid();
        return parentUid != null && (parentUid == linkUid || parentUid.equals(linkUid));
    }

    // ===== Status Propagation =====

    public static <P, C> void propagateStatusToChildren(P parentEntity, String parentInstanceId,
                                                        Class<C> childClass, String foreignKeyFieldName,
                                                        org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (shouldApplyReferenceEntityLogic(childClass, mainEntity)) {
            return;
        }

        StatusType parentStatus = getStatusFromEntity(parentEntity);
        if (parentStatus == null) {
            return;
        }

        List<Object> children = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);
        if (children == null || children.isEmpty()) {
            return;
        }

        for (Object obj : children) {
            if (childClass.isInstance(obj)) {
                updateChildEntityStatus(obj, parentStatus);
            }
        }
    }

    private static void updateChildEntityStatus(Object childEntity, StatusType newStatus) {
        if (childEntity == null || newStatus == null) {
            return;
        }
        try {
            Object versionObj = invokeGetter(childEntity, VERSION_GETTERS, "getVersion");
            if (versionObj instanceof Versioningstatus vs) {
                if (!newStatus.name().equals(vs.getStatus())) {
                    if (newStatus == StatusType.PUBLISHED && vs.getUid() != null) {
                        archiveOldPublishedVersions(vs.getUid(), vs.getVersionId());
                    }
                    vs.setStatus(newStatus.name());
                    vs.setChangeTimestamp(OffsetDateTime.now());
                    EposDataModelDAO.getInstance().updateObject(vs);
                }
            }
        } catch (NoSuchMethodError | IllegalStateException e) {
            updateChildStatusByInstanceId(childEntity, newStatus);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Child status update failed: {0}", e.getMessage());
        }
    }

    private static void archiveOldPublishedVersions(String uid, String currentVersionId) {
        if (uid == null) {
            return;
        }
        try {
            List<Versioningstatus> allVersions = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(uid, Versioningstatus.class);
            OffsetDateTime now = OffsetDateTime.now(); // Single timestamp for batch consistency

            for (Versioningstatus vs : allVersions) {
                String versionId = vs.getVersionId();
                if (versionId != null && versionId.equals(currentVersionId)) {
                    continue;
                }
                String metaId = vs.getMetaId();
                if (metaId != null && metaId.indexOf('.') >= 0) {
                    continue;
                }
                String status = vs.getStatus();
                if (STATUS_PENDING.equals(status)) {
                    continue;
                }
                if (STATUS_PUBLISHED.equals(status)) {
                    vs.setStatus(STATUS_ARCHIVED);
                    vs.setChangeTimestamp(now);
                    vs.setChangeComment("Auto-archived on status propagation");
                    EposDataModelDAO.getInstance().updateObject(vs);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Archive operation failed: {0}", e.getMessage());
        }
    }

    private static void updateChildStatusByInstanceId(Object childEntity, StatusType newStatus) {
        try {
            String instanceId = getModelId(childEntity);
            if (instanceId == null) {
                return;
            }
            List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceId(instanceId, Versioningstatus.class);
            if (vsList != null && !vsList.isEmpty()) {
                Versioningstatus vs = vsList.get(0);
                if (!newStatus.name().equals(vs.getStatus())) {
                    if (newStatus == StatusType.PUBLISHED && vs.getUid() != null) {
                        archiveOldPublishedVersions(vs.getUid(), vs.getVersionId());
                    }
                    vs.setStatus(newStatus.name());
                    vs.setChangeTimestamp(OffsetDateTime.now());
                    EposDataModelDAO.getInstance().updateObject(vs);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Fallback status update failed: {0}", e.getMessage());
        }
    }

    // ===== Simple One-to-Many Sync =====

    public static <P, C> void syncSimpleOneToMany(P parentEntity, String parentInstanceId, List<String> newValues,
                                                  Class<C> childClass, String foreignKeyFieldName, String uidPrefix,
                                                  Function<C, String> valueGetter, BiConsumer<C, String> valueSetter,
                                                  BiConsumer<C, P> parentSetter) {

        // Defensive copy with pre-sized capacity and null filtering
        Set<String> newValuesSet;
        if (newValues == null || newValues.isEmpty()) {
            newValuesSet = Collections.emptySet();
        } else {
            newValuesSet = new HashSet<>(Math.max(newValues.size(), 4));
            for (String val : newValues) {
                if (val != null) {
                    newValuesSet.add(val);
                }
            }
        }

        StatusType parentStatus = getStatusFromEntity(parentEntity);
        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        // Build existing map with predictable sizing
        Map<String, C> existingMap;
        List<C> existingEntities;
        if (rawObjects == null || rawObjects.isEmpty()) {
            existingMap = Collections.emptyMap();
            existingEntities = Collections.emptyList();
        } else {
            existingEntities = new ArrayList<>(rawObjects.size());
            existingMap = new HashMap<>(rawObjects.size());
            for (Object obj : rawObjects) {
                if (childClass.isInstance(obj)) {
                    C entity = childClass.cast(obj);
                    existingEntities.add(entity);
                    String key = valueGetter.apply(entity);
                    if (key != null) {
                        existingMap.putIfAbsent(key, entity); // Keep first occurrence
                    }
                }
            }
        }

        // Delete removed entries
        for (C existing : existingEntities) {
            String val = valueGetter.apply(existing);
            if (val != null && !newValuesSet.contains(val)) {
                EposDataModelDAO.getInstance().deleteObject(existing);
            }
        }

        // Add new or update existing
        for (String newValue : newValuesSet) {
            C existing = existingMap.get(newValue);
            if (existing == null) {
                try {
                    C newEntity = childClass.getDeclaredConstructor().newInstance();
                    setStandardFields(newEntity, uidPrefix, parentStatus);
                    valueSetter.accept(newEntity, newValue);
                    parentSetter.accept(newEntity, parentEntity);
                    EposDataModelDAO.getInstance().updateObject(newEntity);
                } catch (Exception e) {
                    throw new RuntimeException("Relation sync failed for " + childClass.getSimpleName(), e);
                }
            } else {
                updateChildEntityStatus(existing, parentStatus);
            }
        }
    }

    public static <P, C> void copySimpleOneToMany(String oldParentInstanceId, P newParentEntity, String newParentInstanceId,
                                                  Class<C> childClass, String foreignKeyFieldName, String uidPrefix,
                                                  Function<C, String> valueGetter, BiConsumer<C, String> valueSetter,
                                                  BiConsumer<C, P> parentSetter) {

        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, oldParentInstanceId, childClass);
        if (oldRelations == null || oldRelations.isEmpty()) {
            return;
        }

        StatusType parentStatus = getStatusFromEntity(newParentEntity);
        for (Object obj : oldRelations) {
            C oldEntity = childClass.cast(obj);
            String value = valueGetter.apply(oldEntity);
            try {
                C newEntity = childClass.getDeclaredConstructor().newInstance();
                setStandardFields(newEntity, uidPrefix, parentStatus);
                valueSetter.accept(newEntity, value);
                parentSetter.accept(newEntity, newParentEntity);
                EposDataModelDAO.getInstance().updateObject(newEntity);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Simple relation copy failed: {0}", e.getMessage());
            }
        }
    }

    // ===== Complex Relation Sync (Main Method) =====

    public static <P, J, T> void syncComplexRelation(P parentDbObject, String parentId, List<LinkedEntity> inputLinks,
                                                     LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate,
                                                     Class<J> joinClass, Class<T> targetClass, String parentFieldName,
                                                     Function<J, T> targetGetter, BiConsumer<J, P> parentSetter,
                                                     BiConsumer<J, T> targetSetter,
                                                     org.epos.eposdatamodel.EPOSDataModelEntity mainEntity,
                                                     org.epos.eposdatamodel.EPOSDataModelEntity previousEntity,
                                                     model.StatusType overrideStatus, boolean enableStore) {

        String previousInstanceId = mainEntity.getInstanceChangedId();
        boolean hasInstanceChanged = previousInstanceId != null && !previousInstanceId.equals(parentId);
        StatusType currentStatus = mainEntity.getStatus();
        boolean isNewVersion = hasInstanceChanged && currentStatus == StatusType.DRAFT;
        boolean effectiveEnableStore = enableStore || isReferenceEntity(targetClass);

        String parentMetaId = null;
        if (isNewVersion) {
            parentMetaId = getMetaId(parentDbObject);
            if (parentMetaId != null) {
                cascadeInProgress.get().add(parentMetaId);
                // Pre-compute cache key to avoid repeated concatenation
                String statusName = overrideStatus != null ? overrideStatus.name() : mainEntity.getStatus().name();
                cascadeCreatedVersions.get().put(parentMetaId + "_" + statusName, parentId);
            }
        }

        try {
            StatusType effectiveStatus = overrideStatus != null ? overrideStatus : mainEntity.getStatus();

            // Handle null or empty list - copy from previous if new version
            if (inputLinks == null || inputLinks.isEmpty()) {
                if (isNewVersion) {
                    copyComplexRelationsFromPreviousVersion(previousInstanceId, parentDbObject, parentId,
                            joinClass, targetClass, parentFieldName, targetGetter, effectiveStatus, mainEntity);
                }
                return;
            }

            // Handle update replacement in-place
            if (relationFromUpdate != null && inputLinks.contains(relationFromUpdate)) {
                inputLinks.remove(relationFromUpdate);
                inputLinks.add(relationToUpdate);
            }

            // Compute embedded ID field name once
            String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");

            // Get existing joins and build lookup map
            List<?> existingRawList = EposDataModelDAO.getInstance()
                    .getJoinEntitiesByParentId(embeddedIdField, parentId, joinClass);
            Map<String, J> existingMap = new HashMap<>(existingRawList != null ? existingRawList.size() : 4);
            if (existingRawList != null) {
                for (Object o : existingRawList) {
                    J joinEntity = joinClass.cast(o);
                    T target = targetGetter.apply(joinEntity);
                    String targetId = getModelId(target);
                    if (targetId != null) {
                        existingMap.put(targetId, joinEntity);
                    }
                }
            }

            Set<String> processedIds = new HashSet<>(inputLinks.size());
            String sourceEntityType = parentDbObject.getClass().getSimpleName().toUpperCase(Locale.ROOT);
            String parentUid = getUid(parentDbObject);
            StatusType cascadeStatus = isNewVersion ? effectiveStatus : null;

            for (LinkedEntity link : inputLinks) {
                // Pre-filter self-references before expensive DB lookups
                if (isSelfReference(link, parentId, parentUid)) {
                    LOG.log(Level.WARNING, "Self-reference blocked: {0}", parentId);
                    continue;
                }

                Object rawTarget = null;

                // Direct lookup optimization for non-new versions with instance ID
                if (!isNewVersion && link.getInstanceId() != null) {
                    List<Object> directResults = EposDataModelDAO.getInstance()
                            .getOneFromDBByInstanceIdNoCache(link.getInstanceId(), targetClass);
                    if (directResults != null && !directResults.isEmpty()) {
                        rawTarget = directResults.get(0);
                    }
                }

                if (rawTarget == null) {
                    rawTarget = RelationChecker.checkRelation(mainEntity, previousEntity, null, link,
                            effectiveStatus, targetClass, effectiveEnableStore);
                }

                // Stub creation fallback for reference entities
                if (rawTarget == null && isReferenceEntity(targetClass) && link.getUid() != null) {
                    rawTarget = createStubEntity(targetClass, link.getUid(), effectiveStatus);
                }

                if (rawTarget != null) {
                    T targetEntity = targetClass.cast(rawTarget);
                    String targetId = getModelId(targetEntity);
                    String targetUid = getUid(targetEntity);

                    if (targetId != null) {
                        // Post-resolution self-reference guard
                        if (targetId.equals(parentId) || (targetUid != null && targetUid.equals(parentUid))) {
                            LOG.log(Level.WARNING, "Post-resolution self-reference blocked: {0}", parentId);
                            continue;
                        }

                        T targetForJoin = targetEntity;
                        String targetIdForJoin = targetId;

                        // Handle cascade versioning or status propagation
                        if (cascadeStatus != null) {
                            if (shouldApplyReferenceEntityLogic(targetClass, mainEntity)) {
                                T publishedVersion = findPublishedVersion(targetEntity, targetClass);
                                if (publishedVersion != null) {
                                    targetForJoin = publishedVersion;
                                    targetIdForJoin = getModelId(publishedVersion);
                                } else {
                                    T newPublished = createReferenceEntityAsPublished(targetEntity, targetClass);
                                    if (newPublished != null) {
                                        targetForJoin = newPublished;
                                        targetIdForJoin = getModelId(newPublished);
                                    }
                                }
                            } else {
                                T newVersionTarget = createCascadeVersion(targetEntity, targetClass, cascadeStatus);
                                if (newVersionTarget != null) {
                                    targetForJoin = newVersionTarget;
                                    targetIdForJoin = getModelId(newVersionTarget);
                                }
                            }
                        } else if (effectiveStatus != null && !shouldApplyReferenceEntityLogic(targetClass, mainEntity)) {
                            updateChildEntityStatus(targetEntity, effectiveStatus);
                        }

                        processedIds.add(targetIdForJoin);

                        if (!existingMap.containsKey(targetIdForJoin)) {
                            boolean created = createJoinEntity(joinClass, parentDbObject, targetForJoin,
                                    parentSetter, targetSetter, parentFieldName);
                            if (!created) {
                                createPendingRelation(parentId, sourceEntityType, link.getUid(),
                                        link.getEntityType(), joinClass.getName());
                            }
                        }
                    }
                } else {
                    createPendingRelation(parentId, sourceEntityType, link.getUid(),
                            link.getEntityType(), joinClass.getName());
                }
            }

            // Delete joins no longer present
            for (Map.Entry<String, J> entry : existingMap.entrySet()) {
                if (!processedIds.contains(entry.getKey())) {
                    EposDataModelDAO.getInstance().deleteObject(entry.getValue());
                }
            }

        } finally {
            if (parentMetaId != null) {
                Set<String> inProgress = cascadeInProgress.get();
                inProgress.remove(parentMetaId);
                if (inProgress.isEmpty()) {
                    // Clean up ThreadLocals only when cascade stack is empty
                    cascadeInProgress.remove();
                    cascadeCreatedVersions.remove();
                }
            }
        }
    }

    // ===== Stub Entity Creation for Reference Entities =====

    @SuppressWarnings("unchecked")
    private static <T> T createStubEntity(Class<T> targetClass, String uid, StatusType status) {
        try {
            String entityName = targetClass.getSimpleName().toUpperCase(Locale.ROOT);
            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) {
                LOG.log(Level.WARNING, "No API for stub creation: {0}", entityName);
                return null;
            }

            org.epos.eposdatamodel.EPOSDataModelEntity dto = createMinimalDto(entityName, uid);
            if (dto == null) {
                return null;
            }

            // Reference entities always publish immediately
            StatusType createStatus = isReferenceEntity(targetClass) ? StatusType.PUBLISHED : status;
            dto.setStatus(createStatus);

            LinkedEntity result = api.create(dto, createStatus, null, null);
            if (result != null && result.getInstanceId() != null) {
                List<Object> created = EposDataModelDAO.getInstance()
                        .getOneFromDBByInstanceIdNoCache(result.getInstanceId(), targetClass);
                if (created != null && !created.isEmpty()) {
                    LOG.log(Level.INFO, "Stub created: {0} uid={1}", new Object[]{entityName, uid});
                    return targetClass.cast(created.get(0));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Stub creation failed: {0}", e.getMessage());
        }
        return null;
    }

    private static org.epos.eposdatamodel.EPOSDataModelEntity createMinimalDto(String entityName, String uid) {
        try {
            org.epos.eposdatamodel.EPOSDataModelEntity dto = switch (entityName) {
                case "CATEGORY" -> {
                    var cat = new org.epos.eposdatamodel.Category();
                    cat.setName("Stub: " + uid);
                    yield cat;
                }
                case "CATEGORYSCHEME" -> {
                    var scheme = new org.epos.eposdatamodel.CategoryScheme();
                    scheme.setTitle("Stub: " + uid);
                    yield scheme;
                }
                case "ORGANIZATION" -> {
                    var org = new org.epos.eposdatamodel.Organization();
                    org.addLegalName("Stub: " + uid);
                    yield org;
                }
                case "PERSON" -> {
                    var person = new org.epos.eposdatamodel.Person();
                    person.setFamilyName("Stub");
                    yield person;
                }
                case "CONTACTPOINT" -> new org.epos.eposdatamodel.ContactPoint();
                default -> createGenericDto(entityName, uid);
            };
            if (dto != null) {
                dto.setUid(uid);
            }
            return dto;
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Minimal DTO creation failed: {0}", e.getMessage());
            return null;
        }
    }

    private static org.epos.eposdatamodel.EPOSDataModelEntity createGenericDto(String entityName, String uid) {
        try {
            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) {
                return null;
            }
            for (Method m : api.getClass().getMethods()) {
                if ("create".equals(m.getName()) && m.getParameterCount() == 4) {
                    Class<?> paramType = m.getParameterTypes()[0];
                    if (org.epos.eposdatamodel.EPOSDataModelEntity.class.isAssignableFrom(paramType)) {
                        org.epos.eposdatamodel.EPOSDataModelEntity dto =
                                (org.epos.eposdatamodel.EPOSDataModelEntity) paramType.getDeclaredConstructor().newInstance();
                        dto.setUid(uid);
                        return dto;
                    }
                }
            }
        } catch (Exception ignored) {
            // Fall through to null
        }
        return null;
    }

    // ===== Join Entity Creation =====

    /**
     * Creates a join entity with fine-grained locking to prevent race conditions.
     * Uses double-check pattern: check existence, acquire lock, re-check, then insert.
     * Duplicate key exceptions are handled gracefully as they indicate concurrent success.
     */
    private static <P, J, T> boolean createJoinEntity(Class<J> joinClass, P parentDbObject, T targetEntity,
                                                      BiConsumer<J, P> parentSetter, BiConsumer<J, T> targetSetter,
                                                      String parentFieldName) {
        String parentId = getModelId(parentDbObject);
        String targetId = getModelId(targetEntity);

        // Final self-reference guard
        if (parentId != null && parentId.equals(targetId)) {
            LOG.log(Level.WARNING, "Self-reference blocked in join creation: {0}", parentId);
            return true;
        }

        // Quick check without lock - optimization for common case where join exists
        if (joinExistsWithFieldName(joinClass, parentId, targetId, parentFieldName)) {
            return true;
        }

        // Fine-grained lock per join table + key combination
        Object lock = getJoinLock(joinClass.getName(), parentId, targetId);
        synchronized (lock) {
            try {
                // Re-check inside synchronized block (double-check pattern)
                if (joinExistsWithFieldName(joinClass, parentId, targetId, parentFieldName)) {
                    return true;
                }

                J newJoin = joinClass.getDeclaredConstructor().newInstance();
                initializeEmbeddedIdWithFieldName(newJoin, parentDbObject, targetEntity, parentFieldName);

                if (parentSetter != null) {
                    parentSetter.accept(newJoin, parentDbObject);
                }
                if (targetSetter != null) {
                    targetSetter.accept(newJoin, targetEntity);
                }

                if (!verifyEmbeddedIdNotSelfReference(newJoin)) {
                    LOG.log(Level.SEVERE, "Embedded ID self-reference detected - aborting");
                    return true;
                }

                EposDataModelDAO.getInstance().updateObject(newJoin);
                cleanupJoinLocksIfNeeded();
                return true;

            } catch (Exception e) {
                // Check both exception message and cause for duplicate key indicators
                if (isDuplicateKeyException(e)) {
                    // Benign - join was created by concurrent operation (different JVM/process)
                    LOG.log(Level.FINE, "Join {0} already exists for parent={1}, target={2} (concurrent insert)",
                            new Object[]{joinClass.getSimpleName(), parentId, targetId});
                    return true;
                }
                LOG.log(Level.WARNING, "Join creation failed for {0}: {1}",
                        new Object[]{joinClass.getSimpleName(), e.getMessage()});
                return false;
            }
        }
    }

    /**
     * Checks if an exception indicates a duplicate key/unique constraint violation.
     * Examines both the exception message and its cause chain.
     */
    private static boolean isDuplicateKeyException(Exception e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("duplicate key") ||
                                msg.contains("already exists") ||
                                msg.contains("unique constraint") ||
                                msg.contains("violates unique"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean verifyEmbeddedIdNotSelfReference(Object joinEntity) {
        try {
            Method getIdMethod = findIdGetter(joinEntity.getClass());
            if (getIdMethod == null) {
                return true;
            }
            Object embeddedId = getIdMethod.invoke(joinEntity);
            if (embeddedId != null) {
                // Check for CategoryIspartof-style self-references
                try {
                    Method getCat1 = embeddedId.getClass().getMethod("getCategory1InstanceId");
                    Method getCat2 = embeddedId.getClass().getMethod("getCategory2InstanceId");
                    String cat1Id = (String) getCat1.invoke(embeddedId);
                    String cat2Id = (String) getCat2.invoke(embeddedId);
                    return cat1Id == null || !cat1Id.equals(cat2Id);
                } catch (NoSuchMethodException ignored) {
                    // Not a category-style join - OK
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Embedded ID verification: {0}", e.getMessage());
        }
        return true;
    }

    private static <J> boolean joinExistsWithFieldName(Class<J> joinClass, String parentId, String targetId,
                                                       String parentFieldName) {
        if (parentId == null || targetId == null) {
            return false;
        }
        try {
            String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
            List<?> existing = EposDataModelDAO.getInstance().getJoinEntitiesByParentId(embeddedIdField, parentId, joinClass);
            if (existing != null) {
                String targetFieldName = deriveTargetFieldName(parentFieldName);
                for (Object obj : existing) {
                    String existingTargetId = getTargetIdFromJoin(obj, targetFieldName);
                    if (targetId.equals(existingTargetId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Join existence check failed: {0}", e.getMessage());
        }
        return false;
    }

    private static String getTargetIdFromJoin(Object joinEntity, String targetFieldName) {
        try {
            String getterName = "get" + capitalize(targetFieldName);
            Method getter = joinEntity.getClass().getMethod(getterName);
            Object target = getter.invoke(joinEntity);
            return target != null ? getModelId(target) : null;
        } catch (Exception e) {
            return null;
        }
    }

    // ===== Embedded ID Initialization =====

    private static <P, T> void initializeEmbeddedIdWithFieldName(Object joinEntity, P parent, T target,
                                                                 String parentFieldName) {
        try {
            Method getIdMethod = findIdGetter(joinEntity.getClass());
            if (getIdMethod == null) {
                return;
            }

            Class<?> idClass = getIdMethod.getReturnType();
            Object idInstance = idClass.getDeclaredConstructor().newInstance();
            String parentInstanceId = getModelId(parent);
            String targetInstanceId = getModelId(target);

            String parentIdField = parentFieldName.replace("Instance", "InstanceId");
            String targetIdField = deriveTargetFieldName(parentFieldName).replace("Instance", "InstanceId");

            // Set parent ID
            invokeCachedSetter(idClass, idInstance, "set" + capitalize(parentIdField), parentInstanceId);
            // Set target ID
            invokeCachedSetter(idClass, idInstance, "set" + capitalize(targetIdField), targetInstanceId);

            Method setIdMethod = joinEntity.getClass().getMethod("setId", idClass);
            setIdMethod.invoke(joinEntity, idInstance);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Embedded ID init failed: {0}", e.getMessage());
        }
    }

    private static String deriveTargetFieldName(String parentFieldName) {
        int idx1 = parentFieldName.indexOf('1');
        if (idx1 >= 0) {
            return parentFieldName.substring(0, idx1) + '2' + parentFieldName.substring(idx1 + 1);
        }
        int idx2 = parentFieldName.indexOf('2');
        if (idx2 >= 0) {
            return parentFieldName.substring(0, idx2) + '1' + parentFieldName.substring(idx2 + 1);
        }
        return parentFieldName;
    }

    // ===== Copy Relations from Previous Version =====

    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(String oldParentInstanceId, P newParentDbObject,
                                                                          String newParentId, Class<J> joinClass,
                                                                          Class<T> targetClass, String parentFieldName,
                                                                          Function<J, T> targetGetter, StatusType cascadeStatus,
                                                                          org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {

        String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
        List<?> oldRelationsRaw = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId(embeddedIdField, oldParentInstanceId, joinClass);
        if (oldRelationsRaw == null || oldRelationsRaw.isEmpty()) {
            return;
        }

        for (Object obj : oldRelationsRaw) {
            J oldJoin = joinClass.cast(obj);
            T target = targetGetter.apply(oldJoin);
            String targetId = getModelId(target);

            if (targetId != null && !targetId.equals(newParentId)) {
                try {
                    T targetForJoin = target;
                    if (cascadeStatus != null) {
                        if (shouldApplyReferenceEntityLogic(targetClass, mainEntity)) {
                            T publishedVersion = findPublishedVersion(target, targetClass);
                            if (publishedVersion != null) {
                                targetForJoin = publishedVersion;
                            }
                        } else {
                            T newVersionTarget = createCascadeVersion(target, targetClass, cascadeStatus);
                            if (newVersionTarget != null) {
                                targetForJoin = newVersionTarget;
                            }
                        }
                    }

                    J newJoin = joinClass.getDeclaredConstructor().newInstance();
                    setJoinRelationship(newJoin, newParentDbObject);
                    setJoinRelationship(newJoin, targetForJoin);
                    initializeEmbeddedIdWithFieldName(newJoin, newParentDbObject, targetForJoin, parentFieldName);
                    EposDataModelDAO.getInstance().updateObject(newJoin);
                } catch (Exception e) {
                    String msg = e.getMessage();
                    if (msg == null || (!msg.contains("duplicate key") && !msg.contains("already exists"))) {
                        LOG.log(Level.WARNING, "Relation copy failed: {0}", msg);
                    }
                }
            }
        }
    }

    // ===== Cascade Version Creation =====

    @SuppressWarnings("unchecked")
    private static <T> T createCascadeVersion(T originalEntity, Class<T> targetClass, StatusType newStatus) {
        String originalInstanceId = getModelId(originalEntity);
        if (originalInstanceId == null) {
            return null;
        }

        String metaId = getMetaId(originalEntity);
        if (metaId == null) {
            metaId = originalInstanceId;
        }
        String cacheKey = metaId + "_" + newStatus.name();

        Map<String, String> createdVersions = cascadeCreatedVersions.get();
        String existingNewInstanceId = createdVersions.get(cacheKey);
        if (existingNewInstanceId != null) {
            List<Object> existingList = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(existingNewInstanceId, targetClass);
            if (existingList != null && !existingList.isEmpty()) {
                return targetClass.cast(existingList.get(0));
            }
        }

        Set<String> inProgress = cascadeInProgress.get();
        if (inProgress.contains(metaId)) {
            return null;
        }

        try {
            inProgress.add(metaId);
            String entityName = targetClass.getSimpleName().toUpperCase(Locale.ROOT);
            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) {
                return null;
            }

            org.epos.eposdatamodel.EPOSDataModelEntity dto =
                    (org.epos.eposdatamodel.EPOSDataModelEntity) api.retrieve(originalInstanceId);
            if (dto == null) {
                return null;
            }

            dto.setInstanceChangedId(originalInstanceId);
            dto.setInstanceId(null);
            dto.setStatus(newStatus);

            LinkedEntity result = api.create(dto, newStatus, null, null);
            if (result != null && result.getInstanceId() != null) {
                createdVersions.put(cacheKey, result.getInstanceId());
                List<Object> newList = EposDataModelDAO.getInstance()
                        .getOneFromDBByInstanceIdNoCache(result.getInstanceId(), targetClass);
                if (newList != null && !newList.isEmpty()) {
                    return targetClass.cast(newList.get(0));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Cascade version creation failed: {0}", e.getMessage());
        } finally {
            inProgress.remove(metaId);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createReferenceEntityAsPublished(T originalEntity, Class<T> targetClass) {
        String originalInstanceId = getModelId(originalEntity);
        if (originalInstanceId == null) {
            return null;
        }

        try {
            String entityName = targetClass.getSimpleName().toUpperCase(Locale.ROOT);
            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) {
                return null;
            }

            org.epos.eposdatamodel.EPOSDataModelEntity dto =
                    (org.epos.eposdatamodel.EPOSDataModelEntity) api.retrieve(originalInstanceId);
            if (dto == null) {
                return null;
            }

            dto.setInstanceId(null);
            dto.setStatus(StatusType.PUBLISHED);

            LinkedEntity result = api.create(dto, StatusType.PUBLISHED, null, null);
            if (result != null && result.getInstanceId() != null) {
                List<Object> newList = EposDataModelDAO.getInstance()
                        .getOneFromDBByInstanceIdNoCache(result.getInstanceId(), targetClass);
                if (newList != null && !newList.isEmpty()) {
                    return targetClass.cast(newList.get(0));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Reference entity publish failed: {0}", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T findPublishedVersion(T entity, Class<T> targetClass) {
        String uid = getUid(entity);
        if (uid == null) {
            return null;
        }
        try {
            List<Object> versions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, targetClass);
            for (Object v : versions) {
                String status = getVersionStatus(v);
                if (STATUS_PUBLISHED.equals(status)) {
                    return targetClass.cast(v);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Published version lookup failed: {0}", e.getMessage());
        }
        return null;
    }

    // ===== Pending Relations =====

    private static void createPendingRelation(String sourceInstanceId, String sourceEntityType,
                                              String targetUid, String targetEntityType, String joinClassName) {
        if (pendingRelationExists(sourceInstanceId, targetUid, joinClassName)) {
            return;
        }
        try {
            Versioningstatus pending = new Versioningstatus();
            pending.setInstanceId(UUID.randomUUID().toString());
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setUid(targetUid);
            pending.setMetaId(joinClassName);
            pending.setStatus(STATUS_PENDING);
            pending.setReviewComment(sourceInstanceId);
            pending.setProvenance(sourceEntityType);
            pending.setChangeTimestamp(OffsetDateTime.now());
            EposDataModelDAO.getInstance().updateObject(pending);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Pending relation creation failed: {0}", e.getMessage());
        }
    }

    private static boolean pendingRelationExists(String sourceInstanceId, String targetUid, String joinClassName) {
        try {
            List<Versioningstatus> existing = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(targetUid, Versioningstatus.class);
            for (Versioningstatus vs : existing) {
                if (STATUS_PENDING.equals(vs.getStatus()) &&
                        joinClassName.equals(vs.getMetaId()) &&
                        sourceInstanceId.equals(vs.getReviewComment())) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Pending relation check failed: {0}", e.getMessage());
        }
        return false;
    }

    public static void resolvePendingRelations(String entityUid, String entityType, Object entityDbObject) {
        try {
            List<Versioningstatus> pendingList = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(entityUid, Versioningstatus.class);
            for (Versioningstatus vs : pendingList) {
                String metaId = vs.getMetaId();
                if (STATUS_PENDING.equals(vs.getStatus()) && metaId != null && metaId.indexOf('.') >= 0) {
                    try {
                        resolveSinglePendingRelation(vs, entityDbObject);
                        EposDataModelDAO.getInstance().deleteObject(vs);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Pending relation resolution failed: {0}", e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Pending relations batch resolution failed: {0}", e.getMessage());
        }
    }

    /**
     * Resolves a single pending relation with fine-grained locking to prevent race conditions.
     * Supports both standard join tables and polymorphic relations (e.g., softwareapplication_creator).
     */
    private static void resolveSinglePendingRelation(Versioningstatus pending, Object targetEntity) throws Exception {
        String sourceInstanceId = pending.getReviewComment();
        if (sourceInstanceId == null) {
            sourceInstanceId = pending.getInstanceId();
        }
        String sourceEntityType = pending.getProvenance();
        String joinClassName = pending.getMetaId();
        // For polymorphic relations, changeComment stores the target entity type (e.g., "ORGANIZATION", "PERSON")
        String targetEntityType = pending.getChangeComment();
        
        if (sourceEntityType == null) {
            return;
        }

        Class<?> sourceClass = AbstractAPI.retrieveClass(sourceEntityType);
        if (sourceClass == null) {
            return;
        }

        List<Object> sourceList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(sourceInstanceId, sourceClass);
        if (sourceList == null || sourceList.isEmpty()) {
            return;
        }

        Object sourceEntity = sourceList.get(0);
        Class<?> joinClass = Class.forName(joinClassName);

        String sourceId = getModelId(sourceEntity);
        String targetId = getModelId(targetEntity);

        // Self-reference check
        if (sourceId != null && sourceId.equals(targetId)) {
            return;
        }

        // Fine-grained lock to prevent duplicate join creation
        Object lock = getJoinLock(joinClass.getName(), sourceId, targetId);
        synchronized (lock) {
            Object newJoin = joinClass.getDeclaredConstructor().newInstance();
            
            // Check if this is a polymorphic relation (e.g., creator, author, etc.)
            if (isPolymorphicJoinClass(joinClass) && targetEntityType != null) {
                // Handle polymorphic relations that store entity reference via entityInstanceId + resourceEntity
                setPolymorphicJoinRelationship(newJoin, sourceEntity, targetEntity, targetEntityType);
            } else {
                // Standard join table handling
                initializeEmbeddedIdLegacy(newJoin, sourceEntity, targetEntity);
                setJoinRelationship(newJoin, sourceEntity);
                setJoinRelationship(newJoin, targetEntity);
            }

            try {
                EposDataModelDAO.getInstance().updateObject(newJoin);
                cleanupJoinLocksIfNeeded();
            } catch (Exception e) {
                if (!isDuplicateKeyException(e)) {
                    throw e;
                }
                // Duplicate key is benign - join already exists
                LOG.log(Level.FINE, "Pending relation join already exists: {0}", joinClass.getSimpleName());
            }
        }
    }

    // Legacy embedded ID init for pending relations (alphabetical ordering for same-class)
    private static <P, T> void initializeEmbeddedIdLegacy(Object joinEntity, P parent, T target) {
        try {
            Method getIdMethod = findIdGetter(joinEntity.getClass());
            if (getIdMethod == null) {
                return;
            }

            Class<?> idClass = getIdMethod.getReturnType();
            Object idInstance = idClass.getDeclaredConstructor().newInstance();
            String parentInstanceId = getModelId(parent);
            String targetInstanceId = getModelId(target);
            String parentClassName = parent.getClass().getSimpleName().toLowerCase(Locale.ROOT);
            String targetClassName = target.getClass().getSimpleName().toLowerCase(Locale.ROOT);

            List<Method> stringSetters = getStringSetters(idClass);

            if (parentClassName.equals(targetClassName)) {
                if (stringSetters.size() < 2) {
                    LOG.log(Level.WARNING, "Expected at least 2 string setters for embedded ID class {0}, found {1}. " +
                            "Cannot initialize join entity for parent={2}, target={3}",
                            new Object[]{idClass.getSimpleName(), stringSetters.size(), parentInstanceId, targetInstanceId});
                    return;
                }
                stringSetters.get(0).invoke(idInstance, parentInstanceId);
                stringSetters.get(1).invoke(idInstance, targetInstanceId);
            } else {
                boolean parentSet = false, targetSet = false;
                for (Method setter : stringSetters) {
                    String setterNameLower = setter.getName().toLowerCase(Locale.ROOT);
                    if (!parentSet && setterNameLower.contains(parentClassName)) {
                        setter.invoke(idInstance, parentInstanceId);
                        parentSet = true;
                    } else if (!targetSet && setterNameLower.contains(targetClassName)) {
                        setter.invoke(idInstance, targetInstanceId);
                        targetSet = true;
                    }
                }
            }
            Method setIdMethod = joinEntity.getClass().getMethod("setId", idClass);
            setIdMethod.invoke(joinEntity, idInstance);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Legacy embedded ID init failed: {0}", e.getMessage());
        }
    }

    // ===== Helper Methods =====

    private static void setStandardFields(Object entity, String uidPrefix, StatusType parentStatus) {
        try {
            // Generate UUIDs once for reuse where appropriate
            String instanceId = UUID.randomUUID().toString();
            String metaId = UUID.randomUUID().toString();
            String baseUid = UUID.randomUUID().toString();
            String uid = uidPrefix != null ? uidPrefix + "/" + baseUid : baseUid;

            invokeSetter(entity, "setInstanceId", instanceId);
            invokeSetter(entity, "setMetaId", metaId);
            invokeSetter(entity, "setUid", uid);

            try {
                Method setVersion = entity.getClass().getMethod("setVersion", Versioningstatus.class);
                Versioningstatus vs = new Versioningstatus();
                vs.setVersionId(UUID.randomUUID().toString());
                vs.setInstanceId(UUID.randomUUID().toString());
                vs.setUid(uidPrefix != null ? uidPrefix + "/" + UUID.randomUUID() : UUID.randomUUID().toString());
                vs.setStatus(parentStatus != null ? parentStatus.name() : STATUS_DRAFT);
                vs.setChangeTimestamp(OffsetDateTime.now());
                vs.setMetaId(entity.getClass().getSimpleName());
                setVersion.invoke(entity, vs);
            } catch (NoSuchMethodException ignored) {
                // Entity doesn't have versioning
            }
        } catch (Exception e) {
            throw new RuntimeException("Standard fields setup failed for " + entity.getClass().getName(), e);
        }
    }

    private static StatusType getStatusFromEntity(Object entity) {
        if (entity == null) {
            return null;
        }
        try {
            Object statusObj = invokeGetter(entity, STATUS_GETTERS, "getStatus");
            if (statusObj instanceof StatusType st) {
                return st;
            }
            if (statusObj != null) {
                try {
                    return StatusType.valueOf(statusObj.toString());
                } catch (IllegalArgumentException ignored) {
                }
            }
        } catch (Exception ignored) {
        }

        try {
            Object versionObj = invokeGetter(entity, VERSION_GETTERS, "getVersion");
            if (versionObj instanceof Versioningstatus vs) {
                String statusStr = vs.getStatus();
                if (statusStr != null) {
                    try {
                        return StatusType.valueOf(statusStr);
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getVersionStatus(Object entity) {
        try {
            Object versionObj = invokeGetter(entity, VERSION_GETTERS, "getVersion");
            if (versionObj != null) {
                Object statusObj = invokeGetter(versionObj, STATUS_GETTERS, "getStatus");
                return statusObj != null ? statusObj.toString() : null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static void setJoinRelationship(Object joinEntity, Object relatedEntity) throws Exception {
        String entityName = relatedEntity.getClass().getSimpleName();
        String entityNameLower = entityName.toLowerCase(Locale.ROOT);

        for (Method method : joinEntity.getClass().getMethods()) {
            String methodName = method.getName();
            if (methodName.startsWith("set") &&
                    methodName.toLowerCase(Locale.ROOT).contains(entityNameLower) &&
                    method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0].isAssignableFrom(relatedEntity.getClass())) {
                method.invoke(joinEntity, relatedEntity);
                return;
            }
        }
    }

    /**
     * Checks if a join class is a polymorphic relation (stores entity reference via entityInstanceId + resourceEntity).
     * Polymorphic join classes have setEntityInstanceId and setResourceEntity methods instead of direct entity setters.
     */
    private static boolean isPolymorphicJoinClass(Class<?> joinClass) {
        boolean hasEntityInstanceId = false;
        boolean hasResourceEntity = false;
        for (Method method : joinClass.getMethods()) {
            String methodName = method.getName();
            if ("setEntityInstanceId".equals(methodName) && method.getParameterCount() == 1 
                    && method.getParameterTypes()[0] == String.class) {
                hasEntityInstanceId = true;
            } else if ("setResourceEntity".equals(methodName) && method.getParameterCount() == 1 
                    && method.getParameterTypes()[0] == String.class) {
                hasResourceEntity = true;
            }
        }
        return hasEntityInstanceId && hasResourceEntity;
    }

    /**
     * Sets up a polymorphic join relationship where the target entity (Organization/Person) is stored
     * via entityInstanceId and resourceEntity fields rather than a direct entity reference.
     * 
     * @param joinEntity The join entity to populate
     * @param sourceEntity The source entity (e.g., Softwareapplication)
     * @param targetEntity The target entity (e.g., Organization or Person)
     * @param targetEntityType The entity type string (e.g., "ORGANIZATION" or "PERSON")
     */
    private static void setPolymorphicJoinRelationship(Object joinEntity, Object sourceEntity, 
            Object targetEntity, String targetEntityType) throws Exception {
        // Set the source entity reference (e.g., setSoftwareapplication)
        setJoinRelationship(joinEntity, sourceEntity);
        
        // Set the source instance ID (e.g., setSoftwareapplicationInstanceId)
        String sourceId = getModelId(sourceEntity);
        String sourceClassName = sourceEntity.getClass().getSimpleName();
        String sourceIdSetterName = "set" + sourceClassName + "InstanceId";
        invokeSetter(joinEntity, sourceIdSetterName, sourceId);
        
        // Set the polymorphic target fields
        String targetId = getModelId(targetEntity);
        invokeSetter(joinEntity, "setEntityInstanceId", targetId);
        invokeSetter(joinEntity, "setResourceEntity", targetEntityType);
    }

    private static void invokeSetter(Object obj, String methodName, String value) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, value);
        } catch (Exception ignored) {
        }
    }

    private static void invokeCachedSetter(Class<?> clazz, Object instance, String methodName, String value) {
        try {
            String cacheKey = clazz.getName() + "#" + methodName;
            Method method = SETTER_CACHE.computeIfAbsent(cacheKey, k -> {
                try {
                    return clazz.getMethod(methodName, String.class);
                } catch (NoSuchMethodException e) {
                    return null;
                }
            });
            if (method != null) {
                method.invoke(instance, value);
            }
        } catch (Exception ignored) {
        }
    }

    private static String getModelId(Object modelObj) {
        return getModelProperty(modelObj, INSTANCE_ID_GETTERS, "getInstanceId");
    }

    private static String getUid(Object modelObj) {
        return getModelProperty(modelObj, UID_GETTERS, "getUid");
    }

    private static String getMetaId(Object modelObj) {
        return getModelProperty(modelObj, META_ID_GETTERS, "getMetaId");
    }

    private static String getModelProperty(Object modelObj, ConcurrentHashMap<Class<?>, MethodHandle> cache,
                                           String methodName) {
        if (modelObj == null) {
            return null;
        }
        try {
            Object result = invokeGetter(modelObj, cache, methodName);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Invokes a cached getter using MethodHandles for optimal performance.
     * Falls back to standard reflection if MethodHandle creation fails.
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
            // MethodHandle.invoke declares throws Throwable; wrap unexpected errors
            if (t instanceof Error) {
                throw (Error) t;
            }
            return null;
        }
    }

    private static Method findIdGetter(Class<?> joinClass) {
        return ID_GETTER_CACHE.computeIfAbsent(joinClass, clazz -> {
            for (Method m : clazz.getMethods()) {
                if ("getId".equals(m.getName()) && m.getParameterCount() == 0) {
                    return m;
                }
            }
            return null;
        });
    }

    private static List<Method> getStringSetters(Class<?> idClass) {
        return STRING_SETTERS_CACHE.computeIfAbsent(idClass, clazz -> {
            List<Method> setters = new ArrayList<>();
            for (Method m : clazz.getMethods()) {
                if (m.getName().startsWith("set") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0] == String.class) {
                    setters.add(m);
                }
            }
            setters.sort(Comparator.comparing(Method::getName));
            return setters;
        });
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Explicitly cleans up ThreadLocal state to prevent memory leaks in thread-pooled environments.
     * Should be called in a finally block at the top-level entry point of operations
     * that use cascade functionality.
     */
    public static void cleanupThreadLocals() {
        cascadeInProgress.remove();
        cascadeCreatedVersions.remove();
    }
}