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

public class EntityManagementBunchTest extends TestcontainersLifecycle {

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

        Address retrievedAddress = (Address) api.retrieveBunch(List.of(address.getInstanceId())).get(0);

        LOG.info("RECEIVED:\n"+address.toString());

        assertNotNull(retrievedAddress);
        assertEquals(address, retrievedAddress);
    }

}
