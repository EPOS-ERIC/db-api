package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.EquipmentAPI;
import org.epos.eposdatamodel.Equipment;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EquipmentSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingTheFullGraph() {
        EquipmentAPI api = (EquipmentAPI) AbstractAPI.retrieveAPI(EntityNames.EQUIPMENT.name());
        Equipment equipment = new Equipment();
        equipment.setUid("summary/equipment/" + UUID.randomUUID());
        equipment.setName("Summary equipment");
        equipment.setDescription("Description retained because it is a scalar field");
        equipment.setKeywords(List.of("one", "two"));
        api.create(equipment, null, null, null);

        List<Equipment> summaries = api.retrieveAllSummary();
        Equipment summary = summaries.stream()
                .filter(candidate -> equipment.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(equipment.getUid(), summary.getUid());
        assertEquals("Summary equipment", summary.getName());
        assertEquals("Description retained because it is a scalar field", summary.getDescription());
        assertEquals(List.of("one", "two"), summary.getKeywords());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getCategory() == null || summary.getCategory().isEmpty());
    }
}
