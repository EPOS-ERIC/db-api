package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive CRUD test for Category and CategoryScheme entities.
 * 
 * Tests all CRUD operations:
 * - CREATE: Basic creation, creation with relations
 * - READ: retrieve, retrieveByUID, retrieveAll
 * - UPDATE: Field updates, relation updates
 * - DELETE: Proper cleanup of relations
 * 
 * Also tests relationships:
 * - CategoryScheme topConcepts -> Category
 * - Category inScheme -> CategoryScheme
 * - Category broader/narrower hierarchy
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategoryCrudTest extends TestcontainersLifecycle {

    // ==================== CATEGORYSCHEME CRUD TESTS ====================

    @Test
    @Order(1)
    @DisplayName("CategoryScheme: CREATE - Basic creation")
    void testCategorySchemeCreate() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        CategoryScheme scheme = new CategoryScheme();
        scheme.setInstanceId(UUID.randomUUID().toString());
        scheme.setMetaId(UUID.randomUUID().toString());
        scheme.setUid("urn:epos:scheme:crud-test-" + UUID.randomUUID());
        scheme.setTitle("CRUD Test Scheme");
        scheme.setDescription("A scheme for CRUD testing");
        scheme.setCode("CTS");
        scheme.setHomepage("https://example.org/scheme");
        scheme.setLogo("https://example.org/logo.png");
        scheme.setColor("#FF5733");
        scheme.setOrderitemnumber("1");

        LinkedEntity result = api.create(scheme, null, null, null);

        assertNotNull(result, "Create should return a LinkedEntity");
        assertNotNull(result.getInstanceId(), "InstanceId should not be null");
        assertEquals(scheme.getUid(), result.getUid(), "UID should match");

        // Verify by retrieval
        CategoryScheme retrieved = (CategoryScheme) api.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertEquals("CRUD Test Scheme", retrieved.getTitle());
        assertEquals("A scheme for CRUD testing", retrieved.getDescription());
        assertEquals("CTS", retrieved.getCode());
        assertEquals("#FF5733", retrieved.getColor());
        assertEquals(StatusType.DRAFT, retrieved.getStatus());

        LOG.info("Created CategoryScheme: " + retrieved.getUid());
    }

    @Test
    @Order(2)
    @DisplayName("CategoryScheme: READ - retrieveByUID")
    void testCategorySchemeRetrieveByUID() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        // Create a scheme first
        String uid = "urn:epos:scheme:retrieve-test-" + UUID.randomUUID();
        CategoryScheme scheme = new CategoryScheme();
        scheme.setInstanceId(UUID.randomUUID().toString());
        scheme.setMetaId(UUID.randomUUID().toString());
        scheme.setUid(uid);
        scheme.setTitle("Retrieve Test Scheme");

        api.create(scheme, null, null, null);

        // Retrieve by UID
        CategoryScheme retrieved = (CategoryScheme) api.retrieveByUID(uid);
        assertNotNull(retrieved, "Should find scheme by UID");
        assertEquals(uid, retrieved.getUid());
        assertEquals("Retrieve Test Scheme", retrieved.getTitle());

        LOG.info("Retrieved CategoryScheme by UID: " + retrieved.getUid());
    }

    @Test
    @Order(3)
    @DisplayName("CategoryScheme: READ - retrieveAll")
    void testCategorySchemeRetrieveAll() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        // Create multiple schemes
        for (int i = 0; i < 3; i++) {
            CategoryScheme scheme = new CategoryScheme();
            scheme.setInstanceId(UUID.randomUUID().toString());
            scheme.setMetaId(UUID.randomUUID().toString());
            scheme.setUid("urn:epos:scheme:all-test-" + i + "-" + UUID.randomUUID());
            scheme.setTitle("All Test Scheme " + i);
            api.create(scheme, null, null, null);
        }

        List<?> all = api.retrieveAll();
        assertNotNull(all);
        assertTrue(all.size() >= 3, "Should have at least 3 schemes");

        LOG.info("Retrieved " + all.size() + " CategorySchemes");
    }

    @Test
    @Order(4)
    @DisplayName("CategoryScheme: UPDATE - Field update")
    void testCategorySchemeUpdate() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        // Create initial scheme
        String uid = "urn:epos:scheme:update-test-" + UUID.randomUUID();
        CategoryScheme scheme = new CategoryScheme();
        scheme.setInstanceId(UUID.randomUUID().toString());
        scheme.setMetaId(UUID.randomUUID().toString());
        scheme.setUid(uid);
        scheme.setTitle("Original Title");
        scheme.setDescription("Original Description");

        LinkedEntity result = api.create(scheme, null, null, null);
        String instanceId = result.getInstanceId();

        // Verify original
        CategoryScheme original = (CategoryScheme) api.retrieve(instanceId);
        assertEquals("Original Title", original.getTitle());

        // Update
        scheme.setInstanceId(instanceId);
        scheme.setTitle("Updated Title");
        scheme.setDescription("Updated Description");
        scheme.setCode("UPD");

        api.create(scheme, null, null, null);

        // Verify update
        CategoryScheme updated = (CategoryScheme) api.retrieve(instanceId);
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Description", updated.getDescription());
        assertEquals("UPD", updated.getCode());

        LOG.info("Updated CategoryScheme: " + updated.getUid());
    }

    // ==================== CATEGORY CRUD TESTS ====================

    @Test
    @Order(10)
    @DisplayName("Category: CREATE - Basic creation")
    void testCategoryCreate() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid("urn:epos:category:crud-test-" + UUID.randomUUID());
        category.setName("CRUD Test Category");
        category.setDescription("A category for CRUD testing");

        LinkedEntity result = api.create(category, null, null, null);

        assertNotNull(result, "Create should return a LinkedEntity");
        assertNotNull(result.getInstanceId(), "InstanceId should not be null");
        assertEquals(category.getUid(), result.getUid(), "UID should match");

        // Verify by retrieval
        Category retrieved = (Category) api.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertEquals("CRUD Test Category", retrieved.getName());
        assertEquals("A category for CRUD testing", retrieved.getDescription());
        assertEquals(StatusType.DRAFT, retrieved.getStatus());

        LOG.info("Created Category: " + retrieved.getUid());
    }

    @Test
    @Order(11)
    @DisplayName("Category: CREATE - With inScheme relation")
    void testCategoryCreateWithInScheme() {
        AbstractAPI schemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create a scheme first
        CategoryScheme scheme = new CategoryScheme();
        scheme.setInstanceId(UUID.randomUUID().toString());
        scheme.setMetaId(UUID.randomUUID().toString());
        scheme.setUid("urn:epos:scheme:for-category-" + UUID.randomUUID());
        scheme.setTitle("Scheme for Category");

        LinkedEntity schemeLE = schemeApi.create(scheme, null, null, null);

        // Create category with inScheme
        LinkedEntity schemeLink = new LinkedEntity();
        schemeLink.setInstanceId(schemeLE.getInstanceId());
        schemeLink.setMetaId(schemeLE.getMetaId());
        schemeLink.setUid(schemeLE.getUid());
        schemeLink.setEntityType(EntityNames.CATEGORYSCHEME.name());

        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid("urn:epos:category:with-scheme-" + UUID.randomUUID());
        category.setName("Category with Scheme");
        category.setInScheme(schemeLink);

        LinkedEntity result = categoryApi.create(category, null, null, null);

        // Verify
        Category retrieved = (Category) categoryApi.retrieve(result.getInstanceId());
        assertNotNull(retrieved);
        assertNotNull(retrieved.getInScheme(), "inScheme should be set");
        assertEquals(schemeLE.getInstanceId(), retrieved.getInScheme().getInstanceId());

        LOG.info("Created Category with inScheme: " + retrieved.getUid() + " -> " + retrieved.getInScheme().getUid());
    }

    @Test
    @Order(12)
    @DisplayName("Category: READ - retrieveByUID")
    void testCategoryRetrieveByUID() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create a category first
        String uid = "urn:epos:category:retrieve-test-" + UUID.randomUUID();
        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid(uid);
        category.setName("Retrieve Test Category");

        api.create(category, null, null, null);

        // Retrieve by UID
        Category retrieved = (Category) api.retrieveByUID(uid);
        assertNotNull(retrieved, "Should find category by UID");
        assertEquals(uid, retrieved.getUid());
        assertEquals("Retrieve Test Category", retrieved.getName());

        LOG.info("Retrieved Category by UID: " + retrieved.getUid());
    }

    @Test
    @Order(13)
    @DisplayName("Category: READ - retrieveAll")
    void testCategoryRetrieveAll() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create multiple categories
        for (int i = 0; i < 3; i++) {
            Category category = new Category();
            category.setInstanceId(UUID.randomUUID().toString());
            category.setMetaId(UUID.randomUUID().toString());
            category.setUid("urn:epos:category:all-test-" + i + "-" + UUID.randomUUID());
            category.setName("All Test Category " + i);
            api.create(category, null, null, null);
        }

        List<?> all = api.retrieveAll();
        assertNotNull(all);
        assertTrue(all.size() >= 3, "Should have at least 3 categories");

        LOG.info("Retrieved " + all.size() + " Categories");
    }

    @Test
    @Order(14)
    @DisplayName("Category: UPDATE - Field update")
    void testCategoryUpdate() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create initial category
        String uid = "urn:epos:category:update-test-" + UUID.randomUUID();
        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid(uid);
        category.setName("Original Name");
        category.setDescription("Original Description");

        LinkedEntity result = api.create(category, null, null, null);
        String instanceId = result.getInstanceId();

        // Verify original
        Category original = (Category) api.retrieve(instanceId);
        assertEquals("Original Name", original.getName());

        // Update
        category.setInstanceId(instanceId);
        category.setName("Updated Name");
        category.setDescription("Updated Description");

        api.create(category, null, null, null);

        // Verify update
        Category updated = (Category) api.retrieve(instanceId);
        assertEquals("Updated Name", updated.getName());
        assertEquals("Updated Description", updated.getDescription());

        LOG.info("Updated Category: " + updated.getUid());
    }

    // ==================== HIERARCHY TESTS ====================

    @Test
    @Order(20)
    @DisplayName("Category: CREATE - Broader/Narrower hierarchy")
    void testCategoryHierarchy() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create parent category
        Category parent = new Category();
        parent.setInstanceId(UUID.randomUUID().toString());
        parent.setMetaId(UUID.randomUUID().toString());
        parent.setUid("urn:epos:category:parent-" + UUID.randomUUID());
        parent.setName("Parent Category");

        LinkedEntity parentLE = api.create(parent, null, null, null);

        // Create child category with broader pointing to parent
        LinkedEntity broaderLink = new LinkedEntity();
        broaderLink.setInstanceId(parentLE.getInstanceId());
        broaderLink.setMetaId(parentLE.getMetaId());
        broaderLink.setUid(parentLE.getUid());
        broaderLink.setEntityType(EntityNames.CATEGORY.name());

        Category child = new Category();
        child.setInstanceId(UUID.randomUUID().toString());
        child.setMetaId(UUID.randomUUID().toString());
        child.setUid("urn:epos:category:child-" + UUID.randomUUID());
        child.setName("Child Category");
        child.addBroader(broaderLink);

        LinkedEntity childLE = api.create(child, null, null, null);

        // Verify child has broader
        Category retrievedChild = (Category) api.retrieve(childLE.getInstanceId());
        assertNotNull(retrievedChild.getBroader(), "Child should have broader list");
        assertFalse(retrievedChild.getBroader().isEmpty(), "Child should have at least 1 broader");
        assertEquals(parentLE.getInstanceId(), retrievedChild.getBroader().get(0).getInstanceId());

        // Verify parent has narrower
        Category retrievedParent = (Category) api.retrieve(parentLE.getInstanceId());
        assertNotNull(retrievedParent.getNarrower(), "Parent should have narrower list");
        assertFalse(retrievedParent.getNarrower().isEmpty(), "Parent should have at least 1 narrower");
        assertEquals(childLE.getInstanceId(), retrievedParent.getNarrower().get(0).getInstanceId());

        LOG.info("Created hierarchy: " + retrievedParent.getName() + " -> " + retrievedChild.getName());
    }

    @Test
    @Order(21)
    @DisplayName("Category: CREATE - Multi-level hierarchy")
    void testCategoryMultiLevelHierarchy() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create root
        Category root = new Category();
        root.setInstanceId(UUID.randomUUID().toString());
        root.setMetaId(UUID.randomUUID().toString());
        root.setUid("urn:epos:category:root-" + UUID.randomUUID());
        root.setName("Root");
        LinkedEntity rootLE = api.create(root, null, null, null);

        // Create level 1
        Category level1 = new Category();
        level1.setInstanceId(UUID.randomUUID().toString());
        level1.setMetaId(UUID.randomUUID().toString());
        level1.setUid("urn:epos:category:level1-" + UUID.randomUUID());
        level1.setName("Level 1");
        level1.addBroader(createLinkedEntity(rootLE));
        LinkedEntity level1LE = api.create(level1, null, null, null);

        // Create level 2
        Category level2 = new Category();
        level2.setInstanceId(UUID.randomUUID().toString());
        level2.setMetaId(UUID.randomUUID().toString());
        level2.setUid("urn:epos:category:level2-" + UUID.randomUUID());
        level2.setName("Level 2");
        level2.addBroader(createLinkedEntity(level1LE));
        LinkedEntity level2LE = api.create(level2, null, null, null);

        // Verify the chain
        Category retrievedLevel2 = (Category) api.retrieve(level2LE.getInstanceId());
        assertNotNull(retrievedLevel2.getBroader());
        assertEquals(level1LE.getInstanceId(), retrievedLevel2.getBroader().get(0).getInstanceId());

        Category retrievedLevel1 = (Category) api.retrieve(level1LE.getInstanceId());
        assertNotNull(retrievedLevel1.getBroader());
        assertEquals(rootLE.getInstanceId(), retrievedLevel1.getBroader().get(0).getInstanceId());
        assertNotNull(retrievedLevel1.getNarrower());
        assertEquals(level2LE.getInstanceId(), retrievedLevel1.getNarrower().get(0).getInstanceId());

        Category retrievedRoot = (Category) api.retrieve(rootLE.getInstanceId());
        assertNotNull(retrievedRoot.getNarrower());
        assertEquals(level1LE.getInstanceId(), retrievedRoot.getNarrower().get(0).getInstanceId());
        assertTrue(retrievedRoot.getBroader() == null || retrievedRoot.getBroader().isEmpty(), 
                "Root should have no broader");

        LOG.info("Created 3-level hierarchy: Root -> Level1 -> Level2");
    }

    // ==================== CATEGORYSCHEME WITH TOPCONCEPTS ====================

    @Test
    @Order(30)
    @DisplayName("CategoryScheme: CREATE - With topConcepts")
    void testCategorySchemeWithTopConcepts() {
        AbstractAPI schemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create categories first
        Category cat1 = new Category();
        cat1.setInstanceId(UUID.randomUUID().toString());
        cat1.setMetaId(UUID.randomUUID().toString());
        cat1.setUid("urn:epos:category:top1-" + UUID.randomUUID());
        cat1.setName("Top Concept 1");
        LinkedEntity cat1LE = categoryApi.create(cat1, null, null, null);

        Category cat2 = new Category();
        cat2.setInstanceId(UUID.randomUUID().toString());
        cat2.setMetaId(UUID.randomUUID().toString());
        cat2.setUid("urn:epos:category:top2-" + UUID.randomUUID());
        cat2.setName("Top Concept 2");
        LinkedEntity cat2LE = categoryApi.create(cat2, null, null, null);

        // Create scheme with topConcepts
        CategoryScheme scheme = new CategoryScheme();
        scheme.setInstanceId(UUID.randomUUID().toString());
        scheme.setMetaId(UUID.randomUUID().toString());
        scheme.setUid("urn:epos:scheme:with-tops-" + UUID.randomUUID());
        scheme.setTitle("Scheme with TopConcepts");
        scheme.addTopConcepts(createLinkedEntity(cat1LE));
        scheme.addTopConcepts(createLinkedEntity(cat2LE));

        LinkedEntity schemeLE = schemeApi.create(scheme, null, null, null);

        // Verify
        CategoryScheme retrieved = (CategoryScheme) schemeApi.retrieve(schemeLE.getInstanceId());
        assertNotNull(retrieved);
        assertNotNull(retrieved.getTopConcepts(), "TopConcepts should not be null");
        assertEquals(2, retrieved.getTopConcepts().size(), "Should have 2 topConcepts");

        LOG.info("Created CategoryScheme with " + retrieved.getTopConcepts().size() + " topConcepts");
    }

    @Test
    @Order(31)
    @DisplayName("CategoryScheme: UPDATE - Add topConcept")
    void testCategorySchemeAddTopConcept() {
        AbstractAPI schemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create scheme without topConcepts
        CategoryScheme scheme = new CategoryScheme();
        scheme.setInstanceId(UUID.randomUUID().toString());
        scheme.setMetaId(UUID.randomUUID().toString());
        scheme.setUid("urn:epos:scheme:add-top-" + UUID.randomUUID());
        scheme.setTitle("Scheme to Add TopConcepts");

        LinkedEntity schemeLE = schemeApi.create(scheme, null, null, null);

        // Verify no topConcepts initially
        CategoryScheme initial = (CategoryScheme) schemeApi.retrieve(schemeLE.getInstanceId());
        assertTrue(initial.getTopConcepts() == null || initial.getTopConcepts().isEmpty(), 
                "Should have no topConcepts initially");

        // Create a category
        Category cat = new Category();
        cat.setInstanceId(UUID.randomUUID().toString());
        cat.setMetaId(UUID.randomUUID().toString());
        cat.setUid("urn:epos:category:new-top-" + UUID.randomUUID());
        cat.setName("New Top Concept");
        LinkedEntity catLE = categoryApi.create(cat, null, null, null);

        // Update scheme with topConcept
        scheme.setInstanceId(schemeLE.getInstanceId());
        scheme.addTopConcepts(createLinkedEntity(catLE));
        schemeApi.create(scheme, null, null, null);

        // Verify topConcept was added
        CategoryScheme updated = (CategoryScheme) schemeApi.retrieve(schemeLE.getInstanceId());
        assertNotNull(updated.getTopConcepts());
        assertEquals(1, updated.getTopConcepts().size());

        LOG.info("Added topConcept to CategoryScheme");
    }

    // ==================== DELETE TESTS ====================

    @Test
    @Order(40)
    @DisplayName("Category: DELETE - Basic deletion")
    void testCategoryDelete() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create a category
        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid("urn:epos:category:delete-test-" + UUID.randomUUID());
        category.setName("Category to Delete");

        LinkedEntity result = api.create(category, null, null, null);
        String instanceId = result.getInstanceId();

        // Verify it exists
        Category retrieved = (Category) api.retrieve(instanceId);
        assertNotNull(retrieved, "Category should exist before deletion");

        // Delete
        Boolean deleted = api.delete(instanceId);
        assertTrue(deleted, "Delete should return true");

        // Verify it's gone
        Category afterDelete = (Category) api.retrieve(instanceId);
        assertNull(afterDelete, "Category should not exist after deletion");

        LOG.info("Deleted Category: " + category.getUid());
    }

    @Test
    @Order(41)
    @DisplayName("CategoryScheme: DELETE - Basic deletion")
    void testCategorySchemeDelete() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        // Create a scheme
        CategoryScheme scheme = new CategoryScheme();
        scheme.setInstanceId(UUID.randomUUID().toString());
        scheme.setMetaId(UUID.randomUUID().toString());
        scheme.setUid("urn:epos:scheme:delete-test-" + UUID.randomUUID());
        scheme.setTitle("Scheme to Delete");

        LinkedEntity result = api.create(scheme, null, null, null);
        String instanceId = result.getInstanceId();

        // Verify it exists
        CategoryScheme retrieved = (CategoryScheme) api.retrieve(instanceId);
        assertNotNull(retrieved, "Scheme should exist before deletion");

        // Delete
        Boolean deleted = api.delete(instanceId);
        assertTrue(deleted, "Delete should return true");

        // Verify it's gone
        CategoryScheme afterDelete = (CategoryScheme) api.retrieve(instanceId);
        assertNull(afterDelete, "Scheme should not exist after deletion");

        LOG.info("Deleted CategoryScheme: " + scheme.getUid());
    }

    // ==================== EDGE CASES ====================

    @Test
    @Order(50)
    @DisplayName("Category: Self-reference prevention")
    void testCategorySelfReferencePrevention() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create a category
        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid("urn:epos:category:self-ref-" + UUID.randomUUID());
        category.setName("Self Reference Test");

        LinkedEntity result = api.create(category, null, null, null);

        // Try to add self as broader
        LinkedEntity selfLink = createLinkedEntity(result);
        category.setInstanceId(result.getInstanceId());
        category.addBroader(selfLink);

        api.create(category, null, null, null);

        // Verify self-reference was filtered out
        Category retrieved = (Category) api.retrieve(result.getInstanceId());
        assertTrue(retrieved.getBroader() == null || retrieved.getBroader().isEmpty() ||
                !retrieved.getBroader().stream().anyMatch(b -> b.getInstanceId().equals(result.getInstanceId())),
                "Category should not have itself as broader");

        LOG.info("Self-reference prevention verified");
    }

    @Test
    @Order(51)
    @DisplayName("Category: READ - retrieveAllWithStatus")
    void testCategoryRetrieveAllWithStatus() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        // Create a DRAFT category
        Category draftCategory = new Category();
        draftCategory.setInstanceId(UUID.randomUUID().toString());
        draftCategory.setMetaId(UUID.randomUUID().toString());
        draftCategory.setUid("urn:epos:category:draft-status-" + UUID.randomUUID());
        draftCategory.setName("Draft Status Category");
        api.create(draftCategory, StatusType.DRAFT, null, null);

        // Create a PUBLISHED category
        Category publishedCategory = new Category();
        publishedCategory.setInstanceId(UUID.randomUUID().toString());
        publishedCategory.setMetaId(UUID.randomUUID().toString());
        publishedCategory.setUid("urn:epos:category:published-status-" + UUID.randomUUID());
        publishedCategory.setName("Published Status Category");
        api.create(publishedCategory, StatusType.PUBLISHED, null, null);

        // Retrieve only DRAFT
        List<?> drafts = api.retrieveAllWithStatus(StatusType.DRAFT);
        assertNotNull(drafts);
        assertTrue(drafts.size() >= 1, "Should have at least 1 DRAFT category");

        // Retrieve only PUBLISHED
        List<?> published = api.retrieveAllWithStatus(StatusType.PUBLISHED);
        assertNotNull(published);
        assertTrue(published.size() >= 1, "Should have at least 1 PUBLISHED category");

        LOG.info("Retrieved " + drafts.size() + " DRAFT and " + published.size() + " PUBLISHED categories");
    }

    // ==================== SUMMARY TEST ====================

    @Test
    @Order(99)
    @DisplayName("SUMMARY: Verify all entities created")
    void testSummary() {
        AbstractAPI schemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        AbstractAPI categoryApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        List<?> allSchemes = schemeApi.retrieveAll();
        List<?> allCategories = categoryApi.retrieveAll();

        LOG.info("========================================");
        LOG.info("  CATEGORY CRUD TEST SUMMARY");
        LOG.info("========================================");
        LOG.info("  Total CategorySchemes: " + allSchemes.size());
        LOG.info("  Total Categories: " + allCategories.size());
        LOG.info("========================================");

        assertTrue(allSchemes.size() >= 5, "Should have at least 5 CategorySchemes");
        assertTrue(allCategories.size() >= 10, "Should have at least 10 Categories");
    }

    // ==================== HELPER METHODS ====================

    private LinkedEntity createLinkedEntity(LinkedEntity source) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(source.getInstanceId());
        le.setMetaId(source.getMetaId());
        le.setUid(source.getUid());
        le.setEntityType(EntityNames.CATEGORY.name());
        return le;
    }
}
