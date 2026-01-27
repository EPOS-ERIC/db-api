package relationsapi;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.LinkedEntity;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

public class RelationSyncUtil {

    private static final Set<String> REFERENCE_ENTITIES = Set.of(
            "CATEGORY",
            "ORGANIZATION",
            "PERSON",
            "CONTACTPOINT",
            // Model class names (lowercase per match case-insensitive)
            "category",
            "organization",
            "person",
            "contactpoint"
    );


    private static boolean isReferenceEntity(Class<?> targetClass) {
        if (targetClass == null) return false;
        String className = targetClass.getSimpleName().toUpperCase();
        return REFERENCE_ENTITIES.contains(className);
    }

    public static <P, C> void propagateStatusToChildren(
            P parentEntity, String parentInstanceId, Class<C> childClass, String foreignKeyFieldName
    ) {
        StatusType parentStatus = getStatusFromEntity(parentEntity);
        if (parentStatus == null) {
            return;
        }

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        if (rawObjects == null || rawObjects.isEmpty()) {
            return;
        }

        for (Object obj : rawObjects) {
            if (childClass.isInstance(obj)) {
                updateChildEntityStatus(obj, parentStatus);
            }
        }
    }

    public static <C> void debugChildStatuses(String parentInstanceId, Class<C> childClass, String foreignKeyFieldName) {

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        if (rawObjects == null || rawObjects.isEmpty()) {
            return;
        }

        for (Object obj : rawObjects) {
            try {
                String instanceId = "?";
                String status = "?";
                String value = "?";

                try {
                    Method getInstanceId = obj.getClass().getMethod("getInstanceId");
                    instanceId = String.valueOf(getInstanceId.invoke(obj));
                } catch (Exception ignored) {}

                try {
                    Method getVersion = obj.getClass().getMethod("getVersion");
                    Object vs = getVersion.invoke(obj);
                    if (vs instanceof Versioningstatus) {
                        status = ((Versioningstatus) vs).getStatus();
                    }
                } catch (Exception ignored) {
                    // Try to get from DB
                    try {
                        List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                                .getOneFromDBByInstanceId(instanceId, Versioningstatus.class);
                        if (vsList != null && !vsList.isEmpty()) {
                            status = vsList.get(0).getStatus();
                        }
                    } catch (Exception e2) {}
                }

                // Try to get a value field (title, description, etc.)
                for (Method m : obj.getClass().getMethods()) {
                    if (m.getName().startsWith("get") && m.getParameterCount() == 0
                            && m.getReturnType() == String.class
                            && !m.getName().equals("getInstanceId")
                            && !m.getName().equals("getClass")) {
                        try {
                            Object v = m.invoke(obj);
                            if (v != null && !v.toString().isEmpty()) {
                                value = v.toString();
                                if (value.length() > 50) value = value.substring(0, 50) + "...";
                                break;
                            }
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception e) {
                System.err.println("    - Error reading entity: " + e.getMessage());
            }
        }
    }

    public static <P, C> void syncSimpleOneToMany(
            P parentEntity, String parentInstanceId, List<String> newValues, Class<C> childClass,
            String foreignKeyFieldName, String uidPrefix,
            Function<C, String> valueGetter, BiConsumer<C, String> valueSetter, BiConsumer<C, P> parentSetter
    ) {
        if (newValues == null) newValues = Collections.emptyList();
        Set<String> newValuesSet = new HashSet<>(newValues);
        newValuesSet.remove(null);

        StatusType parentStatus = getStatusFromEntity(parentEntity);

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        List<C> existingEntities = new ArrayList<>();
        if (rawObjects != null) {
            for (Object obj : rawObjects) {
                if (childClass.isInstance(obj)) existingEntities.add(childClass.cast(obj));
            }
        }

        Map<String, C> existingMap = existingEntities.stream()
                .collect(Collectors.toMap(valueGetter, Function.identity(), (a, b) -> a));

        for (C existing : existingEntities) {
            String val = valueGetter.apply(existing);
            if (val != null && !newValuesSet.contains(val)) {
                EposDataModelDAO.getInstance().deleteObject(existing);
            }
        }

        for (String newValue : newValuesSet) {
            if (!existingMap.containsKey(newValue)) {
                try {
                    C newEntity = childClass.getDeclaredConstructor().newInstance();
                    setStandardFields(newEntity, uidPrefix, parentStatus);
                    valueSetter.accept(newEntity, newValue);
                    parentSetter.accept(newEntity, parentEntity);
                    EposDataModelDAO.getInstance().updateObject(newEntity);
                } catch (Exception e) {
                    throw new RuntimeException("Error syncing relation for " + childClass.getSimpleName(), e);
                }
            } else {
                C existingEntity = existingMap.get(newValue);
                updateChildEntityStatus(existingEntity, parentStatus);
            }
        }
    }

    private static void updateChildEntityStatus(Object childEntity, StatusType newStatus) {
        if (childEntity == null || newStatus == null) return;

        String childClassName = childEntity.getClass().getSimpleName();

        try {
            Method getVersion = childEntity.getClass().getMethod("getVersion");
            Object versionObj = getVersion.invoke(childEntity);

            if (versionObj instanceof Versioningstatus) {
                Versioningstatus vs = (Versioningstatus) versionObj;
                String currentStatus = vs.getStatus();

                if (!newStatus.name().equals(currentStatus)) {

                    if (newStatus == StatusType.PUBLISHED && vs.getUid() != null) {
                        archiveOldPublishedVersionsForChild(vs.getUid(), vs.getVersionId(), childClassName);
                    }

                    vs.setStatus(newStatus.name());
                    vs.setChangeTimestamp(OffsetDateTime.now());
                    EposDataModelDAO.getInstance().updateObject(vs);
                } else {
                }
            } else {
                updateChildStatusByInstanceId(childEntity, newStatus, childClassName);
            }
        } catch (NoSuchMethodException e) {
            updateChildStatusByInstanceId(childEntity, newStatus, childClassName);
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error updating " + childClassName + " status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void archiveOldPublishedVersionsForChild(String uid, String currentVersionId, String childClassName) {
        if (uid == null) return;

        try {
            List<Versioningstatus> allVersionsRaw = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(uid, Versioningstatus.class);

            for (Versioningstatus rawVs : allVersionsRaw) {
                if (rawVs.getVersionId() != null && rawVs.getVersionId().equals(currentVersionId)) continue;

                String metaId = rawVs.getMetaId();
                if (metaId != null && metaId.contains(".")) continue;
                if (StatusType.PENDING.name().equals(rawVs.getStatus())) continue;

                if (StatusType.PUBLISHED.name().equals(rawVs.getStatus())) {
                    rawVs.setStatus(StatusType.ARCHIVED.name());
                    rawVs.setChangeTimestamp(OffsetDateTime.now());
                    rawVs.setChangeComment("Auto-archived on child status propagation");
                    EposDataModelDAO.getInstance().updateObject(rawVs);
                }
            }
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error archiving old versions for " + childClassName + ": " + e.getMessage());
        }
    }

    private static void updateChildStatusByInstanceId(Object childEntity, StatusType newStatus, String childClassName) {
        try {
            Method getInstanceId = childEntity.getClass().getMethod("getInstanceId");
            Object instanceIdObj = getInstanceId.invoke(childEntity);

            if (instanceIdObj != null) {
                String instanceId = instanceIdObj.toString();
                List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                        .getOneFromDBByInstanceId(instanceId, Versioningstatus.class);

                if (vsList != null && !vsList.isEmpty()) {
                    Versioningstatus vs = vsList.get(0);
                    String currentStatus = vs.getStatus();

                    if (!newStatus.name().equals(currentStatus)) {

                        if (newStatus == StatusType.PUBLISHED && vs.getUid() != null) {
                            archiveOldPublishedVersionsForChild(vs.getUid(), vs.getVersionId(), childClassName);
                        }

                        vs.setStatus(newStatus.name());
                        vs.setChangeTimestamp(OffsetDateTime.now());
                        EposDataModelDAO.getInstance().updateObject(vs);
                    }
                } else {
                    System.err.println("[RelationSyncUtil] No Versioningstatus found for " +
                            childClassName + " instanceId=" + instanceId);
                }
            }
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error updating " + childClassName +
                    " status by instanceId: " + e.getMessage());
        }
    }

    public static <P, C> void copySimpleOneToMany(
            String oldParentInstanceId, P newParentEntity, String newParentInstanceId, Class<C> childClass,
            String foreignKeyFieldName, String uidPrefix, Function<C, String> valueGetter,
            BiConsumer<C, String> valueSetter, BiConsumer<C, P> parentSetter
    ) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, oldParentInstanceId, childClass);
        if (oldRelations == null || oldRelations.isEmpty()) return;

        StatusType parentStatus = getStatusFromEntity(newParentEntity);

        for (Object obj : oldRelations) {
            C oldEntity = childClass.cast(obj);
            String value = valueGetter.apply(oldEntity);
            try {
                C newEntity = childClass.getDeclaredConstructor().newInstance();
                setStandardFields(newEntity, uidPrefix, parentStatus);  // FIX: Passa lo status del parent
                valueSetter.accept(newEntity, value);
                parentSetter.accept(newEntity, newParentEntity);
                EposDataModelDAO.getInstance().updateObject(newEntity);
            } catch (Exception e) {
                System.err.println("[RelationSyncUtil] Error copying simple relation: " + e.getMessage());
            }
        }
    }

    private static void setStandardFields(Object entity, String uidPrefix) {
        setStandardFields(entity, uidPrefix, null);
    }

    private static void setStandardFields(Object entity, String uidPrefix, StatusType parentStatus) {
        try {
            invokeSetter(entity, "setInstanceId", UUID.randomUUID().toString());
            invokeSetter(entity, "setMetaId", UUID.randomUUID().toString());
            invokeSetter(entity, "setUid", (uidPrefix != null ? uidPrefix + "/" : "") + UUID.randomUUID().toString());

            try {
                Method setVersion = entity.getClass().getMethod("setVersion", model.Versioningstatus.class);
                Versioningstatus vs = new Versioningstatus();

                vs.setVersionId(UUID.randomUUID().toString());
                vs.setInstanceId(UUID.randomUUID().toString());
                vs.setUid((uidPrefix != null ? uidPrefix + "/" : "") + UUID.randomUUID().toString());
                StatusType statusToUse = parentStatus != null ? parentStatus : StatusType.DRAFT;
                vs.setStatus(statusToUse.name());
                vs.setChangeTimestamp(java.time.OffsetDateTime.now());
                vs.setMetaId(entity.getClass().getSimpleName());

                setVersion.invoke(entity, vs);
            } catch (NoSuchMethodException ignored) {}

            for (Method m : entity.getClass().getMethods()) {
                if (m.getName().startsWith("set") && m.getParameterCount() == 1
                        && m.getParameterTypes()[0].equals(List.class)) {
                    try {
                        m.invoke(entity, new ArrayList<>());
                    } catch (Exception ignored) {}
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Cannot set standard fields on " + entity.getClass().getName(), e);
        }

        for (java.lang.reflect.Method m : entity.getClass().getMethods()) {
            if (m.getName().startsWith("set") && m.getParameterCount() == 1
                    && m.getParameterTypes()[0].equals(List.class)) {
                try {
                    m.invoke(entity, new ArrayList<>());
                } catch (Exception ignored) {}
            }
        }
    }

    private static StatusType getStatusFromEntity(Object entity) {
        if (entity == null) return null;
        try {
            Method getStatus = entity.getClass().getMethod("getStatus");
            Object statusObj = getStatus.invoke(entity);

            if (statusObj instanceof StatusType) {
                return (StatusType) statusObj;
            } else if (statusObj instanceof model.StatusType) {
                return (model.StatusType) statusObj;
            } else if (statusObj != null) {
                try {
                    return StatusType.valueOf(statusObj.toString());
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
        }

        try {
            Method getVersion = entity.getClass().getMethod("getVersion");
            Object versionObj = getVersion.invoke(entity);

            if (versionObj instanceof Versioningstatus) {
                Versioningstatus vs = (Versioningstatus) versionObj;
                String statusStr = vs.getStatus();
                if (statusStr != null) {
                    try {
                        return StatusType.valueOf(statusStr);
                    } catch (IllegalArgumentException e) {
                    }
                }
            }
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error getting status from entity: " + e.getMessage());
        }

        return null;
    }

    private static void invokeSetter(Object obj, String methodName, String value) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, value);
        } catch (Exception ignored) { }
    }

    public static <P, J, T> void syncComplexRelation(
            P parentDbObject, String parentId, List<LinkedEntity> inputLinks,
            LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate,
            Class<J> joinClass, Class<T> targetClass, String parentFieldName,
            Function<J, T> targetGetter, BiConsumer<J, P> parentSetter, BiConsumer<J, T> targetSetter,
            org.epos.eposdatamodel.EPOSDataModelEntity mainEntity,
            org.epos.eposdatamodel.EPOSDataModelEntity previousEntity,
            model.StatusType overrideStatus, boolean enableStore
    ) {
        String previousInstanceId = mainEntity.getInstanceChangedId();

        boolean hasInstanceChanged = previousInstanceId != null && !previousInstanceId.equals(parentId);
        StatusType currentStatus = mainEntity.getStatus();
        boolean isNewVersion = hasInstanceChanged && currentStatus == StatusType.DRAFT;

        String parentMetaId = null;
        if (isNewVersion) {
            parentMetaId = getMetaId(parentDbObject);
            if (parentMetaId != null) {
                cascadeInProgress.get().add(parentMetaId);
                String cacheKey = parentMetaId + "_" + (overrideStatus != null ? overrideStatus.name() : mainEntity.getStatus().name());
                cascadeCreatedVersions.get().put(cacheKey, parentId);
            }
        }

        try {
            StatusType effectiveStatus = overrideStatus != null ? overrideStatus : mainEntity.getStatus();

            if (inputLinks == null || inputLinks.isEmpty()) {
                if (isNewVersion) {
                    StatusType cascadeStatus = effectiveStatus;
                    copyComplexRelationsFromPreviousVersion(previousInstanceId, parentDbObject, parentId,
                            joinClass, targetClass, parentFieldName, targetGetter, cascadeStatus);
                } else if (effectiveStatus != null) {
                    List<Object> existingRaw = EposDataModelDAO.getInstance()
                            .getOneFromDBBySpecificKeyNoCache(parentFieldName, parentId, joinClass);
                    if (existingRaw != null) {
                        for (Object o : existingRaw) {
                            T target = targetGetter.apply(joinClass.cast(o));
                            if (target != null) {
                                updateChildEntityStatus(target, effectiveStatus);
                            }
                        }
                    }
                }
                return;
            }

            if (relationFromUpdate != null && inputLinks.contains(relationFromUpdate)) {
                inputLinks.remove(relationFromUpdate);
                inputLinks.add(relationToUpdate);
            }

            List<Object> existingRaw = EposDataModelDAO.getInstance().getOneFromDBBySpecificKeyNoCache(parentFieldName, parentId, joinClass);

            Map<String, J> existingMap = new HashMap<>();
            if (existingRaw != null) {
                for (Object o : existingRaw) {
                    J joinEntity = joinClass.cast(o);
                    T target = targetGetter.apply(joinEntity);
                    String targetId = getModelId(target);
                    if (targetId != null) existingMap.put(targetId, joinEntity);
                }
            }

            Set<String> processedIds = new HashSet<>();
            String sourceEntityType = parentDbObject.getClass().getSimpleName().toUpperCase();

            StatusType cascadeStatus = isNewVersion ? effectiveStatus : null;

            for (LinkedEntity link : inputLinks) {
                Object rawTarget = null;

                if (!isNewVersion && link.getInstanceId() != null) {
                    List<Object> directResults = EposDataModelDAO.getInstance()
                            .getOneFromDBByInstanceIdNoCache(link.getInstanceId(), targetClass);
                    if (directResults != null && !directResults.isEmpty()) {
                        rawTarget = directResults.get(0);
                    }
                }

                if (rawTarget == null) {
                    rawTarget = relationsapi.RelationChecker.checkRelation(mainEntity, previousEntity, null, link, effectiveStatus, targetClass, enableStore);
                }

                if (rawTarget != null) {
                    T targetEntity = targetClass.cast(rawTarget);
                    String targetId = getModelId(targetEntity);

                    if (targetId != null) {
                        if (targetId.equals(parentId)) continue;

                        T targetForJoin = targetEntity;
                        String targetIdForJoin = targetId;

                        if (cascadeStatus != null) {
                            if (isReferenceEntity(targetClass)) {
                                T publishedVersion = findPublishedVersion(targetEntity, targetClass);
                                if (publishedVersion != null) {
                                    targetForJoin = publishedVersion;
                                    targetIdForJoin = getModelId(publishedVersion);
                                } else {
                                }
                            } else {
                                T newVersionTarget = createCascadeVersion(targetEntity, targetClass, cascadeStatus);
                                if (newVersionTarget != null) {
                                    targetForJoin = newVersionTarget;
                                    targetIdForJoin = getModelId(newVersionTarget);
                                }
                            }
                        } else if (effectiveStatus != null) {
                            if (isReferenceEntity(targetClass)) {
                            } else {
                                updateChildEntityStatus(targetEntity, effectiveStatus);
                            }
                        }

                        processedIds.add(targetIdForJoin);

                        if (!existingMap.containsKey(targetIdForJoin)) {
                            boolean created = createJoinEntity(joinClass, parentDbObject, targetForJoin, parentSetter, targetSetter);
                            if (!created) {
                                createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
                            }
                        }
                    }
                } else {
                    createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
                }
            }

            for (Map.Entry<String, J> entry : existingMap.entrySet()) {
                if (!processedIds.contains(entry.getKey())) {
                    EposDataModelDAO.getInstance().deleteObject(entry.getValue());
                }
            }

        } finally {
            if (parentMetaId != null) {
                cascadeInProgress.get().remove(parentMetaId);
                if (cascadeInProgress.get().isEmpty()) {
                    cascadeInProgress.remove();
                    cascadeCreatedVersions.remove();
                }
            }
        }
    }

    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(
            String oldParentInstanceId, P newParentDbObject, String newParentId,
            Class<J> joinClass, Class<T> targetClass, String parentFieldName, Function<J, T> targetGetter
    ) {
        copyComplexRelationsFromPreviousVersion(
                oldParentInstanceId, newParentDbObject, newParentId,
                joinClass, targetClass, parentFieldName, targetGetter,
                null  // No status override = just copy references
        );
    }

    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(
            String oldParentInstanceId, P newParentDbObject, String newParentId,
            Class<J> joinClass, Class<T> targetClass, String parentFieldName, Function<J, T> targetGetter,
            StatusType cascadeStatus
    ) {
        List<Object> oldRelations = EposDataModelDAO.getInstance().getOneFromDBBySpecificKeyNoCache(parentFieldName, oldParentInstanceId, joinClass);
        if (oldRelations == null || oldRelations.isEmpty()) {
            try {
                String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
                List<?> embeddedResults = EposDataModelDAO.getInstance().getJoinEntitiesByParentId(embeddedIdField, oldParentInstanceId, joinClass);
                if (embeddedResults != null) oldRelations = new ArrayList<>(embeddedResults);
            } catch (Exception e) {}
        }
        if (oldRelations == null || oldRelations.isEmpty()) return;

        for (Object obj : oldRelations) {
            J oldJoin = joinClass.cast(obj);
            T target = targetGetter.apply(oldJoin);
            String targetId = getModelId(target);

            if (targetId != null) {
                try {
                    T targetForJoin = target;
                    String targetIdForJoin = targetId;

                    if (cascadeStatus != null) {
                        if (isReferenceEntity(targetClass)) {
                            T publishedVersion = findPublishedVersion(target, targetClass);
                            if (publishedVersion != null) {
                                targetForJoin = publishedVersion;
                                targetIdForJoin = getModelId(publishedVersion);
                            } else {
                            }
                        } else {
                            T newVersionTarget = createCascadeVersion(target, targetClass, cascadeStatus);
                            if (newVersionTarget != null) {
                                targetForJoin = newVersionTarget;
                                targetIdForJoin = getModelId(newVersionTarget);
                            }
                        }
                    }

                    J newJoin = joinClass.getDeclaredConstructor().newInstance();
                    initializeEmbeddedId(newJoin, newParentDbObject, targetForJoin);
                    EposDataModelDAO.getInstance().createJoinEntity(newJoin, newParentId, newParentDbObject.getClass(), targetIdForJoin, targetForJoin.getClass());
                } catch (Exception e) {
                    System.err.println("[RelationSyncUtil] Error copying relation: " + e.getMessage());
                }
            }
        }
    }

    private static final ThreadLocal<Set<String>> cascadeInProgress = ThreadLocal.withInitial(HashSet::new);

    private static final ThreadLocal<Map<String, String>> cascadeCreatedVersions = ThreadLocal.withInitial(HashMap::new);

    @SuppressWarnings("unchecked")
    private static <T> T createCascadeVersion(T originalEntity, Class<T> targetClass, StatusType newStatus) {
        String originalInstanceId = getModelId(originalEntity);
        if (originalInstanceId == null) return null;

        String metaId = getMetaId(originalEntity);

        if (metaId == null) metaId = originalInstanceId; // fallback

        String cacheKey = metaId + "_" + newStatus.name();

        Map<String, String> createdVersions = cascadeCreatedVersions.get();
        if (createdVersions.containsKey(cacheKey)) {
            String existingNewInstanceId = createdVersions.get(cacheKey);
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

            String entityName = targetClass.getSimpleName().toUpperCase();

            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) {
                System.err.println("[RelationSyncUtil] CASCADE: No API found for " + entityName);
                return null;
            }

            org.epos.eposdatamodel.EPOSDataModelEntity dto =
                    (org.epos.eposdatamodel.EPOSDataModelEntity) api.retrieve(originalInstanceId);
            if (dto == null) {
                System.err.println("[RelationSyncUtil] CASCADE: Could not retrieve entity " + originalInstanceId);
                return null;
            }

            LinkedEntity newVersionLe = api.create(dto, newStatus, null, null);
            if (newVersionLe == null || newVersionLe.getInstanceId() == null) {
                System.err.println("[RelationSyncUtil] CASCADE: Failed to create new version");
                return null;
            }

            createdVersions.put(cacheKey, newVersionLe.getInstanceId());

            List<Object> newVersionList = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(newVersionLe.getInstanceId(), targetClass);

            if (newVersionList != null && !newVersionList.isEmpty()) {
                return targetClass.cast(newVersionList.get(0));
            }

        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] CASCADE error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            inProgress.remove(metaId);
            if (inProgress.isEmpty()) {
                cascadeInProgress.remove();
                cascadeCreatedVersions.remove(); // Pulisci anche la cache
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T findPublishedVersion(T originalEntity, Class<T> targetClass) {
        if (originalEntity == null) return null;

        String uid = getUid(originalEntity);

        if (uid != null) {
            try {
                List<Object> allVersions = EposDataModelDAO.getInstance()
                        .getOneFromDBByUIDNoCache(uid, targetClass);

                if (allVersions != null && !allVersions.isEmpty()) {
                    T published = findEntityWithStatus(allVersions, StatusType.PUBLISHED, targetClass);
                    if (published != null) return published;
                }
            } catch (Exception e) {
                System.err.println("[RelationSyncUtil] Error finding PUBLISHED by UID: " + e.getMessage());
            }
        }

        // Fallback: prova con metaId (usa versione con cache)
        String metaId = getMetaId(originalEntity);
        if (metaId != null) {
            try {
                List<Object> allVersions = EposDataModelDAO.getInstance()
                        .getOneFromDBByMetaId(metaId, targetClass);
                if (allVersions != null && !allVersions.isEmpty()) {
                    return findEntityWithStatus(allVersions, StatusType.PUBLISHED, targetClass);
                }
            } catch (Exception e) {
                System.err.println("[RelationSyncUtil] Error finding PUBLISHED by metaId: " + e.getMessage());
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T findEntityWithStatus(List<Object> versions, StatusType targetStatus, Class<T> targetClass) {
        if (versions == null || versions.isEmpty()) return null;

        for (Object v : versions) {
            try {
                Method getVersion = v.getClass().getMethod("getVersion");
                Object versionObj = getVersion.invoke(v);

                if (versionObj instanceof Versioningstatus) {
                    Versioningstatus vs = (Versioningstatus) versionObj;
                    if (targetStatus.name().equals(vs.getStatus())) {
                        return targetClass.cast(v);
                    }
                }
            } catch (Exception e) {
                try {
                    String instanceId = getModelId(v);
                    if (instanceId != null) {
                        List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                                .getOneFromDBByInstanceIdNoCache(instanceId, Versioningstatus.class);
                        if (vsList != null && !vsList.isEmpty()) {
                            if (targetStatus.name().equals(vsList.get(0).getStatus())) {
                                return targetClass.cast(v);
                            }
                        }
                    }
                } catch (Exception e2) {
                }
            }
        }
        return null;
    }

    private static String getUid(Object modelObj) {
        if (modelObj == null) return null;
        try {
            Method m = modelObj.getClass().getMethod("getUid");
            Object res = m.invoke(modelObj);
            return res != null ? res.toString() : null;
        } catch (Exception e) { return null; }
    }

    private static String getMetaId(Object modelObj) {
        if (modelObj == null) return null;
        try {
            Method m = modelObj.getClass().getMethod("getMetaId");
            Object res = m.invoke(modelObj);
            return res != null ? res.toString() : null;
        } catch (Exception e) { return null; }
    }

    private static <P, J, T> boolean createJoinEntity(
            Class<J> joinClass, P parentDbObject, T targetEntity,
            BiConsumer<J, P> parentSetter, BiConsumer<J, T> targetSetter
    ) {
        try {
            String parentId = getModelId(parentDbObject);
            String targetId = getModelId(targetEntity);

            if (parentId != null && parentId.equals(targetId)) return true;

            if (joinExists(joinClass, parentDbObject, targetEntity)) {
                return true;
            }

            J newJoin = joinClass.getDeclaredConstructor().newInstance();
            initializeEmbeddedId(newJoin, parentDbObject, targetEntity);

            Boolean result = EposDataModelDAO.getInstance().createJoinEntity(
                    newJoin, parentId, parentDbObject.getClass(), targetId, targetEntity.getClass()
            );
            return result != null && result;
        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") || e.getMessage().contains("already exists"))) {
                return true;
            }
            e.printStackTrace();
            return false;
        }
    }

    private static <J, P, T> boolean joinExists(Class<J> joinClass, P parent, T target) {
        try {
            String parentId = getModelId(parent);
            String targetId = getModelId(target);
            if (parentId == null || targetId == null) return false;

            String parentClassName = parent.getClass().getSimpleName().toLowerCase();
            String parentFieldName = parentClassName + "Instance";

            List<Object> existing = EposDataModelDAO.getInstance()
                    .getOneFromDBBySpecificKeyNoCache(parentFieldName, parentId, joinClass);

            if (existing != null) {
                for (Object obj : existing) {
                    String existingTargetId = getTargetIdFromJoin(obj, target.getClass().getSimpleName());
                    if (targetId.equals(existingTargetId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    private static String getTargetIdFromJoin(Object joinEntity, String targetClassName) {
        try {
            String getterName = "get" + targetClassName + "Instance";
            Method getter = joinEntity.getClass().getMethod(getterName);
            Object target = getter.invoke(joinEntity);
            if (target != null) {
                return getModelId(target);
            }
        } catch (Exception e) {
        }
        return null;
    }

    private static <P, T> void initializeEmbeddedId(Object joinEntity, P parent, T target) {
        try {
            Method getIdMethod = null;
            for (Method m : joinEntity.getClass().getMethods()) {
                if (m.getName().equals("getId") && m.getParameterCount() == 0) {
                    getIdMethod = m;
                    break;
                }
            }
            if (getIdMethod == null) return;

            Class<?> idClass = getIdMethod.getReturnType();
            Object idInstance = idClass.getDeclaredConstructor().newInstance();
            String parentInstanceId = getModelId(parent);
            String targetInstanceId = getModelId(target);
            String parentClassName = parent.getClass().getSimpleName().toLowerCase();
            String targetClassName = target.getClass().getSimpleName().toLowerCase();

            boolean parentSet = false;
            boolean targetSet = false;

            for (Method setter : idClass.getMethods()) {
                if (setter.getName().startsWith("set") && setter.getParameterCount() == 1 && setter.getParameterTypes()[0] == String.class) {
                    String setterNameLower = setter.getName().toLowerCase();
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
            System.err.println("[RelationSyncUtil] Error initializing EmbeddedId: " + e.getMessage());
        }
    }

    private static void createPendingRelation(String sourceInstanceId, String sourceEntityType, String targetUid, String targetEntityType, String joinClassName) {
        try {
            if (pendingRelationExists(sourceInstanceId, targetUid, joinClassName)) {
                return;
            }

            Versioningstatus pending = new Versioningstatus();
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setInstanceId(UUID.randomUUID().toString());
            pending.setUid(targetUid);
            pending.setMetaId(joinClassName);
            pending.setStatus(StatusType.PENDING.name());
            pending.setProvenance(sourceEntityType);
            pending.setChangeComment(targetEntityType);
            pending.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
            pending.setReviewComment(sourceInstanceId);

            EposDataModelDAO.getInstance().createObject(pending);
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error creating pending relation: " + e.getMessage());
        }
    }

    private static boolean pendingRelationExists(String sourceInstanceId, String targetUid, String joinClassName) {
        try {
            List<Versioningstatus> existing = EposDataModelDAO.getInstance()
                    .getOneFromDBBySpecificKeySimpleNoCache("uid", targetUid, Versioningstatus.class);

            if (existing != null) {
                for (Versioningstatus vs : existing) {
                    if (StatusType.PENDING.name().equals(vs.getStatus()) &&
                            joinClassName.equals(vs.getMetaId()) &&
                            sourceInstanceId.equals(vs.getReviewComment())) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
        }
        return false;
    }

    public static void resolvePendingRelations(String entityUid, String entityType, Object entityDbObject) {
        if (entityUid == null || entityType == null) return;

        try {
            List<Versioningstatus> candidates = EposDataModelDAO.getInstance()
                    .getOneFromDBBySpecificKeySimpleNoCache("uid", entityUid, Versioningstatus.class);

            if (candidates == null || candidates.isEmpty()) return;

            for (Versioningstatus vs : candidates) {
                if (StatusType.PENDING.name().equals(vs.getStatus()) && entityType.equalsIgnoreCase(vs.getChangeComment())) {
                    try {
                        resolveSinglePendingRelation(vs, entityDbObject);
                        EposDataModelDAO.getInstance().deleteObject(vs);
                    } catch (Exception e) {
                        System.err.println("[RelationSyncUtil] Error resolving pending relation " + vs.getVersionId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error resolving pending relations: " + e.getMessage());
        }
    }

    private static void resolveSinglePendingRelation(Versioningstatus pending, Object targetEntity) throws Exception {
        String sourceInstanceId = pending.getReviewComment();
        if (sourceInstanceId == null) sourceInstanceId = pending.getInstanceId();

        String sourceEntityType = pending.getProvenance();
        String joinClassName = pending.getMetaId();

        if (sourceEntityType == null) return;

        Class<?> sourceClass = AbstractAPI.retrieveClass(sourceEntityType);
        if (sourceClass == null) return;

        List<Object> sourceList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(sourceInstanceId, sourceClass);
        if (sourceList == null || sourceList.isEmpty()) {
            System.err.println("[RelationSyncUtil] Source entity not found: " + sourceInstanceId);
            return;
        }
        Object sourceEntity = sourceList.get(0);

        Class<?> joinClass;
        try {
            joinClass = Class.forName(joinClassName);
        } catch (ClassNotFoundException | NullPointerException e) {
            return;
        }

        String sourceId = getModelId(sourceEntity);
        String targetId = getModelId(targetEntity);

        if (joinRelationAlreadyExists(joinClass, sourceId, targetId, sourceEntity, targetEntity)) {
            return;
        }

        Object newJoin = joinClass.getDeclaredConstructor().newInstance();
        initializeEmbeddedId(newJoin, sourceEntity, targetEntity);
        setJoinRelationship(newJoin, sourceEntity);
        setJoinRelationship(newJoin, targetEntity);

        try {
            EposDataModelDAO.getInstance().createJoinEntity(newJoin, sourceId, sourceEntity.getClass(), targetId, targetEntity.getClass());
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && (msg.contains("duplicate key") ||
                    msg.contains("already exists") ||
                    msg.contains("unique constraint"))) {
            } else {
                throw e;
            }
        }
    }

    private static boolean joinRelationAlreadyExists(Class<?> joinClass, String sourceId, String targetId,
                                                     Object sourceEntity, Object targetEntity) {
        try {
            String sourceClassName = sourceEntity.getClass().getSimpleName().toLowerCase();
            String targetClassName = targetEntity.getClass().getSimpleName().toLowerCase();

            String sourceFieldName = sourceClassName + "Instance";
            List<Object> existing = null;

            try {
                existing = EposDataModelDAO.getInstance()
                        .getOneFromDBBySpecificKeyNoCache(sourceFieldName, sourceId, joinClass);
            } catch (Exception e) {
                try {
                    existing = EposDataModelDAO.getInstance()
                            .getOneFromDBBySpecificKeyNoCache(sourceClassName + "1Instance", sourceId, joinClass);
                } catch (Exception e2) {
                }
            }

            if (existing != null && !existing.isEmpty()) {
                for (Object obj : existing) {
                    String existingTargetId = extractRelatedEntityId(obj, targetClassName);
                    if (targetId.equals(existingTargetId)) {
                        return true;
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private static String extractRelatedEntityId(Object joinEntity, String targetClassName) {
        // Prova vari pattern di nomi getter
        String[] patterns = {
                "get" + capitalize(targetClassName) + "Instance",
                "get" + capitalize(targetClassName) + "2Instance",  // Per relazioni ricorsive
        };

        for (String getterName : patterns) {
            try {
                Method getter = joinEntity.getClass().getMethod(getterName);
                Object related = getter.invoke(joinEntity);
                if (related != null) {
                    return getModelId(related);
                }
            } catch (Exception e) {
                // Prova il prossimo pattern
            }
        }
        return null;
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private static void setJoinRelationship(Object joinEntity, Object relatedEntity) throws Exception {
        String entityName = relatedEntity.getClass().getSimpleName();
        for (Method method : joinEntity.getClass().getMethods()) {
            if (method.getName().startsWith("set") &&
                    method.getName().toLowerCase().contains(entityName.toLowerCase()) &&
                    method.getParameterCount() == 1 &&
                    method.getParameterTypes()[0].isAssignableFrom(relatedEntity.getClass())) {
                method.invoke(joinEntity, relatedEntity);
                return;
            }
        }
    }

    private static String getModelId(Object modelObj) {
        if (modelObj == null) return null;
        try {
            Method m = modelObj.getClass().getMethod("getInstanceId");
            Object res = m.invoke(modelObj);
            return res != null ? res.toString() : null;
        } catch (Exception e) { return null; }
    }
}