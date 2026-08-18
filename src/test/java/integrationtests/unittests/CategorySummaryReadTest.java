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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategorySummaryReadTest extends TestcontainersLifecycle {

    @Test
    void summaryKeepsScalarFieldsWithoutLoadingRelationsOrGroups() {
        CategorySchemeAPI schemeApi = (CategorySchemeAPI) AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        CategoryScheme scheme = new CategoryScheme();
        scheme.setUid("summary/category-scheme/" + UUID.randomUUID());
        scheme.setTitle("Summary scheme");
        schemeApi.create(scheme, null, null, null);

        CategoryAPI api = (CategoryAPI) AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        Category category = new Category();
        category.setUid("summary/category/" + UUID.randomUUID());
        category.setName("Summary category");
        category.setDescription("Scalar description");
        category.setInScheme(new LinkedEntity().instanceId(scheme.getInstanceId()).uid(scheme.getUid())
                .entityType(EntityNames.CATEGORYSCHEME.name()));
        api.create(category, null, null, null);

        Category summary = api.retrieveAllSummary().stream()
                .filter(candidate -> category.getInstanceId().equals(candidate.getInstanceId()))
                .findFirst()
                .orElseThrow();

        assertEquals(category.getUid(), summary.getUid());
        assertEquals("Summary category", summary.getName());
        assertEquals("Scalar description", summary.getDescription());
        assertNotNull(summary.getStatus());
        assertNotNull(summary.getVersionId());
        assertNull(summary.getInScheme());
        assertTrue(summary.getGroups().isEmpty());
    }
}
