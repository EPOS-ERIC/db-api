package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import model.StatusType;
import metadataapis.EntityNames;
import metadataapis.WebServiceAPI;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServiceSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingTheFullGraph() {
        WebServiceAPI api = (WebServiceAPI) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        WebService webService = new WebService();
        webService.setUid("summary/web-service/" + UUID.randomUUID());
        webService.setName("Summary service");
        webService.setDescription("Description retained because it is a scalar field");
        api.create(webService, null, null, null);

        List<WebService> summaries = api.retrieveAllSummary();
        WebService summary = summaries.stream()
                .filter(candidate -> webService.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(webService.getUid(), summary.getUid());
        assertEquals("Summary service", summary.getName());
        assertEquals("Description retained because it is a scalar field", summary.getDescription());
        assertNotNull(summary.getStatus());
        assertTrue(summary.getCategory() == null || summary.getCategory().isEmpty());
    }

    @Test
    void summarySupportsBunchAndStatusFilters() {
        WebServiceAPI api = (WebServiceAPI) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        WebService webService = new WebService();
        webService.setUid("summary/web-service-filter/" + UUID.randomUUID());
        webService.setName("Filtered summary service");
        api.create(webService, StatusType.DRAFT, null, null);

        assertEquals(1, api.retrieveBunchSummary(List.of(webService.getInstanceId())).size());
        assertTrue(api.retrieveAllSummaryWithStatus(StatusType.DRAFT).stream()
                .anyMatch(candidate -> webService.getInstanceId().equals(candidate.getInstanceId())));
    }
}
