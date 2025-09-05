package integrationtests.unittests;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.*;

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


        //LOG.info("CREATED:\n"+organization.toString());

        LinkedEntity linkedEntity = api.create(organization, null, null, null);

        Organization organization1 = new Organization();
        organization1.setInstanceId(UUID.randomUUID().toString());
        organization1.setMetaId(UUID.randomUUID().toString());
        organization1.setUid(UUID.randomUUID().toString());
        organization1.setLegalName(List.of("Son 1 organization"));
        organization1.setMemberOf(List.of(linkedEntity));

        Organization organization2 = new Organization();
        organization2.setInstanceId(UUID.randomUUID().toString());
        organization2.setMetaId(UUID.randomUUID().toString());
        organization2.setUid(UUID.randomUUID().toString());
        organization2.setLegalName(List.of("Son 2 organization"));
        organization2.setMemberOf(List.of(linkedEntity));

        LinkedEntity linkedEntity1 = api.create(organization1, null, null, null);
        LinkedEntity linkedEntity2 = api.create(organization2, null, null, null);

        List<Organization> organizations = api.retrieveAll();

        //LOG.info("RECEIVED:\n"+organizations.toString());

        for (Organization org : organizations) {
            // only take into account the organization with legalname
            if (org.getLegalName() != null && !org.getLegalName().isEmpty()) {
                String mainOrganizationLegalName = String.join(".", org.getLegalName());
                System.out.println(mainOrganizationLegalName);

                if (org.getMemberOf() == null) {
                    organizations.stream().filter(organization3 -> organization3.getMemberOf()!=null)
                            .forEach(organization3 -> {
                                Optional<LinkedEntity> resultEntities = organization3.getMemberOf().stream()
                                        .filter(linkedEntity3 -> linkedEntity3.getInstanceId().equals(organization3.getInstanceId()))
                                        .findAny();
                                if (resultEntities.isPresent()) {
                                    if (organization3.getLegalName() != null && !organization3.getLegalName().isEmpty()) {
                                        String relatedOrganizationLegalName = String.join(".", organization3.getLegalName());
                                        System.out.println(relatedOrganizationLegalName);
                                    }
                                }
                            });
                }
            }
        }

        assertNotNull(organizations);
    }


}
