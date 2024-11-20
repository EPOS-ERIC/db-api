package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementOrganizationsTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetAddress() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());

        Organization organization = new Organization();
        organization.setInstanceId(UUID.randomUUID().toString());
        organization.setMetaId(UUID.randomUUID().toString());
        organization.setUid(UUID.randomUUID().toString());
        organization.setLegalName(List.of("Father organization"));


        LOG.info("CREATED:\n"+organization.toString());

        LinkedEntity linkedEntity = api.create(organization, null, null, null);

        Organization organization1 = new Organization();
        organization1.setInstanceId(UUID.randomUUID().toString());
        organization1.setMetaId(UUID.randomUUID().toString());
        organization1.setUid(UUID.randomUUID().toString());
        organization1.setLegalName(List.of("Son organization"));
        organization1.setMemberOf(List.of(linkedEntity));

        LinkedEntity linkedEntity1 = api.create(organization1, null, null, null);

        Organization sonReOrganization2 = (Organization) api.retrieve(linkedEntity1.getInstanceId());

        LOG.info("RECEIVED:\n"+sonReOrganization2.toString());

        assertNotNull(sonReOrganization2);
        assertNotNull(sonReOrganization2.getMemberOf());
    }


}
