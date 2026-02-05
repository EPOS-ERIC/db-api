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
 * Test per verificare il comportamento quando un'Organization è condivisa
 * tra WebService (come provider) e DataProduct (come publisher).
 *
 * Questo test diagnostica problemi di:
 * - Cache inconsistente
 * - instanceId che cambia tra chiamate
 * - RelationChecker che non trova la versione corretta
 * - Relazioni perse durante transizioni di status
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrganizationSharedRelationsTest extends TestcontainersLifecycle {

    // Shared test data
    private String orgUid;
    private String orgInstanceId;
    private String wsUid;
    private String wsInstanceId;
    private String dpUid;
    private String dpInstanceId;
    private LinkedEntity orgLinkedEntity;

    // =========================================================================
    // TEST 1: Crea Organization, poi WebService che la referenzia come provider
    // =========================================================================
    @Test
    @Order(1)
    public void test01_CreateOrganizationAndWebService() {
        System.out.println("\n========== TEST 1: Create Organization + WebService ==========");

        // 1. Create Organization
        Organization organization = new Organization();
        orgUid = "https://test.org/Organization/SharedOrg_" + UUID.randomUUID();
        organization.setUid(orgUid);
        organization.setLegalName(List.of("Test Shared Organization"));
        organization.setInstanceId(UUID.randomUUID().toString());
        organization.setMetaId(UUID.randomUUID().toString());

        orgLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(organization, StatusType.PUBLISHED, null, null);

        assertNotNull(orgLinkedEntity, "Organization creation should return LinkedEntity");
        orgInstanceId = orgLinkedEntity.getInstanceId();

        System.out.println("Created Organization:");
        System.out.println("  UID: " + orgUid);
        System.out.println("  InstanceId: " + orgInstanceId);
        System.out.println("  LinkedEntity.instanceId: " + orgLinkedEntity.getInstanceId());

        // 2. Create WebService with Organization as provider
        WebService webService = new WebService();
        wsUid = "https://test.org/WebService/TestWS_" + UUID.randomUUID();
        webService.setUid(wsUid);
        webService.setName("Test WebService with Provider");
        webService.setDescription("WebService linked to shared organization");
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setProvider(orgLinkedEntity);

        LinkedEntity wsLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webService, StatusType.PUBLISHED, null, null);

        assertNotNull(wsLinkedEntity, "WebService creation should return LinkedEntity");
        wsInstanceId = wsLinkedEntity.getInstanceId();

        System.out.println("Created WebService:");
        System.out.println("  UID: " + wsUid);
        System.out.println("  InstanceId: " + wsInstanceId);

        // 3. Verify WebService has provider
        WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceId);

        System.out.println("Retrieved WebService:");
        System.out.println("  Provider: " + (retrieved.getProvider() != null ? retrieved.getProvider().getUid() : "NULL!"));

        assertNotNull(retrieved.getProvider(), "WebService.provider should NOT be null");
        assertEquals(orgUid, retrieved.getProvider().getUid(), "Provider UID should match Organization UID");
    }

    // =========================================================================
    // TEST 2: Crea DataProduct che usa la STESSA Organization come publisher
    // =========================================================================
    @Test
    @Order(2)
    public void test02_CreateDataProductWithSameOrganization() {
        System.out.println("\n========== TEST 2: Create DataProduct with same Organization ==========");

        // Verify Organization still exists and get fresh LinkedEntity
        Organization orgCheck = (Organization) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieve(orgInstanceId);
        assertNotNull(orgCheck, "Organization should still exist");
        System.out.println("Organization still exists with instanceId: " + orgCheck.getInstanceId());

        // Get fresh LinkedEntity for the Organization
        LinkedEntity freshOrgLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieveLinkedEntity(orgInstanceId);

        System.out.println("Fresh Organization LinkedEntity:");
        System.out.println("  InstanceId: " + freshOrgLinkedEntity.getInstanceId());
        System.out.println("  UID: " + freshOrgLinkedEntity.getUid());

        // Create DataProduct with Organization as publisher
        DataProduct dataProduct = new DataProduct();
        dpUid = "https://test.org/DataProduct/TestDP_" + UUID.randomUUID();
        dataProduct.setUid(dpUid);
        dataProduct.setTitle(List.of("Test DataProduct with Publisher"));
        dataProduct.setDescription(List.of("DataProduct linked to same shared organization"));
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setPublisher(List.of(freshOrgLinkedEntity));

        LinkedEntity dpLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .create(dataProduct, StatusType.PUBLISHED, null, null);

        assertNotNull(dpLinkedEntity, "DataProduct creation should return LinkedEntity");
        dpInstanceId = dpLinkedEntity.getInstanceId();

        System.out.println("Created DataProduct:");
        System.out.println("  UID: " + dpUid);
        System.out.println("  InstanceId: " + dpInstanceId);

        // Verify DataProduct has publisher
        DataProduct retrievedDp = (DataProduct) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .retrieve(dpInstanceId);

        System.out.println("Retrieved DataProduct:");
        System.out.println("  Publishers: " + (retrievedDp.getPublisher() != null ? retrievedDp.getPublisher().size() : "NULL!"));
        if (retrievedDp.getPublisher() != null && !retrievedDp.getPublisher().isEmpty()) {
            System.out.println("  Publisher[0] UID: " + retrievedDp.getPublisher().get(0).getUid());
        }

        assertNotNull(retrievedDp.getPublisher(), "DataProduct.publisher should NOT be null");
        assertFalse(retrievedDp.getPublisher().isEmpty(), "DataProduct.publisher should NOT be empty");
        assertEquals(orgUid, retrievedDp.getPublisher().get(0).getUid(), "Publisher UID should match Organization UID");
    }

    // =========================================================================
    // TEST 3: Verifica che WebService abbia ancora il provider dopo creazione DataProduct
    // =========================================================================
    @Test
    @Order(3)
    public void test03_VerifyWebServiceProviderStillExists() {
        System.out.println("\n========== TEST 3: Verify WebService provider still exists ==========");

        // This is the critical test - after creating DataProduct with same Organization,
        // does WebService still have its provider?

        WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceId);

        System.out.println("WebService after DataProduct creation:");
        System.out.println("  InstanceId: " + retrieved.getInstanceId());
        System.out.println("  Provider: " + (retrieved.getProvider() != null ? "EXISTS" : "NULL!"));

        if (retrieved.getProvider() != null) {
            System.out.println("  Provider.UID: " + retrieved.getProvider().getUid());
            System.out.println("  Provider.InstanceId: " + retrieved.getProvider().getInstanceId());
        }

        assertNotNull(retrieved.getProvider(),
                "CRITICAL: WebService.provider became NULL after DataProduct creation!");
        assertEquals(orgUid, retrieved.getProvider().getUid(),
                "Provider UID should still match Organization UID");
    }

    // =========================================================================
    // TEST 4: Verifica a livello database - Webservice.provider field
    // =========================================================================
    @Test
    @Order(4)
    public void test04_VerifyDatabaseDirectly() {
        System.out.println("\n========== TEST 4: Direct database verification ==========");

        // Check Webservice table directly
        List<Object> wsResults = EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceIdNoCache(wsInstanceId, Webservice.class);

        assertFalse(wsResults.isEmpty(), "Webservice should exist in DB");

        Webservice wsModel = (Webservice) wsResults.get(0);
        System.out.println("Database Webservice record:");
        System.out.println("  instanceId: " + wsModel.getInstanceId());
        System.out.println("  provider field: " + wsModel.getProvider());

        assertNotNull(wsModel.getProvider(),
                "CRITICAL: Webservice.provider is NULL in database!");

        // Verify the provider instanceId matches Organization
        System.out.println("Expected Organization instanceId: " + orgInstanceId);
        System.out.println("Webservice.provider value: " + wsModel.getProvider());

        // The provider field should contain the Organization's instanceId
        // or be a reference to the Organization
    }

    // =========================================================================
    // TEST 5: Verifica DataproductPublisher join table
    // =========================================================================
    @Test
    @Order(5)
    public void test05_VerifyDataproductPublisherJoinTable() {
        System.out.println("\n========== TEST 5: Verify DataproductPublisher join table ==========");

        List<Object> joinResults = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache("dataproductInstance", dpInstanceId, DataproductPublisher.class);

        System.out.println("DataproductPublisher records for DataProduct: " + joinResults.size());

        assertFalse(joinResults.isEmpty(), "DataproductPublisher join record should exist");

        for (Object obj : joinResults) {
            DataproductPublisher dp = (DataproductPublisher) obj;
            System.out.println("  Join record:");
            System.out.println("    DataProduct instanceId: " +
                    (dp.getDataproductInstance() != null ? dp.getDataproductInstance().getInstanceId() : "NULL"));
            System.out.println("    Organization instanceId: " +
                    (dp.getOrganizationInstance() != null ? dp.getOrganizationInstance().getInstanceId() : "NULL"));
        }
    }

    // =========================================================================
    // TEST 6: Crea un SECONDO WebService con la stessa Organization
    // =========================================================================
    @Test
    @Order(6)
    public void test06_CreateSecondWebServiceWithSameOrganization() {
        System.out.println("\n========== TEST 6: Create SECOND WebService with same Organization ==========");

        // Get fresh LinkedEntity
        LinkedEntity freshOrgLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieveLinkedEntity(orgInstanceId);

        // Create second WebService
        WebService webService2 = new WebService();
        String ws2Uid = "https://test.org/WebService/TestWS2_" + UUID.randomUUID();
        webService2.setUid(ws2Uid);
        webService2.setName("Second WebService with same Provider");
        webService2.setDescription("Another WebService linked to same organization");
        webService2.setInstanceId(UUID.randomUUID().toString());
        webService2.setMetaId(UUID.randomUUID().toString());
        webService2.setProvider(freshOrgLinkedEntity);

        LinkedEntity ws2LinkedEntity = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webService2, StatusType.PUBLISHED, null, null);

        assertNotNull(ws2LinkedEntity, "Second WebService creation should return LinkedEntity");
        String ws2InstanceId = ws2LinkedEntity.getInstanceId();

        System.out.println("Created second WebService:");
        System.out.println("  UID: " + ws2Uid);
        System.out.println("  InstanceId: " + ws2InstanceId);

        // Verify second WebService has provider
        WebService retrieved2 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(ws2InstanceId);

        System.out.println("Second WebService provider: " +
                (retrieved2.getProvider() != null ? retrieved2.getProvider().getUid() : "NULL!"));

        assertNotNull(retrieved2.getProvider(), "Second WebService.provider should NOT be null");
        assertEquals(orgUid, retrieved2.getProvider().getUid(), "Provider UID should match Organization UID");

        // NOW CHECK: Does FIRST WebService still have its provider?
        WebService retrieved1 = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsInstanceId);

        System.out.println("FIRST WebService provider after second creation: " +
                (retrieved1.getProvider() != null ? retrieved1.getProvider().getUid() : "NULL!"));

        assertNotNull(retrieved1.getProvider(),
                "CRITICAL: First WebService lost its provider after second WebService creation!");
    }

    // =========================================================================
    // TEST 7: Status transition - Organization PUBLISHED, create DRAFT WebService
    // =========================================================================
    @Test
    @Order(7)
    public void test07_DraftWebServiceWithPublishedOrganization() {
        System.out.println("\n========== TEST 7: DRAFT WebService with PUBLISHED Organization ==========");

        // Get fresh LinkedEntity for PUBLISHED Organization
        LinkedEntity freshOrgLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieveLinkedEntity(orgInstanceId);

        System.out.println("Organization status: PUBLISHED");
        System.out.println("Organization instanceId: " + freshOrgLinkedEntity.getInstanceId());

        // Create DRAFT WebService referencing PUBLISHED Organization
        WebService webService = new WebService();
        String wsDraftUid = "https://test.org/WebService/DraftWS_" + UUID.randomUUID();
        webService.setUid(wsDraftUid);
        webService.setName("DRAFT WebService with PUBLISHED Provider");
        webService.setDescription("Test status mismatch scenario");
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setProvider(freshOrgLinkedEntity);

        // Create as DRAFT (not PUBLISHED)
        LinkedEntity wsDraftLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webService, StatusType.DRAFT, null, null);

        assertNotNull(wsDraftLinkedEntity, "DRAFT WebService creation should return LinkedEntity");
        String wsDraftInstanceId = wsDraftLinkedEntity.getInstanceId();

        // Verify
        WebService retrievedDraft = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsDraftInstanceId);

        System.out.println("DRAFT WebService:");
        System.out.println("  Status: " + retrievedDraft.getStatus());
        System.out.println("  Provider: " + (retrievedDraft.getProvider() != null ?
                retrievedDraft.getProvider().getUid() : "NULL!"));

        assertNotNull(retrievedDraft.getProvider(),
                "DRAFT WebService should have provider even if Organization is PUBLISHED");
    }

    // =========================================================================
    // TEST 8: Crea tutto in ordine inverso - prima WebService e DataProduct, poi Organization
    // =========================================================================
    @Test
    @Order(8)
    public void test08_CreateInReverseOrder_DeferredRelations() {
        System.out.println("\n========== TEST 8: Reverse order - Deferred relations ==========");

        // 1. Create LinkedEntity for Organization that doesn't exist yet
        String futureOrgUid = "https://test.org/Organization/FutureOrg_" + UUID.randomUUID();
        LinkedEntity futureOrgLinkedEntity = new LinkedEntity();
        futureOrgLinkedEntity.setUid(futureOrgUid);
        futureOrgLinkedEntity.setEntityType("ORGANIZATION");

        System.out.println("Creating WebService with non-existent Organization...");
        System.out.println("  Future Organization UID: " + futureOrgUid);

        // 2. Create WebService referencing non-existent Organization
        WebService webService = new WebService();
        String wsFutureUid = "https://test.org/WebService/FutureWS_" + UUID.randomUUID();
        webService.setUid(wsFutureUid);
        webService.setName("WebService with future Provider");
        webService.setDescription("Test deferred relation");
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setProvider(futureOrgLinkedEntity);

        LinkedEntity wsFutureLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webService, StatusType.PUBLISHED, null, null);

        String wsFutureInstanceId = wsFutureLinkedEntity.getInstanceId();

        // 3. Check WebService - provider should be null (Organization doesn't exist)
        WebService retrievedBefore = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsFutureInstanceId);

        System.out.println("WebService BEFORE Organization creation:");
        System.out.println("  Provider: " + (retrievedBefore.getProvider() != null ?
                retrievedBefore.getProvider().getUid() : "null (expected)"));

        // 4. NOW create the Organization
        System.out.println("\nCreating the Organization...");
        Organization futureOrg = new Organization();
        futureOrg.setUid(futureOrgUid);
        futureOrg.setLegalName(List.of("Future Organization"));
        futureOrg.setInstanceId(UUID.randomUUID().toString());
        futureOrg.setMetaId(UUID.randomUUID().toString());

        LinkedEntity createdOrgLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(futureOrg, StatusType.PUBLISHED, null, null);

        System.out.println("Created Organization:");
        System.out.println("  InstanceId: " + createdOrgLinkedEntity.getInstanceId());

        // 5. Check WebService again - should provider be resolved now?
        WebService retrievedAfter = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(wsFutureInstanceId);

        System.out.println("\nWebService AFTER Organization creation:");
        System.out.println("  Provider: " + (retrievedAfter.getProvider() != null ?
                retrievedAfter.getProvider().getUid() : "null (deferred not resolved)"));

        // Note: This test documents current behavior - deferred relations may or may not auto-resolve
    }

    // =========================================================================
    // TEST 9: Debug - Print all Versioningstatus for Organization
    // =========================================================================
    @Test
    @Order(9)
    public void test09_DebugOrganizationVersioningstatus() {
        System.out.println("\n========== TEST 9: Debug Organization Versioningstatus ==========");

        // Get all Versioningstatus records for the Organization UID
        List<Versioningstatus> vsList = EposDataModelDAO.getInstance()
                .getOneFromDBByUIDNoCache(orgUid, Versioningstatus.class);

        System.out.println("Versioningstatus records for Organization UID '" + orgUid + "': " + vsList.size());

        for (Versioningstatus vs : vsList) {
            System.out.println("  Record:");
            System.out.println("    versionId: " + vs.getVersionId());
            System.out.println("    instanceId: " + vs.getInstanceId());
            System.out.println("    metaId: " + vs.getMetaId());
            System.out.println("    uid: " + vs.getUid());
            System.out.println("    status: " + vs.getStatus());
            System.out.println("    provenance: " + vs.getProvenance());
            System.out.println("    changeComment: " + vs.getChangeComment());
        }

        // Also check Organization table directly
        List<Object> orgList = EposDataModelDAO.getInstance()
                .getOneFromDBByUIDNoCache(orgUid, model.Organization.class);

        System.out.println("\nOrganization records in DB: " + orgList.size());
        for (Object obj : orgList) {
            model.Organization org = (model.Organization) obj;
            System.out.println("  Organization:");
            System.out.println("    instanceId: " + org.getInstanceId());
            if (org.getVersion() != null) {
                System.out.println("    version.status: " + org.getVersion().getStatus());
                System.out.println("    version.instanceId: " + org.getVersion().getInstanceId());
            }
        }
    }

    // =========================================================================
    // TEST 10: Cache vs NoCache comparison - CRITICAL DIAGNOSTIC
    // =========================================================================
    @Test
    @Order(10)
    public void test10_CacheVsNoCacheComparison() {
        System.out.println("\n========== TEST 10: Cache vs NoCache comparison ==========");

        System.out.println("Organization instanceId to find: " + orgInstanceId);

        // Test 1: With Cache
        List<Object> withCache = EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceId(orgInstanceId, model.Organization.class);
        System.out.println("\nWith CACHE: found " + withCache.size() + " results");
        if (!withCache.isEmpty()) {
            model.Organization org = (model.Organization) withCache.get(0);
            System.out.println("  instanceId: " + org.getInstanceId());
            System.out.println("  uid: " + org.getUid());
        }

        // Test 2: Without Cache
        List<Object> noCache = EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceIdNoCache(orgInstanceId, model.Organization.class);
        System.out.println("\nWithout CACHE (NoCache): found " + noCache.size() + " results");
        if (!noCache.isEmpty()) {
            model.Organization org = (model.Organization) noCache.get(0);
            System.out.println("  instanceId: " + org.getInstanceId());
            System.out.println("  uid: " + org.getUid());
        }

        // Test 3: retrieveLinkedEntity (uses cache internally)
        LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .retrieveLinkedEntity(orgInstanceId);
        System.out.println("\nretrieveLinkedEntity result: " + (le != null ? "FOUND" : "NULL!"));
        if (le != null) {
            System.out.println("  instanceId: " + le.getInstanceId());
            System.out.println("  uid: " + le.getUid());
        }

        // Verify consistency
        if (withCache.isEmpty() && !noCache.isEmpty()) {
            System.out.println("\n*** CACHE INCONSISTENCY DETECTED! ***");
            System.out.println("Cache returned empty but direct DB query found the entity.");
            System.out.println("This is likely the cause of the provider being null!");
        }

        if (le == null && !noCache.isEmpty()) {
            System.out.println("\n*** CRITICAL: retrieveLinkedEntity returned null but entity exists! ***");
            System.out.println("The cache is stale or corrupted.");
        }
    }

    // =========================================================================
    // TEST 11: Verify Webservice.provider field directly in DB
    // =========================================================================
    @Test
    @Order(11)
    public void test11_VerifyWebserviceProviderFieldDirectly() {
        System.out.println("\n========== TEST 11: Verify Webservice.provider field directly ==========");

        // Get Webservice with cache
        List<Object> wsCache = EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceId(wsInstanceId, Webservice.class);

        // Get Webservice without cache
        List<Object> wsNoCache = EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceIdNoCache(wsInstanceId, Webservice.class);

        System.out.println("Webservice with CACHE: " + wsCache.size() + " results");
        if (!wsCache.isEmpty()) {
            Webservice ws = (Webservice) wsCache.get(0);
            System.out.println("  provider field: " + ws.getProvider());
        }

        System.out.println("\nWebservice without CACHE: " + wsNoCache.size() + " results");
        if (!wsNoCache.isEmpty()) {
            Webservice ws = (Webservice) wsNoCache.get(0);
            System.out.println("  provider field: " + ws.getProvider());

            // Now try to resolve the provider
            if (ws.getProvider() != null) {
                System.out.println("\nTrying to resolve provider instanceId: " + ws.getProvider());

                List<Object> providerNoCache = EposDataModelDAO.getInstance()
                        .getOneFromDBByInstanceIdNoCache(ws.getProvider(), model.Organization.class);
                System.out.println("Provider lookup (NoCache): " + providerNoCache.size() + " results");

                if (providerNoCache.isEmpty()) {
                    System.out.println("*** CRITICAL: Provider instanceId exists but Organization not found! ***");
                    System.out.println("The Organization may have been deleted or its instanceId changed.");
                }
            }
        }
    }

    // =========================================================================
    // TEST 12: Rapid sequential creation - Potential race condition
    // =========================================================================
    @Test
    @Order(12)
    public void test12_RapidSequentialCreation() {
        System.out.println("\n========== TEST 12: Rapid sequential creation ==========");

        // Create a fresh Organization
        Organization org = new Organization();
        String rapidOrgUid = "https://test.org/Organization/RapidOrg_" + UUID.randomUUID();
        org.setUid(rapidOrgUid);
        org.setLegalName(List.of("Rapid Test Organization"));
        org.setInstanceId(UUID.randomUUID().toString());
        org.setMetaId(UUID.randomUUID().toString());

        LinkedEntity orgLe = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(org, StatusType.PUBLISHED, null, null);

        System.out.println("Created Organization: " + rapidOrgUid);

        // Rapidly create multiple WebServices with same Organization
        int successCount = 0;
        int failCount = 0;
        String[] wsInstanceIds = new String[5];

        for (int i = 0; i < 5; i++) {
            WebService ws = new WebService();
            ws.setUid("https://test.org/WebService/RapidWS_" + i + "_" + UUID.randomUUID());
            ws.setName("Rapid WebService " + i);
            ws.setDescription("Rapid test " + i);
            ws.setInstanceId(UUID.randomUUID().toString());
            ws.setMetaId(UUID.randomUUID().toString());
            ws.setProvider(orgLe);

            LinkedEntity wsLe = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .create(ws, StatusType.PUBLISHED, null, null);

            wsInstanceIds[i] = wsLe.getInstanceId();
        }

        // Now verify all WebServices have their provider
        System.out.println("\nVerifying all WebServices:");
        for (int i = 0; i < 5; i++) {
            WebService retrieved = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                    .retrieve(wsInstanceIds[i]);

            boolean hasProvider = retrieved.getProvider() != null;
            System.out.println("  WebService " + i + ": provider = " +
                    (hasProvider ? "OK (" + retrieved.getProvider().getUid() + ")" : "NULL!"));

            if (hasProvider) successCount++;
            else failCount++;
        }

        System.out.println("\nResults: " + successCount + " OK, " + failCount + " FAILED");

        assertEquals(5, successCount, "All 5 WebServices should have their provider");
    }
}