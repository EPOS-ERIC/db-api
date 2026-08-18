package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.PayloadAPI;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.OutputMapping;
import org.epos.eposdatamodel.Payload;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayloadSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        OutputMapping outputMapping = new OutputMapping();
        outputMapping.setUid("summary/output-mapping/" + UUID.randomUUID());
        LinkedEntity outputMappingLink = AbstractAPI.retrieveAPI(EntityNames.OUTPUTMAPPING.name())
                .create(outputMapping, null, null, null);

        PayloadAPI api = (PayloadAPI) AbstractAPI.retrieveAPI(EntityNames.PAYLOAD.name());
        Payload payload = new Payload();
        payload.setUid("summary/payload/" + UUID.randomUUID());
        payload.setOutputMapping(List.of(outputMappingLink));
        api.create(payload, null, null, null);

        Payload summary = api.retrieveAllSummary().stream()
                .filter(candidate -> payload.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(payload.getUid(), summary.getUid());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getOutputMapping() == null || summary.getOutputMapping().isEmpty());
        assertTrue(summary.getGroups().isEmpty());
    }
}
