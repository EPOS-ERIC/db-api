package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.Category;
import org.epos.eposdatamodel.CategoryScheme;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EntityManagementSimulateFacets extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGetCategories() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.ADDRESS.name());

        CategoryScheme categoryScheme = new CategoryScheme();
        categoryScheme.setUid("test/categoryscheme");
        categoryScheme.setDescription("Test Category Scheme");
        categoryScheme.setTitle("Test Category Scheme");

        LinkedEntity linkedEntity = new LinkedEntity();
        linkedEntity.setEntityType("CATEGORYSCHEME");
        linkedEntity.setUid("test/categoryscheme");

        Category category1 = new Category();
        category1.setUid("test/category1");
        category1.setDescription("Test Category 1");
        category1.setName("Test Category 1");
        category1.setInScheme(linkedEntity);

        AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(categoryScheme, StatusType.PUBLISHED,null,null);

        AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name()).create(category1, StatusType.PUBLISHED,null,null);

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveAll()){
            System.out.println(object);
        }



    }

}
