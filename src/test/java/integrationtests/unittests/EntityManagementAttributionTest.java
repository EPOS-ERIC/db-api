package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.Attribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntityManagementAttributionTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAttribution() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ATTRIBUTION.name());

        LinkedEntity org = new LinkedEntity();
        org.setInstanceId(UUID.randomUUID().toString());
        org.setMetaId(UUID.randomUUID().toString());
        org.setUid("test");
        org.setEntityType(EntityNames.ORGANIZATION.name());

        Attribution attribution = new Attribution();
        attribution.setInstanceId(UUID.randomUUID().toString());
        attribution.setMetaId(UUID.randomUUID().toString());
        attribution.setUid("testattribution");
        attribution.setAgent(org);
        attribution.setRole(List.of("testrole","testrole2"));


        LOG.info("CREATED:\n"+attribution.toString());

        api.create(attribution, StatusType.PUBLISHED, null, null);

        Attribution retrievedAddress = (Attribution) api.retrieve(attribution.getInstanceId());

        LOG.info("RECEIVED:\n"+retrievedAddress.toString());

        assertAll(
                () -> assertEquals(attribution.getAgent(),retrievedAddress.getAgent()),
                () -> assertEquals(attribution.getRole(),retrievedAddress.getRole())
        );
    }

}
