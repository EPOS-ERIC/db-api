package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.CategoryAPI;
import metadataapis.CategorySchemeAPI;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategorySchemeSummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        CategoryAPI categoryApi = (CategoryAPI) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        Category category = new Category();
        category.setUid("summary/top-concept/" + UUID.randomUUID());
        category.setName("Top concept");
        categoryApi.create(category, null, null, null);

        CategorySchemeAPI api = (CategorySchemeAPI) AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        CategoryScheme scheme = new CategoryScheme();
        scheme.setUid("summary/category-scheme/" + UUID.randomUUID());
        scheme.setTitle("Summary scheme");
        scheme.setCode("SUMMARY");
        scheme.addTopConcepts(new LinkedEntity().instanceId(category.getInstanceId()).uid(category.getUid())
                .entityType(EntityNames.CATEGORY.name()));
        api.create(scheme, null, null, null);

        CategoryScheme summary = api.retrieveAllSummary().stream()
                .filter(candidate -> scheme.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(scheme.getUid(), summary.getUid());
        assertEquals("Summary scheme", summary.getTitle());
        assertEquals("SUMMARY", summary.getCode());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertTrue(summary.getTopConcepts() == null || summary.getTopConcepts().isEmpty());
        assertTrue(summary.getGroups().isEmpty());
    }
}
