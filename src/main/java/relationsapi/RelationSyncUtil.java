package relationsapi;

import dao.EposDataModelDAO;
import org.epos.eposdatamodel.LinkedEntity;

import java.lang.reflect.Method;
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
            P parentEntity,
            String parentInstanceId,
            List<String> newValues,
            Class<C> childClass,
            String foreignKeyFieldName,
            String uidPrefix,
            Function<C, String> valueGetter,
            BiConsumer<C, String> valueSetter,
            BiConsumer<C, P> parentSetter
    ) {
        if (newValues == null) newValues = Collections.emptyList();

        Set<String> newValuesSet = new HashSet<>(newValues);
        newValuesSet.remove(null);

        List<Object> rawObjects = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey(foreignKeyFieldName, parentInstanceId, childClass);

        List<C> existingEntities = new ArrayList<>();
        if (rawObjects != null) {
            for (Object obj : rawObjects) {
                if (childClass.isInstance(obj)) {
                    existingEntities.add(childClass.cast(obj));
                }
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

    private static void setStandardFields(Object entity, String uidPrefix) {
        try {
            invokeSetter(entity, "setInstanceId", UUID.randomUUID().toString());
            invokeSetter(entity, "setMetaId", UUID.randomUUID().toString());
            invokeSetter(entity, "setUid", (uidPrefix != null ? uidPrefix + "/" : "") + UUID.randomUUID().toString());


            try {
                Method setVersion = entity.getClass().getMethod("setVersion", model.Versioningstatus.class);
                setVersion.invoke(entity, (Object) null);
            } catch (NoSuchMethodException | SecurityException e) {
            }

        } catch (Exception e) {
            throw new RuntimeException("Impossibile settare i campi ID su " + entity.getClass().getName(), e);
        }
    }

    private static void invokeSetter(Object obj, String methodName, String value) {
        try {
            Method method = obj.getClass().getMethod(methodName, String.class);
            method.invoke(obj, value);
        } catch (NoSuchMethodException e) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Synchronizes a "complex" One-To-Many / Many-To-Many relation involving a Join Class.
     * Manages the lifecycle of the join entity (creation, deletion) based on the input list.
     *
     * @param <P>                  Parent entity type (e.g., model.Distribution)
     * @param <J>                  Join entity type (e.g., model.WebserviceDistribution)
     * @param <T>                  Target entity type (e.g., model.Webservice)
     * @param parentDbObject       Parent DB Object (e.g., Distribution edmobj)
     * @param parentId             Parent ID
     * @param inputLinks           Input list (e.g., obj.getAccessService())
     * @param relationFromUpdate   Parameter for swap/update source
     * @param relationToUpdate     Parameter for swap/update target
     * @param joinClass            Join Class (e.g., WebserviceDistribution.class)
     * @param targetClass          Target DB Class (e.g., Webservice.class)
     * @param parentFieldName      Parent field name in the Join (e.g., "distributionInstance")
     * @param targetGetter         Getter for the target from the Join
     * @param parentSetter         Setter for the parent in the Join
     * @param targetSetter         Setter for the target in the Join
     * @param mainEntity           Main DTO (obj)
     * @param previousEntity       Previous DTO (previousObj)
     * @param overrideStatus       Override Status
     * @param enableStore          Flag for RelationChecker
     */
    public static <P, J, T> void syncComplexRelation(
            P parentDbObject,                    // Parent DB Object (e.g., Distribution edmobj)
            String parentId,                     // Parent ID
            List<LinkedEntity> inputLinks,       // Input list (e.g., obj.getAccessService())
            LinkedEntity relationFromUpdate,     // Parameter for swap
            LinkedEntity relationToUpdate,       // Parameter for swap
            Class<J> joinClass,                  // Join Class (e.g., WebserviceDistribution.class)
            Class<T> targetClass,                // Target DB Class (e.g., Webservice.class)
            String parentFieldName,              // Parent field name in the Join (e.g., "distributionInstance")
            Function<J, T> targetGetter,         // Getter for the target from the Join
            BiConsumer<J, P> parentSetter,       // Setter for the parent in the Join
            BiConsumer<J, T> targetSetter,       // Setter for the target in the Join
            org.epos.eposdatamodel.EPOSDataModelEntity mainEntity, // Main DTO (obj)
            org.epos.eposdatamodel.EPOSDataModelEntity previousEntity, // Previous DTO (previousObj)
            model.StatusType overrideStatus,     // Override Status
            boolean enableStore                  // Flag for RelationChecker
    ) {
        if (inputLinks == null) return;

        if (relationFromUpdate != null && inputLinks.contains(relationFromUpdate)) {
            inputLinks.remove(relationFromUpdate);
            inputLinks.add(relationToUpdate);
        }

        List<Object> existingRaw = dao.EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey(parentFieldName, parentId, joinClass);

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

        for (LinkedEntity link : inputLinks) {
            Object rawTarget = relationsapi.RelationChecker.checkRelation(
                    mainEntity, previousEntity, null, link, overrideStatus, targetClass, enableStore
            );

            if (rawTarget != null) {
                T targetEntity = targetClass.cast(rawTarget);
                String targetId = getModelId(targetEntity);

                if (targetId != null) {
                    processedIds.add(targetId);

                    if (!existingMap.containsKey(targetId)) {
                        try {
                            J newJoin = joinClass.getDeclaredConstructor().newInstance();
                            parentSetter.accept(newJoin, parentDbObject);
                            targetSetter.accept(newJoin, targetEntity);
                            dao.EposDataModelDAO.getInstance().updateObject(newJoin);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        for (Map.Entry<String, J> entry : existingMap.entrySet()) {
            if (!processedIds.contains(entry.getKey())) {
                dao.EposDataModelDAO.getInstance().deleteObject(entry.getValue());
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