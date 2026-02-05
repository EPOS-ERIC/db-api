package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntityOrganizationsAndDataProductDeleteTest extends TestcontainersLifecycle {

    static LinkedEntity organizationLE = null;
    static LinkedEntity categoryLe = null;

    @Test
    @Order(1)
    public void testCreateOrganization() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        Organization organization = new Organization();
        organization.setInstanceId(UUID.randomUUID().toString());
        organization.setMetaId(UUID.randomUUID().toString());
        organization.setUid(UUID.randomUUID().toString());
        organization.setAcronym("INGV");
        organization.setLegalName(List.of("Istituto Nazionale di Geofisica e Vulcanologia"));

        organizationLE = api.create(organization, null, null, null);

        Organization retrievedOrganization = (Organization) api.retrieve(organization.getInstanceId());

        LOG.info("RECEIVED:\n"+organization.toString());

        assertNotNull(retrievedOrganization);
        assertAll(
                () -> assertEquals(organization.getInstanceId(), retrievedOrganization.getInstanceId()),
                () -> assertEquals(organization.getMetaId(), retrievedOrganization.getMetaId()),
                () -> assertEquals(organization.getUid(), retrievedOrganization.getUid()),
                () -> assertEquals(organization.getAcronym(), retrievedOrganization.getAcronym()),
                () -> assertEquals(StatusType.DRAFT, retrievedOrganization.getStatus()),
                () -> assertNotNull(retrievedOrganization.getChangeTimestamp())
        );
    }

    @Test
    @Order(2)
    public void testCreateCategory() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid(UUID.randomUUID().toString());
        category.setName("Test category");
        category.setDescription("Test category description");

        categoryLe = api.create(category, null, null, null);

        Category retrievedCategory = (Category) api.retrieve(category.getInstanceId());

        LOG.info("RECEIVED:\n"+category.toString());

        assertNotNull(retrievedCategory);
        assertAll(
                () -> assertEquals(category.getInstanceId(), retrievedCategory.getInstanceId()),
                () -> assertEquals(category.getMetaId(), retrievedCategory.getMetaId()),
                () -> assertEquals(category.getUid(), retrievedCategory.getUid())
        );
    }

    @Test
    @Order(3)
    public void testCreateDatasetWithProvider() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setPublisher(List.of(organizationLE));
        dataProduct.addKeywords("Test");
        dataProduct.addKeywords("Test 2");
        dataProduct.addKeywords("Test 3");
        dataProduct.addKeywords("Test 4");
        LinkedEntity ispartof = new LinkedEntity();
        ispartof.setInstanceId(UUID.randomUUID().toString());
        ispartof.setMetaId(UUID.randomUUID().toString());
        ispartof.setUid(UUID.randomUUID().toString());
        ispartof.setEntityType(EntityNames.DATAPRODUCT.name());
        dataProduct.addIsPartOf(ispartof);
        dataProduct.setDocumentation("Test Documentation");
        dataProduct.addCategory(categoryLe);

        api.create(dataProduct, null, null, null);

        api = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        Organization retrievedOrganization = (Organization) api.retrieve(organizationLE.getInstanceId());

        LOG.info("RECEIVED:\n"+retrievedOrganization.toString());

        assertNotNull(retrievedOrganization);

        api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct retrievedDataproduct = (DataProduct) api.retrieve(dataProduct.getInstanceId());

        LOG.info("RECEIVED:\n"+retrievedDataproduct.toString());

        assertNotNull(retrievedDataproduct);
    }

    @Test
    @Order(4)
    public void testCreateAndDeleteDatasetWithProvider() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setPublisher(List.of(organizationLE));
        dataProduct.addKeywords("Test");
        dataProduct.addKeywords("Test 2");
        dataProduct.addKeywords("Test 3");
        dataProduct.addKeywords("Test 4");
        LinkedEntity ispartof = new LinkedEntity();
        ispartof.setInstanceId(UUID.randomUUID().toString());
        ispartof.setMetaId(UUID.randomUUID().toString());
        ispartof.setUid(UUID.randomUUID().toString());
        ispartof.setEntityType(EntityNames.DATAPRODUCT.name());
        dataProduct.addIsPartOf(ispartof);
        dataProduct.setDocumentation("Test Documentation");
        dataProduct.addCategory(categoryLe);

        api.create(dataProduct, null, null, null);

        api = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        Organization retrievedOrganization = (Organization) api.retrieve(organizationLE.getInstanceId());

        LOG.info("RECEIVED:\n"+retrievedOrganization.toString());

        assertNotNull(retrievedOrganization);

        assertEquals(Boolean.TRUE,api.delete(organizationLE.getInstanceId()));

        retrievedOrganization = (Organization) api.retrieve(organizationLE.getInstanceId());

        assertNull(retrievedOrganization);

        api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct retrievedDataproduct = (DataProduct) api.retrieve(dataProduct.getInstanceId());

        LOG.info("RECEIVED:\n"+retrievedDataproduct.toString());

        assertNotNull(retrievedDataproduct);
    }

}
