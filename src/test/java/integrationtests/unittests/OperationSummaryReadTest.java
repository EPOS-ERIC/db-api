package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.OperationAPI;
import org.epos.eposdatamodel.Operation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OperationSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        OperationAPI api = (OperationAPI) AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());
        Operation operation = new Operation();
        operation.setUid("summary/operation/" + UUID.randomUUID());
        operation.setMethod("GET");
        operation.setTemplate("/summary/{id}");
        operation.addReturns("application/json");
        api.create(operation, null, null, null);

        Operation summary = api.retrieveAllSummary().stream()
                .filter(candidate -> operation.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(operation.getUid(), summary.getUid());
        assertEquals("GET", summary.getMethod());
        assertEquals("/summary/{id}", summary.getTemplate());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getReturns() == null || summary.getReturns().isEmpty());
        assertTrue(summary.getGroups().isEmpty());
    }
}
