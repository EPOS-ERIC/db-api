package integrationtests.unittests;

import integrationtests.TestcontainersLifecycle;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.sql.DriverManager;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertFalse;

class VersioningDuplicateDiagnosticsTest extends TestcontainersLifecycle {

    @Test
    void diagnosticsRunAgainstMigratedDatabase() throws Exception {
        String sql = new String(
                getClass().getResourceAsStream("/db/versioning-duplicate-diagnostics.sql").readAllBytes(),
                StandardCharsets.UTF_8);
        String[] queries = sql.lines()
                .filter(line -> !line.stripLeading().startsWith("--"))
                .collect(java.util.stream.Collectors.joining("\n"))
                .split(";");

        try (var connection = DriverManager.getConnection(
                METADATA_CATALOGUE.getJdbcUrl(),
                METADATA_CATALOGUE.getUsername(),
                METADATA_CATALOGUE.getPassword())) {
            for (String query : queries) {
                if (query.isBlank()) continue;
                try (var statement = connection.createStatement();
                     ResultSet results = statement.executeQuery(query)) {
                    assertFalse(results.next(), "Clean Testcontainers database must have no versioning duplicates");
                }
            }
        }
    }
}
