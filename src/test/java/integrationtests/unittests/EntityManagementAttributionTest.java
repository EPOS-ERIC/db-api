package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntityManagementAttributionTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAttribution() {

        LinkedEntity org = new LinkedEntity();
        org.setUid("test");
        org.setEntityType(EntityNames.ORGANIZATION.name());

        Attribution attribution = new Attribution();
        attribution.setUid("testattribution");
        attribution.setAgent(org);
        attribution.setRole(List.of("testrole","testrole2"));

        LinkedEntity attributionLE = new LinkedEntity();
        attributionLE.setUid(attribution.getUid());
        attributionLE.setEntityType(EntityNames.ATTRIBUTION.name());

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid(UUID.randomUUID().toString());
        dataProduct.setPublisher(List.of(org));
        dataProduct.addQualifiedAttribution(attributionLE);

        List<EPOSDataModelEntity> classes = new ArrayList<>();
        classes.add(dataProduct);
        classes.add(attribution);

        for (EPOSDataModelEntity eposDataModelEntity : classes) {
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.ContactPoint) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.Category) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.CategoryScheme) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.Organization) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            if(eposDataModelEntity instanceof org.epos.eposdatamodel.Person) eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            // System.out.println("[ADDING TO DATABASE] "+eposDataModelEntity);
            try {
                AbstractAPI api = AbstractAPI.retrieveAPI(eposDataModelEntity.getClass().getSimpleName().toUpperCase());
               System.out.println("Ingesting -> "+eposDataModelEntity);
                LinkedEntity le = api.create(eposDataModelEntity, null, null, null);
            } catch (Exception apiCreationException) {
                apiCreationException.printStackTrace();
                System.out.println("[ERROR] ON: " + eposDataModelEntity.toString() + "\n[EXCEPTION]: "
                        + apiCreationException.getLocalizedMessage());
            }
        }

        Attribution retrievedAddress = (Attribution) AbstractAPI.retrieveAPI(EntityNames.ATTRIBUTION.name()).retrieve(attribution.getInstanceId());

        LOG.info("RECEIVED:\n"+retrievedAddress.toString());

        List<DataProduct> retrieveAll = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll();
        for (DataProduct dp : retrieveAll) {
            LOG.info("DP: "+dp.toString());
        }

        assertAll(
                () -> assertEquals(attribution.getAgent(),retrievedAddress.getAgent()),
                () -> assertEquals(attribution.getRole(),retrievedAddress.getRole())
        );
    }

}
