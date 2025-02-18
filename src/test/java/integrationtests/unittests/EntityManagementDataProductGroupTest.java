package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementDataProductGroupTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetDataProductWithoutExplicitRelation() {

        Group group = new Group();
        group.setName("group");
        group.setDescription("group description");

        UserGroupManagementAPI.createGroup(group);

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("");

        LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, StatusType.PUBLISHED, null, null);

        UserGroupManagementAPI.addMetadataElementToGroup(le.getMetaId(), UserGroupManagementAPI.retrieveGroupByName(group.getName()).getId());

        List<DataProduct> retrievedDataProduct = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();
        System.out.println(retrievedDataProduct);


        assertNotNull(retrievedDataProduct);
        assertEquals(1,retrievedDataProduct.size());
    }


}
