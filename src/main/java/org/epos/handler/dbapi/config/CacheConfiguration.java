package org.epos.handler.dbapi.config;

import java.util.logging.Logger;

/**
 * Centralized configuration class for cache and RabbitMQ settings.
 * This class reads all configuration from environment variables and provides
 * default values where appropriate.
 */
public class CacheConfiguration {
    private static final Logger LOGGER = Logger.getLogger(CacheConfiguration.class.getName());

    // Cache Configuration
    public static final boolean CACHE_ENABLED = Boolean.parseBoolean(
            System.getenv().getOrDefault("CACHE_ENABLED", "true"));

    public static final long CACHE_REFRESH_INTERVAL = Long.parseLong(
            System.getenv().getOrDefault("CACHE_REFRESH_INTERVAL", "60000"));

    public static final int CACHE_MAX_SIZE = Integer.parseInt(
            System.getenv().getOrDefault("CACHE_MAX_SIZE", "10000"));

    public static final boolean CACHE_STATISTICS_ENABLED = Boolean.parseBoolean(
            System.getenv().getOrDefault("CACHE_STATISTICS_ENABLED", "true"));

    public static final long CACHE_STATISTICS_INTERVAL = Long.parseLong(
            System.getenv().getOrDefault("CACHE_STATISTICS_INTERVAL", "300000")); // 5 minutes

    // RabbitMQ Configuration
    public static final boolean RABBITMQ_ENABLED = Boolean.parseBoolean(
            System.getenv().getOrDefault("RABBITMQ_ENABLED", "true"));

    public static final String BROKER_HOST = System.getenv().getOrDefault("BROKER_HOST", "localhost");

    public static final int BROKER_PORT = Integer.parseInt(
            System.getenv().getOrDefault("BROKER_PORT", "5672"));

    public static final String BROKER_USERNAME = System.getenv().getOrDefault("BROKER_USERNAME", "guest");

    public static final String BROKER_PASSWORD = System.getenv().getOrDefault("BROKER_PASSWORD", "guest");

    public static final String BROKER_VHOST = System.getenv().getOrDefault("BROKER_VHOST", "/");

    public static final int BROKER_MAX_RETRIES = Integer.parseInt(
            System.getenv().getOrDefault("BROKER_MAX_RETRIES", "3"));

    public static final long BROKER_RETRY_DELAY_MS = Long.parseLong(
            System.getenv().getOrDefault("BROKER_RETRY_DELAY_MS", "5000"));

    public static final long BROKER_RECONNECT_DELAY_MS = Long.parseLong(
            System.getenv().getOrDefault("BROKER_RECONNECT_DELAY_MS", "10000"));

    public static final int BROKER_CONNECTION_TIMEOUT = Integer.parseInt(
            System.getenv().getOrDefault("BROKER_CONNECTION_TIMEOUT", "10000"));

    public static final int BROKER_HEARTBEAT = Integer.parseInt(
            System.getenv().getOrDefault("BROKER_HEARTBEAT", "30"));

    // Cache Invalidation Configuration
    public static final boolean ENABLE_CACHE_INVALIDATION_PUBLISHING = Boolean.parseBoolean(
            System.getenv().getOrDefault("ENABLE_CACHE_INVALIDATION_PUBLISHING", "true"));

    public static final boolean ENABLE_RELATED_ENTITY_INVALIDATION = Boolean.parseBoolean(
            System.getenv().getOrDefault("ENABLE_RELATED_ENTITY_INVALIDATION", "true"));

    public static final long CACHE_INVALIDATION_TIMEOUT_MS = Long.parseLong(
            System.getenv().getOrDefault("CACHE_INVALIDATION_TIMEOUT_MS", "5000"));

    // Service Configuration
    public static final String SERVICE_NAME = System.getenv().getOrDefault("SERVICE_NAME", "unknown");

    public static final String SERVICE_INSTANCE_ID = System.getenv().getOrDefault("SERVICE_INSTANCE_ID",
            SERVICE_NAME + "-" + System.currentTimeMillis());

    // Database Configuration
    public static final String PERSISTENCE_NAME = System.getenv().getOrDefault("PERSISTENCE_NAME", "EPOSDataModel");

    public static final int CONNECTION_POOL_MAX_SIZE = Integer.parseInt(
            System.getenv().getOrDefault("CONNECTION_POOL_MAX_SIZE", "10"));

    public static final long CONNECTION_MAX_LIFETIME = Long.parseLong(
            System.getenv().getOrDefault("CONNECTION_MAX_LIFETIME", "40000"));

    public static final long CONNECTION_TEST_IDLE_INTERVAL_TIME = Long.parseLong(
            System.getenv().getOrDefault("CONNECTION_TEST_IDLE_INTERVAL_TIME", "30000"));

    // PostgreSQL Configuration
    public static final String POSTGRESQL_CONNECTION_STRING = System.getenv("POSTGRESQL_CONNECTION_STRING");
    public static final String POSTGRESQL_HOST = System.getenv("POSTGRESQL_HOST");
    public static final String POSTGRESQL_DBNAME = System.getenv("POSTGRESQL_DBNAME");
    public static final String POSTGRESQL_USERNAME = System.getenv("POSTGRESQL_USERNAME");
    public static final String POSTGRESQL_PASSWORD = System.getenv("POSTGRESQL_PASSWORD");

    static {
        logConfiguration();
    }

    /**
     * Log the current configuration for debugging purposes
     */
    private static void logConfiguration() {
        LOGGER.info("=== Cache Configuration ===");
        LOGGER.info("Cache Enabled: " + CACHE_ENABLED);
        LOGGER.info("Cache Refresh Interval: " + CACHE_REFRESH_INTERVAL + "ms");
        LOGGER.info("Cache Max Size: " + CACHE_MAX_SIZE);
        LOGGER.info("Cache Statistics Enabled: " + CACHE_STATISTICS_ENABLED);

        LOGGER.info("=== RabbitMQ Configuration ===");
        LOGGER.info("RabbitMQ Enabled: " + RABBITMQ_ENABLED);
        LOGGER.info("Broker Host: " + BROKER_HOST + ":" + BROKER_PORT);
        LOGGER.info("Broker VHost: " + BROKER_VHOST);
        LOGGER.info("Max Retries: " + BROKER_MAX_RETRIES);
        LOGGER.info("Retry Delay: " + BROKER_RETRY_DELAY_MS + "ms");

        LOGGER.info("=== Service Configuration ===");
        LOGGER.info("Service Name: " + SERVICE_NAME);
        LOGGER.info("Service Instance ID: " + SERVICE_INSTANCE_ID);
        LOGGER.info("Cache Invalidation Publishing: " + ENABLE_CACHE_INVALIDATION_PUBLISHING);
        LOGGER.info("Related Entity Invalidation: " + ENABLE_RELATED_ENTITY_INVALIDATION);

        LOGGER.info("=== Database Configuration ===");
        LOGGER.info("Persistence Name: " + PERSISTENCE_NAME);
        LOGGER.info("Connection Pool Max Size: " + CONNECTION_POOL_MAX_SIZE);
        LOGGER.info("PostgreSQL Host: " + (POSTGRESQL_HOST != null ? POSTGRESQL_HOST : "from connection string"));
    }

    /**
     * Validate configuration and return any issues found
     */
    public static String validateConfiguration() {
        StringBuilder issues = new StringBuilder();

        if (CACHE_ENABLED && CACHE_REFRESH_INTERVAL <= 0) {
            issues.append("Cache refresh interval must be positive when cache is enabled.\n");
        }

        if (CACHE_ENABLED && CACHE_MAX_SIZE <= 0) {
            issues.append("Cache max size must be positive when cache is enabled.\n");
        }

        if (RABBITMQ_ENABLED && ENABLE_CACHE_INVALIDATION_PUBLISHING) {
            if (BROKER_HOST == null || BROKER_HOST.trim().isEmpty()) {
                issues.append("Broker host must be specified when RabbitMQ is enabled.\n");
            }

            if (BROKER_PORT <= 0 || BROKER_PORT > 65535) {
                issues.append("Broker port must be a valid port number.\n");
            }

            if (BROKER_MAX_RETRIES < 0) {
                issues.append("Broker max retries cannot be negative.\n");
            }
        }

        if (CONNECTION_POOL_MAX_SIZE <= 0) {
            issues.append("Connection pool max size must be positive.\n");
        }

        if (POSTGRESQL_CONNECTION_STRING == null &&
                (POSTGRESQL_HOST == null || POSTGRESQL_DBNAME == null ||
                        POSTGRESQL_USERNAME == null || POSTGRESQL_PASSWORD == null)) {
            issues.append("Either PostgreSQL connection string or individual connection parameters must be provided.\n");
        }

        return issues.length() > 0 ? issues.toString() : null;
    }

    /**
     * Get a summary of the current configuration
     */
    public static String getConfigurationSummary() {
        return String.format(
                "Cache: %s, RabbitMQ: %s, Service: %s, Publishing: %s",
                CACHE_ENABLED ? "enabled" : "disabled",
                RABBITMQ_ENABLED ? "enabled" : "disabled",
                SERVICE_NAME,
                ENABLE_CACHE_INVALIDATION_PUBLISHING ? "enabled" : "disabled"
        );
    }

    /**
     * Check if the configuration is valid for production use
     */
    public static boolean isProductionReady() {
        return validateConfiguration() == null &&
                CACHE_ENABLED &&
                RABBITMQ_ENABLED &&
                !SERVICE_NAME.equals("unknown") &&
                POSTGRESQL_CONNECTION_STRING != null ||
                (POSTGRESQL_HOST != null && POSTGRESQL_DBNAME != null);
    }
}