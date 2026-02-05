package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FacilityTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreation() {

        Facility facility = new Facility();
        facility.setUid("Facility");
        facility.setTitle("facility");
        facility.setDescription("description");
        facility.setKeywords(List.of("Laboratory of sedimentology","Particle size measurement"));
        facility.setStatus(StatusType.PUBLISHED);
        facility.setEditorId("test");
        facility.setFileProvenance("prov1");

        AbstractAPI.retrieveAPI(EntityNames.FACILITY.name()).create(facility, null, null, null);

        System.out.println("-------------------- first ingestion --------------------");
        for(Object object : AbstractAPI.retrieveAPI(EntityNames.FACILITY.name()).retrieveAll()){
            System.out.println(object);
        }

        System.out.println("-------------------- first ingestion --------------------");

    }

}
