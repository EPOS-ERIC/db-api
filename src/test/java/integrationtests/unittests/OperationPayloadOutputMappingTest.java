package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.OutputMappingAPI;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class OperationPayloadOutputMappingTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testOperation() {


        OutputMapping outputMapping = new OutputMapping();
        outputMapping.setUid("output_mapping");
        outputMapping.setProperty("TEST");
        outputMapping.setRange("TEST2");
        outputMapping.setLabel("TEST3");
        outputMapping.setRequired("TRUE");
        outputMapping.setValuePattern("TEST4");
        outputMapping.setStatus(StatusType.DRAFT);

        LinkedEntity outputmappingLE = AbstractAPI.retrieveAPI("OUTPUTMAPPING").create(outputMapping,null,null,null);

        LinkedEntity temp = new LinkedEntity();
        temp.setUid("operation");
        temp.setEntityType("OPERATION");

        Payload payload = new Payload();
        payload.setUid("payload");
        payload.setSupportedOperation(temp);
        payload.setOutputMapping(List.of(outputmappingLE));
        payload.setStatus(StatusType.DRAFT);

        LinkedEntity payloadLE = AbstractAPI.retrieveAPI("PAYLOAD").create(payload,null,null,null);

        Operation operation = new Operation();
        operation.setUid("operation");
        operation.setReturns(List.of("application/json"));
        operation.setMethod("GET");
        operation.setTemplate("template");
        operation.setPayload(List.of(payloadLE));
        operation.setStatus(StatusType.DRAFT);

        AbstractAPI.retrieveAPI("OPERATION").create(operation,null,null,null);


        AbstractAPI.retrieveAPI("PAYLOAD").retrieveAll().forEach(e->{
            System.out.println(e);
        });

        AbstractAPI.retrieveAPI("OPERATION").retrieveAll().forEach(e->{
            System.out.println(e);
        });

        AbstractAPI.retrieveAPI("OUTPUTMAPPING").retrieveAll().forEach(e->{
            System.out.println(e);
        });
    }

}
