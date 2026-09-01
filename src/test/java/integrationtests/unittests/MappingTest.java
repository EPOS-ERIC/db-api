package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class MappingTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testMapping() {
        Mapping mapping1 = new Mapping();
        mapping1.setUid(UUID.randomUUID().toString());
        mapping1.setStatus(StatusType.DRAFT);
        mapping1.setLabel("label1");
        mapping1.setVariable("var1");
        mapping1.setDefaultValue("param1");
        mapping1.setParamValue(List.of("param1"));

        LinkedEntity linkedEntity1 = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).create(mapping1, null, null, null);

        Operation operation = new Operation();
        operation.setUid(UUID.randomUUID().toString());
        operation.setStatus(StatusType.DRAFT);
        operation.setMethod("GET");
        operation.setTemplate("Template");
        operation.setReturns(List.of("returns", "returns", "application/json"));
        operation.setMapping(List.of(linkedEntity1));

        LinkedEntity linkedEntity2 = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).create(operation, null, null, null);

        List<Operation> operations = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).retrieveAll();
        List<Mapping> mappings = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();

        System.out.println(operations);
        System.out.println(mappings);

        assertAll(
                () -> assertEquals(1,operations.size()),
                () -> assertEquals(1,mappings.size()),
                () -> assertEquals(new HashSet<>(List.of("returns", "application/json")),
                        new HashSet<>(operations.get(0).getReturns()))
        );
    }

    @Test
    @Order(2)
    public void testUpdateMapping() {

        List<Operation> tmpoperations = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).retrieveAll();
        List<Mapping> tmpmappings = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();

        Operation operation = tmpoperations.get(0);
        Mapping mapping = tmpmappings.get(0);

        mapping.setStatus(StatusType.PUBLISHED);
        operation.setStatus(StatusType.PUBLISHED);

        LinkedEntity linkedEntity1 = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).create(mapping, null, null, null);
        LinkedEntity linkedEntity2 = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).create(operation, null, null, null);

        List<Operation> operations = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).retrieveAll();
        List<Mapping> mappings = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();

        System.out.println(operations);
        System.out.println(mappings);

        assertAll(
                () -> assertEquals(1,operations.size()),
                () -> assertEquals(StatusType.PUBLISHED,operations.get(0).getStatus()),
                () -> assertEquals(1,mappings.size()),
                () -> assertEquals(StatusType.PUBLISHED,mappings.get(0).getStatus())
        );
    }

    @Test
    @Order(3)
    public void testNewDraftMapping() {

        List<Operation> tmpoperations = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).retrieveAll();
        List<Mapping> tmpmappings = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();

        Operation operation = tmpoperations.get(0);
        Mapping mapping = tmpmappings.get(0);

        //mapping.setStatus(StatusType.DRAFT);
        operation.setStatus(StatusType.DRAFT);

        LinkedEntity linkedEntity1 = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).create(mapping, null, null, null);
        LinkedEntity linkedEntity2 = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).create(operation, null, null, null);

        List<Operation> operations = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).retrieveAll();
        List<Mapping> mappings = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();

        for (Operation operation1 : operations) {
            System.out.println(operation1);
        }
        for (Mapping mapping1 : mappings) {
            System.out.println(mapping1);
        }

        assertAll(
                () -> assertEquals(2,operations.size()),
                () -> assertEquals(2,mappings.size())
        );
    }

    @Test
    @Order(4)
    public void testAddParameterMapping() {

        List<Mapping> tmpmappings = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();

        Mapping mapping = tmpmappings.stream().filter(o -> o.getStatus().equals(StatusType.DRAFT)).findFirst().get();

        mapping.getParamValue().add("param2");
        LinkedEntity linkedEntity1 = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).create(mapping, null, null, null);

        List<Operation> operations = AbstractAPI.retrieveAPI(EntityNames.OPERATION.name()).retrieveAll();
        List<Mapping> mappings = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieveAll();

        for (Operation operation1 : operations) {
            System.out.println(operation1);
        }
        for (Mapping mapping1 : mappings) {
            System.out.println(mapping1);
        }

        assertAll(
                () -> assertEquals(2,operations.size()),
                () -> assertEquals(2,mappings.size())
        );
    }

    @Test
    @Order(5)
    public void testDeduplicateParameterMappingValues() {
        Mapping mapping = new Mapping();
        mapping.setUid(UUID.randomUUID().toString());
        mapping.setStatus(StatusType.DRAFT);
        mapping.setLabel("dedupe");
        mapping.setVariable("dedupe");
        mapping.setParamValue(List.of("alpha", "beta", "alpha", "gamma", "beta"));

        LinkedEntity linkedEntity = AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).create(mapping, null, null, null);
        Mapping stored = (Mapping) AbstractAPI.retrieveAPI(EntityNames.MAPPING.name()).retrieve(linkedEntity.getInstanceId());

        assertAll(
                () -> assertEquals(3, stored.getParamValue().size()),
                () -> assertEquals(3, new HashSet<>(stored.getParamValue()).size())
        );
    }

}
