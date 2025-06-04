package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementFacilityTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetFacility() {

        LinkedEntity le = new LinkedEntity();
        le.setUid("category1");
        le.setEntityType(EntityNames.CATEGORY.name());

        LinkedEntity le2 = new LinkedEntity();
        le2.setUid("contact1");
        le2.setEntityType(EntityNames.CONTACTPOINT.name());

        LinkedEntity le3 = new LinkedEntity();
        le3.setUid("address1");
        le3.setEntityType(EntityNames.ADDRESS.name());

        LinkedEntity le4 = new LinkedEntity();
        le4.setUid("faciity2");
        le4.setEntityType(EntityNames.FACILITY.name());

        LinkedEntity le5 = new LinkedEntity();
        le5.setUid("location");
        le5.setEntityType(EntityNames.LOCATION.name());

        Facility facility = new Facility();
        facility.setUid(UUID.randomUUID().toString());
        facility.setDescription("description");
        facility.addCategory(le);
        facility.addContactPoint(le2);
        facility.addAddress(le3);
        facility.addPageURL("PAGE URL");
        facility.setIdentifier("identifier");
        facility.setTitle("name");
        facility.setType("type");
        facility.addRelation(le4);
        facility.addIsPartOf(le4);
        facility.addSpatialExtentItem(le5);
        facility.setStatus(StatusType.PUBLISHED);

        AbstractAPI.retrieveAPI(EntityNames.FACILITY.name()).create(facility, null, null, null);

        List<Facility> retrievedFacilities = AbstractAPI.retrieveAPI(EntityNames.FACILITY.name()).retrieveAll();
        assertNotNull(retrievedFacilities);
        assertEquals(2,retrievedFacilities.size());

        for(Facility f : retrievedFacilities){
            System.out.println(f);
        }
    }


}
