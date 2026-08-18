package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import model.StatusType;
import metadataapis.EntityNames;
import metadataapis.SoftwareApplicationAPI;
import org.epos.eposdatamodel.SoftwareApplication;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoftwareApplicationSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingTheFullGraph() {
        SoftwareApplicationAPI api = (SoftwareApplicationAPI) AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());
        SoftwareApplication application = new SoftwareApplication();
        application.setUid("summary/software-application/" + UUID.randomUUID());
        application.setName("Summary application");
        application.setDescription("Description retained because it is a scalar field");
        api.create(application, null, null, null);

        List<SoftwareApplication> summaries = api.retrieveAllSummary();
        SoftwareApplication summary = summaries.stream()
                .filter(candidate -> application.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(application.getUid(), summary.getUid());
        assertEquals("Summary application", summary.getName());
        assertEquals("Description retained because it is a scalar field", summary.getDescription());
        assertNotNull(summary.getStatus());
        assertTrue(summary.getCategory() == null || summary.getCategory().isEmpty());
    }

    @Test
    void summarySupportsBunchAndStatusFilters() {
        SoftwareApplicationAPI api = (SoftwareApplicationAPI) AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());
        SoftwareApplication application = new SoftwareApplication();
        application.setUid("summary/software-application-filter/" + UUID.randomUUID());
        application.setName("Filtered summary application");
        api.create(application, StatusType.DRAFT, null, null);

        assertEquals(1, api.retrieveBunchSummary(List.of(application.getInstanceId())).size());
        assertTrue(api.retrieveAllSummaryWithStatus(StatusType.DRAFT).stream()
                .anyMatch(candidate -> application.getInstanceId().equals(candidate.getInstanceId())));
    }
}
