package integrationtests.unittests;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.*;
import model.*;
import model.Category;
import model.CategoryScheme;
import model.Distribution;
import model.Organization;
import model.Person;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Specific tests to verify the bug fixes:
 *
 * BUG 1: PENDING/Versioning Conflict
 *   - The Versioningstatus table is used for both versioning records and pending relation markers
 *   - checkVersion() was incorrectly finding PENDING markers when looking up entities by UID
 *   - FIX: isPendingRelationMarker() method to skip PENDING markers
 *
 * BUG 2: Duplicate Key Violations
 *   - resolveSinglePendingRelation() didn't check if relation already exists
 *   - FIX: joinRelationAlreadyExists() check before creating relations
 *
 * BUG 3: CategoryAPI.findOrCreateStub() not persisting Versioningstatus
 *   - Stub categories had Versioningstatus in memory but not persisted
 *   - FIX: Properly create and persist Versioningstatus before Category
 */
@Testcontainers
@TestMethodOrder(OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Bug Fix Verification Tests")
public class BugFixVerificationTest extends TestcontainersLifecycle {

    private DataProductAPI dataProductAPI;
    private DistributionAPI distributionAPI;
    private CategoryAPI categoryAPI;
    private CategorySchemeAPI categorySchemeAPI;
    private ContactPointAPI contactPointAPI;
    private WebServiceAPI webServiceAPI;
    private OrganizationAPI organizationAPI;
    private PersonAPI personAPI;

    @BeforeAll
    void setup() {
        dataProductAPI = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);
        distributionAPI = new DistributionAPI(EntityNames.DISTRIBUTION.name(), Distribution.class);
        categoryAPI = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);
        categorySchemeAPI = new CategorySchemeAPI(EntityNames.CATEGORYSCHEME.name(), CategoryScheme.class);
        contactPointAPI = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);
        webServiceAPI = new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class);
        organizationAPI = new OrganizationAPI(EntityNames.ORGANIZATION.name(), Organization.class);
        personAPI = new PersonAPI(EntityNames.PERSON.name(), Person.class);

        System.out.println("=== Bug Fix Verification Tests Started ===");
    }

    // =========================================================================
    // BUG 1: PENDING/Versioning Conflict Tests
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("BUG1-1: DataProduct→Distribution deferred resolution works correctly")
    void testBug1_DataProductDistributionDeferredResolution() {
        String futureDistUid = "distribution:bug1test:" + UUID.randomUUID();

        // Step 1: Create DataProduct referencing non-existent Distribution
        System.out.println("Step 1: Creating DataProduct with reference to future Distribution: " + futureDistUid);

        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:bug1test:" + UUID.randomUUID());
        dp.addTitle("Bug1 Test DataProduct");
        dp.addDescription("Testing PENDING/Versioning conflict fix");
        dp.setStatus(StatusType.PUBLISHED);

        LinkedEntity distLink = new LinkedEntity();
        distLink.setUid(futureDistUid);
        distLink.setEntityType(EntityNames.DISTRIBUTION.name());
        dp.addDistribution(distLink);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult, "DataProduct should be created");
        System.out.println("  DataProduct created with instanceId: " + dpResult.getInstanceId());

        // At this point, a PENDING marker should exist in versioningstatus table
        // When we create the Distribution, checkVersion() should NOT confuse this with a versioning record

        // Step 2: Create the Distribution
        System.out.println("Step 2: Creating Distribution with UID: " + futureDistUid);

        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid(futureDistUid);
        dist.addTitle("Bug1 Test Distribution");
        dist.setFormat("application/json");
        dist.setStatus(StatusType.PUBLISHED);

        LinkedEntity distResult = distributionAPI.create(dist, StatusType.PUBLISHED, null, null);

        assertNotNull(distResult, "Distribution should be created successfully");
        System.out.println("  Distribution created with instanceId: " + distResult.getInstanceId());

        // Step 3: Verify Distribution has correct status (not PENDING from the marker)
        org.epos.eposdatamodel.Distribution distRetrieved = distributionAPI.retrieve(distResult.getInstanceId());
        assertNotNull(distRetrieved, "Distribution should be retrievable");
        assertEquals(StatusType.PUBLISHED, distRetrieved.getStatus(),
                "BUG1 VERIFICATION: Distribution status should be PUBLISHED, not corrupted by PENDING marker");
        assertEquals("Bug1 Test Distribution", distRetrieved.getTitle().get(0),
                "Distribution title should be correct");

        // Step 4: Verify DataProduct now has Distribution linked
        DataProduct dpRetrieved = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertNotNull(dpRetrieved, "DataProduct should be retrievable");
        assertNotNull(dpRetrieved.getDistribution(), "Distribution list should not be null");
        assertFalse(dpRetrieved.getDistribution().isEmpty(),
                "BUG1 VERIFICATION: Distribution should be linked to DataProduct after deferred resolution");

        boolean found = dpRetrieved.getDistribution().stream()
                .anyMatch(d -> futureDistUid.equals(d.getUid()));
        assertTrue(found, "The specific Distribution should be found in the list");

        System.out.println("✓ BUG1-1 PASSED: PENDING/Versioning conflict fix verified for DataProduct→Distribution");
    }

    @Test
    @Order(2)
    @DisplayName("BUG1-2: Category retrieval doesn't return null due to PENDING markers")
    void testBug1_CategoryRetrievalNotNull() {
        // This tests the scenario where Category.getUid() threw NullPointerException
        // because retrieve() returned null due to PENDING marker confusion

        String futureCatUid = "category:bug1cattest:" + UUID.randomUUID();

        // Create scheme first
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:bug1cattest:" + UUID.randomUUID());
        scheme.setTitle("Bug1 Category Test Scheme");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        // Create DataProduct referencing non-existent Category
        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:bug1cattest:" + UUID.randomUUID());
        dp.addTitle("Bug1 Category Test DataProduct");
        dp.setStatus(StatusType.PUBLISHED);

        LinkedEntity catLink = new LinkedEntity();
        catLink.setUid(futureCatUid);
        catLink.setEntityType(EntityNames.CATEGORY.name());
        dp.addCategory(catLink);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult);

        // Create the Category
        org.epos.eposdatamodel.Category cat = new org.epos.eposdatamodel.Category();
        cat.setUid(futureCatUid);
        cat.setName("Bug1 Test Category");
        cat.setInScheme(schemeResult);
        cat.setStatus(StatusType.PUBLISHED);

        LinkedEntity catResult = categoryAPI.create(cat, StatusType.PUBLISHED, null, null);
        assertNotNull(catResult, "Category should be created");

        // Critical: Retrieve should NOT return null
        org.epos.eposdatamodel.Category catRetrieved = categoryAPI.retrieve(catResult.getInstanceId());
        assertNotNull(catRetrieved,
                "BUG1 VERIFICATION: Category retrieve() should NOT return null");
        assertNotNull(catRetrieved.getUid(),
                "BUG1 VERIFICATION: Category.getUid() should NOT throw NullPointerException");
        assertEquals(StatusType.PUBLISHED, catRetrieved.getStatus(),
                "Category should have PUBLISHED status");

        System.out.println("✓ BUG1-2 PASSED: Category retrieval doesn't return null");
    }

    @Test
    @Order(3)
    @DisplayName("BUG1-3: Multiple entities referencing same future entity")
    void testBug1_MultipleReferencesToSameFuture() {
        String futureOrgUid = "organization:bug1multi:" + UUID.randomUUID();

        // Create multiple DataProducts all referencing the same non-existent Organization
        List<LinkedEntity> dpResults = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            DataProduct dp = new DataProduct();
            dp.setUid("dataproduct:bug1multi:" + i + ":" + UUID.randomUUID());
            dp.addTitle("Bug1 Multi Test " + i);
            dp.setStatus(StatusType.PUBLISHED);

            LinkedEntity orgLink = new LinkedEntity();
            orgLink.setUid(futureOrgUid);
            orgLink.setEntityType(EntityNames.ORGANIZATION.name());
            dp.addPublisher(orgLink);

            LinkedEntity result = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
            dpResults.add(result);
        }

        assertEquals(3, dpResults.size(), "All 3 DataProducts should be created");

        // Now create the Organization - should resolve for ALL DataProducts
        org.epos.eposdatamodel.Organization org = new org.epos.eposdatamodel.Organization();
        org.setUid(futureOrgUid);
        org.addLegalName("Bug1 Multi Test Organization");
        org.setStatus(StatusType.PUBLISHED);

        LinkedEntity orgResult = organizationAPI.create(org, StatusType.PUBLISHED, null, null);
        assertNotNull(orgResult, "Organization should be created");

        // Verify Organization has correct status
        org.epos.eposdatamodel.Organization orgRetrieved = organizationAPI.retrieve(orgResult.getInstanceId());
        assertEquals(StatusType.PUBLISHED, orgRetrieved.getStatus(),
                "Organization should have PUBLISHED status");

        // Verify all DataProducts have Organization linked
        for (int i = 0; i < dpResults.size(); i++) {
            DataProduct dpRetrieved = dataProductAPI.retrieve(dpResults.get(i).getInstanceId());
            assertNotNull(dpRetrieved.getPublisher(),
                    "DataProduct " + i + " should have publisher after resolution");
        }

        System.out.println("✓ BUG1-3 PASSED: Multiple references to same future entity resolved correctly");
    }

    // =========================================================================
    // BUG 2: Duplicate Key Violations Tests
    // =========================================================================

    @Test
    @Order(10)
    @DisplayName("BUG2-1: No duplicate key when resolving pending relations")
    void testBug2_NoDuplicateKeyOnPendingResolution() {
        String futureDistUid = "distribution:bug2test:" + UUID.randomUUID();

        // Create DataProduct referencing Distribution
        DataProduct dp1 = new DataProduct();
        dp1.setUid("dataproduct:bug2test:1:" + UUID.randomUUID());
        dp1.addTitle("Bug2 Test 1");
        dp1.setStatus(StatusType.PUBLISHED);

        LinkedEntity distLink = new LinkedEntity();
        distLink.setUid(futureDistUid);
        distLink.setEntityType(EntityNames.DISTRIBUTION.name());
        dp1.addDistribution(distLink);

        LinkedEntity dp1Result = dataProductAPI.create(dp1, StatusType.PUBLISHED, null, null);

        // Create Distribution - this triggers resolvePendingRelations
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid(futureDistUid);
        dist.addTitle("Bug2 Test Distribution");
        dist.setFormat("text/plain");
        dist.setStatus(StatusType.PUBLISHED);

        // This should NOT throw duplicate key exception
        assertDoesNotThrow(() -> {
            distributionAPI.create(dist, StatusType.PUBLISHED, null, null);
        }, "BUG2 VERIFICATION: Creating Distribution should not cause duplicate key error");

        System.out.println("✓ BUG2-1 PASSED: No duplicate key on pending relation resolution");
    }

    @Test
    @Order(11)
    @DisplayName("BUG2-2: Multiple updates don't cause duplicate join records")
    void testBug2_MultipleUpdatesDontDuplicate() {
        // Create ContactPoint
        ContactPoint cp = new ContactPoint();
        cp.setUid("contactpoint:bug2update:" + UUID.randomUUID());
        cp.setRole("Bug2 Test Contact");
        cp.addEmail("bug2@test.com");
        cp.setStatus(StatusType.PUBLISHED);
        LinkedEntity cpResult = contactPointAPI.create(cp, StatusType.PUBLISHED, null, null);

        // Create WebService with ContactPoint
        WebService ws = new WebService();
        ws.setUid("webservice:bug2update:" + UUID.randomUUID());
        ws.setName("Bug2 WebService Test");
        ws.setStatus(StatusType.PUBLISHED);
        ws.addContactPoint(cpResult);

        LinkedEntity wsResult = webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);

        // Update WebService multiple times with the same ContactPoint
        for (int i = 0; i < 3; i++) {
            ws.setInstanceId(wsResult.getInstanceId());
            ws.setMetaId(wsResult.getMetaId());
            ws.setUid(wsResult.getUid());
            ws.setName("Bug2 WebService Test - Update " + i);

            assertDoesNotThrow(() -> {
                webServiceAPI.create(ws, StatusType.PUBLISHED, null, null);
            }, "Update " + i + " should not cause duplicate key error");
        }

        System.out.println("✓ BUG2-2 PASSED: Multiple updates don't create duplicate join records");
    }

    @Test
    @Order(12)
    @DisplayName("BUG2-3: Concurrent deferred resolutions don't conflict")
    void testBug2_ConcurrentDeferredResolutions() throws Exception {
        String futurePersonUid = "person:bug2concurrent:" + UUID.randomUUID();

        // Create multiple ContactPoints referencing same non-existent Person
        List<LinkedEntity> cpResults = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            ContactPoint cp = new ContactPoint();
            cp.setUid("contactpoint:bug2concurrent:" + i + ":" + UUID.randomUUID());
            cp.setRole("Concurrent Test " + i);
            cp.addEmail("concurrent" + i + "@test.com");
            cp.setStatus(StatusType.PUBLISHED);

            LinkedEntity personLink = new LinkedEntity();
            personLink.setUid(futurePersonUid);
            personLink.setEntityType(EntityNames.PERSON.name());
            cp.setPerson(personLink);

            LinkedEntity result = contactPointAPI.create(cp, StatusType.PUBLISHED, null, null);
            cpResults.add(result);
        }

        // Create Person - should resolve all pending relations without duplicate key errors
        org.epos.eposdatamodel.Person person = new org.epos.eposdatamodel.Person();
        person.setUid(futurePersonUid);
        person.setFamilyName("Bug2");
        person.setGivenName("ConcurrentTest");
        person.setStatus(StatusType.PUBLISHED);

        assertDoesNotThrow(() -> {
            personAPI.create(person, StatusType.PUBLISHED, null, null);
        }, "BUG2 VERIFICATION: Person creation should not cause duplicate key errors");

        System.out.println("✓ BUG2-3 PASSED: Concurrent deferred resolutions don't conflict");
    }

    // =========================================================================
    // BUG 3: CategoryAPI.findOrCreateStub() Tests
    // =========================================================================

    @Test
    @Order(20)
    @DisplayName("BUG3-1: Category stub has valid Versioningstatus")
    void testBug3_CategoryStubHasValidVersioningstatus() {
        // Create scheme
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:bug3test:" + UUID.randomUUID());
        scheme.setTitle("Bug3 Test Scheme");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        // Create parent Category
        org.epos.eposdatamodel.Category parent = new org.epos.eposdatamodel.Category();
        parent.setUid("category:bug3parent:" + UUID.randomUUID());
        parent.setName("Bug3 Parent Category");
        parent.setInScheme(schemeResult);
        parent.setStatus(StatusType.PUBLISHED);
        LinkedEntity parentResult = categoryAPI.create(parent, StatusType.PUBLISHED, null, null);

        // Create child Category with reference to non-existent broader
        // This will trigger findOrCreateStub()
        String futureBroaderUid = "category:bug3broader:" + UUID.randomUUID();

        LinkedEntity broaderLink = new LinkedEntity();
        broaderLink.setUid(futureBroaderUid);
        broaderLink.setEntityType(EntityNames.CATEGORY.name());

        org.epos.eposdatamodel.Category child = new org.epos.eposdatamodel.Category();
        child.setUid("category:bug3child:" + UUID.randomUUID());
        child.setName("Bug3 Child Category");
        child.setInScheme(schemeResult);
        child.addBroader(broaderLink);
        child.setStatus(StatusType.PUBLISHED);

        LinkedEntity childResult = categoryAPI.create(child, StatusType.PUBLISHED, null, null);
        assertNotNull(childResult, "Child category should be created");

        // The stub broader should have been created with valid Versioningstatus
        // Now let's retrieve by UID
        org.epos.eposdatamodel.Category stubRetrieved = categoryAPI.retrieveByUID(futureBroaderUid);

        // The stub should exist and be retrievable
        assertNotNull(stubRetrieved,
                "BUG3 VERIFICATION: Category stub should be retrievable (has valid Versioningstatus)");

        System.out.println("✓ BUG3-1 PASSED: Category stub has valid Versioningstatus");
    }

    @Test
    @Order(21)
    @DisplayName("BUG3-2: Category narrower creates valid stubs")
    void testBug3_CategoryNarrowerCreatesValidStubs() {
        // Create scheme
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:bug3narrower:" + UUID.randomUUID());
        scheme.setTitle("Bug3 Narrower Test Scheme");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        String futureNarrowerUid = "category:bug3narrower:" + UUID.randomUUID();

        // Create parent with reference to non-existent narrower
        LinkedEntity narrowerLink = new LinkedEntity();
        narrowerLink.setUid(futureNarrowerUid);
        narrowerLink.setEntityType(EntityNames.CATEGORY.name());

        org.epos.eposdatamodel.Category parent = new org.epos.eposdatamodel.Category();
        parent.setUid("category:bug3narrowerparent:" + UUID.randomUUID());
        parent.setName("Bug3 Parent with Narrower");
        parent.setInScheme(schemeResult);
        parent.addNarrower(narrowerLink);
        parent.setStatus(StatusType.PUBLISHED);

        LinkedEntity parentResult = categoryAPI.create(parent, StatusType.PUBLISHED, null, null);
        assertNotNull(parentResult);

        // The stub narrower should be retrievable
        org.epos.eposdatamodel.Category stubRetrieved = categoryAPI.retrieveByUID(futureNarrowerUid);
        assertNotNull(stubRetrieved,
                "BUG3 VERIFICATION: Category narrower stub should be retrievable");

        System.out.println("✓ BUG3-2 PASSED: Category narrower creates valid stubs");
    }

    // =========================================================================
    // INTEGRATION TEST: All bugs together
    // =========================================================================

    @Test
    @Order(100)
    @DisplayName("INTEGRATION: Full workflow with all bug scenarios")
    void testIntegration_FullWorkflowWithAllBugs() {
        System.out.println("\n=== INTEGRATION TEST: Full Workflow ===");

        String suffix = UUID.randomUUID().toString().substring(0, 8);

        // 1. Create CategoryScheme
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid("categoryscheme:integration:" + suffix);
        scheme.setTitle("Integration Test Scheme");
        scheme.setStatus(StatusType.PUBLISHED);
        LinkedEntity schemeResult = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);
        System.out.println("  Created CategoryScheme");

        // 2. Create DataProduct referencing non-existent Distribution, Category, Organization
        String futureDistUid = "distribution:integration:" + suffix;
        String futureCatUid = "category:integration:" + suffix;
        String futureOrgUid = "organization:integration:" + suffix;

        DataProduct dp = new DataProduct();
        dp.setUid("dataproduct:integration:" + suffix);
        dp.addTitle("Integration Test DataProduct");
        dp.addDescription("Full workflow test");
        dp.setStatus(StatusType.PUBLISHED);

        LinkedEntity distLink = new LinkedEntity();
        distLink.setUid(futureDistUid);
        distLink.setEntityType(EntityNames.DISTRIBUTION.name());
        dp.addDistribution(distLink);

        LinkedEntity catLink = new LinkedEntity();
        catLink.setUid(futureCatUid);
        catLink.setEntityType(EntityNames.CATEGORY.name());
        dp.addCategory(catLink);

        LinkedEntity orgLink = new LinkedEntity();
        orgLink.setUid(futureOrgUid);
        orgLink.setEntityType(EntityNames.ORGANIZATION.name());
        dp.addPublisher(orgLink);

        LinkedEntity dpResult = dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
        assertNotNull(dpResult, "DataProduct should be created with pending relations");
        System.out.println("  Created DataProduct with 3 pending relations");

        // 3. Create Distribution
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setUid(futureDistUid);
        dist.addTitle("Integration Test Distribution");
        dist.setFormat("application/json");
        dist.setStatus(StatusType.PUBLISHED);

        LinkedEntity distResult = assertDoesNotThrow(() ->
                        distributionAPI.create(dist, StatusType.PUBLISHED, null, null),
                "Distribution creation should not fail");
        System.out.println("  Created Distribution - pending relation resolved");

        // 4. Create Category
        org.epos.eposdatamodel.Category cat = new org.epos.eposdatamodel.Category();
        cat.setUid(futureCatUid);
        cat.setName("Integration Test Category");
        cat.setInScheme(schemeResult);
        cat.setStatus(StatusType.PUBLISHED);

        LinkedEntity catResult = assertDoesNotThrow(() ->
                        categoryAPI.create(cat, StatusType.PUBLISHED, null, null),
                "Category creation should not fail");
        System.out.println("  Created Category - pending relation resolved");

        // 5. Create Organization
        org.epos.eposdatamodel.Organization org = new org.epos.eposdatamodel.Organization();
        org.setUid(futureOrgUid);
        org.addLegalName("Integration Test Organization");
        org.setStatus(StatusType.PUBLISHED);

        LinkedEntity orgResult = assertDoesNotThrow(() ->
                        organizationAPI.create(org, StatusType.PUBLISHED, null, null),
                "Organization creation should not fail");
        System.out.println("  Created Organization - pending relation resolved");

        // 6. Verify all entities have correct status (BUG1 verification)
        org.epos.eposdatamodel.Distribution distRetrieved = distributionAPI.retrieve(distResult.getInstanceId());
        assertEquals(StatusType.PUBLISHED, distRetrieved.getStatus(), "Distribution status should be PUBLISHED");

        org.epos.eposdatamodel.Category catRetrieved = categoryAPI.retrieve(catResult.getInstanceId());
        assertNotNull(catRetrieved, "Category should be retrievable (BUG3 fix)");
        assertEquals(StatusType.PUBLISHED, catRetrieved.getStatus(), "Category status should be PUBLISHED");

        org.epos.eposdatamodel.Organization orgRetrieved = organizationAPI.retrieve(orgResult.getInstanceId());
        assertEquals(StatusType.PUBLISHED, orgRetrieved.getStatus(), "Organization status should be PUBLISHED");

        System.out.println("  Verified all entities have correct PUBLISHED status");

        // 7. Verify DataProduct has all relations resolved
        DataProduct dpRetrieved = dataProductAPI.retrieve(dpResult.getInstanceId());
        assertNotNull(dpRetrieved);

        assertNotNull(dpRetrieved.getDistribution(), "Distribution should be linked");
        assertFalse(dpRetrieved.getDistribution().isEmpty(), "Distribution list should not be empty");

        assertNotNull(dpRetrieved.getCategory(), "Category should be linked");
        assertFalse(dpRetrieved.getCategory().isEmpty(), "Category list should not be empty");

        assertNotNull(dpRetrieved.getPublisher(), "Publisher should be linked");
        assertFalse(dpRetrieved.getPublisher().isEmpty(), "Publisher list should not be empty");

        System.out.println("  Verified all relations resolved on DataProduct");

        // 8. Update DataProduct multiple times (BUG2 verification)
        for (int i = 0; i < 3; i++) {
            dp.setInstanceId(dpResult.getInstanceId());
            dp.setMetaId(dpResult.getMetaId());
            dp.setTitle(Arrays.asList("Integration Test DataProduct - Update " + i));

            assertDoesNotThrow(() -> {
                dataProductAPI.create(dp, StatusType.PUBLISHED, null, null);
            }, "Update " + i + " should not cause duplicate key errors");
        }
        System.out.println("  Multiple updates completed without duplicate key errors");

        System.out.println("✓ INTEGRATION TEST PASSED: Full workflow completed successfully");
    }
}