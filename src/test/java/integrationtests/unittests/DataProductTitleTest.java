package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataProductTitleTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testDataProviders() {

        List<EPOSDataModelEntity> classes = new ArrayList<>();

        DataProduct dataProduct = new DataProduct();
        classes.add(dataProduct);
        dataProduct.setUid("https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001");
        dataProduct.setTitle(List.of("Test"));

        for(EPOSDataModelEntity entity : classes) {
            AbstractAPI.retrieveAPI(EntityNames.valueOf(entity.getClass().getSimpleName().toUpperCase(Locale.ROOT)).name()).create(entity, null, null, null);
        }

        //DO UPDATE

        for(EPOSDataModelEntity entity : classes) {
            AbstractAPI.retrieveAPI(EntityNames.valueOf(entity.getClass().getSimpleName().toUpperCase(Locale.ROOT)).name()).create(entity, null, null, null);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

    }

}
