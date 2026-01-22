package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.Equipment;
import org.epos.eposdatamodel.Facility;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

public class EquipmentTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreation() {

        Equipment equipment = new Equipment();
        equipment.setUid("Equipment");
        equipment.setName("equipment");
        equipment.setDescription("description");
        equipment.setStatus(StatusType.PUBLISHED);
        equipment.setEditorId("test");
        equipment.setFileProvenance("prov1");

        AbstractAPI.retrieveAPI(EntityNames.EQUIPMENT.name()).create(equipment, null, null, null);

        System.out.println("-------------------- first ingestion --------------------");
        for(Object object : AbstractAPI.retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveAll()){
            System.out.println(object);
        }

        System.out.println("-------------------- first ingestion --------------------");

    }

}
