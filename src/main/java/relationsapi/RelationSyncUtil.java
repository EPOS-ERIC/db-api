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

    // =========================================================================
    // METODO PUBBLICO: Propaga lo status del parent a tutte le entità figlie
    // Può essere chiamato esplicitamente dalle API dopo un cambio di status
    // =========================================================================

    /**
     * Propaga lo status del parent a tutte le entità figlie di un determinato tipo.
     * Utile quando il parent cambia stato (es. DRAFT → SUBMITTED → PUBLISHED)
     * e le entità figlie devono essere allineate.
     *
     * @param parentEntity L'entità parent
     * @param parentInstanceId L'instanceId del parent
     * @param childClass La classe delle entità figlie
     * @param foreignKeyFieldName Il nome del campo che referenzia il parent
     */
    public static <P, C> void propagateStatusToChildren(
            P parentEntity, String parentInstanceId, Class<C> childClass, String foreignKeyFieldName
    ) {
        StatusType parentStatus = getStatusFromEntity(parentEntity);
        if (parentStatus == null) {
            System.out.println("[RelationSyncUtil] Cannot propagate status: parent status is null");
            return;
        }

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        if (rawObjects == null || rawObjects.isEmpty()) {
            return;
        }

        System.out.println("[RelationSyncUtil] Propagating status " + parentStatus +
                " to " + rawObjects.size() + " " + childClass.getSimpleName() + " entities");

        for (Object obj : rawObjects) {
            if (childClass.isInstance(obj)) {
                updateChildEntityStatus(obj, parentStatus);
            }
        }
    }

    /**
     * DEBUG: Stampa lo status di tutte le entità figlie di un parent.
     * Utile per diagnosticare problemi di propagazione status.
     */
    public static <C> void debugChildStatuses(String parentInstanceId, Class<C> childClass, String foreignKeyFieldName) {
        System.out.println("=== DEBUG: Child statuses for parent " + parentInstanceId + " ===");

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache(foreignKeyFieldName, parentInstanceId, childClass);

        if (rawObjects == null || rawObjects.isEmpty()) {
            System.out.println("  No " + childClass.getSimpleName() + " found for this parent");
            return;
        }

        System.out.println("  Found " + rawObjects.size() + " " + childClass.getSimpleName() + " entities:");

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

                System.out.println("    - instanceId=" + instanceId + ", status=" + status + ", value=" + value);
            } catch (Exception e) {
                System.out.println("    - Error reading entity: " + e.getMessage());
            }
        }
        System.out.println("=== END DEBUG ===");
    }

    public static <P, C> void syncSimpleOneToMany(
            P parentEntity, String parentInstanceId, List<String> newValues, Class<C> childClass,
            String foreignKeyFieldName, String uidPrefix,
            Function<C, String> valueGetter, BiConsumer<C, String> valueSetter, BiConsumer<C, P> parentSetter
    ) {
        if (newValues == null) newValues = Collections.emptyList();
        Set<String> newValuesSet = new HashSet<>(newValues);
        newValuesSet.remove(null);

        // FIX: Leggi lo status dal parent entity
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
                // Crea nuova entità
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
                // FIX: Aggiorna lo status dell'entità esistente se diverso dal parent
                C existingEntity = existingMap.get(newValue);
                updateChildEntityStatus(existingEntity, parentStatus);
            }
        }
    }

    /**
     * Aggiorna lo status di un'entità figlia per allinearlo al parent.
     * Questo è necessario quando il parent cambia stato (es. DRAFT → SUBMITTED → PUBLISHED)
     */
    private static void updateChildEntityStatus(Object childEntity, StatusType newStatus) {
        if (childEntity == null || newStatus == null) return;

        String childClassName = childEntity.getClass().getSimpleName();

        try {
            // Prima prova a ottenere il Versioningstatus tramite getVersion()
            Method getVersion = childEntity.getClass().getMethod("getVersion");
            Object versionObj = getVersion.invoke(childEntity);

            if (versionObj instanceof Versioningstatus) {
                Versioningstatus vs = (Versioningstatus) versionObj;
                String currentStatus = vs.getStatus();

                // Aggiorna solo se lo status è diverso
                if (!newStatus.name().equals(currentStatus)) {
                    System.out.println("[RelationSyncUtil] Updating " + childClassName +
                            " status: " + currentStatus + " → " + newStatus.name() +
                            " (instanceId=" + vs.getInstanceId() + ")");

                    vs.setStatus(newStatus.name());
                    vs.setChangeTimestamp(OffsetDateTime.now());
                    EposDataModelDAO.getInstance().updateObject(vs);
                } else {
                    System.out.println("[RelationSyncUtil] " + childClassName +
                            " already has status " + currentStatus + ", skipping update");
                }
            } else {
                // Se getVersion() non restituisce un Versioningstatus valido,
                // prova a cercare il Versioningstatus nel DB tramite instanceId
                updateChildStatusByInstanceId(childEntity, newStatus, childClassName);
            }
        } catch (NoSuchMethodException e) {
            // Entity non ha getVersion(), prova con instanceId
            updateChildStatusByInstanceId(childEntity, newStatus, childClassName);
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error updating " + childClassName + " status: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Aggiorna lo status di un'entità figlia cercando il Versioningstatus nel DB tramite instanceId.
     * Fallback usato quando getVersion() non funziona.
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
                        System.out.println("[RelationSyncUtil] Updating " + childClassName +
                                " status via instanceId: " + currentStatus + " → " + newStatus.name() +
                                " (instanceId=" + instanceId + ")");

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

        // FIX: Leggi lo status dal parent entity
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
                // FIX: Eredita lo status dal parent, default DRAFT se non specificato
                StatusType statusToUse = parentStatus != null ? parentStatus : StatusType.DRAFT;
                vs.setStatus(statusToUse.name());
                vs.setChangeTimestamp(java.time.OffsetDateTime.now());
                vs.setMetaId(entity.getClass().getSimpleName()); // Usa il nome classe come riferimento

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

    /**
     * Estrae lo StatusType dal parent entity.
     * Prima prova entity.getStatus() (più affidabile dopo checkVersion()),
     * poi fallback a entity.getVersion().getStatus().
     */
    private static StatusType getStatusFromEntity(Object entity) {
        if (entity == null) return null;

        // FIX: Prima prova getStatus() direttamente sull'entity
        // Questo è più affidabile perché checkVersion() chiama obj.setStatus(targetStatus)
        try {
            Method getStatus = entity.getClass().getMethod("getStatus");
            Object statusObj = getStatus.invoke(entity);

            if (statusObj instanceof StatusType) {
                return (StatusType) statusObj;
            } else if (statusObj instanceof model.StatusType) {
                return (model.StatusType) statusObj;
            } else if (statusObj != null) {
                // Potrebbe essere una stringa o enum diverso
                try {
                    return StatusType.valueOf(statusObj.toString());
                } catch (IllegalArgumentException ignored) {}
            }
        } catch (NoSuchMethodException e) {
            // Entity non ha getStatus(), prova getVersion()
        } catch (Exception e) {
            // Ignora e prova il fallback
        }

        // Fallback: prova getVersion().getStatus()
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
                        // Status non valido
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // Entity non ha getVersion()
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
        boolean isNewVersion = previousInstanceId != null && !previousInstanceId.equals(parentId);

        if ((inputLinks == null || inputLinks.isEmpty()) && isNewVersion) {
            copyComplexRelationsFromPreviousVersion(previousInstanceId, parentDbObject, parentId, joinClass, targetClass, parentFieldName, targetGetter);
            return;
        }
        if (inputLinks == null) return;

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

        for (LinkedEntity link : inputLinks) {
            Object rawTarget = relationsapi.RelationChecker.checkRelation(mainEntity, previousEntity, null, link, overrideStatus, targetClass, enableStore);

            if (rawTarget != null) {
                T targetEntity = targetClass.cast(rawTarget);
                String targetId = getModelId(targetEntity);

                if (targetId != null) {
                    if (targetId.equals(parentId)) continue;
                    processedIds.add(targetId);

                    if (!existingMap.containsKey(targetId)) {
                        boolean created = createJoinEntity(joinClass, parentDbObject, targetEntity, parentSetter, targetSetter);
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
    }

    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(
            String oldParentInstanceId, P newParentDbObject, String newParentId,
            Class<J> joinClass, Class<T> targetClass, String parentFieldName, Function<J, T> targetGetter
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
                    J newJoin = joinClass.getDeclaredConstructor().newInstance();
                    initializeEmbeddedId(newJoin, newParentDbObject, target);
                    EposDataModelDAO.getInstance().createJoinEntity(newJoin, newParentId, newParentDbObject.getClass(), targetId, target.getClass());
                } catch (Exception e) {
                    System.err.println("[RelationSyncUtil] Error copying relation: " + e.getMessage());
                }
            }
        }
    }

    private static <P, J, T> boolean createJoinEntity(
            Class<J> joinClass, P parentDbObject, T targetEntity,
            BiConsumer<J, P> parentSetter, BiConsumer<J, T> targetSetter
    ) {
        try {J newJoin = joinClass.getDeclaredConstructor().newInstance();
            String parentId = getModelId(parentDbObject);
            String targetId = getModelId(targetEntity);

            if (parentId != null && parentId.equals(targetId)) return true;

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

    // =========================================================================
    // FIX: Aggiunta verifica per evitare duplicati nelle pending relations
    // =========================================================================
    private static void createPendingRelation(String sourceInstanceId, String sourceEntityType, String targetUid, String targetEntityType, String joinClassName) {
        try {
            // FIX: Verifica se esiste già una pending relation per questa combinazione
            if (pendingRelationExists(sourceInstanceId, targetUid, joinClassName)) {
                System.out.println("[RelationSyncUtil] Pending relation already exists for UID: " + targetUid);
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
            System.out.println("[RelationSyncUtil] Created pending relation for UID: " + targetUid);
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error creating pending relation: " + e.getMessage());
        }
    }

    // =========================================================================
    // FIX: Nuovo metodo per verificare esistenza pending relation
    // =========================================================================
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
            // Ignora errori e procedi con la creazione
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
                        System.out.println("[RelationSyncUtil] Resolved pending relation for UID: " + entityUid);
                    } catch (Exception e) {
                        System.err.println("[RelationSyncUtil] Error resolving pending relation " + vs.getVersionId() + ": " + e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error resolving pending relations: " + e.getMessage());
        }
    }

    // =========================================================================
    // FIX: Aggiunta verifica esistenza relazione prima di crearla
    // =========================================================================
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

        // FIX: Verifica se la relazione esiste già prima di crearla
        if (joinRelationAlreadyExists(joinClass, sourceId, targetId, sourceEntity, targetEntity)) {
            System.out.println("[RelationSyncUtil] Join relation already exists, skipping: " +
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
            // FIX: Gestisci duplicate key silenziosamente
            String msg = e.getMessage();
            if (msg != null && (msg.contains("duplicate key") ||
                    msg.contains("already exists") ||
                    msg.contains("unique constraint"))) {
                System.out.println("[RelationSyncUtil] Relation already exists (caught on insert): " +
                        joinClass.getSimpleName());
            } else {
                throw e;
            }
        }
    }

    // =========================================================================
    // FIX: Nuovi metodi helper per verificare esistenza relazione
    // =========================================================================
    private static boolean joinRelationAlreadyExists(Class<?> joinClass, String sourceId, String targetId,
                                                     Object sourceEntity, Object targetEntity) {
        try {
            String sourceClassName = sourceEntity.getClass().getSimpleName().toLowerCase();
            String targetClassName = targetEntity.getClass().getSimpleName().toLowerCase();

            // Prova con il campo source
            String sourceFieldName = sourceClassName + "Instance";
            List<Object> existing = null;

            try {
                existing = EposDataModelDAO.getInstance()
                        .getOneFromDBBySpecificKeyNoCache(sourceFieldName, sourceId, joinClass);
            } catch (Exception e) {
                // Prova nome alternativo (es. category1Instance per relazioni ricorsive)
                try {
                    existing = EposDataModelDAO.getInstance()
                            .getOneFromDBBySpecificKeyNoCache(sourceClassName + "1Instance", sourceId, joinClass);
                } catch (Exception e2) {
                    // Ignora
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