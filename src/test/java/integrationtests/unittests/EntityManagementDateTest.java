package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.DataProduct;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementDateTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreate() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("2024-07-11T09:35:25.018Z");


        api.create(dataProduct, null, null, null);

        DataProduct dataProduct1 = (DataProduct) api.retrieve(dataProduct.getInstanceId());

        assertEquals(dataProduct.getCreated(),dataProduct1.getCreated());

    }

    @Test
    @Order(2)
    public void testCreate2() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("2024-07-11T09:35:25Z");


        api.create(dataProduct, null, null, null);

    }

    @Test
    @Order(3)
    public void testCreate3() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("2024-07-11T09:35:25");


        api.create(dataProduct, null, null, null);

    }

    @Test
    @Order(4)
    public void testCreate4() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("2024-07-11");


        api.create(dataProduct, null, null, null);

    }

    @Test
    @Order(5)
    public void testCreate5() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("2024-07-11T");


        api.create(dataProduct, null, null, null);

    }

    @Test
    @Order(6)
    public void testCreate6() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("2024-07-11T01:00");


        api.create(dataProduct, null, null, null);

    }

    @Test
    @Order(7)
    public void testCreate7() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setCreated("2024");


        api.create(dataProduct, null, null, null);

    }

}
