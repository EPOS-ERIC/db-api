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

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ExampleTest extends TestcontainersLifecycle {



    @Test
    @Order(1)
    public void testCreateAndGet() {

        List<EPOSDataModelEntity> classes = new ArrayList<>();

        classes.add(EPOSTestDataFactory.createDatasetTest1());
        classes.add(EPOSTestDataFactory.createDatasetTest2());
        classes.add(EPOSTestDataFactory.createDistributionTest1());
        classes.add(EPOSTestDataFactory.createDistributionTest2());
        classes.add(EPOSTestDataFactory.createWebServiceTest2());
        classes.addAll(EPOSTestDataFactory.createAllTestCategories());
        classes.addAll(EPOSTestDataFactory.createAllTestCategorySchemes());
        classes.add(EPOSTestDataFactory.createTestAddress());
        classes.add(EPOSTestDataFactory.createTestPerson());
        classes.add(EPOSTestDataFactory.createGlobalLocation());
        classes.add(EPOSTestDataFactory.createPointLocation());
        classes.add(EPOSTestDataFactory.createScientificContactPoint());

        for(EPOSDataModelEntity eposDataModelEntity : classes){
            eposDataModelEntity.setEditorId("JUNIT_TEST");
            eposDataModelEntity.setFileProvenance("JUNIT_TEST");
            eposDataModelEntity.setStatus(StatusType.PUBLISHED);
            System.out.println(eposDataModelEntity);
        }

        System.out.println("---------------------------------------");

        for(EPOSDataModelEntity eposDataModelEntity : classes){
            if(eposDataModelEntity.getUid()=="" || eposDataModelEntity.getUid()==null) System.out.println("UID IS NULL OR EMPTY "+eposDataModelEntity.toString());
            if(eposDataModelEntity.getEditorId()=="" || eposDataModelEntity.getEditorId()==null) System.out.println("EDITOR ID IS NULL OR EMPTY "+eposDataModelEntity.toString());
            System.out.println(eposDataModelEntity);
        }

        List<LinkedEntity> linkedEntities = new ArrayList<>();
        for(EPOSDataModelEntity eposDataModelEntity : classes){
            //System.out.println("[ADDING TO DATABASE] "+eposDataModelEntity);
            try {
                AbstractAPI api = AbstractAPI.retrieveAPI(eposDataModelEntity.getClass().getSimpleName().toUpperCase());
                System.out.println("Ingesting -> "+eposDataModelEntity);
                linkedEntities.add(api.create(eposDataModelEntity, StatusType.PUBLISHED, null, null));
            }catch(Exception apiCreationException){
                apiCreationException.printStackTrace();
                System.out.println("[ERROR] ON: "+ eposDataModelEntity.toString()+"\n[EXCEPTION]: "+apiCreationException.getLocalizedMessage());
            }
        }

        for(LinkedEntity linkedEntity : linkedEntities){
            System.out.println(AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType().toUpperCase()).name()).retrieve(linkedEntity.getInstanceId()));
        }
    }

}
