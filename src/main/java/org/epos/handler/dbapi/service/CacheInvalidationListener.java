package org.epos.handler.dbapi.service;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PreRemove;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Enhanced JPA Entity Listener to invalidate cache when database changes occur.
 * This class should be registered with your entities using @EntityListeners annotation.
 * It also publishes cache invalidation events via RabbitMQ to notify other services.
 *
 * Features:
 * - Granular invalidation based on entity relationships
 * - Async publishing with error handling
 * - Configurable through environment variables
 * - Support for related entity invalidation
 */
public class CacheInvalidationListener {
    private static final Logger LOGGER = Logger.getLogger(CacheInvalidationListener.class.getName());

    private static final RabbitMQCacheInvalidationManager invalidationManager =
            RabbitMQCacheInvalidationManager.getInstance();
    private static final DatabaseCacheService cacheService = EntityManagerService.getCacheService();

    // Configuration from environment variables
    private static final boolean publishInvalidations = isPublishingEnabled();
    private static final boolean invalidateRelatedEntities = isRelatedInvalidationEnabled();
    private static final long publishTimeout = getPublishTimeout();

    /**
     * Check if invalidation publishing is enabled based on environment variable
     */
    private static boolean isPublishingEnabled() {
        String publishEnv = System.getenv("ENABLE_CACHE_INVALIDATION_PUBLISHING");
        return publishEnv == null || Boolean.parseBoolean(publishEnv);
    }

    /**
     * Check if related entity invalidation is enabled
     */
    private static boolean isRelatedInvalidationEnabled() {
        String relatedEnv = System.getenv("ENABLE_RELATED_ENTITY_INVALIDATION");
        return relatedEnv == null || Boolean.parseBoolean(relatedEnv);
    }

    /**
     * Get publish timeout from environment
     */
    private static long getPublishTimeout() {
        String timeoutEnv = System.getenv("CACHE_INVALIDATION_TIMEOUT_MS");
        return timeoutEnv != null ? Long.parseLong(timeoutEnv) : 5000L; // 5 seconds default
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

    /**
     * Main cache invalidation method with enhanced features
     */
    private void invalidateCache(Object entity, String operation) {
        if (entity == null) {
            return;
        }

        String entityType = entity.getClass().getSimpleName();
        LOGGER.fine(operation + " operation detected on entity: " + entityType);

        try {
            // Local cache invalidation
            invalidateLocalCache(entityType);

            // Invalidate related entities if enabled
            if (invalidateRelatedEntities) {
                invalidateRelatedEntityCaches(entity);
            }

            // Publish invalidation event to other services if enabled
            if (publishInvalidations && invalidationManager != null) {
                publishInvalidationEventAsync(entityType, operation);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during cache invalidation for entity: " + entityType, e);
        }
    }

    /**
     * Invalidate local cache for the entity type
     */
    private void invalidateLocalCache(String entityType) {
        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityType);
            LOGGER.fine("Local cache invalidated for entity type: " + entityType);
        }
    }

    /**
     * Invalidate cache for related entities based on relationships
     */
    private void invalidateRelatedEntityCaches(Object entity) {
        try {
            Class<?> entityClass = entity.getClass();

            // Check all fields for relationships that might need cache invalidation
            for (Field field : entityClass.getDeclaredFields()) {
                field.setAccessible(true);

                // Check for JPA relationship annotations
                if (field.isAnnotationPresent(jakarta.persistence.OneToMany.class) ||
                        field.isAnnotationPresent(jakarta.persistence.ManyToOne.class) ||
                        field.isAnnotationPresent(jakarta.persistence.OneToOne.class) ||
                        field.isAnnotationPresent(jakarta.persistence.ManyToMany.class)) {

                    try {
                        Object relatedEntity = field.get(entity);
                        if (relatedEntity != null) {
                            String relatedEntityType = getEntityType(relatedEntity);
                            if (relatedEntityType != null) {
                                invalidateLocalCache(relatedEntityType);

                                // Publish related entity invalidation
                                if (publishInvalidations && invalidationManager != null) {
                                    publishInvalidationEventAsync(relatedEntityType, "RELATED");
                                }
                            }
                        }
                    } catch (IllegalAccessException e) {
                        LOGGER.log(Level.WARNING, "Could not access field " + field.getName() + " for related entity invalidation", e);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error invalidating related entity caches", e);
        }
    }

    /**
     * Get entity type from a related entity object
     */
    private String getEntityType(Object relatedEntity) {
        if (relatedEntity instanceof java.util.Collection) {
            // Handle collections - get the first element to determine type
            java.util.Collection<?> collection = (java.util.Collection<?>) relatedEntity;
            if (!collection.isEmpty()) {
                Object firstElement = collection.iterator().next();
                return firstElement.getClass().getSimpleName();
            }
        } else {
            return relatedEntity.getClass().getSimpleName();
        }
        return null;
    }

    /**
     * Publish invalidation event asynchronously with timeout and error handling
     */
    private void publishInvalidationEventAsync(String entityType, String operation) {
        CompletableFuture<Void> future = invalidationManager.publishInvalidationEvent(entityType);

        // Add timeout and completion handling
        future.orTimeout(publishTimeout, TimeUnit.MILLISECONDS)
                .thenRun(() -> LOGGER.fine("Published cache invalidation event for: " + entityType + " (operation: " + operation + ")"))
                .exceptionally(ex -> {
                    if (ex instanceof java.util.concurrent.TimeoutException) {
                        LOGGER.warning("Timeout publishing cache invalidation event for: " + entityType + " (operation: " + operation + ")");
                    } else {
                        LOGGER.log(Level.SEVERE, "Failed to publish cache invalidation event for: " + entityType + " (operation: " + operation + ")", ex);
                    }
                    return null;
                });
    }

    /**
     * Manual cache invalidation method for programmatic use
     */
    public static void invalidateEntityCache(String entityType) {
        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityType);
            LOGGER.info("Manually invalidated cache for entity type: " + entityType);
        }

        if (publishInvalidations && invalidationManager != null) {
            invalidationManager.publishInvalidationEvent(entityType)
                    .thenRun(() -> LOGGER.info("Published manual cache invalidation event for: " + entityType))
                    .exceptionally(ex -> {
                        LOGGER.log(Level.SEVERE, "Failed to publish manual cache invalidation event: " + ex.getMessage(), ex);
                        return null;
                    });
        }
    }

    /**
     * Invalidate cache by key pattern
     */
    public static void invalidateCacheByPattern(String keyPattern) {
        if (cacheService != null) {
            cacheService.invalidateByKeyPattern(keyPattern);
            LOGGER.info("Invalidated cache entries matching pattern: " + keyPattern);
        }
    }

    /**
     * Get cache statistics for monitoring
     */
    public static java.util.Map<String, Long> getCacheStatistics() {
        return cacheService != null ? cacheService.getStatistics() : null;
    }

    /**
     * Get invalidation manager status for health checks
     */
    public static boolean isInvalidationManagerHealthy() {
        return invalidationManager != null && invalidationManager.isConnected();
    }
}