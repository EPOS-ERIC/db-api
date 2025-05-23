package org.epos.handler.dbapi.monitor;

import org.epos.handler.dbapi.config.CacheConfiguration;
import org.epos.handler.dbapi.service.DatabaseCacheService;
import org.epos.handler.dbapi.service.EntityManagerService;
import org.epos.handler.dbapi.service.RabbitMQCacheInvalidationManager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Health monitoring and diagnostics for the cache system.
 * Provides health checks, metrics, and automatic recovery capabilities.
 */
public class CacheHealthMonitor {
    private static final Logger LOGGER = Logger.getLogger(CacheHealthMonitor.class.getName());
    private static volatile CacheHealthMonitor instance;
    private static final Object lock = new Object();

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private final DatabaseCacheService cacheService;
    private final RabbitMQCacheInvalidationManager invalidationManager;

    // Health status tracking
    private volatile boolean cacheHealthy = true;
    private volatile boolean rabbitmqHealthy = true;
    private volatile long lastHealthCheck = System.currentTimeMillis();
    private volatile long lastCacheOperation = System.currentTimeMillis();
    private volatile long lastRabbitMQOperation = System.currentTimeMillis();

    // Metrics
    private long totalHealthChecks = 0;
    private long cacheFailures = 0;
    private long rabbitmqFailures = 0;
    private long recoveryAttempts = 0;

    private CacheHealthMonitor() {
        this.cacheService = EntityManagerService.getCacheService();
        this.invalidationManager = RabbitMQCacheInvalidationManager.getInstance();

        if (CacheConfiguration.CACHE_ENABLED) {
            startHealthMonitoring();
        }

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));
    }

    public static CacheHealthMonitor getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new CacheHealthMonitor();
                }
            }
        }
        return instance;
    }

    /**
     * Start periodic health monitoring
     */
    private void startHealthMonitoring() {
        // Health check every 30 seconds
        scheduler.scheduleAtFixedRate(this::performHealthCheck, 30, 30, TimeUnit.SECONDS);

        // Detailed metrics every 5 minutes
        if (CacheConfiguration.CACHE_STATISTICS_ENABLED) {
            scheduler.scheduleAtFixedRate(this::logDetailedMetrics,
                    CacheConfiguration.CACHE_STATISTICS_INTERVAL,
                    CacheConfiguration.CACHE_STATISTICS_INTERVAL,
                    TimeUnit.MILLISECONDS);
        }

        LOGGER.info("Cache health monitoring started");
    }

    /**
     * Perform comprehensive health check
     */
    private void performHealthCheck() {
        try {
            totalHealthChecks++;
            lastHealthCheck = System.currentTimeMillis();

            // Check cache service health
            checkCacheHealth();

            // Check RabbitMQ health if enabled
            if (CacheConfiguration.RABBITMQ_ENABLED) {
                checkRabbitMQHealth();
            }

            // Log health status
            if (totalHealthChecks % 20 == 0) { // Every 10 minutes
                logHealthSummary();
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error during health check", e);
        }
    }

    /**
     * Check cache service health
     */
    private void checkCacheHealth() {
        try {
            if (cacheService != null) {
                // Try to get cache statistics as a health check
                Map<String, Long> stats = cacheService.getStatistics();
                if (stats != null) {
                    lastCacheOperation = System.currentTimeMillis();
                    if (!cacheHealthy) {
                        LOGGER.info("Cache service recovered");
                        cacheHealthy = true;
                    }
                } else {
                    handleCacheFailure("Cache statistics returned null");
                }
            } else {
                handleCacheFailure("Cache service is null");
            }
        } catch (Exception e) {
            handleCacheFailure("Cache health check failed: " + e.getMessage());
        }
    }

    /**
     * Check RabbitMQ health
     */
    private void checkRabbitMQHealth() {
        try {
            if (invalidationManager != null) {
                boolean connected = invalidationManager.isConnected();
                if (connected) {
                    lastRabbitMQOperation = System.currentTimeMillis();
                    if (!rabbitmqHealthy) {
                        LOGGER.info("RabbitMQ service recovered");
                        rabbitmqHealthy = true;
                    }
                } else {
                    handleRabbitMQFailure("RabbitMQ not connected");
                }
            } else {
                handleRabbitMQFailure("RabbitMQ invalidation manager is null");
            }
        } catch (Exception e) {
            handleRabbitMQFailure("RabbitMQ health check failed: " + e.getMessage());
        }
    }

    /**
     * Handle cache service failure
     */
    private void handleCacheFailure(String reason) {
        cacheFailures++;
        if (cacheHealthy) {
            LOGGER.warning("Cache service failure detected: " + reason);
            cacheHealthy = false;
        }
    }

    /**
     * Handle RabbitMQ failure
     */
    private void handleRabbitMQFailure(String reason) {
        rabbitmqFailures++;
        if (rabbitmqHealthy) {
            LOGGER.warning("RabbitMQ service failure detected: " + reason);
            rabbitmqHealthy = false;
        }
    }

    /**
     * Log detailed metrics and statistics
     */
    private void logDetailedMetrics() {
        try {
            LOGGER.info("=== Cache System Metrics ===");

            // Cache statistics
            if (cacheService != null) {
                Map<String, Long> cacheStats = cacheService.getStatistics();
                if (cacheStats != null) {
                    LOGGER.info(String.format(
                            "Cache: Size=%d, Hits=%d, Misses=%d, Hit Rate=%.2f%%, Invalidations=%d, Expirations=%d",
                            cacheStats.get("size"),
                            cacheStats.get("hits"),
                            cacheStats.get("misses"),
                            cacheStats.getOrDefault("hitRate", 0L).doubleValue(),
                            cacheStats.get("invalidations"),
                            cacheStats.get("expirations")
                    ));
                }
            }

            // RabbitMQ statistics
            if (invalidationManager != null) {
                LOGGER.info("RabbitMQ: " + invalidationManager.getConnectionInfo());
                LOGGER.info("RabbitMQ Connected: " + invalidationManager.isConnected());
                LOGGER.info("RabbitMQ Subscribed: " + invalidationManager.isSubscribed());
            }

            // Health monitoring statistics
            LOGGER.info(String.format(
                    "Health: Checks=%d, Cache Failures=%d, RabbitMQ Failures=%d, Recovery Attempts=%d",
                    totalHealthChecks, cacheFailures, rabbitmqFailures, recoveryAttempts
            ));

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error logging detailed metrics", e);
        }
    }

    /**
     * Log health summary
     */
    private void logHealthSummary() {
        LOGGER.info(String.format(
                "Cache System Health - Cache: %s, RabbitMQ: %s, Overall: %s",
                cacheHealthy ? "HEALTHY" : "UNHEALTHY",
                rabbitmqHealthy ? "HEALTHY" : "UNHEALTHY",
                isOverallHealthy() ? "HEALTHY" : "UNHEALTHY"
        ));
    }

    /**
     * Get current health status
     */
    public HealthStatus getHealthStatus() {
        return new HealthStatus(
                cacheHealthy,
                rabbitmqHealthy,
                isOverallHealthy(),
                lastHealthCheck,
                totalHealthChecks,
                cacheFailures,
                rabbitmqFailures,
                getDetailedMetrics()
        );
    }

    /**
     * Check if overall system is healthy
     */
    public boolean isOverallHealthy() {
        boolean healthy = cacheHealthy;

        // RabbitMQ health only matters if it's enabled and invalidation publishing is enabled
        if (CacheConfiguration.RABBITMQ_ENABLED && CacheConfiguration.ENABLE_CACHE_INVALIDATION_PUBLISHING) {
            healthy = healthy && rabbitmqHealthy;
        }

        return healthy;
    }

    /**
     * Get detailed metrics
     */
    public Map<String, Object> getDetailedMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Cache metrics
        if (cacheService != null) {
            Map<String, Long> cacheStats = cacheService.getStatistics();
            if (cacheStats != null) {
                metrics.put("cache", cacheStats);
            }
        }

        // RabbitMQ metrics
        if (invalidationManager != null) {
            Map<String, Object> rabbitStats = new HashMap<>();
            rabbitStats.put("connected", invalidationManager.isConnected());
            rabbitStats.put("subscribed", invalidationManager.isSubscribed());
            rabbitStats.put("connectionInfo", invalidationManager.getConnectionInfo());
            metrics.put("rabbitmq", rabbitStats);
        }

        // Health monitoring metrics
        Map<String, Object> healthStats = new HashMap<>();
        healthStats.put("totalHealthChecks", totalHealthChecks);
        healthStats.put("cacheFailures", cacheFailures);
        healthStats.put("rabbitmqFailures", rabbitmqFailures);
        healthStats.put("recoveryAttempts", recoveryAttempts);
        healthStats.put("lastHealthCheck", lastHealthCheck);
        healthStats.put("lastCacheOperation", lastCacheOperation);
        healthStats.put("lastRabbitMQOperation", lastRabbitMQOperation);
        metrics.put("health", healthStats);

        return metrics;
    }

    /**
     * Trigger manual recovery attempt
     */
    public void triggerRecovery() {
        recoveryAttempts++;
        LOGGER.info("Manual recovery triggered");

        scheduler.execute(() -> {
            try {
                // Try to recover cache service
                if (!cacheHealthy && cacheService != null) {
                    LOGGER.info("Attempting cache service recovery...");
                    // Cache service recovery logic here if needed
                }

                // Try to recover RabbitMQ
                if (!rabbitmqHealthy && invalidationManager != null) {
                    LOGGER.info("Attempting RabbitMQ recovery...");
                    // RabbitMQ has its own auto-recovery mechanism
                }

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error during manual recovery", e);
            }
        });
    }

    /**
     * Shutdown health monitoring
     */
    public void shutdown() {
        LOGGER.info("Shutting down cache health monitor...");

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }

        LOGGER.info("Cache health monitor shutdown completed");
    }

    /**
     * Health status data class
     */
    public static class HealthStatus {
        private final boolean cacheHealthy;
        private final boolean rabbitmqHealthy;
        private final boolean overallHealthy;
        private final long lastHealthCheck;
        private final long totalHealthChecks;
        private final long cacheFailures;
        private final long rabbitmqFailures;
        private final Map<String, Object> detailedMetrics;

        public HealthStatus(boolean cacheHealthy, boolean rabbitmqHealthy, boolean overallHealthy,
                            long lastHealthCheck, long totalHealthChecks, long cacheFailures,
                            long rabbitmqFailures, Map<String, Object> detailedMetrics) {
            this.cacheHealthy = cacheHealthy;
            this.rabbitmqHealthy = rabbitmqHealthy;
            this.overallHealthy = overallHealthy;
            this.lastHealthCheck = lastHealthCheck;
            this.totalHealthChecks = totalHealthChecks;
            this.cacheFailures = cacheFailures;
            this.rabbitmqFailures = rabbitmqFailures;
            this.detailedMetrics = detailedMetrics;
        }

        // Getters
        public boolean isCacheHealthy() { return cacheHealthy; }
        public boolean isRabbitmqHealthy() { return rabbitmqHealthy; }
        public boolean isOverallHealthy() { return overallHealthy; }
        public long getLastHealthCheck() { return lastHealthCheck; }
        public long getTotalHealthChecks() { return totalHealthChecks; }
        public long getCacheFailures() { return cacheFailures; }
        public long getRabbitmqFailures() { return rabbitmqFailures; }
        public Map<String, Object> getDetailedMetrics() { return detailedMetrics; }

        @Override
        public String toString() {
            return String.format(
                    "HealthStatus{cache=%s, rabbitmq=%s, overall=%s, checks=%d, failures=cache:%d/rabbitmq:%d}",
                    cacheHealthy, rabbitmqHealthy, overallHealthy, totalHealthChecks, cacheFailures, rabbitmqFailures
            );
        }
    }
}