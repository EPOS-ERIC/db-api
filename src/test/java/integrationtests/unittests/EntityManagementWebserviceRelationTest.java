package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementWebserviceRelationTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetItems() {

        WebService webService = new WebService();
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setUid(UUID.randomUUID().toString());
        webService.setName("Test Webservice");
        webService.setDescription("Test Webservice Description");
        webService.addKeywords("Test");
        webService.addKeywords("Test 2");
        webService.addKeywords("Test 3");
        webService.addKeywords("Test 4");

        LinkedEntity webserviceCreated = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).create(webService, null, null, null);
        System.out.println(webserviceCreated);

        WebService webService2 = new WebService();
        webService2.setInstanceId(UUID.randomUUID().toString());
        webService2.setMetaId(UUID.randomUUID().toString());
        webService2.setUid(UUID.randomUUID().toString());
        webService2.setName("Test Webservice 2");
        webService2.setDescription("Test Webservice Description 2");
        webService2.addWebserviceRelation(webserviceCreated);

        LinkedEntity webserviceCreated2 = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).create(webService2, null, null, null);
        System.out.println(webserviceCreated2);

        WebService retrievedWebservice = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).retrieve(webService2.getInstanceId());

        System.out.println(retrievedWebservice);

        assertNotNull(retrievedWebservice);
    }


}
