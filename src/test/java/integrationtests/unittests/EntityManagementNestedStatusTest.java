package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementNestedStatusTest extends TestcontainersLifecycle {

    static DataProduct dataProduct;
    static Distribution distribution;
    @Test
    @Order(1)
    public void testCreateAndGetDataProduct() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setStatus(StatusType.PUBLISHED);

        api.create(dataProduct, null, null, null);

        DataProduct retrieved = (DataProduct) api.retrieve(dataProduct.getInstanceId());

        assertNotNull(retrieved);
        assertEquals(StatusType.PUBLISHED, retrieved.getStatus());
    }


    @Test
    @Order(2)
    public void testCreateAndGetDIstribution() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());

        distribution = new Distribution();
        distribution.setInstanceId(UUID.randomUUID().toString());
        distribution.setMetaId(UUID.randomUUID().toString());
        distribution.setUid(UUID.randomUUID().toString());
        distribution.setStatus(StatusType.PUBLISHED);

        LinkedEntity le = api.create(distribution, null, null, null);

        dataProduct.addDistribution(le);

        Distribution retrieved = (Distribution) api.retrieve(distribution.getInstanceId());

        assertNotNull(retrieved);
        assertEquals(StatusType.PUBLISHED, retrieved.getStatus());
    }

    @Test
    @Order(3)
    public void testCreateAndGetDataProductWithDistribution() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        api.create(dataProduct, null, null, null);

        DataProduct retrieved = (DataProduct) api.retrieve(dataProduct.getInstanceId());

        System.out.println(retrieved);
        assertNotNull(retrieved);
        assertEquals(StatusType.PUBLISHED, retrieved.getStatus());
    }

    @Test
    @Order(4)
    public void testUpdateStatus() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        dataProduct.setStatus(StatusType.DRAFT);

        LinkedEntity le = api.create(dataProduct, null, null, null);

        DataProduct retrievedDataProduct1 = (DataProduct) api.retrieve(le.getInstanceId());

        System.out.println(retrievedDataProduct1);

        api = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());

        List<Distribution> distributionList = api.retrieveAll();

        System.out.println(distributionList);

        assertEquals(StatusType.DRAFT, retrievedDataProduct1.getStatus());
        assertEquals(2, distributionList.size());
    }

}
