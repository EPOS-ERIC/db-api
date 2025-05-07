package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DataProductDeepTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testDataProviders() {

        List<EPOSDataModelEntity> classes = new ArrayList<>();

        DataProduct dataProduct = new DataProduct();
        classes.add(dataProduct);
        dataProduct.setUid("https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001");
        dataProduct.setQualityAssurance("TEST QUALITY ASSURANCE");
        dataProduct.setHasQualityAnnotation("TEST HAS QUALITY ANNOTATION");

        Identifier identifier1 = new Identifier();
        classes.add(identifier1);
        identifier1.setUid("Identifier/1");
        identifier1.setIdentifier("https://www.epos-eu.org/epos-dcat-ap/Seismology/Dataset/001");
        LinkedEntity linkedEntity1 = new LinkedEntity();
        linkedEntity1.setUid("Identifier/1");
        linkedEntity1.setEntityType(EntityNames.IDENTIFIER.name());
        dataProduct.addIdentifier(linkedEntity1);

        Identifier identifier2 = new Identifier();
        classes.add(identifier2);
        identifier2.setUid("Identifier/2");
        identifier2.setType("DDSS-ID");
        identifier2.setIdentifier("WP08-DDSS-001");
        LinkedEntity linkedEntity2 = new LinkedEntity();
        linkedEntity2.setUid("Identifier/2");
        linkedEntity2.setEntityType(EntityNames.IDENTIFIER.name());
        dataProduct.addIdentifier(linkedEntity2);

        dataProduct.setTitle(List.of("Primary Seismic Waveform Data"));
        dataProduct.setDescription(List.of("Primary Seismic Waveform Data description"));
        dataProduct.setCreated("2016-01-01T00:00:00Z");
        dataProduct.setIssued("2016-01-01T00:00:00Z");
        dataProduct.setModified("2016-01-01T00:00:00Z");
        dataProduct.setVersionInfo("1.0.0");
        dataProduct.setType("http://purl.org/dc/dcmitype/Collection");
        dataProduct.setAccrualPeriodicity("http://purl.org/cld/freq/continuous");

        Category category1 = new Category();
        classes.add(category1);
        category1.setUid("epos:SeismicWaveform");
        LinkedEntity linkedEntity3 = new LinkedEntity();
        linkedEntity3.setUid("epos:SeismicWaveform");
        linkedEntity3.setEntityType(EntityNames.CATEGORY.name());
        dataProduct.addCategory(linkedEntity3);

        dataProduct.addKeywords("seismic waveform,continuous waveform,mseed");


        Organization dataprovider1 = new Organization();
        classes.add(dataprovider1);
        dataprovider1.setUid("PIC:000518944");

        Identifier identifier3 = new Identifier();
        classes.add(identifier3);
        identifier3.setUid("Identifier/1");
        identifier3.setIdentifier("000518944");
        identifier3.setType("PIC");
        LinkedEntity linkedEntity4 = new LinkedEntity();
        linkedEntity4.setUid("Identifier/1");
        linkedEntity4.setEntityType(EntityNames.IDENTIFIER.name());
        dataprovider1.setIdentifier(List.of(linkedEntity4));

        dataprovider1.setLegalName(List.of("Institute test2"));
        dataprovider1.setLeiCode("Legal Entity Identifier Search");

        Address address1 = new Address();
        classes.add(address1);
        address1.setUid("Address/1");
        address1.setPostalCode("00143");
        address1.street("address, 002");
        address1.setLocality("Rome");
        address1.setCountry("Italy");
        LinkedEntity linkedEntity5 = new LinkedEntity();
        linkedEntity5.setUid("Address/1");
        linkedEntity5.setEntityType(EntityNames.ADDRESS.name());
        dataprovider1.setAddress(linkedEntity5);


        dataprovider1.setLogo("http://www.test2.it/logo.png");
        dataprovider1.setURL("http://www.test2.it");
        dataprovider1.setEmail(List.of("test2_institute@email.it"));
        dataprovider1.setTelephone(List.of("+0302206911"));

        ContactPoint contactPoint1 = new ContactPoint();
        classes.add(contactPoint1);
        contactPoint1.setUid("http://orcid.org/0000-0001-7750-0000/legalContact");
        LinkedEntity linkedEntity6 = new LinkedEntity();
        linkedEntity6.setUid("http://orcid.org/0000-0001-7750-0000/legalContact");
        linkedEntity6.setEntityType(EntityNames.CONTACTPOINT.name());
        dataprovider1.addContactPoint(linkedEntity6);

        LinkedEntity linkedEntity7 = new LinkedEntity();
        linkedEntity7.setUid("PIC:000518944");
        linkedEntity7.setEntityType(EntityNames.ORGANIZATION.name());

        dataProduct.addContactPoint(linkedEntity6);
        dataProduct.addPublisher(linkedEntity7);
        Location location1 = new Location();
        classes.add(location1);
        location1.setUid("Location/1");
        location1.setLocation("POLYGON()");
        LinkedEntity linkedEntity8 = new LinkedEntity();
        linkedEntity8.setUid("Location/1");
        linkedEntity8.setEntityType(EntityNames.LOCATION.name());
        dataProduct.addSpatialExtentItem(linkedEntity8);

        for(EPOSDataModelEntity entity : classes) {
            AbstractAPI.retrieveAPI(EntityNames.valueOf(entity.getClass().getSimpleName().toUpperCase(Locale.ROOT)).name()).create(entity, null, null, null);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveAll()){
            System.out.println(object);
        }

    }

}
