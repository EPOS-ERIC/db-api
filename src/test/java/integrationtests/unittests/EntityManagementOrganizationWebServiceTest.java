package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Organization;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementOrganizationWebServiceTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testWebServiceProviderRelation() {

        // 1. Create the Organization (The Provider)
        Organization organization = new Organization();
        // Generate unique UIDs to ensure test isolation
        String orgUid = "https://www.epos-eu.org/epos-dcat-ap/Organization/TestProvider_" + UUID.randomUUID();

        organization.setUid(orgUid);
        organization.setLegalName(List.of("Test Provider Organization"));
        // Set IDs explicitly or let the API handle it (setting here for clarity)
        organization.setInstanceId(UUID.randomUUID().toString());
        organization.setMetaId(UUID.randomUUID().toString());

        // Persist Organization
        LinkedEntity organizationLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name())
                .create(organization, StatusType.PUBLISHED, null, null);

        assertNotNull(organizationLinkedEntity, "Organization LinkedEntity should not be null after creation");

        // 2. Create the WebService and link the Provider
        WebService webService = new WebService();
        String wsUid = "https://www.epos-eu.org/epos-dcat-ap/WebService/TestService_" + UUID.randomUUID();

        webService.setUid(wsUid);
        webService.setName("Test WebService Linked to Org");
        webService.setDescription("A webservice to test provider relationship");
        webService.setInstanceId(UUID.randomUUID().toString());
        webService.setMetaId(UUID.randomUUID().toString());

        // SET THE RELATIONSHIP: Organization -> provides -> WebService
        // We use the LinkedEntity returned from the Organization creation
        webService.setProvider(organizationLinkedEntity);

        // Persist WebService
        LinkedEntity webServiceLinkedEntity = AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .create(webService, StatusType.PUBLISHED, null, null);

        assertNotNull(webServiceLinkedEntity, "WebService LinkedEntity should not be null after creation");

        // 3. Retrieve the WebService and Verify the Relationship
        WebService retrievedWebService = (WebService) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name())
                .retrieve(webServiceLinkedEntity.getInstanceId());

        System.out.println("Retrieved WebService: " + retrievedWebService.getName());
        if(retrievedWebService.getProvider() != null) {
            System.out.println("Provider UID: " + retrievedWebService.getProvider().getUid());
        }

        // Assertions
        assertNotNull(retrievedWebService, "Retrieved WebService should not be null");
        assertNotNull(retrievedWebService.getProvider(), "The Provider field should not be null");

        // Check that the UID of the provider matches the Organization we created
        assertEquals(orgUid, retrievedWebService.getProvider().getUid(),
                "The WebService provider UID should match the Organization UID");

        // Optional: Verify instanceId match if available in the LinkedEntity
        assertEquals(organizationLinkedEntity.getInstanceId(), retrievedWebService.getProvider().getInstanceId(),
                "The WebService provider InstanceID should match");
    }
}