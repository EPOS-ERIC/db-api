package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Address;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetAddress() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name());

        Address address = new Address();
        address.setInstanceId(UUID.randomUUID().toString());
        address.setMetaId(UUID.randomUUID().toString());
        address.setUid(UUID.randomUUID().toString());
        address.setCountry("Italy");
        address.setCountryCode("IT");
        address.setStreet("Via Roma");
        address.setPostalCode("00100");
        address.setLocality("Rome");


        LOG.info("CREATED:\n"+address.toString());

        api.create(address, null, null, null);

        Address retrievedAddress = (Address) api.retrieve(address.getInstanceId());

        LOG.info("RECEIVED:\n"+address.toString());

        assertNotNull(retrievedAddress);
        assertEquals(address, retrievedAddress);
    }


    @Test
    @Order(2)
    public void testUpdateAddress() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name());

        Address address = new Address();
        address.setInstanceId(UUID.randomUUID().toString());
        address.setMetaId(UUID.randomUUID().toString());
        address.setUid(UUID.randomUUID().toString());
        address.setCountry("France");
        address.setCountryCode("FR");
        address.setStreet("Rue de la Paix");
        address.setPostalCode("75002");
        address.setLocality("Paris");
        address.setEditorId("test/editor");

        api.create(address, null, null, null);

        address.setCountry("Spain");
        address.setPostalCode("28001");
        address.setLocality("Madrid");

        api.create(address, null, null, null);

        Address retrievedAddress = (Address) api.retrieve(address.getInstanceId());

        assertNotNull(retrievedAddress);
        assertEquals(address, retrievedAddress);
    }

    @Test
    @Order(3)
    public void testDeleteAddress() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name());

        Address address = new Address();
        address.setInstanceId(UUID.randomUUID().toString());
        address.setMetaId(UUID.randomUUID().toString());
        address.setUid(UUID.randomUUID().toString());
        address.setCountry("Germany");
        address.setCountryCode("DE");
        address.setStreet("Unter den Linden");
        address.setPostalCode("10117");
        address.setLocality("Berlin");

        api.create(address, null, null, null);

       List<model.Address> addressList = api.getDbaccess().getOneFromDBByInstanceId(address.getInstanceId(),model.Address.class);

        LOG.info(api.getDbaccess().deleteObject(addressList.get(0)).toString());
        Address retrievedAddress = (Address) api.retrieve(address.getInstanceId());

        assertEquals(null,retrievedAddress);
    }


}
