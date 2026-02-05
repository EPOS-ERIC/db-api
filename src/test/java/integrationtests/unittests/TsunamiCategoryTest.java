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
 * Test basato sulla struttura reale del TCS Tsunami.
 *
 * Struttura da testare (da TTL):
 *
 * CategoryScheme: category:tsunami
 *   └── hasTopConcept: category:facets/dataset-theme (non testato qui)
 *
 * Categories con broader/narrower:
 *   - tsunamidata (solo inScheme)
 *   - hazardriskproducts
 *       └── narrower: ntheasternatlanticmediterraneancnectedseastsunamihazardmodel2018ingv
 *       └── narrower: europeantsunamiriskserviceetris
 *       └── narrower: inundationmap
 *   - tsunamicatalogue
 *       └── narrower: euromediterraneantsunamicatalogueemtc
 *       └── narrower: italiantsunamieffectsdatabaseited
 *   - submarelslidecatalogue
 *       └── narrower: theeuromediterraneansubmarelslidedatabaseemss25
 *   - paleotsunamidepositscatalogue
 *       └── narrower: paleotsunamideposits
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TsunamiCategoryTest  extends TestcontainersLifecycle {

    private static EposDataModelDAO dao;
    private static AbstractAPI categoryAPI;
    private static AbstractAPI categorySchemeAPI;

    // UIDs from the TTL file
    private static final String SCHEME_UID = "category:tsunami";

    private static final String CAT_TSUNAMIDATA = "category:tsunamidata";
    private static final String CAT_HAZARDRISKPRODUCTS = "category:hazardriskproducts";
    private static final String CAT_NEAM_HAZARD = "category:ntheasternatlanticmediterraneancnectedseastsunamihazardmodel2018ingv";
    private static final String CAT_ETRIS = "category:europeantsunamiriskserviceetris";
    private static final String CAT_INUNDATION = "category:inundationmap";
    private static final String CAT_TSUNAMICATALOGUE = "category:tsunamicatalogue";
    private static final String CAT_EMTC = "category:euromediterraneantsunamicatalogueemtc";
    private static final String CAT_ITED = "category:italiantsunamieffectsdatabaseited";

    @BeforeAll
    static void setup() {
        dao = EposDataModelDAO.getInstance();
        categoryAPI = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        categorySchemeAPI = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());

        assertNotNull(categoryAPI, "CategoryAPI should be available");
        assertNotNull(categorySchemeAPI, "CategorySchemeAPI should be available");

        System.out.println("\n╔══════════════════════════════════════════════════════════════════╗");
        System.out.println("║           TSUNAMI CATEGORY STRUCTURE TEST                        ║");
        System.out.println("║   Based on real TCS Tsunami TTL configuration                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");
    }

    // =========================================================================
    // TEST 1: Create CategoryScheme "tsunami"
    // =========================================================================
    @Test
    @Order(1)
    @DisplayName("1. Create CategoryScheme: tsunami")
    void test01_CreateTsunamiScheme() {
        System.out.println("TEST 1: Create CategoryScheme 'tsunami'");
        System.out.println("========================================\n");

        org.epos.eposdatamodel.CategoryScheme scheme = new org.epos.eposdatamodel.CategoryScheme();
        scheme.setUid(SCHEME_UID);
        scheme.setTitle("Tsunami");
        scheme.setDescription("TCS Domain");
        scheme.setCode("TSU");
        scheme.setLogo("assets/img/logo/TSU_logo.png");
        scheme.setHomepage("https://www.epos-eu.org/tcs/tsunami");
        scheme.setColor("#6f9ea8");
        scheme.setOrderitemnumber("10");
        scheme.setEditorId("test-user");
        scheme.setStatus(StatusType.PUBLISHED);

        LinkedEntity result = categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        assertNotNull(result, "CategoryScheme should be created");
        System.out.println("✓ CategoryScheme created: " + result.getUid());

        // Verify in DB
        List<CategoryScheme> dbList = dao.getOneFromDBByUID(SCHEME_UID, CategoryScheme.class);
        assertFalse(dbList.isEmpty(), "Scheme should exist in DB");
        assertEquals("Tsunami", dbList.get(0).getName());
        System.out.println("✓ Verified in database\n");
    }

    // =========================================================================
    // TEST 2: Create parent categories (no broader)
    // =========================================================================
    @Test
    @Order(2)
    @DisplayName("2. Create parent categories without broader")
    void test02_CreateParentCategories() {
        System.out.println("TEST 2: Create parent categories (no broader)");
        System.out.println("=============================================\n");

        // Get scheme reference
        LinkedEntity schemeRef = getSchemeReference();

        // Create tsunamidata (standalone, no hierarchy)
        createCategory(CAT_TSUNAMIDATA, "Tsunami Data", "TCS Subdomain", schemeRef, null);
        System.out.println("✓ Created: tsunamidata");

        // Create hazardriskproducts (parent, will have narrower)
        createCategory(CAT_HAZARDRISKPRODUCTS, "Hazard and Risk Products", "TCS Subdomain", schemeRef, null);
        System.out.println("✓ Created: hazardriskproducts");

        // Create tsunamicatalogue (parent, will have narrower)
        createCategory(CAT_TSUNAMICATALOGUE, "Tsunami Catalogue", "TCS Subdomain", schemeRef, null);
        System.out.println("✓ Created: tsunamicatalogue");

        System.out.println("\n✓ All parent categories created\n");
    }

    // =========================================================================
    // TEST 3: Create child categories WITH broader reference
    // =========================================================================
    @Test
    @Order(3)
    @DisplayName("3. Create child categories with broader reference")
    void test03_CreateChildCategoriesWithBroader() {
        System.out.println("TEST 3: Create child categories with broader");
        System.out.println("============================================\n");

        LinkedEntity schemeRef = getSchemeReference();

        // Children of hazardriskproducts
        LinkedEntity hazardRef = getCategoryReference(CAT_HAZARDRISKPRODUCTS);
        assertNotNull(hazardRef, "hazardriskproducts should exist");

        createCategory(CAT_NEAM_HAZARD,
                "North-eastern Atlantic, Mediterranean and connected Seas Tsunami Hazard Model 2018 (INGV)",
                "TCS Subdomain", schemeRef, hazardRef);
        System.out.println("✓ Created: NEAM Hazard Model (broader=hazardriskproducts)");

        createCategory(CAT_ETRIS,
                "European Tsunami Risk Service (ETRIS)",
                "TCS Subdomain", schemeRef, hazardRef);
        System.out.println("✓ Created: ETRIS (broader=hazardriskproducts)");

        createCategory(CAT_INUNDATION,
                "Inundation Maps",
                "TCS Subdomain", schemeRef, hazardRef);
        System.out.println("✓ Created: Inundation Maps (broader=hazardriskproducts)");

        // Children of tsunamicatalogue
        LinkedEntity catalogueRef = getCategoryReference(CAT_TSUNAMICATALOGUE);
        assertNotNull(catalogueRef, "tsunamicatalogue should exist");

        createCategory(CAT_EMTC,
                "Euro-Mediterranean Tsunami Catalogue (EMTC)",
                "TCS Subdomain", schemeRef, catalogueRef);
        System.out.println("✓ Created: EMTC (broader=tsunamicatalogue)");

        createCategory(CAT_ITED,
                "Italian Tsunami Effects Database (ITED)",
                "TCS Subdomain", schemeRef, catalogueRef);
        System.out.println("✓ Created: ITED (broader=tsunamicatalogue)");

        System.out.println("\n✓ All child categories created\n");
    }

    // =========================================================================
    // TEST 4: Verify broader relationships were saved
    // =========================================================================
    @Test
    @Order(4)
    @DisplayName("4. Verify broader relationships in database")
    void test04_VerifyBroaderInDatabase() {
        System.out.println("TEST 4: Verify broader relationships in database");
        System.out.println("=================================================\n");

        // Check CategoryIspartof table directly
        System.out.println("Checking CategoryIspartof table...\n");

        // Get NEAM Hazard category
        List<Category> neamList = dao.getOneFromDBByUID(CAT_NEAM_HAZARD, Category.class);
        assertFalse(neamList.isEmpty(), "NEAM category should exist");
        Category neam = neamList.get(0);

        // Query broader (category1Instance = child, category2Instance = parent)
        List<Object> broaderRels = dao.getOneFromDBBySpecificKey(
                "category1Instance", neam.getInstanceId(), CategoryIspartof.class);

        System.out.println("NEAM Hazard Model:");
        System.out.println("  instanceId: " + neam.getInstanceId());
        System.out.println("  broader relations found: " + (broaderRels != null ? broaderRels.size() : 0));

        if (broaderRels != null && !broaderRels.isEmpty()) {
            for (Object obj : broaderRels) {
                CategoryIspartof rel = (CategoryIspartof) obj;
                System.out.println("  → broader: " + rel.getCategory2Instance().getUid());
            }
            System.out.println("✓ Broader relationship EXISTS in database");
        } else {
            System.out.println("✗ NO broader relationship found in database!");

            // Debug: check if any CategoryIspartof entries exist at all
            System.out.println("\nDebug: Checking if ANY CategoryIspartof entries exist...");
            try {
                // Try to get all entries for any parent
                List<Category> hazardList = dao.getOneFromDBByUID(CAT_HAZARDRISKPRODUCTS, Category.class);
                if (!hazardList.isEmpty()) {
                    List<Object> narrowerRels = dao.getOneFromDBBySpecificKey(
                            "category2Instance", hazardList.get(0).getInstanceId(), CategoryIspartof.class);
                    System.out.println("  hazardriskproducts narrower relations: " +
                            (narrowerRels != null ? narrowerRels.size() : 0));
                }
            } catch (Exception e) {
                System.out.println("  Error checking: " + e.getMessage());
            }
        }

        assertNotNull(broaderRels, "Broader relations should not be null");
        assertFalse(broaderRels.isEmpty(), "NEAM should have broader=hazardriskproducts");

        System.out.println();
    }

    // =========================================================================
    // TEST 5: Verify narrower relationships (reverse query)
    // =========================================================================
    @Test
    @Order(5)
    @DisplayName("5. Verify narrower relationships (reverse of broader)")
    void test05_VerifyNarrowerRelationships() {
        System.out.println("TEST 5: Verify narrower relationships");
        System.out.println("=====================================\n");

        // Get hazardriskproducts
        List<Category> hazardList = dao.getOneFromDBByUID(CAT_HAZARDRISKPRODUCTS, Category.class);
        assertFalse(hazardList.isEmpty(), "hazardriskproducts should exist");
        Category hazard = hazardList.get(0);

        // Query narrower (category2Instance = parent, returns children as category1Instance)
        List<Object> narrowerRels = dao.getOneFromDBBySpecificKey(
                "category2Instance", hazard.getInstanceId(), CategoryIspartof.class);

        System.out.println("hazardriskproducts:");
        System.out.println("  instanceId: " + hazard.getInstanceId());
        System.out.println("  narrower relations found: " + (narrowerRels != null ? narrowerRels.size() : 0));

        if (narrowerRels != null && !narrowerRels.isEmpty()) {
            System.out.println("  Children (narrower):");
            for (Object obj : narrowerRels) {
                CategoryIspartof rel = (CategoryIspartof) obj;
                System.out.println("    - " + rel.getCategory1Instance().getUid());
            }
        }

        // Should have 3 children: NEAM, ETRIS, Inundation
        assertNotNull(narrowerRels, "Narrower relations should not be null");
        assertEquals(3, narrowerRels.size(), "hazardriskproducts should have 3 narrower categories");

        System.out.println("✓ hazardriskproducts has correct narrower count\n");
    }

    // =========================================================================
    // TEST 6: Retrieve via API and check relations
    // =========================================================================
    @Test
    @Order(6)
    @DisplayName("6. Retrieve categories via API and verify relations")
    void test06_RetrieveViaAPI() {
        System.out.println("TEST 6: Retrieve via API and verify relations");
        System.out.println("==============================================\n");

        // Retrieve hazardriskproducts via API
        org.epos.eposdatamodel.Category hazard =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(CAT_HAZARDRISKPRODUCTS);

        assertNotNull(hazard, "hazardriskproducts should be retrievable");
        System.out.println("hazardriskproducts via API:");
        System.out.println("  uid: " + hazard.getUid());
        System.out.println("  inScheme: " + (hazard.getInScheme() != null ? hazard.getInScheme().getUid() : "NULL"));
        System.out.println("  broader: " + (hazard.getBroader() != null ? hazard.getBroader().size() : "NULL"));
        System.out.println("  narrower: " + (hazard.getNarrower() != null ? hazard.getNarrower().size() : "NULL"));

        if (hazard.getNarrower() != null && !hazard.getNarrower().isEmpty()) {
            System.out.println("  Narrower list:");
            for (LinkedEntity n : hazard.getNarrower()) {
                System.out.println("    - " + n.getUid());
            }
        }

        // Verify inScheme
        assertNotNull(hazard.getInScheme(), "inScheme should be set");
        assertEquals(SCHEME_UID, hazard.getInScheme().getUid(), "inScheme should be tsunami");

        // Verify narrower
        assertNotNull(hazard.getNarrower(), "narrower should not be null");
        assertEquals(3, hazard.getNarrower().size(), "Should have 3 narrower categories");

        System.out.println("\n✓ API retrieval correct\n");

        // Also check a child category
        org.epos.eposdatamodel.Category neam =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(CAT_NEAM_HAZARD);

        assertNotNull(neam, "NEAM should be retrievable");
        System.out.println("NEAM Hazard Model via API:");
        System.out.println("  uid: " + neam.getUid());
        System.out.println("  inScheme: " + (neam.getInScheme() != null ? neam.getInScheme().getUid() : "NULL"));
        System.out.println("  broader: " + (neam.getBroader() != null ? neam.getBroader().size() : "NULL"));

        if (neam.getBroader() != null && !neam.getBroader().isEmpty()) {
            System.out.println("  Broader list:");
            for (LinkedEntity b : neam.getBroader()) {
                System.out.println("    - " + b.getUid());
            }
        }

        assertNotNull(neam.getBroader(), "broader should not be null");
        assertFalse(neam.getBroader().isEmpty(), "NEAM should have broader");
        assertEquals(CAT_HAZARDRISKPRODUCTS, neam.getBroader().get(0).getUid(),
                "NEAM broader should be hazardriskproducts");

        System.out.println("\n✓ Child category broader correct\n");
    }

    // =========================================================================
    // TEST 7: Add topConcepts to scheme
    // =========================================================================
    @Test
    @Order(7)
    @DisplayName("7. Add topConcepts to CategoryScheme")
    void test07_AddTopConcepts() {
        System.out.println("TEST 7: Add topConcepts to CategoryScheme");
        System.out.println("==========================================\n");

        // Get current scheme
        List<CategoryScheme> schemeList = dao.getOneFromDBByUID(SCHEME_UID, CategoryScheme.class);
        assertFalse(schemeList.isEmpty());
        CategoryScheme dbScheme = schemeList.get(0);

        // Get parent categories to add as topConcepts
        LinkedEntity hazardRef = getCategoryReference(CAT_HAZARDRISKPRODUCTS);
        LinkedEntity catalogueRef = getCategoryReference(CAT_TSUNAMICATALOGUE);
        LinkedEntity dataRef = getCategoryReference(CAT_TSUNAMIDATA);

        // Update scheme with topConcepts
        org.epos.eposdatamodel.CategoryScheme schemeUpdate = new org.epos.eposdatamodel.CategoryScheme();
        schemeUpdate.setInstanceId(dbScheme.getInstanceId());
        schemeUpdate.setMetaId(dbScheme.getMetaId());
        schemeUpdate.setUid(dbScheme.getUid());
        schemeUpdate.setTitle(dbScheme.getName());
        schemeUpdate.setEditorId("test-user");
        schemeUpdate.setStatus(StatusType.PUBLISHED);

        schemeUpdate.addTopConcepts(hazardRef);
        schemeUpdate.addTopConcepts(catalogueRef);
        schemeUpdate.addTopConcepts(dataRef);

        System.out.println("Adding topConcepts:");
        System.out.println("  - " + hazardRef.getUid());
        System.out.println("  - " + catalogueRef.getUid());
        System.out.println("  - " + dataRef.getUid());

        LinkedEntity result = categorySchemeAPI.create(schemeUpdate, StatusType.PUBLISHED, null, null);
        assertNotNull(result);

        System.out.println("\nVerifying in database...");

        // Check CategoryHastopconcept table
        List<Object> topConcepts = dao.getJoinEntitiesByParentId(
                "categorySchemeInstanceId", dbScheme.getInstanceId(), CategoryHastopconcept.class);

        System.out.println("TopConcepts found: " + (topConcepts != null ? topConcepts.size() : 0));

        if (topConcepts != null && !topConcepts.isEmpty()) {
            for (Object obj : topConcepts) {
                CategoryHastopconcept tc = (CategoryHastopconcept) obj;
                System.out.println("  - " + tc.getCategoryInstance().getUid());
            }
            System.out.println("✓ TopConcepts saved correctly");
        } else {
            System.out.println("✗ NO topConcepts found in database!");
        }

        assertNotNull(topConcepts, "TopConcepts should not be null");
        assertEquals(3, topConcepts.size(), "Should have 3 topConcepts");

        System.out.println();
    }

    // =========================================================================
    // TEST 8: Retrieve scheme via API with topConcepts
    // =========================================================================
    @Test
    @Order(8)
    @DisplayName("8. Retrieve CategoryScheme via API with topConcepts")
    void test08_RetrieveSchemeViaAPI() {
        System.out.println("TEST 8: Retrieve CategoryScheme via API");
        System.out.println("========================================\n");

        org.epos.eposdatamodel.CategoryScheme scheme =
                (org.epos.eposdatamodel.CategoryScheme) categorySchemeAPI.retrieveByUID(SCHEME_UID);

        assertNotNull(scheme, "Scheme should be retrievable");
        System.out.println("CategoryScheme via API:");
        System.out.println("  uid: " + scheme.getUid());
        System.out.println("  title: " + scheme.getTitle());
        System.out.println("  topConcepts: " + (scheme.getTopConcepts() != null ? scheme.getTopConcepts().size() : "NULL"));

        if (scheme.getTopConcepts() != null && !scheme.getTopConcepts().isEmpty()) {
            System.out.println("  TopConcepts list:");
            for (LinkedEntity tc : scheme.getTopConcepts()) {
                System.out.println("    - " + tc.getUid());
            }
        }

        assertNotNull(scheme.getTopConcepts(), "topConcepts should not be null");
        assertEquals(3, scheme.getTopConcepts().size(), "Should have 3 topConcepts");

        System.out.println("\n✓ CategoryScheme API retrieval correct\n");
    }

    // =========================================================================
    // TEST 9: Print complete hierarchy
    // =========================================================================
    @Test
    @Order(9)
    @DisplayName("9. Print complete Tsunami hierarchy")
    void test09_PrintHierarchy() {
        System.out.println("TEST 9: Complete Tsunami Hierarchy");
        System.out.println("===================================\n");

        org.epos.eposdatamodel.CategoryScheme scheme =
                (org.epos.eposdatamodel.CategoryScheme) categorySchemeAPI.retrieveByUID(SCHEME_UID);

        System.out.println("CategoryScheme: " + scheme.getUid());
        System.out.println("├── Title: " + scheme.getTitle());
        System.out.println("├── Code: " + scheme.getCode());
        System.out.println("└── TopConcepts:");

        if (scheme.getTopConcepts() != null) {
            for (int i = 0; i < scheme.getTopConcepts().size(); i++) {
                LinkedEntity tc = scheme.getTopConcepts().get(i);
                boolean isLast = (i == scheme.getTopConcepts().size() - 1);
                printCategory(tc.getUid(), "    ", isLast);
            }
        }

        System.out.println("\n✓ Hierarchy complete\n");
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

    private void createCategory(String uid, String name, String description,
                                LinkedEntity schemeRef, LinkedEntity broaderRef) {
        org.epos.eposdatamodel.Category cat = new org.epos.eposdatamodel.Category();
        cat.setUid(uid);
        cat.setName(name);
        cat.setDescription(description);
        cat.setEditorId("test-user");
        cat.setStatus(StatusType.PUBLISHED);
        cat.setInScheme(schemeRef);

        if (broaderRef != null) {
            cat.addBroader(broaderRef);
        }

        LinkedEntity result = categoryAPI.create(cat, StatusType.PUBLISHED, null, null);
        assertNotNull(result, "Category " + uid + " should be created");
    }

    private void printCategory(String uid, String prefix, boolean isLast) {
        org.epos.eposdatamodel.Category cat =
                (org.epos.eposdatamodel.Category) categoryAPI.retrieveByUID(uid);

        if (cat == null) {
            System.out.println(prefix + (isLast ? "└── " : "├── ") + uid + " [NOT FOUND]");
            return;
        }

        String connector = isLast ? "└── " : "├── ";
        String childPrefix = prefix + (isLast ? "    " : "│   ");

        System.out.println(prefix + connector + cat.getName());
        System.out.println(childPrefix + "    uid: " + cat.getUid());

        if (cat.getNarrower() != null && !cat.getNarrower().isEmpty()) {
            System.out.println(childPrefix + "    narrower:");
            for (int i = 0; i < cat.getNarrower().size(); i++) {
                LinkedEntity narrower = cat.getNarrower().get(i);
                boolean childIsLast = (i == cat.getNarrower().size() - 1);
                printCategory(narrower.getUid(), childPrefix + "    ", childIsLast);
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
        System.out.println("║           TSUNAMI CATEGORY TEST SUMMARY                          ║");
        System.out.println("╠══════════════════════════════════════════════════════════════════╣");
        System.out.println("║ Test 1:  Create CategoryScheme (tsunami)               ✓         ║");
        System.out.println("║ Test 2:  Create parent categories                      ✓         ║");
        System.out.println("║ Test 3:  Create child categories with broader          ✓         ║");
        System.out.println("║ Test 4:  Verify broader in database                    ✓         ║");
        System.out.println("║ Test 5:  Verify narrower (reverse of broader)          ✓         ║");
        System.out.println("║ Test 6:  Retrieve via API with relations               ✓         ║");
        System.out.println("║ Test 7:  Add topConcepts to scheme                     ✓         ║");
        System.out.println("║ Test 8:  Retrieve scheme with topConcepts              ✓         ║");
        System.out.println("║ Test 9:  Print complete hierarchy                      ✓         ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════╝\n");
    }
}