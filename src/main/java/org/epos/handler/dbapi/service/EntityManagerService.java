package org.epos.handler.dbapi.service;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.eclipse.persistence.config.PersistenceUnitProperties;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

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
        hikariConfig.setConnectionTimeout(30000);

        hikariConfig.setAutoCommit(false);

        hikariConfig.setDriverClassName("org.postgresql.Driver");
        hikariConfig.setPoolName("metadata_catalogue");
        hikariConfig.setConnectionTimeout(1000);
        hikariConfig.setInitializationFailTimeout(9000);

        // PostgreSQL specific optimizations
        hikariConfig.addDataSourceProperty("cachePrepStmts", "true");
        hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250");
        hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        hikariConfig.addDataSourceProperty("useServerPrepStmts", "true");
        hikariConfig.addDataSourceProperty("rewriteBatchedStatements", "true");

        // Connection validation
        hikariConfig.setConnectionTestQuery("SELECT 1");
        hikariConfig.setValidationTimeout(5000);

        this.connectionString = entityManagerServiceBuilder.connectionString;
        this.postgresqlHost = entityManagerServiceBuilder.postgresqlHost;
        this.postgresqlDBName = entityManagerServiceBuilder.postgresqlDBName;
        this.postgresqlUsername = entityManagerServiceBuilder.postgresqlUsername;
        this.postgresqlPassword = entityManagerServiceBuilder.postgresqlPassword;

        //System.out.println(this.postgresqlHost);

        if (connectionString != null) {
            hikariConfig.setJdbcUrl(connectionString);
        } else {
            String dburl = "jdbc:postgresql://";

            dburl += postgresqlHost;
            dburl += "/";
            dburl += postgresqlDBName;

            String user = postgresqlUsername;
            String password = postgresqlPassword;

            hikariConfig.setJdbcUrl(dburl);
            hikariConfig.setUsername(user);
            hikariConfig.setPassword(password);
        }


        hikariDataSource = new HikariDataSource(hikariConfig);

        properties.put(PersistenceUnitProperties.NON_JTA_DATASOURCE, hikariDataSource);
        instance = Persistence.createEntityManagerFactory(persistenceName, properties);
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

        public EntityManagerService build(){
            return new EntityManagerService(this);
        }

    }

    public static synchronized EntityManagerFactory getInstance() {
        if (instance != null) return instance;
        return null;
    }

    public void close() {
        if (instance != null && instance.isOpen()) {
            instance.close();
        }
        if (hikariDataSource != null && !hikariDataSource.isClosed()) {
            hikariDataSource.close();
        }
        //LOG.info("Enhanced EntityManagerService closed");
    }

}
