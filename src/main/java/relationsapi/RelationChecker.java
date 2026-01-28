package relationsapi;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import utilities.OperationWebserviceInDistributionSingleton;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * RelationChecker - Validates and creates entity relations.
 *
 * Handles cycle detection and ensures proper relation creation between entities.
 *
 * REFERENCE_ENTITIES are shared entities that should use PUBLISHED versions
 * when creating relations, unless the editor is "ingestor".
 */
public class RelationChecker {

    private static final Logger LOG = Logger.getLogger(RelationChecker.class.getName());

    private static final ThreadLocal<Set<String>> processingEntities =
            ThreadLocal.withInitial(HashSet::new);

    /**
     * REFERENCE_ENTITIES: Shared entities that should use existing PUBLISHED versions
     * instead of creating new versions during cascade.
     *
     * FIX: Added CATEGORYSCHEME to the list of reference entities.
     */
    private static final Set<String> REFERENCE_ENTITIES = Set.of(
            EntityNames.ATTRIBUTION.name(),
            EntityNames.PERSON.name(),
            EntityNames.MAPPING.name(),
            EntityNames.CATEGORY.name(),
            EntityNames.CATEGORYSCHEME.name(),  // FIX: Added CATEGORYSCHEME
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

    /**
     * Special reference entities that are SHARED and should always use
     * the PUBLISHED version (unless ingestor mode).
     */
    private static final Set<String> SHARED_REFERENCE_ENTITIES = Set.of(
            EntityNames.CATEGORY.name(),
            EntityNames.CATEGORYSCHEME.name(),
            EntityNames.ORGANIZATION.name(),
            EntityNames.PERSON.name(),
            EntityNames.CONTACTPOINT.name()
    );

    /**
     * Checks if an entity type is a shared reference entity.
     */
    private static boolean isSharedReferenceEntity(String entityType) {
        if (entityType == null) return false;
        return SHARED_REFERENCE_ENTITIES.contains(entityType.toUpperCase());
    }

    /**
     * Determines if REFERENCE_ENTITY logic should be applied.
     * Returns true if the entity is a shared reference entity AND
     * the editor is not "ingestor".
     */
    private static boolean shouldUsePublishedVersion(String entityType, EPOSDataModelEntity mainEntity) {
        if (!isSharedReferenceEntity(entityType)) {
            return false;
        }

        // Ingestor mode: don't use special logic, cascade normally
        if (mainEntity != null) {
            String editorId = mainEntity.getEditorId();
            if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) {
                return false;
            }
        }

        return true;
    }

    private static String getEntityKey(LinkedEntity linkedEntity) {
        String base = linkedEntity.getEntityType() + ":";

        if (linkedEntity.getUid() != null && !linkedEntity.getUid().trim().isEmpty()) {
            return base + linkedEntity.getUid();
        }

        if (linkedEntity.getMetaId() != null && !linkedEntity.getMetaId().trim().isEmpty()) {
            return base + linkedEntity.getMetaId();
        }

        return base + linkedEntity.getInstanceId();
    }

    public static Object checkRelation(EPOSDataModelEntity mainEntity,
                                       EPOSDataModelEntity oldMainEntity,
                                       Class mainEntityClazz,
                                       LinkedEntity linkedEntity,
                                       StatusType overrideStatus,
                                       Class clazz,
                                       Boolean enableStore) {

        String entityKey = getEntityKey(linkedEntity);
        Set<String> processing = processingEntities.get();

        boolean isInCycle = processing.contains(entityKey);

        if (isInCycle) {
            return handleCycleCase(linkedEntity, clazz);
        }

        try {
            processing.add(entityKey);
            return processRelation(mainEntity, oldMainEntity, mainEntityClazz,
                    linkedEntity, overrideStatus, clazz, enableStore);
        } finally {
            processing.remove(entityKey);
            if (processing.isEmpty()) {
                processingEntities.remove();
            }
        }
    }

    private static Object handleCycleCase(LinkedEntity linkedEntity, Class clazz) {
        List<Object> results = EposDataModelDAO.getInstance()
                .getOneFromDBByLinkedEntity(linkedEntity, clazz);
        return results.isEmpty() ? null : results.get(0);
    }

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

        LinkedEntity newLinkedEntityMainEntity = mainEntity == null ? null :
                AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName()
                                .toUpperCase(Locale.ROOT)).name())
                        .retrieveLinkedEntity(mainEntity.getInstanceId());

        LinkedEntity oldLinkedEntityMainEntity = oldMainEntity == null ? null :
                AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName()
                                .toUpperCase(Locale.ROOT)).name())
                        .retrieveLinkedEntity(oldMainEntity.getInstanceId());

        EPOSDataModelEntity relationEntity = (EPOSDataModelEntity)
                LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);

        LinkedEntity obj = null;

        // FIX: For shared reference entities, try to find PUBLISHED version first
        if (shouldUsePublishedVersion(linkedEntity.getEntityType(), mainEntity)) {
            Object publishedVersion = findPublishedVersionOfEntity(linkedEntity, clazz);
            if (publishedVersion != null) {
                obj = createLinkedEntityFromModel(publishedVersion, linkedEntity.getEntityType());
                LOG.fine("[RelationChecker] REFERENCE_ENTITY: Found PUBLISHED version for " +
                        linkedEntity.getEntityType() + " uid=" + linkedEntity.getUid());

                // Return the published version directly
                return publishedVersion;
            }
        }

        if (relationEntity != null) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus :
                    (mainEntity != null ? mainEntity.getStatus() : null);

            if (targetStatus != null) {
                List<Object> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(relationEntity.getUid(), clazz);

                if (allVersions.isEmpty()) {
                    allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(relationEntity.getUid(), clazz);
                }

                // For shared reference entities, prefer PUBLISHED version
                if (shouldUsePublishedVersion(linkedEntity.getEntityType(), mainEntity)) {
                    for (Object v : allVersions) {
                        String statusStr = getModelVersionStatus(v);
                        if (statusStr != null && statusStr.equals(StatusType.PUBLISHED.toString())) {
                            try {
                                obj = new LinkedEntity();
                                obj.setInstanceId(getModelStrProperty(v, "getInstanceId"));
                                obj.setMetaId(getModelStrProperty(v, "getMetaId"));
                                obj.setUid(getModelStrProperty(v, "getUid"));
                                obj.setEntityType(linkedEntity.getEntityType());

                                // Return the published version
                                return v;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                // Normal behavior: find matching status
                for (Object v : allVersions) {
                    String statusStr = getModelVersionStatus(v);
                    if (statusStr != null && statusStr.equals(targetStatus.toString())) {
                        try {
                            obj = new LinkedEntity();
                            obj.setInstanceId(getModelStrProperty(v, "getInstanceId"));
                            obj.setMetaId(getModelStrProperty(v, "getMetaId"));
                            obj.setUid(getModelStrProperty(v, "getUid"));
                            obj.setEntityType(linkedEntity.getEntityType());
                            break;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        if (obj == null) {
            if (relationEntity != null && newLinkedEntityMainEntity != null) {
                boolean statusMismatch = mainEntity.getStatus() != null
                        && relationEntity.getStatus() != null
                        && !mainEntity.getStatus().equals(relationEntity.getStatus());

                boolean isReference = linkedEntity.getEntityType() != null &&
                        REFERENCE_ENTITIES.contains(linkedEntity.getEntityType().toUpperCase());

                // FIX: For shared reference entities, don't propagate status
                boolean isSharedReference = isSharedReferenceEntity(linkedEntity.getEntityType());

                if (statusMismatch && !isReference && !isSharedReference) {
                    relationEntity.setStatus(mainEntity.getStatus());

                    if (Boolean.TRUE.equals(enableStore)) {
                        obj = OperationWebserviceInDistributionSingleton.getInstance()
                                .getTarget(linkedEntity);
                    }

                    if (obj == null) {
                        obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType()
                                        .toUpperCase(Locale.ROOT)).name())
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

                if (results.isEmpty() && linkedEntity.getInstanceId() != null) {
                    results = EposDataModelDAO.getInstance().getOneFromDBByInstanceIdNoCache(linkedEntity.getInstanceId(), clazz);
                }

                if (!results.isEmpty()) {
                    // FIX: For shared reference entities, prefer PUBLISHED version from results
                    if (shouldUsePublishedVersion(linkedEntity.getEntityType(), mainEntity)) {
                        for (Object result : results) {
                            String status = getModelVersionStatus(result);
                            if (StatusType.PUBLISHED.toString().equals(status)) {
                                obj = createLinkedEntityFromModel(result, linkedEntity.getEntityType());
                                return result;
                            }
                        }
                    }
                    obj = linkedEntity;
                } else {
                    if (linkedEntity.getUid() != null) {
                        List<Object> byUid = EposDataModelDAO.getInstance()
                                .getOneFromDBByUIDNoCache(linkedEntity.getUid(), clazz);

                        if (byUid.isEmpty()) {
                            byUid = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(linkedEntity.getUid(), clazz);
                        }

                        if (!byUid.isEmpty()) {
                            // FIX: For shared reference entities, prefer PUBLISHED
                            StatusType preferredStatus = shouldUsePublishedVersion(linkedEntity.getEntityType(), mainEntity)
                                    ? StatusType.PUBLISHED
                                    : (overrideStatus != null ? overrideStatus : (mainEntity != null ? mainEntity.getStatus() : null));

                            Object existing = findBestMatchingVersion(byUid, preferredStatus);

                            if (existing != null) {
                                obj = new LinkedEntity();
                                obj.setInstanceId(getModelStrProperty(existing, "getInstanceId"));
                                obj.setMetaId(getModelStrProperty(existing, "getMetaId"));
                                obj.setUid(getModelStrProperty(existing, "getUid"));
                                obj.setEntityType(linkedEntity.getEntityType());
                            }
                        }
                    }

                    if (obj == null) {
                        if (Boolean.TRUE.equals(enableStore) && linkedEntity.getEntityType() != null) {
                            try {
                                String apiName = EntityNames.valueOf(linkedEntity.getEntityType().toUpperCase(Locale.ROOT)).name();
                                AbstractAPI api = AbstractAPI.retrieveAPI(apiName);

                                if (api != null) {
                                    Class<?> dtoClass = null;
                                    for (Method m : api.getClass().getMethods()) {
                                        if (m.getName().equals("create") && m.getParameterCount() == 4) {
                                            Class<?> paramType = m.getParameterTypes()[0];
                                            if (org.epos.eposdatamodel.EPOSDataModelEntity.class.isAssignableFrom(paramType)) {
                                                dtoClass = paramType;
                                                break;
                                            }
                                        }
                                    }

                                    if (dtoClass != null) {
                                        org.epos.eposdatamodel.EPOSDataModelEntity newDto =
                                                (org.epos.eposdatamodel.EPOSDataModelEntity) dtoClass.getDeclaredConstructor().newInstance();
                                        newDto.setUid(linkedEntity.getUid());
                                        newDto.setInstanceId(UUID.randomUUID().toString());
                                        newDto.setMetaId(UUID.randomUUID().toString());

                                        // FIX: For shared reference entities, always create as PUBLISHED
                                        StatusType createStatus;
                                        if (shouldUsePublishedVersion(linkedEntity.getEntityType(), mainEntity)) {
                                            createStatus = StatusType.PUBLISHED;
                                            LOG.fine("[RelationChecker] REFERENCE_ENTITY: Creating " +
                                                    linkedEntity.getEntityType() + " as PUBLISHED");
                                        } else {
                                            createStatus = overrideStatus != null ? overrideStatus :
                                                    (mainEntity != null ? mainEntity.getStatus() : StatusType.DRAFT);
                                        }
                                        newDto.setStatus(createStatus);

                                        LinkedEntity createdLe = api.create(newDto, createStatus, null, null);
                                        if (createdLe != null) {
                                            obj = createdLe;
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                LOG.warning("Failed to implicitly create stub entity: " + e.getMessage());
                            }
                        }
                    }
                }
            }
        }

        if (obj == null) return null;

        List<Object> results = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(obj, clazz);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Finds the PUBLISHED version of an entity by LinkedEntity.
     */
    private static Object findPublishedVersionOfEntity(LinkedEntity linkedEntity, Class clazz) {
        String uid = linkedEntity.getUid();
        if (uid != null) {
            List<Object> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, clazz);
            for (Object v : allVersions) {
                String status = getModelVersionStatus(v);
                if (StatusType.PUBLISHED.toString().equals(status)) {
                    return v;
                }
            }
        }

        // Try by instanceId
        if (linkedEntity.getInstanceId() != null) {
            List<Object> byInstanceId = EposDataModelDAO.getInstance().getOneFromDBByInstanceIdNoCache(linkedEntity.getInstanceId(), clazz);
            if (!byInstanceId.isEmpty()) {
                Object found = byInstanceId.get(0);
                // Get the UID and search for PUBLISHED
                String foundUid = getModelStrProperty(found, "getUid");
                if (foundUid != null) {
                    List<Object> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(foundUid, clazz);
                    for (Object v : allVersions) {
                        String status = getModelVersionStatus(v);
                        if (StatusType.PUBLISHED.toString().equals(status)) {
                            return v;
                        }
                    }
                }
            }
        }

        return null;
    }

    /**
     * Creates a LinkedEntity from a model object.
     */
    private static LinkedEntity createLinkedEntityFromModel(Object model, String entityType) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(getModelStrProperty(model, "getInstanceId"));
        le.setMetaId(getModelStrProperty(model, "getMetaId"));
        le.setUid(getModelStrProperty(model, "getUid"));
        le.setEntityType(entityType);
        return le;
    }

    private static Object findBestMatchingVersion(List<Object> versions, StatusType targetStatus) {
        if (versions == null || versions.isEmpty()) return null;
        if (targetStatus == null) return versions.get(0);

        for (Object v : versions) {
            String status = getModelVersionStatus(v);
            if (status != null && status.equals(targetStatus.toString())) return v;
        }
        return versions.get(0);
    }

    private static String getModelVersionStatus(Object modelEntity) {
        try {
            Method getVersion = modelEntity.getClass().getMethod("getVersion");
            Object versionObj = getVersion.invoke(modelEntity);
            if (versionObj != null) {
                Method getStatus = versionObj.getClass().getMethod("getStatus");
                Object statusObj = getStatus.invoke(versionObj);
                return statusObj != null ? statusObj.toString() : null;
            }
        } catch (Exception e) { }
        return null;
    }

    private static String getModelStrProperty(Object modelEntity, String methodName) {
        try {
            Method method = modelEntity.getClass().getMethod(methodName);
            Object result = method.invoke(modelEntity);
            return result != null ? result.toString() : null;
        } catch (Exception e) { return null; }
    }
}