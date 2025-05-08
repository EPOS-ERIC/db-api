package org.epos.handler.dbapi.service;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import java.util.logging.Logger;

/**
 * JPA Entity Listener to invalidate cache when database changes occur.
 * This class should be registered with your entities using @EntityListeners annotation.
 * It also publishes cache invalidation events via RabbitMQ to notify other services.
 */
public class CacheInvalidationListener {
    private static final Logger LOGGER = Logger.getLogger(CacheInvalidationListener.class.getName());
    private static final RabbitMQCacheInvalidationManager invalidationManager =
            RabbitMQCacheInvalidationManager.getInstance();
    private static final boolean publishInvalidations = isPublishingEnabled();

    /**
     * Check if invalidation publishing is enabled based on environment variable
     */
    private static boolean isPublishingEnabled() {
        String publishEnv = System.getenv("ENABLE_CACHE_INVALIDATION_PUBLISHING");
        return publishEnv == null || Boolean.parseBoolean(publishEnv);
    }

    @PostPersist
    public void postPersist(Object entity) {
        invalidateCache(entity, "INSERT");
    }

    @PostUpdate
    public void postUpdate(Object entity) {
        invalidateCache(entity, "UPDATE");
    }

    @PostRemove
    public void postRemove(Object entity) {
        invalidateCache(entity, "DELETE");
    }

    private void invalidateCache(Object entity, String operation) {
        String entityType = entity.getClass().getSimpleName();
        LOGGER.info(operation + " operation detected on entity: " + entityType);

        // Get the cache service and invalidate entries for this entity type
        DatabaseCacheService cacheService = EntityManagerService.getCacheService();
        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityType);
        }

        // Publish invalidation event to other services if enabled
        if (publishInvalidations && invalidationManager != null) {
            invalidationManager.publishInvalidationEvent(entityType)
                    .thenRun(() -> LOGGER.info("Published cache invalidation event for: " + entityType))
                    .exceptionally(ex -> {
                        LOGGER.severe("Failed to publish cache invalidation event: " + ex.getMessage());
                        return null;
                    });
        }
    }
}