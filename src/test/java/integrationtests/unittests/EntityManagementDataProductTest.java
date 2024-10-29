package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementDataProductTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetAddress() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        LinkedEntity le = new LinkedEntity();
        le.setUid("test");
        le.setEntityType(EntityNames.DISTRIBUTION.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.addDistribution(le);
        dataProduct.setCreated("");

        api.create(dataProduct, null, null, null);

        DataProduct retrievedDataProduct = (DataProduct) api.retrieve(dataProduct.getInstanceId());
        System.out.println(retrievedDataProduct.getDistribution());


        AbstractAPI api2 = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        List<Distribution> retrieve = api2.retrieveAll();

        System.out.println(retrieve.size());

        assertNotNull(retrievedDataProduct);
        assertNotNull(retrievedDataProduct.getDistribution());
    }


}
