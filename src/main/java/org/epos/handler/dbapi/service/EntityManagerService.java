package org.epos.handler.dbapi.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.epos.handler.dbapi.config.CacheConfiguration;
import org.epos.handler.dbapi.monitor.CacheHealthMonitor;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.logging.Logger;

public class EntityManagerService {
    private static final Logger LOGGER = Logger.getLogger(EntityManagerService.class.getName());

    private static volatile EntityManagerFactory instance;
    private static volatile DatabaseCacheService cacheService;
    private static volatile CacheHealthMonitor healthMonitor;
    private static final Object lock = new Object();

    private String persistenceName;
    private String poolMaxSize;
    private String maxConnectionLifetime;
    private String keepAliveTime;
    private String cacheEnabled;
    private String cacheRefreshInterval;

    private String connectionString;
    private String postgresqlHost;
    private String postgresqlDBName;
    private String postgresqlUsername;
    private String postgresqlPassword;

    private EntityManagerService(EntityManagerServiceBuilder entityManagerServiceBuilder) {
        this.persistenceName = entityManagerServiceBuilder.persistenceName;
        this.poolMaxSize = entityManagerServiceBuilder.poolMaxSize;
        this.maxConnectionLifetime = entityManagerServiceBuilder.maxConnectionLifetime;
        this.keepAliveTime = entityManagerServiceBuilder.keepAliveTime;
        this.cacheEnabled = entityManagerServiceBuilder.cacheEnabled;
        this.cacheRefreshInterval = entityManagerServiceBuilder.cacheRefreshInterval;

        // Setup HikariCP configuration
        HikariConfig hikariConfig = createHikariConfig(entityManagerServiceBuilder);
        DataSource hikariDataSource = new HikariDataSource(hikariConfig);

        // Setup JPA properties
        HashMap<String, Object> properties = createJPAProperties(hikariDataSource);

        // Initialize cache service if enabled
        if (Boolean.parseBoolean(cacheEnabled)) {
            initializeCacheService();
        }

        // Create EntityManagerFactory
        instance = Persistence.createEntityManagerFactory(persistenceName, properties);

        // Initialize health monitoring only if cache is enabled
        if (Boolean.parseBoolean(cacheEnabled)) {
            healthMonitor = CacheHealthMonitor.getInstance();
        }

        // Register shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        LOGGER.info("EntityManagerService initialized successfully");
    }

    private HikariConfig createHikariConfig(EntityManagerServiceBuilder builder) {
        HikariConfig hikariConfig = new HikariConfig();

        // Basic pool configuration
        hikariConfig.setMaximumPoolSize(Integer.parseInt(poolMaxSize));
        hikariConfig.setMaxLifetime(Long.parseLong(maxConnectionLifetime));
        hikariConfig.setKeepaliveTime(Long.parseLong(keepAliveTime));
        hikariConfig.setAutoCommit(false);

        // Driver and pool settings
        String serviceName = System.getenv().getOrDefault("SERVICE_NAME", "test-service");
        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setPoolName("metadata_catalogue_" + serviceName + "_" + System.currentTimeMillis());
        hikariConfig.setConnectionTimeout(10000);
        hikariConfig.setInitializationFailTimeout(9000);

        // Performance optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("useLocalSessionState", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");
        hikariConfig.addDataSourceProperty("cacheResultSetMetadata", "true");
        hikariConfig.addDataSourceProperty("cacheServerConfiguration", "true");
        hikariConfig.addDataSourceProperty("elideSetAutoCommits", "true");
        hikariConfig.addDataSourceProperty("maintainTimeStats", "false");

        // Database connection setup
        setupDatabaseConnection(hikariConfig, builder);

        return hikariConfig;
    }

    private void setupDatabaseConnection(HikariConfig hikariConfig, EntityManagerServiceBuilder builder) {
        this.connectionString = builder.connectionString;
        this.postgresqlHost = builder.postgresqlHost;
        this.postgresqlDBName = builder.postgresqlDBName;
        this.postgresqlUsername = builder.postgresqlUsername;
        this.postgresqlPassword = builder.postgresqlPassword;

        if (connectionString != null && !connectionString.trim().isEmpty()) {
            hikariConfig.setJdbcUrl(connectionString);
            LOGGER.info("Using connection string for database connection");
        } else {
            String dburl = "jdbc:postgresql://" + postgresqlHost + "/" + postgresqlDBName;
            hikariConfig.setJdbcUrl(dburl);
            hikariConfig.setUsername(postgresqlUsername);
            hikariConfig.setPassword(postgresqlPassword);
            LOGGER.info("Using individual connection parameters for database connection to: " + postgresqlHost);
        }
    }

    private HashMap<String, Object> createJPAProperties(DataSource dataSource) {
        HashMap<String, Object> properties = new HashMap<>();
        properties.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, dataSource);

        // Configure EclipseLink cache settings
        if (Boolean.parseBoolean(cacheEnabled)) {
            // Enable JPA shared cache mode
            properties.put(PersistenceUnitProperties.SHARED_CACHE_MODE, "ENABLE_SELECTIVE");

            // Set cache type and size
            properties.put(PersistenceUnitProperties.CACHE_TYPE_DEFAULT, "SOFT");
            properties.put(PersistenceUnitProperties.CACHE_SIZE_DEFAULT, "1000"); // Smaller for tests

            // Enable prepared statement caching
            properties.put(PersistenceUnitProperties.CACHE_STATEMENTS, "true");
            properties.put(PersistenceUnitProperties.CACHE_STATEMENTS_SIZE, "100"); // Smaller for tests

            // Disable weaving to avoid issues
            properties.put(PersistenceUnitProperties.WEAVING, "false");

            // Logging configuration
            properties.put(PersistenceUnitProperties.LOGGING_LEVEL, "INFO");
            properties.put(PersistenceUnitProperties.LOGGING_LOGGER, "JavaLogger");

            LOGGER.info("JPA cache enabled");
        } else {
            // Disable caching completely
            properties.put(PersistenceUnitProperties.SHARED_CACHE_MODE, "NONE");
            properties.put(PersistenceUnitProperties.WEAVING, "false");
            LOGGER.info("JPA cache disabled");
        }

        return properties;
    }

    private void initializeCacheService() {
        cacheService = DatabaseCacheService.getInstance();
        long refreshInterval = Long.parseLong(cacheRefreshInterval);
        cacheService.setCacheExpirationTime(refreshInterval);
        LOGGER.info("Cache service initialized with refresh interval: " + refreshInterval + "ms");
    }

    public static class EntityManagerServiceBuilder {
        private String persistenceName;
        private String poolMaxSize;
        private String maxConnectionLifetime;
        private String keepAliveTime;
        private String cacheEnabled;
        private String cacheRefreshInterval;

        private String connectionString;
        private String postgresqlHost;
        private String postgresqlDBName;
        private String postgresqlUsername;
        private String postgresqlPassword;

        // Flag to indicate if this is being used in tests
        private boolean isTestMode = false;

        public EntityManagerServiceBuilder() {
            // Initialize with defaults, prioritizing environment variables but with fallbacks
            this.persistenceName = getEnvOrDefault("PERSISTENCE_NAME", "EPOSDataModel");
            this.poolMaxSize = getEnvOrDefault("CONNECTION_POOL_MAX_SIZE", "10");
            this.maxConnectionLifetime = getEnvOrDefault("CONNECTION_MAX_LIFETIME", "40000");
            this.keepAliveTime = getEnvOrDefault("CONNECTION_TEST_IDLE_INTERVAL_TIME", "30000");
            this.cacheEnabled = getEnvOrDefault("CACHE_ENABLED", "true");
            this.cacheRefreshInterval = getEnvOrDefault("CACHE_REFRESH_INTERVAL", "60000");

            // Database connection from environment (can be overridden by setters)
            this.connectionString = System.getenv("POSTGRESQL_CONNECTION_STRING");
            this.postgresqlHost = System.getenv("POSTGRESQL_HOST");
            this.postgresqlDBName = System.getenv("POSTGRESQL_DBNAME");
            this.postgresqlUsername = System.getenv("POSTGRESQL_USERNAME");
            this.postgresqlPassword = System.getenv("POSTGRESQL_PASSWORD");
        }

        /**
         * Get environment variable or default value
         */
        private String getEnvOrDefault(String envVar, String defaultValue) {
            String value = System.getenv(envVar);
            return value != null ? value : defaultValue;
        }

        /**
         * Enable test mode - this disables some production features like health monitoring
         */
        public EntityManagerServiceBuilder enableTestMode() {
            this.isTestMode = true;
            // Override some defaults for testing
            this.cacheRefreshInterval = "30000"; // Shorter cache time for tests
            this.poolMaxSize = "5"; // Smaller pool for tests
            return this;
        }

        public EntityManagerServiceBuilder setPersistenceName(String persistenceName) {
            this.persistenceName = persistenceName;
            return this;
        }

        public EntityManagerServiceBuilder setPoolMaxSize(String poolMaxSize) {
            this.poolMaxSize = poolMaxSize;
            return this;
        }

        public EntityManagerServiceBuilder setMaxConnectionLifetime(String maxConnectionLifetime) {
            this.maxConnectionLifetime = maxConnectionLifetime;
            return this;
        }

        public EntityManagerServiceBuilder setKeepAliveTime(String keepAliveTime) {
            this.keepAliveTime = keepAliveTime;
            return this;
        }

        public EntityManagerServiceBuilder setCacheEnabled(String cacheEnabled) {
            this.cacheEnabled = cacheEnabled;
            return this;
        }

        public EntityManagerServiceBuilder setCacheRefreshInterval(String cacheRefreshInterval) {
            this.cacheRefreshInterval = cacheRefreshInterval;
            return this;
        }

        public EntityManagerServiceBuilder setConnectionString(String connectionString) {
            this.connectionString = connectionString;
            return this;
        }

        public EntityManagerServiceBuilder setPostgresqlHost(String postgresqlHost) {
            this.postgresqlHost = postgresqlHost;
            return this;
        }

        public EntityManagerServiceBuilder setPostgresqlDBName(String postgresqlDBName) {
            this.postgresqlDBName = postgresqlDBName;
            return this;
        }

        public EntityManagerServiceBuilder setPostgresqlUsername(String postgresqlUsername) {
            this.postgresqlUsername = postgresqlUsername;
            return this;
        }

        public EntityManagerServiceBuilder setPostgresqlPassword(String postgresqlPassword) {
            this.postgresqlPassword = postgresqlPassword;
            return this;
        }

        public EntityManagerService build() {
            // Validate that we have either connection string or individual parameters
            if ((connectionString == null || connectionString.trim().isEmpty()) &&
                    (postgresqlHost == null || postgresqlDBName == null ||
                            postgresqlUsername == null || postgresqlPassword == null)) {

                // Only throw exception if we're not in test mode and missing required config
                if (!isTestMode) {
                    String validationError = validateProductionConfiguration();
                    if (validationError != null) {
                        throw new IllegalStateException("Invalid configuration: " + validationError);
                    }
                } else {
                    LOGGER.info("Test mode enabled - skipping production configuration validation");
                }
            }

            return new EntityManagerService(this);
        }

        /**
         * Validate configuration for production use
         */
        private String validateProductionConfiguration() {
            StringBuilder issues = new StringBuilder();

            if (connectionString == null &&
                    (postgresqlHost == null || postgresqlDBName == null ||
                            postgresqlUsername == null || postgresqlPassword == null)) {
                issues.append("Either PostgreSQL connection string or individual connection parameters must be provided.\n");
            }

            try {
                int poolSize = Integer.parseInt(poolMaxSize);
                if (poolSize <= 0) {
                    issues.append("Connection pool max size must be positive.\n");
                }
            } catch (NumberFormatException e) {
                issues.append("Connection pool max size must be a valid number.\n");
            }

            return issues.length() > 0 ? issues.toString() : null;
        }
    }

    public static synchronized EntityManagerFactory getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    LOGGER.warning("EntityManagerFactory not initialized. Creating with default configuration.");
                    new EntityManagerServiceBuilder().build();
                }
            }
        }
        return instance;
    }

    public static DatabaseCacheService getCacheService() {
        return cacheService;
    }

    public static CacheHealthMonitor getHealthMonitor() {
        return healthMonitor;
    }

    /**
     * Get service information for monitoring
     */
    public static String getServiceInfo() {
        StringBuilder info = new StringBuilder();
        info.append("EntityManagerService Status:\n");

        if (instance != null) {
            info.append("- EntityManagerFactory: ACTIVE\n");
        } else {
            info.append("- EntityManagerFactory: NOT INITIALIZED\n");
        }

        if (cacheService != null) {
            info.append("- Cache Service: ACTIVE (size: ").append(cacheService.size()).append(")\n");
        } else {
            info.append("- Cache Service: NOT ACTIVE\n");
        }

        if (healthMonitor != null) {
            info.append("- Health Monitor: ").append(healthMonitor.isOverallHealthy() ? "HEALTHY" : "UNHEALTHY").append("\n");
        } else {
            info.append("- Health Monitor: NOT ACTIVE\n");
        }

        return info.toString();
    }

    /**
     * Force cache clearing across all entity types
     */
    public static void clearAllCaches() {
        if (cacheService != null) {
            cacheService.invalidateAll();
            LOGGER.info("All caches cleared manually");
        }

        if (instance != null) {
            // Also clear JPA second-level cache
            instance.getCache().evictAll();
            LOGGER.info("JPA second-level cache cleared");
        }
    }

    /**
     * Get comprehensive health status
     */
    public static boolean isHealthy() {
        if (healthMonitor != null) {
            return healthMonitor.isOverallHealthy();
        }

        // Basic health check if monitor is not available
        boolean healthy = true;

        if (instance == null || !instance.isOpen()) {
            healthy = false;
        }

        return healthy;
    }

    /**
     * Shutdown the service gracefully
     */
    public void shutdown() {
        LOGGER.info("Shutting down EntityManagerService...");

        try {
            // Shutdown health monitor first
            if (healthMonitor != null) {
                healthMonitor.shutdown();
            }

            // Shutdown cache service
            if (cacheService != null) {
                cacheService.shutdown();
            }

            // Close EntityManagerFactory
            if (instance != null && instance.isOpen()) {
                instance.close();
            }

            LOGGER.info("EntityManagerService shutdown completed");
        } catch (Exception e) {
            LOGGER.severe("Error during EntityManagerService shutdown: " + e.getMessage());
        }
    }

    /**
     * Reinitialize the service (useful for configuration changes and tests)
     */
    public static synchronized void reinitialize() {
        LOGGER.info("Reinitializing EntityManagerService...");

        if (instance != null) {
            // Shutdown current instance
            if (instance.isOpen()) {
                instance.close();
            }
            instance = null;
        }

        if (cacheService != null) {
            cacheService.shutdown();
            cacheService = null;
        }

        if (healthMonitor != null) {
            healthMonitor.shutdown();
            healthMonitor = null;
        }

        LOGGER.info("EntityManagerService reinitialized successfully");
    }

    /**
     * Reset instance for tests - useful when you need to create a new instance with different configuration
     */
    public static synchronized void resetForTests() {
        reinitialize();
        LOGGER.info("EntityManagerService reset for tests");
    }
}