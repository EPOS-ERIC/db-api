package org.epos.handler.dbapi.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.eclipse.persistence.config.PersistenceUnitProperties;

import javax.sql.DataSource;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EntityManagerService {

    private static final Logger LOG = Logger.getLogger(EntityManagerService.class.getName());

    private static EntityManagerFactory instance;
    private HikariDataSource hikariDataSource;

    private String persistenceName;
    private String poolMaxSize;
    private String maxConnectionLifetime;
    private String keepAliveTime;
    private String leakDetectionThreshold;

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
        this.leakDetectionThreshold = entityManagerServiceBuilder.leakDetectionThreshold;

        HikariConfig hikariConfig = new HikariConfig();
        HashMap<String, Object> properties = new HashMap<>();
        hikariConfig.setMaximumPoolSize(Integer.parseInt(poolMaxSize));
        hikariConfig.setMaxLifetime(Long.parseLong(maxConnectionLifetime));
        hikariConfig.setKeepaliveTime(Long.parseLong(keepAliveTime));
        hikariConfig.setLeakDetectionThreshold(Long.parseLong(leakDetectionThreshold));
        hikariConfig.setConnectionTimeout(30000); // 30 seconds to acquire connection from pool

        hikariConfig.setAutoCommit(false);

        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setPoolName("metadata_catalogue");
        hikariConfig.setInitializationFailTimeout(9000);

        // PostgreSQL specific optimizations
        hikariConfig.addDataSourceProperty("prepareThreshold", "5");
        hikariConfig.addDataSourceProperty("preparedStatementCacheQueries", "256");
        hikariConfig.addDataSourceProperty("preparedStatementCacheSizeMiB", "8");
        hikariConfig.addDataSourceProperty("reWriteBatchedInserts", "true");

        // Connection validation
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setValidationTimeout(5000);

        this.connectionString = entityManagerServiceBuilder.connectionString;
        this.postgresqlHost = entityManagerServiceBuilder.postgresqlHost;
        this.postgresqlDBName = entityManagerServiceBuilder.postgresqlDBName;
        this.postgresqlUsername = entityManagerServiceBuilder.postgresqlUsername;
        this.postgresqlPassword = entityManagerServiceBuilder.postgresqlPassword;

        if (connectionString != null) {
            // Apply dynamic DNS resolution to provided connection string
            try {
                String resolvedUrl = resolveConnectionString(connectionString);
                hikariConfig.setJdbcUrl(resolvedUrl);
                LOG.fine("Using resolved connection string: " + maskPassword(resolvedUrl));
            } catch (Exception e) {
                LOG.warning("Failed to resolve connection string, using as-is: " + e.getMessage());
                hikariConfig.setJdbcUrl(connectionString);
                LOG.fine("Using original connection string");
            }
        } else {
            // Build connection string with dynamic host resolution
            try {
                String dburl = buildDynamicJdbcUrl(postgresqlHost, postgresqlDBName);
                hikariConfig.setJdbcUrl(dburl);
                LOG.fine("Built dynamic JDBC URL: " + maskPassword(dburl));
            } catch (Exception e) {
                LOG.severe("Failed to build dynamic JDBC URL: " + e.getMessage());
                // Fallback to simple URL
                String dburl = "jdbc:postgresql://" + postgresqlHost + "/" + postgresqlDBName;
                hikariConfig.setJdbcUrl(dburl);
                LOG.warning("Using fallback JDBC URL: " + dburl);
            }
        }

        hikariConfig.setUsername(postgresqlUsername);
        hikariConfig.setPassword(postgresqlPassword);

        hikariDataSource = new HikariDataSource(hikariConfig);

        properties.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, hikariDataSource);
        instance = Persistence.createEntityManagerFactory(persistenceName, properties);

        LOG.fine("EntityManagerService initialized successfully");
    }

    /**
     * Resolves DNS for hostnames in an existing JDBC connection string.
     * Parses the connection string, resolves all hostnames to IPs, and rebuilds
     * the URL with primary targeting parameters.
     */
    private String resolveConnectionString(String jdbcUrl) throws Exception {
        LOG.fine("Resolving connection string: " + maskPassword(jdbcUrl));

        // Parse JDBC URL format: jdbc:postgresql://host1:port1,host2:port2/database?params
        if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
            throw new IllegalArgumentException("Invalid PostgreSQL JDBC URL format");
        }

        // Remove jdbc:postgresql:// prefix
        String remaining = jdbcUrl.substring("jdbc:postgresql://".length());

        // Split into hosts and rest (database + params)
        String[] parts = remaining.split("/", 2);
        if (parts.length < 2) {
            throw new IllegalArgumentException("JDBC URL missing database name");
        }

        String hostsString = parts[0];
        String databaseAndParams = parts[1];

        // Split database name and parameters
        String database;
        String existingParams = "";
        if (databaseAndParams.contains("?")) {
            String[] dbParts = databaseAndParams.split("\\?", 2);
            database = dbParts[0];
            existingParams = dbParts[1];
        } else {
            database = databaseAndParams;
        }

        // Parse and resolve each host
        String[] hosts = hostsString.split(",");
        StringBuilder resolvedHosts = new StringBuilder();

        for (int i = 0; i < hosts.length; i++) {
            String host = hosts[i].trim();
            String hostname;
            String port = "5432";

            // Check if host contains port
            if (host.contains(":")) {
                String[] hostParts = host.split(":");
                hostname = hostParts[0];
                port = hostParts[1];
            } else {
                hostname = host;
            }

            LOG.fine("Resolving hostname: " + hostname);

            try {
                // Resolve all IPs for this hostname
                InetAddress[] addresses = InetAddress.getAllByName(hostname);
                LOG.fine("Found " + addresses.length + " IP(s) for " + hostname);

                // Add all resolved IPs
                for (int j = 0; j < addresses.length; j++) {
                    if (addresses[j] instanceof java.net.Inet6Address) {
                        LOG.warning("Skipping IPv6 address: " + addresses[j].getHostAddress());
                        continue;
                    }

                    if (resolvedHosts.length() > 0) {
                        resolvedHosts.append(",");
                    }
                    resolvedHosts.append(addresses[j].getHostAddress()).append(":").append(port);
                }

                if (resolvedHosts.length() == 0) {
                    resolvedHosts.append(hostname).append(":").append(port);
                }
            }catch (UnknownHostException e) {
                LOG.warning("Could not resolve " + hostname + ", keeping original: " + e.getMessage());
                // Keep original hostname if resolution fails
                if (resolvedHosts.length() > 0) {
                    resolvedHosts.append(",");
                }
                resolvedHosts.append(host);
            }
        }

        // Build new parameters ensuring targetServerType and loadBalanceHosts are set
        Map<String, String> params = parseQueryParameters(existingParams);

        // Ensure critical parameters are set for primary detection
        if (!params.containsKey("targetServerType")) {
            params.put("targetServerType", "primary");
        }
        if (!params.containsKey("loadBalanceHosts")) {
            params.put("loadBalanceHosts", "true");
        }

        // Rebuild parameter string
        String newParams = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("&"));

        // Rebuild complete JDBC URL
        String resolvedUrl = "jdbc:postgresql://" + resolvedHosts.toString() + "/" + database;
        if (!newParams.isEmpty()) {
            resolvedUrl += "?" + newParams;
        }

        LOG.fine("Resolved to: " + maskPassword(resolvedUrl));
        return resolvedUrl;
    }

    /**
     * Parse query parameters from a parameter string
     */
    private Map<String, String> parseQueryParameters(String paramString) {
        Map<String, String> params = new HashMap<>();
        if (paramString == null || paramString.isEmpty()) {
            return params;
        }

        String[] pairs = paramString.split("&");
        for (String pair : pairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                params.put(keyValue[0], keyValue[1]);
            }
        }
        return params;
    }

    /**
     * Builds a JDBC URL with dynamic DNS resolution for Kubernetes headless services.
     * Resolves all IPs from the hostname and creates a multi-host connection string
     * with targetServerType=primary to ensure connection to the primary database.
     */
    private String buildDynamicJdbcUrl(String host, String dbName) throws UnknownHostException {
        // Check if host contains port specification
        String hostname;
        String port; // default PostgreSQL port

        if (host.contains(":")) {
            String[] parts = host.split(":");
            hostname = parts[0];
            port = parts[1];
        } else {
            port = "5432";
            hostname = host;
        }

        LOG.fine("Resolving DNS for hostname: " + hostname);

        // Resolve all IPs for the hostname (important for Kubernetes headless services)
        InetAddress[] addresses = InetAddress.getAllByName(hostname);

        LOG.fine("Found " + addresses.length + " IP address(es) for " + hostname);

        if (addresses.length == 0) {
            throw new UnknownHostException("No IP addresses found for host: " + hostname);
        }

        // Build multi-host connection string
        String hosts;
        if (addresses.length == 1) {
            // Single host - simple URL
            hosts = addresses[0].getHostAddress() + ":" + port;
            LOG.fine("Single host detected: " + hosts);
        } else {
            // Multiple hosts - build comma-separated list
            hosts = Arrays.stream(addresses)
                    .map(addr -> addr.getHostAddress() + ":" + port)
                    .collect(Collectors.joining(","));
            LOG.fine("Multiple hosts detected: " + hosts);
        }

        // Build JDBC URL with primary targeting
        // targetServerType=primary ensures connection only to read-write (primary) server
        // loadBalanceHosts=true makes the driver try all hosts until it finds the primary
        String jdbcUrl = "jdbc:postgresql://" + hosts + "/" + dbName +
                "?targetServerType=primary&loadBalanceHosts=true";

        return jdbcUrl;
    }

    /**
     * Masks password in JDBC URL for logging purposes
     */
    private String maskPassword(String jdbcUrl) {
        if (jdbcUrl == null) return null;
        return jdbcUrl.replaceAll("password=[^&]*", "password=***");
    }

    // Enhanced monitoring methods
    public Map<String, Object> getDetailedPoolStats() {
        Map<String, Object> stats = new HashMap<>();
        if (hikariDataSource != null) {
            try {
                stats.put("activeConnections", hikariDataSource.getHikariPoolMXBean().getActiveConnections());
                stats.put("idleConnections", hikariDataSource.getHikariPoolMXBean().getIdleConnections());
                stats.put("totalConnections", hikariDataSource.getHikariPoolMXBean().getTotalConnections());
                stats.put("threadsAwaitingConnection", hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection());
                stats.put("poolName", hikariDataSource.getPoolName());
                stats.put("maximumPoolSize", hikariDataSource.getMaximumPoolSize());
                stats.put("minimumIdle", hikariDataSource.getMinimumIdle());
            } catch (Exception e) {
                LOG.warning("Error getting pool statistics: " + e.getMessage());
                stats.put("error", e.getMessage());
            }
        }
        return stats;
    }

    public boolean isHealthy() {
        try {
            if (hikariDataSource != null && !hikariDataSource.isClosed()) {
                return hikariDataSource.getHikariPoolMXBean().getThreadsAwaitingConnection() < 5;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static class EntityManagerServiceBuilder {
        private String persistenceName;
        private String poolMaxSize;
        private String maxConnectionLifetime;
        private String keepAliveTime;
        private String leakDetectionThreshold;

        private String connectionString;
        private String postgresqlHost;
        private String postgresqlDBName;
        private String postgresqlUsername;
        private String postgresqlPassword;

        private final String PERSISTENCE_NAME_DEFAULT = "EPOSDataModel";
        private final String CONNECTION_POOL_MAX_SIZE_DEFAULT = "10";
        private final String CONNECTION_MAX_LIFETIME_DEFAULT = "1800000";
        private final String CONNECTION_TEST_IDLE_INTERVAL_TIME_DEFAULT = "600000";
        private final String CONNECTION_LEAK_DETECTION_THRESHOLD = "600000";

        public EntityManagerServiceBuilder(){
            persistenceName = System.getenv("PERSISTENCE_NAME");
            persistenceName = persistenceName == null ? PERSISTENCE_NAME_DEFAULT : persistenceName;

            poolMaxSize = System.getenv("CONNECTION_POOL_MAX_SIZE");
            poolMaxSize = poolMaxSize == null ? CONNECTION_POOL_MAX_SIZE_DEFAULT : poolMaxSize;

            maxConnectionLifetime = System.getenv("CONNECTION_MAX_LIFETIME");
            maxConnectionLifetime = maxConnectionLifetime == null ? CONNECTION_MAX_LIFETIME_DEFAULT : maxConnectionLifetime;

            keepAliveTime = System.getenv("CONNECTION_TEST_IDLE_INTERVAL_TIME");
            keepAliveTime = keepAliveTime == null ? CONNECTION_TEST_IDLE_INTERVAL_TIME_DEFAULT : keepAliveTime;

            leakDetectionThreshold = System.getenv("CONNECTION_LEAK_DETECTION_THRESHOLD");
            leakDetectionThreshold = leakDetectionThreshold == null ? CONNECTION_LEAK_DETECTION_THRESHOLD : leakDetectionThreshold;

            this.connectionString = System.getenv("POSTGRESQL_CONNECTION_STRING");
            this.postgresqlHost =  System.getenv("POSTGRESQL_HOST");
            this.postgresqlDBName =  System.getenv("POSTGRESQL_DBNAME");
            this.postgresqlUsername =  System.getenv("POSTGRESQL_USERNAME");
            this.postgresqlPassword =  System.getenv("POSTGRESQL_PASSWORD");
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

        /**
         * Builds and initializes the EntityManagerService.
         * If already initialized with an open connection, returns this builder instance
         * to indicate success without creating a duplicate.
         * 
         * @return the EntityManagerService instance, or this builder if already initialized
         */
        public EntityManagerService build(){
            if (instance != null && instance.isOpen()) {
                LOG.fine("EntityManagerService already initialized, reusing existing instance");
                return null; // Indicates reuse - callers should use getInstance()
            }
            return new EntityManagerService(this);
        }

    }

    /**
     * Returns the EntityManagerFactory instance.
     * 
     * @return the EntityManagerFactory instance
     * @throws IllegalStateException if the service has not been initialized via build()
     */
    public static synchronized EntityManagerFactory getInstance() {
        if (instance == null) {
            throw new IllegalStateException(
                "EntityManagerService not initialized. Call EntityManagerServiceBuilder.build() first.");
        }
        return instance;
    }
    
    /**
     * Checks if the EntityManagerService has been initialized.
     * 
     * @return true if initialized and open, false otherwise
     */
    public static synchronized boolean isInitialized() {
        return instance != null && instance.isOpen();
    }

    public void close() {
        if (instance != null && instance.isOpen()) {
            instance.close();
        }
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
        instance = null;
        hikariDataSource = null;
        LOG.fine("EntityManagerService closed");
    }

}
