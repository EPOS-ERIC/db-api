package integrationtests;

import org.epos.handler.dbapi.service.EntityManagerService;
import org.junit.jupiter.api.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestcontainersLifecycle {

    protected static Logger LOG = Logger.getGlobal();
    public static PostgreSQLContainer<?> METADATA_CATALOGUE = new PostgreSQLContainer<>(
            DockerImageName.parse("epos/metadata-database-deploy-test:latest")
                    .asCompatibleSubstituteFor("postgres")

    ).withDatabaseName("metadata_catalogue").withUsername("postgres").withPassword("changeme").withExposedPorts(5432);

    @BeforeAll
    static void startContainers()  {
        METADATA_CATALOGUE.start();
    }

    @BeforeEach
    void connect() throws InterruptedException {
        new EntityManagerService.EntityManagerServiceBuilder()
                .setPostgresqlHost(METADATA_CATALOGUE.getHost()+":"+METADATA_CATALOGUE.getMappedPort(5432))
                .setPostgresqlDBName(METADATA_CATALOGUE.getDatabaseName())
                .setPostgresqlUsername(METADATA_CATALOGUE.getUsername())
                .setPostgresqlPassword(METADATA_CATALOGUE.getPassword())
                .build();
    }

    @AfterAll
    static void stopContainers() {
        METADATA_CATALOGUE.stop();
    }

}

