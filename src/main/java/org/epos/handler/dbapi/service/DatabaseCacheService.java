package org.epos.handler.dbapi.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

/**
 * A simple cache service that manages database query results and provides
 * invalidation mechanisms for database changes.
 */
public class DatabaseCacheService {
    private static final Logger LOGGER = Logger.getLogger(DatabaseCacheService.class.getName());
    private static DatabaseCacheService instance;

    // Cache storage using ConcurrentHashMap for thread safety
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    // Cache expiration time in milliseconds
    private long cacheExpirationTime = 60000; // Default 1 minute

    // Scheduler for periodic cache cleanup
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    // Cache statistics
    private long hits = 0;
    private long misses = 0;
    private long invalidations = 0;
    private long expirations = 0;

    // Cache entry class to store value and metadata
    private static class CacheEntry<T> {
        private final T value;
        private final long timestamp;
        private final String entityType;

        public CacheEntry(T value, String entityType) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.entityType = entityType;
        }

        public T getValue() {
            return value;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public String getEntityType() {
            return entityType;
        }

        public boolean isExpired(long expirationTime) {
            return System.currentTimeMillis() - timestamp > expirationTime;
        }
    }

    private DatabaseCacheService() {
        // Start periodic cache cleanup task
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 30, 30, TimeUnit.SECONDS);
    }

    public static synchronized DatabaseCacheService getInstance() {
        if (instance == null) {
            instance = new DatabaseCacheService();
        }
        return instance;
    }

    /**
     * Set the cache expiration time in milliseconds
     *
     * @param expirationTime The expiration time in milliseconds
     */
    public void setCacheExpirationTime(long expirationTime) {
        this.cacheExpirationTime = expirationTime;
        LOGGER.info("Cache expiration time set to " + expirationTime + " ms");
    }

    /**
     * Get a value from cache or compute it if not present or expired
     *
     * @param key Cache key (typically query string or identifier)
     * @param entityType Type of entity being cached (used for invalidation)
     * @param supplier Function to compute value if not in cache
     * @return The cached or computed value
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrCompute(String key, String entityType, Supplier<T> supplier) {
        CacheEntry<?> entry = cache.get(key);

        if (entry != null && !entry.isExpired(cacheExpirationTime)) {
            hits++;
            LOGGER.fine("Cache hit for key: " + key);
            return (T) entry.getValue();
        }

        // Cache miss or expired entry
        if (entry == null) {
            misses++;
            LOGGER.fine("Cache miss for key: " + key);
        } else {
            expirations++;
            LOGGER.fine("Cache entry expired for key: " + key);
        }

        T value = supplier.get();
        cache.put(key, new CacheEntry<>(value, entityType));
        return value;
    }

    /**
     * Invalidate cache entries for a specific entity type
     *
     * @param entityType The entity type to invalidate
     */
    public void invalidateByEntityType(String entityType) {
        int count = 0;
        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            if (entityType.equals(entry.getValue().getEntityType())) {
                cache.remove(entry.getKey());
                count++;
            }
        }
        invalidations += count;
        LOGGER.info("Invalidated " + count + " cache entries for entity type: " + entityType);
    }

    /**
     * Invalidate all cache entries
     */
    public void invalidateAll() {
        int size = cache.size();
        cache.clear();
        invalidations += size;
        LOGGER.info("Invalidated all cache entries: " + size);
    }

    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredEntries() {
        int count = 0;
        for (Map.Entry<String, CacheEntry<?>> entry : cache.entrySet()) {
            if (entry.getValue().isExpired(cacheExpirationTime)) {
                cache.remove(entry.getKey());
                count++;
            }
        }
        if (count > 0) {
            expirations += count;
            LOGGER.fine("Cleaned up " + count + " expired cache entries");
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Long> getStatistics() {
        Map<String, Long> stats = new ConcurrentHashMap<>();
        stats.put("hits", hits);
        stats.put("misses", misses);
        stats.put("invalidations", invalidations);
        stats.put("expirations", expirations);
        stats.put("size", (long) cache.size());
        return stats;
    }

    /**
     * Shutdown the cache service
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}