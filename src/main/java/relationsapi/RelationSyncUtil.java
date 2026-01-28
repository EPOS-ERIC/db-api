package relationsapi;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.LinkedEntity;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * RelationSyncUtil - Utility class for synchronizing entity relations.
 *
 * REFERENCE_ENTITIES: Shared entities (Category, CategoryScheme, Organization, Person, ContactPoint)
 * that should NOT be duplicated during cascade. They maintain links to existing PUBLISHED versions.
 * Exception: In "ingestor" mode, these are treated as normal entities.
 */
public class RelationSyncUtil {

    private static final Logger LOG = Logger.getLogger(RelationSyncUtil.class.getName());

    private static final Set<String> REFERENCE_ENTITIES = Set.of(
            "CATEGORY", "CATEGORYSCHEME", "ORGANIZATION", "PERSON", "CONTACTPOINT",
            "category", "categoryscheme", "organization", "person", "contactpoint"
    );

    private static final ThreadLocal<Set<String>> cascadeInProgress = ThreadLocal.withInitial(HashSet::new);
    private static final ThreadLocal<Map<String, String>> cascadeCreatedVersions = ThreadLocal.withInitial(HashMap::new);

    // ===== Reference Entity Checks =====

    private static boolean isReferenceEntity(Class<?> targetClass) {
        return targetClass != null && REFERENCE_ENTITIES.contains(targetClass.getSimpleName().toUpperCase());
    }

    private static boolean shouldApplyReferenceEntityLogic(Class<?> targetClass, org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (!isReferenceEntity(targetClass)) return false;
        if (mainEntity != null) {
            String editorId = mainEntity.getEditorId();
            if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) return false;
        }
        return true;
    }

    public static boolean isReferenceEntityType(String entityType) {
        return entityType != null && (REFERENCE_ENTITIES.contains(entityType.toUpperCase()) ||
                REFERENCE_ENTITIES.contains(entityType.toLowerCase()));
    }

    public static boolean shouldApplyReferenceEntityLogicForType(String entityType, org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (!isReferenceEntityType(entityType)) return false;
        if (mainEntity != null) {
            String editorId = mainEntity.getEditorId();
            if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) return false;
        }
        return true;
    }

    // ===== Self-Reference Check =====

    private static boolean isSelfReference(LinkedEntity link, String parentId, String parentUid) {
        if (link == null) return false;
        if (parentId != null && parentId.equals(link.getInstanceId())) return true;
        if (parentUid != null && parentUid.equals(link.getUid())) return true;
        return false;
    }

    // ===== Status Propagation =====

    public static <P, C> void propagateStatusToChildren(P parentEntity, String parentInstanceId,
                                                        Class<C> childClass, String foreignKeyFieldName, org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (shouldApplyReferenceEntityLogic(childClass, mainEntity)) return;

        StatusType parentStatus = getStatusFromEntity(parentEntity);
        if (parentStatus == null) return;

        List<Object> children = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);
        if (children == null || children.isEmpty()) return;

        for (Object obj : children) {
            if (childClass.isInstance(obj)) updateChildEntityStatus(obj, parentStatus);
        }
    }

    private static void updateChildEntityStatus(Object childEntity, StatusType newStatus) {
        if (childEntity == null || newStatus == null) return;
        try {
            Method getVersion = childEntity.getClass().getMethod("getVersion");
            Object versionObj = getVersion.invoke(childEntity);
            if (versionObj instanceof Versioningstatus) {
                Versioningstatus vs = (Versioningstatus) versionObj;
                if (!newStatus.name().equals(vs.getStatus())) {
                    if (newStatus == StatusType.PUBLISHED && vs.getUid() != null) {
                        archiveOldPublishedVersions(vs.getUid(), vs.getVersionId());
                    }
                    vs.setStatus(newStatus.name());
                    vs.setChangeTimestamp(OffsetDateTime.now());
                    EposDataModelDAO.getInstance().updateObject(vs);
                }
            }
        } catch (NoSuchMethodException e) {
            updateChildStatusByInstanceId(childEntity, newStatus);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error updating child status: " + e.getMessage());
        }
    }

    private static void archiveOldPublishedVersions(String uid, String currentVersionId) {
        if (uid == null) return;
        try {
            List<Versioningstatus> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, Versioningstatus.class);
            for (Versioningstatus vs : allVersions) {
                if (vs.getVersionId() != null && vs.getVersionId().equals(currentVersionId)) continue;
                if (vs.getMetaId() != null && vs.getMetaId().contains(".")) continue;
                if (StatusType.PENDING.name().equals(vs.getStatus())) continue;
                if (StatusType.PUBLISHED.name().equals(vs.getStatus())) {
                    vs.setStatus(StatusType.ARCHIVED.name());
                    vs.setChangeTimestamp(OffsetDateTime.now());
                    vs.setChangeComment("Auto-archived on status propagation");
                    EposDataModelDAO.getInstance().updateObject(vs);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error archiving old versions: " + e.getMessage());
        }
    }

    private static void updateChildStatusByInstanceId(Object childEntity, StatusType newStatus) {
        try {
            Method getInstanceId = childEntity.getClass().getMethod("getInstanceId");
            Object instanceIdObj = getInstanceId.invoke(childEntity);
            if (instanceIdObj != null) {
                String instanceId = instanceIdObj.toString();
                List<Versioningstatus> vsList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(instanceId, Versioningstatus.class);
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
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error updating child status by instanceId: " + e.getMessage());
        }
    }

    // ===== Simple One-to-Many Sync =====

    public static <P, C> void syncSimpleOneToMany(P parentEntity, String parentInstanceId, List<String> newValues,
                                                  Class<C> childClass, String foreignKeyFieldName, String uidPrefix,
                                                  Function<C, String> valueGetter, BiConsumer<C, String> valueSetter, BiConsumer<C, P> parentSetter) {

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

        // Delete removed
        for (C existing : existingEntities) {
            String val = valueGetter.apply(existing);
            if (val != null && !newValuesSet.contains(val)) {
                EposDataModelDAO.getInstance().deleteObject(existing);
            }
        }

        // Add new or update existing
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
                updateChildEntityStatus(existingMap.get(newValue), parentStatus);
            }
        }
    }

    public static <P, C> void copySimpleOneToMany(String oldParentInstanceId, P newParentEntity, String newParentInstanceId,
                                                  Class<C> childClass, String foreignKeyFieldName, String uidPrefix,
                                                  Function<C, String> valueGetter, BiConsumer<C, String> valueSetter, BiConsumer<C, P> parentSetter) {

        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, oldParentInstanceId, childClass);
        if (oldRelations == null || oldRelations.isEmpty()) return;

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
                LOG.log(Level.WARNING, "Error copying simple relation: " + e.getMessage());
            }
        }
    }

    // ===== Complex Relation Sync (Main Method) =====

    public static <P, J, T> void syncComplexRelation(P parentDbObject, String parentId, List<LinkedEntity> inputLinks,
                                                     LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate,
                                                     Class<J> joinClass, Class<T> targetClass, String parentFieldName,
                                                     Function<J, T> targetGetter, BiConsumer<J, P> parentSetter, BiConsumer<J, T> targetSetter,
                                                     org.epos.eposdatamodel.EPOSDataModelEntity mainEntity, org.epos.eposdatamodel.EPOSDataModelEntity previousEntity,
                                                     model.StatusType overrideStatus, boolean enableStore) {

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

            // Handle null or empty list
            if (inputLinks == null || inputLinks.isEmpty()) {
                if (isNewVersion) {
                    copyComplexRelationsFromPreviousVersion(previousInstanceId, parentDbObject, parentId,
                            joinClass, targetClass, parentFieldName, targetGetter, effectiveStatus, mainEntity);
                }
                return;
            }

            if (relationFromUpdate != null && inputLinks.contains(relationFromUpdate)) {
                inputLinks.remove(relationFromUpdate);
                inputLinks.add(relationToUpdate);
            }

            // Get existing joins
            String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
            List<?> existingRawList = EposDataModelDAO.getInstance().getJoinEntitiesByParentId(embeddedIdField, parentId, joinClass);
            Map<String, J> existingMap = new HashMap<>();
            if (existingRawList != null) {
                for (Object o : existingRawList) {
                    J joinEntity = joinClass.cast(o);
                    T target = targetGetter.apply(joinEntity);
                    String targetId = getModelId(target);
                    if (targetId != null) existingMap.put(targetId, joinEntity);
                }
            }

            Set<String> processedIds = new HashSet<>();
            String sourceEntityType = parentDbObject.getClass().getSimpleName().toUpperCase();
            String parentUid = getUid(parentDbObject);
            StatusType cascadeStatus = isNewVersion ? effectiveStatus : null;

            for (LinkedEntity link : inputLinks) {
                // Self-reference check before processing
                if (isSelfReference(link, parentId, parentUid)) {
                    LOG.log(Level.WARNING, "SELF-REFERENCE BLOCKED: " + parentId);
                    continue;
                }

                Object rawTarget = null;
                if (!isNewVersion && link.getInstanceId() != null) {
                    List<Object> directResults = EposDataModelDAO.getInstance()
                            .getOneFromDBByInstanceIdNoCache(link.getInstanceId(), targetClass);
                    if (directResults != null && !directResults.isEmpty()) {
                        rawTarget = directResults.get(0);
                    }
                }

                if (rawTarget == null) {
                    rawTarget = RelationChecker.checkRelation(mainEntity, previousEntity, null, link, effectiveStatus, targetClass, enableStore);
                }

                if (rawTarget != null) {
                    T targetEntity = targetClass.cast(rawTarget);
                    String targetId = getModelId(targetEntity);
                    String targetUid = getUid(targetEntity);

                    if (targetId != null) {
                        // Post-resolution self-reference check
                        if (targetId.equals(parentId) || (targetUid != null && targetUid.equals(parentUid))) {
                            LOG.log(Level.WARNING, "SELF-REFERENCE BLOCKED (post-resolution): " + parentId);
                            continue;
                        }

                        T targetForJoin = targetEntity;
                        String targetIdForJoin = targetId;

                        // Handle cascade or status propagation
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
                                createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
                            }
                        }
                    }
                } else {
                    createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
                }
            }

            // Delete removed joins
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

    // ===== Join Entity Creation =====

    private static <P, J, T> boolean createJoinEntity(Class<J> joinClass, P parentDbObject, T targetEntity,
                                                      BiConsumer<J, P> parentSetter, BiConsumer<J, T> targetSetter, String parentFieldName) {
        try {
            String parentId = getModelId(parentDbObject);
            String targetId = getModelId(targetEntity);

            // Final self-reference check
            if (parentId != null && parentId.equals(targetId)) {
                LOG.log(Level.WARNING, "BLOCKING SELF-REFERENCE in createJoinEntity: " + parentId);
                return true;
            }

            if (joinExistsWithFieldName(joinClass, parentId, targetId, parentFieldName)) return true;

            J newJoin = joinClass.getDeclaredConstructor().newInstance();

            // Initialize embedded ID with explicit field names (critical for same-class relations)
            initializeEmbeddedIdWithFieldName(newJoin, parentDbObject, targetEntity, parentFieldName);

            // Set entity references
            if (parentSetter != null) parentSetter.accept(newJoin, parentDbObject);
            if (targetSetter != null) targetSetter.accept(newJoin, targetEntity);

            // Verify embedded ID before persist
            if (!verifyEmbeddedIdNotSelfReference(newJoin)) {
                LOG.log(Level.SEVERE, "Self-reference in embedded ID! Aborting.");
                return true;
            }

            // Persist using updateObject (bypasses DAO's createJoinEntity ID manipulation)
            EposDataModelDAO.getInstance().updateObject(newJoin);
            return true;

        } catch (Exception e) {
            if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                    e.getMessage().contains("already exists") || e.getMessage().contains("unique constraint"))) {
                return true;
            }
            LOG.log(Level.WARNING, "Error creating join: " + e.getMessage());
            return false;
        }
    }

    private static boolean verifyEmbeddedIdNotSelfReference(Object joinEntity) {
        try {
            Method getIdMethod = joinEntity.getClass().getMethod("getId");
            Object embeddedId = getIdMethod.invoke(joinEntity);
            if (embeddedId != null) {
                Method getCat1Id = embeddedId.getClass().getMethod("getCategory1InstanceId");
                Method getCat2Id = embeddedId.getClass().getMethod("getCategory2InstanceId");
                String cat1Id = (String) getCat1Id.invoke(embeddedId);
                String cat2Id = (String) getCat2Id.invoke(embeddedId);
                return cat1Id == null || !cat1Id.equals(cat2Id);
            }
        } catch (NoSuchMethodException e) {
            // Not a CategoryIspartof - OK
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error verifying embedded ID: " + e.getMessage());
        }
        return true;
    }

    private static <J> boolean joinExistsWithFieldName(Class<J> joinClass, String parentId, String targetId, String parentFieldName) {
        try {
            if (parentId == null || targetId == null) return false;
            String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
            List<?> existing = EposDataModelDAO.getInstance().getJoinEntitiesByParentId(embeddedIdField, parentId, joinClass);
            if (existing != null) {
                String targetFieldName = deriveTargetFieldName(parentFieldName);
                for (Object obj : existing) {
                    String existingTargetId = getTargetIdFromJoin(obj, targetFieldName);
                    if (targetId.equals(existingTargetId)) return true;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "joinExists check failed: " + e.getMessage());
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

    // ===== Embedded ID Initialization (Critical Fix for Same-Class Relations) =====

    private static <P, T> void initializeEmbeddedIdWithFieldName(Object joinEntity, P parent, T target, String parentFieldName) {
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

            // Derive field names from parentFieldName
            String parentIdField = parentFieldName.replace("Instance", "InstanceId");
            String targetIdField = deriveTargetFieldName(parentFieldName).replace("Instance", "InstanceId");

            // Set parent ID
            try {
                Method setter = idClass.getMethod("set" + capitalize(parentIdField), String.class);
                setter.invoke(idInstance, parentInstanceId);
            } catch (NoSuchMethodException ignored) {}

            // Set target ID
            try {
                Method setter = idClass.getMethod("set" + capitalize(targetIdField), String.class);
                setter.invoke(idInstance, targetInstanceId);
            } catch (NoSuchMethodException ignored) {}

            Method setIdMethod = joinEntity.getClass().getMethod("setId", idClass);
            setIdMethod.invoke(joinEntity, idInstance);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error in initializeEmbeddedIdWithFieldName: " + e.getMessage());
        }
    }

    private static String deriveTargetFieldName(String parentFieldName) {
        if (parentFieldName.contains("1")) return parentFieldName.replace("1", "2");
        if (parentFieldName.contains("2")) return parentFieldName.replace("2", "1");
        return parentFieldName; // Fallback
    }

    // ===== Copy Relations from Previous Version =====

    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(String oldParentInstanceId, P newParentDbObject,
                                                                          String newParentId, Class<J> joinClass, Class<T> targetClass, String parentFieldName,
                                                                          Function<J, T> targetGetter, StatusType cascadeStatus, org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {

        String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
        List<?> oldRelationsRaw = EposDataModelDAO.getInstance().getJoinEntitiesByParentId(embeddedIdField, oldParentInstanceId, joinClass);
        if (oldRelationsRaw == null || oldRelationsRaw.isEmpty()) return;

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
                    if (e.getMessage() == null || (!e.getMessage().contains("duplicate key") && !e.getMessage().contains("already exists"))) {
                        LOG.log(Level.WARNING, "Error copying relation: " + e.getMessage());
                    }
                }
            }
        }
    }

    // ===== Cascade Version Creation =====

    @SuppressWarnings("unchecked")
    private static <T> T createCascadeVersion(T originalEntity, Class<T> targetClass, StatusType newStatus) {
        String originalInstanceId = getModelId(originalEntity);
        if (originalInstanceId == null) return null;

        String metaId = getMetaId(originalEntity);
        if (metaId == null) metaId = originalInstanceId;
        String cacheKey = metaId + "_" + newStatus.name();

        Map<String, String> createdVersions = cascadeCreatedVersions.get();
        if (createdVersions.containsKey(cacheKey)) {
            String existingNewInstanceId = createdVersions.get(cacheKey);
            List<Object> existingList = EposDataModelDAO.getInstance().getOneFromDBByInstanceIdNoCache(existingNewInstanceId, targetClass);
            if (existingList != null && !existingList.isEmpty()) {
                return targetClass.cast(existingList.get(0));
            }
        }

        if (cascadeInProgress.get().contains(metaId)) return null;

        try {
            cascadeInProgress.get().add(metaId);
            String entityName = targetClass.getSimpleName().toUpperCase();
            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) return null;

            org.epos.eposdatamodel.EPOSDataModelEntity dto = (org.epos.eposdatamodel.EPOSDataModelEntity) api.retrieve(originalInstanceId);
            if (dto == null) return null;

            dto.setInstanceChangedId(originalInstanceId);
            dto.setInstanceId(null);
            dto.setStatus(newStatus);

            LinkedEntity result = api.create(dto, newStatus, null, null);
            if (result != null && result.getInstanceId() != null) {
                createdVersions.put(cacheKey, result.getInstanceId());
                List<Object> newList = EposDataModelDAO.getInstance().getOneFromDBByInstanceIdNoCache(result.getInstanceId(), targetClass);
                if (newList != null && !newList.isEmpty()) {
                    return targetClass.cast(newList.get(0));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error creating cascade version: " + e.getMessage());
        } finally {
            cascadeInProgress.get().remove(metaId);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T createReferenceEntityAsPublished(T originalEntity, Class<T> targetClass) {
        String originalInstanceId = getModelId(originalEntity);
        if (originalInstanceId == null) return null;

        try {
            String entityName = targetClass.getSimpleName().toUpperCase();
            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) return null;

            org.epos.eposdatamodel.EPOSDataModelEntity dto = (org.epos.eposdatamodel.EPOSDataModelEntity) api.retrieve(originalInstanceId);
            if (dto == null) return null;

            dto.setInstanceId(null);
            dto.setStatus(StatusType.PUBLISHED);

            LinkedEntity result = api.create(dto, StatusType.PUBLISHED, null, null);
            if (result != null && result.getInstanceId() != null) {
                List<Object> newList = EposDataModelDAO.getInstance().getOneFromDBByInstanceIdNoCache(result.getInstanceId(), targetClass);
                if (newList != null && !newList.isEmpty()) {
                    return targetClass.cast(newList.get(0));
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error creating reference entity as published: " + e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T> T findPublishedVersion(T entity, Class<T> targetClass) {
        String uid = getUid(entity);
        if (uid == null) return null;
        try {
            List<Object> versions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, targetClass);
            for (Object v : versions) {
                String status = getVersionStatus(v);
                if (StatusType.PUBLISHED.toString().equals(status)) {
                    return targetClass.cast(v);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error finding published version: " + e.getMessage());
        }
        return null;
    }

    // ===== Pending Relations =====

    private static void createPendingRelation(String sourceInstanceId, String sourceEntityType,
                                              String targetUid, String targetEntityType, String joinClassName) {
        if (pendingRelationExists(sourceInstanceId, targetUid, joinClassName)) return;
        try {
            Versioningstatus pending = new Versioningstatus();
            pending.setInstanceId(UUID.randomUUID().toString());
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setUid(targetUid);
            pending.setMetaId(joinClassName);
            pending.setStatus(StatusType.PENDING.toString());
            pending.setReviewComment(sourceInstanceId);
            pending.setProvenance(sourceEntityType);
            pending.setChangeTimestamp(OffsetDateTime.now());
            EposDataModelDAO.getInstance().updateObject(pending);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error creating pending relation: " + e.getMessage());
        }
    }

    private static boolean pendingRelationExists(String sourceInstanceId, String targetUid, String joinClassName) {
        try {
            List<Versioningstatus> existing = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(targetUid, Versioningstatus.class);
            for (Versioningstatus vs : existing) {
                if (StatusType.PENDING.toString().equals(vs.getStatus()) &&
                        joinClassName.equals(vs.getMetaId()) &&
                        sourceInstanceId.equals(vs.getReviewComment())) {
                    return true;
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "Error checking pending relation: " + e.getMessage());
        }
        return false;
    }

    public static void resolvePendingRelations(String entityUid, String entityType, Object entityDbObject) {
        try {
            List<Versioningstatus> pendingList = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(entityUid, Versioningstatus.class);
            for (Versioningstatus vs : pendingList) {
                if (StatusType.PENDING.toString().equals(vs.getStatus()) && vs.getMetaId() != null && vs.getMetaId().contains(".")) {
                    try {
                        resolveSinglePendingRelation(vs, entityDbObject);
                        EposDataModelDAO.getInstance().deleteObject(vs);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "Error resolving pending relation: " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error in resolvePendingRelations: " + e.getMessage());
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
        if (sourceList == null || sourceList.isEmpty()) return;

        Object sourceEntity = sourceList.get(0);
        Class<?> joinClass = Class.forName(joinClassName);

        String sourceId = getModelId(sourceEntity);
        String targetId = getModelId(targetEntity);

        // Self-reference check
        if (sourceId != null && sourceId.equals(targetId)) return;

        Object newJoin = joinClass.getDeclaredConstructor().newInstance();
        initializeEmbeddedIdLegacy(newJoin, sourceEntity, targetEntity);
        setJoinRelationship(newJoin, sourceEntity);
        setJoinRelationship(newJoin, targetEntity);

        try {
            EposDataModelDAO.getInstance().updateObject(newJoin);
        } catch (Exception e) {
            if (e.getMessage() == null || (!e.getMessage().contains("duplicate key") && !e.getMessage().contains("already exists"))) {
                throw e;
            }
        }
    }

    // Legacy embedded ID init for pending relations (uses alphabetical ordering for same-class)
    private static <P, T> void initializeEmbeddedIdLegacy(Object joinEntity, P parent, T target) {
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

            List<Method> stringSetters = new ArrayList<>();
            for (Method m : idClass.getMethods()) {
                if (m.getName().startsWith("set") && m.getParameterCount() == 1 && m.getParameterTypes()[0] == String.class) {
                    stringSetters.add(m);
                }
            }
            stringSetters.sort(Comparator.comparing(Method::getName));

            if (parentClassName.equals(targetClassName) && stringSetters.size() >= 2) {
                stringSetters.get(0).invoke(idInstance, parentInstanceId);
                stringSetters.get(1).invoke(idInstance, targetInstanceId);
            } else {
                boolean parentSet = false, targetSet = false;
                for (Method setter : stringSetters) {
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
            LOG.log(Level.WARNING, "Error in initializeEmbeddedIdLegacy: " + e.getMessage());
        }
    }

    // ===== Helper Methods =====

    private static void setStandardFields(Object entity, String uidPrefix, StatusType parentStatus) {
        try {
            invokeSetter(entity, "setInstanceId", UUID.randomUUID().toString());
            invokeSetter(entity, "setMetaId", UUID.randomUUID().toString());
            invokeSetter(entity, "setUid", (uidPrefix != null ? uidPrefix + "/" : "") + UUID.randomUUID().toString());

            try {
                Method setVersion = entity.getClass().getMethod("setVersion", Versioningstatus.class);
                Versioningstatus vs = new Versioningstatus();
                vs.setVersionId(UUID.randomUUID().toString());
                vs.setInstanceId(UUID.randomUUID().toString());
                vs.setUid((uidPrefix != null ? uidPrefix + "/" : "") + UUID.randomUUID().toString());
                vs.setStatus((parentStatus != null ? parentStatus : StatusType.DRAFT).name());
                vs.setChangeTimestamp(OffsetDateTime.now());
                vs.setMetaId(entity.getClass().getSimpleName());
                setVersion.invoke(entity, vs);
            } catch (NoSuchMethodException ignored) {}
        } catch (Exception e) {
            throw new RuntimeException("Cannot set standard fields on " + entity.getClass().getName(), e);
        }
    }

    private static StatusType getStatusFromEntity(Object entity) {
        if (entity == null) return null;
        try {
            Method getStatus = entity.getClass().getMethod("getStatus");
            Object statusObj = getStatus.invoke(entity);
            if (statusObj instanceof StatusType) return (StatusType) statusObj;
            if (statusObj != null) {
                try { return StatusType.valueOf(statusObj.toString()); } catch (IllegalArgumentException ignored) {}
            }
        } catch (Exception ignored) {}

        try {
            Method getVersion = entity.getClass().getMethod("getVersion");
            Object versionObj = getVersion.invoke(entity);
            if (versionObj instanceof Versioningstatus) {
                String statusStr = ((Versioningstatus) versionObj).getStatus();
                if (statusStr != null) {
                    try { return StatusType.valueOf(statusStr); } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private static String getVersionStatus(Object entity) {
        try {
            Method getVersion = entity.getClass().getMethod("getVersion");
            Object versionObj = getVersion.invoke(entity);
            if (versionObj != null) {
                Method getStatus = versionObj.getClass().getMethod("getStatus");
                Object statusObj = getStatus.invoke(versionObj);
                return statusObj != null ? statusObj.toString() : null;
            }
        } catch (Exception ignored) {}
        return null;
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

    private static void invokeSetter(Object obj, String methodName, String value) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, value);
        } catch (Exception ignored) {}
    }

    private static String getModelId(Object modelObj) {
        return getModelProperty(modelObj, "getInstanceId");
    }

    private static String getUid(Object modelObj) {
        return getModelProperty(modelObj, "getUid");
    }

    private static String getMetaId(Object modelObj) {
        return getModelProperty(modelObj, "getMetaId");
    }

    private static String getModelProperty(Object modelObj, String methodName) {
        if (modelObj == null) return null;
        try {
            Method method = modelObj.getClass().getMethod(methodName);
            Object result = method.invoke(modelObj);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String capitalize(String s) {
        return (s == null || s.isEmpty()) ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}