package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.ContactPoint;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class IngestionPendingStubTest extends TestcontainersLifecycle {

    private static final String ORG_UID = "https://example.org/org/stub-test-" + UUID.randomUUID();
    private static final String CONTACT_UID = "https://example.org/contact/stub-test-" + UUID.randomUUID();

    @BeforeEach
    public void setup() {
        cleanup();
    }

    @AfterEach
    public void tearDown() {
        cleanup();
    }

    private void cleanup() {
        try {
            AbstractAPI orgApi = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());
            AbstractAPI cpApi = AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name());

            Organization org = (Organization) orgApi.retrieveByUID(ORG_UID);
            if (org != null) orgApi.delete(org.getInstanceId());

            ContactPoint cp = (ContactPoint) cpApi.retrieveByUID(CONTACT_UID);
            if (cp != null) cpApi.delete(cp.getInstanceId());
        } catch (Exception e) {
            System.out.println("Cleanup warning: " + e.getMessage());
        }
    }

    @Test
    public void testStubEntityUpdateDoesNotCrash() {
        // =================================================================================
        // STEP 1: Trigger Implicit Stub Creation
        // Create an Organization that links to a NON-EXISTENT ContactPoint.
        // This forces the system to create a "Stub" ContactPoint in the DB.
        // =================================================================================
        System.out.println("--- STEP 1: Creating Organization (Triggering ContactPoint Stub) ---");

        LinkedEntity contactLink = new LinkedEntity();
        contactLink.setUid(CONTACT_UID);
        contactLink.setEntityType(EntityNames.CONTACTPOINT.name());

        Organization org = new Organization();
        org.setUid(ORG_UID);
        org.setLegalName(List.of("Organization With Stub Contact"));
        org.setContactPoint(List.of(contactLink)); // This link triggers creation

        // Execute Creation
        AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(org, StatusType.PUBLISHED, null, null);

        // Verify Stub Exists
        AbstractAPI cpApi = AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name());
        ContactPoint stubCp = (ContactPoint) cpApi.retrieveByUID(CONTACT_UID);

        assertNotNull(stubCp, "The Stub ContactPoint should have been created implicitly");
        System.out.println("Stub Created. Instance ID: " + stubCp.getInstanceId());

        // =================================================================================
        // STEP 2: Update the Stub with actual Data (The Crash Point)
        // Now we simulate ingesting the real ContactPoint definition.
        // We add an EMAIL, which triggers 'createInnerElement'.
        // IF 'getVersion()' is null on the stub, this will throw NPE.
        // =================================================================================
        System.out.println("--- STEP 2: Updating Stub with Email (The Potential Crash) ---");

        ContactPoint realCp = new ContactPoint();
        realCp.setUid(CONTACT_UID);
        realCp.setRole("Updated Real Contact");
        realCp.setEmail(List.of("test@example.org")); // <--- This triggers element creation

        try {
            // This line calls ContactPointAPI.create -> createInnerElement
            // It will crash with NPE if the fix is not applied.
            cpApi.create(realCp, StatusType.PUBLISHED, null, null);

            System.out.println("SUCCESS: Stub updated without crash.");
        } catch (NullPointerException e) {
            e.printStackTrace();
            fail("CRASH DETECTED: NullPointerException during Stub update! The fix for getVersion() is missing.");
        } catch (Exception e) {
            e.printStackTrace();
            fail("Update failed with exception: " + e.getMessage());
        }

        // =================================================================================
        // STEP 3: Verify Data
        // =================================================================================
        ContactPoint updatedCp = (ContactPoint) cpApi.retrieveByUID(CONTACT_UID);
        assertNotNull(updatedCp.getEmail());
        assertFalse(updatedCp.getEmail().isEmpty());
        assertEquals("test@example.org", updatedCp.getEmail().get(0));

        System.out.println("TEST PASSED: Stub entity handled correctly.");
    }
}