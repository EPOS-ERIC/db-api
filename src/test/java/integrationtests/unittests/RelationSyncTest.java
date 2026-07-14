package integrationtests.unittests;

import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import model.*;
import model.Category;
import org.epos.eposdatamodel.*;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.Operation;
import org.epos.eposdatamodel.Organization;
import org.epos.eposdatamodel.Payload;
import org.epos.eposdatamodel.Person;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import metadataapis.*;
import commonapis.*;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Relation Sync Fix Tests")
public class RelationSyncTest extends TestcontainersLifecycle{

    private static final String TEST_UID_PREFIX = "TEST-" + System.currentTimeMillis() + "-";

    // Stored entities for cross-test usage
    private static LinkedEntity publishedDistribution;
    private static LinkedEntity person1Entity;
    private static LinkedEntity person2Entity;
    private static LinkedEntity category1Entity;
    private static LinkedEntity category2Entity;

    @BeforeAll
    static void setUpAll() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║         RELATION SYNC FIX TESTS - JUnit 5                    ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝\n");
    }

    @AfterAll
    static void tearDownAll() {
        System.out.println("\n✓ All Relation Sync Tests Completed\n");
    }

    // ========================================================================
    // TEST GROUP 1: Distribution Elements Sync
    // ========================================================================

    @Test
    @Order(1)
    @DisplayName("1. Distribution - Elements should be deleted on update")
    void distribution_ElementsAreDeleted_OnUpdate() {
        Distribution dto = new Distribution();
        dto.setUid(TEST_UID_PREFIX + "dist-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addAccessURL("http://old1.com");
        dto.addAccessURL("http://old2.com");

        DistributionAPI api = new DistributionAPI(EntityNames.DISTRIBUTION.name(), model.Distribution.class);
        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        Distribution updateDto = new Distribution();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.addAccessURL("http://new1.com");

        api.create(updateDto, StatusType.DRAFT, null, null);

        Distribution retrieved = api.retrieve(created.getInstanceId());

        assertNotNull(retrieved, "Retrieved distribution should not be null");
        assertEquals(1, retrieved.getAccessURL().size(), "Should have exactly 1 accessURL after update");
        assertTrue(retrieved.getAccessURL().contains("http://new1.com"), "Should contain new URL");
        assertFalse(retrieved.getAccessURL().contains("http://old1.com"), "Should NOT contain old URL 1");
        assertFalse(retrieved.getAccessURL().contains("http://old2.com"), "Should NOT contain old URL 2");

        System.out.println("   ✓ Old elements deleted, new elements added");
    }

    @Test
    @Order(2)
    @DisplayName("2. Distribution - Update with new elements replaces old ones")
    void distribution_UpdateWithNewElements_ReplacesOldOnes() {
        Distribution dto = new Distribution();
        dto.setUid(TEST_UID_PREFIX + "dist-002");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addAccessURL("http://original1.com");
        dto.addAccessURL("http://original2.com");
        dto.addDownloadURL("http://download-original.com");

        DistributionAPI api = new DistributionAPI(EntityNames.DISTRIBUTION.name(), model.Distribution.class);
        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        publishedDistribution = created;

        Distribution initial = api.retrieve(created.getInstanceId());
        assertEquals(2, initial.getAccessURL().size(), "Initial should have 2 accessURLs");
        assertEquals(1, initial.getDownloadURL().size(), "Initial should have 1 downloadURL");

        Distribution updateDto = new Distribution();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.addAccessURL("http://replaced.com");
        updateDto.addDownloadURL("http://download-replaced1.com");
        updateDto.addDownloadURL("http://download-replaced2.com");

        api.create(updateDto, StatusType.DRAFT, null, null);

        Distribution retrieved = api.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getAccessURL().size(), "Should have 1 accessURL after update");
        assertEquals(2, retrieved.getDownloadURL().size(), "Should have 2 downloadURLs after update");
        assertTrue(retrieved.getAccessURL().contains("http://replaced.com"), "Should have new accessURL");
        assertFalse(retrieved.getAccessURL().contains("http://original1.com"), "Should NOT have old accessURL");
        assertTrue(retrieved.getDownloadURL().contains("http://download-replaced1.com"), "Should have new downloadURL 1");
        assertTrue(retrieved.getDownloadURL().contains("http://download-replaced2.com"), "Should have new downloadURL 2");

        System.out.println("   ✓ Elements replaced correctly on update");
    }

    @Test
    @Order(3)
    @DisplayName("3. Person - Telephone/Email elements should sync")
    void person_ElementsSync() {
        Person dto = new Person();
        dto.setUid(TEST_UID_PREFIX + "person-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setGivenName("Test");
        dto.setFamilyName("Person");
        dto.addTelephone("+1-OLD-001");
        dto.addTelephone("+1-OLD-002");
        dto.addEmail("old@test.com");

        PersonAPI api = new PersonAPI(EntityNames.PERSON.name(), model.Person.class);
        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        Person initial = api.retrieve(created.getInstanceId());
        assertEquals(2, initial.getTelephone().size(), "Initial should have 2 telephones");
        assertEquals(1, initial.getEmail().size(), "Initial should have 1 email");

        Person updateDto = new Person();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setGivenName("Test");
        updateDto.setFamilyName("Person");
        updateDto.addTelephone("+1-NEW-001");
        updateDto.addEmail("new1@test.com");
        updateDto.addEmail("new2@test.com");

        api.create(updateDto, StatusType.DRAFT, null, null);

        Person retrieved = api.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getTelephone().size(), "Should have 1 telephone after update");
        assertEquals(2, retrieved.getEmail().size(), "Should have 2 emails after update");
        assertTrue(retrieved.getTelephone().contains("+1-NEW-001"), "Should have new telephone");
        assertFalse(retrieved.getTelephone().contains("+1-OLD-001"), "Should NOT have old telephone");
        assertTrue(retrieved.getEmail().contains("new1@test.com"), "Should have new email 1");
        assertFalse(retrieved.getEmail().contains("old@test.com"), "Should NOT have old email");

        System.out.println("   ✓ Person elements synced correctly");
    }

    @Test
    @Order(4)
    @DisplayName("4. Setup - Create Person entities for polymorphic tests")
    void setup_CreatePersonEntities() {
        PersonAPI personApi = new PersonAPI(EntityNames.PERSON.name(), model.Person.class);

        Person person1 = new Person();
        person1.setUid(TEST_UID_PREFIX + "author-001");
        person1.setEditorId("test");
        person1.setFileProvenance("test");
        person1.setGivenName("Author");
        person1.setFamilyName("One");
        person1Entity = personApi.create(person1, StatusType.PUBLISHED, null, null);

        Person person2 = new Person();
        person2.setUid(TEST_UID_PREFIX + "author-002");
        person2.setEditorId("test");
        person2.setFileProvenance("test");
        person2.setGivenName("Author");
        person2.setFamilyName("Two");
        person2Entity = personApi.create(person2, StatusType.PUBLISHED, null, null);

        assertNotNull(person1Entity, "Person 1 should be created");
        assertNotNull(person2Entity, "Person 2 should be created");

        System.out.println("   ✓ Person entities created for polymorphic tests");
    }

    @Test
    @Order(5)
    @DisplayName("5. Setup - Create Category entities")
    void setup_CreateCategoryEntities() {
        CategoryAPI catApi = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);

        org.epos.eposdatamodel.Category cat1 = new org.epos.eposdatamodel.Category();
        cat1.setUid(TEST_UID_PREFIX + "cat-001");
        cat1.setEditorId("test");
        cat1.setFileProvenance("test");
        cat1.setName("Category 1");
        category1Entity = catApi.create(cat1, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.Category cat2 = new org.epos.eposdatamodel.Category();
        cat2.setUid(TEST_UID_PREFIX + "cat-002");
        cat2.setEditorId("test");
        cat2.setFileProvenance("test");
        cat2.setName("Category 2");
        category2Entity = catApi.create(cat2, StatusType.PUBLISHED, null, null);

        assertNotNull(category1Entity, "Category 1 should be created");
        assertNotNull(category2Entity, "Category 2 should be created");

        System.out.println("   ✓ Category entities created");
    }

    @Test
    @Order(6)
    @DisplayName("6. SoftwareApplication - Polymorphic relations sync")
    void softwareApplication_PolymorphicRelationsSync() {
        assertNotNull(person1Entity, "Person1 should exist from setup");
        assertNotNull(person2Entity, "Person2 should exist from setup");

        SoftwareApplicationAPI saApi = new SoftwareApplicationAPI(
                EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class);

        SoftwareApplication dto = new SoftwareApplication();
        dto.setUid(TEST_UID_PREFIX + "sa-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setName("Test App");
        dto.setDescription("Test Description");
        dto.addAuthor(new LinkedEntity()
                .instanceId(person1Entity.getInstanceId())
                .uid(person1Entity.getUid())
                .entityType(EntityNames.PERSON.name()));
        dto.addCitation("Old Citation");

        LinkedEntity created = saApi.create(dto, StatusType.DRAFT, null, null);

        SoftwareApplication initial = saApi.retrieve(created.getInstanceId());
        assertEquals(1, initial.getAuthor().size(), "Initial should have 1 author");
        assertEquals(1, initial.getCitation().size(), "Initial should have 1 citation");

        SoftwareApplication updateDto = new SoftwareApplication();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setName("Test App");
        updateDto.setDescription("Test Description");
        updateDto.addAuthor(new LinkedEntity()
                .instanceId(person2Entity.getInstanceId())
                .uid(person2Entity.getUid())
                .entityType(EntityNames.PERSON.name()));
        updateDto.addCitation("New Citation");

        saApi.create(updateDto, StatusType.DRAFT, null, null);

        SoftwareApplication retrieved = saApi.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getAuthor().size(), "Should have 1 author after update");

        // CHECK UID instead of InstanceID to tolerate branching
        assertEquals(person2Entity.getUid(), retrieved.getAuthor().get(0).getUid(), "Author should be person2");

        assertEquals(1, retrieved.getCitation().size(), "Should have 1 citation");
        assertTrue(retrieved.getCitation().contains("New Citation"), "Should have new citation");
        assertFalse(retrieved.getCitation().contains("Old Citation"), "Should NOT have old citation");

        System.out.println("   ✓ Polymorphic relations synced correctly");
    }

    @Test
    @Order(7)
    @DisplayName("7. DataProduct - Category relations sync")
    void dataProduct_CategoryRelationsSync() {
        assertNotNull(category1Entity, "Category1 should exist from setup");
        assertNotNull(category2Entity, "Category2 should exist from setup");

        DataProductAPI dpApi = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);

        DataProduct dto = new DataProduct();
        dto.setUid(TEST_UID_PREFIX + "dp-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addTitle("Test DataProduct");
        dto.addCategory(new LinkedEntity()
                .instanceId(category1Entity.getInstanceId())
                .uid(category1Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));

        LinkedEntity created = dpApi.create(dto, StatusType.DRAFT, null, null);

        DataProduct initial = dpApi.retrieve(created.getInstanceId());
        assertEquals(1, initial.getCategory().size(), "Initial should have 1 category");

        DataProduct updateDto = new DataProduct();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.addTitle("Test DataProduct");
        updateDto.addCategory(new LinkedEntity()
                .instanceId(category2Entity.getInstanceId())
                .uid(category2Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));

        dpApi.create(updateDto, StatusType.DRAFT, null, null);

        DataProduct retrieved = dpApi.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getCategory().size(), "Should have 1 category after update");

        // CHECK UID instead of InstanceID
        assertEquals(category2Entity.getUid(), retrieved.getCategory().get(0).getUid(), "Category should be cat2");

        System.out.println("   ✓ DataProduct category relations synced correctly");
    }

    @Test
    @Order(8)
    @DisplayName("8. Payload - Operation relation (extractInstanceId fix)")
    void payload_OperationRelation_NoClassCastException() {
        WebServiceAPI wsApi = new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class);
        WebService wsDto = new WebService();
        wsDto.setUid(TEST_UID_PREFIX + "ws-001");
        wsDto.setEditorId("test");
        wsDto.setFileProvenance("test");
        wsDto.setName("Test WS");
        LinkedEntity ws = wsApi.create(wsDto, StatusType.PUBLISHED, null, null);

        OperationAPI opApi = new OperationAPI(EntityNames.OPERATION.name(), model.Operation.class);
        Operation opDto = new Operation();
        opDto.setUid(TEST_UID_PREFIX + "op-001");
        opDto.setEditorId("test");
        opDto.setFileProvenance("test");
        opDto.setMethod("GET");
        opDto.setTemplate("http://example.com/api");
        opDto.addWebservice(new LinkedEntity()
                .instanceId(ws.getInstanceId())
                .uid(ws.getUid())
                .entityType(EntityNames.WEBSERVICE.name()));
        LinkedEntity op = opApi.create(opDto, StatusType.PUBLISHED, null, null);

        PayloadAPI payloadApi = new PayloadAPI(EntityNames.PAYLOAD.name(), model.Payload.class);
        Payload payloadDto = new Payload();
        payloadDto.setUid(TEST_UID_PREFIX + "payload-001");
        payloadDto.setEditorId("test");
        payloadDto.setFileProvenance("test");
        payloadDto.setSupportedOperation(new LinkedEntity()
                .instanceId(op.getInstanceId())
                .uid(op.getUid())
                .entityType(EntityNames.OPERATION.name()));

        LinkedEntity payload = assertDoesNotThrow(() ->
                        payloadApi.create(payloadDto, StatusType.PUBLISHED, null, null),
                "Should NOT throw ClassCastException"
        );

        Payload retrieved = payloadApi.retrieve(payload.getInstanceId());

        assertNotNull(retrieved, "Payload should be created");
        assertNotNull(retrieved.getSupportedOperation(), "SupportedOperation should not be null");
        assertEquals(op.getUid(), retrieved.getSupportedOperation().getUid(), "SupportedOperation UID should match");

        System.out.println("   ✓ Payload with Operation works (extractInstanceId fix verified)");
    }

    @Test
    @Order(9)
    @DisplayName("9. Distribution - Empty list should delete all elements")
    void distribution_EmptyList_DeletesAllElements() {
        Distribution dto = new Distribution();
        dto.setUid(TEST_UID_PREFIX + "dist-empty-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addAccessURL("http://url1.com");
        dto.addAccessURL("http://url2.com");

        DistributionAPI api = new DistributionAPI(EntityNames.DISTRIBUTION.name(), model.Distribution.class);
        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        Distribution initial = api.retrieve(created.getInstanceId());
        assertEquals(2, initial.getAccessURL().size(), "Initial should have 2 URLs");

        Distribution updateDto = new Distribution();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setAccessURL(new ArrayList<>());

        api.create(updateDto, StatusType.DRAFT, null, null);

        Distribution retrieved = api.retrieve(created.getInstanceId());

        assertTrue(retrieved.getAccessURL() == null || retrieved.getAccessURL().isEmpty(),
                "Should have no accessURLs after update with empty list");

        System.out.println("   ✓ Empty list correctly deletes all elements");
    }

    @Test
    @Order(10)
    @DisplayName("10. DataProduct - Multiple categories add/remove")
    void dataProduct_MultipleCategoriesSync() {
        CategoryAPI catApi = new CategoryAPI(EntityNames.CATEGORY.name(), Category.class);
        org.epos.eposdatamodel.Category cat3 = new org.epos.eposdatamodel.Category();
        cat3.setUid(TEST_UID_PREFIX + "cat-003");
        cat3.setEditorId("test");
        cat3.setFileProvenance("test");
        cat3.setName("Category 3");
        LinkedEntity category3Entity = catApi.create(cat3, StatusType.PUBLISHED, null, null);

        DataProductAPI dpApi = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);

        DataProduct dto = new DataProduct();
        dto.setUid(TEST_UID_PREFIX + "dp-multi-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addTitle("Test Multi-Category DP");
        dto.addCategory(new LinkedEntity()
                .instanceId(category1Entity.getInstanceId())
                .uid(category1Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));
        dto.addCategory(new LinkedEntity()
                .instanceId(category2Entity.getInstanceId())
                .uid(category2Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));

        LinkedEntity created = dpApi.create(dto, StatusType.DRAFT, null, null);

        DataProduct initial = dpApi.retrieve(created.getInstanceId());
        assertEquals(2, initial.getCategory().size(), "Initial should have 2 categories");

        DataProduct updateDto = new DataProduct();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.addTitle("Test Multi-Category DP");
        updateDto.addCategory(new LinkedEntity()
                .instanceId(category2Entity.getInstanceId())
                .uid(category2Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));
        updateDto.addCategory(new LinkedEntity()
                .instanceId(category3Entity.getInstanceId())
                .uid(category3Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));

        dpApi.create(updateDto, StatusType.DRAFT, null, null);

        DataProduct retrieved = dpApi.retrieve(created.getInstanceId());

        assertEquals(2, retrieved.getCategory().size(), "Should have 2 categories after update");

        Set<String> categoryUids = retrieved.getCategory().stream()
                .map(LinkedEntity::getUid)
                .collect(Collectors.toSet());

        assertTrue(categoryUids.contains(category2Entity.getUid()), "Should have cat2");
        assertTrue(categoryUids.contains(category3Entity.getUid()), "Should have cat3");
        assertFalse(categoryUids.contains(category1Entity.getUid()), "Should NOT have cat1");

        System.out.println("   ✓ Multiple categories synced correctly (add/remove)");
    }

    @Test
    @Order(11)
    @DisplayName("11. SoftwareSourceCode - ProgrammingLanguage elements sync")
    void softwareSourceCode_ProgrammingLanguageElementsSync() {
        SoftwareSourceCodeAPI api = new SoftwareSourceCodeAPI(
                EntityNames.SOFTWARESOURCECODE.name(), Softwaresourcecode.class);

        SoftwareSourceCode dto = new SoftwareSourceCode();
        dto.setUid(TEST_UID_PREFIX + "ssc-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setName("Test Source Code");
        dto.setDescription("Test Description");
        dto.addProgrammingLanguage("Java");
        dto.addProgrammingLanguage("Python");
        dto.addProgrammingLanguage("JavaScript");

        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        SoftwareSourceCode initial = api.retrieve(created.getInstanceId());
        assertEquals(3, initial.getProgrammingLanguage().size(), "Initial should have 3 languages");

        SoftwareSourceCode updateDto = new SoftwareSourceCode();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setName("Test Source Code");
        updateDto.setDescription("Test Description");
        updateDto.addProgrammingLanguage("Java");
        updateDto.addProgrammingLanguage("Go");

        api.create(updateDto, StatusType.DRAFT, null, null);

        SoftwareSourceCode retrieved = api.retrieve(created.getInstanceId());

        assertEquals(2, retrieved.getProgrammingLanguage().size(), "Should have 2 languages after update");
        assertTrue(retrieved.getProgrammingLanguage().contains("Java"), "Should have Java");
        assertTrue(retrieved.getProgrammingLanguage().contains("Go"), "Should have Go");
        assertFalse(retrieved.getProgrammingLanguage().contains("Python"), "Should NOT have Python");
        assertFalse(retrieved.getProgrammingLanguage().contains("JavaScript"), "Should NOT have JavaScript");

        System.out.println("   ✓ SoftwareSourceCode programming languages synced correctly");
    }

    @Test
    @Order(12)
    @DisplayName("12. WebService - Category and ContactPoint sync together")
    void webService_CategoryAndContactPointSyncTogether() {
        ContactPointAPI cpApi = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);

        ContactPoint cp1 = new ContactPoint();
        cp1.setUid(TEST_UID_PREFIX + "ws-cp-001");
        cp1.setEditorId("test");
        cp1.setFileProvenance("test");
        cp1.addEmail("contact1@test.com");
        LinkedEntity cp1Entity = cpApi.create(cp1, StatusType.PUBLISHED, null, null);

        ContactPoint cp2 = new ContactPoint();
        cp2.setUid(TEST_UID_PREFIX + "ws-cp-002");
        cp2.setEditorId("test");
        cp2.setFileProvenance("test");
        cp2.addEmail("contact2@test.com");
        LinkedEntity cp2Entity = cpApi.create(cp2, StatusType.PUBLISHED, null, null);

        WebServiceAPI wsApi = new WebServiceAPI(EntityNames.WEBSERVICE.name(), Webservice.class);

        WebService dto = new WebService();
        dto.setUid(TEST_UID_PREFIX + "ws-sync-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setName("Test WebService");
        dto.setDescription("Test Description");
        dto.addCategory(new LinkedEntity()
                .instanceId(category1Entity.getInstanceId())
                .uid(category1Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));
        dto.addContactPoint(new LinkedEntity()
                .instanceId(cp1Entity.getInstanceId())
                .uid(cp1Entity.getUid())
                .entityType(EntityNames.CONTACTPOINT.name()));

        LinkedEntity created = wsApi.create(dto, StatusType.DRAFT, null, null);

        WebService initial = wsApi.retrieve(created.getInstanceId());
        assertEquals(1, initial.getCategory().size(), "Initial should have 1 category");
        assertEquals(1, initial.getContactPoint().size(), "Initial should have 1 contactPoint");

        WebService updateDto = new WebService();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setName("Test WebService");
        updateDto.setDescription("Test Description");
        updateDto.addCategory(new LinkedEntity()
                .instanceId(category2Entity.getInstanceId())
                .uid(category2Entity.getUid())
                .entityType(EntityNames.CATEGORY.name()));
        updateDto.addContactPoint(new LinkedEntity()
                .instanceId(cp2Entity.getInstanceId())
                .uid(cp2Entity.getUid())
                .entityType(EntityNames.CONTACTPOINT.name()));

        wsApi.create(updateDto, StatusType.DRAFT, null, null);

        WebService retrieved = wsApi.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getCategory().size(), "Should have 1 category");
        assertEquals(1, retrieved.getContactPoint().size(), "Should have 1 contactPoint");

        // CHECK UID
        assertEquals(category2Entity.getUid(), retrieved.getCategory().get(0).getUid(), "Category should be cat2");
        assertEquals(cp2Entity.getUid(), retrieved.getContactPoint().get(0).getUid(), "ContactPoint should be cp2");

        System.out.println("   ✓ WebService category and contactPoint synced together correctly");
    }

    @Test
    @Order(13)
    @DisplayName("13. Person - Organization affiliation sync")
    void person_OrganizationAffiliationSync() {
        OrganizationAPI orgApi = new OrganizationAPI(EntityNames.ORGANIZATION.name(), model.Organization.class);

        Organization org1 = new Organization();
        org1.setUid(TEST_UID_PREFIX + "org-aff-001");
        org1.setEditorId("test");
        org1.setFileProvenance("test");
        org1.addLegalName("Organization One");
        LinkedEntity org1Entity = orgApi.create(org1, StatusType.PUBLISHED, null, null);

        Organization org2 = new Organization();
        org2.setUid(TEST_UID_PREFIX + "org-aff-002");
        org2.setEditorId("test");
        org2.setFileProvenance("test");
        org2.addLegalName("Organization Two");
        LinkedEntity org2Entity = orgApi.create(org2, StatusType.PUBLISHED, null, null);

        PersonAPI personApi = new PersonAPI(EntityNames.PERSON.name(), model.Person.class);

        Person dto = new Person();
        dto.setUid(TEST_UID_PREFIX + "person-aff-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setGivenName("Test");
        dto.setFamilyName("Researcher");
        dto.addAffiliation(new LinkedEntity()
                .instanceId(org1Entity.getInstanceId())
                .uid(org1Entity.getUid())
                .entityType(EntityNames.ORGANIZATION.name()));

        LinkedEntity created = personApi.create(dto, StatusType.DRAFT, null, null);

        Person initial = personApi.retrieve(created.getInstanceId());
        assertEquals(1, initial.getAffiliation().size(), "Initial should have 1 affiliation");

        Person updateDto = new Person();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setGivenName("Test");
        updateDto.setFamilyName("Researcher");
        updateDto.addAffiliation(new LinkedEntity()
                .instanceId(org2Entity.getInstanceId())
                .uid(org2Entity.getUid())
                .entityType(EntityNames.ORGANIZATION.name()));

        personApi.create(updateDto, StatusType.DRAFT, null, null);

        Person retrieved = personApi.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getAffiliation().size(), "Should have 1 affiliation");

        // CHECK UID
        assertEquals(org2Entity.getUid(), retrieved.getAffiliation().get(0).getUid(), "Affiliation should be org2");

        System.out.println("   ✓ Person organization affiliation synced correctly");
    }

    @Test
    @Order(14)
    @DisplayName("14. SoftwareApplication - Mixed polymorphic authors (Person + Organization)")
    void softwareApplication_MixedPolymorphicAuthors() {
        OrganizationAPI orgApi = new OrganizationAPI(EntityNames.ORGANIZATION.name(), model.Organization.class);
        Organization org = new Organization();
        org.setUid(TEST_UID_PREFIX + "org-author-001");
        org.setEditorId("test");
        org.setFileProvenance("test");
        org.addLegalName("Research Institute");
        LinkedEntity orgEntity = orgApi.create(org, StatusType.PUBLISHED, null, null);

        SoftwareApplicationAPI saApi = new SoftwareApplicationAPI(
                EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class);

        SoftwareApplication dto = new SoftwareApplication();
        dto.setUid(TEST_UID_PREFIX + "sa-mixed-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setName("Mixed Authors App");
        dto.setDescription("Test Description");
        dto.addAuthor(new LinkedEntity()
                .instanceId(person1Entity.getInstanceId())
                .uid(person1Entity.getUid())
                .entityType(EntityNames.PERSON.name()));

        LinkedEntity created = saApi.create(dto, StatusType.DRAFT, null, null);

        SoftwareApplication initial = saApi.retrieve(created.getInstanceId());
        assertEquals(1, initial.getAuthor().size(), "Initial should have 1 author");

        SoftwareApplication updateDto = new SoftwareApplication();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setName("Mixed Authors App");
        updateDto.setDescription("Test Description");
        updateDto.addAuthor(new LinkedEntity()
                .instanceId(orgEntity.getInstanceId())
                .uid(orgEntity.getUid())
                .entityType(EntityNames.ORGANIZATION.name()));

        saApi.create(updateDto, StatusType.DRAFT, null, null);

        SoftwareApplication retrieved = saApi.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getAuthor().size(), "Should have 1 author");
        assertEquals(EntityNames.ORGANIZATION.name(), retrieved.getAuthor().get(0).getEntityType(), "Author should be ORGANIZATION");

        // CHECK UID
        assertEquals(orgEntity.getUid(), retrieved.getAuthor().get(0).getUid(), "Author UID should match organization");

        System.out.println("   ✓ Mixed polymorphic authors (Person -> Organization) synced correctly");
    }

    @Test
    @Order(15)
    @DisplayName("15. ContactPoint - Email and Telephone elements sync")
    void contactPoint_EmailAndTelephoneElementsSync() {
        ContactPointAPI api = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);

        ContactPoint dto = new ContactPoint();
        dto.setUid(TEST_UID_PREFIX + "cp-elements-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addEmail("old1@contact.com");
        dto.addEmail("old2@contact.com");
        dto.addTelephone("+1-OLD-CP-001");

        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        ContactPoint initial = api.retrieve(created.getInstanceId());
        assertEquals(2, initial.getEmail().size(), "Initial should have 2 emails");
        assertEquals(1, initial.getTelephone().size(), "Initial should have 1 telephone");

        ContactPoint updateDto = new ContactPoint();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.addEmail("new@contact.com");
        updateDto.addTelephone("+1-NEW-CP-001");
        updateDto.addTelephone("+1-NEW-CP-002");

        api.create(updateDto, StatusType.DRAFT, null, null);

        ContactPoint retrieved = api.retrieve(created.getInstanceId());

        assertEquals(1, retrieved.getEmail().size(), "Should have 1 email after update");
        assertEquals(2, retrieved.getTelephone().size(), "Should have 2 telephones after update");
        assertTrue(retrieved.getEmail().contains("new@contact.com"), "Should have new email");
        assertFalse(retrieved.getEmail().contains("old1@contact.com"), "Should NOT have old email 1");
        assertTrue(retrieved.getTelephone().contains("+1-NEW-CP-001"), "Should have new telephone 1");
        assertTrue(retrieved.getTelephone().contains("+1-NEW-CP-002"), "Should have new telephone 2");

        System.out.println("   ✓ ContactPoint email and telephone elements synced correctly");
    }

    @Test
    @Order(16)
    @DisplayName("16. SoftwareSourceCode - RuntimePlatform elements sync")
    void softwareSourceCode_RuntimePlatformSync() {
        SoftwareSourceCodeAPI api = new SoftwareSourceCodeAPI(
                EntityNames.SOFTWARESOURCECODE.name(), Softwaresourcecode.class);

        SoftwareSourceCode dto = new SoftwareSourceCode();
        dto.setUid(TEST_UID_PREFIX + "ssc-runtime-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setName("Multi-Platform Code");
        dto.setDescription("Runs on multiple platforms");
        dto.setCodeRepository("https://github.com/test/multiplatform");
        dto.setRuntimePlatform("JVM");

        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        SoftwareSourceCode initial = api.retrieve(created.getInstanceId());
        assertEquals(dto.getRuntimePlatform(), initial.getRuntimePlatform(), "Initial should have JVM");

        SoftwareSourceCode updateDto = new SoftwareSourceCode();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setName("Multi-Platform Code");
        updateDto.setDescription("Runs on multiple platforms");
        updateDto.setCodeRepository("https://github.com/test/multiplatform");
        updateDto.setRuntimePlatform("PYTHON");

        api.create(updateDto, StatusType.DRAFT, null, null);

        SoftwareSourceCode retrieved = api.retrieve(created.getInstanceId());

        assertEquals(updateDto.getRuntimePlatform(), retrieved.getRuntimePlatform(), "Should have PYTHON after update");

        System.out.println("   ✓ SoftwareSourceCode runtimePlatform elements synced correctly");
    }

    @Test
    @Order(17)
    @DisplayName("17. Organization - Multiple ContactPoints sync")
    void organization_MultipleContactPointsSync() {
        ContactPointAPI cpApi = new ContactPointAPI(EntityNames.CONTACTPOINT.name(), Contactpoint.class);

        ContactPoint cp1 = new ContactPoint();
        cp1.setUid(TEST_UID_PREFIX + "org-cp-001");
        cp1.setEditorId("test");
        cp1.setFileProvenance("test");
        cp1.addEmail("sales@org.com");
        LinkedEntity cp1Entity = cpApi.create(cp1, StatusType.PUBLISHED, null, null);

        ContactPoint cp2 = new ContactPoint();
        cp2.setUid(TEST_UID_PREFIX + "org-cp-002");
        cp2.setEditorId("test");
        cp2.setFileProvenance("test");
        cp2.addEmail("support@org.com");
        LinkedEntity cp2Entity = cpApi.create(cp2, StatusType.PUBLISHED, null, null);

        ContactPoint cp3 = new ContactPoint();
        cp3.setUid(TEST_UID_PREFIX + "org-cp-003");
        cp3.setEditorId("test");
        cp3.setFileProvenance("test");
        cp3.addEmail("hr@org.com");
        LinkedEntity cp3Entity = cpApi.create(cp3, StatusType.PUBLISHED, null, null);

        OrganizationAPI orgApi = new OrganizationAPI(EntityNames.ORGANIZATION.name(), model.Organization.class);

        Organization dto = new Organization();
        dto.setUid(TEST_UID_PREFIX + "org-multi-cp-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addLegalName("Multi Contact Org");
        dto.addContactPoint(new LinkedEntity()
                .instanceId(cp1Entity.getInstanceId())
                .uid(cp1Entity.getUid())
                .entityType(EntityNames.CONTACTPOINT.name()));
        dto.addContactPoint(new LinkedEntity()
                .instanceId(cp2Entity.getInstanceId())
                .uid(cp2Entity.getUid())
                .entityType(EntityNames.CONTACTPOINT.name()));

        LinkedEntity created = orgApi.create(dto, StatusType.DRAFT, null, null);

        Organization initial = orgApi.retrieve(created.getInstanceId());
        assertEquals(2, initial.getContactPoint().size(), "Initial should have 2 contactPoints");

        Organization updateDto = new Organization();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.addLegalName("Multi Contact Org");
        updateDto.addContactPoint(new LinkedEntity()
                .instanceId(cp2Entity.getInstanceId())
                .uid(cp2Entity.getUid())
                .entityType(EntityNames.CONTACTPOINT.name()));
        updateDto.addContactPoint(new LinkedEntity()
                .instanceId(cp3Entity.getInstanceId())
                .uid(cp3Entity.getUid())
                .entityType(EntityNames.CONTACTPOINT.name()));

        orgApi.create(updateDto, StatusType.DRAFT, null, null);

        Organization retrieved = orgApi.retrieve(created.getInstanceId());

        assertEquals(2, retrieved.getContactPoint().size(), "Should have 2 contactPoints after update");

        Set<String> cpUids = retrieved.getContactPoint().stream()
                .map(LinkedEntity::getUid)
                .collect(Collectors.toSet());

        assertTrue(cpUids.contains(cp2Entity.getUid()), "Should have cp2");
        assertTrue(cpUids.contains(cp3Entity.getUid()), "Should have cp3");
        assertFalse(cpUids.contains(cp1Entity.getUid()), "Should NOT have cp1");

        System.out.println("   ✓ Organization multiple ContactPoints synced correctly");
    }

    @Test
    @Order(18)
    @DisplayName("18. SoftwareApplication - Keywords elements sync")
    void softwareApplication_KeywordsSync() {
        SoftwareApplicationAPI api = new SoftwareApplicationAPI(
                EntityNames.SOFTWAREAPPLICATION.name(), Softwareapplication.class);

        SoftwareApplication dto = new SoftwareApplication();
        dto.setUid(TEST_UID_PREFIX + "sa-keywords-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.setName("Keyword Test App");
        dto.setDescription("Testing keyword sync");
        dto.addKeywords("machine-learning");
        dto.addKeywords("AI");
        dto.addKeywords("deep-learning");
        dto.addKeywords("neural-network");

        LinkedEntity created = api.create(dto, StatusType.DRAFT, null, null);

        SoftwareApplication initial = api.retrieve(created.getInstanceId());
        assertEquals(4, Arrays.asList(initial.getKeywords().split(",")).size(), "Initial should have 4 keywords");

        SoftwareApplication updateDto = new SoftwareApplication();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.setName("Keyword Test App");
        updateDto.setDescription("Testing keyword sync");
        updateDto.addKeywords("machine-learning");
        updateDto.addKeywords("AI");
        updateDto.addKeywords("NLP");

        api.create(updateDto, StatusType.DRAFT, null, null);

        SoftwareApplication retrieved = api.retrieve(created.getInstanceId());

        assertEquals(3, Arrays.asList(retrieved.getKeywords().split(",")).size(), "Should have 3 keywords after update");
        assertTrue(retrieved.getKeywords().contains("machine-learning"), "Should have machine-learning");
        assertTrue(retrieved.getKeywords().contains("AI"), "Should have AI");
        assertTrue(retrieved.getKeywords().contains("NLP"), "Should have NLP");
        assertFalse(retrieved.getKeywords().contains("deep-learning"), "Should NOT have deep-learning");
        assertFalse(retrieved.getKeywords().contains("neural-network"), "Should NOT have neural-network");

        System.out.println("   ✓ SoftwareApplication keywords elements synced correctly");
    }

    @Test
    @Order(99)
    @DisplayName("99. DataProduct - Spatial relation is removed on PUT without spatial")
    void dataProduct_SpatialExtentRemoved_OnUpdateWithoutSpatial() {
        SpatialAPI spatialAPI = new SpatialAPI(EntityNames.LOCATION.name(), Spatial.class);
        DataProductAPI dataProductAPI = new DataProductAPI(EntityNames.DATAPRODUCT.name(), Dataproduct.class);

        Location spatial = new Location();
        spatial.setUid(TEST_UID_PREFIX + "dp-spatial-001");
        spatial.setEditorId("test");
        spatial.setFileProvenance("test");
        spatial.setStatus(StatusType.DRAFT);
        spatial.setLocation("POLYGON((0 0, 1 0, 1 1, 0 0))");
        LinkedEntity spatialEntity = spatialAPI.create(spatial, StatusType.DRAFT, null, null);

        DataProduct dto = new DataProduct();
        dto.setUid(TEST_UID_PREFIX + "dp-spatial-remove-001");
        dto.setEditorId("test");
        dto.setFileProvenance("test");
        dto.addTitle("DataProduct with spatial");
        dto.addSpatialExtentItem(new LinkedEntity()
                .instanceId(spatialEntity.getInstanceId())
                .uid(spatialEntity.getUid())
                .entityType(EntityNames.LOCATION.name()));

        LinkedEntity created = dataProductAPI.create(dto, StatusType.DRAFT, null, null);

        DataProduct updateDto = new DataProduct();
        updateDto.setInstanceId(created.getInstanceId());
        updateDto.setMetaId(created.getMetaId());
        updateDto.setUid(created.getUid());
        updateDto.setEditorId("test");
        updateDto.setFileProvenance("test");
        updateDto.addTitle("DataProduct with spatial");

        dataProductAPI.create(updateDto, StatusType.DRAFT, null, null);

        DataProduct retrieved = dataProductAPI.retrieve(created.getInstanceId());
        assertTrue(retrieved.getSpatialExtent() == null || retrieved.getSpatialExtent().isEmpty(),
                "SpatialExtent should be removed when omitted from PUT");

        List<Object> joins = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId("dataproductInstance", created.getInstanceId(), DataproductSpatial.class);
        assertTrue(joins == null || joins.isEmpty(), "DataproductSpatial join should be deleted");

        System.out.println("   ✓ DataProduct spatial relation removed correctly on update without spatial");
    }
}
