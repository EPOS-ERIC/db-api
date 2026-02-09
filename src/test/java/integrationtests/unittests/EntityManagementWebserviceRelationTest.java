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
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementWebserviceRelationTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetItems() {

        WebService webService = new WebService();
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setUid(UUID.randomUUID().toString());
        webService.setName("Test Webservice");
        webService.setDescription("Test Webservice Description");
        webService.addKeywords("Test");
        webService.addKeywords("Test 2");
        webService.addKeywords("Test 3");
        webService.addKeywords("Test 4");

        LinkedEntity webserviceCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).create(webService, null, null, null);
        System.out.println(webserviceCreated);

        WebService webService2 = new WebService();
        webService2.setInstanceId(UUID.randomUUID().toString());
        webService2.setMetaId(UUID.randomUUID().toString());
        webService2.setUid(UUID.randomUUID().toString());
        webService2.setName("Test Webservice 2");
        webService2.setDescription("Test Webservice Description 2");
        webService2.addWebserviceRelation(webserviceCreated);

        LinkedEntity webserviceCreated2 = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).create(webService2, null, null, null);
        System.out.println(webserviceCreated2);

        WebService retrievedWebservice = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).retrieve(webService2.getInstanceId());

        System.out.println(retrievedWebservice.getWebserviceRelation());

        assertNotNull(retrievedWebservice);
    }

    /**
     * Test case: WebService C is related to both WebService A and WebService B
     * All WebServices are PUBLISHED.
     * 
     * Creates 3 webservices:
     * - webserviceA (PUBLISHED)
     * - webserviceB (PUBLISHED)
     * - webserviceC (PUBLISHED, related to both A and B)
     * 
     * Verifies that webserviceC correctly has relations to both A and B.
     */
    @Test
    @Order(2)
    public void testWebserviceCRelatedToBothAAndB() {
        // Create WebService A (PUBLISHED)
        WebService webserviceA = new WebService();
        webserviceA.setInstanceId(UUID.randomUUID().toString());
        webserviceA.setMetaId(UUID.randomUUID().toString());
        webserviceA.setUid("webservice-a-uid");
        webserviceA.setName("WebService A");
        webserviceA.setDescription("First webservice");
        webserviceA.setStatus(StatusType.PUBLISHED);

        LinkedEntity webserviceACreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webserviceA, StatusType.PUBLISHED, null, null);
        System.out.println("Created WebService A: " + webserviceACreated);

        // Create WebService B (PUBLISHED)
        WebService webserviceB = new WebService();
        webserviceB.setInstanceId(UUID.randomUUID().toString());
        webserviceB.setMetaId(UUID.randomUUID().toString());
        webserviceB.setUid("webservice-b-uid");
        webserviceB.setName("WebService B");
        webserviceB.setDescription("Second webservice");
        webserviceB.setStatus(StatusType.PUBLISHED);

        LinkedEntity webserviceBCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webserviceB, StatusType.PUBLISHED, null, null);
        System.out.println("Created WebService B: " + webserviceBCreated);

        // Create WebService C (PUBLISHED) with relations to both A and B
        WebService webserviceC = new WebService();
        webserviceC.setInstanceId(UUID.randomUUID().toString());
        webserviceC.setMetaId(UUID.randomUUID().toString());
        webserviceC.setUid("webservice-c-uid");
        webserviceC.setName("WebService C");
        webserviceC.setDescription("Third webservice - related to A and B");
        webserviceC.setStatus(StatusType.PUBLISHED);
        webserviceC.addWebserviceRelation(webserviceACreated);
        webserviceC.addWebserviceRelation(webserviceBCreated);

        LinkedEntity webserviceCCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webserviceC, StatusType.PUBLISHED, null, null);
        System.out.println("Created WebService C: " + webserviceCCreated);

        // Retrieve WebService C and verify relations
        WebService retrievedWebserviceC = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(webserviceC.getInstanceId());

        System.out.println("Retrieved WebService C: " + retrievedWebserviceC.getName());
        System.out.println("WebService C status: " + retrievedWebserviceC.getStatus());
        System.out.println("WebService C relations: " + retrievedWebserviceC.getWebserviceRelation());

        // Assertions
        assertNotNull(retrievedWebserviceC, "WebService C should be retrieved");
        assertEquals(StatusType.PUBLISHED, retrievedWebserviceC.getStatus(), 
                "WebService C should be PUBLISHED");
        assertNotNull(retrievedWebserviceC.getWebserviceRelation(), "WebService C should have relations");
        assertEquals(2, retrievedWebserviceC.getWebserviceRelation().size(), 
                "WebService C should have exactly 2 relations");

        // Verify that both A and B are in the relations
        List<String> relatedUids = retrievedWebserviceC.getWebserviceRelation().stream()
                .map(LinkedEntity::getUid)
                .toList();
        
        assertTrue(relatedUids.contains("webservice-a-uid"), 
                "WebService C should be related to WebService A");
        assertTrue(relatedUids.contains("webservice-b-uid"), 
                "WebService C should be related to WebService B");

        // Also verify A and B are PUBLISHED and don't have relations (unidirectional)
        WebService retrievedWebserviceA = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(webserviceA.getInstanceId());
        WebService retrievedWebserviceB = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(webserviceB.getInstanceId());

        assertEquals(StatusType.PUBLISHED, retrievedWebserviceA.getStatus(), 
                "WebService A should be PUBLISHED");
        assertEquals(StatusType.PUBLISHED, retrievedWebserviceB.getStatus(), 
                "WebService B should be PUBLISHED");

        assertTrue(retrievedWebserviceA.getWebserviceRelation() == null || 
                retrievedWebserviceA.getWebserviceRelation().isEmpty(),
                "WebService A should NOT have relations (unidirectional)");
        assertTrue(retrievedWebserviceB.getWebserviceRelation() == null || 
                retrievedWebserviceB.getWebserviceRelation().isEmpty(),
                "WebService B should NOT have relations (unidirectional)");

        System.out.println("Test passed: WebService C (PUBLISHED) is correctly related to both A and B (PUBLISHED)");
    }

    /**
     * Test case that mimics ProcessingDetailsItemGenerationJPA logic:
     * Find all webservices that depend on the target webservice (reverse dependencies).
     * 
     * Given:
     * - webserviceA (PUBLISHED)
     * - webserviceB (PUBLISHED)
     * - webserviceC (PUBLISHED, related to both A and B)
     * 
     * When querying for A's dependencies:
     * - Should return [C] because C depends on A
     * 
     * When querying for B's dependencies:
     * - Should return [C] because C depends on B
     */
    @Test
    @Order(3)
    public void testFindReverseDependencies() {
        // Create WebService A (PUBLISHED)
        WebService webserviceA = new WebService();
        String aInstanceId = UUID.randomUUID().toString();
        webserviceA.setInstanceId(aInstanceId);
        webserviceA.setMetaId(UUID.randomUUID().toString());
        webserviceA.setUid("reverse-test-a-uid");
        webserviceA.setName("Reverse Test WebService A");
        webserviceA.setDescription("Target webservice A");
        webserviceA.setStatus(StatusType.PUBLISHED);

        LinkedEntity wsACreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webserviceA, StatusType.PUBLISHED, null, null);

        // Create WebService B (PUBLISHED)
        WebService webserviceB = new WebService();
        String bInstanceId = UUID.randomUUID().toString();
        webserviceB.setInstanceId(bInstanceId);
        webserviceB.setMetaId(UUID.randomUUID().toString());
        webserviceB.setUid("reverse-test-b-uid");
        webserviceB.setName("Reverse Test WebService B");
        webserviceB.setDescription("Target webservice B");
        webserviceB.setStatus(StatusType.PUBLISHED);

        LinkedEntity wsBCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webserviceB, StatusType.PUBLISHED, null, null);

        // Create WebService C (PUBLISHED) with relations to both A and B
        WebService webserviceC = new WebService();
        String cInstanceId = UUID.randomUUID().toString();
        webserviceC.setInstanceId(cInstanceId);
        webserviceC.setMetaId(UUID.randomUUID().toString());
        webserviceC.setUid("reverse-test-c-uid");
        webserviceC.setName("Reverse Test WebService C");
        webserviceC.setDescription("Depends on A and B");
        webserviceC.setStatus(StatusType.PUBLISHED);
        webserviceC.addWebserviceRelation(wsACreated);
        webserviceC.addWebserviceRelation(wsBCreated);

        LinkedEntity wsCCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webserviceC, StatusType.PUBLISHED, null, null);

        // Now mimic ProcessingDetailsItemGenerationJPA logic
        // Step 1: Get all published webservices with relations
        List<WebService> webservicesWithRelations = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieveAll().stream()
                .filter(item -> ((WebService) item).getStatus() != null 
                        && ((WebService) item).getStatus().equals(StatusType.PUBLISHED))
                .filter(item -> ((WebService) item).getWebserviceRelation() != null
                        && !((WebService) item).getWebserviceRelation().isEmpty())
                .map(item -> (WebService) item)
                .toList();

        System.out.println("\n=== Reverse Dependencies Test ===");
        System.out.println("Webservices with relations (PUBLISHED): " + webservicesWithRelations.size());
        webservicesWithRelations.forEach(ws -> {
            System.out.println("  - " + ws.getName() + " has relations to: " + 
                    ws.getWebserviceRelation().stream().map(LinkedEntity::getUid).toList());
        });

        // Step 2: Find reverse dependencies of A (who depends on A?)
        List<String> reverseDepsOfA = new java.util.ArrayList<>();
        for (WebService ws : webservicesWithRelations) {
            for (LinkedEntity le : ws.getWebserviceRelation()) {
                if (le.getInstanceId().equals(aInstanceId)) {
                    reverseDepsOfA.add(ws.getInstanceId());
                }
            }
        }

        System.out.println("\nReverse dependencies of A: " + reverseDepsOfA);
        
        // Assertions
        assertFalse(webservicesWithRelations.isEmpty(), 
                "Should have at least one webservice with relations");
        
        assertTrue(webservicesWithRelations.stream()
                .anyMatch(ws -> ws.getUid().equals("reverse-test-c-uid")),
                "WebService C should be in the list of webservices with relations");
        
        assertFalse(reverseDepsOfA.isEmpty(), 
                "A should have at least one reverse dependency (C depends on A)");
        
        assertTrue(reverseDepsOfA.contains(cInstanceId), 
                "C should be in the reverse dependencies of A");

        // Step 3: Find reverse dependencies of B (who depends on B?)
        List<String> reverseDepsOfB = new java.util.ArrayList<>();
        for (WebService ws : webservicesWithRelations) {
            for (LinkedEntity le : ws.getWebserviceRelation()) {
                if (le.getInstanceId().equals(bInstanceId)) {
                    reverseDepsOfB.add(ws.getInstanceId());
                }
            }
        }

        System.out.println("Reverse dependencies of B: " + reverseDepsOfB);
        
        assertFalse(reverseDepsOfB.isEmpty(), 
                "B should have at least one reverse dependency (C depends on B)");
        
        assertTrue(reverseDepsOfB.contains(cInstanceId), 
                "C should be in the reverse dependencies of B");

        System.out.println("\nTest passed: Reverse dependencies work correctly!");
    }

    /**
     * Test case based on SWIRRL TTL file structure.
     * 
     * This simulates the real-world scenario from the TTL where:
     * - swirrlapi/jupyternotebook/virtual_access/webservice (Jupyter Notebooks)
     * - swirrlapi/enlighten/virtual_access/webservice (Enlighten Web)
     * - swirrlapi/workflow/virtual_access/webservice (Workflows) - has dct:relation to BOTH above
     * 
     * The TTL defines:
     * <swirrlapi/workflow/virtual_access/webservice> a epos:WebService;
     *     ...
     *     dct:relation <swirrlapi/enlighten/virtual_access/webservice>;
     *     dct:relation <swirrlapi/jupyternotebook/virtual_access/webservice>;
     * 
     * This test verifies that the webserviceRelation is properly created and retrieved.
     */
    @Test
    @Order(4)
    public void testSwirrlApiWebserviceRelationsFromTtl() {
        System.out.println("\n=== SWIRRL TTL WebService Relations Test ===\n");

        // Create WebService: SWIRRL API Jupyter Notebooks (PUBLISHED)
        WebService jupyterNotebookWs = new WebService();
        jupyterNotebookWs.setInstanceId(UUID.randomUUID().toString());
        jupyterNotebookWs.setMetaId(UUID.randomUUID().toString());
        jupyterNotebookWs.setUid("swirrlapi/jupyternotebook/virtual_access/webservice");
        jupyterNotebookWs.setName("SWIRRL API Jupyter Notebooks provided by CYFRONET");
        jupyterNotebookWs.setDescription("API that allows deploying Jupyter notebooks and running CWL workflows on the input directory of these notebooks");
        jupyterNotebookWs.setEntryPoint("http://swirrl.epos.hpc.cyfronet.pl/swirrl-api/");
        jupyterNotebookWs.setLicense("https://creativecommons.org/licenses/by/4.0/");
        jupyterNotebookWs.addKeywords("processing");
        jupyterNotebookWs.addKeywords("swirrl");
        jupyterNotebookWs.addKeywords("jupyter");
        jupyterNotebookWs.addKeywords("notebooks");
        jupyterNotebookWs.setStatus(StatusType.PUBLISHED);

        LinkedEntity jupyterWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(jupyterNotebookWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Jupyter Notebooks WS: " + jupyterWsCreated.getUid());

        // Create WebService: SWIRRL API Enlighten Web (PUBLISHED)
        WebService enlightenWs = new WebService();
        enlightenWs.setInstanceId(UUID.randomUUID().toString());
        enlightenWs.setMetaId(UUID.randomUUID().toString());
        enlightenWs.setUid("swirrlapi/enlighten/virtual_access/webservice");
        enlightenWs.setName("SWIRRL API Enlighten Web provided by CYFRONET");
        enlightenWs.setDescription("API that allows deploying Enlighten Web and running CWL workflows on the input directory of these notebooks");
        enlightenWs.setEntryPoint("http://swirrl.epos.hpc.cyfronet.pl/swirrl-api/");
        enlightenWs.setLicense("https://creativecommons.org/licenses/by/4.0/");
        enlightenWs.addKeywords("processing");
        enlightenWs.addKeywords("swirrl");
        enlightenWs.addKeywords("enlighten");
        enlightenWs.setStatus(StatusType.PUBLISHED);

        LinkedEntity enlightenWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(enlightenWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Enlighten Web WS: " + enlightenWsCreated.getUid());

        // Create WebService: SWIRRL API Workflows (PUBLISHED) - with relations to both above
        WebService workflowWs = new WebService();
        workflowWs.setInstanceId(UUID.randomUUID().toString());
        workflowWs.setMetaId(UUID.randomUUID().toString());
        workflowWs.setUid("swirrlapi/workflow/virtual_access/webservice");
        workflowWs.setName("SWIRRL API Workflows provided by CYFRONET");
        workflowWs.setDescription("API that allows deploying Workflows and running CWL workflows on the input directory of these notebooks");
        workflowWs.setEntryPoint("http://swirrl.epos.hpc.cyfronet.pl/swirrl-api/");
        workflowWs.setLicense("https://creativecommons.org/licenses/by/4.0/");
        workflowWs.addKeywords("processing");
        workflowWs.addKeywords("swirrl");
        workflowWs.addKeywords("workflow");
        workflowWs.setStatus(StatusType.PUBLISHED);
        
        // Add dct:relation to both Enlighten and Jupyter Notebooks (as per TTL)
        workflowWs.addWebserviceRelation(enlightenWsCreated);
        workflowWs.addWebserviceRelation(jupyterWsCreated);

        LinkedEntity workflowWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(workflowWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Workflow WS: " + workflowWsCreated.getUid());

        // Retrieve the Workflow WebService and verify relations
        WebService retrievedWorkflowWs = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(workflowWs.getInstanceId());

        System.out.println("\n--- Verification ---");
        System.out.println("Retrieved Workflow WS: " + retrievedWorkflowWs.getName());
        System.out.println("Status: " + retrievedWorkflowWs.getStatus());
        System.out.println("WebserviceRelation: " + retrievedWorkflowWs.getWebserviceRelation());

        // Assertions
        assertNotNull(retrievedWorkflowWs, "Workflow WebService should be retrieved");
        assertEquals(StatusType.PUBLISHED, retrievedWorkflowWs.getStatus(), 
                "Workflow WebService should be PUBLISHED");
        assertNotNull(retrievedWorkflowWs.getWebserviceRelation(), 
                "Workflow WebService should have webserviceRelation");
        assertEquals(2, retrievedWorkflowWs.getWebserviceRelation().size(), 
                "Workflow WebService should have exactly 2 relations (Enlighten + Jupyter)");

        // Verify the UIDs of related webservices match the TTL structure
        List<String> relatedUids = retrievedWorkflowWs.getWebserviceRelation().stream()
                .map(LinkedEntity::getUid)
                .toList();
        
        System.out.println("Related UIDs: " + relatedUids);

        assertTrue(relatedUids.contains("swirrlapi/enlighten/virtual_access/webservice"), 
                "Workflow WS should be related to Enlighten WS");
        assertTrue(relatedUids.contains("swirrlapi/jupyternotebook/virtual_access/webservice"), 
                "Workflow WS should be related to Jupyter Notebooks WS");

        // Verify that Jupyter and Enlighten don't have relations (unidirectional)
        WebService retrievedJupyterWs = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(jupyterNotebookWs.getInstanceId());
        WebService retrievedEnlightenWs = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(enlightenWs.getInstanceId());

        assertTrue(retrievedJupyterWs.getWebserviceRelation() == null || 
                retrievedJupyterWs.getWebserviceRelation().isEmpty(),
                "Jupyter Notebooks WS should NOT have relations (unidirectional)");
        assertTrue(retrievedEnlightenWs.getWebserviceRelation() == null || 
                retrievedEnlightenWs.getWebserviceRelation().isEmpty(),
                "Enlighten WS should NOT have relations (unidirectional)");

        System.out.println("\nTest PASSED: SWIRRL WebService relations work as expected!");
    }

    /**
     * Test case that simulates TTL parsing order issue.
     * 
     * In TTL files, the Workflow webservice (which has dct:relation) might be defined
     * BEFORE the webservices it relates to. This tests if the relation can be established
     * when using UID-based lookup (LinkedEntity with only UID set).
     */
    @Test
    @Order(5)
    public void testSwirrlApiRelationWithUidOnlyLookup() {
        System.out.println("\n=== SWIRRL TTL WebService Relations (UID Lookup) Test ===\n");

        // First, create the target webservices (Jupyter and Enlighten)
        WebService jupyterNotebookWs = new WebService();
        jupyterNotebookWs.setInstanceId(UUID.randomUUID().toString());
        jupyterNotebookWs.setMetaId(UUID.randomUUID().toString());
        jupyterNotebookWs.setUid("swirrlapi/jupyternotebook/uid-test");
        jupyterNotebookWs.setName("SWIRRL API Jupyter Notebooks (UID Test)");
        jupyterNotebookWs.setDescription("Jupyter Notebooks for UID lookup test");
        jupyterNotebookWs.setStatus(StatusType.PUBLISHED);

        LinkedEntity jupyterWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(jupyterNotebookWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Jupyter WS with UID: " + jupyterWsCreated.getUid());

        WebService enlightenWs = new WebService();
        enlightenWs.setInstanceId(UUID.randomUUID().toString());
        enlightenWs.setMetaId(UUID.randomUUID().toString());
        enlightenWs.setUid("swirrlapi/enlighten/uid-test");
        enlightenWs.setName("SWIRRL API Enlighten Web (UID Test)");
        enlightenWs.setDescription("Enlighten Web for UID lookup test");
        enlightenWs.setStatus(StatusType.PUBLISHED);

        LinkedEntity enlightenWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(enlightenWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Enlighten WS with UID: " + enlightenWsCreated.getUid());

        // Now create Workflow WS with relations using ONLY UID (simulating TTL parsing)
        // In TTL, relations are defined by UID reference only, not instanceId
        LinkedEntity jupyterRef = new LinkedEntity();
        jupyterRef.setUid("swirrlapi/jupyternotebook/uid-test");
        jupyterRef.setEntityType(EntityNames.WEBSERVICE.name());

        LinkedEntity enlightenRef = new LinkedEntity();
        enlightenRef.setUid("swirrlapi/enlighten/uid-test");
        enlightenRef.setEntityType(EntityNames.WEBSERVICE.name());

        WebService workflowWs = new WebService();
        workflowWs.setInstanceId(UUID.randomUUID().toString());
        workflowWs.setMetaId(UUID.randomUUID().toString());
        workflowWs.setUid("swirrlapi/workflow/uid-test");
        workflowWs.setName("SWIRRL API Workflows (UID Test)");
        workflowWs.setDescription("Workflow with UID-only relations");
        workflowWs.setStatus(StatusType.PUBLISHED);
        workflowWs.addWebserviceRelation(enlightenRef);
        workflowWs.addWebserviceRelation(jupyterRef);

        LinkedEntity workflowWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(workflowWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Workflow WS with UID: " + workflowWsCreated.getUid());

        // Retrieve and verify
        WebService retrievedWorkflowWs = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(workflowWs.getInstanceId());

        System.out.println("\n--- Verification (UID Lookup) ---");
        System.out.println("Retrieved Workflow WS: " + retrievedWorkflowWs.getName());
        System.out.println("WebserviceRelation: " + retrievedWorkflowWs.getWebserviceRelation());

        // This is the critical assertion - does UID-only lookup work?
        assertNotNull(retrievedWorkflowWs.getWebserviceRelation(), 
                "Workflow WebService should have webserviceRelation (UID lookup)");
        assertEquals(2, retrievedWorkflowWs.getWebserviceRelation().size(), 
                "Workflow WebService should have exactly 2 relations when using UID-only lookup");

        List<String> relatedUids = retrievedWorkflowWs.getWebserviceRelation().stream()
                .map(LinkedEntity::getUid)
                .toList();

        assertTrue(relatedUids.contains("swirrlapi/enlighten/uid-test"), 
                "Should be related to Enlighten (via UID lookup)");
        assertTrue(relatedUids.contains("swirrlapi/jupyternotebook/uid-test"), 
                "Should be related to Jupyter (via UID lookup)");

        System.out.println("\nTest PASSED: UID-only lookup for webservice relations works!");
    }

    /**
     * Test case for deferred/pending webserviceRelation creation.
     * 
     * This simulates real TTL ingestion where the order of entities in the file
     * might cause the Workflow webservice (which has dct:relation) to be processed
     * BEFORE the Jupyter/Enlighten webservices exist.
     * 
     * The test verifies that:
     * 1. When a webservice with relations is created before its targets exist,
     *    pending relations are created
     * 2. When the target webservices are created later, the pending relations
     *    are resolved and the actual WebserviceRelation join records are created
     */
    @Test
    @Order(6)
    public void testWebserviceRelationWithDeferredCreation() {
        System.out.println("\n=== BUG TEST: WebService Relations with Deferred Creation ===\n");

        // Step 1: Create references to webservices that DON'T EXIST YET
        LinkedEntity jupyterRef = new LinkedEntity();
        jupyterRef.setUid("deferred/jupyternotebook/webservice");
        jupyterRef.setEntityType(EntityNames.WEBSERVICE.name());

        LinkedEntity enlightenRef = new LinkedEntity();
        enlightenRef.setUid("deferred/enlighten/webservice");
        enlightenRef.setEntityType(EntityNames.WEBSERVICE.name());

        // Step 2: Create Workflow webservice FIRST (with relations to non-existent webservices)
        WebService workflowWs = new WebService();
        workflowWs.setInstanceId(UUID.randomUUID().toString());
        workflowWs.setMetaId(UUID.randomUUID().toString());
        workflowWs.setUid("deferred/workflow/webservice");
        workflowWs.setName("SWIRRL API Workflows (Deferred Test)");
        workflowWs.setDescription("Workflow created BEFORE related webservices exist");
        workflowWs.setStatus(StatusType.PUBLISHED);
        workflowWs.addWebserviceRelation(enlightenRef);
        workflowWs.addWebserviceRelation(jupyterRef);

        System.out.println("Creating Workflow WS with relations to non-existent webservices...");
        LinkedEntity workflowWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(workflowWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Workflow WS: " + workflowWsCreated.getUid());

        // Step 3: Now create the target webservices AFTER the workflow
        WebService jupyterWs = new WebService();
        jupyterWs.setInstanceId(UUID.randomUUID().toString());
        jupyterWs.setMetaId(UUID.randomUUID().toString());
        jupyterWs.setUid("deferred/jupyternotebook/webservice");
        jupyterWs.setName("SWIRRL API Jupyter Notebooks (Deferred Test)");
        jupyterWs.setDescription("Jupyter Notebooks created AFTER workflow");
        jupyterWs.setStatus(StatusType.PUBLISHED);

        LinkedEntity jupyterWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(jupyterWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Jupyter WS: " + jupyterWsCreated.getUid());

        WebService enlightenWs = new WebService();
        enlightenWs.setInstanceId(UUID.randomUUID().toString());
        enlightenWs.setMetaId(UUID.randomUUID().toString());
        enlightenWs.setUid("deferred/enlighten/webservice");
        enlightenWs.setName("SWIRRL API Enlighten Web (Deferred Test)");
        enlightenWs.setDescription("Enlighten Web created AFTER workflow");
        enlightenWs.setStatus(StatusType.PUBLISHED);

        LinkedEntity enlightenWsCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(enlightenWs, StatusType.PUBLISHED, null, null);
        System.out.println("Created Enlighten WS: " + enlightenWsCreated.getUid());

        // Step 4: Retrieve the Workflow and check if relations were established
        WebService retrievedWorkflowWs = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(workflowWs.getInstanceId());

        System.out.println("\n--- Verification (Deferred Creation) ---");
        System.out.println("Retrieved Workflow WS: " + retrievedWorkflowWs.getName());
        System.out.println("WebserviceRelation: " + retrievedWorkflowWs.getWebserviceRelation());

        // Verify that the pending relations were resolved
        assertNotNull(retrievedWorkflowWs.getWebserviceRelation(), 
                "Workflow WebService should have webserviceRelation (deferred creation)");
        assertEquals(2, retrievedWorkflowWs.getWebserviceRelation().size(), 
                "Workflow WebService should have 2 relations after deferred creation");
        
        // Verify the UIDs are correct
        List<String> relatedUids = retrievedWorkflowWs.getWebserviceRelation().stream()
                .map(LinkedEntity::getUid)
                .toList();
        
        assertTrue(relatedUids.contains("deferred/enlighten/webservice"), 
                "Should be related to Enlighten (via deferred creation)");
        assertTrue(relatedUids.contains("deferred/jupyternotebook/webservice"), 
                "Should be related to Jupyter (via deferred creation)");

        System.out.println("\nTest PASSED: Deferred webservice relations work correctly!");
    }

    @Test
    @Order(7)
    public void testCreateAndGetItems2() {


        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(UUID.randomUUID().toString());
        le.setMetaId(UUID.randomUUID().toString());
        le.setUid(UUID.randomUUID().toString());
        le.setEntityType(EntityNames.OPERATION.name());

        WebService webService = new WebService();
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setUid(UUID.randomUUID().toString());
        webService.setName("Test Webservice");
        webService.setDescription("Test Webservice Description");
        webService.addKeywords("Test");
        webService.addKeywords("Test 2");
        webService.addKeywords("Test 3");
        webService.addKeywords("Test 4");
        webService.addSupportedOperation(le);


        Operation operation = new Operation();
        operation.setInstanceId(le.getInstanceId());
        operation.setMetaId(le.getMetaId());
        operation.setUid(le.getUid());
        operation.setTemplate("Test Webservice 2");

        AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).create(webService, null, null, null);
        AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).create(operation, null, null, null);

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveAll()){
            System.out.println(object);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).retrieveAll()){
            System.out.println(object);
        }
    }


}
