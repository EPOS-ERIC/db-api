package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class IngestionCompleteNestedCrashTest extends TestcontainersLifecycle {

    // Unique UIDs to prevent conflicts
    private final String ORG_UID = "https://example.org/org/full-" + UUID.randomUUID();
    private final String PERSON_UID = "https://example.org/person/full-" + UUID.randomUUID();
    private final String FACILITY_UID = "https://example.org/facility/full-" + UUID.randomUUID();
    private final String EQUIPMENT_UID = "https://example.org/equipment/full-" + UUID.randomUUID();
    private final String WEBSERVICE_UID = "https://example.org/webservice/full-" + UUID.randomUUID();
    private final String OPERATION_UID = "https://example.org/operation/full-" + UUID.randomUUID();
    private final String DATAPRODUCT_UID = "https://example.org/dataproduct/full-" + UUID.randomUUID();
    private final String ADDRESS_UID = "https://example.org/address/full-" + UUID.randomUUID();
    private final String CONTACT_UID = "https://example.org/contact/full-" + UUID.randomUUID();

    @BeforeEach
    public void setup() {
        // Optional cleanup
    }

    @Test
    public void testCompleteNestedIngestion() {
        System.out.println("--- STARTING COMPLETE NESTED INGESTION TEST ---");

        // 1. PREPARE COMMON LINKED ENTITIES (To be used as stubs/references)

        // Address (Nested in Org, Person, ContactPoint, Facility)
        Address address = new Address();
        address.setUid(ADDRESS_UID);
        address.setStreet("Via di Roma 1");
        address.setLocality("Rome");
        address.setCountry("Italy");

        LinkedEntity addressLink = new LinkedEntity();
        addressLink.setUid(ADDRESS_UID);
        addressLink.setEntityType(EntityNames.ADDRESS.name());
        // Pre-ingest Address to ensure it exists (optional, but good for stability)
        AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name()).create(address, StatusType.PUBLISHED, null, null);


        // 2. ORGANIZATION (The Root Provider)
        Organization org = new Organization();
        org.setUid(ORG_UID);
        org.setLegalName(List.of("Global Seismic Institute"));
        org.setAddress(addressLink); // Link Address
        org.setEmail(List.of("info@gsi.org")); // Triggers Element Creation (Risk of NPE)
        org.setTelephone(List.of("+39 000 0000")); // Triggers Element Creation

        LinkedEntity orgLink = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(org, StatusType.PUBLISHED, null, null);


        // 3. PERSON (Affiliated with Org)
        Person person = new Person();
        person.setUid(PERSON_UID);
        person.setFamilyName("Rossi");
        person.setGivenName("Mario");
        person.setEmail(List.of("mario.rossi@gsi.org")); // Nested Element
        person.setAffiliation(List.of(orgLink)); // Nested Relation
        person.setAddress(addressLink);

        LinkedEntity personLink = AbstractAPI.retrieveAPI(EntityNames.PERSON.name())
                .create(person, StatusType.PUBLISHED, null, null);


        // 4. FACILITY (Owned by Org, Managed by Person)
        Facility facility = new Facility();
        facility.setUid(FACILITY_UID);
        facility.setTitle("Seismic Monitoring Station A1");
        facility.setAddress(List.of(addressLink));

        // Create a contact point for facility
        ContactPoint facilityContact = new ContactPoint();
        facilityContact.setUid(CONTACT_UID);
        facilityContact.setRole("Station Manager Contact");
        facilityContact.setEmail(List.of("station.manager@gsi.org")); // Nested Element

        LinkedEntity contactLink = AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name())
                .create(facilityContact, StatusType.PUBLISHED, null, null);

        facility.setContactPoint(List.of(contactLink));

        // Complex Relation: Facility Category (Stub creation)
        LinkedEntity catLink = new LinkedEntity();
        catLink.setUid("https://epos-eu.org/vocab/Facility/Station");
        catLink.setEntityType(EntityNames.CATEGORY.name());
        facility.setCategory(List.of(catLink));

        LinkedEntity facilityLink = AbstractAPI.retrieveAPI(EntityNames.FACILITY.name())
                .create(facility, StatusType.PUBLISHED, null, null);


        // 5. EQUIPMENT (Inside Facility)
        Equipment equipment = new Equipment();
        equipment.setUid(EQUIPMENT_UID);
        equipment.setName("Seismometer Type X");
        equipment.setManufacturer(orgLink); // Org is manufacturer
        equipment.setIsPartOf(List.of(facilityLink)); // Equipment inside Facility

        LinkedEntity equipmentLink = AbstractAPI.retrieveAPI(EntityNames.EQUIPMENT.name())
                .create(equipment, StatusType.PUBLISHED, null, null);


        // 6. WEBSERVICE & OPERATION
        WebService ws = new WebService();
        ws.setUid(WEBSERVICE_UID);
        ws.setName("Seismic Data API");
        ws.setProvider(orgLink); // Linked to Org

        LinkedEntity wsLink = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(ws, StatusType.PUBLISHED, null, null);

        Operation op = new Operation();
        op.setUid(OPERATION_UID);
        op.setTemplate("Get Waveforms");
        op.setWebservice(List.of(wsLink)); // Linked to WS

        LinkedEntity opLink = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name())
                .create(op, StatusType.PUBLISHED, null, null);


        // 7. DATA PRODUCT (The Grand Finale - Links everything)
        DataProduct dp = new DataProduct();
        dp.setUid(DATAPRODUCT_UID);
        dp.setTitle(List.of("Complete Seismic Dataset 2024"));
        dp.setDescription(List.of("Dataset linking all entities."));

        // Publisher
        dp.setPublisher(List.of(orgLink));

        // Contact Point
        dp.setContactPoint(List.of(contactLink));

        // Related Facility / Equipment (via Qualified Attribution usually, but using relations available)
        // Note: Check if DataProduct has direct fields for facility/equipment in your version,
        // if not, we rely on the graph being consistent.

        // Distribution
        Distribution dist = new Distribution();
        dist.setUid("https://example.org/dist/full-" + UUID.randomUUID());
        dist.setFormat("application/json");
        dist.setDownloadURL(List.of("http://download.me")); // Nested Element
        dist.setAccessService(List.of(wsLink)); // Linked to WS

        LinkedEntity distLink = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name())
                .create(dist, StatusType.PUBLISHED, null, null);

        dp.setDistribution(List.of(distLink));

        // CREATE FINAL DATA PRODUCT
        System.out.println("--- CREATING FINAL DATA PRODUCT ---");
        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .create(dp, StatusType.PUBLISHED, null, null);


        // =================================================================================
        // 8. VERIFICATION
        // =================================================================================
        System.out.println("--- VERIFYING DATA INTEGRITY ---");

        DataProduct retrievedDp = (DataProduct) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .retrieveByUID(DATAPRODUCT_UID);

        assertNotNull(retrievedDp, "DataProduct must exist");
        assertEquals(1, retrievedDp.getPublisher().size(), "Publisher missing");
        assertEquals(ORG_UID, retrievedDp.getPublisher().get(0).getUid());

        assertEquals(1, retrievedDp.getDistribution().size(), "Distribution missing");

        // Deep Dive: Verify Nested Elements created successfully
        Organization retrievedOrg = (Organization) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieveByUID(ORG_UID);
        assertNotNull(retrievedOrg.getEmail(), "Org Email missing");
        assertEquals("info@gsi.org", retrievedOrg.getEmail().get(0));

        Person retrievedPerson = (Person) AbstractAPI.retrieveAPI(EntityNames.PERSON.name())
                .retrieveByUID(PERSON_UID);
        assertNotNull(retrievedPerson.getEmail(), "Person Email missing");

        System.out.println("TEST SUCCESS: All entities created and linked without NPE or FK violations.");
    }
}