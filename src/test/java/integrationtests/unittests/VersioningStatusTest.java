package integrationtests.unittests;

import abstractapis.AbstractAPI;
import commonapis.VersioningStatusAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class VersioningStatusTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testVersioningStatus() {

        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("Test");
        dataProduct.setStatus(StatusType.DRAFT);
        dataProduct.setEditorId("test");

        System.out.println(AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .create(dataProduct, null, null, null));

        System.out.println(VersioningStatusAPI.retrieveVersion(dataProduct).getEditorId());

        dataProduct.setStatus(StatusType.PUBLISHED);

        System.out.println(AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .create(dataProduct, null, null, null));

        System.out.println(VersioningStatusAPI.retrieveVersion(dataProduct).getEditorId());

        dataProduct.setStatus(StatusType.DRAFT);

        System.out.println(AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name())
                .create(dataProduct, null, null, null));

        System.out.println(VersioningStatusAPI.retrieveVersion(dataProduct).getEditorId());
    }

}
