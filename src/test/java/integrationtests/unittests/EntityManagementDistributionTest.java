package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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

    @Test
    @Order(2)
    public void testUpdateClearsAccessServiceWithEmptyList() {
        AbstractAPI distApi = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        AbstractAPI wsApi = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        LinkedEntity ws = createWebService(wsApi);
        Distribution distribution = createDistributionWithAccessService(ws);
        distApi.create(distribution, null, null, null);

        Distribution update = new Distribution();
        update.setInstanceId(distribution.getInstanceId());
        update.setMetaId(distribution.getMetaId());
        update.setUid(distribution.getUid());
        update.setAccessService(Collections.emptyList());

        distApi.create(update, null, null, null);

        Distribution retrieved = (Distribution) distApi.retrieve(distribution.getInstanceId());
        assertNull(retrieved.getAccessService());
    }

    @Test
    @Order(3)
    public void testUpdateClearsAccessServiceWithNull() {
        AbstractAPI distApi = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        AbstractAPI wsApi = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        LinkedEntity ws = createWebService(wsApi);
        Distribution distribution = createDistributionWithAccessService(ws);
        distApi.create(distribution, null, null, null);

        Distribution update = new Distribution();
        update.setInstanceId(distribution.getInstanceId());
        update.setMetaId(distribution.getMetaId());
        update.setUid(distribution.getUid());
        update.setAccessService(null);

        distApi.create(update, null, null, null);

        Distribution retrieved = (Distribution) distApi.retrieve(distribution.getInstanceId());
        assertNull(retrieved.getAccessService());
    }

    private LinkedEntity createWebService(AbstractAPI wsApi) {
        org.epos.eposdatamodel.WebService webService = new org.epos.eposdatamodel.WebService();
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setUid(UUID.randomUUID().toString());
        return wsApi.create(webService, null, null, null);
    }

    private Distribution createDistributionWithAccessService(LinkedEntity ws) {
        Distribution distribution = new Distribution();
        distribution.setInstanceId(UUID.randomUUID().toString());
        distribution.setMetaId(UUID.randomUUID().toString());
        distribution.setUid(UUID.randomUUID().toString());
        distribution.setAccessService(List.of(ws));
        return distribution;
    }


}
