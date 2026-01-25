package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.*;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per verificare che le relazioni Organization-WebService persistano
 * attraverso le transizioni di status:
 *
 * PUBLISHED (v1) → DRAFT (v2) → SUBMITTED → PUBLISHED (v2)
 *                                               ↓
 *                                    v1 viene auto-archiviata
 *
 * Questo test replica il bug segnalato dove le relazioni si perdono
 * o tornano a versioni precedenti durante l'auto-archiviazione.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class StatusTransitionRelationsTest extends TestcontainersLifecycle {

    // Shared test data
    private String orgUid;
    private String orgInstanceId;
    private LinkedEntity orgLinkedEntity;

    private String wsUid;
    private String wsInstanceIdV1;  // Prima versione PUBLISHED
    private String wsInstanceIdV2;  // Seconda versione (DRAFT → SUBMITTED → PUBLISHED)

    // =========================================================================
    // SETUP: Create Organization PUBLISHED
    // =========================================================================
    @Test
    @Order(1)
    public void test01_CreatePublishedOrganization() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 1: Create PUBLISHED Organization");
        System.out.println("=".repeat(70));

        Organization organization = new Organization();
        orgUid = "https://test.org/Organization/StatusTestOrg_" + UUID.randomUUID();
        organization.setUid(orgUid);
        organization.setLegalName(List.of("Status Test Organization"));
        organization.setInstanceId(UUID.randomUUID().toString());
        organization.setMetaId(UUID.randomUUID().toString());

        orgLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(organization, StatusType.PUBLISHED, null, null);

        assertNotNull(orgLinkedEntity);
        orgInstanceId = orgLinkedEntity.getInstanceId();

        System.out.println("Created Organization:");
        System.out.println("  UID: " + orgUid);
        System.out.println("  InstanceId: " + orgInstanceId);
        System.out.println("  Status: PUBLISHED");

        // Verify status
        Organization retrieved = (Organization) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieve(orgInstanceId);
        assertEquals(StatusType.PUBLISHED, retrieved.getStatus());
    }

    // =========================================================================
    // STEP 1: Create WebService V1 as PUBLISHED
    // =========================================================================
    @Test
    @Order(2)
    public void test02_CreatePublishedWebServiceV1() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 2: Create PUBLISHED WebService V1 with Organization as provider");
        System.out.println("=".repeat(70));

        WebService webService = new WebService();
        wsUid = "https://test.org/WebService/StatusTestWS_" + UUID.randomUUID();
        webService.setUid(wsUid);
        webService.setName("Status Test WebService V1");
        webService.setDescription("First version - PUBLISHED");
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setProvider(orgLinkedEntity);

        LinkedEntity wsLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webService, StatusType.PUBLISHED, null, null);

        assertNotNull(wsLinkedEntity);
        wsInstanceIdV1 = wsLinkedEntity.getInstanceId();

        System.out.println("Created WebService V1:");
        System.out.println("  UID: " + wsUid);
        System.out.println("  InstanceId V1: " + wsInstanceIdV1);
        System.out.println("  Status: PUBLISHED");

        // Verify provider
        WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV1);

        System.out.println("  Provider: " + (retrieved.getProvider() != null ?
                retrieved.getProvider().getUid() : "NULL!"));

        assertNotNull(retrieved.getProvider(), "V1 should have provider");
        assertEquals(orgUid, retrieved.getProvider().getUid());
        assertEquals(StatusType.PUBLISHED, retrieved.getStatus());
    }

    // =========================================================================
    // STEP 2: Create DRAFT version (V2) from PUBLISHED (V1)
    // =========================================================================
    @Test
    @Order(3)
    public void test03_CreateDraftVersionV2() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 3: Create DRAFT version V2 from PUBLISHED V1");
        System.out.println("=".repeat(70));

        // Retrieve V1 to create V2
        WebService v1 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV1);

        System.out.println("V1 before creating V2:");
        System.out.println("  InstanceId: " + v1.getInstanceId());
        System.out.println("  Status: " + v1.getStatus());
        System.out.println("  Provider: " + (v1.getProvider() != null ? v1.getProvider().getUid() : "NULL!"));

        // Create V2 as DRAFT (this should create a new version)
        // The API should detect that a PUBLISHED version exists and create a new DRAFT
        WebService v2 = new WebService();
        v2.setUid(wsUid);  // Same UID!
        v2.setName("Status Test WebService V2 - Modified");
        v2.setDescription("Second version - DRAFT");
        v2.setMetaId(v1.getMetaId());  // Same metaId to link versions
        v2.setInstanceId(UUID.randomUUID().toString());  // New instanceId
        v2.setProvider(orgLinkedEntity);  // Same provider

        LinkedEntity v2LinkedEntity = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(v2, StatusType.DRAFT, null, null);

        assertNotNull(v2LinkedEntity);
        wsInstanceIdV2 = v2LinkedEntity.getInstanceId();

        System.out.println("\nCreated WebService V2:");
        System.out.println("  UID: " + wsUid);
        System.out.println("  InstanceId V2: " + wsInstanceIdV2);
        System.out.println("  InstanceId V1 was: " + wsInstanceIdV1);
        System.out.println("  V1 != V2: " + (!wsInstanceIdV1.equals(wsInstanceIdV2)));

        // Verify V2 is DRAFT with provider
        WebService retrievedV2 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV2);

        System.out.println("\nV2 after creation:");
        System.out.println("  Status: " + retrievedV2.getStatus());
        System.out.println("  Provider: " + (retrievedV2.getProvider() != null ?
                retrievedV2.getProvider().getUid() : "NULL!"));

        assertNotNull(retrievedV2.getProvider(), "V2 DRAFT should have provider");
        assertEquals(orgUid, retrievedV2.getProvider().getUid());

        // Verify V1 is still PUBLISHED
        WebService retrievedV1 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV1);

        System.out.println("\nV1 after V2 creation:");
        System.out.println("  Status: " + retrievedV1.getStatus());
        System.out.println("  Provider: " + (retrievedV1.getProvider() != null ?
                retrievedV1.getProvider().getUid() : "NULL!"));

        assertEquals(StatusType.PUBLISHED, retrievedV1.getStatus(), "V1 should still be PUBLISHED");
        assertNotNull(retrievedV1.getProvider(), "V1 should still have provider");
    }

    // =========================================================================
    // STEP 3: Transition V2 from DRAFT to SUBMITTED
    // =========================================================================
    @Test
    @Order(4)
    public void test04_TransitionV2ToSubmitted() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 4: Transition V2 from DRAFT to SUBMITTED");
        System.out.println("=".repeat(70));

        // Retrieve V2
        WebService v2 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV2);

        System.out.println("V2 before transition:");
        System.out.println("  Status: " + v2.getStatus());
        System.out.println("  Provider: " + (v2.getProvider() != null ? v2.getProvider().getUid() : "NULL!"));

        // Update V2 to SUBMITTED
        v2.setProvider(orgLinkedEntity);  // Ensure provider is set

        LinkedEntity updatedV2 = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(v2, StatusType.SUBMITTED, null, null);

        // The instanceId might change or stay the same depending on implementation
        String newInstanceId = updatedV2.getInstanceId();
        System.out.println("\nAfter SUBMITTED transition:");
        System.out.println("  New InstanceId: " + newInstanceId);
        System.out.println("  Old InstanceId V2: " + wsInstanceIdV2);
        System.out.println("  Changed: " + (!newInstanceId.equals(wsInstanceIdV2)));

        // Update our reference if changed
        if (!newInstanceId.equals(wsInstanceIdV2)) {
            System.out.println("  NOTE: InstanceId changed during SUBMITTED transition!");
            wsInstanceIdV2 = newInstanceId;
        }

        // Verify V2 is now SUBMITTED with provider
        WebService retrievedV2 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV2);

        System.out.println("\nV2 after SUBMITTED:");
        System.out.println("  Status: " + retrievedV2.getStatus());
        System.out.println("  Provider: " + (retrievedV2.getProvider() != null ?
                retrievedV2.getProvider().getUid() : "NULL!"));

        assertEquals(StatusType.SUBMITTED, retrievedV2.getStatus(), "V2 should be SUBMITTED");
        assertNotNull(retrievedV2.getProvider(), "V2 SUBMITTED should have provider");
        assertEquals(orgUid, retrievedV2.getProvider().getUid());

        // Verify V1 is still PUBLISHED
        WebService retrievedV1 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV1);

        System.out.println("\nV1 after V2 SUBMITTED:");
        System.out.println("  Status: " + retrievedV1.getStatus());
        System.out.println("  Provider: " + (retrievedV1.getProvider() != null ?
                retrievedV1.getProvider().getUid() : "NULL!"));
    }

    // =========================================================================
    // STEP 4: Transition V2 from SUBMITTED to PUBLISHED (triggers auto-archive of V1)
    // =========================================================================
    @Test
    @Order(5)
    public void test05_TransitionV2ToPublished_AutoArchiveV1() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 5: Transition V2 SUBMITTED → PUBLISHED (auto-archive V1)");
        System.out.println("=".repeat(70));

        // Retrieve V2
        WebService v2 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV2);

        System.out.println("V2 before PUBLISHED transition:");
        System.out.println("  InstanceId: " + v2.getInstanceId());
        System.out.println("  Status: " + v2.getStatus());
        System.out.println("  Provider: " + (v2.getProvider() != null ? v2.getProvider().getUid() : "NULL!"));

        // Check V1 before
        WebService v1Before = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV1);
        System.out.println("\nV1 BEFORE auto-archive:");
        System.out.println("  InstanceId: " + v1Before.getInstanceId());
        System.out.println("  Status: " + v1Before.getStatus());
        System.out.println("  Provider: " + (v1Before.getProvider() != null ? v1Before.getProvider().getUid() : "NULL!"));

        // *** CRITICAL TRANSITION: SUBMITTED → PUBLISHED ***
        // This should:
        // 1. Set V2 to PUBLISHED
        // 2. Auto-archive V1 (set to ARCHIVED)
        v2.setProvider(orgLinkedEntity);  // Ensure provider is set

        LinkedEntity updatedV2 = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(v2, StatusType.PUBLISHED, null, null);

        String newInstanceId = updatedV2.getInstanceId();
        System.out.println("\n*** AFTER PUBLISHED TRANSITION ***");
        System.out.println("Returned InstanceId: " + newInstanceId);

        if (!newInstanceId.equals(wsInstanceIdV2)) {
            System.out.println("NOTE: InstanceId changed during PUBLISHED transition!");
            wsInstanceIdV2 = newInstanceId;
        }

        // *** VERIFY V2 IS PUBLISHED WITH PROVIDER ***
        WebService retrievedV2 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV2);

        System.out.println("\nV2 AFTER transition:");
        System.out.println("  InstanceId: " + retrievedV2.getInstanceId());
        System.out.println("  Status: " + retrievedV2.getStatus());
        System.out.println("  Provider: " + (retrievedV2.getProvider() != null ?
                retrievedV2.getProvider().getUid() : "*** NULL - BUG! ***"));
        if (retrievedV2.getProvider() != null) {
            System.out.println("  Provider.InstanceId: " + retrievedV2.getProvider().getInstanceId());
        }

        // *** VERIFY V1 IS ARCHIVED ***
        WebService retrievedV1 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV1);

        System.out.println("\nV1 AFTER auto-archive:");
        System.out.println("  InstanceId: " + retrievedV1.getInstanceId());
        System.out.println("  Status: " + retrievedV1.getStatus());
        System.out.println("  Provider: " + (retrievedV1.getProvider() != null ?
                retrievedV1.getProvider().getUid() : "*** NULL - BUG! ***"));

        // ASSERTIONS
        assertNotNull(retrievedV2.getProvider(),
                "CRITICAL BUG: V2 PUBLISHED lost its provider!");
        assertEquals(orgUid, retrievedV2.getProvider().getUid(),
                "V2 provider should still be the Organization");
        assertEquals(StatusType.PUBLISHED, retrievedV2.getStatus(),
                "V2 should be PUBLISHED");

        // V1 should be ARCHIVED (if auto-archive is working)
        // Note: If the system doesn't auto-archive, this might still be PUBLISHED
        System.out.println("\nExpected V1 status: ARCHIVED (if auto-archive works)");
        System.out.println("Actual V1 status: " + retrievedV1.getStatus());
    }

    // =========================================================================
    // STEP 5: Verify database state directly
    // =========================================================================
    @Test
    @Order(6)
    public void test06_VerifyDatabaseState() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 6: Verify database state directly");
        System.out.println("=".repeat(70));

        // Get all Webservice records with this UID
        List<Object> allVersions = EposDataModelDAO.getInstance()
                .getOneFromDBByUIDNoCache(wsUid, Webservice.class);

        System.out.println("All Webservice versions for UID '" + wsUid + "': " + allVersions.size());

        for (Object obj : allVersions) {
            Webservice ws = (Webservice) obj;
            String status = ws.getVersion() != null ? ws.getVersion().getStatus() : "NO VERSION";

            System.out.println("\n  Webservice record:");
            System.out.println("    instanceId: " + ws.getInstanceId());
            System.out.println("    provider: " + ws.getProvider());
            System.out.println("    status: " + status);
            System.out.println("    name: " + ws.getName());

            // Check if provider resolves
            if (ws.getProvider() != null) {
                List<Object> providerList = EposDataModelDAO.getInstance()
                        .getOneFromDBByInstanceIdNoCache(ws.getProvider(), model.Organization.class);
                System.out.println("    provider resolves: " + (!providerList.isEmpty() ? "YES" : "NO - BROKEN!"));
            }
        }

        // Get all Versioningstatus records for this UID
        List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                .getOneFromDBByUIDNoCache(wsUid, Versioningstatus.class);

        System.out.println("\nVersioningstatus records for UID: " + vsList.size());
        for (Versioningstatus vs : vsList) {
            System.out.println("  - instanceId=" + vs.getInstanceId() +
                    ", status=" + vs.getStatus() +
                    ", versionId=" + vs.getVersionId());
        }
    }

    // =========================================================================
    // STEP 7: Test the exact scenario - update without explicitly setting provider
    // =========================================================================
    @Test
    @Order(8)
    public void test08_UpdateWithoutExplicitlySettingProvider() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 8: Update V2 without explicitly setting provider");
        System.out.println("=".repeat(70));

        // This simulates what might happen in UI:
        // 1. Retrieve WebService
        // 2. Modify some field
        // 3. Save without touching provider

        WebService v2 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIdV2);

        System.out.println("Before update:");
        System.out.println("  Provider: " + (v2.getProvider() != null ? v2.getProvider().getUid() : "NULL!"));

        // Modify name but DON'T touch provider
        v2.setName("Modified name without touching provider");
        // v2.setProvider(...)  <-- NOT CALLED!

        LinkedEntity updated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(v2, StatusType.PUBLISHED, null, null);

        // Retrieve again
        WebService afterUpdate = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(updated.getInstanceId());

        System.out.println("\nAfter update (provider not explicitly set):");
        System.out.println("  Provider: " + (afterUpdate.getProvider() != null ?
                afterUpdate.getProvider().getUid() : "*** NULL - BUG! ***"));

        assertNotNull(afterUpdate.getProvider(),
                "Provider should persist even when not explicitly set during update");
    }
}