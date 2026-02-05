package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
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


}
