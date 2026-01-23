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

    /**
     * Synchronizes a "simple" One-To-Many relationship for classes in the model.* package.
     * Uses reflection for standard fields (InstanceId, MetaId, Uid) to avoid inheritance constraints.
     *
     * @param <P>                 Type of the Parent entity (e.g., model.Dataproduct)
     * @param <C>                 Type of the Child entity (e.g., model.DistributionTitle)
     * @param parentEntity        The parent instance
     * @param parentInstanceId    The parent ID (passed explicitly for safety)
     * @param newValues           The list of new values (e.g., titles)
     * @param childClass          The child class
     * @param foreignKeyFieldName Name of the FK field in the DB (e.g., "distributionInstance")
     * @param uidPrefix           Prefix for the UID (e.g., "Title")
     * @param valueGetter         Lambda to retrieve the value (e.g., DistributionTitle::getTitle)
     * @param valueSetter         Lambda to set the value (e.g., DistributionTitle::setTitle)
     * @param parentSetter        Lambda to link the parent (e.g., DistributionTitle::setDistributionInstance)
     */
    public static <P, C> void syncSimpleOneToMany(
            P parentEntity, String parentInstanceId, List<String> newValues, Class<C> childClass,
            String foreignKeyFieldName, String uidPrefix,
            Function<C, String> valueGetter, BiConsumer<C, String> valueSetter, BiConsumer<C, P> parentSetter
    ) {
        if (newValues == null) newValues = Collections.emptyList();
        Set<String> newValuesSet = new HashSet<>(newValues);
        newValuesSet.remove(null);

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey(foreignKeyFieldName, parentInstanceId, childClass);

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
                    setStandardFields(newEntity, uidPrefix);
                    valueSetter.accept(newEntity, newValue);
                    parentSetter.accept(newEntity, parentEntity);
                    EposDataModelDAO.getInstance().updateObject(newEntity);
                } catch (Exception e) {
                    throw new RuntimeException("Errore sync relazione per " + childClass.getSimpleName(), e);
                }
            }
        }
    }

    /**
     * Copies simple one-to-many relations from a previous version to a new version.
     * Used when creating a DRAFT from PUBLISHED.
     */
    public static <P, C> void copySimpleOneToMany(
            String oldParentInstanceId, P newParentEntity, String newParentInstanceId, Class<C> childClass,
            String foreignKeyFieldName, String uidPrefix, Function<C, String> valueGetter,
            BiConsumer<C, String> valueSetter, BiConsumer<C, P> parentSetter
    ) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey(foreignKeyFieldName, oldParentInstanceId, childClass);
        if (oldRelations == null || oldRelations.isEmpty()) return;

        for (Object obj : oldRelations) {
            C oldEntity = childClass.cast(obj);
            String value = valueGetter.apply(oldEntity);
            try {
                C newEntity = childClass.getDeclaredConstructor().newInstance();
                setStandardFields(newEntity, uidPrefix);
                valueSetter.accept(newEntity, value);
                parentSetter.accept(newEntity, newParentEntity);
                EposDataModelDAO.getInstance().updateObject(newEntity);
            } catch (Exception e) {
                System.err.println("[RelationSyncUtil] Error copying simple relation: " + e.getMessage());
            }
        }
    }

    private static void setStandardFields(Object entity, String uidPrefix) {
        try {
            invokeSetter(entity, "setInstanceId", UUID.randomUUID().toString());
            invokeSetter(entity, "setMetaId", UUID.randomUUID().toString());
            invokeSetter(entity, "setUid", (uidPrefix != null ? uidPrefix + "/" : "") + UUID.randomUUID().toString());
            try {
                Method setVersion = entity.getClass().getMethod("setVersion", model.Versioningstatus.class);
                setVersion.invoke(entity, (Object) null);
            } catch (NoSuchMethodException | SecurityException ignored) {}
        } catch (Exception e) {
            throw new RuntimeException("Impossibile settare i campi ID su " + entity.getClass().getName(), e);
        }
    }


    private static void invokeSetter(Object obj, String methodName, String value) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, value);
        } catch (Exception ignored) { }
    }

    /**
     * Synchronizes a "complex" One-To-Many / Many-To-Many relation involving a Join Class.
     * Manages the lifecycle of the join entity (creation, deletion) based on the input list.
     * Supports deferred relations when target doesn't exist yet.
     * Supports copying relations from previous version when creating DRAFT from PUBLISHED.
     *
     * @param <P>                  Parent entity type (e.g., model.Distribution)
     * @param <J>                  Join entity type (e.g., model.WebserviceDistribution)
     * @param <T>                  Target entity type (e.g., model.Webservice)
     * @param parentDbObject       Parent DB Object (e.g., Distribution edmobj)
     * @param parentId             Parent ID (NEW instanceId)
     * @param inputLinks           Input list (e.g., obj.getAccessService()) - can be null if copying from previous
     * @param relationFromUpdate   Parameter for swap/update source
     * @param relationToUpdate     Parameter for swap/update target
     * @param joinClass            Join Class (e.g., WebserviceDistribution.class)
     * @param targetClass          Target DB Class (e.g., Webservice.class)
     * @param parentFieldName      Parent field name in the Join (e.g., "distributionInstance")
     * @param targetGetter         Getter for the target from the Join
     * @param parentSetter         Setter for the parent in the Join
     * @param targetSetter         Setter for the target in the Join
     * @param mainEntity           Main DTO (obj)
     * @param previousEntity       Previous DTO (previousObj) - used for relation copying
     * @param overrideStatus       Override Status
     * @param enableStore          Flag for RelationChecker
     */
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

        List<Object> existingRaw = EposDataModelDAO.getInstance().getOneFromDBBySpecificKey(parentFieldName, parentId, joinClass);
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
                        // FALLBACK: Se la creazione fallisce (es. target non trovato nel DB anche se RelationChecker l'ha ritornato), crea PENDING
                        if (!created) {
                            System.out.println("[RelationSyncUtil] Join creation failed for " + targetId + " (" + link.getUid() + "). Falling back to pending relation.");
                            createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
                        }
                    }
                }
            } else {
                System.out.println("[RelationSyncUtil] Deferred relation for " + joinClass.getSimpleName() + ": " + link);
                createPendingRelation(parentId, sourceEntityType, link.getUid(), link.getEntityType(), joinClass.getName());
            }
        }

        for (Map.Entry<String, J> entry : existingMap.entrySet()) {
            if (!processedIds.contains(entry.getKey())) {
                EposDataModelDAO.getInstance().deleteObject(entry.getValue());
            }
        }
    }

    /**
     * Copies complex (many-to-many) relations from a previous version to the new version.
     * Used when creating a DRAFT from PUBLISHED.
     */
    private static <P, J, T> void copyComplexRelationsFromPreviousVersion(
            String oldParentInstanceId, P newParentDbObject, String newParentId,
            Class<J> joinClass, Class<T> targetClass, String parentFieldName, Function<J, T> targetGetter
    ) {
        List<Object> oldRelations = EposDataModelDAO.getInstance().getOneFromDBBySpecificKey(parentFieldName, oldParentInstanceId, joinClass);
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

    /**
     * Creates a join entity with properly initialized EmbeddedId
     */
    private static <P, J, T> boolean createJoinEntity(
            Class<J> joinClass, P parentDbObject, T targetEntity,
            BiConsumer<J, P> parentSetter, BiConsumer<J, T> targetSetter
    ) {
        try {
            J newJoin = joinClass.getDeclaredConstructor().newInstance();
            String parentId = getModelId(parentDbObject);
            String targetId = getModelId(targetEntity);

            if (parentId != null && parentId.equals(targetId)) return true;

            initializeEmbeddedId(newJoin, parentDbObject, targetEntity);

            // Usiamo reflection se i setter sono null (backward compatibility) o usiamo i setter se forniti
            if (parentSetter != null) parentSetter.accept(newJoin, parentDbObject);
            else setJoinRelationship(newJoin, parentDbObject);

            if (targetSetter != null) targetSetter.accept(newJoin, targetEntity);
            else setJoinRelationship(newJoin, targetEntity);

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

    /**
     * Initializes the EmbeddedId for join entities like WebserviceDistribution
     */
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

    /**
     * Creates a pending relation record using Versioningstatus table with PENDING status.
     *
     * Field mapping:
     * - instanceId = sourceInstanceId
     * - uid = targetUid
     * - metaId = joinClassName
     * - status = PENDING
     * - provenance = sourceEntityType
     * - changeComment = targetEntityType
     */
    private static void createPendingRelation(String sourceInstanceId, String sourceEntityType, String targetUid, String targetEntityType, String joinClassName) {
        try {
            Versioningstatus pending = new Versioningstatus();
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setInstanceId(UUID.randomUUID().toString());
            pending.setUid(targetUid);
            pending.setMetaId(joinClassName); // FIX: Salviamo solo il nome della classe
            pending.setStatus(StatusType.PENDING.name());
            pending.setProvenance(sourceEntityType);
            pending.setChangeComment(targetEntityType);
            pending.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
            pending.setReviewComment(sourceInstanceId);

            System.out.println("[RelationSyncUtil] createPendingRelation: " + pending);
            EposDataModelDAO.getInstance().createObject(pending);
        } catch (Exception e) {
            System.err.println("[RelationSyncUtil] Error creating pending relation: " + e.getMessage());
        }
    }

    /**
     * Resolves all pending relations for a newly created entity.
     * Call this at the END of every entity's create() method.
     */
    public static void resolvePendingRelations(String entityUid, String entityType, Object entityDbObject) {
        if (entityUid == null || entityType == null) return;
        System.out.println("[RelationSyncUtil] resolvePendingRelations START. Searching for UID: '" + entityUid + "' Type: '" + entityType + "'");

        try {
            List<Versioningstatus> candidates = EposDataModelDAO.getInstance().getOneFromDBBySpecificKeySimpleNoCache("uid", entityUid, Versioningstatus.class);
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

    /**
     * Resolves a single pending relation by creating the join entity
     */
    private static void resolveSinglePendingRelation(Versioningstatus pending, Object targetEntity) throws Exception {
        String sourceInstanceId = pending.getReviewComment();
        if (sourceInstanceId == null) sourceInstanceId = pending.getInstanceId();

        String sourceEntityType = pending.getProvenance();
        String joinClassName = pending.getMetaId();

        if (sourceEntityType == null) {
            System.out.println("[RelationSyncUtil] Skipping pending relation: Provenance is NULL.");
            return;
        }

        Class<?> sourceClass = AbstractAPI.retrieveClass(sourceEntityType);
        if (sourceClass == null) {
            System.out.println("[RelationSyncUtil] Skipping pending relation: Unknown source type " + sourceEntityType);
            return;
        }

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
            System.out.println("[RelationSyncUtil] Skipping pending relation: Invalid Join Class '" + joinClassName + "' (Custom resolver needed?)");
            return;
        }

        Object newJoin = joinClass.getDeclaredConstructor().newInstance();
        initializeEmbeddedId(newJoin, sourceEntity, targetEntity);
        setJoinRelationship(newJoin, sourceEntity);
        setJoinRelationship(newJoin, targetEntity);

        String sourceId = getModelId(sourceEntity);
        String targetId = getModelId(targetEntity);

        EposDataModelDAO.getInstance().createJoinEntity(newJoin, sourceId, sourceEntity.getClass(), targetId, targetEntity.getClass());
    }

    /**
     * Sets a relationship on a join entity by finding the appropriate setter
     */
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