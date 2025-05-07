package org.epos.handler.dbapi.service;

import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;

import java.util.logging.Logger;

/**
 * JPA Entity Listener to invalidate cache when database changes occur.
 * This class should be registered with your entities using @EntityListeners annotation.
 */
public class CacheInvalidationListener {
    private static final Logger LOGGER = Logger.getLogger(CacheInvalidationListener.class.getName());

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
        DatabaseCacheService cacheService = DatabaseCacheService.getInstance();
        cacheService.invalidateByEntityType(entityType);
    }
}