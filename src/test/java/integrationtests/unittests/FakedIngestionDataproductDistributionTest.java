package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

public class FakedIngestionDataproductDistributionTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreation() {

        Distribution distribution = new Distribution();
        distribution.setUid("Distribution");
        distribution.setTitle(List.of("distribution"));
        distribution.setStatus(StatusType.PUBLISHED);
        distribution.setEditorId("test");
        distribution.setFileProvenance("prov1");

        LinkedEntity dsitributionLinkedEntity = new LinkedEntity();
        dsitributionLinkedEntity.setUid(distribution.getUid());
        dsitributionLinkedEntity.setEntityType(EntityNames.DISTRIBUTION.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("Dataproduct");
        dataProduct.setTitle(List.of("title"));
        dataProduct.setDistribution(List.of(dsitributionLinkedEntity));
        dataProduct.setStatus(StatusType.PUBLISHED);
        dataProduct.setEditorId("test");
        dataProduct.setFileProvenance("prov1");

        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);
        AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(distribution, null, null, null);

        System.out.println("-------------------- first ingestion --------------------");
        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll()){
            System.out.println(object);
        }
        System.out.println("-------------------- first ingestion --------------------");

        distribution.setStatus(StatusType.DRAFT);
        distribution.setFileProvenance("newprov");
        distribution.setTitle(List.of("distributiontitle new test"));
        dataProduct.setStatus(StatusType.DRAFT);
        dataProduct.setFileProvenance("newprov");


        AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(distribution, null, null, null);
        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);

        System.out.println("-------------------- second ingestion --------------------");
        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll()){
            System.out.println(object);
        }
        System.out.println("-------------------- second ingestion --------------------");


    }

}
