package relationsapi;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.StatusType;
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

public class RelationChecker {

    private static final Logger LOG = Logger.getLogger(RelationChecker.class.getName());

    private static final ThreadLocal<Set<String>> processingEntities =
            ThreadLocal.withInitial(HashSet::new);

    private static String getEntityKey(LinkedEntity linkedEntity) {
        return linkedEntity.getEntityType() + ":" +
                linkedEntity.getInstanceId() + ":" +
                linkedEntity.getMetaId();
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
            // FIX: Remove recursive create call to prevent StackOverflow
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
        // Simply try to retrieve existing state from DB/Cache
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

        if (relationEntity != null) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus :
                    (mainEntity != null ? mainEntity.getStatus() : null);

            if (targetStatus != null) {
                List<Object> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUID(relationEntity.getUid(), clazz);
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

                if (statusMismatch) {
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

                if (!results.isEmpty()) {
                    obj = linkedEntity;
                } else {
                    if (linkedEntity.getUid() != null) {
                        List<Object> byUid = EposDataModelDAO.getInstance()
                                .getOneFromDBByUID(linkedEntity.getUid(), clazz);

                        if (!byUid.isEmpty()) {
                            Object existing = findBestMatchingVersion(byUid,
                                    overrideStatus != null ? overrideStatus :
                                            (mainEntity != null ? mainEntity.getStatus() : null));

                            if (existing != null) {
                                LOG.fine("Found existing entity by UID: " + linkedEntity.getUid());
                                obj = new LinkedEntity();
                                obj.setInstanceId(getModelStrProperty(existing, "getInstanceId"));
                                obj.setMetaId(getModelStrProperty(existing, "getMetaId"));
                                obj.setUid(getModelStrProperty(existing, "getUid"));
                                obj.setEntityType(linkedEntity.getEntityType());
                            }
                        }
                    }

                    // IMPLICIT STUB CREATION (Robust Version)
                    if (obj == null) {
                        if (Boolean.TRUE.equals(enableStore) && linkedEntity.getEntityType() != null) {
                            try {
                                //LOG.info("Implicitly creating stub entity for UID: " + linkedEntity.getUid());

                                String apiName = EntityNames.valueOf(linkedEntity.getEntityType().toUpperCase(Locale.ROOT)).name();
                                AbstractAPI api = AbstractAPI.retrieveAPI(apiName);

                                if (api != null) {
                                    // Use Reflection to find the DTO class accepted by api.create()
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
                                        newDto.setStatus(overrideStatus != null ? overrideStatus :
                                                (mainEntity != null ? mainEntity.getStatus() : StatusType.DRAFT));

                                        LinkedEntity createdLe = api.create(newDto, overrideStatus, null, null);
                                        if (createdLe != null) {
                                            obj = createdLe;
                                        }
                                    } else {
                                        LOG.warning("Could not determine DTO class for API: " + apiName);
                                    }
                                }
                            } catch (Exception e) {
                                LOG.warning("Failed to implicitly create stub entity: " + e.getMessage());
                            }
                        }

                        if (obj == null) {
                            //LOG.info("Entity not found for relation: " + linkedEntity.getEntityType() +
                            //        " uid=" + linkedEntity.getUid() + " - returning null (will be deferred)");
                            return null;
                        }
                    }
                }
            }
        }

        if (obj == null) return null;

        List<Object> results = EposDataModelDAO.getInstance()
                .getOneFromDBByLinkedEntity(obj, clazz);
        return results.isEmpty() ? null : results.get(0);
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