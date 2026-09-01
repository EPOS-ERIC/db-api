package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementContactPointGroupTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetDataProductWithoutExplicitRelation() {

        Group group = new Group();
        group.setName("group");
        group.setDescription("group description");

        UserGroupManagementAPI.createGroup(group);

        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setUid(UUID.randomUUID().toString());

        LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name()).create(contactPoint, StatusType.PUBLISHED, null, null);

        UserGroupManagementAPI.addMetadataElementToGroup(le.getMetaId(), UserGroupManagementAPI.retrieveGroupByName(group.getName()).getId());

        List<ContactPoint> retrievedContactPoint = AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveAll();
        System.out.println(retrievedContactPoint);


        assertNotNull(retrievedContactPoint);
        assertEquals(1,retrievedContactPoint.size());
    }

    @Test
    void referenceEntitiesAlwaysPersistAsPublished() {
        assertPublished(new Address(), EntityNames.ADDRESS);
        assertPublished(new Category(), EntityNames.CATEGORY);
        assertPublished(new CategoryScheme(), EntityNames.CATEGORYSCHEME);
        assertPublished(new ContactPoint(), EntityNames.CONTACTPOINT);
        assertPublished(new Organization(), EntityNames.ORGANIZATION);
    }

    @Test
    void backofficeDraftCannotCreateMissingContactPointThroughARelation() {
        String missingUid = "https://example.org/contact-point/" + UUID.randomUUID();
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("https://example.org/data-product/" + UUID.randomUUID());
        dataProduct.setEditorId("editor");
        dataProduct.setStatus(StatusType.DRAFT);
        dataProduct.setContactPoint(List.of(new LinkedEntity()
                .entityType(EntityNames.CONTACTPOINT.name())
                .uid(missingUid)));

        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);

        assertEquals(0, EposDataModelDAO.getInstance()
                .getOneFromDBByUIDNoCache(missingUid, model.Contactpoint.class).size());
    }

    private void assertPublished(EPOSDataModelEntity entity, EntityNames entityName) {
        entity.setUid(UUID.randomUUID().toString());
        entity.setStatus(StatusType.DRAFT);

        LinkedEntity link = AbstractAPI.retrieveAPI(entityName.name()).create(entity, StatusType.DRAFT, null, null);
        Versioningstatus version = (Versioningstatus) EposDataModelDAO.getInstance()
                .getOneFromDBByInstanceIdNoCache(link.getInstanceId(), Versioningstatus.class)
                .get(0);

        assertEquals(StatusType.PUBLISHED.name(), version.getStatus(), entityName + " must remain published");
    }


}
