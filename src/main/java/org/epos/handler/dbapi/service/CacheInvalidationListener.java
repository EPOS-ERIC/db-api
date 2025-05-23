package org.epos.handler.dbapi.service;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
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
 * - Deduplication to prevent multiple invalidations for the same entity
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
    private static final boolean deduplicationEnabled = isDeduplicationEnabled();
    private static final long deduplicationWindowMs = getDeduplicationWindow();

    // Deduplication cache to prevent multiple invalidations for the same entity
    private static final ConcurrentHashMap<String, Long> recentInvalidations = new ConcurrentHashMap<>();

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
     * Check if deduplication is enabled
     */
    private static boolean isDeduplicationEnabled() {
        String dedupEnv = System.getenv("ENABLE_CACHE_INVALIDATION_DEDUPLICATION");
        return dedupEnv == null || Boolean.parseBoolean(dedupEnv);
    }

    /**
     * Get deduplication window in milliseconds
     */
    private static long getDeduplicationWindow() {
        String windowEnv = System.getenv("CACHE_INVALIDATION_DEDUPLICATION_WINDOW_MS");
        return windowEnv != null ? Long.parseLong(windowEnv) : 1000L; // 1 second default
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
     * Main cache invalidation method with enhanced features and deduplication
     */
    private void invalidateCache(Object entity, String operation) {
        if (entity == null) {
            return;
        }

        String entityType = entity.getClass().getSimpleName();

        // Check for deduplication
        if (deduplicationEnabled && isRecentlyInvalidated(entityType)) {
            LOGGER.fine("Skipping cache invalidation for " + entityType + " - recently invalidated");
            return;
        }

        LOGGER.fine(operation + " operation detected on entity: " + entityType);

        try {
            // Mark as recently invalidated
            if (deduplicationEnabled) {
                markAsRecentlyInvalidated(entityType);
            }

            // Local cache invalidation
            invalidateLocalCache(entityType);

            // Invalidate related entities if enabled
            if (invalidateRelatedEntities) {
                invalidateRelatedEntityCaches(entity);
            }

            // Publish invalidation event to other services if enabled
            if (publishInvalidations && invalidationManager != null && invalidationManager.isEnabled()) {
                publishInvalidationEventAsync(entityType, operation);
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during cache invalidation for entity: " + entityType, e);
        }
    }

    /**
     * Check if an entity type was recently invalidated
     */
    private boolean isRecentlyInvalidated(String entityType) {
        Long lastInvalidation = recentInvalidations.get(entityType);
        if (lastInvalidation == null) {
            return false;
        }

        long currentTime = System.currentTimeMillis();
        boolean isRecent = (currentTime - lastInvalidation) < deduplicationWindowMs;

        // Clean up old entries
        if (!isRecent) {
            recentInvalidations.remove(entityType);
        }

        return isRecent;
    }

    /**
     * Mark an entity type as recently invalidated
     */
    private void markAsRecentlyInvalidated(String entityType) {
        recentInvalidations.put(entityType, System.currentTimeMillis());

        // Cleanup old entries periodically (keep map size reasonable)
        if (recentInvalidations.size() > 1000) {
            cleanupOldInvalidations();
        }
    }

    /**
     * Clean up old invalidation entries
     */
    private void cleanupOldInvalidations() {
        long currentTime = System.currentTimeMillis();
        recentInvalidations.entrySet().removeIf(entry ->
                (currentTime - entry.getValue()) > deduplicationWindowMs);
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
                            if (relatedEntityType != null && !isRecentlyInvalidated(relatedEntityType)) {
                                markAsRecentlyInvalidated(relatedEntityType);
                                invalidateLocalCache(relatedEntityType);

                                // Publish related entity invalidation
                                if (publishInvalidations && invalidationManager != null && invalidationManager.isEnabled()) {
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
                        LOGGER.log(Level.WARNING, "Failed to publish cache invalidation event for: " + entityType + " (operation: " + operation + "): " + ex.getMessage());
                    }
                    return null;
                });
    }

    /**
     * Manual cache invalidation method for programmatic use
     */
    public static void invalidateEntityCache(String entityType) {
        // Check deduplication
        if (deduplicationEnabled && instance.isRecentlyInvalidated(entityType)) {
            LOGGER.fine("Skipping manual cache invalidation for " + entityType + " - recently invalidated");
            return;
        }

        if (deduplicationEnabled) {
            instance.markAsRecentlyInvalidated(entityType);
        }

        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityType);
            LOGGER.info("Manually invalidated cache for entity type: " + entityType);
        }

        if (publishInvalidations && invalidationManager != null && invalidationManager.isEnabled()) {
            invalidationManager.publishInvalidationEvent(entityType)
                    .thenRun(() -> LOGGER.info("Published manual cache invalidation event for: " + entityType))
                    .exceptionally(ex -> {
                        LOGGER.log(Level.WARNING, "Failed to publish manual cache invalidation event: " + ex.getMessage());
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

    /**
     * Get deduplication statistics
     */
    public static java.util.Map<String, Object> getDeduplicationStatistics() {
        java.util.Map<String, Object> stats = new java.util.HashMap<>();
        stats.put("enabled", deduplicationEnabled);
        stats.put("windowMs", deduplicationWindowMs);
        stats.put("currentTrackedEntities", recentInvalidations.size());
        stats.put("recentInvalidations", new java.util.HashMap<>(recentInvalidations));
        return stats;
    }

    /**
     * Clear deduplication cache (for testing purposes)
     */
    public static void clearDeduplicationCache() {
        recentInvalidations.clear();
        LOGGER.info("Deduplication cache cleared");
    }

    // Singleton instance for accessing non-static methods
    private static final CacheInvalidationListener instance = new CacheInvalidationListener();
}