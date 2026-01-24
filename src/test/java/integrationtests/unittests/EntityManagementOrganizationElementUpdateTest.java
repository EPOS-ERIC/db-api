package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntityManagementOrganizationElementUpdateTest extends TestcontainersLifecycle {

    private final String ORG_UID = "https://example.org/org/test-fk-violation-" + UUID.randomUUID();

    @Test
    @Order(1)
    public void testOrganizationElementUpdateDoesNotCrash() {
        AbstractAPI orgApi = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        // =================================================================================
        // STEP 1: Create Organization with an Element (Telephone)
        // =================================================================================
        System.out.println("--- STEP 1: Creating Organization with Telephone ---");

        Organization orgV1 = new Organization();
        orgV1.setUid(ORG_UID);
        orgV1.setLegalName(List.of("Test Org With Elements"));
        // This creates an entry in the 'Element' table and a link in 'OrganizationElement'
        orgV1.setTelephone(List.of("+39 06 12345678"));
        orgV1.setEmail(List.of("test@ingv.it"));

        // Setting IDs explicitly to control the update process later
        String instanceId = UUID.randomUUID().toString();
        String metaId = UUID.randomUUID().toString();
        orgV1.setInstanceId(instanceId);
        orgV1.setMetaId(metaId);

        LinkedEntity createdV1 = orgApi.create(orgV1, StatusType.PUBLISHED, null, null);
        assertNotNull(createdV1);

        // Verify insertion
        Organization retrievedV1 = (Organization) orgApi.retrieve(instanceId);
        assertNotNull(retrievedV1.getTelephone());
        assertEquals(1, retrievedV1.getTelephone().size());
        assertEquals("+39 06 12345678", retrievedV1.getTelephone().get(0));

        // =================================================================================
        // STEP 2: Update Organization (Change Telephone)
        // This forces the API to delete the old Element.
        // IF THE BUG EXISTS: This will throw PSQLException (Foreign Key Violation).
        // =================================================================================
        System.out.println("--- STEP 2: Updating Organization (Replacing Telephone) ---");

        Organization orgV2 = new Organization();
        orgV2.setUid(ORG_UID);
        // reusing same IDs simulates an UPDATE on the same record
        orgV2.setInstanceId(instanceId);
        orgV2.setMetaId(metaId);
        orgV2.setLegalName(List.of("Test Org Updated"));

        // Changing the number forces deletion of the old element "+39 06 12345678"
        orgV2.setTelephone(List.of("+39 06 87654321"));
        // Removing email forces deletion of the email element
        orgV2.setEmail(null);

        try {
            // This is the line that will CRASH if OrganizationAPI.java is not fixed
            orgApi.create(orgV2, StatusType.PUBLISHED, null, null);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Update failed with exception: " + e.getMessage());
        }

        // =================================================================================
        // STEP 3: Verify the update
        // =================================================================================
        System.out.println("--- STEP 3: Verifying Data Integrity ---");

        Organization retrievedV2 = (Organization) orgApi.retrieve(instanceId);

        // Check Name updated
        assertEquals("Test Org Updated", retrievedV2.getLegalName().get(0));

        // Check Phone updated (Old one deleted, new one added)
        assertEquals(1, retrievedV2.getTelephone().size());
        assertEquals("+39 06 87654321", retrievedV2.getTelephone().get(0));

        // Check Email removed
        assertTrue(retrievedV2.getEmail() == null || retrievedV2.getEmail().isEmpty());

        System.out.println("TEST PASSED: Elements updated without Foreign Key violations.");
    }
}