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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Replays the published graph represented by ah.ttl and verifies that partial
 * updates never mix published versions with an archived graph.
 */
class AhTtlPublishedGraphAlignmentTest extends TestcontainersLifecycle {

    private static final String TTL_DATASET = "anthropogenic_hazards/dataset/virtual_access/apps";
    private static final String TTL_DISTRIBUTION = "anthropogenic_hazards/distribution/apps/sspe";
    private static final String TTL_WEBSERVICE = "anthropogenic_hazards/webservice/is-epos_platform";
    private static final String TTL_OPERATION = "anthropogenic_hazards/webservice/is-epos_platform/apps/sspe";

    @Test
    void partialUpdatesKeepEveryPublishedGraphAlignedWithPublishedTargets() throws IOException {
        String ttl = readTtl();
        assertTrue(ttl.contains("<" + TTL_DATASET + "> a dcat:Dataset"));
        assertTrue(ttl.contains("<" + TTL_WEBSERVICE + "> a epos:WebService"));
        assertTrue(ttl.contains("<" + TTL_OPERATION + "> a hydra:Operation"));
        assertTrue(ttl.contains("<" + TTL_DISTRIBUTION + ">"));

        OperationAPI operationApi = new OperationAPI(EntityNames.OPERATION.name(), model.Operation.class);
        WebServiceAPI webServiceApi = new WebServiceAPI(EntityNames.WEBSERVICE.name(), model.Webservice.class);
        DistributionAPI distributionApi = new DistributionAPI(EntityNames.DISTRIBUTION.name(), model.Distribution.class);
        DataProductAPI dataProductApi = new DataProductAPI(EntityNames.DATAPRODUCT.name(), model.Dataproduct.class);

        String suffix = UUID.randomUUID().toString();
        LinkedEntity operationV1 = createOperation(operationApi, TTL_OPERATION + "/" + suffix, "GET");
        LinkedEntity webServiceV1 = createWebService(webServiceApi, TTL_WEBSERVICE + "/" + suffix, operationV1);
        LinkedEntity distributionV1 = createDistribution(distributionApi, TTL_DISTRIBUTION + "/" + suffix,
                webServiceV1, operationV1);
        LinkedEntity dataProductV1 = createDataProduct(dataProductApi, TTL_DATASET + "/" + suffix, distributionV1);

        assertPublishedGraph(dataProductApi, distributionApi, webServiceApi, operationApi, dataProductV1,
                StatusType.PUBLISHED);

        DataProduct dataProductDraftRequest = dataProductApi.retrieve(dataProductV1.getInstanceId());
        dataProductDraftRequest.setStatus(StatusType.DRAFT);
        LinkedEntity dataProductV2 = dataProductApi.create(dataProductDraftRequest, StatusType.DRAFT, null, null);
        DataProduct dataProductDraft = dataProductApi.retrieve(dataProductV2.getInstanceId());
        assertEquals(StatusType.DRAFT, dataProductDraft.getStatus());
        assertNotNull(dataProductDraft.getDistribution());

        String distributionV2Id = dataProductDraft.getDistribution().get(0).getInstanceId();
        Distribution distributionDraft = distributionApi.retrieve(distributionV2Id);
        distributionDraft.setTitle(List.of("partial distribution update"));
        distributionApi.create(distributionDraft, StatusType.DRAFT, null, null);

        Distribution distributionSubmitted = distributionApi.retrieve(distributionV2Id);
        distributionApi.create(distributionSubmitted, StatusType.SUBMITTED, null, null);
        Distribution distributionPublished = distributionApi.retrieve(distributionV2Id);
        distributionApi.create(distributionPublished, StatusType.PUBLISHED, null, null);
        Distribution distributionAfterPublish = distributionApi.retrieve(distributionV2Id);
        assertNotEquals(webServiceV1.getInstanceId(),
                distributionAfterPublish.getAccessService().get(0).getInstanceId(),
                "Distribution must use the versioned WebService before DataProduct publish");

        DataProduct dataProductSubmitted = dataProductApi.retrieve(dataProductV2.getInstanceId());
        dataProductApi.create(dataProductSubmitted, StatusType.SUBMITTED, null, null);
        DataProduct dataProductPublished = dataProductApi.retrieve(dataProductV2.getInstanceId());
        dataProductApi.create(dataProductPublished, StatusType.PUBLISHED, null, null);

        assertEquals(StatusType.ARCHIVED,
                distributionApi.retrieve(distributionV1.getInstanceId()).getStatus());
        assertEquals(StatusType.PUBLISHED, distributionApi.retrieve(distributionV2Id).getStatus());
        Distribution publishedDistribution = distributionApi.retrieve(distributionV2Id);
        assertNotNull(publishedDistribution.getAccessService());
        assertFalse(publishedDistribution.getAccessService().isEmpty());
        assertNotEquals(webServiceV1.getInstanceId(),
                publishedDistribution.getAccessService().get(0).getInstanceId(),
                "published Distribution must use the versioned WebService");
        assertEquals(StatusType.ARCHIVED,
                webServiceApi.retrieve(webServiceV1.getInstanceId()).getStatus());

        assertPublishedGraph(dataProductApi, distributionApi, webServiceApi, operationApi, dataProductV2,
                StatusType.PUBLISHED);
        assertArchivedSnapshot(dataProductApi, distributionApi, webServiceApi, operationApi, dataProductV1,
                dataProductV2);
    }

    private String readTtl() throws IOException {
        try (InputStream input = getClass().getResourceAsStream("/ah.ttl")) {
            assertNotNull(input, "ah.ttl must be available as a test resource");
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private LinkedEntity createOperation(OperationAPI api, String uid, String method) {
        Operation operation = new Operation();
        operation.setUid(uid);
        operation.setMethod(method);
        operation.setTemplate("https://episodesplatform.eu/api/epos/episode-elements");
        return api.create(operation, StatusType.PUBLISHED, null, null);
    }

    private LinkedEntity createWebService(WebServiceAPI api, String uid, LinkedEntity operation) {
        WebService webService = new WebService();
        webService.setUid(uid);
        webService.setName("Virtual access to EPISODES Platform");
        webService.setEntryPoint("https://episodesplatform.eu/");
        webService.setSupportedOperation(List.of(operation));
        return api.create(webService, StatusType.PUBLISHED, null, null);
    }

    private LinkedEntity createDistribution(DistributionAPI api, String uid, LinkedEntity webService,
            LinkedEntity operation) {
        Distribution distribution = new Distribution();
        distribution.setUid(uid);
        distribution.setTitle(List.of("Virtual access distribution"));
        distribution.setAccessService(List.of(webService));
        distribution.setSupportedOperation(List.of(operation));
        return api.create(distribution, StatusType.PUBLISHED, null, null);
    }

    private LinkedEntity createDataProduct(DataProductAPI api, String uid, LinkedEntity distribution) {
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid(uid);
        dataProduct.setTitle(List.of("Virtual access to TCS AH Applications"));
        dataProduct.setDistribution(List.of(distribution));
        return api.create(dataProduct, StatusType.PUBLISHED, null, null);
    }

    private void assertPublishedGraph(DataProductAPI dataProductApi, DistributionAPI distributionApi,
            WebServiceAPI webServiceApi, OperationAPI operationApi, LinkedEntity dataProductLink,
            StatusType expectedStatus) {
        DataProduct dataProduct = dataProductApi.retrieve(dataProductLink.getInstanceId());
        assertEquals(expectedStatus, dataProduct.getStatus());
        assertNotNull(dataProduct.getDistribution());
        assertFalse(dataProduct.getDistribution().isEmpty());

        Distribution distribution = distributionApi.retrieve(dataProduct.getDistribution().get(0).getInstanceId());
        assertEquals(expectedStatus, distribution.getStatus());
        assertNotNull(distribution.getAccessService());
        assertFalse(distribution.getAccessService().isEmpty());
        assertNotNull(distribution.getSupportedOperation());

        WebService webService = webServiceApi.retrieve(distribution.getAccessService().get(0).getInstanceId());
        assertEquals(expectedStatus, webService.getStatus());
        assertNotNull(webService.getSupportedOperation());
        assertFalse(webService.getSupportedOperation().isEmpty());

        for (LinkedEntity operationLink : webService.getSupportedOperation()) {
            assertEquals(expectedStatus, operationApi.retrieve(operationLink.getInstanceId()).getStatus());
        }
        for (LinkedEntity operationLink : distribution.getSupportedOperation()) {
            assertEquals(expectedStatus, operationApi.retrieve(operationLink.getInstanceId()).getStatus());
        }
    }

    private void assertArchivedSnapshot(DataProductAPI dataProductApi, DistributionAPI distributionApi,
            WebServiceAPI webServiceApi, OperationAPI operationApi, LinkedEntity archivedDataProduct,
            LinkedEntity publishedDataProduct) {
        DataProduct archived = dataProductApi.retrieve(archivedDataProduct.getInstanceId());
        assertEquals(StatusType.ARCHIVED, archived.getStatus());
        assertNotNull(archived.getDistribution());

        Distribution archivedDistribution = distributionApi.retrieve(archived.getDistribution().get(0).getInstanceId());
        assertEquals(StatusType.ARCHIVED, archivedDistribution.getStatus());
        WebService archivedWebService = webServiceApi.retrieve(
                archivedDistribution.getAccessService().get(0).getInstanceId());
        assertEquals(StatusType.ARCHIVED, archivedWebService.getStatus());

        for (LinkedEntity operationLink : archivedWebService.getSupportedOperation()) {
            assertEquals(StatusType.ARCHIVED, operationApi.retrieve(operationLink.getInstanceId()).getStatus());
        }

        DataProduct published = dataProductApi.retrieve(publishedDataProduct.getInstanceId());
        assertEquals(StatusType.PUBLISHED, published.getStatus());
        assertTrue(published.getDistribution().get(0).getInstanceId()
                .equals(archived.getDistribution().get(0).getInstanceId()) == false,
                "published DataProduct must not reuse archived Distribution");
    }
}
