package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DataProductCategoryHierarchyTest extends TestcontainersLifecycle {

    // Definiamo UID costanti per tracciare le entità nel test
    private final String SCHEME_UID = "https://epos-eu.org/vocab/TestScheme_" + UUID.randomUUID();
    private final String PARENT_CAT_UID = "https://epos-eu.org/vocab/Parent_" + UUID.randomUUID();
    private final String CHILD_CAT_UID = "https://epos-eu.org/vocab/Child_" + UUID.randomUUID();
    private final String DATAPRODUCT_UID = "https://epos-eu.org/dataproduct/TestDP_" + UUID.randomUUID();

    @Test
    @Order(1)
    public void testCompleteHierarchyWithDirectSchemeRelationship() {

        // =================================================================================
        // FASE 1: Creazione dello Schema (CategoryScheme)
        // =================================================================================
        CategoryScheme scheme = new CategoryScheme();
        scheme.setUid(SCHEME_UID);
        scheme.setTitle("Scientific Domain");
        scheme.setDescription("Vocabulary for scientific domains");

        AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name())
                .create(scheme, StatusType.PUBLISHED, null, null);

        // Prepariamo il Link allo schema da usare nelle categorie
        LinkedEntity schemeLink = new LinkedEntity();
        schemeLink.setUid(SCHEME_UID);
        schemeLink.setEntityType(EntityNames.CATEGORYSCHEME.name());

        // =================================================================================
        // FASE 2: Creazione Categoria Padre (Top Concept)
        // =================================================================================
        Category parentCat = new Category();
        parentCat.setUid(PARENT_CAT_UID);
        parentCat.setName("Seismology (Parent)");
        parentCat.setDescription("Broad domain");

        // RELAZIONE DIRETTA: Category -> inScheme -> CategoryScheme
        parentCat.setInScheme(schemeLink);

        AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .create(parentCat, StatusType.PUBLISHED, null, null);

        // Aggiorniamo lo Schema per includere il Padre come "Top Concept" (Relazione inversa)
        LinkedEntity parentLink = new LinkedEntity();
        parentLink.setUid(PARENT_CAT_UID);
        parentLink.setEntityType(EntityNames.CATEGORY.name());

        scheme.setTopConcepts(List.of(parentLink));
        AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name())
                .create(scheme, StatusType.PUBLISHED, null, null);

        // =================================================================================
        // FASE 3: Creazione Categoria Figlio (Narrower)
        // =================================================================================
        Category childCat = new Category();
        childCat.setUid(CHILD_CAT_UID);
        childCat.setName("Seismic Waveform (Child)");
        childCat.setDescription("Specific data type");

        // RELAZIONE DIRETTA: Anche il figlio appartiene allo schema
        childCat.setInScheme(schemeLink);

        // GERARCHIA: Child -> Broader -> Parent
        childCat.setBroader(List.of(parentLink));

        AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .create(childCat, StatusType.PUBLISHED, null, null);

        // =================================================================================
        // FASE 4: Creazione DataProduct
        // =================================================================================
        DataProduct dp = new DataProduct();
        dp.setUid(DATAPRODUCT_UID);
        dp.setTitle(List.of("Waveform Dataset"));
        dp.setDescription(List.of("A dataset belonging to the child category"));

        // Collega alla categoria figlio
        LinkedEntity childLink = new LinkedEntity();
        childLink.setUid(CHILD_CAT_UID);
        childLink.setEntityType(EntityNames.CATEGORY.name());
        dp.setCategory(List.of(childLink));

        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .create(dp, StatusType.PUBLISHED, null, null);

        // =================================================================================
        // FASE 5: Verifiche (Assertions)
        // =================================================================================

        // 1. Verifica DataProduct -> Categoria
        DataProduct retrievedDP = (DataProduct) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .retrieveByUID(DATAPRODUCT_UID);
        assertNotNull(retrievedDP);
        assertEquals(1, retrievedDP.getCategory().size());
        assertEquals(CHILD_CAT_UID, retrievedDP.getCategory().get(0).getUid());
        System.out.println("CHECK 1: DataProduct linked to Child Category OK");

        // 2. Verifica Categoria Figlio -> Schema e Broader
        Category retrievedChild = (Category) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .retrieveByUID(CHILD_CAT_UID);
        assertNotNull(retrievedChild);

        // Verifica inScheme (Relazione Diretta)
        assertNotNull(retrievedChild.getInScheme(), "Child Category must have inScheme set");
        assertEquals(SCHEME_UID, retrievedChild.getInScheme().getUid());

        // Verifica Broader
        assertNotNull(retrievedChild.getBroader());
        assertFalse(retrievedChild.getBroader().isEmpty());
        assertEquals(PARENT_CAT_UID, retrievedChild.getBroader().get(0).getUid());
        System.out.println("CHECK 2: Child Category relations (inScheme, Broader) OK");

        // 3. Verifica Categoria Padre -> Narrower e inScheme
        Category retrievedParent = (Category) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .retrieveByUID(PARENT_CAT_UID);

        assertNotNull(retrievedParent.getInScheme());
        assertEquals(SCHEME_UID, retrievedParent.getInScheme().getUid());

        // Verifica Narrower (Inverso di Broader)
        assertNotNull(retrievedParent.getNarrower());
        boolean foundChildAsNarrower = retrievedParent.getNarrower().stream()
                .anyMatch(cat -> CHILD_CAT_UID.equals(cat.getUid()));
        assertTrue(foundChildAsNarrower, "Parent should have Child as narrower");
        System.out.println("CHECK 3: Parent Category relations (inScheme, Narrower) OK");

        // 4. Verifica Schema -> Top Concepts
        CategoryScheme retrievedScheme = (CategoryScheme) AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name())
                .retrieveByUID(SCHEME_UID);

        assertNotNull(retrievedScheme.getTopConcepts());
        boolean foundParentAsTop = retrievedScheme.getTopConcepts().stream()
                .anyMatch(cat -> PARENT_CAT_UID.equals(cat.getUid()));
        assertTrue(foundParentAsTop, "Scheme should have Parent Category as Top Concept");
        System.out.println("CHECK 4: Scheme Top Concepts OK");
    }
}