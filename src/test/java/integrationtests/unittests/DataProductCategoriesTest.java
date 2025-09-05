package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataProductCategoriesTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testDataProviders() {

        List<EPOSDataModelEntity> classes = new ArrayList<>();

        LinkedEntity category = new LinkedEntity();
        category.setUid("test");
        category.setEntityType(EntityNames.CATEGORY.name());


        LinkedEntity category2 = new LinkedEntity();
        category2.setUid("test2");
        category2.setEntityType(EntityNames.CATEGORY.name());

        DataProduct dataProduct = new DataProduct();
        classes.add(dataProduct);
        dataProduct.setUid("https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001");
        dataProduct.setTitle(List.of("Test"));
        dataProduct.setCategory(List.of(category,category2));

        for(EPOSDataModelEntity entity : classes) {
            AbstractAPI.retrieveAPI(EntityNames.valueOf(entity.getClass().getSimpleName().toUpperCase(Locale.ROOT)).name()).create(entity, null, null, null);
        }

        //DO UPDATE
        dataProduct.setCategory(List.of(category2));
        //System.out.println(dataProduct);
        for(EPOSDataModelEntity entity : classes) {
            AbstractAPI.retrieveAPI(EntityNames.valueOf(entity.getClass().getSimpleName().toUpperCase(Locale.ROOT)).name()).create(entity, null, null, null);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

    }

}
