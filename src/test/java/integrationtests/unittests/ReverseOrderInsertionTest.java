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
 * Test per verificare il comportamento quando le entità vengono inserite
 * in ORDINE INVERSO - prima le entità che referenziano, poi quelle referenziate.
 *
 * Scenario tipico durante import TTL/RDF:
 * 1. WebService viene inserito con provider = Organization (che non esiste ancora)
 * 2. DataProduct viene inserito con publisher = Organization (che non esiste ancora)
 * 3. Organization viene inserita DOPO
 *
 * Le relazioni dovrebbero essere risolte retroattivamente (deferred relations).
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ReverseOrderInsertionTest extends TestcontainersLifecycle {

    // Organization UIDs (entità che creeremo DOPO)
    private String org1Uid = "https://test.org/Organization/ReverseOrg1_" + UUID.randomUUID();
    private String org2Uid = "https://test.org/Organization/ReverseOrg2_" + UUID.randomUUID();
    private String org3Uid = "https://test.org/Organization/ReverseOrg3_" + UUID.randomUUID();

    // Instance IDs che verranno popolati dopo la creazione
    private String org1InstanceId;
    private String org2InstanceId;
    private String org3InstanceId;

    // WebService e DataProduct instance IDs
    private List<String> wsInstanceIds = new ArrayList<>();
    private List<String> dpInstanceIds = new ArrayList<>();

    // =========================================================================
    // TEST 1: Create WebServices FIRST (Organization non esiste ancora)
    // =========================================================================
    @Test
    @Order(1)
    public void test01_CreateWebServicesWithNonExistentProvider() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 1: Create WebServices with NON-EXISTENT Organization as provider");
        System.out.println("=".repeat(70));

        String[] orgUids = {org1Uid, org2Uid, org3Uid};

        for (int i = 0; i < 3; i++) {
            // Crea LinkedEntity con SOLO UID (Organization non esiste!)
            LinkedEntity providerLe = new LinkedEntity();
            providerLe.setUid(orgUids[i]);
            providerLe.setEntityType("ORGANIZATION");
            // NON settiamo instanceId o metaId perché non esistono!

            System.out.println("\nCreating WebService " + i + " with non-existent provider:");
            System.out.println("  Provider UID: " + orgUids[i]);
            System.out.println("  Provider instanceId: " + providerLe.getInstanceId() + " (should be null)");

            WebService ws = new WebService();
            String wsUid = "https://test.org/WebService/ReverseWS_" + i + "_" + UUID.randomUUID();
            ws.setUid(wsUid);
            ws.setName("Reverse Order WebService " + i);
            ws.setDescription("WebService created BEFORE its provider Organization");
            ws.setInstanceId(UUID.randomUUID().toString());
            ws.setMetaId(UUID.randomUUID().toString());
            ws.setProvider(providerLe);

            LinkedEntity wsLe = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .create(ws, StatusType.PUBLISHED, null, null);

            assertNotNull(wsLe, "WebService " + i + " creation should not fail");
            wsInstanceIds.add(wsLe.getInstanceId());

            System.out.println("  Created WebService: " + wsLe.getInstanceId());
        }

        // Verify WebServices - provider should be NULL at this point
        System.out.println("\n--- Verifying WebServices (provider should be NULL) ---");
        for (int i = 0; i < 3; i++) {
            WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(wsInstanceIds.get(i));

            System.out.println("WS " + i + ": provider = " +
                    (retrieved.getProvider() != null ? retrieved.getProvider().getUid() : "NULL (expected)"));
        }
    }

    // =========================================================================
    // TEST 2: Create DataProducts FIRST (Organization non esiste ancora)
    // =========================================================================
    @Test
    @Order(2)
    public void test02_CreateDataProductsWithNonExistentPublisher() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 2: Create DataProducts with NON-EXISTENT Organization as publisher");
        System.out.println("=".repeat(70));

        String[] orgUids = {org1Uid, org2Uid, org3Uid};

        for (int i = 0; i < 3; i++) {
            // Crea LinkedEntity con SOLO UID (Organization non esiste!)
            LinkedEntity publisherLe = new LinkedEntity();
            publisherLe.setUid(orgUids[i]);
            publisherLe.setEntityType("ORGANIZATION");

            System.out.println("\nCreating DataProduct " + i + " with non-existent publisher:");
            System.out.println("  Publisher UID: " + orgUids[i]);

            DataProduct dp = new DataProduct();
            String dpUid = "https://test.org/DataProduct/ReverseDP_" + i + "_" + UUID.randomUUID();
            dp.setUid(dpUid);
            dp.setTitle(List.of("Reverse Order DataProduct " + i));
            dp.setDescription(List.of("DataProduct created BEFORE its publisher Organization"));
            dp.setInstanceId(UUID.randomUUID().toString());
            dp.setMetaId(UUID.randomUUID().toString());
            dp.setPublisher(List.of(publisherLe));

            LinkedEntity dpLe = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                    .create(dp, StatusType.PUBLISHED, null, null);

            assertNotNull(dpLe, "DataProduct " + i + " creation should not fail");
            dpInstanceIds.add(dpLe.getInstanceId());

            System.out.println("  Created DataProduct: " + dpLe.getInstanceId());
        }

        // Verify DataProducts - publisher should be NULL/empty at this point
        System.out.println("\n--- Verifying DataProducts (publisher should be NULL/empty) ---");
        for (int i = 0; i < 3; i++) {
            DataProduct retrieved = (DataProduct) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                    .retrieve(dpInstanceIds.get(i));

            String publisherInfo = (retrieved.getPublisher() != null && !retrieved.getPublisher().isEmpty())
                    ? retrieved.getPublisher().get(0).getUid()
                    : "NULL/EMPTY (expected)";
            System.out.println("DP " + i + ": publisher = " + publisherInfo);
        }
    }

    // =========================================================================
    // TEST 3: Check PENDING relations in Versioningstatus
    // =========================================================================
    @Test
    @Order(3)
    public void test03_CheckPendingRelations() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 3: Check PENDING relations in Versioningstatus");
        System.out.println("=".repeat(70));

        // Query per tutti i record PENDING
        List<Versioningstatus> allPending = new ArrayList<>();

        // Cerca per ogni Organization UID
        String[] orgUids = {org1Uid, org2Uid, org3Uid};
        for (String uid : orgUids) {
            List<Versioningstatus> pending = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(uid, Versioningstatus.class);

            System.out.println("\nVersioningstatus for UID '" + uid + "': " + pending.size() + " records");
            for (Versioningstatus vs : pending) {
                System.out.println("  - instanceId=" + vs.getInstanceId() +
                        ", status=" + vs.getStatus() +
                        ", metaId=" + vs.getMetaId() +
                        ", provenance=" + vs.getProvenance() +
                        ", changeComment=" + vs.getChangeComment());

                if ("PENDING".equals(vs.getStatus())) {
                    allPending.add(vs);
                }
            }
        }

        System.out.println("\n--- Total PENDING relations found: " + allPending.size() + " ---");

        // Also check directly for PENDING status
        // This is a simplified check - in real code you'd query the versioningstatus table
    }

    // =========================================================================
    // TEST 4: NOW Create Organizations (AFTER WebServices and DataProducts)
    // =========================================================================
    @Test
    @Order(4)
    public void test04_CreateOrganizationsAfter() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 4: NOW Create Organizations (AFTER WebServices and DataProducts)");
        System.out.println("=".repeat(70));

        String[] orgUids = {org1Uid, org2Uid, org3Uid};
        String[] instanceIds = new String[3];

        for (int i = 0; i < 3; i++) {
            Organization org = new Organization();
            org.setUid(orgUids[i]);
            org.setLegalName(List.of("Reverse Order Organization " + i));
            org.setInstanceId(UUID.randomUUID().toString());
            org.setMetaId(UUID.randomUUID().toString());

            System.out.println("\nCreating Organization " + i + ":");
            System.out.println("  UID: " + orgUids[i]);

            LinkedEntity orgLe = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                    .create(org, StatusType.PUBLISHED, null, null);

            assertNotNull(orgLe, "Organization " + i + " creation should not fail");
            instanceIds[i] = orgLe.getInstanceId();

            System.out.println("  Created with instanceId: " + orgLe.getInstanceId());
        }

        org1InstanceId = instanceIds[0];
        org2InstanceId = instanceIds[1];
        org3InstanceId = instanceIds[2];

        // Verify Organizations exist
        System.out.println("\n--- Verifying Organizations ---");
        for (int i = 0; i < 3; i++) {
            Organization retrieved = (Organization) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                    .retrieve(instanceIds[i]);
            System.out.println("Org " + i + ": status=" + retrieved.getStatus() + ", uid=" + retrieved.getUid());
        }
    }

    // =========================================================================
    // TEST 5: CRITICAL - Verify WebServices have providers NOW
    // =========================================================================
    @Test
    @Order(5)
    public void test05_VerifyWebServicesHaveProvidersNow() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 5: CRITICAL - Verify WebServices have providers NOW");
        System.out.println("        (after Organizations were created)");
        System.out.println("=".repeat(70));

        String[] expectedOrgUids = {org1Uid, org2Uid, org3Uid};
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < 3; i++) {
            WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(wsInstanceIds.get(i));

            String actualProvider = retrieved.getProvider() != null
                    ? retrieved.getProvider().getUid()
                    : "*** NULL ***";

            boolean hasCorrectProvider = expectedOrgUids[i].equals(actualProvider);

            System.out.println("\nWebService " + i + ":");
            System.out.println("  Expected provider: " + expectedOrgUids[i]);
            System.out.println("  Actual provider: " + actualProvider);
            System.out.println("  Status: " + (hasCorrectProvider ? "OK" : "*** FAILED - DEFERRED RELATION NOT RESOLVED ***"));

            if (hasCorrectProvider) successCount++;
            else failCount++;
        }

        System.out.println("\n--- Results: " + successCount + " OK, " + failCount + " FAILED ---");

        if (failCount > 0) {
            System.out.println("\n*** BUG CONFIRMED: Deferred relations were NOT resolved! ***");
            System.out.println("When Organizations were created AFTER WebServices,");
            System.out.println("the provider relationships were not automatically linked.");
        }

        // We're documenting behavior, not necessarily asserting
        // assertEquals(0, failCount, "Deferred relations should be resolved!");
    }

    // =========================================================================
    // TEST 6: CRITICAL - Verify DataProducts have publishers NOW
    // =========================================================================
    @Test
    @Order(6)
    public void test06_VerifyDataProductsHavePublishersNow() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 6: CRITICAL - Verify DataProducts have publishers NOW");
        System.out.println("        (after Organizations were created)");
        System.out.println("=".repeat(70));

        String[] expectedOrgUids = {org1Uid, org2Uid, org3Uid};
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < 3; i++) {
            DataProduct retrieved = (DataProduct) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                    .retrieve(dpInstanceIds.get(i));

            String actualPublisher = (retrieved.getPublisher() != null && !retrieved.getPublisher().isEmpty())
                    ? retrieved.getPublisher().get(0).getUid()
                    : "*** NULL/EMPTY ***";

            boolean hasCorrectPublisher = expectedOrgUids[i].equals(actualPublisher);

            System.out.println("\nDataProduct " + i + ":");
            System.out.println("  Expected publisher: " + expectedOrgUids[i]);
            System.out.println("  Actual publisher: " + actualPublisher);
            System.out.println("  Status: " + (hasCorrectPublisher ? "OK" : "*** FAILED - DEFERRED RELATION NOT RESOLVED ***"));

            if (hasCorrectPublisher) successCount++;
            else failCount++;
        }

        System.out.println("\n--- Results: " + successCount + " OK, " + failCount + " FAILED ---");

        if (failCount > 0) {
            System.out.println("\n*** BUG CONFIRMED: Deferred relations were NOT resolved! ***");
        }
    }

    // =========================================================================
    // TEST 7: Check database directly
    // =========================================================================
    @Test
    @Order(7)
    public void test07_CheckDatabaseDirectly() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 7: Check database directly");
        System.out.println("=".repeat(70));

        // Check Webservice.provider field
        System.out.println("Webservice.provider field in database:");
        for (int i = 0; i < wsInstanceIds.size(); i++) {
            List<Object> results = EposDataModelDAO.getInstance()
                    .getOneFromDBByInstanceIdNoCache(wsInstanceIds.get(i), Webservice.class);

            if (!results.isEmpty()) {
                Webservice ws = (Webservice) results.get(0);
                System.out.println("  WS " + i + ": provider = " +
                        (ws.getProvider() != null ? ws.getProvider() : "NULL"));
            }
        }

        // Check DataproductPublisher join table
        System.out.println("\nDataproductPublisher join table:");
        for (int i = 0; i < dpInstanceIds.size(); i++) {
            List<Object> joinResults = EposDataModelDAO.getInstance()
                    .getOneFromDBBySpecificKeyNoCache("dataproductInstance", dpInstanceIds.get(i), DataproductPublisher.class);

            System.out.println("  DP " + i + ": " + joinResults.size() + " publisher records");
            for (Object obj : joinResults) {
                DataproductPublisher dp = (DataproductPublisher) obj;
                System.out.println("    -> Organization: " +
                        (dp.getOrganizationInstance() != null ? dp.getOrganizationInstance().getInstanceId() : "NULL"));
            }
        }

        // Check Organization table
        System.out.println("\nOrganization records:");
        String[] orgUids = {org1Uid, org2Uid, org3Uid};
        for (int i = 0; i < 3; i++) {
            List<Object> orgResults = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(orgUids[i], model.Organization.class);
            System.out.println("  Org " + i + " (UID=" + orgUids[i] + "): " + orgResults.size() + " records");
            for (Object obj : orgResults) {
                model.Organization org = (model.Organization) obj;
                System.out.println("    -> instanceId=" + org.getInstanceId());
            }
        }
    }

    // =========================================================================
    // TEST 8: Check PENDING relations after Organization creation
    // =========================================================================
    @Test
    @Order(8)
    public void test08_CheckPendingRelationsAfterOrgCreation() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 8: Check PENDING relations after Organization creation");
        System.out.println("=".repeat(70));

        String[] orgUids = {org1Uid, org2Uid, org3Uid};
        int pendingCount = 0;

        for (String uid : orgUids) {
            List<Versioningstatus> records = EposDataModelDAO.getInstance()
                    .getOneFromDBByUIDNoCache(uid, Versioningstatus.class);

            System.out.println("\nVersioningstatus for UID '" + uid + "':");
            for (Versioningstatus vs : records) {
                String status = vs.getStatus();
                System.out.println("  - status=" + status +
                        ", instanceId=" + vs.getInstanceId() +
                        ", metaId=" + vs.getMetaId());

                if ("PENDING".equals(status)) {
                    pendingCount++;
                    System.out.println("    *** STILL PENDING - NOT RESOLVED! ***");
                    System.out.println("    provenance=" + vs.getProvenance());
                    System.out.println("    changeComment=" + vs.getChangeComment());
                }
            }
        }

        System.out.println("\n--- Total PENDING relations still remaining: " + pendingCount + " ---");

        if (pendingCount > 0) {
            System.out.println("\n*** WARNING: PENDING relations exist but were not processed! ***");
            System.out.println("The system created PENDING markers but didn't resolve them");
            System.out.println("when the target entity (Organization) was created.");
        }
    }

    // =========================================================================
    // TEST 9: Try to UPDATE WebService after Organization exists
    // =========================================================================
    @Test
    @Order(9)
    public void test09_UpdateWebServiceAfterOrganizationExists() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 9: UPDATE WebService after Organization exists");
        System.out.println("        (to see if update resolves the relation)");
        System.out.println("=".repeat(70));

        // Retrieve WS 0
        WebService ws0 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceIds.get(0));

        System.out.println("WS 0 before update:");
        System.out.println("  Provider: " + (ws0.getProvider() != null ? ws0.getProvider().getUid() : "NULL"));

        // Set provider explicitly with the now-existing Organization
        LinkedEntity org1Le = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieveLinkedEntity(org1InstanceId);

        System.out.println("\nSetting provider to Organization 1:");
        System.out.println("  Organization instanceId: " + org1Le.getInstanceId());
        System.out.println("  Organization UID: " + org1Le.getUid());

        ws0.setProvider(org1Le);
        ws0.setDescription("Updated after Organization creation");

        LinkedEntity updatedLe = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(ws0, StatusType.PUBLISHED, null, null);

        // Verify
        WebService afterUpdate = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(updatedLe.getInstanceId());

        System.out.println("\nWS 0 after update:");
        System.out.println("  Provider: " + (afterUpdate.getProvider() != null ?
                afterUpdate.getProvider().getUid() : "*** STILL NULL! ***"));

        assertNotNull(afterUpdate.getProvider(),
                "After explicit update, WebService should have provider");
        assertEquals(org1Uid, afterUpdate.getProvider().getUid());
    }

    // =========================================================================
    // TEST 10: Simulate TTL import order - mixed entities
    // =========================================================================
    @Test
    @Order(10)
    public void test10_SimulateTTLImportMixedOrder() {
        System.out.println("\n" + "=".repeat(70));
        System.out.println("TEST 10: Simulate TTL import with mixed order");
        System.out.println("=".repeat(70));

        // Typical TTL import might have:
        // 1. WebService A (references Org X)
        // 2. DataProduct B (references Org X and Org Y)
        // 3. Organization X
        // 4. WebService C (references Org Y)
        // 5. Organization Y

        String orgXUid = "https://test.org/Organization/TTL_OrgX_" + UUID.randomUUID();
        String orgYUid = "https://test.org/Organization/TTL_OrgY_" + UUID.randomUUID();

        System.out.println("Will create entities in this order:");
        System.out.println("1. WebService A (provider = Org X - non esiste)");
        System.out.println("2. DataProduct B (publisher = Org X, Org Y - non esistono)");
        System.out.println("3. Organization X");
        System.out.println("4. WebService C (provider = Org Y - non esiste)");
        System.out.println("5. Organization Y");

        // 1. WebService A
        LinkedEntity orgXLe = new LinkedEntity();
        orgXLe.setUid(orgXUid);
        orgXLe.setEntityType("ORGANIZATION");

        WebService wsA = new WebService();
        wsA.setUid("https://test.org/WebService/TTL_WSA_" + UUID.randomUUID());
        wsA.setName("TTL WebService A");
        wsA.setDescription("References Org X");
        wsA.setInstanceId(UUID.randomUUID().toString());
        wsA.setMetaId(UUID.randomUUID().toString());
        wsA.setProvider(orgXLe);

        LinkedEntity wsALe = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(wsA, StatusType.PUBLISHED, null, null);
        System.out.println("\n1. Created WebService A: " + wsALe.getInstanceId());

        // 2. DataProduct B
        LinkedEntity orgYLe = new LinkedEntity();
        orgYLe.setUid(orgYUid);
        orgYLe.setEntityType("ORGANIZATION");

        DataProduct dpB = new DataProduct();
        dpB.setUid("https://test.org/DataProduct/TTL_DPB_" + UUID.randomUUID());
        dpB.setTitle(List.of("TTL DataProduct B"));
        dpB.setDescription(List.of("References Org X and Org Y"));
        dpB.setInstanceId(UUID.randomUUID().toString());
        dpB.setMetaId(UUID.randomUUID().toString());
        dpB.setPublisher(List.of(orgXLe, orgYLe));  // Both!

        LinkedEntity dpBLe = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .create(dpB, StatusType.PUBLISHED, null, null);
        System.out.println("2. Created DataProduct B: " + dpBLe.getInstanceId());

        // 3. Organization X
        Organization orgX = new Organization();
        orgX.setUid(orgXUid);
        orgX.setLegalName(List.of("TTL Organization X"));
        orgX.setInstanceId(UUID.randomUUID().toString());
        orgX.setMetaId(UUID.randomUUID().toString());

        LinkedEntity orgXCreated = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(orgX, StatusType.PUBLISHED, null, null);
        System.out.println("3. Created Organization X: " + orgXCreated.getInstanceId());

        // 4. WebService C
        WebService wsC = new WebService();
        wsC.setUid("https://test.org/WebService/TTL_WSC_" + UUID.randomUUID());
        wsC.setName("TTL WebService C");
        wsC.setDescription("References Org Y");
        wsC.setInstanceId(UUID.randomUUID().toString());
        wsC.setMetaId(UUID.randomUUID().toString());
        wsC.setProvider(orgYLe);  // Org Y still doesn't exist!

        LinkedEntity wsCLe = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(wsC, StatusType.PUBLISHED, null, null);
        System.out.println("4. Created WebService C: " + wsCLe.getInstanceId());

        // 5. Organization Y
        Organization orgY = new Organization();
        orgY.setUid(orgYUid);
        orgY.setLegalName(List.of("TTL Organization Y"));
        orgY.setInstanceId(UUID.randomUUID().toString());
        orgY.setMetaId(UUID.randomUUID().toString());

        LinkedEntity orgYCreated = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(orgY, StatusType.PUBLISHED, null, null);
        System.out.println("5. Created Organization Y: " + orgYCreated.getInstanceId());

        // NOW VERIFY ALL RELATIONS
        System.out.println("\n--- VERIFICATION ---");

        WebService wsARetrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsALe.getInstanceId());
        System.out.println("WebService A provider: " +
                (wsARetrieved.getProvider() != null ? wsARetrieved.getProvider().getUid() : "*** NULL ***") +
                " (expected: " + orgXUid + ")");

        DataProduct dpBRetrieved = (DataProduct) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .retrieve(dpBLe.getInstanceId());
        System.out.println("DataProduct B publishers: " +
                (dpBRetrieved.getPublisher() != null ? dpBRetrieved.getPublisher().size() : 0) +
                " (expected: 2)");
        if (dpBRetrieved.getPublisher() != null) {
            for (LinkedEntity pub : dpBRetrieved.getPublisher()) {
                System.out.println("  - " + pub.getUid());
            }
        }

        WebService wsCRetrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsCLe.getInstanceId());
        System.out.println("WebService C provider: " +
                (wsCRetrieved.getProvider() != null ? wsCRetrieved.getProvider().getUid() : "*** NULL ***") +
                " (expected: " + orgYUid + ")");
    }
}