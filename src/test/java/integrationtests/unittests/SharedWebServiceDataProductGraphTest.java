package integrationtests.unittests;

import integrationtests.TestcontainersLifecycle;
import metadataapis.DataProductAPI;
import metadataapis.DistributionAPI;
import metadataapis.EntityNames;
import metadataapis.OperationAPI;
import metadataapis.WebServiceAPI;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Operation;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Reproduces two datasets sharing one WebService and its operations. */
class SharedWebServiceDataProductGraphTest extends TestcontainersLifecycle {

    @Test
    void keepsSharedWebServiceGraphWhenSecondDatasetIsCreated() {
        OperationAPI operationApi = new OperationAPI(EntityNames.OPERATION.name(), model.Operation.class);
        WebServiceAPI webServiceApi = new WebServiceAPI(EntityNames.WEBSERVICE.name(), model.Webservice.class);
        DistributionAPI distributionApi = new DistributionAPI(EntityNames.DISTRIBUTION.name(), model.Distribution.class);
        DataProductAPI dataProductApi = new DataProductAPI(EntityNames.DATAPRODUCT.name(), model.Dataproduct.class);

        LinkedEntity operation1 = createOperation(operationApi, "operation1");
        LinkedEntity operation2 = createOperation(operationApi, "operation2");
        String operation1InstanceId = operation1.getInstanceId();
        String operation2InstanceId = operation2.getInstanceId();
        LinkedEntity webService1 = createWebService(webServiceApi, "webservice1", operation1);
        String webService1InstanceId = webService1.getInstanceId();
        LinkedEntity distribution1 = createDistribution(distributionApi, "distribution1", webService1, operation1);
        String distribution1InstanceId = distribution1.getInstanceId();
        LinkedEntity dataset1 = createDataProduct(dataProductApi, "dataset1", distribution1);
        String dataset1InstanceId = dataset1.getInstanceId();

        assertGraph("after dataset1", webServiceApi, distributionApi, dataProductApi,
                webService1InstanceId, distribution1InstanceId, dataset1InstanceId,
                List.of(operation1InstanceId));

        // The second dataset adds operation2 to the already shared WebService.
        WebService webServiceUpdate = webServiceApi.retrieve(webService1InstanceId);
        webServiceUpdate.setSupportedOperation(List.of(operation1, operation2));
        webServiceApi.create(webServiceUpdate, StatusType.PUBLISHED, null, null);

        LinkedEntity distribution2 = createDistribution(distributionApi, "distribution2", webService1, operation2);
        String distribution2InstanceId = distribution2.getInstanceId();
        LinkedEntity dataset2 = createDataProduct(dataProductApi, "dataset2", distribution2);
        String dataset2InstanceId = dataset2.getInstanceId();

        assertGraph("after dataset2", webServiceApi, distributionApi, dataProductApi,
                webService1InstanceId, distribution1InstanceId, dataset1InstanceId,
                List.of(operation1InstanceId, operation2InstanceId));

        Distribution retrievedDistribution1 = distributionApi.retrieve(distribution1InstanceId);
        assertEquals(operation1InstanceId, retrievedDistribution1.getSupportedOperation().get(0).getInstanceId(),
                "dataset1 distribution operation was overridden");

        Distribution retrievedDistribution2 = distributionApi.retrieve(distribution2InstanceId);
        assertNotNull(retrievedDistribution2.getAccessService(), "dataset2 distribution lost accessService");
        assertEquals(webService1InstanceId, retrievedDistribution2.getAccessService().get(0).getInstanceId());
        assertEquals(operation2InstanceId, retrievedDistribution2.getSupportedOperation().get(0).getInstanceId());

        DataProduct retrievedDataset2 = dataProductApi.retrieve(dataset2InstanceId);
        assertNotNull(retrievedDataset2.getDistribution(), "dataset2 lost distribution");
        assertEquals(distribution2.getInstanceId(), retrievedDataset2.getDistribution().get(0).getInstanceId());

        // Drafting dataset1 must draft its complete reachable graph, including both
        // operations exposed by the shared WebService.
        DataProduct dataset1DraftRequest = dataProductApi.retrieve(dataset1InstanceId);
        dataset1DraftRequest.setStatus(StatusType.DRAFT);
        dataset1DraftRequest.setEditorId("draft-owner");
        LinkedEntity dataset1Draft = dataProductApi.create(dataset1DraftRequest, StatusType.DRAFT, null, null);
        DataProduct draftedDataset1 = dataProductApi.retrieve(dataset1Draft.getInstanceId());

        assertEquals(StatusType.DRAFT, draftedDataset1.getStatus());
        assertEquals("dataset1", draftedDataset1.getTitle().get(0));
        assertNotEquals(dataset1InstanceId, draftedDataset1.getInstanceId());

        Distribution draftedDistribution1 = distributionApi.retrieve(draftedDataset1.getDistribution().get(0).getInstanceId());
        assertEquals(StatusType.DRAFT, draftedDistribution1.getStatus());
        assertEquals("distribution1", draftedDistribution1.getTitle().get(0));
        assertNotEquals(distribution1InstanceId, draftedDistribution1.getInstanceId());

        WebService draftedWebService1 = webServiceApi.retrieve(draftedDistribution1.getAccessService().get(0).getInstanceId());
        assertEquals(StatusType.DRAFT, draftedWebService1.getStatus());
        assertEquals("webservice1", draftedWebService1.getName());
        assertNotEquals(webService1InstanceId, draftedWebService1.getInstanceId());
        assertEquals(2, draftedWebService1.getSupportedOperation().size(),
                "draft WebService lost one of its operations");

        for (LinkedEntity operationLink : draftedWebService1.getSupportedOperation()) {
            Operation draftedOperation = operationApi.retrieve(operationLink.getInstanceId());
            assertEquals(StatusType.DRAFT, draftedOperation.getStatus());
            assertEquals("GET", draftedOperation.getMethod());
        }

        // Update draft properties and prove that the published graph is not overwritten.
        draftedDataset1.setTitle(List.of("dataset1-draft"));
        dataProductApi.create(draftedDataset1, StatusType.DRAFT, null, null);
        draftedDistribution1.setTitle(List.of("distribution1-draft"));
        distributionApi.create(draftedDistribution1, StatusType.DRAFT, null, null);
        draftedWebService1.setName("webservice1-draft");
        webServiceApi.create(draftedWebService1, StatusType.DRAFT, null, null);

        Operation draftedOperation1 = operationApi.retrieve(
                draftedWebService1.getSupportedOperation().get(0).getInstanceId());
        draftedOperation1.setTemplate("https://example.org/operation1-draft");
        operationApi.create(draftedOperation1, StatusType.DRAFT, null, null);

        assertEquals("dataset1", dataProductApi.retrieve(dataset1InstanceId).getTitle().get(0));
        assertEquals("distribution1", distributionApi.retrieve(distribution1InstanceId).getTitle().get(0));
        assertEquals("webservice1", webServiceApi.retrieve(webService1InstanceId).getName());
        assertEquals("https://example.org/operation1",
                operationApi.retrieve(operation1InstanceId).getTemplate());

        assertEquals("dataset1-draft", dataProductApi.retrieve(dataset1Draft.getInstanceId()).getTitle().get(0));
        assertEquals("distribution1-draft",
                distributionApi.retrieve(draftedDistribution1.getInstanceId()).getTitle().get(0));
        assertEquals("webservice1-draft",
                webServiceApi.retrieve(draftedWebService1.getInstanceId()).getName());
        assertEquals("https://example.org/operation1-draft",
                operationApi.retrieve(draftedOperation1.getInstanceId()).getTemplate());
    }

    private LinkedEntity createOperation(OperationAPI api, String name) {
        Operation operation = new Operation();
        operation.setUid("test:" + name + ":" + UUID.randomUUID());
        operation.setMethod("GET");
        operation.setTemplate("https://example.org/" + name);
        operation.setStatus(StatusType.PUBLISHED);
        return api.create(operation, StatusType.PUBLISHED, null, null);
    }

    private LinkedEntity createWebService(WebServiceAPI api, String name, LinkedEntity operation) {
        WebService webService = new WebService();
        webService.setUid("test:" + name + ":" + UUID.randomUUID());
        webService.setName(name);
        webService.setEntryPoint("https://example.org/" + name);
        webService.setSupportedOperation(List.of(operation));
        webService.setStatus(StatusType.PUBLISHED);
        return api.create(webService, StatusType.PUBLISHED, null, null);
    }

    private LinkedEntity createDistribution(DistributionAPI api, String name, LinkedEntity webService,
            LinkedEntity operation) {
        Distribution distribution = new Distribution();
        distribution.setUid("test:" + name + ":" + UUID.randomUUID());
        distribution.setTitle(List.of(name));
        distribution.setFormat("application/json");
        distribution.setAccessService(List.of(webService));
        distribution.setSupportedOperation(List.of(operation));
        distribution.setStatus(StatusType.PUBLISHED);
        return api.create(distribution, StatusType.PUBLISHED, null, null);
    }

    private LinkedEntity createDataProduct(DataProductAPI api, String name, LinkedEntity distribution) {
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("test:" + name + ":" + UUID.randomUUID());
        dataProduct.setTitle(List.of(name));
        dataProduct.setDistribution(List.of(distribution));
        dataProduct.setStatus(StatusType.PUBLISHED);
        return api.create(dataProduct, StatusType.PUBLISHED, null, null);
    }

    private void assertGraph(String checkpoint, WebServiceAPI webServiceApi, DistributionAPI distributionApi,
            DataProductAPI dataProductApi, String webServiceInstanceId, String distributionInstanceId,
            String dataProductInstanceId, List<String> expectedOperationInstanceIds) {
        WebService retrievedWebService = webServiceApi.retrieve(webServiceInstanceId);
        assertNotNull(retrievedWebService, checkpoint + ": webservice1 missing");
        assertNotNull(retrievedWebService.getSupportedOperation(), checkpoint + ": supportedOperation missing");
        assertEquals(expectedOperationInstanceIds.size(), retrievedWebService.getSupportedOperation().size(),
                checkpoint + ": supportedOperation count changed");
        System.out.println(checkpoint + " supportedOperations actual=" + retrievedWebService.getSupportedOperation());
        for (int i = 0; i < expectedOperationInstanceIds.size(); i++) {
            String expectedId = expectedOperationInstanceIds.get(i);
            assertTrue(retrievedWebService.getSupportedOperation().stream()
                            .anyMatch(operation -> expectedId.equals(operation.getInstanceId())),
                    checkpoint + ": supportedOperation " + expectedId + " is missing");
        }

        Distribution retrievedDistribution = distributionApi.retrieve(distributionInstanceId);
        assertNotNull(retrievedDistribution.getAccessService(), checkpoint + ": distribution lost accessService");
        assertEquals(webServiceInstanceId, retrievedDistribution.getAccessService().get(0).getInstanceId(),
                checkpoint + ": distribution accessService changed");

        DataProduct retrievedDataProduct = dataProductApi.retrieve(dataProductInstanceId);
        assertNotNull(retrievedDataProduct.getDistribution(), checkpoint + ": dataset lost distribution");
        assertEquals(distributionInstanceId, retrievedDataProduct.getDistribution().get(0).getInstanceId(),
                checkpoint + ": dataset distribution changed");
    }
}
