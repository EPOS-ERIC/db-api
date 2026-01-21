package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Person;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

public class AddressPersonTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreation() {

        LinkedEntity le = new LinkedEntity();
        le.setUid("test");
        le.setEntityType(EntityNames.ADDRESS.name());
        le.setMetaId("metaid");
        le.setInstanceId("instanceid");

        Person person = new Person();
        person.setUid("Person");
        person.setGivenName("givenname");
        person.setFamilyName("familyname");
        person.setAddress(le);
        person.setStatus(StatusType.PUBLISHED);
        person.setEditorId("test");
        person.setFileProvenance("prov1");

        AbstractAPI.retrieveAPI(EntityNames.PERSON.name()).create(person, null, null, null);

        System.out.println("-------------------- first ingestion --------------------");
        for(Object object : AbstractAPI.retrieveAPI(EntityNames.PERSON.name()).retrieveAll()){
            System.out.println(object);
        }

        System.out.println("-------------------- first ingestion --------------------");

    }

}
