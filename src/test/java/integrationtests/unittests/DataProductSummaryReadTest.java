package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import model.StatusType;
import metadataapis.DataProductAPI;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.DataProduct;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataProductSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsListFieldsWithoutLoadingTheFullGraph() {
        DataProductAPI api = (DataProductAPI) DataProductAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("summary/dataproduct/" + UUID.randomUUID());
        dataProduct.setType("Dataset");
        dataProduct.addTitle("Summary title");
        dataProduct.addDescription("Description excluded from the summary");
        api.create(dataProduct, null, null, null);

        List<?> summaries = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAllSummary();
        DataProduct summary = summaries.stream()
                .map(DataProduct.class::cast)
                .filter(candidate -> dataProduct.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(dataProduct.getUid(), summary.getUid());
        assertEquals("Dataset", summary.getType());
        assertEquals(List.of("Summary title"), summary.getTitle());
        assertNotNull(summary.getStatus());
        assertTrue(summary.getDescription() == null || summary.getDescription().isEmpty());
    }

    @Test
    void summarySupportsBunchAndStatusFilters() {
        DataProductAPI api = (DataProductAPI) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("summary/dataproduct-filter/" + UUID.randomUUID());
        dataProduct.setType("Dataset");
        api.create(dataProduct, StatusType.DRAFT, null, null);

        assertEquals(1, api.retrieveBunchSummary(List.of(dataProduct.getInstanceId())).size());
        assertTrue(api.retrieveAllSummaryWithStatus(StatusType.DRAFT).stream()
                .anyMatch(candidate -> dataProduct.getInstanceId().equals(candidate.getInstanceId())));
    }
}
