package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.FacilityAPI;
import org.epos.eposdatamodel.Facility;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FacilitySummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingTheFullGraph() {
        FacilityAPI api = (FacilityAPI) AbstractAPI.retrieveAPI(EntityNames.FACILITY.name());
        Facility facility = new Facility();
        facility.setUid("summary/facility/" + UUID.randomUUID());
        facility.setTitle("Summary facility");
        facility.setDescription("Description retained because it is a scalar field");
        facility.setKeywords(List.of("one", "two"));
        api.create(facility, null, null, null);

        List<Facility> summaries = api.retrieveAllSummary();
        Facility summary = summaries.stream()
                .filter(candidate -> facility.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(facility.getUid(), summary.getUid());
        assertEquals("Summary facility", summary.getTitle());
        assertEquals("Description retained because it is a scalar field", summary.getDescription());
        assertEquals(List.of("one", "two"), summary.getKeywords());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getCategory() == null || summary.getCategory().isEmpty());
    }
}
