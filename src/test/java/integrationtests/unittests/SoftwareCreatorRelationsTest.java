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

/**
 * Comprehensive test for SoftwareApplication and SoftwareSourceCode entities,
 * including polymorphic creator relations (creator, author, maintainer, publisher, provider, funder, contributor).
 * 
 * IMPORTANT DATABASE CONSTRAINT:
 * The polymorphic relation tables (e.g., softwaresourcecode_creator) have a unique constraint on
 * (entity_instance_id, resource_entity). This means:
 * - A Person/Organization can only be a creator of ONE software entity globally (not per software)
 * - Each test must create unique Person/Organization entities to avoid constraint violations
 * 
 * These tests verify:
 * 1. Basic creation and retrieval of software entities with creator relations
 * 2. Linking software entities to Person and Organization creators
 * 3. Different creator relation types on the same software entity
 * 4. Proper retrieval of creator relations after persistence
 */
public class SoftwareCreatorRelationsTest extends TestcontainersLifecycle {

    // ==================== Helper Methods ====================

    private LinkedEntity createPerson(String familyName, String givenName, String orcid) {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.PERSON.name());
        
        Person person = new Person();
        person.setInstanceId(UUID.randomUUID().toString());
        person.setMetaId(UUID.randomUUID().toString());
        person.setUid(orcid);
        person.setFamilyName(familyName);
        person.setGivenName(givenName);
        
        return api.create(person, null, null, null);
    }

    private LinkedEntity createOrganization(String acronym, String legalName, String rorId) {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());
        
        Organization org = new Organization();
        org.setInstanceId(UUID.randomUUID().toString());
        org.setMetaId(UUID.randomUUID().toString());
        org.setUid(rorId);
        org.setAcronym(acronym);
        org.setLegalName(List.of(legalName));
        
        return api.create(org, null, null, null);
    }

    private LinkedEntity createLinkedEntity(LinkedEntity source, String entityType) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(source.getInstanceId());
        le.setMetaId(source.getMetaId());
        le.setUid(source.getUid());
        le.setEntityType(entityType);
        return le;
    }

    // ==================== SoftwareSourceCode Tests ====================

    @Test
    @Order(1)
    public void testSoftwareSourceCodeWithPersonCreator() {
        // Create a unique person for this test
        LinkedEntity personLE = createPerson("Smith", "John", "https://orcid.org/0000-0001-" + UUID.randomUUID().toString().substring(0, 8));
        
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());

        LinkedEntity creatorLink = createLinkedEntity(personLE, EntityNames.PERSON.name());

        SoftwareSourceCode ssc = new SoftwareSourceCode();
        ssc.setInstanceId(UUID.randomUUID().toString());
        ssc.setMetaId(UUID.randomUUID().toString());
        ssc.setUid("https://github.com/epos-eu/ssc-person-creator-" + UUID.randomUUID());
        ssc.setName("SSC with Person Creator");
        ssc.setDescription("A test software source code with a person creator");
        ssc.setCodeRepository("https://github.com/epos-eu/test-repo");
        ssc.setSoftwareVersion("1.0.0");
        ssc.setProgrammingLanguage(List.of("Java", "Python"));
        ssc.setCreator(List.of(creatorLink));

        LinkedEntity result = api.create(ssc, null, null, null);
        assertNotNull(result, "Create result should not be null");

        // Retrieve and verify
        SoftwareSourceCode retrieved = (SoftwareSourceCode) api.retrieve(ssc.getInstanceId());
        LOG.info("Retrieved SoftwareSourceCode with person creator: " + retrieved);

        assertNotNull(retrieved);
        assertEquals(ssc.getName(), retrieved.getName());
        assertEquals(ssc.getDescription(), retrieved.getDescription());
        assertEquals(ssc.getCodeRepository(), retrieved.getCodeRepository());
        
        // Verify creator relation
        assertNotNull(retrieved.getCreator(), "Creator list should not be null");
        assertEquals(1, retrieved.getCreator().size(), "Should have exactly 1 creator");
        assertEquals(EntityNames.PERSON.name(), retrieved.getCreator().get(0).getEntityType());
        assertEquals(personLE.getInstanceId(), retrieved.getCreator().get(0).getInstanceId());
    }

    @Test
    @Order(2)
    public void testSoftwareSourceCodeWithOrganizationCreator() {
        // Create a unique organization for this test
        LinkedEntity orgLE = createOrganization("EPOS", "European Plate Observing System", "https://ror.org/" + UUID.randomUUID().toString().substring(0, 8));

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());

        LinkedEntity creatorLink = createLinkedEntity(orgLE, EntityNames.ORGANIZATION.name());

        SoftwareSourceCode ssc = new SoftwareSourceCode();
        ssc.setInstanceId(UUID.randomUUID().toString());
        ssc.setMetaId(UUID.randomUUID().toString());
        ssc.setUid("https://github.com/epos-eu/ssc-org-creator-" + UUID.randomUUID());
        ssc.setName("SSC with Organization Creator");
        ssc.setDescription("A test software source code with an organization creator");
        ssc.setCodeRepository("https://github.com/epos-eu/test-repo-org");
        ssc.setSoftwareVersion("1.0.0");
        ssc.setCreator(List.of(creatorLink));

        LinkedEntity result = api.create(ssc, null, null, null);
        assertNotNull(result);

        // Retrieve and verify
        SoftwareSourceCode retrieved = (SoftwareSourceCode) api.retrieve(ssc.getInstanceId());
        LOG.info("Retrieved SoftwareSourceCode with organization creator: " + retrieved);

        assertNotNull(retrieved);
        assertNotNull(retrieved.getCreator(), "Creator list should not be null");
        assertEquals(1, retrieved.getCreator().size(), "Should have exactly 1 creator");
        assertEquals(EntityNames.ORGANIZATION.name(), retrieved.getCreator().get(0).getEntityType());
        assertEquals(orgLE.getInstanceId(), retrieved.getCreator().get(0).getInstanceId());
    }

    @Test
    @Order(3)
    public void testSoftwareSourceCodeWithAllRelationTypes() {
        // Create unique entities for each relation type to avoid constraint violations
        LinkedEntity creatorPerson = createPerson("Creator", "Alice", "https://orcid.org/creator-" + UUID.randomUUID());
        LinkedEntity authorPerson = createPerson("Author", "Bob", "https://orcid.org/author-" + UUID.randomUUID());
        LinkedEntity maintainerOrg = createOrganization("MAINT", "Maintainer Org", "https://ror.org/maintainer-" + UUID.randomUUID());
        LinkedEntity publisherOrg = createOrganization("PUB", "Publisher Org", "https://ror.org/publisher-" + UUID.randomUUID());
        LinkedEntity providerOrg = createOrganization("PROV", "Provider Org", "https://ror.org/provider-" + UUID.randomUUID());
        LinkedEntity funderOrg = createOrganization("FUND", "Funder Org", "https://ror.org/funder-" + UUID.randomUUID());
        LinkedEntity contributorPerson = createPerson("Contributor", "Charlie", "https://orcid.org/contributor-" + UUID.randomUUID());

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());

        SoftwareSourceCode ssc = new SoftwareSourceCode();
        ssc.setInstanceId(UUID.randomUUID().toString());
        ssc.setMetaId(UUID.randomUUID().toString());
        ssc.setUid("https://github.com/epos-eu/ssc-all-relations-" + UUID.randomUUID());
        ssc.setName("SSC with All Relation Types");
        ssc.setDescription("A test software with all relation types");
        ssc.setCodeRepository("https://github.com/epos-eu/test-all-relations");
        ssc.setSoftwareVersion("3.0.0");
        ssc.setLicenseURL("https://opensource.org/licenses/MIT");
        ssc.setCreator(List.of(createLinkedEntity(creatorPerson, EntityNames.PERSON.name())));
        ssc.setAuthor(List.of(createLinkedEntity(authorPerson, EntityNames.PERSON.name())));
        ssc.setMaintainer(List.of(createLinkedEntity(maintainerOrg, EntityNames.ORGANIZATION.name())));
        ssc.setPublisher(List.of(createLinkedEntity(publisherOrg, EntityNames.ORGANIZATION.name())));
        ssc.setProvider(List.of(createLinkedEntity(providerOrg, EntityNames.ORGANIZATION.name())));
        ssc.setFunder(List.of(createLinkedEntity(funderOrg, EntityNames.ORGANIZATION.name())));
        ssc.setContributor(List.of(createLinkedEntity(contributorPerson, EntityNames.PERSON.name())));

        LinkedEntity result = api.create(ssc, null, null, null);
        assertNotNull(result);

        // Retrieve and verify all relations
        SoftwareSourceCode retrieved = (SoftwareSourceCode) api.retrieve(ssc.getInstanceId());
        LOG.info("Retrieved SoftwareSourceCode with all relation types: " + retrieved);

        assertNotNull(retrieved);
        
        // Verify all relation types are present
        assertAll("All relation types should be present",
                () -> assertNotNull(retrieved.getCreator(), "Creator should not be null"),
                () -> assertEquals(1, retrieved.getCreator().size(), "Should have 1 creator"),
                
                () -> assertNotNull(retrieved.getAuthor(), "Author should not be null"),
                () -> assertEquals(1, retrieved.getAuthor().size(), "Should have 1 author"),
                
                () -> assertNotNull(retrieved.getMaintainer(), "Maintainer should not be null"),
                () -> assertEquals(1, retrieved.getMaintainer().size(), "Should have 1 maintainer"),
                
                () -> assertNotNull(retrieved.getPublisher(), "Publisher should not be null"),
                () -> assertEquals(1, retrieved.getPublisher().size(), "Should have 1 publisher"),
                
                () -> assertNotNull(retrieved.getProvider(), "Provider should not be null"),
                () -> assertEquals(1, retrieved.getProvider().size(), "Should have 1 provider"),
                
                () -> assertNotNull(retrieved.getFunder(), "Funder should not be null"),
                () -> assertEquals(1, retrieved.getFunder().size(), "Should have 1 funder"),
                
                () -> assertNotNull(retrieved.getContributor(), "Contributor should not be null"),
                () -> assertEquals(1, retrieved.getContributor().size(), "Should have 1 contributor")
        );

        // Verify specific entity types
        assertEquals(EntityNames.PERSON.name(), retrieved.getCreator().get(0).getEntityType());
        assertEquals(EntityNames.PERSON.name(), retrieved.getAuthor().get(0).getEntityType());
        assertEquals(EntityNames.ORGANIZATION.name(), retrieved.getMaintainer().get(0).getEntityType());
        assertEquals(EntityNames.ORGANIZATION.name(), retrieved.getPublisher().get(0).getEntityType());
    }

    // ==================== SoftwareApplication Tests ====================

    @Test
    @Order(4)
    public void testSoftwareApplicationWithPersonCreator() {
        // Create a unique person for this test
        LinkedEntity personLE = createPerson("AppCreator", "David", "https://orcid.org/app-creator-" + UUID.randomUUID());

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());

        LinkedEntity creatorLink = createLinkedEntity(personLE, EntityNames.PERSON.name());

        SoftwareApplication sa = new SoftwareApplication();
        sa.setInstanceId(UUID.randomUUID().toString());
        sa.setMetaId(UUID.randomUUID().toString());
        sa.setUid("https://software.example.org/sa-person-creator-" + UUID.randomUUID());
        sa.setName("SA with Person Creator");
        sa.setDescription("A test application with a person creator");
        sa.setSoftwareVersion("1.0.0");
        sa.setDownloadURL("https://download.example.org/app.zip");
        sa.setCreator(List.of(creatorLink));

        LinkedEntity result = api.create(sa, null, null, null);
        assertNotNull(result);

        // Retrieve and verify
        SoftwareApplication retrieved = (SoftwareApplication) api.retrieve(sa.getInstanceId());
        LOG.info("Retrieved SoftwareApplication with person creator: " + retrieved);

        assertNotNull(retrieved);
        assertEquals(sa.getName(), retrieved.getName());
        assertEquals(sa.getDescription(), retrieved.getDescription());
        
        // Verify creator relation
        assertNotNull(retrieved.getCreator(), "Creator list should not be null");
        assertEquals(1, retrieved.getCreator().size(), "Should have exactly 1 creator");
        assertEquals(EntityNames.PERSON.name(), retrieved.getCreator().get(0).getEntityType());
        assertEquals(personLE.getInstanceId(), retrieved.getCreator().get(0).getInstanceId());
    }

    @Test
    @Order(5)
    public void testSoftwareApplicationWithOrganizationCreator() {
        // Create a unique organization for this test
        LinkedEntity orgLE = createOrganization("APPORG", "App Creator Org", "https://ror.org/app-org-" + UUID.randomUUID());

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());

        LinkedEntity creatorLink = createLinkedEntity(orgLE, EntityNames.ORGANIZATION.name());

        SoftwareApplication sa = new SoftwareApplication();
        sa.setInstanceId(UUID.randomUUID().toString());
        sa.setMetaId(UUID.randomUUID().toString());
        sa.setUid("https://software.example.org/sa-org-creator-" + UUID.randomUUID());
        sa.setName("SA with Organization Creator");
        sa.setDescription("A test application with an organization creator");
        sa.setSoftwareVersion("2.0.0");
        sa.setCreator(List.of(creatorLink));

        LinkedEntity result = api.create(sa, null, null, null);
        assertNotNull(result);

        // Retrieve and verify
        SoftwareApplication retrieved = (SoftwareApplication) api.retrieve(sa.getInstanceId());
        LOG.info("Retrieved SoftwareApplication with organization creator: " + retrieved);

        assertNotNull(retrieved);
        assertNotNull(retrieved.getCreator(), "Creator list should not be null");
        assertEquals(1, retrieved.getCreator().size(), "Should have exactly 1 creator");
        assertEquals(EntityNames.ORGANIZATION.name(), retrieved.getCreator().get(0).getEntityType());
        assertEquals(orgLE.getInstanceId(), retrieved.getCreator().get(0).getInstanceId());
    }

    @Test
    @Order(6)
    public void testSoftwareApplicationWithAllRelationTypes() {
        // Create unique entities for each relation type
        LinkedEntity creatorOrg = createOrganization("SACREATOR", "SA Creator Org", "https://ror.org/sa-creator-" + UUID.randomUUID());
        LinkedEntity authorPerson = createPerson("SAAuthor", "Eve", "https://orcid.org/sa-author-" + UUID.randomUUID());
        LinkedEntity maintainerPerson = createPerson("SAMaintainer", "Frank", "https://orcid.org/sa-maintainer-" + UUID.randomUUID());
        LinkedEntity publisherOrg = createOrganization("SAPUB", "SA Publisher Org", "https://ror.org/sa-publisher-" + UUID.randomUUID());
        LinkedEntity providerOrg = createOrganization("SAPROV", "SA Provider Org", "https://ror.org/sa-provider-" + UUID.randomUUID());
        LinkedEntity funderOrg = createOrganization("SAFUND", "SA Funder Org", "https://ror.org/sa-funder-" + UUID.randomUUID());
        LinkedEntity contributorPerson = createPerson("SAContributor", "Grace", "https://orcid.org/sa-contributor-" + UUID.randomUUID());

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());

        SoftwareApplication sa = new SoftwareApplication();
        sa.setInstanceId(UUID.randomUUID().toString());
        sa.setMetaId(UUID.randomUUID().toString());
        sa.setUid("https://software.example.org/sa-all-relations-" + UUID.randomUUID());
        sa.setName("SA with All Relation Types");
        sa.setDescription("A test application with all relation types");
        sa.setSoftwareVersion("3.0.0");
        sa.setLicenseURL("https://opensource.org/licenses/Apache-2.0");
        sa.setDownloadURL("https://download.example.org/app-all.zip");
        sa.setInstallURL("https://install.example.org/app-all");
        sa.setRequirements("Java 21+");
        sa.setCreator(List.of(createLinkedEntity(creatorOrg, EntityNames.ORGANIZATION.name())));
        sa.setAuthor(List.of(createLinkedEntity(authorPerson, EntityNames.PERSON.name())));
        sa.setMaintainer(List.of(createLinkedEntity(maintainerPerson, EntityNames.PERSON.name())));
        sa.setPublisher(List.of(createLinkedEntity(publisherOrg, EntityNames.ORGANIZATION.name())));
        sa.setProvider(List.of(createLinkedEntity(providerOrg, EntityNames.ORGANIZATION.name())));
        sa.setFunder(List.of(createLinkedEntity(funderOrg, EntityNames.ORGANIZATION.name())));
        sa.setContributor(List.of(createLinkedEntity(contributorPerson, EntityNames.PERSON.name())));

        LinkedEntity result = api.create(sa, null, null, null);
        assertNotNull(result);

        // Retrieve and verify all relations
        SoftwareApplication retrieved = (SoftwareApplication) api.retrieve(sa.getInstanceId());
        LOG.info("Retrieved SoftwareApplication with all relation types: " + retrieved);

        assertNotNull(retrieved);
        
        // Verify all relation types are present
        assertAll("All relation types should be present",
                () -> assertNotNull(retrieved.getCreator(), "Creator should not be null"),
                () -> assertEquals(1, retrieved.getCreator().size(), "Should have 1 creator"),
                
                () -> assertNotNull(retrieved.getAuthor(), "Author should not be null"),
                () -> assertEquals(1, retrieved.getAuthor().size(), "Should have 1 author"),
                
                () -> assertNotNull(retrieved.getMaintainer(), "Maintainer should not be null"),
                () -> assertEquals(1, retrieved.getMaintainer().size(), "Should have 1 maintainer"),
                
                () -> assertNotNull(retrieved.getPublisher(), "Publisher should not be null"),
                () -> assertEquals(1, retrieved.getPublisher().size(), "Should have 1 publisher"),
                
                () -> assertNotNull(retrieved.getProvider(), "Provider should not be null"),
                () -> assertEquals(1, retrieved.getProvider().size(), "Should have 1 provider"),
                
                () -> assertNotNull(retrieved.getFunder(), "Funder should not be null"),
                () -> assertEquals(1, retrieved.getFunder().size(), "Should have 1 funder"),
                
                () -> assertNotNull(retrieved.getContributor(), "Contributor should not be null"),
                () -> assertEquals(1, retrieved.getContributor().size(), "Should have 1 contributor")
        );

        // Verify specific entity types
        assertEquals(EntityNames.ORGANIZATION.name(), retrieved.getCreator().get(0).getEntityType());
        assertEquals(EntityNames.PERSON.name(), retrieved.getAuthor().get(0).getEntityType());
        assertEquals(EntityNames.PERSON.name(), retrieved.getMaintainer().get(0).getEntityType());
        assertEquals(EntityNames.ORGANIZATION.name(), retrieved.getPublisher().get(0).getEntityType());
    }

    // ==================== RetrieveAll Tests ====================

    @Test
    @Order(7)
    public void testRetrieveAllSoftwareSourceCode() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());
        
        List<?> all = api.retrieveAll();
        LOG.info("Retrieved all SoftwareSourceCode entities: " + all.size());
        
        assertNotNull(all);
        assertTrue(all.size() >= 3, "Should have at least 3 SoftwareSourceCode entities from previous tests");
        
        // Verify all retrieved entities have their relations intact
        for (Object obj : all) {
            SoftwareSourceCode ssc = (SoftwareSourceCode) obj;
            assertNotNull(ssc.getInstanceId(), "Each entity should have instanceId");
            assertNotNull(ssc.getName(), "Each entity should have name");
            LOG.info("  - " + ssc.getName() + " (creators: " + 
                    (ssc.getCreator() != null ? ssc.getCreator().size() : 0) + ")");
        }
    }

    @Test
    @Order(8)
    public void testRetrieveAllSoftwareApplication() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());
        
        List<?> all = api.retrieveAll();
        LOG.info("Retrieved all SoftwareApplication entities: " + all.size());
        
        assertNotNull(all);
        assertTrue(all.size() >= 3, "Should have at least 3 SoftwareApplication entities from previous tests");
        
        // Verify all retrieved entities have their relations intact
        for (Object obj : all) {
            SoftwareApplication sa = (SoftwareApplication) obj;
            assertNotNull(sa.getInstanceId(), "Each entity should have instanceId");
            assertNotNull(sa.getName(), "Each entity should have name");
            LOG.info("  - " + sa.getName() + " (creators: " + 
                    (sa.getCreator() != null ? sa.getCreator().size() : 0) + ")");
        }
    }

    // ==================== Update Test ====================

    @Test
    @Order(9)
    public void testUpdateSoftwareSourceCodeCreator() {
        // Create unique persons for this test
        LinkedEntity person1LE = createPerson("UpdateTest1", "Henry", "https://orcid.org/update1-" + UUID.randomUUID());
        LinkedEntity person2LE = createPerson("UpdateTest2", "Ivy", "https://orcid.org/update2-" + UUID.randomUUID());

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());

        // Create with person1 as creator
        LinkedEntity initialCreator = createLinkedEntity(person1LE, EntityNames.PERSON.name());

        SoftwareSourceCode ssc = new SoftwareSourceCode();
        ssc.setInstanceId(UUID.randomUUID().toString());
        ssc.setMetaId(UUID.randomUUID().toString());
        ssc.setUid("https://github.com/epos-eu/ssc-update-test-" + UUID.randomUUID());
        ssc.setName("SSC for Update Test");
        ssc.setDescription("A test software for creator update");
        ssc.setCodeRepository("https://github.com/epos-eu/update-test");
        ssc.setSoftwareVersion("1.0.0");
        ssc.setCreator(List.of(initialCreator));

        api.create(ssc, null, null, null);

        // Verify initial creator
        SoftwareSourceCode retrieved = (SoftwareSourceCode) api.retrieve(ssc.getInstanceId());
        assertNotNull(retrieved.getCreator());
        assertEquals(1, retrieved.getCreator().size());
        assertEquals(person1LE.getInstanceId(), retrieved.getCreator().get(0).getInstanceId());

        // Update with person2 as creator
        LinkedEntity newCreator = createLinkedEntity(person2LE, EntityNames.PERSON.name());
        ssc.setCreator(List.of(newCreator));
        ssc.setDescription("Updated description");

        api.create(ssc, null, null, null);

        // Verify updated creator
        SoftwareSourceCode updated = (SoftwareSourceCode) api.retrieve(ssc.getInstanceId());
        LOG.info("Updated SoftwareSourceCode creator: " + updated);
        
        assertNotNull(updated.getCreator());
        assertEquals(1, updated.getCreator().size());
        assertEquals(person2LE.getInstanceId(), updated.getCreator().get(0).getInstanceId());
        assertEquals("Updated description", updated.getDescription());
    }
}
