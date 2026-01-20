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

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class RelationChecker {

    /**
     * ThreadLocal set to track entities currently being processed in the current thread.
     * This prevents infinite recursion when bidirectional relationships exist
     * (e.g., DataProduct <-> Distribution, WebService <-> Operation).
     */
    private static final ThreadLocal<Set<String>> processingEntities =
            ThreadLocal.withInitial(HashSet::new);

    /**
     * Generates a unique key for a LinkedEntity to identify it during cycle detection.
     *
     * @param linkedEntity the linked entity to generate a key for
     * @return a unique string key combining entity type, instance ID, and meta ID
     */
    private static String getEntityKey(LinkedEntity linkedEntity) {
        return linkedEntity.getEntityType() + ":" +
                linkedEntity.getInstanceId() + ":" +
                linkedEntity.getMetaId();
    }

    /**
     * Checks and manages relationships between entities, handling status synchronization
     * and entity creation/update operations.
     *
     * This method includes cycle detection to prevent StackOverflowError when
     * processing bidirectional relationships.
     *
     * @param mainEntity         the main entity being processed
     * @param oldMainEntity      the previous version of the main entity (for versioning)
     * @param mainEntityClazz    the class of the main entity (optional, derived from mainEntity if null)
     * @param linkedEntity       the linked entity representing the relationship to check
     * @param overrideStatus     the status to override (if applicable)
     * @param clazz              the class type for database queries
     * @param enableStore        whether to enable storing in the singleton cache
     * @return the resolved entity object from the database, or null if not found
     */
    public static Object checkRelation(EPOSDataModelEntity mainEntity,
                                       EPOSDataModelEntity oldMainEntity,
                                       Class mainEntityClazz,
                                       LinkedEntity linkedEntity,
                                       StatusType overrideStatus,
                                       Class clazz,
                                       Boolean enableStore) {;

        String entityKey = getEntityKey(linkedEntity);
        Set<String> processing = processingEntities.get();

        /*
         * If we're already processing this entity in the current call stack,
         * we still need to handle status updates but WITHOUT recursive relation processing.
         * This prevents infinite recursion while still allowing status propagation.
         */
        boolean isInCycle = processing.contains(entityKey);

        if (isInCycle) {
            // In a cycle: perform limited processing without recursion
            return handleCycleCase(mainEntity, linkedEntity, overrideStatus, clazz, enableStore);
        }

        try {
            // Mark this entity as currently being processed
            processing.add(entityKey);

            // Full processing with recursive relation handling
            return processRelation(mainEntity, oldMainEntity, mainEntityClazz,
                    linkedEntity, overrideStatus, clazz, enableStore);

        } finally {
            /*
             * Always remove the entity from the processing set when done,
             * regardless of success or exception.
             */
            processing.remove(entityKey);

            // Clean up ThreadLocal if empty to prevent memory leaks
            if (processing.isEmpty()) {
                processingEntities.remove();
            }
        }
    }

    /**
     * Handles the case when a cycle is detected.
     * Performs status update if needed but does NOT process nested relations recursively.
     * This breaks the infinite recursion while still propagating status changes.
     *
     * @param mainEntity     the main entity being processed
     * @param linkedEntity   the linked entity in the cycle
     * @param overrideStatus the status to override
     * @param clazz          the class type for database queries
     * @param enableStore    whether to enable storing in the singleton cache
     * @return the resolved entity object from the database, or null if not found
     */
    private static Object handleCycleCase(EPOSDataModelEntity mainEntity,
                                          LinkedEntity linkedEntity,
                                          StatusType overrideStatus,
                                          Class clazz,
                                          Boolean enableStore) {

        // Retrieve the existing entity WITHOUT provenance filter to ensure we find it
        EPOSDataModelEntity relationEntity = (EPOSDataModelEntity)
                LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);

        LinkedEntity obj = linkedEntity;

        if (relationEntity != null && mainEntity != null) {
            /*
             * Check if status synchronization is needed even in cycle case.
             * This ensures status propagation continues even when we detect a cycle.
             */
            boolean needsStatusUpdate = mainEntity.getStatus() != null
                    && relationEntity.getStatus() != null
                    && !mainEntity.getStatus().equals(relationEntity.getStatus());

            if (needsStatusUpdate) {
                // Update status on the relation entity
                relationEntity.setStatus(mainEntity.getStatus());

                // Check singleton cache first
                if (enableStore) {
                    obj = OperationWebserviceInDistributionSingleton.getInstance()
                            .getTarget(linkedEntity);
                }

                if (obj == null || obj.equals(linkedEntity)) {
                    /*
                     * Create/update the entity WITHOUT processing its nested relations.
                     * We pass null for oldLinkedEntity and newLinkedEntity to signal
                     * that this is a limited update (no relation traversal).
                     */
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

        // Return the entity from database
        List<Object> results = EposDataModelDAO.getInstance()
                .getOneFromDBByLinkedEntity(obj, clazz);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Full relation processing with recursive relation handling.
     * This is the main processing logic when no cycle is detected.
     * No PROVENANCE used
     *
     * @param mainEntity       the main entity being processed
     * @param oldMainEntity    the previous version of the main entity
     * @param mainEntityClazz  the class of the main entity
     * @param linkedEntity     the linked entity representing the relationship
     * @param overrideStatus   the status to override
     * @param clazz            the class type for database queries
     * @param enableStore      whether to enable storing in the singleton cache
     * @return the resolved entity object from the database, or null if not found
     */
    private static Object processRelation(EPOSDataModelEntity mainEntity,
                                          EPOSDataModelEntity oldMainEntity,
                                          Class mainEntityClazz,
                                          LinkedEntity linkedEntity,
                                          StatusType overrideStatus,
                                          Class clazz,
                                          Boolean enableStore) {

        // Derive main entity class if not provided
        if (mainEntityClazz == null) {
            mainEntityClazz = mainEntity.getClass();
        }

        // Retrieve LinkedEntity representations for the main entity (new and old versions)
        LinkedEntity newLinkedEntityMainEntity = mainEntity == null ? null :
                AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName()
                                .toUpperCase(Locale.ROOT)).name())
                        .retrieveLinkedEntity(mainEntity.getInstanceId());

        LinkedEntity oldLinkedEntityMainEntity = oldMainEntity == null ? null :
                AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName()
                                .toUpperCase(Locale.ROOT)).name())
                        .retrieveLinkedEntity(oldMainEntity.getInstanceId());

        /*
         * Retrieve the relation entity WITHOUT provenance filter.
         */
        EPOSDataModelEntity relationEntity = (EPOSDataModelEntity)
                LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);

        LinkedEntity obj = null;

        /*
         * If the relation entity exists and both old/new main entity references exist,
         * we can perform status synchronization with full recursive processing.
         */
        if (relationEntity != null && newLinkedEntityMainEntity != null && oldLinkedEntityMainEntity != null) {

            // Check if status synchronization is needed
            boolean statusMismatch = mainEntity.getStatus() != null
                    && relationEntity.getStatus() != null
                    && !mainEntity.getStatus().equals(relationEntity.getStatus());

            if (statusMismatch) {
                // Synchronize status from main entity to relation entity
                relationEntity.setStatus(mainEntity.getStatus());

                // Try to get existing target from singleton cache
                if (enableStore) {
                    obj = OperationWebserviceInDistributionSingleton.getInstance()
                            .getTarget(linkedEntity);
                }

                // If not found in cache, create/update the entity with full relation processing
                if (obj == null) {
                    obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType()
                                    .toUpperCase(Locale.ROOT)).name())
                            .create(relationEntity, overrideStatus, oldLinkedEntityMainEntity, newLinkedEntityMainEntity);

                    // Store in singleton cache if enabled
                    if (enableStore) {
                        OperationWebserviceInDistributionSingleton.getInstance()
                                .createRelation(linkedEntity, obj);
                    }
                }
            } else {
                // Status already matches, no update needed
                obj = linkedEntity;
            }

        } else if (relationEntity != null && newLinkedEntityMainEntity != null) {
            /*
             * Relation entity exists but oldLinkedEntityMainEntity is null.
             * This can happen on first creation or when the old reference doesn't exist.
             * We still need to propagate status if there's a mismatch.
             */
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
                    // Create with newLinkedEntityMainEntity only, oldLinkedEntity is null
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
            /*
             * The relation entity doesn't exist in DB.
             * Try to find existing or create new placeholder.
             */
            List<Object> results = EposDataModelDAO.getInstance()
                    .getOneFromDBByLinkedEntity(linkedEntity, clazz);

            if (!results.isEmpty()) {
                // Entity exists in database, use existing reference
                obj = linkedEntity;
            } else {
                // Entity doesn't exist, create a new placeholder
                obj = LinkedEntityAPI.createFromLinkedEntity(
                        linkedEntity,
                        mainEntity.getStatus(),
                        VersioningStatusAPI.retrieveVersioningStatus(mainEntity),
                        mainEntity.getFileProvenance()
                );
            }
        }

        // Final database lookup to return the actual entity object
        List<Object> results = EposDataModelDAO.getInstance()
                .getOneFromDBByLinkedEntity(obj, clazz);
        return results.isEmpty() ? null : results.get(0);
    }
}