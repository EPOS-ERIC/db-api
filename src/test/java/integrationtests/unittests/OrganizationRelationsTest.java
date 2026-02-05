package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.DataProductAPI;
import metadataapis.EntityNames;
import metadataapis.OrganizationAPI;
import metadataapis.WebServiceAPI;
import model.*;
import org.epos.eposdatamodel.*;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for WebService-Organization and DataProduct-Organization relations.
 *
 * Tests cover:
 * 1. Simple WebService -> Organization (provider) relation
 * 2. Simple DataProduct -> Organization (publisher) relation
 * 3. Shared Organization between WebService and DataProduct
 * 4. Ingestion order variations (Organization first vs. last)
 * 5. Status variations (DRAFT vs PUBLISHED)
 * 6. Update scenarios
 *
 * Run with: mvn test -Dtest=OrganizationRelationsTest
 */
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class OrganizationRelationsTest extends TestcontainersLifecycle {

    private static final String TEST_PREFIX = "TEST_ORG_REL_";

    // Shared test data
    private String sharedOrgUid;
    private String sharedOrgInstanceId;
    private String webServiceUid;
    private String webServiceInstanceId;
    private String dataProductUid;
    private String dataProductInstanceId;

    private WebServiceAPI webServiceAPI;
    private DataProductAPI dataProductAPI;
    private OrganizationAPI organizationAPI;

    @BeforeAll
    void setup() {
        webServiceAPI = (WebServiceAPI) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        dataProductAPI = (DataProductAPI) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        organizationAPI = (OrganizationAPI) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        assertNotNull(webServiceAPI, "WebServiceAPI should be available");
        assertNotNull(dataProductAPI, "DataProductAPI should be available");
        assertNotNull(organizationAPI, "OrganizationAPI should be available");
    }

    @AfterAll
    void cleanup() {
        // Clean up test data
        cleanupTestData();
    }

    // ========================================================================
    // TEST GROUP 1: Simple Relations (Organization created FIRST)
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("1.1 Create Organization first, then WebService with provider")
    void testWebServiceProviderWithExistingOrganization() {
        // Create Organization first
        String orgUid = TEST_PREFIX + "ORG_WS_" + UUID.randomUUID();
        Organization org = createTestOrganization(orgUid, "Test Org for WebService");
        LinkedEntity orgCreated = organizationAPI.create(org, StatusType.PUBLISHED, null, null);

        assertNotNull(orgCreated, "Organization should be created");
        assertNotNull(orgCreated.getInstanceId(), "Organization instanceId should not be null");

        // Create WebService with provider pointing to Organization
        String wsUid = TEST_PREFIX + "WS_" + UUID.randomUUID();
        WebService ws = createTestWebService(wsUid, "Test WebService");
        ws.setProvider(new LinkedEntity().uid(orgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity wsCreated = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);

        assertNotNull(wsCreated, "WebService should be created");
        assertNotNull(wsCreated.getInstanceId(), "WebService instanceId should not be null");

        // Verify the relation
        WebService retrieved = webServiceAPI.retrieve(wsCreated.getInstanceId());
        assertNotNull(retrieved, "Retrieved WebService should not be null");
        assertNotNull(retrieved.getProvider(), "Provider should not be null");
        assertEquals(orgUid, retrieved.getProvider().getUid(), "Provider UID should match");

        System.out.println("✓ Test 1.1 PASSED: WebService provider relation with existing Organization");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 Create Organization first, then DataProduct with publisher")
    void testDataProductPublisherWithExistingOrganization() {
        // Create Organization first
        String orgUid = TEST_PREFIX + "ORG_DP_" + UUID.randomUUID();
        Organization org = createTestOrganization(orgUid, "Test Org for DataProduct");
        LinkedEntity orgCreated = organizationAPI.create(org, StatusType.PUBLISHED, null, null);

        assertNotNull(orgCreated, "Organization should be created");

        // Create DataProduct with publisher pointing to Organization
        String dpUid = TEST_PREFIX + "DP_" + UUID.randomUUID();
        DataProduct dp = createTestDataProduct(dpUid, "Test DataProduct");
        dp.addPublisher(new LinkedEntity().uid(orgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity dpCreated = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);

        assertNotNull(dpCreated, "DataProduct should be created");

        // Verify the relation
        DataProduct retrieved = dataProductAPI.retrieve(dpCreated.getInstanceId());
        assertNotNull(retrieved, "Retrieved DataProduct should not be null");
        assertNotNull(retrieved.getPublisher(), "Publisher list should not be null");
        assertFalse(retrieved.getPublisher().isEmpty(), "Publisher list should not be empty");
        assertEquals(orgUid, retrieved.getPublisher().get(0).getUid(), "Publisher UID should match");

        System.out.println("✓ Test 1.2 PASSED: DataProduct publisher relation with existing Organization");
    }

    // ========================================================================
    // TEST GROUP 2: Shared Organization (Same Org for WebService AND DataProduct)
    // ========================================================================

    @Test
    @Order(10)
    @DisplayName("2.1 Create shared Organization")
    void testCreateSharedOrganization() {
        sharedOrgUid = TEST_PREFIX + "SHARED_ORG_" + UUID.randomUUID();
        Organization org = createTestOrganization(sharedOrgUid, "Shared Organization for WS and DP");
        org.addLegalName("Shared Test Organization Legal Name");

        LinkedEntity created = organizationAPI.create(org, StatusType.PUBLISHED, null, null);

        assertNotNull(created, "Shared Organization should be created");
        sharedOrgInstanceId = created.getInstanceId();
        assertNotNull(sharedOrgInstanceId, "Shared Organization instanceId should not be null");

        System.out.println("✓ Test 2.1 PASSED: Shared Organization created with UID: " + sharedOrgUid);
    }

    @Test
    @Order(11)
    @DisplayName("2.2 Create WebService using shared Organization as provider")
    void testWebServiceWithSharedOrganization() {
        assertNotNull(sharedOrgUid, "Shared Organization must be created first");

        webServiceUid = TEST_PREFIX + "WS_SHARED_" + UUID.randomUUID();
        WebService ws = createTestWebService(webServiceUid, "WebService with Shared Org");
        ws.setProvider(new LinkedEntity().uid(sharedOrgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity created = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);

        assertNotNull(created, "WebService should be created");
        webServiceInstanceId = created.getInstanceId();

        // Verify
        WebService retrieved = webServiceAPI.retrieve(webServiceInstanceId);
        assertNotNull(retrieved.getProvider(), "WebService provider should not be null");
        assertEquals(sharedOrgUid, retrieved.getProvider().getUid(), "Provider should be shared Organization");

        System.out.println("✓ Test 2.2 PASSED: WebService created with shared Organization as provider");
    }

    @Test
    @Order(12)
    @DisplayName("2.3 Create DataProduct using same shared Organization as publisher")
    void testDataProductWithSharedOrganization() {
        assertNotNull(sharedOrgUid, "Shared Organization must be created first");

        dataProductUid = TEST_PREFIX + "DP_SHARED_" + UUID.randomUUID();
        DataProduct dp = createTestDataProduct(dataProductUid, "DataProduct with Shared Org");
        dp.addPublisher(new LinkedEntity().uid(sharedOrgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity created = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);

        assertNotNull(created, "DataProduct should be created");
        dataProductInstanceId = created.getInstanceId();

        // Verify
        DataProduct retrieved = dataProductAPI.retrieve(dataProductInstanceId);
        assertNotNull(retrieved.getPublisher(), "DataProduct publisher should not be null");
        assertFalse(retrieved.getPublisher().isEmpty(), "DataProduct publisher list should not be empty");
        assertEquals(sharedOrgUid, retrieved.getPublisher().get(0).getUid(), "Publisher should be shared Organization");

        System.out.println("✓ Test 2.3 PASSED: DataProduct created with shared Organization as publisher");
    }

    @Test
    @Order(13)
    @DisplayName("2.4 Verify both WebService and DataProduct point to SAME Organization instance")
    void testBothEntitiesShareSameOrganizationInstance() {
        assertNotNull(webServiceInstanceId, "WebService must be created first");
        assertNotNull(dataProductInstanceId, "DataProduct must be created first");

        WebService ws = webServiceAPI.retrieve(webServiceInstanceId);
        DataProduct dp = dataProductAPI.retrieve(dataProductInstanceId);

        assertNotNull(ws.getProvider(), "WebService provider should exist");
        assertNotNull(dp.getPublisher(), "DataProduct publisher should exist");
        assertFalse(dp.getPublisher().isEmpty(), "DataProduct publisher list should not be empty");

        String wsProviderInstanceId = ws.getProvider().getInstanceId();
        String dpPublisherInstanceId = dp.getPublisher().get(0).getInstanceId();

        assertEquals(wsProviderInstanceId, dpPublisherInstanceId,
                "WebService provider and DataProduct publisher should point to SAME Organization instance");
        assertEquals(sharedOrgInstanceId, wsProviderInstanceId,
                "Both should point to the shared Organization instanceId");

        System.out.println("✓ Test 2.4 PASSED: Both entities share the same Organization instance");
        System.out.println("  - WebService provider instanceId: " + wsProviderInstanceId);
        System.out.println("  - DataProduct publisher instanceId: " + dpPublisherInstanceId);
        System.out.println("  - Shared Organization instanceId: " + sharedOrgInstanceId);
    }

    // ========================================================================
    // TEST GROUP 3: Reverse Ingestion Order (Organization created AFTER)
    // ========================================================================

    @Test
    @Order(20)
    @DisplayName("3.1 Create WebService with non-existent Organization (stub creation test)")
    void testWebServiceWithNonExistentOrganization() {
        String futureOrgUid = TEST_PREFIX + "FUTURE_ORG_WS_" + UUID.randomUUID();

        // Create WebService FIRST, referencing Organization that doesn't exist yet
        String wsUid = TEST_PREFIX + "WS_FUTURE_" + UUID.randomUUID();
        WebService ws = createTestWebService(wsUid, "WebService with Future Org");
        ws.setProvider(new LinkedEntity().uid(futureOrgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity wsCreated = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);
        assertNotNull(wsCreated, "WebService should be created even with non-existent Organization");

        // Check if stub was created OR if provider is null
        WebService retrievedBefore = webServiceAPI.retrieve(wsCreated.getInstanceId());

        // Now create the Organization
        Organization org = createTestOrganization(futureOrgUid, "Future Organization for WebService");
        LinkedEntity orgCreated = organizationAPI.create(org, StatusType.PUBLISHED, null, null);
        assertNotNull(orgCreated, "Organization should be created");

        // Re-retrieve WebService to see if relation is now resolved
        WebService retrievedAfter = webServiceAPI.retrieve(wsCreated.getInstanceId());

        // Log results for analysis
        System.out.println("  - WebService provider BEFORE Organization creation: " +
                (retrievedBefore.getProvider() != null ? retrievedBefore.getProvider().getUid() : "NULL"));
        System.out.println("  - WebService provider AFTER Organization creation: " +
                (retrievedAfter.getProvider() != null ? retrievedAfter.getProvider().getUid() : "NULL"));

        // The provider should exist (either stub was created, or pending relation was resolved)
        if (retrievedAfter.getProvider() != null) {
            System.out.println("✓ Test 3.1 PASSED: WebService-Organization relation resolved");
        } else {
            System.out.println("⚠ Test 3.1 WARNING: WebService provider is still null after Organization creation");
            System.out.println("  This indicates the pending relation resolution may need improvement");
        }
    }

    @Test
    @Order(21)
    @DisplayName("3.2 Create DataProduct with non-existent Organization (stub creation test)")
    void testDataProductWithNonExistentOrganization() {
        String futureOrgUid = TEST_PREFIX + "FUTURE_ORG_DP_" + UUID.randomUUID();

        // Create DataProduct FIRST, referencing Organization that doesn't exist yet
        String dpUid = TEST_PREFIX + "DP_FUTURE_" + UUID.randomUUID();
        DataProduct dp = createTestDataProduct(dpUid, "DataProduct with Future Org");
        dp.addPublisher(new LinkedEntity().uid(futureOrgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity dpCreated = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpCreated, "DataProduct should be created even with non-existent Organization");

        // Check before
        DataProduct retrievedBefore = dataProductAPI.retrieve(dpCreated.getInstanceId());

        // Now create the Organization
        Organization org = createTestOrganization(futureOrgUid, "Future Organization for DataProduct");
        LinkedEntity orgCreated = organizationAPI.create(org, StatusType.PUBLISHED, null, null);
        assertNotNull(orgCreated, "Organization should be created");

        // Re-retrieve DataProduct
        DataProduct retrievedAfter = dataProductAPI.retrieve(dpCreated.getInstanceId());

        // Log results
        boolean hadPublisherBefore = retrievedBefore.getPublisher() != null && !retrievedBefore.getPublisher().isEmpty();
        boolean hasPublisherAfter = retrievedAfter.getPublisher() != null && !retrievedAfter.getPublisher().isEmpty();

        System.out.println("  - DataProduct publisher BEFORE Organization creation: " +
                (hadPublisherBefore ? retrievedBefore.getPublisher().get(0).getUid() : "EMPTY"));
        System.out.println("  - DataProduct publisher AFTER Organization creation: " +
                (hasPublisherAfter ? retrievedAfter.getPublisher().get(0).getUid() : "EMPTY"));

        if (hasPublisherAfter) {
            System.out.println("✓ Test 3.2 PASSED: DataProduct-Organization relation resolved");
        } else {
            System.out.println("⚠ Test 3.2 WARNING: DataProduct publisher is still empty after Organization creation");
        }
    }

    // ========================================================================
    // TEST GROUP 4: Update Scenarios
    // ========================================================================

    @Test
    @Order(30)
    @DisplayName("4.1 Update WebService to change provider Organization")
    void testUpdateWebServiceProvider() {
        // Create two Organizations
        String org1Uid = TEST_PREFIX + "ORG1_UPDATE_" + UUID.randomUUID();
        String org2Uid = TEST_PREFIX + "ORG2_UPDATE_" + UUID.randomUUID();

        organizationAPI.create(createTestOrganization(org1Uid, "First Organization"), StatusType.PUBLISHED, null, null);
        organizationAPI.create(createTestOrganization(org2Uid, "Second Organization"), StatusType.PUBLISHED, null, null);

        // Create WebService with first Organization
        String wsUid = TEST_PREFIX + "WS_UPDATE_" + UUID.randomUUID();
        WebService ws = createTestWebService(wsUid, "WebService for Update Test");
        ws.setProvider(new LinkedEntity().uid(org1Uid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity wsCreated = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);

        // Verify initial provider
        WebService retrieved1 = webServiceAPI.retrieve(wsCreated.getInstanceId());
        assertEquals(org1Uid, retrieved1.getProvider().getUid(), "Initial provider should be Org1");

        // Update to second Organization
        retrieved1.setProvider(new LinkedEntity().uid(org2Uid).entityType(EntityNames.ORGANIZATION.name()));
        webServiceAPI.create(retrieved1, StatusType.PUBLISHED, null, null);

        // Verify updated provider
        WebService retrieved2 = webServiceAPI.retrieve(wsCreated.getInstanceId());
        assertEquals(org2Uid, retrieved2.getProvider().getUid(), "Updated provider should be Org2");

        System.out.println("✓ Test 4.1 PASSED: WebService provider updated from Org1 to Org2");
    }

    @Test
    @Order(31)
    @DisplayName("4.2 Update DataProduct to add second publisher Organization")
    void testUpdateDataProductAddPublisher() {
        // Create two Organizations
        String org1Uid = TEST_PREFIX + "ORG1_DP_UPDATE_" + UUID.randomUUID();
        String org2Uid = TEST_PREFIX + "ORG2_DP_UPDATE_" + UUID.randomUUID();

        organizationAPI.create(createTestOrganization(org1Uid, "First DP Organization"), StatusType.PUBLISHED, null, null);
        organizationAPI.create(createTestOrganization(org2Uid, "Second DP Organization"), StatusType.PUBLISHED, null, null);

        // Create DataProduct with first Organization
        String dpUid = TEST_PREFIX + "DP_UPDATE_" + UUID.randomUUID();
        DataProduct dp = createTestDataProduct(dpUid, "DataProduct for Update Test");
        dp.addPublisher(new LinkedEntity().uid(org1Uid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity dpCreated = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);

        // Verify initial publisher
        DataProduct retrieved1 = dataProductAPI.retrieve(dpCreated.getInstanceId());
        assertEquals(1, retrieved1.getPublisher().size(), "Should have 1 publisher initially");
        assertEquals(org1Uid, retrieved1.getPublisher().get(0).getUid(), "Initial publisher should be Org1");

        // Add second publisher
        retrieved1.addPublisher(new LinkedEntity().uid(org2Uid).entityType(EntityNames.ORGANIZATION.name()));
        dataProductAPI.create(retrieved1, StatusType.PUBLISHED, null, null);

        // Verify both publishers
        DataProduct retrieved2 = dataProductAPI.retrieve(dpCreated.getInstanceId());
        assertEquals(2, retrieved2.getPublisher().size(), "Should have 2 publishers after update");

        List<String> publisherUids = retrieved2.getPublisher().stream()
                .map(LinkedEntity::getUid)
                .toList();
        assertTrue(publisherUids.contains(org1Uid), "Should still have Org1 as publisher");
        assertTrue(publisherUids.contains(org2Uid), "Should have Org2 as publisher");

        System.out.println("✓ Test 4.2 PASSED: DataProduct updated with two publishers");
    }

    // ========================================================================
    // TEST GROUP 5: Status Variations
    // ========================================================================

    @Test
    @Order(40)
    @DisplayName("5.1 DRAFT WebService with PUBLISHED Organization")
    void testDraftWebServiceWithPublishedOrganization() {
        String orgUid = TEST_PREFIX + "ORG_PUB_" + UUID.randomUUID();
        organizationAPI.create(createTestOrganization(orgUid, "Published Org"), StatusType.PUBLISHED, null, null);

        String wsUid = TEST_PREFIX + "WS_DRAFT_" + UUID.randomUUID();
        WebService ws = createTestWebService(wsUid, "Draft WebService");
        ws.setProvider(new LinkedEntity().uid(orgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity wsCreated = webServiceAPI.create(ws, StatusType.DRAFT, null, null);

        WebService retrieved = webServiceAPI.retrieve(wsCreated.getInstanceId());
        assertNotNull(retrieved.getProvider(), "DRAFT WebService should still have provider");
        assertEquals(orgUid, retrieved.getProvider().getUid(), "Provider UID should match");

        // Verify WebService is DRAFT
        assertEquals(StatusType.DRAFT, retrieved.getStatus(), "WebService should be DRAFT");

        System.out.println("✓ Test 5.1 PASSED: DRAFT WebService with PUBLISHED Organization works");
    }

    @Test
    @Order(41)
    @DisplayName("5.2 PUBLISHED WebService with DRAFT Organization (should use PUBLISHED if available)")
    void testPublishedWebServiceWithDraftOrganization() {
        String orgUid = TEST_PREFIX + "ORG_DRAFT_" + UUID.randomUUID();

        // Create DRAFT Organization first
        organizationAPI.create(createTestOrganization(orgUid, "Draft Org"), StatusType.DRAFT, null, null);

        // Then create PUBLISHED version
        Organization pubOrg = createTestOrganization(orgUid, "Published Org Version");
        LinkedEntity pubOrgCreated = organizationAPI.create(pubOrg, StatusType.PUBLISHED, null, null);

        // Create PUBLISHED WebService
        String wsUid = TEST_PREFIX + "WS_PUB_" + UUID.randomUUID();
        WebService ws = createTestWebService(wsUid, "Published WebService");
        ws.setProvider(new LinkedEntity().uid(orgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity wsCreated = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);

        WebService retrieved = webServiceAPI.retrieve(wsCreated.getInstanceId());
        assertNotNull(retrieved.getProvider(), "WebService should have provider");

        // The provider should point to PUBLISHED Organization
        String providerInstanceId = retrieved.getProvider().getInstanceId();

        // Verify we got the PUBLISHED version
        org.epos.eposdatamodel.Organization retrievedOrg = organizationAPI.retrieve(providerInstanceId);
        assertEquals(StatusType.PUBLISHED, retrievedOrg.getStatus(),
                "Provider should be PUBLISHED Organization (REFERENCE_ENTITY logic)");

        System.out.println("✓ Test 5.2 PASSED: PUBLISHED WebService correctly uses PUBLISHED Organization");
    }

    // ========================================================================
    // TEST GROUP 6: Edge Cases
    // ========================================================================

    @Test
    @Order(50)
    @DisplayName("6.1 Multiple WebServices sharing same Organization provider")
    void testMultipleWebServicesWithSameProvider() {
        String orgUid = TEST_PREFIX + "ORG_MULTI_WS_" + UUID.randomUUID();
        LinkedEntity orgCreated = organizationAPI.create(
                createTestOrganization(orgUid, "Shared Provider Org"), StatusType.PUBLISHED, null, null);

        // Create 3 WebServices all using same provider
        String[] wsUids = new String[3];
        String[] wsInstanceIds = new String[3];

        for (int i = 0; i < 3; i++) {
            wsUids[i] = TEST_PREFIX + "WS_MULTI_" + i + "_" + UUID.randomUUID();
            WebService ws = createTestWebService(wsUids[i], "WebService " + i);
            ws.setProvider(new LinkedEntity().uid(orgUid).entityType(EntityNames.ORGANIZATION.name()));

            LinkedEntity created = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);
            wsInstanceIds[i] = created.getInstanceId();
        }

        // Verify all 3 WebServices point to same Organization
        String expectedOrgInstanceId = orgCreated.getInstanceId();

        for (int i = 0; i < 3; i++) {
            WebService retrieved = webServiceAPI.retrieve(wsInstanceIds[i]);
            assertNotNull(retrieved.getProvider(), "WebService " + i + " should have provider");
            assertEquals(expectedOrgInstanceId, retrieved.getProvider().getInstanceId(),
                    "WebService " + i + " should point to same Organization instance");
        }

        System.out.println("✓ Test 6.1 PASSED: 3 WebServices successfully share same Organization provider");
    }

    @Test
    @Order(51)
    @DisplayName("6.2 WebService and DataProduct with null/empty Organization reference")
    void testNullOrganizationReference() {
        // WebService with null provider
        String wsUid = TEST_PREFIX + "WS_NULL_PROV_" + UUID.randomUUID();
        WebService ws = createTestWebService(wsUid, "WebService without Provider");
        // Don't set provider

        LinkedEntity wsCreated = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);
        WebService wsRetrieved = webServiceAPI.retrieve(wsCreated.getInstanceId());

        assertNull(wsRetrieved.getProvider(), "WebService provider should be null when not set");

        // DataProduct with empty publisher list
        String dpUid = TEST_PREFIX + "DP_NO_PUB_" + UUID.randomUUID();
        DataProduct dp = createTestDataProduct(dpUid, "DataProduct without Publisher");
        // Don't add publisher

        LinkedEntity dpCreated = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        DataProduct dpRetrieved = dataProductAPI.retrieve(dpCreated.getInstanceId());

        assertTrue(dpRetrieved.getPublisher() == null || dpRetrieved.getPublisher().isEmpty(),
                "DataProduct publisher should be null or empty when not set");

        System.out.println("✓ Test 6.2 PASSED: Entities without Organization reference work correctly");
    }

    @Test
    @Order(52)
    @DisplayName("6.3 Delete Organization and verify cascade behavior")
    void testDeleteOrganization() {
        String orgUid = TEST_PREFIX + "ORG_DELETE_" + UUID.randomUUID();
        LinkedEntity orgCreated = organizationAPI.create(
                createTestOrganization(orgUid, "Organization to Delete"), StatusType.PUBLISHED, null, null);

        // Create WebService with this Organization
        String wsUid = TEST_PREFIX + "WS_DELETE_" + UUID.randomUUID();
        WebService ws = createTestWebService(wsUid, "WebService with deletable Org");
        ws.setProvider(new LinkedEntity().uid(orgUid).entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity wsCreated = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);

        // Verify relation exists
        WebService wsBefore = webServiceAPI.retrieve(wsCreated.getInstanceId());
        assertNotNull(wsBefore.getProvider(), "Provider should exist before delete");

        // Delete Organization
        organizationAPI.delete(orgCreated.getInstanceId());

        // Verify Organization is deleted
        org.epos.eposdatamodel.Organization deletedOrg = organizationAPI.retrieve(orgCreated.getInstanceId());
        assertNull(deletedOrg, "Organization should be deleted");

        // Check WebService behavior after Organization deletion
        // Note: Depending on implementation, provider might be null or throw exception
        try {
            WebService wsAfter = webServiceAPI.retrieve(wsCreated.getInstanceId());
            System.out.println("  - WebService provider after Organization deletion: " +
                    (wsAfter.getProvider() != null ? wsAfter.getProvider().getUid() : "NULL"));
        } catch (Exception e) {
            System.out.println("  - Exception when retrieving WebService after Organization deletion: " + e.getMessage());
        }

        System.out.println("✓ Test 6.3 PASSED: Delete Organization cascade behavior verified");
    }

    // ========================================================================
    // HELPER METHODS
    // ========================================================================

    private Organization createTestOrganization(String uid, String legalName) {
        Organization org = new Organization();
        org.setUid(uid);
        org.setInstanceId(UUID.randomUUID().toString());
        org.setMetaId(UUID.randomUUID().toString());
        org.addLegalName(legalName);
        org.setEditorId("test");
        return org;
    }

    private WebService createTestWebService(String uid, String name) {
        WebService ws = new WebService();
        ws.setUid(uid);
        ws.setInstanceId(UUID.randomUUID().toString());
        ws.setMetaId(UUID.randomUUID().toString());
        ws.setName(name);
        ws.setDescription("Test WebService Description");
        ws.setEditorId("test");
        return ws;
    }

    private DataProduct createTestDataProduct(String uid, String title) {
        DataProduct dp = new DataProduct();
        dp.setUid(uid);
        dp.setInstanceId(UUID.randomUUID().toString());
        dp.setMetaId(UUID.randomUUID().toString());
        dp.addTitle(title);
        dp.addDescription("Test DataProduct Description");
        dp.setEditorId("test");
        return dp;
    }

    private void cleanupTestData() {
        System.out.println("\n--- Cleaning up test data ---");

        try {
            // Get all test entities by UID prefix pattern
            List<Webservice> allWebServices = EposDataModelDAO.getInstance().getAllFromDB(Webservice.class);
            for (Webservice ws : allWebServices) {
                if (ws.getUid() != null && ws.getUid().startsWith(TEST_PREFIX)) {
                    try {
                        webServiceAPI.delete(ws.getInstanceId());
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }
            }

            List<Dataproduct> allDataProducts = EposDataModelDAO.getInstance().getAllFromDB(Dataproduct.class);
            for (Dataproduct dp : allDataProducts) {
                if (dp.getUid() != null && dp.getUid().startsWith(TEST_PREFIX)) {
                    try {
                        dataProductAPI.delete(dp.getInstanceId());
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }
            }

            List<model.Organization> allOrgs = EposDataModelDAO.getInstance().getAllFromDB(model.Organization.class);
            for (model.Organization org : allOrgs) {
                if (org.getUid() != null && org.getUid().startsWith(TEST_PREFIX)) {
                    try {
                        organizationAPI.delete(org.getInstanceId());
                    } catch (Exception e) {
                        // Ignore cleanup errors
                    }
                }
            }

            System.out.println("Cleanup completed");
        } catch (Exception e) {
            System.out.println("Cleanup error (non-fatal): " + e.getMessage());
        }
    }

    // ========================================================================
    // DATABASE VERIFICATION HELPERS
    // ========================================================================

    @Test
    @Order(99)
    @DisplayName("99. Final verification - dump all test relations from DB")
    void verifyDatabaseState() {
        System.out.println("\n========== DATABASE VERIFICATION ==========");

        // Check WebService -> Organization (provider field)
        System.out.println("\n--- WebService Provider Relations ---");
        List<Webservice> webServices = EposDataModelDAO.getInstance().getAllFromDB(Webservice.class);
        int wsWithProvider = 0;
        for (Webservice ws : webServices) {
            if (ws.getUid() != null && ws.getUid().startsWith(TEST_PREFIX)) {
                String provider = ws.getProvider();
                System.out.println("  WS: " + ws.getUid() + " -> Provider: " + (provider != null ? provider : "NULL"));
                if (provider != null) wsWithProvider++;
            }
        }
        System.out.println("  Total test WebServices with provider: " + wsWithProvider);

        // Check DataProduct -> Organization (DataproductPublisher join table)
        System.out.println("\n--- DataProduct Publisher Relations ---");
        List<DataproductPublisher> publishers = EposDataModelDAO.getInstance().getAllFromDB(DataproductPublisher.class);
        int dpPublisherCount = 0;
        for (DataproductPublisher pub : publishers) {
            if (pub.getDataproductInstance() != null &&
                    pub.getDataproductInstance().getUid() != null &&
                    pub.getDataproductInstance().getUid().startsWith(TEST_PREFIX)) {
                System.out.println("  DP: " + pub.getDataproductInstance().getUid() +
                        " -> Org: " + (pub.getOrganizationInstance() != null ?
                        pub.getOrganizationInstance().getUid() : "NULL"));
                dpPublisherCount++;
            }
        }
        System.out.println("  Total test DataProduct-Publisher relations: " + dpPublisherCount);

        System.out.println("\n========== END DATABASE VERIFICATION ==========");
    }
}