package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.OrganizationAPI;
import org.epos.eposdatamodel.Organization;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrganizationSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingTheFullGraph() {
        OrganizationAPI api = (OrganizationAPI) AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());
        Organization organization = new Organization();
        organization.setUid("summary/organization/" + UUID.randomUUID());
        organization.setAcronym("SUMMARY");
        organization.addLegalName("Summary organization");
        api.create(organization, null, null, null);

        List<Organization> summaries = api.retrieveAllSummary();
        Organization summary = summaries.stream()
                .filter(candidate -> organization.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(organization.getUid(), summary.getUid());
        assertEquals("SUMMARY", summary.getAcronym());
        assertEquals(List.of("Summary organization"), summary.getLegalName());
        assertNotNull(summary.getStatus());
        assertTrue(summary.getIdentifier() == null || summary.getIdentifier().isEmpty());
    }
}
