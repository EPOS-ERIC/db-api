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
import java.util.Locale;

public class DataProvidersWithStatusTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testDataProviders() {

        List<EPOSDataModelEntity> classes = new ArrayList<>();

        Organization dataprovider1 = new Organization();
        classes.add(dataprovider1);
        dataprovider1.setUid("PIC:000518944");

        Identifier identifier1 = new Identifier();
        classes.add(identifier1);
        identifier1.setUid("Identifier/1");
        identifier1.setIdentifier("000518944");
        identifier1.setType("PIC");
        LinkedEntity linkedEntity1 = new LinkedEntity();
        linkedEntity1.setUid("Identifier/1");
        linkedEntity1.setEntityType(EntityNames.IDENTIFIER.name());
        dataprovider1.setIdentifier(List.of(linkedEntity1));

        dataprovider1.setLegalName(List.of("Institute test2"));
        dataprovider1.setLeiCode("Legal Entity Identifier Search");

        Address address1 = new Address();
        classes.add(address1);
        address1.setUid("Address/1");
        address1.setPostalCode("00143");
        address1.street("address, 002");
        address1.setLocality("Rome");
        address1.setCountry("Italy");
        LinkedEntity linkedEntity2 = new LinkedEntity();
        linkedEntity2.setUid("Address/1");
        linkedEntity2.setEntityType(EntityNames.ADDRESS.name());
        dataprovider1.setAddress(linkedEntity2);

        dataprovider1.setLogo("http://www.test2.it/logo.png");
        dataprovider1.setURL("http://www.test2.it");
        dataprovider1.setEmail(List.of("test2_institute@email.it"));
        dataprovider1.setTelephone(List.of("+0302206911"));

        ContactPoint contactPoint1 = new ContactPoint();
        classes.add(contactPoint1);
        contactPoint1.setUid("http://orcid.org/0000-0001-7750-0000/legalContact");
        LinkedEntity linkedEntity3 = new LinkedEntity();
        linkedEntity3.setUid("http://orcid.org/0000-0001-7750-0000/legalContact");
        linkedEntity3.setEntityType(EntityNames.CONTACTPOINT.name());
        dataprovider1.addContactPoint(linkedEntity3);

        for(EPOSDataModelEntity entity : classes) {
            AbstractAPI.retrieveAPI(EntityNames.valueOf(entity.getClass().getSimpleName().toUpperCase(Locale.ROOT)).name()).create(entity, null, null, null);
        }

        System.out.println("RESULT RIGHT: "+AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name()).retrieveAllWithStatus(StatusType.DRAFT));
        System.out.println("RESULT WRONG: "+AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name()).retrieveAllWithStatus(StatusType.PUBLISHED));
        System.out.println("RESULT ALL: "+AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name()).retrieveAll());

    }

}
