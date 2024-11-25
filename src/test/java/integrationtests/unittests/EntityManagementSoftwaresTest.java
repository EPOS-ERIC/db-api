package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.Softwareapplication;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.epos.eposdatamodel.SoftwareApplication;
import org.epos.eposdatamodel.SoftwareSourceCode;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementSoftwaresTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testSoftwareApplicationCreation() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());

        LinkedEntity contactpoint = new LinkedEntity();
        contactpoint.setInstanceId(UUID.randomUUID().toString());
        contactpoint.setMetaId(UUID.randomUUID().toString());
        contactpoint.setUid(UUID.randomUUID().toString());
        contactpoint.setEntityType(EntityNames.CONTACTPOINT.name());

        LinkedEntity category = new LinkedEntity();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid(UUID.randomUUID().toString());
        category.setEntityType(EntityNames.CATEGORY.name());

        LinkedEntity operation = new LinkedEntity();
        operation.setInstanceId(UUID.randomUUID().toString());
        operation.setMetaId(UUID.randomUUID().toString());
        operation.setUid(UUID.randomUUID().toString());
        operation.setEntityType(EntityNames.OPERATION.name());

        LinkedEntity identifier = new LinkedEntity();
        identifier.setInstanceId(UUID.randomUUID().toString());
        identifier.setMetaId(UUID.randomUUID().toString());
        identifier.setUid(UUID.randomUUID().toString());
        identifier.setEntityType(EntityNames.IDENTIFIER.name());

        LinkedEntity parameter = new LinkedEntity();
        parameter.setInstanceId(UUID.randomUUID().toString());
        parameter.setMetaId(UUID.randomUUID().toString());
        parameter.setUid(UUID.randomUUID().toString());
        parameter.setEntityType(EntityNames.SOFTWAREAPPLICATIONPARAMETER.name());

        SoftwareApplication softwareApplication = new SoftwareApplication();
        softwareApplication.setInstanceId(UUID.randomUUID().toString());
        softwareApplication.setMetaId(UUID.randomUUID().toString());
        softwareApplication.setUid(UUID.randomUUID().toString());
        softwareApplication.setName("software application");
        softwareApplication.setDescription("software application description");
        softwareApplication.setVersion("1.0.0");
        softwareApplication.setContactPoint(List.of(contactpoint));
        softwareApplication.setCategory(List.of(category));
        softwareApplication.setDownloadURL("downloadurl");
        softwareApplication.setInstallURL("installurl");
        softwareApplication.setLicenseURL("licenseurl");
        softwareApplication.setMainEntityOfPage("mainentityofpage");
        softwareApplication.setIdentifier(List.of(identifier));
        softwareApplication.setRequirements("requirements");
        softwareApplication.setRelation(List.of(operation));
        softwareApplication.setParameter(List.of(parameter));

        api.create(softwareApplication, null, null, null);

        System.out.println(api.retrieveAll());
    }

    @Test
    @Order(2)
    public void testSoftwareSourceCodeCreation() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());

        LinkedEntity contactpoint = new LinkedEntity();
        contactpoint.setInstanceId(UUID.randomUUID().toString());
        contactpoint.setMetaId(UUID.randomUUID().toString());
        contactpoint.setUid(UUID.randomUUID().toString());
        contactpoint.setEntityType(EntityNames.CONTACTPOINT.name());

        LinkedEntity category = new LinkedEntity();
        category.setInstanceId(UUID.randomUUID().toString());
        category.setMetaId(UUID.randomUUID().toString());
        category.setUid(UUID.randomUUID().toString());
        category.setEntityType(EntityNames.CATEGORY.name());

        LinkedEntity operation = new LinkedEntity();
        operation.setInstanceId(UUID.randomUUID().toString());
        operation.setMetaId(UUID.randomUUID().toString());
        operation.setUid(UUID.randomUUID().toString());
        operation.setEntityType(EntityNames.OPERATION.name());

        LinkedEntity identifier = new LinkedEntity();
        identifier.setInstanceId(UUID.randomUUID().toString());
        identifier.setMetaId(UUID.randomUUID().toString());
        identifier.setUid(UUID.randomUUID().toString());
        identifier.setEntityType(EntityNames.IDENTIFIER.name());

        LinkedEntity parameter = new LinkedEntity();
        parameter.setInstanceId(UUID.randomUUID().toString());
        parameter.setMetaId(UUID.randomUUID().toString());
        parameter.setUid(UUID.randomUUID().toString());
        parameter.setEntityType(EntityNames.SOFTWAREAPPLICATIONPARAMETER.name());

        SoftwareSourceCode softwareSourceCode = new SoftwareSourceCode();
        softwareSourceCode.setInstanceId(UUID.randomUUID().toString());
        softwareSourceCode.setMetaId(UUID.randomUUID().toString());
        softwareSourceCode.setUid(UUID.randomUUID().toString());
        softwareSourceCode.setName("software application");
        softwareSourceCode.setDescription("software application description");
        softwareSourceCode.setContactPoint(List.of(contactpoint));
        softwareSourceCode.setCategory(List.of(category));
        softwareSourceCode.setDownloadURL("downloadurl");
        softwareSourceCode.setLicenseURL("licenseurl");
        softwareSourceCode.setIdentifier(List.of(identifier));
        softwareSourceCode.setRelation(List.of(operation));
        softwareSourceCode.setCodeRepository("coderepository");
        softwareSourceCode.setMainEntityofPage("mainentityofpage");
        softwareSourceCode.setProgrammingLanguage(List.of("Java"));
        softwareSourceCode.setSoftwareVersion("1.0.0");
        softwareSourceCode.setRuntimePlatform("runtimeplatform");


        api.create(softwareSourceCode, null, null, null);

        System.out.println(api.retrieveAll());
    }


}
