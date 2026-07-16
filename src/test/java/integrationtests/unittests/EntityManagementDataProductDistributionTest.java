package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.WebServiceAPI;
import model.DistributionDataproduct;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementDataProductDistributionTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetDataProductWithoutExplicitRelation() {
        WebServiceAPI webServiceAPI = new WebServiceAPI(
                EntityNames.WEBSERVICE.name(), model.Webservice.class);

        /**
         * CREATE FIRST PUBLISHED DISTRIBUTION AND DATAPRODUCT
         */
        System.out.println("CREATE FIRST PUBLISHED DISTRIBUTION AND DATAPRODUCT");
        LinkedEntity le = new LinkedEntity();
        le.setUid("test");
        le.setEntityType(EntityNames.DISTRIBUTION.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.addDistribution(le);
        dataProduct.setCreated("");
        dataProduct.setStatus(StatusType.PUBLISHED);
        dataProduct.setEditorId("test");

        LinkedEntity linkedEntity = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);
        dataProduct.setInstanceId(linkedEntity.getInstanceId());
        dataProduct.setMetaId(linkedEntity.getMetaId());

        Distribution distribution = new Distribution();
        distribution.setUid("test");
        distribution.setTitle(List.of("Distribution"));
        distribution.setEditorId("test");
        distribution.setStatus(StatusType.PUBLISHED);

        WebService webService = new WebService();
        webService.setUid("test-webservice");
        webService.setName("WebService");
        webService.setEditorId("test");
        webService.setStatus(StatusType.PUBLISHED);
        LinkedEntity webServiceLinkedEntity = webServiceAPI.create(webService, null, null, null);
        distribution.addAccessService(webServiceLinkedEntity);

        linkedEntity = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(distribution, null, null, null);
        distribution.setInstanceId(linkedEntity.getInstanceId());
        distribution.setMetaId(linkedEntity.getMetaId());


        List<DataProduct> retrievedDataProduct = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();
        System.out.println(retrievedDataProduct);

        /**
         * CREATE FIRST DRAFT DATAPRODUCT -> EXPECTING DISTRIBUTION DRAFT
         */

        System.out.println("CREATE FIRST DRAFT DATAPRODUCT -> EXPECTING DISTRIBUTION DRAFT");
        dataProduct.setStatus(StatusType.DRAFT);

        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);

        assertNotNull(retrievedDataProduct);
        assertEquals(1,retrievedDataProduct.size());

        retrievedDataProduct = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();

        DataProduct publishedDataProduct = retrievedDataProduct.stream()
                .filter(dp -> dp.getStatus() == StatusType.PUBLISHED)
                .findFirst()
                .orElseThrow();
        DataProduct firstDraftDataProduct = retrievedDataProduct.stream()
                .filter(dp -> dp.getStatus() == StatusType.DRAFT && "test".equals(dp.getEditorId()))
                .findFirst()
                .orElseThrow();
        assertPersistedDistributionJoin(retrievedDataProduct, publishedDataProduct);
        assertEquals(1, publishedDataProduct.getDistribution().size());
        assertEquals(1, firstDraftDataProduct.getDistribution().size());

        for(DataProduct dp : retrievedDataProduct){
            System.out.println(dp);
        }

        List<Distribution> retrievedDistribution = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll();

        for (Distribution dp : retrievedDistribution) {
            System.out.println(dp);
        }

        Distribution publishedDistribution = retrievedDistribution.stream()
                .filter(dist -> dist.getStatus() == StatusType.PUBLISHED)
                .findFirst()
                .orElseThrow();
        Distribution firstDraftDistribution = retrievedDistribution.stream()
                .filter(dist -> dist.getStatus() == StatusType.DRAFT && "test".equals(dist.getEditorId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, publishedDistribution.getDataProduct().size());
        assertEquals(1, firstDraftDistribution.getDataProduct().size());
        assertEquals(publishedDataProduct.getInstanceId(),
                publishedDistribution.getDataProduct().get(0).getInstanceId());
        assertEquals(firstDraftDataProduct.getInstanceId(),
                firstDraftDistribution.getDataProduct().get(0).getInstanceId());
        assertNotNull(publishedDistribution.getAccessService());
        assertNotNull(firstDraftDistribution.getAccessService());
        assertEquals(1, publishedDistribution.getAccessService().size());
        assertEquals(1, firstDraftDistribution.getAccessService().size());
        WebService publishedWebService = webServiceAPI.retrieve(webServiceLinkedEntity.getInstanceId());
        WebService firstDraftWebService = webServiceAPI
                .retrieveAll().stream()
                .filter(ws -> ws.getStatus() == StatusType.DRAFT && "test".equals(ws.getEditorId()))
                .findFirst()
                .orElseThrow();
        assertEquals(publishedWebService.getInstanceId(),
                publishedDistribution.getAccessService().get(0).getInstanceId());
        assertEquals(firstDraftWebService.getInstanceId(),
                firstDraftDistribution.getAccessService().get(0).getInstanceId());
        assertNotNull(publishedWebService.getDistribution());
        assertNotNull(firstDraftWebService.getDistribution());
        assertEquals(1, publishedWebService.getDistribution().size());
        assertEquals(1, firstDraftWebService.getDistribution().size());
        assertEquals(publishedDistribution.getInstanceId(),
                publishedWebService.getDistribution().get(0).getInstanceId());
        assertEquals(firstDraftDistribution.getInstanceId(),
                firstDraftWebService.getDistribution().get(0).getInstanceId());
        System.out.println("Published WebService: " + publishedWebService);
        System.out.println("First draft WebService: " + firstDraftWebService);
        distribution.setInstanceId(firstDraftDistribution.getInstanceId());
        distribution.setMetaId(firstDraftDistribution.getMetaId());
        distribution.setEditorId(firstDraftDistribution.getEditorId());

        /**
         * USE DRAFT DISTRIBUTION TO CHANGE IT'S TITLE
         */
        System.out.println("USE DRAFT DISTRIBUTION TO CHANGE IT'S TITLE");

        distribution.setTitle(List.of("DO I HAVE THIS TITLE?"));
        distribution.setStatus(StatusType.DRAFT);

        AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(distribution, null, null, null);

        retrievedDistribution = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll();

        for(Distribution dp : retrievedDistribution){
            System.out.println(dp);
        }
        Distribution firstDraftDistributionAfterUpdate = retrievedDistribution.stream()
                .filter(dist -> firstDraftDistribution.getInstanceId().equals(dist.getInstanceId()))
                .findFirst()
                .orElseThrow();
        assertNotNull(firstDraftDistributionAfterUpdate.getAccessService());
        assertEquals(1, firstDraftDistributionAfterUpdate.getAccessService().size());
        assertEquals(firstDraftWebService.getInstanceId(),
                firstDraftDistributionAfterUpdate.getAccessService().get(0).getInstanceId());
        System.out.println("WebServices after first draft Distribution update:");
        for (WebService ws : webServiceAPI.retrieveAll()) {
            System.out.println(ws);
        }

        /**
         * CREATE SECOND DRAFT DATAPRODUCT WITH DIFFERENT USER -> EXPECTING DISTRIBUTION DRAFT
         */
        System.out.println("CREATE SECOND DRAFT DATAPRODUCT WITH DIFFERENT USER -> EXPECTING DISTRIBUTION DRAFT");

        dataProduct.setEditorId("USER2");

        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);

        retrievedDataProduct = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();

        DataProduct secondDraftDataProduct = retrievedDataProduct.stream()
                .filter(dp -> dp.getStatus() == StatusType.DRAFT && "USER2".equals(dp.getEditorId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, secondDraftDataProduct.getDistribution().size());

        for(DataProduct dp : retrievedDataProduct){
            System.out.println(dp);
        }

        retrievedDistribution = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll();

        for (Distribution dp : retrievedDistribution) {
            System.out.println(dp);
        }

        Distribution secondDraftDistribution = retrievedDistribution.stream()
                .filter(dist -> dist.getStatus() == StatusType.DRAFT && "USER2".equals(dist.getEditorId()))
                .findFirst()
                .orElseThrow();
        assertEquals(1, secondDraftDistribution.getDataProduct().size());
        assertEquals(secondDraftDataProduct.getInstanceId(),
                secondDraftDistribution.getDataProduct().get(0).getInstanceId());
        assertNotNull(secondDraftDistribution.getAccessService());
        assertEquals(1, secondDraftDistribution.getAccessService().size());
        WebService secondDraftWebService = webServiceAPI
                .retrieveAll().stream()
                .filter(ws -> ws.getStatus() == StatusType.DRAFT && "USER2".equals(ws.getEditorId()))
                .findFirst()
                .orElseThrow();
        assertEquals(secondDraftWebService.getInstanceId(),
                secondDraftDistribution.getAccessService().get(0).getInstanceId());
        assertNotNull(secondDraftWebService.getDistribution());
        assertEquals(1, secondDraftWebService.getDistribution().size());
        assertEquals(secondDraftDistribution.getInstanceId(),
                secondDraftWebService.getDistribution().get(0).getInstanceId());
        System.out.println("Second draft WebService: " + secondDraftWebService);
        distribution.setInstanceId(secondDraftDistribution.getInstanceId());
        distribution.setMetaId(secondDraftDistribution.getMetaId());
        distribution.setEditorId(secondDraftDistribution.getEditorId());

        /**
         * USE DRAFT DISTRIBUTION TO CHANGE IT'S TITLE
         */
        System.out.println("USE DRAFT DISTRIBUTION TO CHANGE IT'S TITLE");

        distribution.setTitle(List.of("OMG ITS IT"));
        distribution.setStatus(StatusType.DRAFT);

        AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(distribution, null, null, null);

        retrievedDistribution = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll();

        for(Distribution dp : retrievedDistribution){
            System.out.println(dp);
        }
        Distribution secondDraftDistributionAfterUpdate = retrievedDistribution.stream()
                .filter(dist -> secondDraftDistribution.getInstanceId().equals(dist.getInstanceId()))
                .findFirst()
                .orElseThrow();
        assertNotNull(secondDraftDistributionAfterUpdate.getAccessService());
        assertEquals(1, secondDraftDistributionAfterUpdate.getAccessService().size());
        assertEquals(secondDraftWebService.getInstanceId(),
                secondDraftDistributionAfterUpdate.getAccessService().get(0).getInstanceId());
        System.out.println("WebServices after second draft Distribution update:");
        for (WebService ws : webServiceAPI.retrieveAll()) {
            System.out.println(ws);
        }

        retrievedDataProduct = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();
        assertEquals(3, retrievedDataProduct.size());
        assertEquals(3, retrievedDistribution.size());
        assertEquals(3, webServiceAPI.retrieveAll().size());
    }

    private void assertPersistedDistributionJoin(List<DataProduct> dataProducts,
                                                 DataProduct publishedDataProduct) {
        EposDataModelDAO<Object> dao = EposDataModelDAO.getInstance();
        List<DistributionDataproduct> joins = dao.getJoinEntitiesByParentId(
                "dataproductInstanceId", publishedDataProduct.getInstanceId(), DistributionDataproduct.class);

        List<String> distributionIds = joins.stream()
                .map(join -> join.getDistributionInstance().getInstanceId())
                .distinct()
                .toList();

        assertEquals(1, joins.size(),
                "Published DataProduct must have exactly one persisted Distribution join: " + distributionIds);
        assertEquals(1, distributionIds.size(),
                "Published DataProduct must not have duplicate Distribution targets");
        assertEquals(StatusType.PUBLISHED, publishedDataProduct.getStatus());
        assertEquals(dataProducts.stream().filter(dp -> dp.getStatus() == StatusType.PUBLISHED).count(), 1);
    }


}
