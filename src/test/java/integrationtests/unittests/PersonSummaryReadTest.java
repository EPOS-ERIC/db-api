package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.PersonAPI;
import org.epos.eposdatamodel.Person;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersonSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingTheFullGraph() {
        PersonAPI api = (PersonAPI) AbstractAPI.retrieveAPI(EntityNames.PERSON.name());
        Person person = new Person();
        person.setUid("summary/person/" + UUID.randomUUID());
        person.setGivenName("Summary");
        person.setFamilyName("Person");
        person.setCVURL("https://example.org/summary-person");
        person.setQualifications(List.of("Engineer", "Researcher"));
        api.create(person, null, null, null);

        List<Person> summaries = api.retrieveAllSummary();
        Person summary = summaries.stream()
                .filter(candidate -> person.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(person.getUid(), summary.getUid());
        assertEquals("Summary", summary.getGivenName());
        assertEquals("Person", summary.getFamilyName());
        assertEquals("https://example.org/summary-person", summary.getCVURL());
        assertEquals(List.of("Engineer", "Researcher"), summary.getQualifications());
        assertNotNull(summary.getStatus());
        assertTrue(summary.getIdentifier() == null || summary.getIdentifier().isEmpty());
    }
}
