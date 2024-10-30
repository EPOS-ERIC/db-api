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

public class EntityManagementDataProductTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetDataProductWithoutExplicitRelation() {


        LinkedEntity le = new LinkedEntity();
        le.setUid("test");
        le.setEntityType(EntityNames.DISTRIBUTION.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.addDistribution(le);
        dataProduct.setCreated("");

        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, StatusType.PUBLISHED, null, null);


        Distribution distribution = new Distribution();
        distribution.setUid("test");
        distribution.setTitle(List.of("Distribution"));

        AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(distribution, StatusType.PUBLISHED, null, null);



        List<DataProduct> retrievedDataProduct = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();
        System.out.println(retrievedDataProduct);


        assertNotNull(retrievedDataProduct);
        assertEquals(1,retrievedDataProduct.size());
    }


}
