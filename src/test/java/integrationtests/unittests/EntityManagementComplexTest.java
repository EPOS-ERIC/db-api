package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.WebserviceIdentifier;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.WebService;
import org.epos.eposdatamodel.Identifier;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntityManagementComplexTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGet() {

        AbstractAPI webserviceAPI = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        AbstractAPI identifierAPI = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());

        Identifier identifier = new Identifier();
        identifier.setInstanceId(UUID.randomUUID().toString());
        identifier.setMetaId(UUID.randomUUID().toString());
        identifier.setUid(UUID.randomUUID().toString());
        identifier.setType("TYPE");
        identifier.setIdentifier("012345678900");

        LinkedEntity le = identifierAPI.create(identifier, null, null, null);

        //LOG.info("CREATED:\n"+identifier.toString());
        //LOG.info("CREATED:\n"+le.toString());

        WebService webservice = new WebService();
        webservice.setInstanceId(UUID.randomUUID().toString());
        webservice.setMetaId(UUID.randomUUID().toString());
        webservice.setUid(UUID.randomUUID().toString());
        webservice.setDescription("Test description");
        webservice.setName("Test name");
        webservice.setIdentifier(List.of(le));

        LinkedEntity newLe = webserviceAPI.create(webservice, null, null, null);

        //LOG.info("CREATED:\n"+webservice.toString());
        //LOG.info("CREATED:\n"+newLe.toString());

        Identifier retrievedIdentifier = (Identifier) identifierAPI.retrieve(identifier.getInstanceId());

        //LOG.info("RECEIVED:\n"+retrievedIdentifier.toString());

        WebService retrievedWebservice = (WebService) webserviceAPI.retrieve(webservice.getInstanceId());

        //LOG.info("RECEIVED:\n"+retrievedWebservice.toString());

        assertAll(
                () -> assertEquals(identifier.getType(), retrievedIdentifier.getType()),
                () -> assertEquals(identifier.getIdentifier(), retrievedIdentifier.getIdentifier()),
                () -> assertEquals(identifier.getUid(), retrievedIdentifier.getUid()),
                () -> assertEquals(identifier.getInstanceId(), retrievedIdentifier.getInstanceId()),
                () -> assertEquals(identifier.getMetaId(), retrievedIdentifier.getMetaId()),
                () -> assertEquals(webservice.getUid(), retrievedWebservice.getUid()),
                () -> assertEquals(webservice.getInstanceId(), retrievedWebservice.getInstanceId()),
                () -> assertEquals(webservice.getMetaId(), retrievedWebservice.getMetaId()),
                () -> assertEquals(webservice.getIdentifier(), retrievedWebservice.getIdentifier()),
                () -> assertEquals(webservice.getName(), retrievedWebservice.getName()),
                () -> assertEquals(webservice.getDescription(), retrievedWebservice.getDescription())
        );

        AbstractAPI dbapi = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());
        Class clazz = AbstractAPI.retrieveClass(EntityNames.IDENTIFIER.name());
        List list = dbapi.getDbaccess().getOneFromDBByInstanceId(le.getInstanceId(), clazz);


        List listRelations = dbapi.getDbaccess().getAllFromDB(WebserviceIdentifier.class);
        for(Object webserviceIdentifier : listRelations){
            WebserviceIdentifier webserviceIdentifier1 = (WebserviceIdentifier) webserviceIdentifier;
            if(webserviceIdentifier1.getIdentifierInstance().getInstanceId().equals(le.getInstanceId())
                && (webserviceIdentifier1.getWebserviceInstance().getInstanceId().equals(newLe.getInstanceId()))){
                dbapi.getDbaccess().deleteObject(webserviceIdentifier1);
                dbapi.getDbaccess().deleteObject(list.get(0));
            }
        }
    }

}
