package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import model.StatusType;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

    @Test
    @Order(4)
    public void testStatusOnlyUpdatePreservesAccessService() {
        AbstractAPI distApi = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        AbstractAPI wsApi = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        LinkedEntity ws = createWebService(wsApi);
        Distribution distribution = createDistributionWithAccessService(ws);
        distApi.create(distribution, StatusType.PUBLISHED, null, null);

        Distribution statusUpdate = new Distribution();
        statusUpdate.setInstanceId(distribution.getInstanceId());
        statusUpdate.setMetaId(distribution.getMetaId());
        statusUpdate.setUid(distribution.getUid());
        statusUpdate.setStatus(StatusType.ARCHIVED);

        distApi.create(statusUpdate, StatusType.ARCHIVED, null, null);

        Distribution retrieved = (Distribution) distApi.retrieve(distribution.getInstanceId());
        assertEquals(StatusType.ARCHIVED, retrieved.getStatus());
        assertNotNull(retrieved.getAccessService());
        assertFalse(retrieved.getAccessService().isEmpty());
        assertEquals(ws.getUid(), retrieved.getAccessService().get(0).getUid());
    }

    @Test
    @Order(5)
    public void testWebServiceInverseUpdateDoesNotReplaceDistributionAccessService() {
        AbstractAPI distApi = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        AbstractAPI wsApi = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        LinkedEntity ws = createWebService(wsApi);
        Distribution distribution = createDistributionWithAccessService(ws);
        distApi.create(distribution, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.WebService inverseUpdate = new org.epos.eposdatamodel.WebService();
        inverseUpdate.setInstanceId(ws.getInstanceId());
        inverseUpdate.setMetaId(ws.getMetaId());
        inverseUpdate.setUid(ws.getUid());
        inverseUpdate.setDistribution(Collections.emptyList());
        wsApi.create(inverseUpdate, StatusType.PUBLISHED, null, null);

        Distribution retrieved = (Distribution) distApi.retrieve(distribution.getInstanceId());
        assertNotNull(retrieved.getAccessService());
        assertEquals(1, retrieved.getAccessService().size());
        assertEquals(ws.getInstanceId(), retrieved.getAccessService().get(0).getInstanceId());
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
