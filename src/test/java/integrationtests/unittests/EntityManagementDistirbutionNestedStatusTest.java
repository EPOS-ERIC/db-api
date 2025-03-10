package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntityManagementDistirbutionNestedStatusTest extends TestcontainersLifecycle {

    static Distribution distribution;
    static WebService webService;
    static Operation operation;

    @Test
    @Order(1)
    public void testCreateAndGetItems() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());

        distribution = new Distribution();
        distribution.setInstanceId(UUID.randomUUID().toString());
        distribution.setMetaId(UUID.randomUUID().toString());
        distribution.setUid(UUID.randomUUID().toString());
        distribution.setStatus(StatusType.PUBLISHED);

        LinkedEntity linkedEntityDistribution = api.create(distribution, null, null, null);

        Distribution retrievedDistribution = (Distribution) api.retrieve(distribution.getInstanceId());

        assertNotNull(retrievedDistribution);
        assertEquals(StatusType.PUBLISHED, retrievedDistribution.getStatus());

        api = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        webService = new WebService();
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setUid(UUID.randomUUID().toString());
        webService.setStatus(StatusType.PUBLISHED);
        webService.setDistribution(List.of(linkedEntityDistribution));

        LinkedEntity linkedEntityWebservice = api.create(webService, null, null, null);

        WebService retrievedWebservice = (WebService) api.retrieve(webService.getInstanceId());

        assertNotNull(retrievedWebservice);
        assertEquals(StatusType.PUBLISHED, retrievedWebservice.getStatus());

        api = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());

        operation = new Operation();
        operation.setInstanceId(UUID.randomUUID().toString());
        operation.setMetaId(UUID.randomUUID().toString());
        operation.setUid(UUID.randomUUID().toString());
        operation.setStatus(StatusType.PUBLISHED);
        operation.setWebservice(List.of(linkedEntityWebservice));

        LinkedEntity linkedEntityOperation = api.create(operation, null, null, null);

        Operation retrievedOperation = (Operation) api.retrieve(operation.getInstanceId());

        assertNotNull(retrievedOperation);
        assertEquals(StatusType.PUBLISHED, retrievedOperation.getStatus());
    }

    @Test
    @Order(2)
    public void testUpdateAndGetItems() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        Distribution retrievedDistribution = (Distribution) api.retrieve(distribution.getInstanceId());
        retrievedDistribution.setStatus(StatusType.DRAFT);

        api.create(retrievedDistribution, null, null, null);

        List<Distribution> distributionList = api.retrieveAll();

        api = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        List<WebService> webserviceList = api.retrieveAll();


        api = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());
        List<Operation> operationList = api.retrieveAll();

        System.out.println(distributionList);
        System.out.println(webserviceList);
        System.out.println(operationList);

        assertAll(
                () -> assertEquals(2, distributionList.size()),
                () -> assertEquals(2, webserviceList.size()),
                () -> assertEquals(2, operationList.size())
        );

    }


}
