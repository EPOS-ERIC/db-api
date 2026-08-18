package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.ContactPointAPI;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.ContactPoint;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContactPointSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        ContactPointAPI api = (ContactPointAPI) AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name());
        ContactPoint contactPoint = new ContactPoint();
        contactPoint.setUid("summary/contact-point/" + UUID.randomUUID());
        contactPoint.setRole("Technical contact");
        contactPoint.addEmail("summary@example.org");
        api.create(contactPoint, null, null, null);

        ContactPoint summary = api.retrieveAllSummary().stream()
                .filter(candidate -> contactPoint.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(contactPoint.getUid(), summary.getUid());
        assertEquals("Technical contact", summary.getRole());
        assertNotNull(summary.getStatus());
        assertTrue(summary.getEmail() == null || summary.getEmail().isEmpty());
        assertTrue(summary.getGroups().isEmpty());
    }
}
