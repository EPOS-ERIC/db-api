package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Operation;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebServiceDeleteReferencedOperationTest extends TestcontainersLifecycle {

    @Test
    void deletesOperationWebserviceRowsBeforeDeletingWebservice() {
        Operation operation = new Operation();
        operation.setUid("delete-webservice-operation-" + UUID.randomUUID());
        operation.setMethod("GET");
        operation.setTemplate("https://example.test/operation");
        operation.setStatus(StatusType.PUBLISHED);
        LinkedEntity operationLink = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name())
                .create(operation, null, null, null);

        WebService webService = new WebService();
        webService.setUid("delete-webservice-" + UUID.randomUUID());
        webService.setName("Webservice deletion test");
        webService.setStatus(StatusType.PUBLISHED);
        webService.addSupportedOperation(operationLink);
        LinkedEntity webServiceLink = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webService, null, null, null);

        assertTrue(AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .delete(webServiceLink.getInstanceId()));
        assertNull(AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(webServiceLink.getInstanceId()));
        assertNotNull(AbstractAPI.retrieveAPI(EntityNames.OPERATION.name())
                .retrieve(operationLink.getInstanceId()));
    }
}
