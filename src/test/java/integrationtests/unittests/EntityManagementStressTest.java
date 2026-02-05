package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityManagementStressTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGet() {

        AbstractAPI mappingAPI = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name());
        AbstractAPI operationAPI = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());

        for(int i = 0; i<50; i++){
            Operation operation = new Operation();
            operation.setInstanceId(UUID.randomUUID().toString());
            operation.setMetaId(UUID.randomUUID().toString());
            operation.setUid(UUID.randomUUID().toString());
            operation.setMethod("GET");
            operation.setTemplate("Template-"+i);
            for(int j = 0; j<20; j++){
                Mapping mapping = new Mapping();
                mapping.setInstanceId(UUID.randomUUID().toString());
                mapping.setMetaId(UUID.randomUUID().toString());
                mapping.setUid(UUID.randomUUID().toString());
                mapping.setLabel("Label-"+j);
                mapping.setVariable("Variable-"+j);
                operation.addMapping(mappingAPI.create(mapping, null, null, null));
            }
            operationAPI.create(operation, null, null, null);
        }

        System.out.println("POPULATION DONE");

        List<org.epos.eposdatamodel.Operation> operationList = operationAPI.retrieveAll();

        System.out.println(operationList);


        List<org.epos.eposdatamodel.Mapping> mappingList = mappingAPI.retrieveAll();
        System.out.println(mappingList);

    }

}
