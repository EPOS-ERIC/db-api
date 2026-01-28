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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * RelationSyncUtil - Utility class for synchronizing entity relations.
 *
 * KEY CONCEPTS:
 *
 * REFERENCE_ENTITIES: Shared entities that should NOT be duplicated during cascade.
 * When creating a new version (e.g., PUBLISHED → DRAFT), these entities maintain
 * the link to the existing PUBLISHED version instead of creating new DRAFT versions.
 *
 * This applies to: CATEGORY, CATEGORYSCHEME, ORGANIZATION, PERSON, CONTACTPOINT
 *
 * INGESTOR MODE: When editorId is "ingestor", REFERENCE_ENTITIES are treated as
 * normal entities (with cascade/new version creation), because ingestor needs to
 * create new versions of all entities during import.
 */
public class RelationSyncUtil {

    private static final Logger LOG = Logger.getLogger(RelationSyncUtil.class.getName());

    // =========================================================================
    // REFERENCE ENTITIES: Shared entities that should NOT be duplicated
    // during cascade. When creating a new version (e.g., PUBLISHED → DRAFT),
    // these entities maintain the link to the existing PUBLISHED version
    // instead of creating new DRAFT versions.
    // =========================================================================
    private static final Set<String> REFERENCE_ENTITIES = Set.of(
            // Entity type names (uppercase)
            "CATEGORY",
            "CATEGORYSCHEME",  // FIX: Added CATEGORYSCHEME to reference entities
            "ORGANIZATION",
            "PERSON",
            "CONTACTPOINT",
            // Model class names (lowercase for case-insensitive match)
            "category",
            "categoryscheme",  // FIX: Added lowercase variant
            "organization",
            "person",
            "contactpoint"
    );

    /**
     * Checks if a class represents a shared reference entity.
     * Reference entities are not duplicated during cascade.
     */
    private static boolean isReferenceEntity(Class<?> targetClass) {
        if (targetClass == null) return false;
        String className = targetClass.getSimpleName().toUpperCase();
        return REFERENCE_ENTITIES.contains(className);
    }

    /**
     * Checks if special behavior should be applied for REFERENCE_ENTITIES
     * (i.e., use existing PUBLISHED version instead of creating a new one).
     *
     * Special behavior applies ONLY if:
     * 1. Target entity is a REFERENCE_ENTITY (Category, CategoryScheme, Organization, Person, ContactPoint)
     * 2. editorId is NOT "ingestor"
     *
     * If editor is "ingestor", REFERENCE_ENTITIES are treated as normal entities
     * (with cascade/new version creation), because ingestor needs to create
     * new versions of all entities during import.
     *
     * @param targetClass The target entity class
     * @param mainEntity The main entity (to get editorId)
     * @return true if REFERENCE_ENTITY logic should be applied (use existing PUBLISHED), false otherwise
     */
    private static boolean shouldApplyReferenceEntityLogic(Class<?> targetClass,
                                                           org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (!isReferenceEntity(targetClass)) {
            return false;
        }

        // Check for ingestor mode (treat as normal entity with cascade)
        if (mainEntity != null) {
            String editorId = mainEntity.getEditorId();
            if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) {
                LOG.log(Level.FINE, "[RelationSyncUtil] INGESTOR MODE: Treating " + targetClass.getSimpleName() +
                        " as normal entity (cascade enabled)");
                return false;
            }
        }

        // Normal user: apply REFERENCE_ENTITY logic (use existing PUBLISHED)
        return true;
    }

    /**
     * Public method to check if an entity type is a REFERENCE_ENTITY.
     * Can be used by other APIs to apply consistent logic.
     */
    public static boolean isReferenceEntityType(String entityType) {
        if (entityType == null) return false;
        return REFERENCE_ENTITIES.contains(entityType.toUpperCase()) ||
                REFERENCE_ENTITIES.contains(entityType.toLowerCase());
    }

    /**
     * Public method to check if REFERENCE_ENTITY logic should be applied.
     * Can be used by other APIs to apply consistent logic.
     */
    public static boolean shouldApplyReferenceEntityLogicForType(String entityType,
                                                                 org.epos.eposdatamodel.EPOSDataModelEntity mainEntity) {
        if (!isReferenceEntityType(entityType)) {
            return false;
        }

        if (mainEntity != null) {
            String editorId = mainEntity.getEditorId();
            if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) {
                return false;
            }
        }

        return true;
    }

    // =========================================================================
    // HELPER: Safe retrieval of join entities (handles both regular and embedded ID entities)
    // =========================================================================

    /**
     * Safely retrieves join entities, handling both regular relationships and embedded ID entities.
     * For embedded ID entities like CategoryHastopconcept, the query needs to use "id.xxxInstanceId"
     * instead of "xxxInstance.instanceId".
     *
     * @param parentFieldName The field name passed from the API (e.g., "categoryschemeInstance")
     * @param parentId The parent instance ID
     * @param joinClass The join entity class
     * @return List of join entities, or empty list if not found or error
     */
    @SuppressWarnings("unchecked")
    private static <J> List<Object> safeGetJoinEntities(String parentFieldName, String parentId, Class<J> joinClass) {
        List<Object> result = null;

        // Try 1: Embedded ID approach FIRST (most join entities use embedded IDs)
        // Convert "xxxInstance" to "xxxInstanceId" for embedded ID field
        try {
            String embeddedIdField = parentFieldName;
            if (parentFieldName.endsWith("Instance")) {
                embeddedIdField = parentFieldName + "Id";
            }
            result = (List<Object>) EposDataModelDAO.getInstance()
                    .getJoinEntitiesByParentId(embeddedIdField, parentId, joinClass);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            // Continue to next approach
        }

        // Try 2: Alternative field pattern without "Instance" suffix
        try {
            String baseFieldName = parentFieldName.replace("Instance", "");
            String altField = baseFieldName + "InstanceId";
            result = (List<Object>) EposDataModelDAO.getInstance()
                    .getJoinEntitiesByParentId(altField, parentId, joinClass);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            // Continue to next approach
        }

        // Try 3: Direct field approach as last resort (for entities with direct relationships)
        try {
            result = EposDataModelDAO.getInstance()
                    .getOneFromDBBySpecificKeyNoCache(parentFieldName, parentId, joinClass);
            if (result != null && !result.isEmpty()) {
                return result;
            }
        } catch (Exception e) {
            // Query failed - probably not a valid approach for this entity
        }

        return result != null ? result : new ArrayList<>();
    }

    // =========================================================================
    // PUBLIC METHOD: Propagates the parent's status to all child entities
    // =========================================================================

    /**
     * Propagates parent status to all child entities of a given type.
     * Useful when parent changes state (e.g., DRAFT → SUBMITTED → PUBLISHED)
     * and child entities need to be aligned.
     *
     * NOTE: Does NOT propagate status to REFERENCE_ENTITIES (they are shared).
     *
     * @param parentEntity The parent entity
     * @param parentInstanceId The parent's instanceId
     * @param childClass The child entity class
     * @param foreignKeyFieldName The field name that references the parent
     */
    public static <P, C> void propagateStatusToChildren(
            P parentEntity, String parentInstanceId, Class<C> childClass, String foreignKeyFieldName
    ) {
        propagateStatusToChildren(parentEntity, parentInstanceId, childClass, foreignKeyFieldName, null);
    }

    /**
     * Propagates parent status to all child entities of a given type.
     * With mainEntity parameter for REFERENCE_ENTITY logic check.
     */
    public static <P, C> void propagateStatusToChildren(
            P parentEntity, String parentInstanceId, Class<C> childClass, String foreignKeyFieldName,
            org.epos.eposdatamodel.EPOSDataModelEntity mainEntity
    ) {
        // Check if child class is a REFERENCE_ENTITY - don't propagate status to shared entities
        if (shouldApplyReferenceEntityLogic(childClass, mainEntity)) {
            LOG.log(Level.FINE, "[RelationSyncUtil] REFERENCE_ENTITY: Skipping status propagation for " +
                    childClass.getSimpleName() + " (shared entity)");
            return;
        }

        StatusType parentStatus = getStatusFromEntity(parentEntity);
        if (parentStatus == null) {
            LOG.log(Level.FINE, "[RelationSyncUtil] Cannot propagate status: parent status is null");
            return;
        }

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        if (rawObjects == null || rawObjects.isEmpty()) {
            return;
        }

        LOG.log(Level.FINE, "[RelationSyncUtil] Propagating status " + parentStatus +
                " to " + rawObjects.size() + " " + childClass.getSimpleName() + " entities");

        for (Object obj : rawObjects) {
            if (childClass.isInstance(obj)) {
                updateChildEntityStatus(obj, parentStatus);
            }
        }
    }

    /**
     * DEBUG: Prints the status of all child entities of a parent.
     */
    public static <C> void debugChildStatuses(String parentInstanceId, Class<C> childClass, String foreignKeyFieldName) {
        LOG.log(Level.FINE, "=== DEBUG: Child statuses for parent " + parentInstanceId + " ===");

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        if (rawObjects == null || rawObjects.isEmpty()) {
            LOG.log(Level.FINE, "  No " + childClass.getSimpleName() + " found for this parent");
            return;
        }

        LOG.log(Level.FINE, "  Found " + rawObjects.size() + " " + childClass.getSimpleName() + " entities:");

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
                    try {
                        List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                                .getOneFromDBByInstanceId(instanceId, Versioningstatus.class);
                        if (vsList != null && !vsList.isEmpty()) {
                            status = vsList.get(0).getStatus();
                        }
                    } catch (Exception e2) {}
                }

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

                LOG.log(Level.FINE, "    - instanceId=" + instanceId + ", status=" + status + ", value=" + value);
            } catch (Exception e) {
                LOG.log(Level.FINE, "    - Error reading entity: " + e.getMessage());
            }
        }
        LOG.log(Level.FINE, "=== END DEBUG ===");
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

    /**
     * Updates the status of a child entity to align with parent.
     */
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
                    LOG.log(Level.FINE, "[RelationSyncUtil] Updating " + childClassName +
                            " status: " + currentStatus + " → " + newStatus.name() +
                            " (instanceId=" + vs.getInstanceId() + ")");

                    if (newStatus == StatusType.PUBLISHED && vs.getUid() != null) {
                        archiveOldPublishedVersionsForChild(vs.getUid(), vs.getVersionId(), childClassName);
                    }

                    vs.setStatus(newStatus.name());
                    vs.setChangeTimestamp(OffsetDateTime.now());
                    EposDataModelDAO.getInstance().updateObject(vs);
                } else {
                    LOG.log(Level.FINE, "[RelationSyncUtil] " + childClassName +
                            " already has status " + currentStatus + ", skipping update");
                }
            } else {
                updateChildStatusByInstanceId(childEntity, newStatus, childClassName);
            }
        } catch (NoSuchMethodException e) {
            updateChildStatusByInstanceId(childEntity, newStatus, childClassName);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error updating " + childClassName + " status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Archives old PUBLISHED versions of a child entity.
     */
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
                    LOG.log(Level.FINE, "[RelationSyncUtil] AUTO-ARCHIVE: Archiving old PUBLISHED " + childClassName +
                            " version " + rawVs.getInstanceId() + " (uid=" + uid + ")");
                    rawVs.setStatus(StatusType.ARCHIVED.name());
                    rawVs.setChangeTimestamp(OffsetDateTime.now());
                    rawVs.setChangeComment("Auto-archived on child status propagation");
                    EposDataModelDAO.getInstance().updateObject(rawVs);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error archiving old versions for " + childClassName + ": " + e.getMessage());
        }
    }

    /**
     * Updates child entity status by searching Versioningstatus in DB via instanceId.
     */
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
                        LOG.log(Level.FINE, "[RelationSyncUtil] Updating " + childClassName +
                                " status via instanceId: " + currentStatus + " → " + newStatus.name() +
                                " (instanceId=" + instanceId + ")");

                        if (newStatus == StatusType.PUBLISHED && vs.getUid() != null) {
                            archiveOldPublishedVersionsForChild(vs.getUid(), vs.getVersionId(), childClassName);
                        }

                        vs.setStatus(newStatus.name());
                        vs.setChangeTimestamp(OffsetDateTime.now());
                        EposDataModelDAO.getInstance().updateObject(vs);
                    }
                } else {
                    LOG.log(Level.WARNING, "[RelationSyncUtil] No Versioningstatus found for " +
                            childClassName + " instanceId=" + instanceId);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error updating " + childClassName +
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
                setStandardFields(newEntity, uidPrefix, parentStatus);
                valueSetter.accept(newEntity, value);
                parentSetter.accept(newEntity, newParentEntity);
                EposDataModelDAO.getInstance().updateObject(newEntity);
            } catch (Exception e) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] Error copying simple relation: " + e.getMessage());
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
    }

    /**
     * Extracts the StatusType from an entity.
     */
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
            // Entity doesn't have getStatus(), try getVersion()
        } catch (Exception e) {
            // Ignore and try fallback
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
                        // Invalid status
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // Entity doesn't have getVersion()
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error getting status from entity: " + e.getMessage());
        }

        return null;
    }

    private static void invokeSetter(Object obj, String methodName, String value) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, value);
        } catch (Exception ignored) { }
    }

    // =========================================================================
    // MAIN SYNC METHOD: syncComplexRelation
    // =========================================================================

    /**
     * ThreadLocal for preventing infinite loops during cascade.
     */
    private static final ThreadLocal<Set<String>> cascadeInProgress = ThreadLocal.withInitial(HashSet::new);

    /**
     * Cache of versions already created during current cascade.
     */
    private static final ThreadLocal<Map<String, String>> cascadeCreatedVersions = ThreadLocal.withInitial(HashMap::new);

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

        // Determine if this is a new version creation
        boolean hasInstanceChanged = previousInstanceId != null && !previousInstanceId.equals(parentId);
        StatusType currentStatus = mainEntity.getStatus();
        boolean isNewVersion = hasInstanceChanged && currentStatus == StatusType.DRAFT;

        LOG.log(Level.FINE, "[RelationSyncUtil] Syncing complex relation " + joinClass.getSimpleName() +
                " for " + parentId + " (isNewVersion=" + isNewVersion + ", hasInstanceChanged=" + hasInstanceChanged +
                ", currentStatus=" + currentStatus + ", overrideStatus=" + overrideStatus +
                ", isReferenceEntity=" + isReferenceEntity(targetClass) + ")");

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
                    StatusType cascadeStatus = effectiveStatus;
                    LOG.log(Level.FINE, "[RelationSyncUtil] CASCADE: Creating new versions with status " + cascadeStatus);
                    copyComplexRelationsFromPreviousVersion(previousInstanceId, parentDbObject, parentId,
                            joinClass, targetClass, parentFieldName, targetGetter, cascadeStatus, mainEntity);
                } else if (effectiveStatus != null) {
                    // Update without list: propagate status to existing relations
                    LOG.log(Level.FINE, "[RelationSyncUtil] PROPAGATE STATUS: Propagating " + effectiveStatus +
                            " to existing relations of " + joinClass.getSimpleName());
                    String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
                    List<?> existingRaw = EposDataModelDAO.getInstance()
                            .getJoinEntitiesByParentId(embeddedIdField, parentId, joinClass);
                    if (existingRaw != null) {
                        for (Object o : existingRaw) {
                            T target = targetGetter.apply(joinClass.cast(o));
                            if (target != null) {
                                // REFERENCE_ENTITIES: don't propagate status (shared entity)
                                if (shouldApplyReferenceEntityLogic(targetClass, mainEntity)) {
                                    LOG.log(Level.FINE, "[RelationSyncUtil] REFERENCE_ENTITY: Skipping status propagation for " +
                                            targetClass.getSimpleName() + " (shared entity, use existing PUBLISHED)");
                                } else {
                                    updateChildEntityStatus(target, effectiveStatus);
                                }
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

            String embeddedIdFieldForExisting = parentFieldName.replace("Instance", "InstanceId");
            List<?> existingRawList = EposDataModelDAO.getInstance().getJoinEntitiesByParentId(embeddedIdFieldForExisting, parentId, joinClass);
            List<Object> existingRaw = existingRawList != null ? new ArrayList<>(existingRawList) : null;

            Map<String, J> existingMap = new HashMap<>();
            if (existingRaw != null) {
                for (Object o : existingRaw) {
                    J joinEntity = joinClass.cast(o);
                    T target = targetGetter.apply(joinEntity);
                    String targetId = getModelId(target);
                    if (targetId != null) existingMap.put(targetId, joinEntity);
                }
            }
            LOG.log(Level.FINE, "[RelationSyncUtil] existingMap has " + existingMap.size() + " entries: " + existingMap.keySet());

            Set<String> processedIds = new HashSet<>();
            String sourceEntityType = parentDbObject.getClass().getSimpleName().toUpperCase();

            StatusType cascadeStatus = isNewVersion ? effectiveStatus : null;

            for (LinkedEntity link : inputLinks) {
                Object rawTarget = null;

                // When NOT cascade (only status propagation), retrieve entity directly
                if (!isNewVersion && link.getInstanceId() != null) {
                    List<Object> directResults = EposDataModelDAO.getInstance()
                            .getOneFromDBByInstanceIdNoCache(link.getInstanceId(), targetClass);
                    if (directResults != null && !directResults.isEmpty()) {
                        rawTarget = directResults.get(0);
                        LOG.log(Level.FINE, "[RelationSyncUtil] DIRECT LOOKUP: Retrieved " + targetClass.getSimpleName() +
                                " " + link.getInstanceId() + " for status propagation");
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

                        // CASCADE: If new parent version, create new versions of related entities
                        if (cascadeStatus != null) {
                            // REFERENCE_ENTITIES: use existing PUBLISHED version (shared entity)
                            if (shouldApplyReferenceEntityLogic(targetClass, mainEntity)) {
                                T publishedVersion = findPublishedVersion(targetEntity, targetClass);
                                if (publishedVersion != null) {
                                    targetForJoin = publishedVersion;
                                    targetIdForJoin = getModelId(publishedVersion);
                                    LOG.log(Level.FINE, "[RelationSyncUtil] REFERENCE_ENTITY: Using existing PUBLISHED " +
                                            targetClass.getSimpleName() + " " + targetIdForJoin + " (shared entity, no cascade)");
                                } else {
                                    // No PUBLISHED exists - create one with PUBLISHED status
                                    LOG.log(Level.FINE, "[RelationSyncUtil] REFERENCE_ENTITY: No PUBLISHED version found for " +
                                            targetClass.getSimpleName() + " " + targetId + ", creating PUBLISHED stub");
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
                                    LOG.log(Level.FINE, "[RelationSyncUtil] CASCADE: Created new " + targetClass.getSimpleName() +
                                            " version " + targetIdForJoin + " with status " + cascadeStatus);
                                }
                            }
                        } else if (effectiveStatus != null) {
                            // Status propagation (DRAFT->SUBMITTED, SUBMITTED->PUBLISHED)
                            // REFERENCE_ENTITIES: do NOT propagate status
                            if (shouldApplyReferenceEntityLogic(targetClass, mainEntity)) {
                                LOG.log(Level.FINE, "[RelationSyncUtil] REFERENCE_ENTITY: Skipping status propagation for " +
                                        targetClass.getSimpleName() + " " + targetId + " (shared entity)");
                            } else {
                                updateChildEntityStatus(targetEntity, effectiveStatus);
                                LOG.log(Level.FINE, "[RelationSyncUtil] PROPAGATE: Updated " + targetClass.getSimpleName() +
                                        " " + targetId + " to status " + effectiveStatus);
                            }
                        }

                        // ALWAYS create/verify the join regardless of status propagation
                        LOG.log(Level.FINE, "[RelationSyncUtil] After status handling: targetIdForJoin=" + targetIdForJoin +
                                ", existingMap.containsKey=" + existingMap.containsKey(targetIdForJoin));
                        processedIds.add(targetIdForJoin);

                        if (!existingMap.containsKey(targetIdForJoin)) {
                            LOG.log(Level.FINE, "[RelationSyncUtil] Creating join entity " + joinClass.getSimpleName() +
                                    " for parent=" + parentId + ", target=" + targetIdForJoin);
                            try {
                                boolean created = createJoinEntity(joinClass, parentDbObject, targetForJoin, parentSetter, targetSetter);
                                LOG.log(Level.FINE, "[RelationSyncUtil] Join creation result: " + created);
                                if (!created) {
                                    LOG.log(Level.FINE, "[RelationSyncUtil] Join creation failed, creating pending relation");
                                    createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
                                }
                            } catch (Exception joinEx) {
                                LOG.log(Level.WARNING, "[RelationSyncUtil] ERROR creating join: " + joinEx.getMessage());
                                joinEx.printStackTrace();
                                createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
                            }
                        } else {
                            LOG.log(Level.FINE, "[RelationSyncUtil] Join already exists in existingMap for target=" + targetIdForJoin);
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

    /**
     * Creates a REFERENCE_ENTITY as PUBLISHED (for when no PUBLISHED version exists).
     */
    @SuppressWarnings("unchecked")
    private static <T> T createReferenceEntityAsPublished(T originalEntity, Class<T> targetClass) {
        String originalInstanceId = getModelId(originalEntity);
        if (originalInstanceId == null) return null;

        try {
            String entityName = targetClass.getSimpleName().toUpperCase();
            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] No API found for REFERENCE_ENTITY " + entityName);
                return null;
            }

            org.epos.eposdatamodel.EPOSDataModelEntity dto =
                    (org.epos.eposdatamodel.EPOSDataModelEntity) api.retrieve(originalInstanceId);
            if (dto == null) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] Could not retrieve REFERENCE_ENTITY " + originalInstanceId);
                return null;
            }

            // Always create as PUBLISHED for REFERENCE_ENTITIES
            LinkedEntity newVersionLe = api.create(dto, StatusType.PUBLISHED, null, null);
            if (newVersionLe == null || newVersionLe.getInstanceId() == null) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] Failed to create PUBLISHED REFERENCE_ENTITY");
                return null;
            }

            List<Object> newVersionList = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(newVersionLe.getInstanceId(), targetClass);

            if (newVersionList != null && !newVersionList.isEmpty()) {
                return targetClass.cast(newVersionList.get(0));
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error creating PUBLISHED REFERENCE_ENTITY: " + e.getMessage());
        }
        return null;
    }

    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(
            String oldParentInstanceId, P newParentDbObject, String newParentId,
            Class<J> joinClass, Class<T> targetClass, String parentFieldName, Function<J, T> targetGetter
    ) {
        copyComplexRelationsFromPreviousVersion(
                oldParentInstanceId, newParentDbObject, newParentId,
                joinClass, targetClass, parentFieldName, targetGetter,
                null, null
        );
    }

    /**
     * Copies relations from previous version, with cascade option.
     */
    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(
            String oldParentInstanceId, P newParentDbObject, String newParentId,
            Class<J> joinClass, Class<T> targetClass, String parentFieldName, Function<J, T> targetGetter,
            StatusType cascadeStatus, org.epos.eposdatamodel.EPOSDataModelEntity mainEntity
    ) {
        String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
        List<?> oldRelationsRaw = EposDataModelDAO.getInstance().getJoinEntitiesByParentId(embeddedIdField, oldParentInstanceId, joinClass);
        List<Object> oldRelations = oldRelationsRaw != null ? new ArrayList<>(oldRelationsRaw) : new ArrayList<>();

        if (oldRelations.isEmpty()) return;

        for (Object obj : oldRelations) {
            J oldJoin = joinClass.cast(obj);
            T target = targetGetter.apply(oldJoin);
            String targetId = getModelId(target);

            if (targetId != null) {
                try {
                    T targetForJoin = target;
                    String targetIdForJoin = targetId;

                    // CASCADE: If requested, create new version of related entity
                    if (cascadeStatus != null) {
                        // REFERENCE_ENTITIES: use existing PUBLISHED
                        if (shouldApplyReferenceEntityLogic(targetClass, mainEntity)) {
                            T publishedVersion = findPublishedVersion(target, targetClass);
                            if (publishedVersion != null) {
                                targetForJoin = publishedVersion;
                                targetIdForJoin = getModelId(publishedVersion);
                                LOG.log(Level.FINE, "[RelationSyncUtil] REFERENCE_ENTITY (copy): Using existing PUBLISHED " +
                                        targetClass.getSimpleName() + " " + targetIdForJoin);
                            } else {
                                LOG.log(Level.FINE, "[RelationSyncUtil] REFERENCE_ENTITY (copy): No PUBLISHED version for " +
                                        targetClass.getSimpleName() + " " + targetId + ", using current");
                            }
                        } else {
                            T newVersionTarget = createCascadeVersion(target, targetClass, cascadeStatus);
                            if (newVersionTarget != null) {
                                targetForJoin = newVersionTarget;
                                targetIdForJoin = getModelId(newVersionTarget);
                                LOG.log(Level.FINE, "[RelationSyncUtil] CASCADE: Created new " + targetClass.getSimpleName() +
                                        " version " + targetIdForJoin + " with status " + cascadeStatus);
                            }
                        }
                    }

                    J newJoin = joinClass.getDeclaredConstructor().newInstance();

                    setJoinRelationship(newJoin, newParentDbObject);
                    setJoinRelationship(newJoin, targetForJoin);

                    initializeEmbeddedId(newJoin, newParentDbObject, targetForJoin);
                    EposDataModelDAO.getInstance().createJoinEntity(newJoin, newParentId, newParentDbObject.getClass(), targetIdForJoin, targetForJoin.getClass());
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "[RelationSyncUtil] Error copying relation: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Creates a new version of a related entity with the new status (cascade).
     */
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
            LOG.log(Level.FINE, "[RelationSyncUtil] CASCADE: Reusing already created " + targetClass.getSimpleName() +
                    " version " + existingNewInstanceId + " for metaId=" + metaId);
            List<Object> existingList = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(existingNewInstanceId, targetClass);
            if (existingList != null && !existingList.isEmpty()) {
                return targetClass.cast(existingList.get(0));
            }
        }

        Set<String> inProgress = cascadeInProgress.get();
        if (inProgress.contains(metaId)) {
            LOG.log(Level.FINE, "[RelationSyncUtil] CASCADE: Skipping " + targetClass.getSimpleName() +
                    " metaId=" + metaId + " (already in progress)");
            return null;
        }

        try {
            inProgress.add(metaId);

            String entityName = targetClass.getSimpleName().toUpperCase();

            AbstractAPI api = AbstractAPI.retrieveAPI(entityName);
            if (api == null) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] CASCADE: No API found for " + entityName);
                return null;
            }

            org.epos.eposdatamodel.EPOSDataModelEntity dto =
                    (org.epos.eposdatamodel.EPOSDataModelEntity) api.retrieve(originalInstanceId);
            if (dto == null) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] CASCADE: Could not retrieve entity " + originalInstanceId);
                return null;
            }

            LinkedEntity newVersionLe = api.create(dto, newStatus, null, null);
            if (newVersionLe == null || newVersionLe.getInstanceId() == null) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] CASCADE: Failed to create new version");
                return null;
            }

            createdVersions.put(cacheKey, newVersionLe.getInstanceId());

            List<Object> newVersionList = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(newVersionLe.getInstanceId(), targetClass);

            if (newVersionList != null && !newVersionList.isEmpty()) {
                return targetClass.cast(newVersionList.get(0));
            }

        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] CASCADE error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            inProgress.remove(metaId);
            if (inProgress.isEmpty()) {
                cascadeInProgress.remove();
                cascadeCreatedVersions.remove();
            }
        }
        return null;
    }

    /**
     * Finds the PUBLISHED version of a reference entity.
     */
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
                LOG.log(Level.WARNING, "[RelationSyncUtil] Error finding PUBLISHED by UID: " + e.getMessage());
            }
        }

        String metaId = getMetaId(originalEntity);
        if (metaId != null) {
            try {
                List<Object> allVersions = EposDataModelDAO.getInstance()
                        .getOneFromDBByMetaId(metaId, targetClass);
                if (allVersions != null && !allVersions.isEmpty()) {
                    return findEntityWithStatus(allVersions, StatusType.PUBLISHED, targetClass);
                }
            } catch (Exception e) {
                LOG.log(Level.WARNING, "[RelationSyncUtil] Error finding PUBLISHED by metaId: " + e.getMessage());
            }
        }

        return null;
    }

    /**
     * Finds an entity with a specific status in a list of versions.
     */
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
                    // Ignore and continue
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

    private static String getModelId(Object modelObj) {
        if (modelObj == null) return null;
        try {
            Method m = modelObj.getClass().getMethod("getInstanceId");
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

            LOG.log(Level.FINE, "[RelationSyncUtil] createJoinEntity called: join=" + joinClass.getSimpleName() +
                    ", parent=" + parentId + ", target=" + targetId);

            if (parentId != null && parentId.equals(targetId)) {
                LOG.log(Level.FINE, "[RelationSyncUtil] createJoinEntity: Skipping self-reference");
                return true;
            }

            if (joinExists(joinClass, parentDbObject, targetEntity)) {
                LOG.log(Level.FINE, "[RelationSyncUtil] Join " + joinClass.getSimpleName() +
                        " already exists for parent=" + parentId + " target=" + targetId);
                return true;
            }

            LOG.log(Level.FINE, "[RelationSyncUtil] createJoinEntity: Creating new join instance...");
            J newJoin = joinClass.getDeclaredConstructor().newInstance();

            if (parentSetter != null) {
                parentSetter.accept(newJoin, parentDbObject);
                LOG.log(Level.FINE, "[RelationSyncUtil] createJoinEntity: Set parent entity reference");
            }
            if (targetSetter != null) {
                targetSetter.accept(newJoin, targetEntity);
                LOG.log(Level.FINE, "[RelationSyncUtil] createJoinEntity: Set target entity reference");
            }

            initializeEmbeddedId(newJoin, parentDbObject, targetEntity);

            LOG.log(Level.FINE, "[RelationSyncUtil] createJoinEntity: Calling DAO.createJoinEntity...");
            Boolean result = EposDataModelDAO.getInstance().createJoinEntity(
                    newJoin, parentId, parentDbObject.getClass(), targetId, targetEntity.getClass()
            );
            LOG.log(Level.FINE, "[RelationSyncUtil] createJoinEntity: DAO result=" + result);
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

            String parentClassName = toCamelCase(parent.getClass().getSimpleName());
            String embeddedIdField = parentClassName + "InstanceId";

            List<?> existing = EposDataModelDAO.getInstance()
                    .getJoinEntitiesByParentId(embeddedIdField, parentId, joinClass);

            if (existing != null) {
                for (Object obj : existing) {
                    String existingTargetId = getTargetIdFromJoin(obj, target.getClass().getSimpleName());
                    if (targetId.equals(existingTargetId)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.FINE, "[RelationSyncUtil] joinExists check failed (will proceed): " + e.getMessage());
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
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error initializing EmbeddedId: " + e.getMessage());
        }
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

    private static String toCamelCase(String className) {
        if (className == null || className.isEmpty()) return className;
        return Character.toLowerCase(className.charAt(0)) + className.substring(1);
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    // =========================================================================
    // PENDING RELATIONS
    // =========================================================================

    private static void createPendingRelation(String sourceInstanceId, String sourceEntityType, String targetUid, String targetEntityType, String joinClassName) {
        try {
            if (pendingRelationExists(sourceInstanceId, targetUid, joinClassName)) {
                LOG.log(Level.FINE, "[RelationSyncUtil] Pending relation already exists for UID: " + targetUid);
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
            LOG.log(Level.FINE, "[RelationSyncUtil] Created pending relation for UID: " + targetUid);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error creating pending relation: " + e.getMessage());
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
            // Ignore errors and proceed with creation
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
                        LOG.log(Level.FINE, "[RelationSyncUtil] Resolved pending relation for UID: " + entityUid);
                    } catch (Exception e) {
                        LOG.log(Level.WARNING, "[RelationSyncUtil] Error resolving pending relation " + vs.getVersionId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            LOG.log(Level.WARNING, "[RelationSyncUtil] Error resolving pending relations: " + e.getMessage());
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
            LOG.log(Level.WARNING, "[RelationSyncUtil] Source entity not found: " + sourceInstanceId);
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
            LOG.log(Level.FINE, "[RelationSyncUtil] Join relation already exists, skipping: " +
                    joinClass.getSimpleName() + " (" + sourceId + " <-> " + targetId + ")");
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
                LOG.log(Level.FINE, "[RelationSyncUtil] Relation already exists (caught on insert): " +
                        joinClass.getSimpleName());
            } else {
                throw e;
            }
        }
    }

    private static boolean joinRelationAlreadyExists(Class<?> joinClass, String sourceId, String targetId,
                                                     Object sourceEntity, Object targetEntity) {
        try {
            String sourceClassName = toCamelCase(sourceEntity.getClass().getSimpleName());
            String targetClassName = targetEntity.getClass().getSimpleName();

            String sourceFieldName = sourceClassName + "InstanceId";
            List<?> existing = null;

            try {
                existing = EposDataModelDAO.getInstance()
                        .getJoinEntitiesByParentId(sourceFieldName, sourceId, joinClass);
            } catch (Exception e) {
                try {
                    existing = EposDataModelDAO.getInstance()
                            .getJoinEntitiesByParentId(sourceClassName + "1InstanceId", sourceId, joinClass);
                } catch (Exception e2) {
                    // Ignore
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
        String[] patterns = {
                "get" + capitalize(targetClassName) + "Instance",
                "get" + capitalize(targetClassName) + "2Instance",
        };

        for (String getterName : patterns) {
            try {
                Method getter = joinEntity.getClass().getMethod(getterName);
                Object related = getter.invoke(joinEntity);
                if (related != null) {
                    return getModelId(related);
                }
            } catch (Exception e) {
            }
        }
        return null;
    }
}