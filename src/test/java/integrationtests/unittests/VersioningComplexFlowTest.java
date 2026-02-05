package integrationtests.unittests;

import abstractapis.AbstractAPI;
import metadataapis.DataProductAPI;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.*;
import integrationtests.TestcontainersLifecycle;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VersioningComplexFlowTest extends TestcontainersLifecycle {

    private final String COMMON_UID = "https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/COMPLEX-001";
    private final String ORG_UID = "PIC:999999999";
    private final String CONTACT_UID = "http://orcid.org/0000-0000-0000-0000/contact";

    @Test
    @Order(1)
    @DisplayName("Complex Workflow: Draft -> Publish -> Branching (New Draft) -> Publish V2 (Auto-Archive V1)")
    public void testComplexVersioningWorkflow() {

        // =================================================================================
        // PHASE 1: SETUP DEPENDENCIES (Organization & ContactPoint)
        // =================================================================================
        System.out.println("--- PHASE 1: Creating Dependencies (Organization, ContactPoint) ---");

        // 1. Create Organization
        Organization org = new Organization();
        org.setUid(ORG_UID);
        org.setLegalName(List.of("Test Institute for Versioning"));
        org.setStatus(StatusType.PUBLISHED); // Dependencies are usually published

        AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name()).create(org, StatusType.PUBLISHED, null, null);

        // 2. Create ContactPoint
        ContactPoint cp = new ContactPoint();
        cp.setUid(CONTACT_UID);
        cp.setRole("Dr. Test User");
        cp.setStatus(StatusType.PUBLISHED);

        AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name()).create(cp, StatusType.PUBLISHED, null, null);

        // =================================================================================
        // PHASE 2: CREATE DATA PRODUCT V1 (DRAFT)
        // =================================================================================
        System.out.println("--- PHASE 2: Creating DataProduct V1 (DRAFT) ---");

        DataProduct dpV1 = new DataProduct();
        dpV1.setUid(COMMON_UID);
        dpV1.setTitle(List.of("Seismic Dataset - Version 1"));
        dpV1.setDescription(List.of("Initial description of the dataset"));
        dpV1.setIssued(OffsetDateTime.now().toLocalDateTime());
        dpV1.setModified(OffsetDateTime.now().toLocalDateTime());
        dpV1.setStatus(StatusType.DRAFT);

        // Link Organization (Publisher)
        LinkedEntity linkOrg = new LinkedEntity();
        linkOrg.setUid(ORG_UID);
        linkOrg.setEntityType(EntityNames.ORGANIZATION.name());
        dpV1.addPublisher(linkOrg);

        // Link ContactPoint
        LinkedEntity linkCp = new LinkedEntity();
        linkCp.setUid(CONTACT_UID);
        linkCp.setEntityType(EntityNames.CONTACTPOINT.name());
        dpV1.addContactPoint(linkCp);

        // Create V1
        DataProductAPI dpApi = (DataProductAPI) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        LinkedEntity createdV1 = dpApi.create(dpV1, StatusType.DRAFT, null, null);

        String v1InstanceId = createdV1.getInstanceId();
        assertNotNull(v1InstanceId, "V1 Instance ID should not be null");

        // Verify V1 State
        DataProduct retrievedV1 = dpApi.retrieve(v1InstanceId);
        assertEquals(StatusType.DRAFT, retrievedV1.getStatus());
        assertEquals("Seismic Dataset - Version 1", retrievedV1.getTitle().get(0));
        assertEquals(1, retrievedV1.getPublisher().size(), "V1 should have 1 Publisher");

        // =================================================================================
        // PHASE 3: PUBLISH V1
        // =================================================================================
        System.out.println("--- PHASE 3: Publishing V1 ---");

        retrievedV1.setStatus(StatusType.PUBLISHED);
        // We simulate an update during publication (e.g. adding a keyword)
        retrievedV1.addKeywords("published");

        LinkedEntity publishedV1Link = dpApi.create(retrievedV1, StatusType.PUBLISHED, null, null);

        // Assert ID is same (Update, not new version)
        assertEquals(v1InstanceId, publishedV1Link.getInstanceId(), "InstanceID should remain the same when promoting DRAFT -> PUBLISHED");

        DataProduct publishedV1 = dpApi.retrieve(v1InstanceId);
        assertEquals(StatusType.PUBLISHED, publishedV1.getStatus());

        // =================================================================================
        // PHASE 4: BRANCHING - CREATE V2 (DRAFT) FROM V1
        // =================================================================================
        System.out.println("--- PHASE 4: Branching V2 (DRAFT) from V1 ---");

        // We simulate a payload coming from an external editor.
        // It has the SAME UID but NO InstanceId, and Status = DRAFT.
        DataProduct dpV2Draft = new DataProduct();
        dpV2Draft.setUid(COMMON_UID); // Same UID as V1
        dpV2Draft.setStatus(StatusType.DRAFT); // Requesting DRAFT
        dpV2Draft.setTitle(List.of("Seismic Dataset - Version 2 (Improved)")); // Changed Title
        dpV2Draft.setDescription(List.of("Updated description for V2"));

        // Re-link dependencies (simulating full payload)
        dpV2Draft.addPublisher(linkOrg);
        dpV2Draft.addContactPoint(linkCp);

        // Create V2
        LinkedEntity createdV2 = dpApi.create(dpV2Draft, StatusType.DRAFT, null, null);
        String v2InstanceId = createdV2.getInstanceId();

        // Assertions for Branching
        assertNotEquals(v1InstanceId, v2InstanceId, "V2 should have a NEW Instance ID");

        DataProduct retrievedV2 = dpApi.retrieve(v2InstanceId);
        assertEquals(StatusType.DRAFT, retrievedV2.getStatus());
        assertEquals("Seismic Dataset - Version 2 (Improved)", retrievedV2.getTitle().get(0));

        // Check Lineage
        assertEquals(v1InstanceId, retrievedV2.getInstanceChangedId(), "V2 should point to V1 as previous version");

        // Check Relations Preserved
        assertEquals(1, retrievedV2.getPublisher().size(), "V2 should still have the Publisher");

        // Verify V1 is UNTOUCHED
        DataProduct checkV1 = dpApi.retrieve(v1InstanceId);
        assertEquals(StatusType.PUBLISHED, checkV1.getStatus(), "V1 should remain PUBLISHED while V2 is in DRAFT");

        // =================================================================================
        // PHASE 5: PUBLISH V2 AND AUTO-ARCHIVE V1
        // =================================================================================
        System.out.println("--- PHASE 5: Publishing V2 (Auto-Archiving V1) ---");

        retrievedV2.setStatus(StatusType.PUBLISHED);
        dpApi.create(retrievedV2, StatusType.PUBLISHED, null, null);

        // Verify V2 is PUBLISHED
        DataProduct finalV2 = dpApi.retrieve(v2InstanceId);
        assertEquals(StatusType.PUBLISHED, finalV2.getStatus());

        // Verify V1 is ARCHIVED
        DataProduct finalV1 = dpApi.retrieve(v1InstanceId);
        assertEquals(StatusType.ARCHIVED, finalV1.getStatus(), "V1 should be auto-archived after V2 publication");

        System.out.println("Test Completed Successfully!");
    }
}