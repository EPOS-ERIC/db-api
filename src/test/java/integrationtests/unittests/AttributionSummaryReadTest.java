package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.AttributionAPI;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Attribution;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttributionSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        AttributionAPI api = (AttributionAPI) AbstractAPI.retrieveAPI(EntityNames.ATTRIBUTION.name());
        Attribution attribution = new Attribution();
        attribution.setUid("summary/attribution/" + UUID.randomUUID());
        attribution.setRole(List.of("Relation excluded from summary"));
        api.create(attribution, null, null, null);

        Attribution summary = api.retrieveAllSummary().stream()
                .filter(candidate -> attribution.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(attribution.getUid(), summary.getUid());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getRole() == null || summary.getRole().isEmpty());
        assertTrue(summary.getGroups().isEmpty());
    }
}
