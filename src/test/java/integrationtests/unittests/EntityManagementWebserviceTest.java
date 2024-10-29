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

public class EntityManagementWebserviceTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetItems() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());

        Category category = new Category();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid(UUID.randomUUID().toString());
        category.setName("Test");
        category.setDescription("Test category");

        LinkedEntity categoryCreated = api.create(category, null, null, null);
        System.out.println(categoryCreated);

        api = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        Organization organization = new Organization();
        organization.setInstanceId(UUID.randomUUID().toString());
        organization.setMetaId(UUID.randomUUID().toString());
        organization.setUid(UUID.randomUUID().toString());
        organization.setLegalName(List.of("Test"));

        LinkedEntity organizationCreated = api.create(organization, null, null, null);
        System.out.println(organizationCreated);


        api = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        Operation operation = new Operation();
        operation.setInstanceId(UUID.randomUUID().toString());
        operation.setMetaId(UUID.randomUUID().toString());
        operation.setUid(UUID.randomUUID().toString());

        LinkedEntity leoperation = new LinkedEntity();
        leoperation.setEntityType(EntityNames.OPERATION.name());
        leoperation.setInstanceId(operation.getInstanceId());
        leoperation.setMetaId(operation.getMetaId());
        leoperation.setUid(operation.getUid());

        WebService webService = new WebService();
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());
        webService.setUid(UUID.randomUUID().toString());
        webService.setName("Test Webservice");
        webService.setDescription("Test Webservice Description");
        webService.addCategory(categoryCreated);
        webService.addSupportedOperation(leoperation);
        webService.setProvider(organizationCreated);
        webService.addKeywords("Test");
        webService.addKeywords("Test 2");
        webService.addKeywords("Test 3");
        webService.addKeywords("Test 4");

        LinkedEntity webserviceCreated = api.create(webService, null, null, null);
        System.out.println(webserviceCreated);

        api = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name());

        Mapping mapping = new Mapping();
        mapping.setInstanceId(UUID.randomUUID().toString());
        mapping.setMetaId(UUID.randomUUID().toString());
        mapping.setUid(UUID.randomUUID().toString());
        mapping.setLabel("label");
        mapping.setVariable("variable");

        LinkedEntity mappingCreated = api.create(mapping, null, null, null);
        System.out.println(mappingCreated);


        api = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());

        operation.setReturns(List.of("application/json"));
        operation.setMethod("GET");
        operation.setWebservice(List.of(webserviceCreated));
        operation.setTemplate("Template");
        operation.setMapping(List.of(mappingCreated));

        LinkedEntity operationCreated = api.create(operation, null, null, null);
        System.out.println(operationCreated);


        api = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        WebService retrievedWebservice = (WebService) api.retrieve(webserviceCreated.getInstanceId());
        api = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());
        Operation retrievedOperation = (Operation) api.retrieve(operationCreated.getInstanceId());
        api = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name());
        Mapping retrievedMapping = (Mapping) api.retrieve(mappingCreated.getInstanceId());

        System.out.println(retrievedWebservice);
        System.out.println(retrievedOperation);
        System.out.println(retrievedMapping);

        assertNotNull(retrievedWebservice);
        assertNotNull(retrievedOperation);
        assertNotNull(retrievedMapping);
    }


}
