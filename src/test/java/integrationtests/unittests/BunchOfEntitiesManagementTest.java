package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class BunchOfEntitiesManagementTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGet() {


        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

        for(int i = 0; i<100; i++){
            DataProduct dataProduct = new DataProduct();
            dataProduct.setInstanceId(UUID.randomUUID().toString());
            dataProduct.setMetaId(UUID.randomUUID().toString());
            dataProduct.setUid(UUID.randomUUID().toString());

            LinkedEntity linkedEntity = new LinkedEntity();
            linkedEntity.setInstanceId(UUID.randomUUID().toString());
            linkedEntity.setMetaId(UUID.randomUUID().toString());
            linkedEntity.setUid(UUID.randomUUID().toString());
            linkedEntity.setEntityType(EntityNames.DISTRIBUTION.name());

            dataProduct.setDistribution(List.of(linkedEntity));

            api.create(dataProduct, StatusType.PUBLISHED, null, null);
        }


        List<DataProduct> retrieveAll = api.retrieveAll();
        LOG.info("RECEIVED:\n"+retrieveAll.size());

        assertNotNull(retrieveAll);
    }

}
