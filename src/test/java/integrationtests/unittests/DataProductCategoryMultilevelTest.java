package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class DataProductCategoryMultilevelTest extends TestcontainersLifecycle {

    // Gerarchia: Grandparent -> Parent (Middle) -> Child
    private final String SCHEME_UID = "https://epos-eu.org/vocab/Scheme_" + UUID.randomUUID();
    private final String GRANDPARENT_UID = "https://epos-eu.org/vocab/Grandparent_" + UUID.randomUUID();
    private final String PARENT_UID = "https://epos-eu.org/vocab/Parent_Middle_" + UUID.randomUUID();
    private final String CHILD_UID = "https://epos-eu.org/vocab/Child_" + UUID.randomUUID();

    @BeforeEach
    public void setup() {
        // Pulizia opzionale se necessaria, ma gli UID random garantiscono isolamento
        cleanupDatabase();
    }

    @Test
    public void testMiddleCategoryIsBothChildAndFather() {

        // =================================================================================
        // 1. SETUP SCHEMA
        // =================================================================================
        CategoryScheme scheme = new CategoryScheme();
        scheme.setUid(SCHEME_UID);
        scheme.setTitle("Multilevel Hierarchy Scheme");
        AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(scheme, StatusType.PUBLISHED, null, null);

        LinkedEntity schemeLink = new LinkedEntity();
        schemeLink.setUid(SCHEME_UID);
        schemeLink.setEntityType(EntityNames.CATEGORYSCHEME.name());

        // =================================================================================
        // 2. CREAZIONE LIVELLO 1 (Grandparent)
        // =================================================================================
        Category grandparent = new Category();
        grandparent.setUid(GRANDPARENT_UID);
        grandparent.setName("Level 1: Grandparent");
        grandparent.setInScheme(schemeLink);

        AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name()).create(grandparent, StatusType.PUBLISHED, null, null);

        LinkedEntity grandparentLink = new LinkedEntity();
        grandparentLink.setUid(GRANDPARENT_UID);
        grandparentLink.setEntityType(EntityNames.CATEGORY.name());

        // =================================================================================
        // 3. CREAZIONE LIVELLO 2 (Parent / Middle Node)
        // Questo è il nodo cruciale: è figlio di Grandparent
        // =================================================================================
        Category parent = new Category();
        parent.setUid(PARENT_UID);
        parent.setName("Level 2: Parent (Middle)");
        parent.setInScheme(schemeLink);
        parent.setBroader(List.of(grandparentLink)); // Link verso l'alto

        AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name()).create(parent, StatusType.PUBLISHED, null, null);

        LinkedEntity parentLink = new LinkedEntity();
        parentLink.setUid(PARENT_UID);
        parentLink.setEntityType(EntityNames.CATEGORY.name());

        // =================================================================================
        // 4. CREAZIONE LIVELLO 3 (Child)
        // Questo nodo rende 'Parent' un padre.
        // =================================================================================
        Category child = new Category();
        child.setUid(CHILD_UID);
        child.setName("Level 3: Child");
        child.setInScheme(schemeLink);
        child.setBroader(List.of(parentLink)); // Link verso l'alto (verso Middle)

        AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name()).create(child, StatusType.PUBLISHED, null, null);

        // =================================================================================
        // 5. VERIFICHE SUL NODO INTERMEDIO (Middle)
        // =================================================================================

        Category retrievedMiddleNode = (Category) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                .retrieveByUID(PARENT_UID);

        assertNotNull(retrievedMiddleNode, "La categoria intermedia deve esistere");

        // VERIFICA 1: Deve avere un Broader (essere figlio)
        assertNotNull(retrievedMiddleNode.getBroader(), "Middle deve avere un Broader");
        assertFalse(retrievedMiddleNode.getBroader().isEmpty(), "Lista Broader vuota");
        assertEquals(GRANDPARENT_UID, retrievedMiddleNode.getBroader().get(0).getUid(),
                "Il Broader di Middle deve essere Grandparent");

        // VERIFICA 2: Deve avere un Narrower (essere padre)
        // Questo conferma che la relazione inversa è stata salvata/recuperata correttamente
        assertNotNull(retrievedMiddleNode.getNarrower(), "Middle deve avere Narrower (inversi)");
        assertFalse(retrievedMiddleNode.getNarrower().isEmpty(), "Lista Narrower vuota");

        boolean foundChild = retrievedMiddleNode.getNarrower().stream()
                .anyMatch(cat -> CHILD_UID.equals(cat.getUid()));

        assertTrue(foundChild, "Tra i Narrower di Middle deve esserci Child");

        System.out.println("TEST SUCCESS: La categoria " + PARENT_UID + " è correttamente Padre E Figlio.");
    }

    private void cleanupDatabase() {
        // Implementazione standard di pulizia (simile agli altri test)
        try {
            AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
            List<Category> list = api.retrieveAll();
            for (Category c : list) api.delete(c.getInstanceId());

            AbstractAPI schemeApi = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
            List<CategoryScheme> schemes = schemeApi.retrieveAll();
            for (CategoryScheme s : schemes) schemeApi.delete(s.getInstanceId());
        } catch (Exception e) {}
    }
}