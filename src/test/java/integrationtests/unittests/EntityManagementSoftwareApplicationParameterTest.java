package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementSoftwareApplicationParameterTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetSoftwareApplicationParameter() {

        SoftwareApplicationInputParameter inputParameter = new SoftwareApplicationInputParameter();
        inputParameter.setInstanceId(UUID.randomUUID().toString());
        inputParameter.setMetaId(UUID.randomUUID().toString());
        inputParameter.setUid(UUID.randomUUID().toString());
        inputParameter.setConformsto(UUID.randomUUID().toString());
        inputParameter.setEncodingformat("ISO-8859-1");

        SoftwareApplicationOutputParameter outputParameter = new SoftwareApplicationOutputParameter();
        outputParameter.setInstanceId(UUID.randomUUID().toString());
        outputParameter.setMetaId(UUID.randomUUID().toString());
        outputParameter.setUid(UUID.randomUUID().toString());
        outputParameter.setConformsto(UUID.randomUUID().toString());
        outputParameter.setEncodingformat("ISO-8859-1");

        AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name()).create(inputParameter, StatusType.PUBLISHED, null, null);
        AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name()).create(outputParameter, StatusType.PUBLISHED, null, null);


        List<SoftwareApplicationParameter> retrievedAll = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name()).retrieveAll();
        System.out.println(retrievedAll);


        assertNotNull(retrievedAll);
        assertEquals(2,retrievedAll.size());
    }


}
