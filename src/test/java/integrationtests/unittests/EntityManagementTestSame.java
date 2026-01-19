package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.Address;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementTestSame extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetAddress() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name());

        Address address = new Address();
        address.setUid("address/test");
        address.setCountry("Italy");
        address.setCountryCode("IT");
        address.setStreet("Via Roma");
        address.setPostalCode("00100");
        address.setLocality("Rome");


        LOG.info("CREATED:\n"+address.toString());

        api.create(address, StatusType.PUBLISHED, null, null);

        List<Address> retrievedAddress = api.retrieveAll();

        for(Address a : retrievedAddress) {
            System.out.println(a);
        }
    }


    @Test
    @Order(2)
    public void testUpdateAddress() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name());

        Address address = new Address();
        address.setUid("address/test");
        address.setCountry("Italy");
        address.setCountryCode("IT");
        address.setStreet("Via Roma");
        address.setPostalCode("00100");
        address.setLocality("Rome");

        api.create(address, StatusType.DRAFT, null, null);

        List<Address> retrievedAddress = api.retrieveAll();
        System.out.println(retrievedAddress);

        for(Address a : retrievedAddress) {
            System.out.println(a);
        }
    }
}
