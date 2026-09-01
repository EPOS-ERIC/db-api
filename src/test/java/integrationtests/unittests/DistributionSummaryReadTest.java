package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.DistributionAPI;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Distribution;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DistributionSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        DistributionAPI api = (DistributionAPI) AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        Distribution distribution = new Distribution();
        distribution.setUid("summary/distribution/" + UUID.randomUUID());
        distribution.setFormat("application/json");
        distribution.setType("DataDownload");
        distribution.addTitle("Relation excluded from summary");
        api.create(distribution, null, null, null);

        Distribution summary = api.retrieveAllSummary().stream()
                .filter(candidate -> distribution.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(distribution.getUid(), summary.getUid());
        assertEquals("application/json", summary.getFormat());
        assertEquals("DataDownload", summary.getType());
        assertNotNull(summary.getStatus());
        assertTrue(summary.getTitle() == null || summary.getTitle().isEmpty());
        assertTrue(summary.getGroups().isEmpty());
    }
}
