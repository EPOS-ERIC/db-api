package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import metadataapis.SoftwareSourceCodeAPI;
import org.epos.eposdatamodel.SoftwareSourceCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoftwareSourceCodeSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingTheFullGraph() {
        SoftwareSourceCodeAPI api = (SoftwareSourceCodeAPI) AbstractAPI.retrieveAPI(EntityNames.SOFTWARESOURCECODE.name());
        SoftwareSourceCode sourceCode = new SoftwareSourceCode();
        sourceCode.setUid("summary/software-source-code/" + UUID.randomUUID());
        sourceCode.setName("Summary source code");
        sourceCode.setDescription("Description retained because it is a scalar field");
        sourceCode.addKeywords("one,two");
        api.create(sourceCode, null, null, null);

        List<SoftwareSourceCode> summaries = api.retrieveAllSummary();
        SoftwareSourceCode summary = summaries.stream()
                .filter(candidate -> sourceCode.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(sourceCode.getUid(), summary.getUid());
        assertEquals("Summary source code", summary.getName());
        assertEquals("Description retained because it is a scalar field", summary.getDescription());
        assertEquals("one,two", summary.getKeywords());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getCategory() == null || summary.getCategory().isEmpty());
    }
}
