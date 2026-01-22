package relationsapi;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import commonapis.VersioningStatusAPI;
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

public class RelationChecker {


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
            return handleCycleCase(mainEntity, linkedEntity, overrideStatus, clazz, enableStore);
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

    private static Object handleCycleCase(EPOSDataModelEntity mainEntity,
                                          LinkedEntity linkedEntity,
                                          StatusType overrideStatus,
                                          Class clazz,
                                          Boolean enableStore) {

        EPOSDataModelEntity relationEntity = (EPOSDataModelEntity)
                LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);

        LinkedEntity obj = linkedEntity;

        if (relationEntity != null) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus :
                    (mainEntity != null ? mainEntity.getStatus() : null);

            if (targetStatus != null) {
                List<Object> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUID(relationEntity.getUid(), clazz);
                for (Object v : allVersions) {
                    String statusStr = getModelVersionStatus(v);
                    if (statusStr != null && statusStr.equals(targetStatus.toString())) {
                        return v;
                    }
                }
            }
        }

        if (relationEntity != null && mainEntity != null) {
            boolean needsStatusUpdate = mainEntity.getStatus() != null
                    && relationEntity.getStatus() != null
                    && !mainEntity.getStatus().equals(relationEntity.getStatus());

            if (needsStatusUpdate) {
                relationEntity.setStatus(mainEntity.getStatus());

                if (enableStore) {
                    obj = OperationWebserviceInDistributionSingleton.getInstance()
                            .getTarget(linkedEntity);
                }

                if (obj == null || obj.equals(linkedEntity)) {
                    obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType()
                                    .toUpperCase(Locale.ROOT)).name())
                            .create(relationEntity, overrideStatus, null, null);

                    if (enableStore && obj != null) {
                        OperationWebserviceInDistributionSingleton.getInstance()
                                .createRelation(linkedEntity, obj);
                    }
                }
            }
        }

        List<Object> results = EposDataModelDAO.getInstance()
                .getOneFromDBByLinkedEntity(obj, clazz);
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

        // Check if a version with the desired status ALREADY exists to avoid overwriting/duplicating.
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
            if (relationEntity != null && newLinkedEntityMainEntity != null && oldLinkedEntityMainEntity != null) {
                boolean statusMismatch = mainEntity.getStatus() != null
                        && relationEntity.getStatus() != null
                        && !mainEntity.getStatus().equals(relationEntity.getStatus());

                if (statusMismatch) {
                    relationEntity.setStatus(mainEntity.getStatus());

                    if (enableStore) {
                        obj = OperationWebserviceInDistributionSingleton.getInstance()
                                .getTarget(linkedEntity);
                    }

                    if (obj == null) {
                        obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType()
                                        .toUpperCase(Locale.ROOT)).name())
                                .create(relationEntity, overrideStatus, oldLinkedEntityMainEntity, newLinkedEntityMainEntity);

                        if (enableStore) {
                            OperationWebserviceInDistributionSingleton.getInstance()
                                    .createRelation(linkedEntity, obj);
                        }
                    }
                } else {
                    obj = linkedEntity;
                }

            } else if (relationEntity != null && newLinkedEntityMainEntity != null) {
                boolean statusMismatch = mainEntity.getStatus() != null
                        && relationEntity.getStatus() != null
                        && !mainEntity.getStatus().equals(relationEntity.getStatus());

                if (statusMismatch) {
                    relationEntity.setStatus(mainEntity.getStatus());

                    if (enableStore) {
                        obj = OperationWebserviceInDistributionSingleton.getInstance()
                                .getTarget(linkedEntity);
                    }

                    if (obj == null) {
                        obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType()
                                        .toUpperCase(Locale.ROOT)).name())
                                .create(relationEntity, overrideStatus, null, newLinkedEntityMainEntity);

                        if (enableStore) {
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
                    obj = LinkedEntityAPI.createFromLinkedEntity(
                            linkedEntity,
                            mainEntity.getStatus(),
                            VersioningStatusAPI.retrieveVersioningStatus(mainEntity),
                            mainEntity.getFileProvenance()
                    );
                }
            }
        }

        List<Object> results = EposDataModelDAO.getInstance()
                .getOneFromDBByLinkedEntity(obj, clazz);
        return results.isEmpty() ? null : results.get(0);
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
        } catch (Exception e) {
            return null;
        }
    }
}