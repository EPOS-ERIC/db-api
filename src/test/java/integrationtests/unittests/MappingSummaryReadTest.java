package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.MappingAPI;
import org.epos.eposdatamodel.Mapping;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MappingSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        MappingAPI api = (MappingAPI) AbstractAPI.retrieveAPI(EntityNames.MAPPING.name());
        Mapping mapping = new Mapping();
        mapping.setUid("summary/mapping/" + UUID.randomUUID());
        mapping.setLabel("Summary mapping");
        mapping.setVariable("station");
        mapping.setRequired("true");
        mapping.addParamValue("Relation excluded from summary");
        api.create(mapping, null, null, null);

        Mapping summary = api.retrieveAllSummary().stream()
                .filter(candidate -> mapping.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(mapping.getUid(), summary.getUid());
        assertEquals("Summary mapping", summary.getLabel());
        assertEquals("station", summary.getVariable());
        assertEquals("true", summary.getRequired());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getParamValue() == null || summary.getParamValue().isEmpty());
        assertTrue(summary.getGroups().isEmpty());
    }
}
