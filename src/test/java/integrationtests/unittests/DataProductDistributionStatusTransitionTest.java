package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DataProductDistributionStatusTransitionTest extends TestcontainersLifecycle {

    @Test
    void transitionsKeepTheLinkedDistributionVersion() {
        AbstractAPI distributionApi = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name());
        Distribution distribution = new Distribution();
        distribution.setUid("distribution/" + UUID.randomUUID());
        distribution.setTitle(List.of("Test distribution"));
        distribution.setFormat("application/json");
        distribution.setLicence("https://example.org/license");
        LinkedEntity distributionLink = distributionApi.create(distribution, StatusType.DRAFT, null, null);

        AbstractAPI dataProductApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("dataproduct/" + UUID.randomUUID());
        dataProduct.setTitle(List.of("Test data product"));
        dataProduct.setDistribution(List.of(distributionLink));
        LinkedEntity dataProductLink = dataProductApi.create(dataProduct, StatusType.DRAFT, null, null);

        DataProduct submitted = (DataProduct) dataProductApi.retrieve(dataProductLink.getInstanceId());
        LinkedEntity submittedLink = dataProductApi.create(submitted, StatusType.SUBMITTED, null, null);
        Distribution submittedDistribution = (Distribution) distributionApi.retrieve(distributionLink.getInstanceId());

        assertEquals(dataProductLink.getInstanceId(), submittedLink.getInstanceId());
        assertEquals(StatusType.SUBMITTED, submittedDistribution.getStatus());

        DataProduct published = (DataProduct) dataProductApi.retrieve(dataProductLink.getInstanceId());
        LinkedEntity publishedLink = dataProductApi.create(published, StatusType.PUBLISHED, null, null);
        Distribution publishedDistribution = (Distribution) distributionApi.retrieve(distributionLink.getInstanceId());

        assertEquals(dataProductLink.getInstanceId(), publishedLink.getInstanceId());
        assertEquals(StatusType.PUBLISHED, publishedDistribution.getStatus());
    }
}
