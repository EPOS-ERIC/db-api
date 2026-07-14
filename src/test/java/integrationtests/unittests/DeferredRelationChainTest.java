package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.DistributionDataproduct;
import model.StatusType;
import model.WebserviceDistribution;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DeferredRelationChainTest extends TestcontainersLifecycle {

    @Test
    public void testCreateDataProductDistributionWebServiceChain() {
        String suffix = UUID.randomUUID().toString();
        String dataProductUid = "dataproduct:chain:" + suffix;
        String distributionUid = "distribution:chain:" + suffix;
        String webServiceUid = "webservice:chain:" + suffix;

        AbstractAPI dataProductAPI = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        AbstractAPI distributionAPI = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        AbstractAPI webServiceAPI = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid(dataProductUid);
        dataProduct.addTitle("Chain DataProduct");
        dataProduct.setStatus(StatusType.PUBLISHED);
        dataProduct.addDistribution(linkedEntity(distributionUid, EntityNames.DISTRIBUTION.name()));

        LinkedEntity dataProductCreated = dataProductAPI.create(dataProduct, StatusType.PUBLISHED, null, null);
        assertNotNull(dataProductCreated);

        Distribution distribution = new Distribution();
        distribution.setUid(distributionUid);
        distribution.addTitle("Chain Distribution");
        distribution.setFormat("application/json");
        distribution.setStatus(StatusType.PUBLISHED);
        distribution.addAccessService(linkedEntity(webServiceUid, EntityNames.WEBSERVICE.name()));

        LinkedEntity distributionCreated = distributionAPI.create(distribution, StatusType.PUBLISHED, null, null);
        assertNotNull(distributionCreated);

        WebService webService = new WebService();
        webService.setUid(webServiceUid);
        webService.setName("Chain WebService");
        webService.setDescription("WebService created after the chain is referenced");
        webService.setStatus(StatusType.PUBLISHED);

        LinkedEntity webServiceCreated = webServiceAPI.create(webService, StatusType.PUBLISHED, null, null);
        assertNotNull(webServiceCreated);

        DataProduct retrievedDataProduct = (DataProduct) dataProductAPI.retrieve(dataProductCreated.getInstanceId());
        assertNotNull(retrievedDataProduct);
        assertNotNull(retrievedDataProduct.getDistribution());
        assertEquals(1, retrievedDataProduct.getDistribution().size());
        assertEquals(distributionUid, retrievedDataProduct.getDistribution().get(0).getUid());

        List<Object> dataproductJoins = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache("dataproductInstance", dataProductCreated.getInstanceId(), DistributionDataproduct.class);
        List<Object> webserviceJoins = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache("distributionInstance", distributionCreated.getInstanceId(), WebserviceDistribution.class);
        List<Object> allWebserviceJoins = EposDataModelDAO.getInstance().getAllFromDB(WebserviceDistribution.class);

        System.out.println("DataProduct joins: " + dataproductJoins.size());
        System.out.println("WebService joins: " + webserviceJoins.size());
        System.out.println("All WebServiceDistribution rows: " + allWebserviceJoins.size());

        if (!allWebserviceJoins.isEmpty()) {
            WebserviceDistribution rel = (WebserviceDistribution) allWebserviceJoins.get(0);
            System.out.println("First WebServiceDistribution row -> distributionInstanceId=" +
                    (rel.getId() != null ? rel.getId().getDistributionInstanceId() : "null") +
                    ", webserviceInstanceId=" +
                    (rel.getId() != null ? rel.getId().getWebserviceInstanceId() : "null"));
        }

        assertEquals(1, dataproductJoins.size());
        assertEquals(1, webserviceJoins.size());

        Distribution retrievedDistribution = (Distribution) distributionAPI.retrieve(distributionCreated.getInstanceId());
        assertNotNull(retrievedDistribution);
        assertNotNull(retrievedDistribution.getDataProduct());
        assertEquals(1, retrievedDistribution.getDataProduct().size());
        assertEquals(dataProductUid, retrievedDistribution.getDataProduct().get(0).getUid());
        assertNotNull(retrievedDistribution.getAccessService());
        assertEquals(1, retrievedDistribution.getAccessService().size());
        assertEquals(webServiceUid, retrievedDistribution.getAccessService().get(0).getUid());

        WebService retrievedWebService = (WebService) webServiceAPI.retrieve(webServiceCreated.getInstanceId());
        assertNotNull(retrievedWebService);
        assertEquals(webServiceUid, retrievedWebService.getUid());
        assertTrue(((DistributionDataproduct) dataproductJoins.get(0)).getDistributionInstance().getUid().equals(distributionUid));
        assertTrue(((WebserviceDistribution) webserviceJoins.get(0)).getWebserviceInstance().getUid().equals(webServiceUid));
    }

    private LinkedEntity linkedEntity(String uid, String entityType) {
        LinkedEntity linkedEntity = new LinkedEntity();
        linkedEntity.setUid(uid);
        linkedEntity.setEntityType(entityType);
        return linkedEntity;
    }
}
