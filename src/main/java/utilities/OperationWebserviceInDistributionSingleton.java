package utilities;

import org.epos.eposdatamodel.LinkedEntity;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe singleton for managing Operation-Webservice relations in Distribution context.
 * Uses ConcurrentHashMap for safe concurrent access and atomic operations.
 */
public class OperationWebserviceInDistributionSingleton {

    // Thread-safe eager initialization (no synchronization needed for getInstance)
    private static final OperationWebserviceInDistributionSingleton INSTANCE = 
            new OperationWebserviceInDistributionSingleton();

    // Thread-safe map for concurrent access
    private final ConcurrentHashMap<LinkedEntity, LinkedEntity> relations;

    public static OperationWebserviceInDistributionSingleton getInstance() {
        return INSTANCE;
    }

    private OperationWebserviceInDistributionSingleton() {
        relations = new ConcurrentHashMap<>();
    }

    /**
     * Creates a relation between source and target entities.
     * Thread-safe: uses ConcurrentHashMap.put()
     */
    public void createRelation(LinkedEntity source, LinkedEntity target) {
        if (source != null && target != null) {
            relations.put(source, target);
        }
    }

    /**
     * Atomically retrieves and removes the target for the given source.
     * Thread-safe: uses ConcurrentHashMap.remove() which is atomic.
     * 
     * @param source the source entity
     * @return the target entity, or null if no mapping exists
     */
    public LinkedEntity getTarget(LinkedEntity source) {
        if (source == null) {
            return null;
        }
        // Atomic get-and-remove operation
        return relations.remove(source);
    }

    /**
     * Checks if a relation exists for the given source without removing it.
     * 
     * @param source the source entity
     * @return true if a relation exists
     */
    public boolean hasRelation(LinkedEntity source) {
        return source != null && relations.containsKey(source);
    }

    /**
     * Returns the current number of relations.
     * Useful for debugging and monitoring.
     */
    public int size() {
        return relations.size();
    }

    /**
     * Clears all relations.
     * Use with caution - primarily for testing purposes.
     */
    public void clear() {
        relations.clear();
    }
}
