package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementDistributionTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetAddress() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());

        LinkedEntity le = new LinkedEntity();
        le.setUid("test");
        le.setEntityType(EntityNames.WEBSERVICE.name());

        LinkedEntity le2 = new LinkedEntity();
        le2.setUid("test2");
        le2.setEntityType(EntityNames.OPERATION.name());

        Distribution distribution = new Distribution();
        distribution.setInstanceId(UUID.randomUUID().toString());
        distribution.setMetaId(UUID.randomUUID().toString());
        distribution.setUid(UUID.randomUUID().toString());
        distribution.addAccessService(le);
        distribution.addSupportedOperation(le2);

        api.create(distribution, null, null, null);

        Distribution retrievedDistribution = (Distribution) api.retrieve(distribution.getInstanceId());

        System.out.println(retrievedDistribution);

        assertNotNull(retrievedDistribution);
    }


}
