package org.epos.handler.dbapi.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * A thread-safe cache service that manages database query results and provides
 * invalidation mechanisms for database changes.
 */
public class DatabaseCacheService {
    private static final Logger LOGGER = Logger.getLogger(DatabaseCacheService.class.getName());
    private static volatile DatabaseCacheService instance;
    private static final Object lock = new Object();

    // Cache storage using ConcurrentHashMap for thread safety
    private final Map<String, CacheEntry<?>> cache = new ConcurrentHashMap<>();

    // ReadWrite lock for cache operations
    private final ReadWriteLock cacheLock = new ReentrantReadWriteLock();

    // Cache expiration time in milliseconds
    private volatile long cacheExpirationTime = 60000; // Default 1 minute

    // Scheduler for periodic cache cleanup
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    // Cache statistics - using volatile for thread safety
    private volatile long hits = 0;
    private volatile long misses = 0;
    private volatile long invalidations = 0;
    private volatile long expirations = 0;

    // Cache entry class to store value and metadata
    private static class CacheEntry<T> {
        private final T value;
        private final long timestamp;
        private final String entityType;
        private final long accessTime;

        public CacheEntry(T value, String entityType) {
            this.value = value;
            this.timestamp = System.currentTimeMillis();
            this.entityType = entityType;
            this.accessTime = System.currentTimeMillis();
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

        public long getAccessTime() {
            return accessTime;
        }

        public boolean isExpired(long expirationTime) {
            return System.currentTimeMillis() - timestamp > expirationTime;
        }

        public CacheEntry<T> updateAccessTime() {
            return new CacheEntry<>(value, entityType);
        }
    }

    private DatabaseCacheService() {
        // Start periodic cache cleanup task
        scheduler.scheduleAtFixedRate(this::cleanupExpiredEntries, 30, 30, TimeUnit.SECONDS);

        // Start cache statistics logging
        scheduler.scheduleAtFixedRate(this::logCacheStatistics, 5, 5, TimeUnit.MINUTES);

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        LOGGER.info("DatabaseCacheService initialized with expiration time: " + cacheExpirationTime + "ms");
    }

    public static DatabaseCacheService getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new DatabaseCacheService();
                }
            }
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
        cacheLock.readLock().lock();
        try {
            CacheEntry<?> entry = cache.get(key);

            if (entry != null && !entry.isExpired(cacheExpirationTime)) {
                incrementHits();
                LOGGER.fine("Cache hit for key: " + key);
                // Update access time for LRU tracking
                cache.put(key, entry.updateAccessTime());
                return (T) entry.getValue();
            }
        } finally {
            cacheLock.readLock().unlock();
        }

        // Cache miss or expired entry - need write lock for computation
        cacheLock.writeLock().lock();
        try {
            // Double-check pattern - another thread might have computed the value
            CacheEntry<?> entry = cache.get(key);
            if (entry != null && !entry.isExpired(cacheExpirationTime)) {
                incrementHits();
                LOGGER.fine("Cache hit (after double-check) for key: " + key);
                return (T) entry.getValue();
            }

            // Actually compute the value
            if (entry == null) {
                incrementMisses();
                LOGGER.fine("Cache miss for key: " + key);
            } else {
                incrementExpirations();
                LOGGER.fine("Cache entry expired for key: " + key);
                cache.remove(key); // Remove expired entry
            }

            T value = supplier.get();
            if (value != null) {
                cache.put(key, new CacheEntry<>(value, entityType));
                LOGGER.fine("Cached value for key: " + key);
            }
            return value;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error computing cached value for key: " + key, e);
            throw e;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Invalidate cache entries for a specific entity type
     *
     * @param entityType The entity type to invalidate
     */
    public void invalidateByEntityType(String entityType) {
        cacheLock.writeLock().lock();
        try {
            int count = 0;
            var iterator = cache.entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entityType.equals(entry.getValue().getEntityType())) {
                    iterator.remove();
                    count++;
                }
            }

            if (count > 0) {
                invalidations += count;
                LOGGER.info("Invalidated " + count + " cache entries for entity type: " + entityType);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Invalidate cache entries by key pattern
     *
     * @param keyPattern The key pattern to match (supports wildcards)
     */
    public void invalidateByKeyPattern(String keyPattern) {
        cacheLock.writeLock().lock();
        try {
            int count = 0;
            var iterator = cache.entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getKey().matches(keyPattern.replace("*", ".*"))) {
                    iterator.remove();
                    count++;
                }
            }

            if (count > 0) {
                invalidations += count;
                LOGGER.info("Invalidated " + count + " cache entries matching pattern: " + keyPattern);
            }
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Invalidate all cache entries
     */
    public void invalidateAll() {
        cacheLock.writeLock().lock();
        try {
            int size = cache.size();
            cache.clear();
            invalidations += size;
            LOGGER.info("Invalidated all cache entries: " + size);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Clean up expired cache entries
     */
    private void cleanupExpiredEntries() {
        cacheLock.writeLock().lock();
        try {
            int count = 0;
            long currentTime = System.currentTimeMillis();
            var iterator = cache.entrySet().iterator();

            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().isExpired(cacheExpirationTime)) {
                    iterator.remove();
                    count++;
                }
            }

            if (count > 0) {
                expirations += count;
                LOGGER.fine("Cleaned up " + count + " expired cache entries");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error during cache cleanup", e);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Log cache statistics periodically
     */
    private void logCacheStatistics() {
        try {
            Map<String, Long> stats = getStatistics();
            LOGGER.info(String.format("Cache Statistics - Size: %d, Hits: %d, Misses: %d, Hit Rate: %.2f%%, Invalidations: %d, Expirations: %d",
                    stats.get("size"),
                    stats.get("hits"),
                    stats.get("misses"),
                    calculateHitRate(),
                    stats.get("invalidations"),
                    stats.get("expirations")));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error logging cache statistics", e);
        }
    }

    /**
     * Calculate hit rate percentage
     */
    private double calculateHitRate() {
        long totalRequests = hits + misses;
        return totalRequests > 0 ? (double) hits / totalRequests * 100.0 : 0.0;
    }

    /**
     * Thread-safe increment methods for statistics
     */
    private void incrementHits() {
        synchronized (this) {
            hits++;
        }
    }

    private void incrementMisses() {
        synchronized (this) {
            misses++;
        }
    }

    private void incrementExpirations() {
        synchronized (this) {
            expirations++;
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Long> getStatistics() {
        cacheLock.readLock().lock();
        try {
            Map<String, Long> stats = new ConcurrentHashMap<>();
            stats.put("hits", hits);
            stats.put("misses", misses);
            stats.put("invalidations", invalidations);
            stats.put("expirations", expirations);
            stats.put("size", (long) cache.size());
            stats.put("hitRate", Math.round(calculateHitRate()));
            return stats;
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Get cache size
     */
    public int size() {
        return cache.size();
    }

    /**
     * Check if cache contains a key
     */
    public boolean containsKey(String key) {
        cacheLock.readLock().lock();
        try {
            CacheEntry<?> entry = cache.get(key);
            return entry != null && !entry.isExpired(cacheExpirationTime);
        } finally {
            cacheLock.readLock().unlock();
        }
    }

    /**
     * Remove a specific cache entry
     */
    public boolean remove(String key) {
        cacheLock.writeLock().lock();
        try {
            CacheEntry<?> removed = cache.remove(key);
            if (removed != null) {
                invalidations++;
                LOGGER.fine("Removed cache entry for key: " + key);
                return true;
            }
            return false;
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    /**
     * Shutdown the cache service
     */
    public void shutdown() {
        LOGGER.info("Shutting down DatabaseCacheService...");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    LOGGER.warning("Cache service scheduler did not terminate gracefully");
                }
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        // Clear cache
        invalidateAll();

        LOGGER.info("DatabaseCacheService shutdown completed");
    }
}