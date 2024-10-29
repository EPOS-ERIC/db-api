package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntityManagementConceptsTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateAndGet() {

        List<EPOSDataModelEntity> classes = new ArrayList<>();

        LinkedEntity inScheme = new LinkedEntity();
        inScheme.setUid("category:anthropogenichazards");
        inScheme.setEntityType("CATEGORYSCHEME");

        Category category1 = new Category();
        category1.setDescription("TCS Subdomain");
        category1.setName("tools");
        category1.setUid("category:processgols");
        category1.setInScheme(inScheme);
        category1.setStatus(StatusType.PUBLISHED);
        classes.add(category1);

        CategoryScheme categoryScheme = new CategoryScheme();
        categoryScheme.setDescription("TCS Domain");
        categoryScheme.setCode("ANT");
        categoryScheme.setLogo("assets/img/logo/ANTHROPO_logo.png");
        categoryScheme.setHomepage("https://www.epos-eu.org/tcs/anthropogenic-hazards");
        categoryScheme.setOrderitemnumber("7");
        categoryScheme.setUid("category:anthropogenichazards");
        categoryScheme.setStatus(StatusType.PUBLISHED);
        classes.add(categoryScheme);

        LinkedEntity inScheme2 = new LinkedEntity();
        inScheme2.setUid("category:anthropogenichazards");
        inScheme2.setEntityType("CATEGORYSCHEME");

        LinkedEntity broader1 = new LinkedEntity();
        broader1.setUid("category:multidisciplaryahdata");
        broader1.setEntityType("CATEGORY");

        Category category2 = new Category();
        category2.setDescription("TCS Subdomain");
        category2.setName("Conventional hydrocarbon");
        category2.setUid("category:cventialhydrocarbextracti");
        category2.setInScheme(inScheme2);
        category2.addBroader(broader1);
        category2.setStatus(StatusType.PUBLISHED);
        classes.add(category2);

        LinkedEntity inScheme3 = new LinkedEntity();
        inScheme3.setUid("category:anthropogenichazards");
        inScheme3.setEntityType("CATEGORYSCHEME");

        LinkedEntity broader2 = new LinkedEntity();
        broader2.setUid("category:multidisciplaryahdata");
        broader2.setEntityType("CATEGORY");

        Category category3 = new Category();
        category3.setDescription("TCS Subdomain");
        category3.setName("Underground gas storage");
        category3.setUid("category:undergroundgasstage");
        category3.setInScheme(inScheme3);
        category3.addBroader(broader2);
        category3.setStatus(StatusType.PUBLISHED);
        classes.add(category3);

        LinkedEntity inScheme4 = new LinkedEntity();
        inScheme4.setUid("category:anthropogenichazards");
        inScheme4.setEntityType("CATEGORYSCHEME");

        LinkedEntity broader3 = new LinkedEntity();
        broader3.setUid("category:multidisciplaryahdata");
        broader3.setEntityType("CATEGORY");

        Category category4 = new Category();
        category4.setDescription("TCS Subdomain");
        category4.setName("Underground mining");
        category4.setUid("category:undergroundmg");
        category4.setInScheme(inScheme4);
        category4.addBroader(broader3);
        category4.setStatus(StatusType.PUBLISHED);
        classes.add(category4);

        LinkedEntity inScheme5 = new LinkedEntity();
        inScheme5.setUid("category:anthropogenichazards");
        inScheme5.setEntityType("CATEGORYSCHEME");

        LinkedEntity narrower1 = new LinkedEntity();
        narrower1.setUid("category:wastewaterjecti");
        narrower1.setEntityType("CATEGORY");

        LinkedEntity narrower2 = new LinkedEntity();
        narrower2.setUid("category:undergroundmg");
        narrower2.setEntityType("CATEGORY");

        LinkedEntity narrower3 = new LinkedEntity();
        narrower3.setUid("category:uncventialhydrocarbextracti");
        narrower3.setEntityType("CATEGORY");

        LinkedEntity narrower4 = new LinkedEntity();
        narrower4.setUid("category:reservoirimpoundment");
        narrower4.setEntityType("CATEGORY");

        LinkedEntity narrower5 = new LinkedEntity();
        narrower5.setUid("category:geothermalenergyproducti");
        narrower5.setEntityType("CATEGORY");

        LinkedEntity narrower6 = new LinkedEntity();
        narrower6.setUid("category:undergroundgasstage");
        narrower6.setEntityType("CATEGORY");

        LinkedEntity narrower7 = new LinkedEntity();
        narrower7.setUid("category:cventialhydrocarbextracti");
        narrower7.setEntityType("CATEGORY");

        Category category5 = new Category();
        category5.setDescription("TCS Subdomain");
        category5.setName("Multidisciplinary AH");
        category5.setUid("category:multidisciplaryahdata");
        category5.setInScheme(inScheme5);
        category5.addNarrower(narrower1);
        category5.addNarrower(narrower2);
        category5.addNarrower(narrower3);
        category5.addNarrower(narrower4);
        category5.addNarrower(narrower5);
        category5.addNarrower(narrower6);
        category5.addNarrower(narrower7);
        category5.setStatus(StatusType.PUBLISHED);
        classes.add(category5);

        LinkedEntity inScheme6 = new LinkedEntity();
        inScheme6.setUid("category:anthropogenichazards");
        inScheme6.setEntityType("CATEGORYSCHEME");

        LinkedEntity broader4 = new LinkedEntity();
        broader4.setUid("category:multidisciplaryahdata");
        broader4.setEntityType("CATEGORY");

        Category category6 = new Category();
        category6.setDescription("TCS Subdomain");
        category6.setName("Unconventional hydrocarbon extraction");
        category6.setUid("category:uncventialhydrocarbextracti");
        category6.setInScheme(inScheme6);
        category6.addBroader(broader4);
        category6.setStatus(StatusType.PUBLISHED);
        classes.add(category6);

        LinkedEntity inScheme7 = new LinkedEntity();
        inScheme7.setUid("category:anthropogenichazards");
        inScheme7.setEntityType("CATEGORYSCHEME");

        LinkedEntity broader5 = new LinkedEntity();
        broader5.setUid("category:multidisciplaryahdata");
        broader5.setEntityType("CATEGORY");

        Category category7 = new Category();
        category7.setDescription("TCS Subdomain");
        category7.setName("Reservoir impoundment");
        category7.setUid("category:reservoirimpoundment");
        category7.setInScheme(inScheme7);
        category7.addBroader(broader5);
        category7.setStatus(StatusType.PUBLISHED);
        classes.add(category7);

        LinkedEntity inScheme8 = new LinkedEntity();
        inScheme8.setUid("category:anthropogenichazards");
        inScheme8.setEntityType("CATEGORYSCHEME");

        LinkedEntity broader6 = new LinkedEntity();
        broader6.setUid("category:multidisciplaryahdata");
        broader6.setEntityType("CATEGORY");

        Category category8 = new Category();
        category8.setDescription("TCS Subdomain");
        category8.setName("Geothermal energy production");
        category8.setUid("category:geothermalenergyproducti");
        category8.setInScheme(inScheme8);
        category8.addBroader(broader6);
        category8.setStatus(StatusType.PUBLISHED);
        classes.add(category8);

        LinkedEntity inScheme9 = new LinkedEntity();
        inScheme9.setUid("category:anthropogenichazards");
        inScheme9.setEntityType("CATEGORYSCHEME");

        LinkedEntity broader7 = new LinkedEntity();
        broader7.setUid("category:multidisciplaryahdata");
        broader7.setEntityType("CATEGORY");

        Category category9 = new Category();
        category9.setDescription("TCS Subdomain");
        category9.setName("Wastewater injection");
        category9.setUid("category:wastewaterjecti");
        category9.setInScheme(inScheme9);
        category9.addBroader(broader7);
        category9.setStatus(StatusType.PUBLISHED);
        classes.add(category9);

        for(EPOSDataModelEntity entity : classes){
            AbstractAPI api = AbstractAPI.retrieveAPI(entity.getClass().getSimpleName().toUpperCase());
            api.create(entity, null, null, null);
        }


        System.out.println(classes.size());


        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        List<Category> categories = api.retrieveAll();

        AbstractAPI api2 = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
        List<CategoryScheme> categorySchemes = api2.retrieveAll();

        for(Category cat : categories){
            System.out.println(cat);
        }

        System.out.println(categories.size());

        assertAll(
                () -> assertEquals(1,categorySchemes.size()),
                () -> assertEquals(9,categories.size())
        );


    }


}
