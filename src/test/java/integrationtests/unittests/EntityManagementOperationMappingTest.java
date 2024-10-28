package integrationtests.unittests;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Mapping;
import org.epos.eposdatamodel.Operation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementOperationMappingTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGet() {
        Operation operation = new Operation();
        operation.setMethod("GET");
        operation.setTemplate("http://template{?test,test1}");
        operation.setReturns(List.of("application/json"));
        operation.setStatus(StatusType.DRAFT);


        AbstractAPI apiOperation = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name());
        LinkedEntity operationLinkedEntity = apiOperation.create(operation, null);

        operation.setInstanceId(operationLinkedEntity.getInstanceId());
        operation.setMetaId(operationLinkedEntity.getMetaId());
        operation.setUid(operationLinkedEntity.getUid());

        Mapping mapping1 = new Mapping();
        mapping1.setVariable("test1");
        mapping1.setLabel("label1");
        mapping1.setStatus(StatusType.DRAFT);

        AbstractAPI apiMapping = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name());
        LinkedEntity mapping1LinkedEntity = apiMapping.create(mapping1, null);

        mapping1.setInstanceId(operationLinkedEntity.getInstanceId());
        mapping1.setMetaId(operationLinkedEntity.getMetaId());
        mapping1.setUid(operationLinkedEntity.getUid());

        operation.setMapping(List.of(mapping1LinkedEntity));

        System.out.println(operation.getMapping());

        operationLinkedEntity = apiOperation.create(operation, null);

        assertEquals(1, ((Operation) LinkedEntityAPI.retrieveFromLinkedEntity(operationLinkedEntity)).getMapping().size());
    }

}
