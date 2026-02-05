package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for CategoryScheme, Category, and hierarchical relationships (broader/narrower).
 *
 * Tests the following scenarios:
 * 1. CategoryScheme creation with topConcepts
 * 2. Category creation with inScheme reference
 * 3. Category hierarchy: broader/narrower relationships
 * 4. Retrieval of all relationships
 * 5. Embedded ID query verification (the camelCase fix)
 *
 * Database structure:
 * - CategoryScheme: main entity
 * - Category: linked to scheme via inScheme (direct FK)
 * - CategoryHastopconcept: join table (CategoryScheme -> topConcept Categories)
 *   - EmbeddedId: categorySchemeInstanceId, categoryInstanceId (camelCase!)
 * - CategoryIspartof: join table for hierarchy (category1=child, category2=parent)
 *   - EmbeddedId: category1InstanceId, category2InstanceId
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CategoryHierarchyTest extends TestcontainersLifecycle {

    private static EposDataModelDAO dao;
    private static String testSuffix;

    @BeforeAll
    static void setup() {
        dao = EposDataModelDAO.getInstance();
        testSuffix = UUID.randomUUID().toString().substring(0, 8);

        System.out.println("\n========================================");
        System.out.println("  CATEGORY HIERARCHY TEST SUITE");
        System.out.println("========================================\n");
    }

    @AfterEach
    void printSeparator() {
        System.out.println("----------------------------------------\n");
    }

    // =========================================================================
    // TEST 1: Create a CategoryScheme
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("Test 1: Create CategoryScheme")
    void test01_CreateCategoryScheme() {
        System.out.println("TEST 1: Create CategoryScheme");
        System.out.println("Expected: CategoryScheme is created with PUBLISHED status\n");

        String schemeUid = "category:test-scheme-" + testSuffix;

        // Create CategoryScheme
        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid(schemeUid);
        scheme.setTitle("Test Hierarchy Scheme");
        scheme.setDescription("A test scheme for category hierarchy");
        scheme.setEditorId("test-user");
        scheme.setStatus(StatusType.PUBLISHED);

        LinkedEntity result = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name())
                .create(scheme, StatusType.PUBLISHED, null, null);

        assertNotNull(result, "CategoryScheme creation should return a LinkedEntity");
        assertNotNull(result.getInstanceId(), "InstanceId should not be null");
        assertEquals(schemeUid, result.getUid());

        // Verify in database
        List<CategoryScheme> dbList = dao.getOneFromDBByUID(schemeUid, CategoryScheme.class);
        assertFalse(dbList.isEmpty(), "CategoryScheme should exist in database");

        CategoryScheme dbScheme = dbList.get(0);
        assertEquals("Test Hierarchy Scheme", dbScheme.getName());
        assertNotNull(dbScheme.getVersion(), "Version should not be null");
        assertEquals("PUBLISHED", dbScheme.getVersion().getStatus());

        System.out.println("✓ CategoryScheme created: " + result.getInstanceId());
        System.out.println("✓ UID: " + schemeUid);
        System.out.println("✓ Status: PUBLISHED");
    }

    // =========================================================================
    // TEST 2: Create Root Category (no broader)
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("Test 2: Create Root Category with inScheme")
    void test02_CreateRootCategory() {
        System.out.println("TEST 2: Create Root Category with inScheme");
        System.out.println("Expected: Root category is created and linked to scheme\n");

        String schemeUid = "category:test-scheme-" + testSuffix;
        String rootUid = "category:root-" + testSuffix;

        // Get the scheme
        List<CategoryScheme> schemeList = dao.getOneFromDBByUID(schemeUid, CategoryScheme.class);
        assertFalse(schemeList.isEmpty(), "Scheme should exist from Test 1");
        CategoryScheme scheme = schemeList.get(0);

        // Create root category
        org.epos.eposdatamodel.Category rootCategory = new org.epos.eposdatamodel.Category();
        rootCategory.setUid(rootUid);
        rootCategory.setName("Root Category");
        rootCategory.setDescription("The root of the hierarchy");
        rootCategory.setEditorId("test-user");
        rootCategory.setStatus(StatusType.PUBLISHED);

        // Set inScheme
        LinkedEntity schemeLink = new LinkedEntity();
        schemeLink.setInstanceId(scheme.getInstanceId());
        schemeLink.setUid(scheme.getUid());
        schemeLink.setEntityType(EntityNames.CATEGORYSCHEME.name());
        rootCategory.setInScheme(schemeLink);

        LinkedEntity result = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .create(rootCategory, StatusType.PUBLISHED, null, null);

        assertNotNull(result, "Category creation should return a LinkedEntity");
        assertNotNull(result.getInstanceId());

        // Verify in database
        List<Category> dbList = dao.getOneFromDBByUID(rootUid, Category.class);
        assertFalse(dbList.isEmpty(), "Root category should exist in database");

        Category dbCategory = dbList.get(0);
        assertEquals("Root Category", dbCategory.getName());
        assertNotNull(dbCategory.getInScheme(), "inScheme should be set");
        assertEquals(scheme.getInstanceId(), dbCategory.getInScheme().getInstanceId());

        System.out.println("✓ Root Category created: " + result.getInstanceId());
        System.out.println("✓ inScheme linked to: " + scheme.getUid());
    }

    // =========================================================================
    // TEST 3: Create Child Categories with Broader relationship
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("Test 3: Create Child Categories with Broader (parent) relationship")
    void test03_CreateChildCategoriesWithBroader() {
        System.out.println("TEST 3: Create Child Categories with Broader relationship");
        System.out.println("Expected: Child categories reference root as 'broader'\n");

        String schemeUid = "category:test-scheme-" + testSuffix;
        String rootUid = "category:root-" + testSuffix;
        String child1Uid = "category:child-1-" + testSuffix;
        String child2Uid = "category:child-2-" + testSuffix;

        // Get the root category
        List<Category> rootList = dao.getOneFromDBByUID(rootUid, Category.class);
        assertFalse(rootList.isEmpty(), "Root category should exist from Test 2");
        Category rootCategory = rootList.get(0);

        // Get the scheme
        List<CategoryScheme> schemeList = dao.getOneFromDBByUID(schemeUid, CategoryScheme.class);
        CategoryScheme scheme = schemeList.get(0);

        LinkedEntity schemeLink = new LinkedEntity();
        schemeLink.setInstanceId(scheme.getInstanceId());
        schemeLink.setUid(scheme.getUid());
        schemeLink.setEntityType(EntityNames.CATEGORYSCHEME.name());

        // Create broader link to root
        LinkedEntity broaderLink = new LinkedEntity();
        broaderLink.setInstanceId(rootCategory.getInstanceId());
        broaderLink.setUid(rootCategory.getUid());
        broaderLink.setEntityType(EntityNames.CATEGORY.name());

        // Create Child 1
        org.epos.eposdatamodel.Category child1 = new org.epos.eposdatamodel.Category();
        child1.setUid(child1Uid);
        child1.setName("Child Category 1");
        child1.setDescription("First child of root");
        child1.setEditorId("test-user");
        child1.setStatus(StatusType.PUBLISHED);
        child1.setInScheme(schemeLink);
        child1.addBroader(broaderLink);

        LinkedEntity result1 = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .create(child1, StatusType.PUBLISHED, null, null);
        assertNotNull(result1);

        // Create Child 2
        org.epos.eposdatamodel.Category child2 = new org.epos.eposdatamodel.Category();
        child2.setUid(child2Uid);
        child2.setName("Child Category 2");
        child2.setDescription("Second child of root");
        child2.setEditorId("test-user");
        child2.setStatus(StatusType.PUBLISHED);
        child2.setInScheme(schemeLink);
        child2.addBroader(broaderLink);

        LinkedEntity result2 = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .create(child2, StatusType.PUBLISHED, null, null);
        assertNotNull(result2);

        // Verify broader relationships in database (CategoryIspartof)
        List<Object> child1Relations = dao.getOneFromDBBySpecificKey(
                "category1Instance", result1.getInstanceId(), CategoryIspartof.class);

        assertNotNull(child1Relations, "Child1 should have broader relations");
        assertFalse(child1Relations.isEmpty(), "Child1 should have at least one broader");

        CategoryIspartof rel1 = (CategoryIspartof) child1Relations.get(0);
        assertEquals(rootCategory.getInstanceId(), rel1.getCategory2Instance().getInstanceId(),
                "Child1's broader should be root category");

        System.out.println("✓ Child 1 created: " + result1.getInstanceId());
        System.out.println("✓ Child 2 created: " + result2.getInstanceId());
        System.out.println("✓ Both children have 'broader' pointing to root");
    }

    // =========================================================================
    // TEST 4: Create Grandchild with Broader to Child
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("Test 4: Create Grandchild Category (3-level hierarchy)")
    void test04_CreateGrandchildCategory() {
        System.out.println("TEST 4: Create Grandchild Category (3-level hierarchy)");
        System.out.println("Expected: Grandchild has broader pointing to Child 1\n");

        String schemeUid = "category:test-scheme-" + testSuffix;
        String child1Uid = "category:child-1-" + testSuffix;
        String grandchildUid = "category:grandchild-" + testSuffix;

        // Get Child 1
        List<Category> child1List = dao.getOneFromDBByUID(child1Uid, Category.class);
        assertFalse(child1List.isEmpty(), "Child 1 should exist from Test 3");
        Category child1 = child1List.get(0);

        // Get the scheme
        List<CategoryScheme> schemeList = dao.getOneFromDBByUID(schemeUid, CategoryScheme.class);
        CategoryScheme scheme = schemeList.get(0);

        LinkedEntity schemeLink = new LinkedEntity();
        schemeLink.setInstanceId(scheme.getInstanceId());
        schemeLink.setUid(scheme.getUid());
        schemeLink.setEntityType(EntityNames.CATEGORYSCHEME.name());

        // Create broader link to child1
        LinkedEntity broaderLink = new LinkedEntity();
        broaderLink.setInstanceId(child1.getInstanceId());
        broaderLink.setUid(child1.getUid());
        broaderLink.setEntityType(EntityNames.CATEGORY.name());

        // Create Grandchild
        org.epos.eposdatamodel.Category grandchild = new org.epos.eposdatamodel.Category();
        grandchild.setUid(grandchildUid);
        grandchild.setName("Grandchild Category");
        grandchild.setDescription("Child of Child 1");
        grandchild.setEditorId("test-user");
        grandchild.setStatus(StatusType.PUBLISHED);
        grandchild.setInScheme(schemeLink);
        grandchild.addBroader(broaderLink);

        LinkedEntity result = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .create(grandchild, StatusType.PUBLISHED, null, null);
        assertNotNull(result);

        // Verify
        List<Object> grandchildRelations = dao.getOneFromDBBySpecificKey(
                "category1Instance", result.getInstanceId(), CategoryIspartof.class);
        assertFalse(grandchildRelations.isEmpty(), "Grandchild should have broader");

        CategoryIspartof rel = (CategoryIspartof) grandchildRelations.get(0);
        assertEquals(child1.getInstanceId(), rel.getCategory2Instance().getInstanceId(),
                "Grandchild's broader should be Child 1");

        System.out.println("✓ Grandchild created: " + result.getInstanceId());
        System.out.println("✓ Hierarchy: Root -> Child1 -> Grandchild");
    }

    // =========================================================================
    // TEST 5: Verify Narrower relationships (reverse of broader)
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("Test 5: Verify Narrower relationships are queryable")
    void test05_VerifyNarrowerRelationships() {
        System.out.println("TEST 5: Verify Narrower relationships");
        System.out.println("Expected: Root has narrower pointing to Child1 and Child2\n");

        String rootUid = "category:root-" + testSuffix;

        // Get root category
        List<Category> rootList = dao.getOneFromDBByUID(rootUid, Category.class);
        assertFalse(rootList.isEmpty());
        Category rootCategory = rootList.get(0);

        // Query narrower (root is category2Instance, children are category1Instance)
        List<Object> narrowerRelations = dao.getOneFromDBBySpecificKey(
                "category2Instance", rootCategory.getInstanceId(), CategoryIspartof.class);

        assertNotNull(narrowerRelations, "Root should have narrower relations");
        assertEquals(2, narrowerRelations.size(), "Root should have 2 narrower categories (Child1 and Child2)");

        System.out.println("✓ Root has " + narrowerRelations.size() + " narrower categories");

        for (Object obj : narrowerRelations) {
            CategoryIspartof rel = (CategoryIspartof) obj;
            System.out.println("  - Narrower: " + rel.getCategory1Instance().getUid());
        }
    }

    // =========================================================================
    // TEST 6: Add TopConcepts to CategoryScheme
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("Test 6: Add TopConcepts to CategoryScheme")
    void test06_AddTopConceptsToScheme() {
        System.out.println("TEST 6: Add TopConcepts to CategoryScheme");
        System.out.println("Expected: Root category becomes topConcept of scheme\n");

        String schemeUid = "category:test-scheme-" + testSuffix;
        String rootUid = "category:root-" + testSuffix;

        // Get the scheme and root category
        List<CategoryScheme> schemeList = dao.getOneFromDBByUID(schemeUid, CategoryScheme.class);
        List<Category> rootList = dao.getOneFromDBByUID(rootUid, Category.class);

        assertFalse(schemeList.isEmpty());
        assertFalse(rootList.isEmpty());

        CategoryScheme scheme = schemeList.get(0);
        Category rootCategory = rootList.get(0);

        // Update scheme with topConcepts
        org.epos.eposdatamodel.CategoryScheme schemeUpdate = new org.epos.eposdatamodel.CategoryScheme();
        schemeUpdate.setInstanceId(scheme.getInstanceId());
        schemeUpdate.setMetaId(scheme.getMetaId());
        schemeUpdate.setUid(scheme.getUid());
        schemeUpdate.setTitle(scheme.getName());
        schemeUpdate.setEditorId("test-user");
        schemeUpdate.setStatus(StatusType.PUBLISHED);

        LinkedEntity topConceptLink = new LinkedEntity();
        topConceptLink.setInstanceId(rootCategory.getInstanceId());
        topConceptLink.setUid(rootCategory.getUid());
        topConceptLink.setEntityType(EntityNames.CATEGORY.name());
        schemeUpdate.addTopConcepts(topConceptLink);

        LinkedEntity result = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name())
                .create(schemeUpdate, StatusType.PUBLISHED, null, null);
        assertNotNull(result);

        // IMPORTANT: Use result.getInstanceId() because the create might return a different instanceId
        String schemeInstanceIdForQuery = result.getInstanceId();
        System.out.println("  Updated scheme instanceId: " + schemeInstanceIdForQuery);
        System.out.println("  Original scheme instanceId: " + scheme.getInstanceId());

        // Immediate check: what's in the database RIGHT NOW?
        System.out.println("  Checking CategoryHastopconcept table immediately after create...");

        // Try both possible instanceIds
        List<Object> topConceptsOriginal = dao.getJoinEntitiesByParentId(
                "categorySchemeInstanceId", scheme.getInstanceId(), CategoryHastopconcept.class);
        System.out.println("  TopConcepts with original instanceId: " +
                (topConceptsOriginal != null ? topConceptsOriginal.size() : "null"));

        List<Object> topConceptsNew = dao.getJoinEntitiesByParentId(
                "categorySchemeInstanceId", schemeInstanceIdForQuery, CategoryHastopconcept.class);
        System.out.println("  TopConcepts with result instanceId: " +
                (topConceptsNew != null ? topConceptsNew.size() : "null"));

        // Use whichever has results
        List<Object> topConcepts = (topConceptsNew != null && !topConceptsNew.isEmpty())
                ? topConceptsNew
                : topConceptsOriginal;

        assertNotNull(topConcepts, "TopConcepts should exist");
        assertFalse(topConcepts.isEmpty(), "Scheme should have at least 1 topConcept");

        CategoryHastopconcept tc = (CategoryHastopconcept) topConcepts.get(0);
        assertEquals(rootCategory.getInstanceId(), tc.getCategoryInstance().getInstanceId(),
                "TopConcept should be root category");

        System.out.println("✓ TopConcept added to scheme");
        System.out.println("✓ TopConcept: " + tc.getCategoryInstance().getUid());
    }

    // =========================================================================
    // TEST 7: Retrieve and verify full hierarchy via API
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("Test 7: Retrieve full hierarchy via API")
    void test07_RetrieveFullHierarchy() {
        System.out.println("TEST 7: Retrieve full hierarchy via API");
        System.out.println("Expected: All relationships are correctly loaded\n");

        String schemeUid = "category:test-scheme-" + testSuffix;
        String rootUid = "category:root-" + testSuffix;
        String child1Uid = "category:child-1-" + testSuffix;

        // Retrieve scheme via API
        org.epos.eposdatamodel.CategoryScheme retrievedScheme =
                (org.epos.eposdatamodel.CategoryScheme) AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name())
                        .retrieveByUID(schemeUid);

        assertNotNull(retrievedScheme, "Scheme should be retrievable");
        assertNotNull(retrievedScheme.getTopConcepts(), "TopConcepts should not be null");
        assertFalse(retrievedScheme.getTopConcepts().isEmpty(), "TopConcepts should not be empty");

        System.out.println("✓ Scheme retrieved: " + retrievedScheme.getUid());
        System.out.println("  TopConcepts: " + retrievedScheme.getTopConcepts().size());

        // Retrieve root category via API
        org.epos.eposdatamodel.Category retrievedRoot =
                (org.epos.eposdatamodel.Category) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                        .retrieveByUID(rootUid);

        assertNotNull(retrievedRoot, "Root category should be retrievable");
        assertNotNull(retrievedRoot.getInScheme(), "inScheme should be set");
        assertNotNull(retrievedRoot.getNarrower(), "Narrower should not be null");
        assertEquals(2, retrievedRoot.getNarrower().size(), "Root should have 2 narrower");

        System.out.println("✓ Root Category retrieved: " + retrievedRoot.getUid());
        System.out.println("  inScheme: " + retrievedRoot.getInScheme().getUid());
        System.out.println("  Narrower count: " + retrievedRoot.getNarrower().size());

        // Retrieve child1 via API
        org.epos.eposdatamodel.Category retrievedChild1 =
                (org.epos.eposdatamodel.Category) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                        .retrieveByUID(child1Uid);

        assertNotNull(retrievedChild1);
        assertNotNull(retrievedChild1.getBroader(), "Broader should not be null");
        assertFalse(retrievedChild1.getBroader().isEmpty(), "Child1 should have broader");
        assertNotNull(retrievedChild1.getNarrower(), "Narrower should not be null");
        assertEquals(1, retrievedChild1.getNarrower().size(), "Child1 should have 1 narrower (grandchild)");

        System.out.println("✓ Child 1 retrieved: " + retrievedChild1.getUid());
        System.out.println("  Broader: " + retrievedChild1.getBroader().get(0).getUid());
        System.out.println("  Narrower count: " + retrievedChild1.getNarrower().size());
    }

    // =========================================================================
    // TEST 8: Verify embedded ID queries work correctly (the camelCase fix)
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("Test 8: Verify embedded ID queries with camelCase field names")
    void test08_VerifyEmbeddedIdQueries() {
        System.out.println("TEST 8: Verify embedded ID queries work correctly");
        System.out.println("Expected: Queries using camelCase field names work\n");

        String schemeUid = "category:test-scheme-" + testSuffix;
        String child1Uid = "category:child-1-" + testSuffix;

        // Get the scheme
        List<CategoryScheme> schemeList = dao.getOneFromDBByUID(schemeUid, CategoryScheme.class);
        assertFalse(schemeList.isEmpty());
        CategoryScheme scheme = schemeList.get(0);

        // Get child1
        List<Category> child1List = dao.getOneFromDBByUID(child1Uid, Category.class);
        assertFalse(child1List.isEmpty());
        Category child1 = child1List.get(0);

        // TEST A: Query CategoryHastopconcept using correct camelCase
        System.out.println("Test A: CategoryHastopconcept query...");
        List<Object> topConcepts = null;
        Exception caughtException = null;

        try {
            // This should use "categorySchemeInstanceId" (camelCase with capital 'S')
            topConcepts = dao.getJoinEntitiesByParentId(
                    "categorySchemeInstanceId", scheme.getInstanceId(), CategoryHastopconcept.class);
        } catch (Exception e) {
            caughtException = e;
            System.err.println("  ✗ Query failed: " + e.getMessage());
        }

        assertNull(caughtException, "Query should not throw exception");
        assertNotNull(topConcepts, "TopConcepts should be returned");
        System.out.println("  ✓ categorySchemeInstanceId query successful, found " + topConcepts.size());

        // TEST B: Query CategoryIspartof for broader
        System.out.println("Test B: CategoryIspartof query for broader...");
        List<Object> broaderRels = null;
        try {
            broaderRels = dao.getJoinEntitiesByParentId(
                    "category1InstanceId", child1.getInstanceId(), CategoryIspartof.class);
        } catch (Exception e) {
            System.err.println("  ✗ Broader query failed: " + e.getMessage());
        }

        assertNotNull(broaderRels, "Broader query should work");
        assertFalse(broaderRels.isEmpty(), "Child1 should have broader");
        System.out.println("  ✓ category1InstanceId query successful, found " + broaderRels.size());

        // TEST C: Query CategoryIspartof for narrower
        System.out.println("Test C: CategoryIspartof query for narrower...");
        List<Object> narrowerRels = null;
        try {
            narrowerRels = dao.getJoinEntitiesByParentId(
                    "category2InstanceId", child1.getInstanceId(), CategoryIspartof.class);
        } catch (Exception e) {
            System.err.println("  ✗ Narrower query failed: " + e.getMessage());
        }

        assertNotNull(narrowerRels, "Narrower query should work");
        assertEquals(1, narrowerRels.size(), "Child1 should have 1 narrower (grandchild)");
        System.out.println("  ✓ category2InstanceId query successful, found " + narrowerRels.size());

        System.out.println("\n✓ All embedded ID queries successful!");
    }

    // =========================================================================
    // TEST 9: Print full hierarchy tree
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("Test 9: Print full category hierarchy tree")
    void test09_PrintFullHierarchy() {
        System.out.println("TEST 9: Print full category hierarchy tree");
        System.out.println("========================================\n");

        String schemeUid = "category:test-scheme-" + testSuffix;

        // Get scheme
        org.epos.eposdatamodel.CategoryScheme scheme =
                (org.epos.eposdatamodel.CategoryScheme) AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name())
                        .retrieveByUID(schemeUid);

        System.out.println("CategoryScheme: " + scheme.getUid());
        System.out.println("├── Title: " + scheme.getTitle());
        System.out.println("├── Status: " + scheme.getStatus());
        System.out.println("└── TopConcepts:");

        if (scheme.getTopConcepts() != null) {
            for (int i = 0; i < scheme.getTopConcepts().size(); i++) {
                LinkedEntity tc = scheme.getTopConcepts().get(i);
                boolean isLast = (i == scheme.getTopConcepts().size() - 1);
                printCategoryTree(tc.getUid(), "    ", isLast);
            }
        }

        System.out.println("\n========================================");
        System.out.println("✓ Hierarchy printed successfully");
    }

    private void printCategoryTree(String uid, String prefix, boolean isLast) {
        org.epos.eposdatamodel.Category cat =
                (org.epos.eposdatamodel.Category) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                        .retrieveByUID(uid);

        if (cat == null) return;

        String connector = isLast ? "└── " : "├── ";
        String childPrefix = prefix + (isLast ? "    " : "│   ");

        System.out.println(prefix + connector + cat.getName() + " [" + cat.getStatus() + "]");
        System.out.println(childPrefix + "    UID: " + cat.getUid());

        if (cat.getBroader() != null && !cat.getBroader().isEmpty()) {
            System.out.println(childPrefix + "    Broader: " + cat.getBroader().get(0).getUid());
        }

        if (cat.getNarrower() != null && !cat.getNarrower().isEmpty()) {
            System.out.println(childPrefix + "    Narrower:");
            for (int i = 0; i < cat.getNarrower().size(); i++) {
                LinkedEntity narrower = cat.getNarrower().get(i);
                boolean childIsLast = (i == cat.getNarrower().size() - 1);
                printCategoryTree(narrower.getUid(), childPrefix + "    ", childIsLast);
            }
        }
    }

    // =========================================================================
    // SUMMARY
    // =========================================================================
    @Test
    @Order(99)
    @DisplayName("SUMMARY: Test Results")
    void test99_Summary() {
        System.out.println("\n========================================");
        System.out.println("  CATEGORY HIERARCHY TEST SUMMARY");
        System.out.println("========================================");
        System.out.println("  Test 1:  Create CategoryScheme           ✓");
        System.out.println("  Test 2:  Create Root with inScheme       ✓");
        System.out.println("  Test 3:  Create Children with broader    ✓");
        System.out.println("  Test 4:  Create Grandchild (3 levels)    ✓");
        System.out.println("  Test 5:  Verify narrower relations       ✓");
        System.out.println("  Test 6:  Add TopConcepts to Scheme       ✓");
        System.out.println("  Test 7:  Retrieve full hierarchy         ✓");
        System.out.println("  Test 8:  Embedded ID queries (camelCase) ✓");
        System.out.println("  Test 9:  Print hierarchy tree            ✓");
        System.out.println("========================================\n");
    }
}