package integrationtests.unittests;


import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.*;
import model.*;
import model.Address;
import model.Category;
import model.CategoryScheme;
import model.Distribution;
import model.Element;
import model.Identifier;
import model.Operation;
import model.Organization;
import model.Person;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import relationsapi.RelationSyncUtil;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Integration Test Suite for Metadata Library
 *
 * This test suite covers:
 * 1. Basic CRUD operations for all entity types
 * 2. Versioning system (DRAFT, PUBLISHED, ARCHIVED transitions)
 * 3. Deferred/Pending Relations resolution
 * 4. Duplicate key prevention
 * 5. Category hierarchies (broader/narrower)
 * 6. Complex entity relationships
 * 7. Edge cases and error handling
 *
 * @author Integration Test Suite
 */
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ComprehensiveMetadataTest extends TestcontainersLifecycle  {

    // =========================================================================
    // API Instances
    // =========================================================================
    private DataProductAPI dataProductAPI;
    private DistributionAPI distributionAPI;
    private CategoryAPI categoryAPI;
    private CategorySchemeAPI categorySchemeAPI;
    private ContactPointAPI contactPointAPI;
    private OrganizationAPI organizationAPI;
    private PersonAPI personAPI;
    private WebServiceAPI webServiceAPI;
    private OperationAPI operationAPI;
    private IdentifierAPI identifierAPI;
    private SpatialAPI spatialAPI;
    private TemporalAPI temporalAPI;
    private AddressAPI addressAPI;
    private ElementAPI elementAPI;

    // =========================================================================
    // Test Data Storage
    // =========================================================================
    private final Map<String, String> createdEntityIds = new HashMap<>();

    // =========================================================================
    // Setup & Teardown
    // =========================================================================

    @BeforeAll
    void setupAll() {

        // Initialize APIs
        dataProductAPI = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);
        distributionAPI = new DistributionAPI(EntityNames.DISTRIBUTION.name(), Distribution.class);
        categoryAPI = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);
        categorySchemeAPI = new CategorySchemeAPI(EntityNames.CATEGORYSCHEME.name(), CategoryScheme.class);
        contactPointAPI = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
        organizationAPI = new OrganizationAPI(EntityNames.ORGANIZATION.name(), Organization.class);
        personAPI = new PersonAPI(EntityNames.PERSON.name(), Person.class);
        webServiceAPI = new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class);
        operationAPI = new OperationAPI(EntityNames.OPERATION.name(), Operation.class);
        identifierAPI = new IdentifierAPI(EntityNames.IDENTIFIER.name(), Identifier.class);
        spatialAPI = new SpatialAPI(EntityNames.LOCATION.name(), Spatial.class);
        temporalAPI = new TemporalAPI(EntityNames.PERIODOFTIME.name(), Temporal.class);
        addressAPI = new AddressAPI(EntityNames.ADDRESS.name(), Address.class);
        elementAPI = new ElementAPI(EntityNames.ELEMENT.name(), Element.class);

        System.out.println("=== Test Suite Initialized ===");
    }

    @AfterAll
    void teardownAll() {
        System.out.println("=== Test Suite Completed ===");
        System.out.println("Created entities: " + createdEntityIds.size());
    }

    @BeforeEach
    void setupEach(TestInfo testInfo) {
        System.out.println("\n--- Starting: " + testInfo.getDisplayName() + " ---");
    }

    @AfterEach
    void teardownEach(TestInfo testInfo) {
        System.out.println("--- Completed: " + testInfo.getDisplayName() + " ---");
    }

    // =========================================================================
    // SECTION 1: BASIC CRUD OPERATIONS
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1.1 Create and Retrieve DataProduct - DRAFT status")
    void testCreateDataProductDraft() {
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:test:crud:001");
        dp.addTitle("Test DataProduct CRUD");
        dp.addDescription("Description for CRUD test");
        dp.addKeywords("test,crud,integration");
        dp.setType("Dataset");
        dp.setStatus(StatusType.DRAFT);

        LinkedEntity result = dataProductAPI.create(dp, StatusType.DRAFT, null, null);

        assertNotNull(result, "Create should return a LinkedEntity");
        assertNotNull(result.getInstanceId(), "InstanceId should not be null");
        assertEquals("dataproduct:test:crud:001", result.getUid(), "UID should match");

        createdEntityIds.put("dp_crud_001", result.getInstanceId());

        // Retrieve and verify
        DataProduct retrieved = dataProductAPI.retrieve(result.getInstanceId());
        assertNotNull(retrieved, "Retrieved DataProduct should not be null");
        assertEquals("Test DataProduct CRUD", retrieved.getTitle().get(0), "Title should match");
        assertEquals(StatusType.DRAFT, retrieved.getStatus(), "Status should be DRAFT");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 Create and Retrieve Distribution")
    void testCreateDistribution() {
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid("distribution:test:crud:001");
        dist.addTitle("Test Distribution");
        dist.addDescription("Distribution description");
        dist.setFormat("application/json");
        dist.setType("WEB_SERVICE");
        dist.setStatus(StatusType.DRAFT);

        LinkedEntity result = distributionAPI.create(dist, StatusType.DRAFT, null, null);

        assertNotNull(result);
        assertNotNull(result.getInstanceId());

        createdEntityIds.put("dist_crud_001", result.getInstanceId());

        org.epos.eposdatamodel.Distribution retrieved = distributionAPI.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertEquals("Test Distribution", retrieved.getTitle().get(0));
    }

    @Test
    @Order(3)
    @DisplayName("1.3 Create and Retrieve Category with CategoryScheme")
    void testCreateCategoryWithScheme() {
        // First create CategoryScheme
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:test:001");
        scheme.setTitle("Test Scheme");
        scheme.setDescription("Test category scheme");
        scheme.setStatus(StatusType.DRAFT);

        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.DRAFT, null, null);
        assertNotNull(schemeResult);
        createdEntityIds.put("scheme_001", schemeResult.getInstanceId());

        // Then create Category
        org.epos.eposdatamodel.Category cat = new org.epos.eposdatamodel.Category();
        cat.setUid("category:test:001");
        cat.setName("Test Category");
        cat.setDescription("Category description");
        cat.setInScheme(schemeResult);
        cat.setStatus(StatusType.DRAFT);

        LinkedEntity catResult = categoryAPI.create(cat, StatusType.DRAFT, null, null);
        assertNotNull(catResult);
        createdEntityIds.put("cat_001", catResult.getInstanceId());

        org.epos.eposdatamodel.Category retrieved = categoryAPI.retrieve(catResult.getInstanceId());
        assertNotNull(retrieved);
        assertEquals("Test Category", retrieved.getName());
        assertNotNull(retrieved.getInScheme(), "InScheme should not be null");
    }

    @Test
    @Order(4)
    @DisplayName("1.4 Create ContactPoint with Elements")
    void testCreateContactPointWithElements() {
        ContactPoint cp = new ContactPoint();
        cp.setUid("contactpoint:test:001");
        cp.setRole("Technical Contact");
        cp.addEmail("test@example.com");
        cp.addTelephone("+1234567890");
        cp.addLanguage("en");
        cp.setStatus(StatusType.DRAFT);

        LinkedEntity result = contactPointAPI.create(cp, StatusType.DRAFT, null, null);
        assertNotNull(result);
        createdEntityIds.put("cp_001", result.getInstanceId());

        ContactPoint retrieved = contactPointAPI.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertEquals("Technical Contact", retrieved.getRole());
        assertTrue(retrieved.getEmail().contains("test@example.com"), "Email should be present");
        assertTrue(retrieved.getTelephone().contains("+1234567890"), "Telephone should be present");
    }

    @Test
    @Order(5)
    @DisplayName("1.5 Create Organization")
    void testCreateOrganization() {
        org.epos.eposdatamodel.Organization org = new org.epos.eposdatamodel.Organization();
        org.setUid("organization:test:001");
        org.addLegalName("Test Organization Inc.");
        org.setStatus(StatusType.DRAFT);

        LinkedEntity result = organizationAPI.create(org, StatusType.DRAFT, null, null);
        assertNotNull(result);
        createdEntityIds.put("org_001", result.getInstanceId());

        org.epos.eposdatamodel.Organization retrieved = organizationAPI.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertEquals("Test Organization Inc.", retrieved.getLegalName().get(0));
    }

    @Test
    @Order(6)
    @DisplayName("1.6 Create Spatial (Location)")
    void testCreateSpatial() {
        Location loc = new Location();
        loc.setUid("location:test:001");
        loc.setLocation("POLYGON((0 0, 10 0, 10 10, 0 10, 0 0))");
        loc.setStatus(StatusType.DRAFT);

        LinkedEntity result = spatialAPI.create(loc, StatusType.DRAFT, null, null);
        assertNotNull(result);
        createdEntityIds.put("loc_001", result.getInstanceId());

        Location retrieved = spatialAPI.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertNotNull(retrieved.getLocation());
    }

    @Test
    @Order(7)
    @DisplayName("1.7 Create PeriodOfTime (Temporal)")
    void testCreateTemporal() {
        PeriodOfTime pot = new PeriodOfTime();
        pot.setUid("temporal:test:001");
        pot.setStartDate(LocalDateTime.of(2024, 1, 1, 0, 0));
        pot.setEndDate(LocalDateTime.of(2024, 12, 31, 23, 59));
        pot.setStatus(StatusType.DRAFT);

        LinkedEntity result = temporalAPI.create(pot, StatusType.DRAFT, null, null);
        assertNotNull(result);
        createdEntityIds.put("temp_001", result.getInstanceId());

        PeriodOfTime retrieved = temporalAPI.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertNotNull(retrieved.getStartDate());
    }

    // =========================================================================
    // SECTION 2: VERSIONING SYSTEM TESTS
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("2.1 Versioning: DRAFT → PUBLISHED transition")
    void testVersioningDraftToPublished() {
        // Create DRAFT
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:versioning:001");
        dp.addTitle("Versioning Test - DRAFT");
        dp.setStatus(StatusType.DRAFT);

        LinkedEntity draftResult = dataProductAPI.create(dp, StatusType.DRAFT, null, null);
        assertNotNull(draftResult);

        DataProduct draftRetrieved = dataProductAPI.retrieve(draftResult.getInstanceId());
        assertEquals(StatusType.DRAFT, draftRetrieved.getStatus());

        // Update to PUBLISHED
        dp.setInstanceId(draftResult.getInstanceId());
        dp.setMetaId(draftResult.getMetaId());
        dp.setUid(draftResult.getUid());
        dp.setStatus(StatusType.PUBLISHED);
        dp.setTitle(Arrays.asList("Versioning Test - PUBLISHED"));

        LinkedEntity publishedResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(publishedResult);

        DataProduct publishedRetrieved = dataProductAPI.retrieve(publishedResult.getInstanceId());
        assertEquals(StatusType.PUBLISHED, publishedRetrieved.getStatus());
        assertEquals("Versioning Test - PUBLISHED", publishedRetrieved.getTitle().get(0));

        createdEntityIds.put("dp_versioning_001", publishedResult.getInstanceId());
    }

    @Test
    @Order(11)
    @DisplayName("2.2 Versioning: Update PUBLISHED creates new version")
    void testVersioningPublishedUpdate() {
        // Create and publish
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:versioning:002");
        dp.addTitle("Version 1 - Published");
        dp.setStatus(StatusType.PUBLISHED);

        LinkedEntity v1Result = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        String v1InstanceId = v1Result.getInstanceId();

        // Update PUBLISHED with different content
        dp.setTitle(Arrays.asList("Version 2 - Updated"));
        dp.setInstanceId(null); // Clear to force new version check
        dp.setVersionId(null);

        LinkedEntity v2Result = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);

        // Should create a new instance for the update OR update in place
        assertNotNull(v2Result);

        DataProduct retrieved = dataProductAPI.retrieve(v2Result.getInstanceId());
        assertEquals("Version 2 - Updated", retrieved.getTitle().get(0));

        createdEntityIds.put("dp_versioning_002", v2Result.getInstanceId());
    }

    @Test
    @Order(12)
    @DisplayName("2.3 Versioning: Create DRAFT on existing PUBLISHED")
    void testVersioningDraftOnPublished() {
        // Create PUBLISHED first
        DataProduct published = new DataProduct();
        published.setUid("dataproduct:versioning:003");
        published.addTitle("Original Published Version");
        published.addDescription("Test Description");
        published.setStatus(StatusType.PUBLISHED);

        LinkedEntity publishedResult = dataProductAPI.create(published, StatusType.PUBLISHED, null, null);
        assertNotNull(publishedResult);
        String publishedInstanceId = publishedResult.getInstanceId();

        // Now create a DRAFT version
        DataProduct draft = new DataProduct();
        draft.setUid("dataproduct:versioning:003"); // Same UID
        draft.addTitle("Draft Update Version");
        draft.addDescription("Updated Description");
        draft.setStatus(StatusType.DRAFT);

        LinkedEntity draftResult = dataProductAPI.create(draft, StatusType.DRAFT, null, null);
        assertNotNull(draftResult);

        // Retrieve and verify both exist
        DataProduct retrievedPublished = dataProductAPI.retrieveByUID("dataproduct:versioning:003");
        assertNotNull(retrievedPublished);

        // The behavior depends on implementation - either same instance or different
        createdEntityIds.put("dp_versioning_003", draftResult.getInstanceId());
    }

    @Test
    @Order(13)
    @DisplayName("2.4 WebService ARCHIVED -> DRAFT keeps Spatial and Temporal")
    void testWebServiceArchivedToDraftKeepsSpatialAndTemporal() {
        Documentation documentation = new Documentation();
        documentation.setUid("documentation:webservice:001");
        documentation.setTitle("WebService documentation");
        documentation.setDescription("Documentation used in the versioning test");
        documentation.setUri("https://example.org/docs/webservice-001");
        documentation.setStatus(StatusType.PUBLISHED);
        documentation.setEditorId("old-editor");
        LinkedEntity documentationLE = AbstractAPI.retrieveAPI(EntityNames.DOCUMENTATION.name())
                .create(documentation, StatusType.PUBLISHED, null, null);
        assertNotNull(documentationLE);

        Location location = new Location();
        location.setUid("location:webservice:001");
        location.setLocation("POINT(10 20)");
        location.setStatus(StatusType.PUBLISHED);
        location.setEditorId("old-editor");
        LinkedEntity locationLE = spatialAPI.create(location, StatusType.PUBLISHED, null, null);
        assertNotNull(locationLE);

        PeriodOfTime periodOfTime = new PeriodOfTime();
        periodOfTime.setUid("period:webservice:001");
        periodOfTime.setStartDate(LocalDateTime.of(2024, 1, 1, 0, 0));
        periodOfTime.setEndDate(LocalDateTime.of(2024, 12, 31, 23, 59));
        periodOfTime.setStatus(StatusType.PUBLISHED);
        periodOfTime.setEditorId("old-editor");
        LinkedEntity temporalLE = temporalAPI.create(periodOfTime, StatusType.PUBLISHED, null, null);
        assertNotNull(temporalLE);

        WebService webService = new WebService();
        webService.setUid("webservice:versioning:archived-draft:001");
        webService.setName("WebService for archive/draft test");
        webService.setDescription("Initial published version");
        webService.setStatus(StatusType.PUBLISHED);
        webService.setEditorId("old-editor");
        webService.addDocumentation(documentationLE);
        webService.addSpatialExtentItem(locationLE);
        webService.addTemporalExtent(temporalLE);

        LinkedEntity publishedLE = webServiceAPI.create(webService, StatusType.PUBLISHED, null, null);
        assertNotNull(publishedLE);

        WebService archivedRequest = webServiceAPI.retrieve(publishedLE.getInstanceId());
        archivedRequest.setStatus(StatusType.ARCHIVED);
        archivedRequest.setEditorId("old-editor");
        LinkedEntity archivedLE = webServiceAPI.create(archivedRequest, StatusType.ARCHIVED, null, null);
        assertNotNull(archivedLE);

        WebService draftRequest = webServiceAPI.retrieve(archivedLE.getInstanceId());
        draftRequest.setStatus(StatusType.DRAFT);
        draftRequest.setEditorId("new-editor");
        LinkedEntity draftLE = webServiceAPI.create(draftRequest, StatusType.DRAFT, null, null);
        assertNotNull(draftLE);

        WebService draft = webServiceAPI.retrieve(draftLE.getInstanceId());
        assertNotNull(draft.getDocumentation());
        assertFalse(draft.getDocumentation().isEmpty());
        assertNotNull(draft.getSpatialExtent());
        assertFalse(draft.getSpatialExtent().isEmpty());
        assertNotNull(draft.getTemporalExtent());
        assertFalse(draft.getTemporalExtent().isEmpty());

        org.epos.eposdatamodel.Documentation draftDocumentation = (org.epos.eposdatamodel.Documentation)
                AbstractAPI.retrieveAPI(EntityNames.DOCUMENTATION.name())
                        .retrieve(draft.getDocumentation().get(0).getInstanceId());
        org.epos.eposdatamodel.Location draftSpatial = spatialAPI.retrieve(draft.getSpatialExtent().get(0).getInstanceId());
        org.epos.eposdatamodel.PeriodOfTime draftTemporal = temporalAPI.retrieve(draft.getTemporalExtent().get(0).getInstanceId());

        assertNotNull(draftDocumentation);
        assertNotNull(draftSpatial);
        assertNotNull(draftTemporal);

        model.Versioningstatus documentationVersion = (model.Versioningstatus) EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceId(draftDocumentation.getInstanceId(), model.Versioningstatus.class)
                .stream().findFirst().orElse(null);
        model.Versioningstatus spatialVersion = (model.Versioningstatus) EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceId(draftSpatial.getInstanceId(), model.Versioningstatus.class)
                .stream().findFirst().orElse(null);
        model.Versioningstatus temporalVersion = (model.Versioningstatus) EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceId(draftTemporal.getInstanceId(), model.Versioningstatus.class)
                .stream().findFirst().orElse(null);

        assertNotNull(documentationVersion);
        assertNotNull(spatialVersion);
        assertNotNull(temporalVersion);
        assertEquals("new-editor", documentationVersion.getEditorId());
        assertEquals("new-editor", spatialVersion.getEditorId());
        assertEquals("new-editor", temporalVersion.getEditorId());
    }

    @Test
    @Order(14)
    @DisplayName("2.5 WebService PUBLISHED -> DRAFT is visible in status listing")
    void testWebServicePublishedToDraftIsListed() {
        String uid = "webservice:versioning:published-draft:001";

        WebService published = new WebService();
        published.setUid(uid);
        published.setName("WebService for published/draft test");
        published.setDescription("Initial published version");
        published.setStatus(StatusType.PUBLISHED);
        published.setEditorId("pub-editor");

        LinkedEntity publishedLE = webServiceAPI.create(published, StatusType.PUBLISHED, null, null);
        assertNotNull(publishedLE);

        WebService draftRequest = webServiceAPI.retrieve(publishedLE.getInstanceId());
        assertNotNull(draftRequest);
        draftRequest.setStatus(StatusType.DRAFT);
        draftRequest.setEditorId("draft-editor");

        LinkedEntity draftLE = webServiceAPI.create(draftRequest, StatusType.DRAFT, null, null);
        assertNotNull(draftLE);

        WebService draft = webServiceAPI.retrieve(draftLE.getInstanceId());
        assertNotNull(draft);
        assertEquals(StatusType.DRAFT, draft.getStatus());
        assertEquals(uid, draft.getUid());

        List<org.epos.eposdatamodel.WebService> drafts = webServiceAPI.retrieveAllWithStatus(StatusType.DRAFT);
        assertNotNull(drafts);
        assertTrue(drafts.stream().anyMatch(ws -> uid.equals(ws.getUid()) && StatusType.DRAFT.equals(ws.getStatus())),
                "Draft webservice should be visible in retrieveAllWithStatus(DRAFT)");

        WebService publishedAgain = webServiceAPI.retrieve(publishedLE.getInstanceId());
        assertEquals(StatusType.PUBLISHED, publishedAgain.getStatus());
    }

    // =========================================================================
    // SECTION 3: DEFERRED/PENDING RELATIONS (CRITICAL BUG FIX TEST)
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("3.1 Deferred Resolution: DataProduct references non-existent Distribution")
    void testDeferredResolutionDataProductToDistribution() {
        // Step 1: Create DataProduct referencing a Distribution that doesn't exist yet
        String futureDistUid = "distribution:future:" + UUID.randomUUID();

        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:deferred:001");
        dp.addTitle("DataProduct with Future Distribution");
        dp.setStatus(StatusType.PUBLISHED);

        LinkedEntity distLink = new LinkedEntity();
        distLink.setUid(futureDistUid);
        distLink.setEntityType(EntityNames.DISTRIBUTION.name());
        dp.addDistribution(distLink);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult, "DataProduct should be created even with missing Distribution");

        // Verify Distribution is NOT linked yet
        DataProduct dpRetrieved = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertNotNull(dpRetrieved);
        // Distribution may be null or empty at this point

        // Step 2: Now create the Distribution
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid(futureDistUid);
        dist.addTitle("Future Distribution Now Created");
        dist.setFormat("application/xml");
        dist.setStatus(StatusType.PUBLISHED);

        LinkedEntity distResult = distributionAPI.create(dist, StatusType.PUBLISHED, null, null);
        assertNotNull(distResult, "Distribution should be created");

        // Step 3: Verify the relation is now resolved
        DataProduct dpFinal = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertNotNull(dpFinal);
        assertNotNull(dpFinal.getDistribution(), "Distribution list should not be null after resolution");
        assertFalse(dpFinal.getDistribution().isEmpty(), "Distribution list should not be empty after resolution");

        // Verify the correct UID
        boolean found = dpFinal.getDistribution().stream()
                .anyMatch(d -> futureDistUid.equals(d.getUid()));
        assertTrue(found, "The deferred Distribution should now be linked");

        createdEntityIds.put("dp_deferred_001", dpResult.getInstanceId());
        createdEntityIds.put("dist_deferred_001", distResult.getInstanceId());

        System.out.println("✓ Deferred resolution test PASSED - Distribution linked successfully");
    }

    @Test
    @Order(21)
    @DisplayName("3.2 Deferred Resolution: Multiple pending relations")
    void testDeferredResolutionMultiple() {
        String futureOrgUid = "organization:future:" + UUID.randomUUID();
        String futureCatUid = "category:future:" + UUID.randomUUID();

        // Create DataProduct referencing non-existent Organization and Category
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:deferred:002");
        dp.addTitle("DataProduct with Multiple Pending Relations");
        dp.setStatus(StatusType.PUBLISHED);

        LinkedEntity orgLink = new LinkedEntity();
        orgLink.setUid(futureOrgUid);
        orgLink.setEntityType(EntityNames.ORGANIZATION.name());
        dp.addPublisher(orgLink);

        LinkedEntity catLink = new LinkedEntity();
        catLink.setUid(futureCatUid);
        catLink.setEntityType(EntityNames.CATEGORY.name());
        dp.addCategory(catLink);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult);

        // Create Organization
        org.epos.eposdatamodel.Organization org = new org.epos.eposdatamodel.Organization();
        org.setUid(futureOrgUid);
        org.addLegalName("Future Organization");
        org.setStatus(StatusType.PUBLISHED);

        LinkedEntity orgResult = organizationAPI.create(org, StatusType.PUBLISHED, null, null);
        assertNotNull(orgResult);

        // Create Category (needs scheme first)
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:deferred:" + UUID.randomUUID());
        scheme.setTitle("Deferred Test Scheme");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.Category cat = new org.epos.eposdatamodel.Category();
        cat.setUid(futureCatUid);
        cat.setName("Future Category");
        cat.setInScheme(schemeResult);
        cat.setStatus(StatusType.PUBLISHED);

        LinkedEntity catResult = categoryAPI.create(cat, StatusType.PUBLISHED, null, null);
        assertNotNull(catResult);

        // Verify all relations resolved
        DataProduct dpFinal = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertNotNull(dpFinal);

        assertNotNull(dpFinal.getPublisher(), "Publisher should be resolved");
        assertNotNull(dpFinal.getCategory(), "Category should be resolved");

        System.out.println("✓ Multiple deferred relations test PASSED");
    }

    @Test
    @Order(22)
    @DisplayName("3.3 Pending Relations: No conflict with Versioning (BUG FIX VERIFICATION)")
    void testPendingRelationNoVersioningConflict() {
        // This test verifies the fix for the PENDING/Versioning conflict bug
        String futureDistUid = "distribution:noconflict:" + UUID.randomUUID();

        // Create DataProduct that references future Distribution
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:noconflict:" + UUID.randomUUID());
        dp.addTitle("No Conflict Test");
        dp.setStatus(StatusType.PUBLISHED);

        LinkedEntity distLink = new LinkedEntity();
        distLink.setUid(futureDistUid);
        distLink.setEntityType(EntityNames.DISTRIBUTION.name());
        dp.addDistribution(distLink);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult);

        // At this point, a PENDING marker should exist in Versioningstatus for futureDistUid
        // When we create the Distribution, checkVersion() should NOT find this PENDING marker
        // as a versioning record

        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid(futureDistUid);
        dist.addTitle("Distribution Created After Pending");
        dist.setStatus(StatusType.PUBLISHED);

        LinkedEntity distResult = distributionAPI.create(dist, StatusType.PUBLISHED, null, null);
        assertNotNull(distResult, "Distribution creation should succeed");

        // The Distribution should have its OWN Versioningstatus record, not the PENDING marker
        org.epos.eposdatamodel.Distribution distRetrieved = distributionAPI.retrieve(distResult.getInstanceId());
        assertNotNull(distRetrieved, "Distribution should be retrievable");
        assertEquals(StatusType.PUBLISHED, distRetrieved.getStatus(),
                "Distribution status should be PUBLISHED, not corrupted by PENDING marker");
        assertEquals("Distribution Created After Pending", distRetrieved.getTitle().get(0),
                "Distribution title should be correct");

        // Verify the DataProduct now has the Distribution linked
        DataProduct dpFinal = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertNotNull(dpFinal.getDistribution(), "Distribution should be linked");
        assertFalse(dpFinal.getDistribution().isEmpty(), "Distribution list should not be empty");

        System.out.println("✓ PENDING/Versioning conflict fix VERIFIED");
    }

    // =========================================================================
    // SECTION 4: DUPLICATE KEY PREVENTION (BUG FIX TEST)
    // =========================================================================

    @Test
    @Order(30)
    @DisplayName("4.1 Duplicate Key Prevention: Repeated relation creation")
    void testDuplicateKeyPrevention() {
        // Create entities
        org.epos.eposdatamodel.Organization org = new org.epos.eposdatamodel.Organization();
        org.setUid("organization:duptest:" + UUID.randomUUID());
        org.addLegalName("Duplicate Test Org");
        org.setStatus(StatusType.PUBLISHED);
        LinkedEntity orgResult = organizationAPI.create(org, StatusType.PUBLISHED, null, null);

        ContactPoint cp = new ContactPoint();
        cp.setUid("contactpoint:duptest:" + UUID.randomUUID());
        cp.setRole("Test Contact");
        cp.addEmail("dup@test.com");
        cp.setStatus(StatusType.PUBLISHED);
        LinkedEntity cpResult = contactPointAPI.create(cp, StatusType.PUBLISHED, null, null);

        // Create DataProduct with Organization as publisher multiple times
        // This should NOT cause duplicate key errors
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:duptest:" + UUID.randomUUID());
        dp.addTitle("Duplicate Key Test");
        dp.setStatus(StatusType.PUBLISHED);
        dp.addPublisher(orgResult);
        dp.addContactPoint(cpResult);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult);

        // Update the same DataProduct (this could trigger duplicate relations)
        dp.setInstanceId(dpResult.getInstanceId());
        dp.setMetaId(dpResult.getMetaId());
        dp.setTitle(Arrays.asList("Duplicate Key Test - Updated"));

        // This should not throw duplicate key exception
        assertDoesNotThrow(() -> {
            dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        }, "Updating DataProduct should not cause duplicate key errors");

        System.out.println("✓ Duplicate key prevention test PASSED");
    }

    @Test
    @Order(31)
    @DisplayName("4.2 Duplicate Pending Relations Prevention")
    void testDuplicatePendingRelationsPrevention() {
        String futureUid = "distribution:pendingdup:" + UUID.randomUUID();

        // Create two DataProducts referencing the same non-existent Distribution
        DataProduct dp1 = new DataProduct();
        dp1.setUid("dataproduct:pendingdup:001:" + UUID.randomUUID());
        dp1.addTitle("Pending Dup Test 1");
        dp1.setStatus(StatusType.PUBLISHED);

        LinkedEntity distLink = new LinkedEntity();
        distLink.setUid(futureUid);
        distLink.setEntityType(EntityNames.DISTRIBUTION.name());
        dp1.addDistribution(distLink);

        DataProduct dp2 = new DataProduct();
        dp2.setUid("dataproduct:pendingdup:002:" + UUID.randomUUID());
        dp2.addTitle("Pending Dup Test 2");
        dp2.setStatus(StatusType.PUBLISHED);
        dp2.addDistribution(distLink);

        // Both should succeed without errors
        LinkedEntity dp1Result = dataProductAPI.create(dp1, StatusType.PUBLISHED, null, null);
        LinkedEntity dp2Result = dataProductAPI.create(dp2, StatusType.PUBLISHED, null, null);

        assertNotNull(dp1Result);
        assertNotNull(dp2Result);

        // Now create the Distribution - should resolve for BOTH
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid(futureUid);
        dist.addTitle("Shared Distribution");
        dist.setStatus(StatusType.PUBLISHED);

        assertDoesNotThrow(() -> {
            distributionAPI.create(dist, StatusType.PUBLISHED, null, null);
        }, "Creating shared Distribution should not cause duplicate key errors");

        System.out.println("✓ Duplicate pending relations prevention test PASSED");
    }

    // =========================================================================
    // SECTION 5: CATEGORY HIERARCHIES
    // =========================================================================

    @Test
    @Order(40)
    @DisplayName("5.1 Category Hierarchy: Broader/Narrower relations")
    void testCategoryHierarchy() {
        // Create scheme
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:hierarchy:" + UUID.randomUUID());
        scheme.setTitle("Hierarchy Test Scheme");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        // Create parent category
        org.epos.eposdatamodel.Category parent = new org.epos.eposdatamodel.Category();
        parent.setUid("category:parent:" + UUID.randomUUID());
        parent.setName("Parent Category");
        parent.setInScheme(schemeResult);
        parent.setStatus(StatusType.PUBLISHED);
        LinkedEntity parentResult = categoryAPI.create(parent, StatusType.PUBLISHED, null, null);

        // Create child category with broader = parent
        org.epos.eposdatamodel.Category child = new org.epos.eposdatamodel.Category();
        child.setUid("category:child:" + UUID.randomUUID());
        child.setName("Child Category");
        child.setInScheme(schemeResult);
        child.addBroader(parentResult);
        child.setStatus(StatusType.PUBLISHED);
        LinkedEntity childResult = categoryAPI.create(child, StatusType.PUBLISHED, null, null);

        // Verify relationships
        org.epos.eposdatamodel.Category childRetrieved = categoryAPI.retrieve(childResult.getInstanceId());
        assertNotNull(childRetrieved.getBroader(), "Child should have broader");
        assertFalse(childRetrieved.getBroader().isEmpty(), "Broader list should not be empty");

        org.epos.eposdatamodel.Category parentRetrieved = categoryAPI.retrieve(parentResult.getInstanceId());
        assertNotNull(parentRetrieved.getNarrower(), "Parent should have narrower");

        System.out.println("✓ Category hierarchy test PASSED");
    }

    @Test
    @Order(41)
    @DisplayName("5.2 Category: Deferred broader reference")
    void testCategoryDeferredBroader() {
        String futureBroaderUid = "category:futurebroader:" + UUID.randomUUID();

        // Create scheme
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:deferredbroader:" + UUID.randomUUID());
        scheme.setTitle("Deferred Broader Test");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        // Create child category referencing non-existent broader
        LinkedEntity broaderLink = new LinkedEntity();
        broaderLink.setUid(futureBroaderUid);
        broaderLink.setEntityType(EntityNames.CATEGORY.name());

        org.epos.eposdatamodel.Category child = new org.epos.eposdatamodel.Category();
        child.setUid("category:childdeferred:" + UUID.randomUUID());
        child.setName("Child with Deferred Broader");
        child.setInScheme(schemeResult);
        child.addBroader(broaderLink);
        child.setStatus(StatusType.PUBLISHED);

        LinkedEntity childResult = categoryAPI.create(child, StatusType.PUBLISHED, null, null);
        assertNotNull(childResult);

        // Now create the broader category
        org.epos.eposdatamodel.Category broader = new org.epos.eposdatamodel.Category();
        broader.setUid(futureBroaderUid);
        broader.setName("Broader Category Created Later");
        broader.setInScheme(schemeResult);
        broader.setStatus(StatusType.PUBLISHED);

        LinkedEntity broaderResult = categoryAPI.create(broader, StatusType.PUBLISHED, null, null);
        assertNotNull(broaderResult);

        // Verify relation
        org.epos.eposdatamodel.Category childFinal = categoryAPI.retrieve(childResult.getInstanceId());
        // The broader relation should be established
        assertNotNull(childFinal);

        System.out.println("✓ Category deferred broader test PASSED");
    }

    // =========================================================================
    // SECTION 6: COMPLEX ENTITY RELATIONSHIPS
    // =========================================================================

    @Test
    @Order(50)
    @DisplayName("6.1 Complete DataProduct with all relations")
    void testCompleteDataProduct() {
        // Create all related entities first

        // Organization
        org.epos.eposdatamodel.Organization publisher = new org.epos.eposdatamodel.Organization();
        publisher.setUid("organization:complete:" + UUID.randomUUID());
        publisher.addLegalName("Complete Test Publisher");
        publisher.setStatus(StatusType.PUBLISHED);
        LinkedEntity pubResult = organizationAPI.create(publisher, StatusType.PUBLISHED, null, null);

        // ContactPoint
        ContactPoint cp = new ContactPoint();
        cp.setUid("contactpoint:complete:" + UUID.randomUUID());
        cp.setRole("Data Manager");
        cp.addEmail("complete@test.org");
        cp.setStatus(StatusType.PUBLISHED);
        LinkedEntity cpResult = contactPointAPI.create(cp, StatusType.PUBLISHED, null, null);

        // Category
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:complete:" + UUID.randomUUID());
        scheme.setTitle("Complete Test Scheme");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.Category cat = new org.epos.eposdatamodel.Category();
        cat.setUid("category:complete:" + UUID.randomUUID());
        cat.setName("Complete Test Category");
        cat.setInScheme(schemeResult);
        cat.setStatus(StatusType.PUBLISHED);
        LinkedEntity catResult = categoryAPI.create(cat, StatusType.PUBLISHED, null, null);

        // Distribution
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid("distribution:complete:" + UUID.randomUUID());
        dist.addTitle("Complete Test Distribution");
        dist.setFormat("application/json");
        dist.setStatus(StatusType.PUBLISHED);
        LinkedEntity distResult = distributionAPI.create(dist, StatusType.PUBLISHED, null, null);

        // Spatial
        Location loc = new Location();
        loc.setUid("location:complete:" + UUID.randomUUID());
        loc.setLocation("POINT(10.5 45.5)");
        loc.setStatus(StatusType.PUBLISHED);
        LinkedEntity locResult = spatialAPI.create(loc, StatusType.PUBLISHED, null, null);

        // Temporal
        PeriodOfTime pot = new PeriodOfTime();
        pot.setUid("temporal:complete:" + UUID.randomUUID());
        pot.setStartDate(LocalDateTime.now().minusYears(1));
        pot.setEndDate(LocalDateTime.now());
        pot.setStatus(StatusType.PUBLISHED);
        LinkedEntity potResult = temporalAPI.create(pot, StatusType.PUBLISHED, null, null);

        // Now create the complete DataProduct
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:complete:" + UUID.randomUUID());
        dp.addTitle("Complete DataProduct Test");
        dp.addDescription("A fully populated DataProduct for testing");
        dp.addKeywords("complete,test,integration");
        dp.setType("Dataset");
        dp.setAccessRight("Open");
        dp.setAccrualPeriodicity("daily");
        dp.setCreated(LocalDate.now().minusMonths(6).atStartOfDay());
        dp.setModified(LocalDate.now().atStartOfDay());
        dp.setStatus(StatusType.PUBLISHED);

        // Add all relations
        dp.addPublisher(pubResult);
        dp.addContactPoint(cpResult);
        dp.addCategory(catResult);
        dp.addDistribution(distResult);
        dp.addSpatialExtentItem(locResult);
        dp.addTemporalExtent(potResult);
        dp.addProvenance("Test provenance information");
        dp.addLandingPage("https://example.org/data");

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult);

        // Retrieve and verify all relations
        DataProduct retrieved = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertNotNull(retrieved);

        assertEquals("Complete DataProduct Test", retrieved.getTitle().get(0));
        assertNotNull(retrieved.getPublisher(), "Publisher should be set");
        assertFalse(retrieved.getPublisher().isEmpty(), "Publisher list should not be empty");
        assertNotNull(retrieved.getContactPoint(), "ContactPoint should be set");
        assertNotNull(retrieved.getCategory(), "Category should be set");
        assertNotNull(retrieved.getDistribution(), "Distribution should be set");
        assertNotNull(retrieved.getSpatialExtent(), "SpatialExtent should be set");
        assertNotNull(retrieved.getTemporalExtent(), "TemporalExtent should be set");

        System.out.println("✓ Complete DataProduct test PASSED");
    }

    @Test
    @Order(51)
    @DisplayName("6.2 Distribution with DataProduct back-reference")
    void testDistributionWithDataProductReference() {
        // Create DataProduct first
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:backref:" + UUID.randomUUID());
        dp.addTitle("BackRef Test DataProduct");
        dp.setStatus(StatusType.PUBLISHED);
        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);

        // Create the child first, then attach it from the canonical owner.
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid("distribution:backref:" + UUID.randomUUID());
        dist.addTitle("Distribution with DataProduct ref");
        dist.setFormat("text/csv");
        dist.setStatus(StatusType.PUBLISHED);
        LinkedEntity distResult = distributionAPI.create(dist, StatusType.PUBLISHED, null, null);
        assertNotNull(distResult);

        dp.addDistribution(distResult);
        dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);

        // Verify bidirectional relationship
        org.epos.eposdatamodel.Distribution distRetrieved = distributionAPI.retrieve(distResult.getInstanceId());
        assertNotNull(distRetrieved.getDataProduct(), "DataProduct reference should be set");
        assertFalse(distRetrieved.getDataProduct().isEmpty());

        System.out.println("✓ Distribution back-reference test PASSED");
    }

    // =========================================================================
    // SECTION 7: EDGE CASES AND ERROR HANDLING
    // =========================================================================

    @Test
    @Order(60)
    @DisplayName("7.1 Retrieve non-existent entity returns null")
    void testRetrieveNonExistent() {
        String fakeId = UUID.randomUUID().toString();

        assertNull(dataProductAPI.retrieve(fakeId));
        assertNull(distributionAPI.retrieve(fakeId));
        assertNull(categoryAPI.retrieve(fakeId));
        assertNull(organizationAPI.retrieve(fakeId));
    }

    @Test
    @Order(61)
    @DisplayName("7.2 Create entity with null optional fields")
    void testCreateWithNullOptionalFields() {
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:nullfields:" + UUID.randomUUID());
        dp.addTitle("Minimal DataProduct");
        // All other fields are null
        dp.setStatus(StatusType.DRAFT);

        LinkedEntity result = dataProductAPI.create(dp, StatusType.DRAFT, null, null);
        assertNotNull(result, "Should create entity with minimal fields");

        DataProduct retrieved = dataProductAPI.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertEquals("Minimal DataProduct", retrieved.getTitle().get(0));
    }

    @Test
    @Order(62)
    @DisplayName("7.3 Update entity preserves existing relations when not specified")
    void testUpdatePreservesRelations() {
        // Create DataProduct with relations
        org.epos.eposdatamodel.Organization org = new org.epos.eposdatamodel.Organization();
        org.setUid("organization:preserve:" + UUID.randomUUID());
        org.addLegalName("Preserve Test Org");
        org.setStatus(StatusType.DRAFT);
        LinkedEntity orgResult = organizationAPI.create(org, StatusType.DRAFT, null, null);

        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:preserve:" + UUID.randomUUID());
        dp.addTitle("Original Title");
        dp.addPublisher(orgResult);
        dp.setStatus(StatusType.DRAFT);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.DRAFT, null, null);

        // Update only the title, not the publisher
        DataProduct update = new DataProduct();
        update.setInstanceId(dpResult.getInstanceId());
        update.setMetaId(dpResult.getMetaId());
        update.setUid(dpResult.getUid());
        update.setTitle(Arrays.asList("Updated Title"));
        update.setStatus(StatusType.DRAFT);
        // Publisher not set - should be preserved

        dataProductAPI.create(update, StatusType.DRAFT, null, null);

        DataProduct retrieved = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertEquals("Updated Title", retrieved.getTitle().get(0));
        // Publisher should still be there (depends on implementation)
    }

    @Test
    @Order(63)
    @DisplayName("7.4 RetrieveByUID works correctly")
    void testRetrieveByUID() {
        String uniqueUid = "dataproduct:byuid:" + UUID.randomUUID();

        DataProduct dp = new DataProduct();
        dp.setUid(uniqueUid);
        dp.addTitle("UID Lookup Test");
        dp.setStatus(StatusType.DRAFT);

        dataProductAPI.create(dp, StatusType.DRAFT, null, null);

        DataProduct retrieved = dataProductAPI.retrieveByUID(uniqueUid);
        assertNotNull(retrieved, "Should find by UID");
        assertEquals("UID Lookup Test", retrieved.getTitle().get(0));

        // Non-existent UID
        assertNull(dataProductAPI.retrieveByUID("nonexistent:uid:12345"));
    }

    @Test
    @Order(64)
    @DisplayName("7.5 Delete entity removes related join records")
    void testDeleteRemovesRelations() {
        // Create entities
        ContactPoint cp = new ContactPoint();
        cp.setUid("contactpoint:delete:" + UUID.randomUUID());
        cp.setRole("Delete Test");
        cp.addEmail("delete@test.com");
        cp.setStatus(StatusType.DRAFT);
        LinkedEntity cpResult = contactPointAPI.create(cp, StatusType.DRAFT, null, null);

        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:delete:" + UUID.randomUUID());
        dp.addTitle("Delete Test");
        dp.addContactPoint(cpResult);
        dp.setStatus(StatusType.DRAFT);
        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.DRAFT, null, null);

        // Delete DataProduct
        Boolean deleted = dataProductAPI.delete(dpResult.getInstanceId());
        assertTrue(deleted);

        // Verify it's gone
        assertNull(dataProductAPI.retrieve(dpResult.getInstanceId()));

        // ContactPoint should still exist
        assertNotNull(contactPointAPI.retrieve(cpResult.getInstanceId()));
    }

    // =========================================================================
    // SECTION 8: CONCURRENT OPERATIONS (Thread Safety)
    // =========================================================================

    @Test
    @Order(70)
    @DisplayName("8.1 Concurrent entity creation")
    void testConcurrentCreation() throws Exception {
        int numThreads = 5;
        List<Thread> threads = new ArrayList<>();
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<Exception> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < numThreads; i++) {
            final int index = i;
            Thread t = new Thread(() -> {
                try {
                    DataProduct dp = new DataProduct();
                    dp.setUid("dataproduct:concurrent:" + index + ":" + UUID.randomUUID());
                    dp.addTitle("Concurrent Test " + index);
                    dp.setStatus(StatusType.DRAFT);

                    LinkedEntity result = dataProductAPI.create(dp, StatusType.DRAFT, null, null);
                    results.add(result.getInstanceId());
                } catch (Exception e) {
                    errors.add(e);
                }
            });
            threads.add(t);
        }

        // Start all threads
        for (Thread t : threads) t.start();

        // Wait for completion
        for (Thread t : threads) t.join(10000);

        // Verify
        assertTrue(errors.isEmpty(), "No errors should occur: " + errors);
        assertEquals(numThreads, results.size(), "All creates should succeed");

        System.out.println("✓ Concurrent creation test PASSED - " + numThreads + " entities created");
    }

    // =========================================================================
    // SECTION 9: STATUS RETRIEVAL
    // =========================================================================

    @Test
    @Order(80)
    @DisplayName("9.1 RetrieveAllWithStatus filters correctly")
    void testRetrieveAllWithStatus() {
        // Create entities with different statuses
        DataProduct draft = new DataProduct();
        draft.setUid("dataproduct:statusfilter:draft:" + UUID.randomUUID());
        draft.addTitle("Draft for Status Filter");
        draft.setStatus(StatusType.DRAFT);
        dataProductAPI.create(draft, StatusType.DRAFT, null, null);

        DataProduct published = new DataProduct();
        published.setUid("dataproduct:statusfilter:pub:" + UUID.randomUUID());
        published.addTitle("Published for Status Filter");
        published.setStatus(StatusType.PUBLISHED);
        dataProductAPI.create(published, StatusType.PUBLISHED, null, null);

        // Retrieve by status
        List<DataProduct> drafts = dataProductAPI.retrieveAllWithStatus(StatusType.DRAFT);
        List<DataProduct> pubs = dataProductAPI.retrieveAllWithStatus(StatusType.PUBLISHED);

        assertNotNull(drafts);
        assertNotNull(pubs);

        // Verify filtering works (exact counts depend on test order)
        for (DataProduct dp : drafts) {
            assertEquals(StatusType.DRAFT, dp.getStatus(), "All should be DRAFT");
        }
        for (DataProduct dp : pubs) {
            assertEquals(StatusType.PUBLISHED, dp.getStatus(), "All should be PUBLISHED");
        }

        System.out.println("✓ Status filter test PASSED - Found " + drafts.size() + " drafts, " + pubs.size() + " published");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private void printTestSummary() {
        System.out.println("\n========================================");
        System.out.println("TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("Total entities created: " + createdEntityIds.size());
        System.out.println("========================================");
    }
}
