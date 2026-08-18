package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.Facility;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.epos.eposdatamodel.Person;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AddressDeleteReferencedEntitiesTest extends TestcontainersLifecycle {

    @Test
    void deletesAddressBeforeEntitiesThatReferenceIt() {
        String suffix = UUID.randomUUID().toString();

        Address address = new Address();
        address.setUid("https://example.org/address/" + suffix);
        address.setStreet("Via Roma 1");
        LinkedEntity addressLink = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name())
                .create(address, null, null, null);

        Organization organization = new Organization();
        organization.setUid("https://example.org/organization/" + suffix);
        organization.setLegalName(List.of("Organization " + suffix));
        organization.setAddress(addressLink);
        LinkedEntity organizationLink = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(organization, null, null, null);

        Person person = new Person();
        person.setUid("https://example.org/person/" + suffix);
        person.setFamilyName("Rossi");
        person.setGivenName("Mario");
        person.setAddress(addressLink);
        LinkedEntity personLink = AbstractAPI.retrieveAPI(EntityNames.PERSON.name())
                .create(person, null, null, null);

        Facility facility = new Facility();
        facility.setUid("https://example.org/facility/" + suffix);
        facility.setTitle("Facility " + suffix);
        facility.setAddress(List.of(addressLink));
        LinkedEntity facilityLink = AbstractAPI.retrieveAPI(EntityNames.FACILITY.name())
                .create(facility, null, null, null);

        assertEquals(Boolean.TRUE, AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name())
                .delete(addressLink.getInstanceId()));

        assertNull(AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name()).retrieve(addressLink.getInstanceId()));

        Organization retrievedOrganization = (Organization) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieve(organizationLink.getInstanceId());
        assertNotNull(retrievedOrganization);
        assertNull(retrievedOrganization.getAddress());

        Person retrievedPerson = (Person) AbstractAPI.retrieveAPI(EntityNames.PERSON.name())
                .retrieve(personLink.getInstanceId());
        assertNotNull(retrievedPerson);
        assertNull(retrievedPerson.getAddress());

        Facility retrievedFacility = (Facility) AbstractAPI.retrieveAPI(EntityNames.FACILITY.name())
                .retrieve(facilityLink.getInstanceId());
        assertNotNull(retrievedFacility);
        assertTrue(retrievedFacility.getAddress() == null || retrievedFacility.getAddress().isEmpty());
    }
}
