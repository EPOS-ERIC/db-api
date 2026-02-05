package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test that simulates the Seismology category hierarchy to debug the inversion issue.
 *
 * EXPECTED HIERARCHY (from categories_good.png):
 *
 * Seismological products services (PARENT)
 *   └── Macroseismic data (CHILD) - has broader=Seismological products services
 *   └── Earthquake source data (CHILD)
 *   └── Earthquake parameters (CHILD)
 *
 * Waveform metrics (PARENT)
 *   └── Quality metrics distributed by EIDA (CHILD) - has broader=Waveform metrics
 *
 * WRONG HIERARCHY (from categories_bad.png):
 *
 * Macroseismic data (appears as PARENT - WRONG!)
 *   └── Seismological products services (appears as CHILD - WRONG!)
 *
 * Quality metrics distributed by EIDA (appears as PARENT - WRONG!)
 *   └── Waveform metrics (appears as CHILD - WRONG!)
 *
 * This test verifies:
 * 1. The database stores the correct parent/child in category1/category2 columns
 * 2. The API retrieves broader/narrower correctly
 * 3. The hierarchy is NOT inverted
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SeismologyHierarchyTest extends TestcontainersLifecycle {

    private static EposDataModelDAO dao;
    private static AbstractAPI categoryAPI;
    private static AbstractAPI categorySchemeAPI;

    // UIDs matching the real data pattern
    private static final String SCHEME_UID = "category:seismology";

    // Parent categories (these are the BROADER)
    private static final String SEISMO_PRODUCTS_UID = "category:seismologicalproductsservices";
    private static final String WAVEFORM_METRICS_UID = "category:waveformmetrics";

    // Child categories (these HAVE broader pointing to parents above)
    private static final String MACROSEISMIC_UID = "category:macroseismicdata";
    private static final String EARTHQUAKE_SOURCE_UID = "category:earthquakesourcedata";
    private static final String QUALITY_METRICS_UID = "category:qualitymetricsdistributedbyeida";

    @BeforeAll
    static void setup() {
        dao = EposDataModelDAO.getInstance();
        categoryAPI = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        categorySchemeAPI = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║        SEISMOLOGY HIERARCHY TEST - DEBUG INVERSION               ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");
    }

    @Test
    @Order(1)
    @DisplayName("1. Create CategoryScheme")
    void test01_CreateScheme() {
        System.out.println("TEST 1: Create CategoryScheme");
        System.out.println("==============================\n");

        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid(SCHEME_UID);
        scheme.setTitle("Seismology");
        scheme.setDescription("Test Seismology Domain");
        scheme.setEditorId("test-user");
        scheme.setStatus(StatusType.PUBLISHED);

        LinkedEntity result = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);
        assertNotNull(result, "CategoryScheme should be created");
        System.out.println("✓ CategoryScheme created: " + result.getUid() + "\n");
    }

    @Test
    @Order(2)
    @DisplayName("2. Create PARENT categories (no broader)")
    void test02_CreateParentCategories() {
        System.out.println("TEST 2: Create PARENT categories (no broader)");
        System.out.println("==============================================\n");

        LinkedEntity schemeRef = getSchemeReference();

        // Create "Seismological products services" - this will be a PARENT
        createCategory(SEISMO_PRODUCTS_UID, "Seismological products services", schemeRef, null);
        System.out.println("✓ Created PARENT: Seismological products services");

        // Create "Waveform metrics" - this will be a PARENT
        createCategory(WAVEFORM_METRICS_UID, "Waveform metrics", schemeRef, null);
        System.out.println("✓ Created PARENT: Waveform metrics");

        System.out.println("\n✓ All PARENT categories created (these have NO broader)\n");
    }

    @Test
    @Order(3)
    @DisplayName("3. Create CHILD categories WITH broader reference")
    void test03_CreateChildCategoriesWithBroader() {
        System.out.println("TEST 3: Create CHILD categories WITH broader");
        System.out.println("=============================================\n");

        LinkedEntity schemeRef = getSchemeReference();

        // Get references to parents
        LinkedEntity seismoProductsRef = getCategoryReference(SEISMO_PRODUCTS_UID);
        LinkedEntity waveformMetricsRef = getCategoryReference(WAVEFORM_METRICS_UID);

        assertNotNull(seismoProductsRef, "Seismological products services should exist");
        assertNotNull(waveformMetricsRef, "Waveform metrics should exist");

        // Create "Macroseismic data" as CHILD of "Seismological products services"
        // This means: Macroseismic data HAS broader = Seismological products services
        createCategory(MACROSEISMIC_UID, "Macroseismic data", schemeRef, seismoProductsRef);
        System.out.println("✓ Created CHILD: Macroseismic data (broader=Seismological products services)");

        // Create "Earthquake source data" as CHILD of "Seismological products services"
        createCategory(EARTHQUAKE_SOURCE_UID, "Earthquake source data", schemeRef, seismoProductsRef);
        System.out.println("✓ Created CHILD: Earthquake source data (broader=Seismological products services)");

        // Create "Quality metrics distributed by EIDA" as CHILD of "Waveform metrics"
        createCategory(QUALITY_METRICS_UID, "Quality metrics distributed by EIDA", schemeRef, waveformMetricsRef);
        System.out.println("✓ Created CHILD: Quality metrics distributed by EIDA (broader=Waveform metrics)");

        System.out.println("\n✓ All CHILD categories created with broader references\n");
    }

    @Test
    @Order(4)
    @DisplayName("4. CRITICAL: Verify database has correct parent/child columns")
    void test04_VerifyDatabaseColumns() {
        System.out.println("TEST 4: Verify database columns (CRITICAL)");
        System.out.println("===========================================\n");

        // Get the child category (Macroseismic data)
        List<Category> macroseismicList = dao.getOneFromDBByUID(MACROSEISMIC_UID, Category.class);
        assertFalse(macroseismicList.isEmpty(), "Macroseismic data should exist");
        Category macroseismic = macroseismicList.get(0);

        // Get the parent category (Seismological products services)
        List<Category> seismoProductsList = dao.getOneFromDBByUID(SEISMO_PRODUCTS_UID, Category.class);
        assertFalse(seismoProductsList.isEmpty(), "Seismological products services should exist");
        Category seismoProducts = seismoProductsList.get(0);

        System.out.println("Database IDs:");
        System.out.println("  CHILD (Macroseismic data) instanceId: " + macroseismic.getInstanceId());
        System.out.println("  PARENT (Seismological products services) instanceId: " + seismoProducts.getInstanceId());

        // Query CategoryIspartof where child is category1
        System.out.println("\nQuerying CategoryIspartof where category1Instance = CHILD (Macroseismic)...");
        List<Object> broaderRels = dao.getOneFromDBBySpecificKey(
                "category1Instance", macroseismic.getInstanceId(), CategoryIspartof.class);

        System.out.println("  Found " + (broaderRels != null ? broaderRels.size() : 0) + " relations");

        if (broaderRels != null && !broaderRels.isEmpty()) {
            for (Object obj : broaderRels) {
                CategoryIspartof rel = (CategoryIspartof) obj;
                String cat1Id = rel.getCategory1Instance() != null ? rel.getCategory1Instance().getInstanceId() : "NULL";
                String cat2Id = rel.getCategory2Instance() != null ? rel.getCategory2Instance().getInstanceId() : "NULL";
                String cat1Uid = rel.getCategory1Instance() != null ? rel.getCategory1Instance().getUid() : "NULL";
                String cat2Uid = rel.getCategory2Instance() != null ? rel.getCategory2Instance().getUid() : "NULL";

                System.out.println("  CategoryIspartof record:");
                System.out.println("    category1_instance_id: " + cat1Id + " (UID: " + cat1Uid + ")");
                System.out.println("    category2_instance_id: " + cat2Id + " (UID: " + cat2Uid + ")");

                // CRITICAL VERIFICATION
                assertEquals(macroseismic.getInstanceId(), cat1Id,
                        "CRITICAL: category1 should be CHILD (Macroseismic data)");
                assertEquals(seismoProducts.getInstanceId(), cat2Id,
                        "CRITICAL: category2 should be PARENT (Seismological products services)");

                System.out.println("  ✓ CORRECT: category1=CHILD, category2=PARENT");
            }
        } else {
            fail("No CategoryIspartof record found! The broader relation was not created.");
        }

        // Also verify by querying where parent is category2
        System.out.println("\nQuerying CategoryIspartof where category2Instance = PARENT (Seismo products)...");
        List<Object> narrowerRels = dao.getOneFromDBBySpecificKey(
                "category2Instance", seismoProducts.getInstanceId(), CategoryIspartof.class);

        System.out.println("  Found " + (narrowerRels != null ? narrowerRels.size() : 0) + " children (narrower)");

        // Should have 2 children: Macroseismic data and Earthquake source data
        assertNotNull(narrowerRels, "Narrower relations should exist");
        assertEquals(2, narrowerRels.size(), "Seismological products services should have 2 children");

        for (Object obj : narrowerRels) {
            CategoryIspartof rel = (CategoryIspartof) obj;
            String childUid = rel.getCategory1Instance() != null ? rel.getCategory1Instance().getUid() : "NULL";
            System.out.println("    Child: " + childUid);
        }

        System.out.println("\n✓ Database columns are CORRECT\n");
    }

    @Test
    @Order(5)
    @DisplayName("5. Verify API retrieval - PARENT should have narrower")
    void test05_VerifyParentHasNarrower() {
        System.out.println("TEST 5: Verify PARENT has narrower via API");
        System.out.println("==========================================\n");

        // Retrieve "Seismological products services" via API
        org.epos.eposdatamodel.Category seismoProducts =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(SEISMO_PRODUCTS_UID);

        assertNotNull(seismoProducts, "Seismological products services should be retrievable");

        System.out.println("Seismological products services via API:");
        System.out.println("  uid: " + seismoProducts.getUid());
        System.out.println("  name: " + seismoProducts.getName());
        System.out.println("  broader count: " + (seismoProducts.getBroader() != null ? seismoProducts.getBroader().size() : 0));
        System.out.println("  narrower count: " + (seismoProducts.getNarrower() != null ? seismoProducts.getNarrower().size() : 0));

        // This PARENT should have NO broader
        assertTrue(seismoProducts.getBroader() == null || seismoProducts.getBroader().isEmpty(),
                "PARENT should have NO broader");

        // This PARENT should have 2 narrower (children)
        assertNotNull(seismoProducts.getNarrower(), "PARENT should have narrower list");
        assertEquals(2, seismoProducts.getNarrower().size(),
                "Seismological products services should have 2 narrower (Macroseismic + Earthquake source)");

        System.out.println("  Narrower (children):");
        for (LinkedEntity narrower : seismoProducts.getNarrower()) {
            System.out.println("    - " + narrower.getUid());
        }

        // Verify the correct children are listed
        boolean hasMacroseismic = seismoProducts.getNarrower().stream()
                .anyMatch(n -> MACROSEISMIC_UID.equals(n.getUid()));
        boolean hasEarthquakeSource = seismoProducts.getNarrower().stream()
                .anyMatch(n -> EARTHQUAKE_SOURCE_UID.equals(n.getUid()));

        assertTrue(hasMacroseismic, "Should have Macroseismic data as narrower");
        assertTrue(hasEarthquakeSource, "Should have Earthquake source data as narrower");

        System.out.println("\n✓ PARENT correctly shows children as narrower\n");
    }

    @Test
    @Order(6)
    @DisplayName("6. Verify API retrieval - CHILD should have broader")
    void test06_VerifyChildHasBroader() {
        System.out.println("TEST 6: Verify CHILD has broader via API");
        System.out.println("=========================================\n");

        // Retrieve "Macroseismic data" via API
        org.epos.eposdatamodel.Category macroseismic =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(MACROSEISMIC_UID);

        assertNotNull(macroseismic, "Macroseismic data should be retrievable");

        System.out.println("Macroseismic data via API:");
        System.out.println("  uid: " + macroseismic.getUid());
        System.out.println("  name: " + macroseismic.getName());
        System.out.println("  broader count: " + (macroseismic.getBroader() != null ? macroseismic.getBroader().size() : 0));
        System.out.println("  narrower count: " + (macroseismic.getNarrower() != null ? macroseismic.getNarrower().size() : 0));

        // This CHILD should have 1 broader (parent)
        assertNotNull(macroseismic.getBroader(), "CHILD should have broader list");
        assertEquals(1, macroseismic.getBroader().size(),
                "Macroseismic data should have 1 broader (Seismological products services)");

        System.out.println("  Broader (parent):");
        for (LinkedEntity broader : macroseismic.getBroader()) {
            System.out.println("    - " + broader.getUid());
        }

        // Verify the correct parent is listed
        assertEquals(SEISMO_PRODUCTS_UID, macroseismic.getBroader().get(0).getUid(),
                "Broader should be Seismological products services");

        // This CHILD should have NO narrower
        assertTrue(macroseismic.getNarrower() == null || macroseismic.getNarrower().isEmpty(),
                "CHILD (Macroseismic data) should have NO narrower");

        System.out.println("\n✓ CHILD correctly shows parent as broader\n");
    }

    @Test
    @Order(7)
    @DisplayName("7. Verify Waveform metrics hierarchy")
    void test07_VerifyWaveformMetricsHierarchy() {
        System.out.println("TEST 7: Verify Waveform metrics hierarchy");
        System.out.println("==========================================\n");

        // Retrieve "Waveform metrics" (PARENT)
        org.epos.eposdatamodel.Category waveformMetrics =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(WAVEFORM_METRICS_UID);

        assertNotNull(waveformMetrics, "Waveform metrics should be retrievable");

        System.out.println("Waveform metrics (PARENT):");
        System.out.println("  broader: " + (waveformMetrics.getBroader() != null ? waveformMetrics.getBroader().size() : 0));
        System.out.println("  narrower: " + (waveformMetrics.getNarrower() != null ? waveformMetrics.getNarrower().size() : 0));

        // Should have 1 narrower (Quality metrics)
        assertNotNull(waveformMetrics.getNarrower(), "Waveform metrics should have narrower");
        assertEquals(1, waveformMetrics.getNarrower().size(),
                "Waveform metrics should have 1 child (Quality metrics)");
        assertEquals(QUALITY_METRICS_UID, waveformMetrics.getNarrower().get(0).getUid(),
                "Narrower should be Quality metrics");

        System.out.println("  Narrower: " + waveformMetrics.getNarrower().get(0).getUid());

        // Retrieve "Quality metrics distributed by EIDA" (CHILD)
        org.epos.eposdatamodel.Category qualityMetrics =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(QUALITY_METRICS_UID);

        assertNotNull(qualityMetrics, "Quality metrics should be retrievable");

        System.out.println("\nQuality metrics distributed by EIDA (CHILD):");
        System.out.println("  broader: " + (qualityMetrics.getBroader() != null ? qualityMetrics.getBroader().size() : 0));
        System.out.println("  narrower: " + (qualityMetrics.getNarrower() != null ? qualityMetrics.getNarrower().size() : 0));

        // Should have 1 broader (Waveform metrics)
        assertNotNull(qualityMetrics.getBroader(), "Quality metrics should have broader");
        assertEquals(1, qualityMetrics.getBroader().size(),
                "Quality metrics should have 1 parent (Waveform metrics)");
        assertEquals(WAVEFORM_METRICS_UID, qualityMetrics.getBroader().get(0).getUid(),
                "Broader should be Waveform metrics");

        System.out.println("  Broader: " + qualityMetrics.getBroader().get(0).getUid());

        System.out.println("\n✓ Waveform metrics hierarchy is CORRECT\n");
    }

    @Test
    @Order(8)
    @DisplayName("8. Print full hierarchy tree")
    void test08_PrintHierarchyTree() {
        System.out.println("TEST 8: Print full hierarchy tree");
        System.out.println("===================================\n");

        System.out.println("EXPECTED HIERARCHY:");
        System.out.println("  Seismological products services (PARENT)");
        System.out.println("    └── Macroseismic data (CHILD)");
        System.out.println("    └── Earthquake source data (CHILD)");
        System.out.println("  Waveform metrics (PARENT)");
        System.out.println("    └── Quality metrics distributed by EIDA (CHILD)");
        System.out.println();

        System.out.println("ACTUAL HIERARCHY FROM API:");

        // Print Seismological products services tree
        org.epos.eposdatamodel.Category seismoProducts =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(SEISMO_PRODUCTS_UID);
        printCategoryTree(seismoProducts, "  ");

        // Print Waveform metrics tree
        org.epos.eposdatamodel.Category waveformMetrics =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(WAVEFORM_METRICS_UID);
        printCategoryTree(waveformMetrics, "  ");

        System.out.println("\n✓ Hierarchy printed\n");
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    private LinkedEntity getSchemeReference() {
        List<CategoryScheme> list = dao.getOneFromDBByUID(SCHEME_UID, CategoryScheme.class);
        if (list.isEmpty()) return null;
        CategoryScheme scheme = list.get(0);

        LinkedEntity ref = new LinkedEntity();
        ref.setInstanceId(scheme.getInstanceId());
        ref.setMetaId(scheme.getMetaId());
        ref.setUid(scheme.getUid());
        ref.setEntityType(EntityNames.CATEGORYSCHEME.name());
        return ref;
    }

    private LinkedEntity getCategoryReference(String uid) {
        List<Category> list = dao.getOneFromDBByUID(uid, Category.class);
        if (list.isEmpty()) return null;
        Category cat = list.get(0);

        LinkedEntity ref = new LinkedEntity();
        ref.setInstanceId(cat.getInstanceId());
        ref.setMetaId(cat.getMetaId());
        ref.setUid(cat.getUid());
        ref.setEntityType(EntityNames.CATEGORY.name());
        return ref;
    }

    private void createCategory(String uid, String name, LinkedEntity schemeRef, LinkedEntity broaderRef) {
        org.epos.eposdatamodel.Category cat = new org.epos.eposdatamodel.Category();
        cat.setUid(uid);
        cat.setName(name);
        cat.setDescription("Test category");
        cat.setEditorId("test-user");
        cat.setStatus(StatusType.PUBLISHED);
        cat.setInScheme(schemeRef);

        if (broaderRef != null) {
            cat.addBroader(broaderRef);
        }

        LinkedEntity result = categoryAPI.create(cat, StatusType.PUBLISHED, null, null);
        assertNotNull(result, "Category " + uid + " should be created");
    }

    private void printCategoryTree(org.epos.eposdatamodel.Category cat, String indent) {
        if (cat == null) return;

        System.out.println(indent + cat.getName() + " [" + cat.getUid() + "]");
        System.out.println(indent + "  (broader: " +
                (cat.getBroader() != null ? cat.getBroader().size() : 0) +
                ", narrower: " +
                (cat.getNarrower() != null ? cat.getNarrower().size() : 0) + ")");

        if (cat.getNarrower() != null) {
            for (LinkedEntity narrower : cat.getNarrower()) {
                org.epos.eposdatamodel.Category child =
                        (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(narrower.getUid());
                if (child != null) {
                    printCategoryTree(child, indent + "    └── ");
                } else {
                    System.out.println(indent + "    └── " + narrower.getUid() + " [NOT FOUND]");
                }
            }
        }
    }

    // =========================================================================
    // SUMMARY
    // =========================================================================
    @Test
    @Order(99)
    @DisplayName("SUMMARY")
    void test99_Summary() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║           SEISMOLOGY HIERARCHY TEST SUMMARY                      ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Test 1:  Create CategoryScheme                         ✓         ║");
        System.out.println("║ Test 2:  Create PARENT categories (no broader)         ✓         ║");
        System.out.println("║ Test 3:  Create CHILD categories with broader          ✓         ║");
        System.out.println("║ Test 4:  Verify database columns (CRITICAL)            ✓         ║");
        System.out.println("║ Test 5:  Verify PARENT has narrower via API            ✓         ║");
        System.out.println("║ Test 6:  Verify CHILD has broader via API              ✓         ║");
        System.out.println("║ Test 7:  Verify Waveform metrics hierarchy             ✓         ║");
        System.out.println("║ Test 8:  Print full hierarchy tree                     ✓         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");
    }
}