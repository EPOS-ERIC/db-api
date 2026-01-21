package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplication;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SoftwareFakedIngestorTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testDataProviders() {

        List<EPOSDataModelEntity> classes = new ArrayList<>();

        LinkedEntity category = new LinkedEntity();
        category.setInstanceId("22d0c8d6-47d1-4dc4-ba4b-361984432de2");
        category.setMetaId("88b6a577-3dc1-4503-9271-f71fa333ed70");
        category.setUid("epos:SourceAndShakingParametersEstimation");
        category.setEntityType(EntityNames.CATEGORY.name());

        LinkedEntity contactpoint = new LinkedEntity();
        contactpoint.setInstanceId("14434ee4-7588-48b0-a4d6-045a277462c2");
        contactpoint.setMetaId("5ebcb384-8bfe-4252-a9fa-5fb04f133942");
        contactpoint.setUid("https://orcid.org/0000-0002-9824-7962/contactPoint");
        contactpoint.setEntityType(EntityNames.CONTACTPOINT.name());

        LinkedEntity identifier = new LinkedEntity();
        identifier.setInstanceId("d3c67d1c-a660-4474-93f3-dd8b4118075f");
        identifier.setMetaId("1e38dbdf-9c35-421c-868f-765fabcf5991");
        identifier.setUid("_:09d9ebace798fa791c692ee52eee9d74");
        identifier.setEntityType(EntityNames.IDENTIFIER.name());

        SoftwareApplication softwareApplication = new SoftwareApplication();
        softwareApplication.setInstanceId("12724e18-1fdd-4973-8ba7-cdd398bd1236");
        softwareApplication.setMetaId("88b6a577-3dc1-4503-9271-f71fa333ed70");
        softwareApplication.setUid("file:///anthropogenic_hazards/software/SeismogramPicks");
        softwareApplication.setCategory(List.of(category));
        softwareApplication.setContactPoint(List.of(contactpoint));
        softwareApplication.setIdentifier(List.of(identifier));
        softwareApplication.setKeywords("format conversion,\twaveform viewing,\tpicking on waveform");
        softwareApplication.setName("Seismogram Picking Tool");
        softwareApplication.setDescription("Performs selection of phases and picks on seismograms");
        softwareApplication.setStatus(StatusType.DRAFT);

        AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name()).create(softwareApplication, null, null, null);

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name()).retrieveAll()){
            System.out.println(object);
        }

    }

}
