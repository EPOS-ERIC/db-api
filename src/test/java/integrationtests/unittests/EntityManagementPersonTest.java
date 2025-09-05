package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Person;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementPersonTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGet() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.PERSON.name());
        AbstractAPI apicp = AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name());

        Person person = new Person();
        person.setInstanceId(UUID.randomUUID().toString());
        person.setMetaId(UUID.randomUUID().toString());
        person.setUid(UUID.randomUUID().toString());
        person.setFamilyName("FamilyName");
        person.setGivenName("GivenName");

        LinkedEntity le = new LinkedEntity();
        le.setEntityType(EntityNames.CONTACTPOINT.name());
        le.setUid("test");

        person.addContactPoint(le);


        //LOG.info("CREATED:\n"+person.toString());

        api.create(person, null, null, null);

        Person retrievedPerson = (Person) api.retrieve(person.getInstanceId());

        //LOG.info("RECEIVED:\n"+person.toString());

        List<ContactPoint> retrieveContactPoint = apicp.retrieveAll();

        //LOG.info("RECEIVED:\n"+retrieveContactPoint);

        assertNotNull(retrievedPerson);
        assertNotNull(retrievedPerson.getContactPoint());
    }

}
