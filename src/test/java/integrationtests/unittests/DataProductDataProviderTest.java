package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DataProductDataProviderTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreation() {

        Organization organization = new Organization();
        organization.setInstanceId(UUID.randomUUID().toString());
        organization.setMetaId(UUID.randomUUID().toString());
        organization.setUid(UUID.randomUUID().toString());
        organization.setLegalName(List.of("organization"));
        organization.setStatus(StatusType.PUBLISHED);

        LinkedEntity organizationLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name()).create(organization,null,null,null);

        DataProduct dataProduct = new DataProduct();
        dataProduct.setInstanceId(UUID.randomUUID().toString());
        dataProduct.setMetaId(UUID.randomUUID().toString());
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setPublisher(List.of(organizationLinkedEntity));
        dataProduct.setStatus(StatusType.PUBLISHED);

        LinkedEntity dataProductLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct,null,null,null);

        Organization organization2 = new Organization();
        organization2.setInstanceId(UUID.randomUUID().toString());
        organization2.setMetaId(UUID.randomUUID().toString());
        organization2.setUid(UUID.randomUUID().toString());
        organization.setLegalName(List.of("organization2"));
        organization.setStatus(StatusType.PUBLISHED);

        LinkedEntity organization2LinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name()).create(organization,null,null,null);

        dataProduct.setPublisher(List.of(organization2LinkedEntity));

        dataProductLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct,null,null,null);

        dataProduct.setStatus(StatusType.DRAFT);

        dataProductLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct,null,null,null);

        dataProduct.setPublisher(List.of(organization2LinkedEntity));

        dataProductLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).create(dataProduct,null,null,null);


        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

    }

}
