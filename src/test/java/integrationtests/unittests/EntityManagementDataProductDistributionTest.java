package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
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

        retrievedDataProduct = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();
        assertEquals(3, retrievedDataProduct.size());
        assertEquals(3, retrievedDistribution.size());
    }


}
