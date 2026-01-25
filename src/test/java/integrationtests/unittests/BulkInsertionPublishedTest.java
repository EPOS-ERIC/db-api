package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.*;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test per verificare il comportamento durante l'inserimento massivo iniziale
 * con overrideStatus=PUBLISHED.
 *
 * Scenario: Inserimento di molte entità correlate, tutte con PUBLISHED,
 * in chiamate separate (come avviene durante l'importazione iniziale).
 *
 * Problema segnalato: alcune relazioni si perdono o puntano a versioni sbagliate.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class BulkInsertionPublishedTest extends TestcontainersLifecycle {

    // Shared test data
    private List<String> orgInstanceIds = new ArrayList<>();
    private List<String> orgUids = new ArrayList<>();
    private List<LinkedEntity> orgLinkedEntities = new ArrayList<>();

    private List<String> wsInstanceIds = new ArrayList<>();
    private List<String> wsUids = new ArrayList<>();

    private List<String> dpInstanceIds = new ArrayList<>();
    private List<String> dpUids = new ArrayList<>();

    // =========================================================================
    // TEST 1: Create multiple Organizations as PUBLISHED
    // =========================================================================
    @Test
    @Order(1)
    public void test01_CreateMultipleOrganizationsPublished() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 1: Create 3 Organizations as PUBLISHED");
        System.out.println("=".repeat(70));

        for (int i = 0; i < 3; i++) {
            Organization org = new Organization();
            String uid = "https://test.org/Organization/BulkOrg_" + i + "_" + UUID.randomUUID();
            org.setUid(uid);
            org.setLegalName(List.of("Bulk Test Organization " + i));
            org.setInstanceId(UUID.randomUUID().toString());
            org.setMetaId(UUID.randomUUID().toString());

            LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                    .create(org, StatusType.PUBLISHED, null, null);

            assertNotNull(le, "Organization " + i + " creation failed");

            orgInstanceIds.add(le.getInstanceId());
            orgUids.add(uid);
            orgLinkedEntities.add(le);

            System.out.println("Created Organization " + i + ":");
            System.out.println("  UID: " + uid);
            System.out.println("  InstanceId: " + le.getInstanceId());
        }

        // Verify all organizations exist and are PUBLISHED
        System.out.println("\nVerifying all Organizations:");
        for (int i = 0; i < 3; i++) {
            Organization retrieved = (Organization) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                    .retrieve(orgInstanceIds.get(i));
            System.out.println("  Org " + i + ": status=" + retrieved.getStatus());
            assertEquals(StatusType.PUBLISHED, retrieved.getStatus());
        }
    }

    // =========================================================================
    // TEST 2: Create WebServices with different Organizations as providers
    // =========================================================================
    @Test
    @Order(2)
    public void test02_CreateWebServicesWithDifferentProviders() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 2: Create 3 WebServices, each with different Organization as provider");
        System.out.println("=".repeat(70));

        for (int i = 0; i < 3; i++) {
            WebService ws = new WebService();
            String uid = "https://test.org/WebService/BulkWS_" + i + "_" + UUID.randomUUID();
            ws.setUid(uid);
            ws.setName("Bulk Test WebService " + i);
            ws.setDescription("WebService with provider Org " + i);
            ws.setInstanceId(UUID.randomUUID().toString());
            ws.setMetaId(UUID.randomUUID().toString());
            ws.setProvider(orgLinkedEntities.get(i));  // Each WS has different provider

            LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .create(ws, StatusType.PUBLISHED, null, null);

            assertNotNull(le, "WebService " + i + " creation failed");

            wsInstanceIds.add(le.getInstanceId());
            wsUids.add(uid);

            System.out.println("Created WebService " + i + ":");
            System.out.println("  UID: " + uid);
            System.out.println("  InstanceId: " + le.getInstanceId());
            System.out.println("  Provider (expected): " + orgUids.get(i));
        }

        // Immediately verify all WebServices have correct providers
        System.out.println("\nVerifying WebService providers immediately after creation:");
        for (int i = 0; i < 3; i++) {
            WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(wsInstanceIds.get(i));

            String expectedProviderUid = orgUids.get(i);
            String actualProviderUid = retrieved.getProvider() != null ?
                    retrieved.getProvider().getUid() : "NULL!";

            System.out.println("  WS " + i + ": provider=" + actualProviderUid +
                    " (expected: " + expectedProviderUid + ")");

            assertNotNull(retrieved.getProvider(),
                    "WebService " + i + " should have provider");
            assertEquals(expectedProviderUid, actualProviderUid,
                    "WebService " + i + " has wrong provider");
        }
    }

    // =========================================================================
    // TEST 3: Create DataProducts with Organization as publisher
    // =========================================================================
    @Test
    @Order(3)
    public void test03_CreateDataProductsWithPublishers() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 3: Create 3 DataProducts, each with Organization as publisher");
        System.out.println("=".repeat(70));

        for (int i = 0; i < 3; i++) {
            DataProduct dp = new DataProduct();
            String uid = "https://test.org/DataProduct/BulkDP_" + i + "_" + UUID.randomUUID();
            dp.setUid(uid);
            dp.setTitle(List.of("Bulk Test DataProduct " + i));
            dp.setDescription(List.of("DataProduct with publisher Org " + i));
            dp.setInstanceId(UUID.randomUUID().toString());
            dp.setMetaId(UUID.randomUUID().toString());
            dp.setPublisher(List.of(orgLinkedEntities.get(i)));  // Each DP has different publisher

            LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                    .create(dp, StatusType.PUBLISHED, null, null);

            assertNotNull(le, "DataProduct " + i + " creation failed");

            dpInstanceIds.add(le.getInstanceId());
            dpUids.add(uid);

            System.out.println("Created DataProduct " + i + ":");
            System.out.println("  UID: " + uid);
            System.out.println("  InstanceId: " + le.getInstanceId());
            System.out.println("  Publisher (expected): " + orgUids.get(i));
        }

        // Immediately verify all DataProducts have correct publishers
        System.out.println("\nVerifying DataProduct publishers immediately after creation:");
        for (int i = 0; i < 3; i++) {
            DataProduct retrieved = (DataProduct) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                    .retrieve(dpInstanceIds.get(i));

            String expectedPublisherUid = orgUids.get(i);
            String actualPublisherUid = (retrieved.getPublisher() != null && !retrieved.getPublisher().isEmpty()) ?
                    retrieved.getPublisher().get(0).getUid() : "NULL/EMPTY!";

            System.out.println("  DP " + i + ": publisher=" + actualPublisherUid +
                    " (expected: " + expectedPublisherUid + ")");

            assertNotNull(retrieved.getPublisher(),
                    "DataProduct " + i + " should have publisher");
            assertFalse(retrieved.getPublisher().isEmpty(),
                    "DataProduct " + i + " publisher list should not be empty");
            assertEquals(expectedPublisherUid, actualPublisherUid,
                    "DataProduct " + i + " has wrong publisher");
        }
    }

    // =========================================================================
    // TEST 4: Verify WebServices still have correct providers after DataProduct creation
    // =========================================================================
    @Test
    @Order(4)
    public void test04_VerifyWebServicesAfterDataProductCreation() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 4: Verify WebServices still have correct providers");
        System.out.println("        (after DataProducts were created with same Organizations)");
        System.out.println("=".repeat(70));

        int failures = 0;
        for (int i = 0; i < 3; i++) {
            WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(wsInstanceIds.get(i));

            String expectedProviderUid = orgUids.get(i);
            String actualProviderUid = retrieved.getProvider() != null ?
                    retrieved.getProvider().getUid() : "NULL!";

            boolean correct = expectedProviderUid.equals(actualProviderUid);

            System.out.println("WebService " + i + ":");
            System.out.println("  Expected provider: " + expectedProviderUid);
            System.out.println("  Actual provider: " + actualProviderUid);
            System.out.println("  Status: " + (correct ? "OK" : "*** WRONG! ***"));

            if (!correct) failures++;
        }

        assertEquals(0, failures, failures + " WebServices have wrong providers!");
    }

    // =========================================================================
    // TEST 5: Create WebService with SAME Organization that's already used
    // =========================================================================
    @Test
    @Order(5)
    public void test05_CreateAnotherWebServiceWithSameOrganization() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 5: Create ANOTHER WebService with Organization 0 (already used by WS0 and DP0)");
        System.out.println("=".repeat(70));

        // Use Organization 0 again
        LinkedEntity org0 = orgLinkedEntities.get(0);
        System.out.println("Using Organization 0:");
        System.out.println("  UID: " + org0.getUid());
        System.out.println("  InstanceId: " + org0.getInstanceId());

        WebService ws = new WebService();
        String uid = "https://test.org/WebService/BulkWS_Extra_" + UUID.randomUUID();
        ws.setUid(uid);
        ws.setName("Extra WebService using Org 0");
        ws.setDescription("This WS also uses Organization 0");
        ws.setInstanceId(UUID.randomUUID().toString());
        ws.setMetaId(UUID.randomUUID().toString());
        ws.setProvider(org0);

        LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(ws, StatusType.PUBLISHED, null, null);

        assertNotNull(le, "Extra WebService creation failed");

        System.out.println("\nCreated Extra WebService:");
        System.out.println("  UID: " + uid);
        System.out.println("  InstanceId: " + le.getInstanceId());

        // Verify this new WS has correct provider
        WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(le.getInstanceId());

        System.out.println("\nVerifying Extra WebService:");
        System.out.println("  Provider: " + (retrieved.getProvider() != null ?
                retrieved.getProvider().getUid() : "NULL!"));

        assertNotNull(retrieved.getProvider(), "Extra WebService should have provider");
        assertEquals(orgUids.get(0), retrieved.getProvider().getUid());

        // NOW CHECK: Does WS0 still have its provider?
        WebService ws0 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIds.get(0));

        System.out.println("\nVerifying WS0 after Extra WS creation:");
        System.out.println("  Provider: " + (ws0.getProvider() != null ?
                ws0.getProvider().getUid() : "*** NULL - BUG! ***"));

        assertNotNull(ws0.getProvider(),
                "CRITICAL: WS0 lost its provider after creating another WS with same Organization!");
    }

    // =========================================================================
    // TEST 6: Bulk create many entities rapidly
    // =========================================================================
    @Test
    @Order(6)
    public void test06_BulkCreateManyEntitiesRapidly() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 6: Bulk create 10 WebServices with same Organization rapidly");
        System.out.println("=".repeat(70));

        // Use Organization 1
        LinkedEntity org1 = orgLinkedEntities.get(1);
        System.out.println("Using Organization 1: " + org1.getUid());

        List<String> bulkWsInstanceIds = new ArrayList<>();

        // Rapidly create 10 WebServices
        for (int i = 0; i < 10; i++) {
            WebService ws = new WebService();
            ws.setUid("https://test.org/WebService/Rapid_" + i + "_" + UUID.randomUUID());
            ws.setName("Rapid WebService " + i);
            ws.setDescription("Rapidly created WS " + i);
            ws.setInstanceId(UUID.randomUUID().toString());
            ws.setMetaId(UUID.randomUUID().toString());
            ws.setProvider(org1);

            LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .create(ws, StatusType.PUBLISHED, null, null);

            bulkWsInstanceIds.add(le.getInstanceId());
        }

        System.out.println("Created 10 WebServices");

        // Verify ALL have correct provider
        int correctCount = 0;
        int wrongCount = 0;

        System.out.println("\nVerifying all 10 WebServices:");
        for (int i = 0; i < 10; i++) {
            WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(bulkWsInstanceIds.get(i));

            boolean hasCorrectProvider = retrieved.getProvider() != null &&
                    orgUids.get(1).equals(retrieved.getProvider().getUid());

            if (hasCorrectProvider) {
                correctCount++;
            } else {
                wrongCount++;
                System.out.println("  WS development-k8s-epos-deploy-latest/ingestor-deployment-7bdd86565b-shq4m" + i + ": provider=" +
                        (retrieved.getProvider() != null ? retrieved.getProvider().getUid() : "NULL") +
                        " *** WRONG! ***");
            }
        }

        System.out.println("\nResults: " + correctCount + " correct, " + wrongCount + " wrong");

        assertEquals(0, wrongCount, wrongCount + " WebServices have wrong/missing providers!");
    }

    // =========================================================================
    // TEST 7: Verify database state directly
    // =========================================================================
    @Test
    @Order(7)
    public void test07_VerifyDatabaseStateDirect() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 7: Verify database state directly");
        System.out.println("=".repeat(70));

        // Check Webservice.provider field directly
        System.out.println("Checking Webservice.provider field in database:");

        for (int i = 0; i < wsInstanceIds.size(); i++) {
            List<Object> results = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(wsInstanceIds.get(i), Webservice.class);

            if (!results.isEmpty()) {
                Webservice ws = (Webservice) results.get(0);
                System.out.println("  WS " + i + " (instanceId=" + ws.getInstanceId() + "):");
                System.out.println("    provider field: " + ws.getProvider());
                System.out.println("    expected: " + orgInstanceIds.get(i));
                System.out.println("    match: " + orgInstanceIds.get(i).equals(ws.getProvider()));
            }
        }

        // Check DataproductPublisher join table
        System.out.println("\nChecking DataproductPublisher join table:");

        for (int i = 0; i < dpInstanceIds.size(); i++) {
            List<Object> joinResults = EposDataModelDAO.getInstance()
                    .getOneFromDBBySpecificKeyNoCache("dataproductInstance", dpInstanceIds.get(i), DataproductPublisher.class);

            System.out.println("  DP " + i + ": " + joinResults.size() + " publisher records");

            for (Object obj : joinResults) {
                DataproductPublisher dp = (DataproductPublisher) obj;
                String orgInstId = dp.getOrganizationInstance() != null ?
                        dp.getOrganizationInstance().getInstanceId() : "NULL";
                System.out.println("    Organization instanceId: " + orgInstId);
                System.out.println("    expected: " + orgInstanceIds.get(i));
            }
        }
    }

    // =========================================================================
    // TEST 8: Check for duplicate Versioningstatus records
    // =========================================================================
    @Test
    @Order(8)
    public void test08_CheckForDuplicateVersioningstatusRecords() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 8: Check for duplicate Versioningstatus records");
        System.out.println("=".repeat(70));

        for (int i = 0; i < orgUids.size(); i++) {
            List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(orgUids.get(i), Versioningstatus.class);

            System.out.println("Organization " + i + " (UID=" + orgUids.get(i) + "):");
            System.out.println("  Versioningstatus records: " + vsList.size());

            if (vsList.size() > 1) {
                System.out.println("  *** WARNING: Multiple Versioningstatus records! ***");
                for (Versioningstatus vs : vsList) {
                    System.out.println("    - instanceId=" + vs.getInstanceId() +
                            ", status=" + vs.getStatus());
                }
            }
        }

        for (int i = 0; i < wsUids.size(); i++) {
            List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(wsUids.get(i), Versioningstatus.class);

            System.out.println("WebService " + i + " (UID=" + wsUids.get(i) + "):");
            System.out.println("  Versioningstatus records: " + vsList.size());

            if (vsList.size() > 1) {
                System.out.println("  *** WARNING: Multiple Versioningstatus records! ***");
                for (Versioningstatus vs : vsList) {
                    System.out.println("    - instanceId=" + vs.getInstanceId() +
                            ", status=" + vs.getStatus());
                }
            }
        }
    }

    // =========================================================================
    // TEST 9: Simulate real-world scenario - create then update
    // =========================================================================
    @Test
    @Order(9)
    public void test09_CreateThenUpdateMultipleTimes() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 9: Create Organization, then update WebService multiple times");
        System.out.println("=".repeat(70));

        // Create a fresh Organization
        Organization org = new Organization();
        String orgUid = "https://test.org/Organization/UpdateTest_" + UUID.randomUUID();
        org.setUid(orgUid);
        org.setLegalName(List.of("Update Test Organization"));
        org.setInstanceId(UUID.randomUUID().toString());
        org.setMetaId(UUID.randomUUID().toString());

        LinkedEntity orgLe = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(org, StatusType.PUBLISHED, null, null);

        System.out.println("Created Organization: " + orgLe.getInstanceId());

        // Create WebService
        WebService ws = new WebService();
        String wsUid = "https://test.org/WebService/UpdateTest_" + UUID.randomUUID();
        ws.setUid(wsUid);
        ws.setName("Update Test WebService");
        ws.setDescription("Initial description");
        ws.setInstanceId(UUID.randomUUID().toString());
        ws.setMetaId(UUID.randomUUID().toString());
        ws.setProvider(orgLe);

        LinkedEntity wsLe = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(ws, StatusType.PUBLISHED, null, null);

        String wsInstanceId = wsLe.getInstanceId();
        System.out.println("Created WebService: " + wsInstanceId);

        // Update WebService 5 times
        for (int i = 1; i <= 5; i++) {
            WebService toUpdate = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(wsInstanceId);

            toUpdate.setDescription("Updated description " + i);
            // Note: NOT explicitly setting provider again

            LinkedEntity updated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .create(toUpdate, StatusType.PUBLISHED, null, null);

            // Verify after each update
            WebService afterUpdate = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(updated.getInstanceId());

            System.out.println("After update " + i + ":");
            System.out.println("  Provider: " + (afterUpdate.getProvider() != null ?
                    afterUpdate.getProvider().getUid() : "*** NULL! ***"));

            if (afterUpdate.getProvider() == null) {
                fail("Provider became NULL after update " + i);
            }
        }
    }

    // =========================================================================
    // TEST 10: Create with LinkedEntity that has only UID (no instanceId)
    // =========================================================================
    @Test
    @Order(10)
    public void test10_CreateWithLinkedEntityOnlyUid() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 10: Create WebService with LinkedEntity that has only UID");
        System.out.println("=".repeat(70));

        // Create Organization
        Organization org = new Organization();
        String orgUid = "https://test.org/Organization/UidOnlyTest_" + UUID.randomUUID();
        org.setUid(orgUid);
        org.setLegalName(List.of("UID Only Test Organization"));
        org.setInstanceId(UUID.randomUUID().toString());
        org.setMetaId(UUID.randomUUID().toString());

        LinkedEntity orgLe = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(org, StatusType.PUBLISHED, null, null);

        System.out.println("Created Organization:");
        System.out.println("  UID: " + orgUid);
        System.out.println("  InstanceId: " + orgLe.getInstanceId());

        // Create LinkedEntity with ONLY UID (simulating TTL import)
        LinkedEntity uidOnlyLe = new LinkedEntity();
        uidOnlyLe.setUid(orgUid);
        uidOnlyLe.setEntityType("ORGANIZATION");
        // NOT setting instanceId or metaId!

        System.out.println("\nCreating WebService with UID-only LinkedEntity:");
        System.out.println("  LinkedEntity.uid: " + uidOnlyLe.getUid());
        System.out.println("  LinkedEntity.instanceId: " + uidOnlyLe.getInstanceId());

        // Create WebService with UID-only provider
        WebService ws = new WebService();
        ws.setUid("https://test.org/WebService/UidOnlyTest_" + UUID.randomUUID());
        ws.setName("UID Only Test WebService");
        ws.setDescription("WebService with UID-only provider");
        ws.setInstanceId(UUID.randomUUID().toString());
        ws.setMetaId(UUID.randomUUID().toString());
        ws.setProvider(uidOnlyLe);

        LinkedEntity wsLe = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(ws, StatusType.PUBLISHED, null, null);

        System.out.println("\nCreated WebService: " + wsLe.getInstanceId());

        // Verify
        WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsLe.getInstanceId());

        System.out.println("Retrieved WebService provider:");
        System.out.println("  Provider: " + (retrieved.getProvider() != null ?
                retrieved.getProvider().getUid() : "*** NULL! ***"));

        assertNotNull(retrieved.getProvider(),
                "WebService should have provider even when LinkedEntity had only UID");
        assertEquals(orgUid, retrieved.getProvider().getUid());
    }
}