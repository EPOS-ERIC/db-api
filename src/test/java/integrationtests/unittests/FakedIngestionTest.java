package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

public class FakedIngestionTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreation() {

        Organization organization = new Organization();
        organization.setUid("Organization");
        organization.setLegalName(List.of("organization"));
        organization.setStatus(StatusType.PUBLISHED);
        organization.setEditorId("test");
        organization.setFileProvenance("prov1");

        LinkedEntity organizationLinkedEntity = new LinkedEntity();
        organizationLinkedEntity.setUid(organization.getUid());
        organizationLinkedEntity.setEntityType(EntityNames.ORGANIZATION.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("Dataproduct");
        dataProduct.setTitle(List.of("title"));
        dataProduct.setPublisher(List.of(organizationLinkedEntity));
        dataProduct.setStatus(StatusType.PUBLISHED);
        dataProduct.setEditorId("test");
        dataProduct.setFileProvenance("prov1");

        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);
        AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name()).create(organization, null, null, null);

        System.out.println("-------------------- first ingestion --------------------");
        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveAll()){
            System.out.println(object);
        }
        System.out.println("-------------------- first ingestion --------------------");

        organization.setStatus(StatusType.DRAFT);
        organization.setFileProvenance("newprov");
        dataProduct.setTitle(List.of("titlenew"));
        dataProduct.setStatus(StatusType.DRAFT);
        dataProduct.setFileProvenance("newprov");


        AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct, null, null, null);
        AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name()).create(organization, null, null, null);

        System.out.println("-------------------- second ingestion --------------------");
        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveAll()){
            System.out.println(object);
        }
        System.out.println("-------------------- second ingestion --------------------");


    }

}
