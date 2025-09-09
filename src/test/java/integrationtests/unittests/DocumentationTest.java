package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.EdmEntityId;
import org.epos.eposdatamodel.Documentation;
import org.epos.eposdatamodel.Element;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Person;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

public class DocumentationTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testDocumentationEntity() {

        Documentation documentation = new Documentation();
        documentation.setDescription("Description");
        documentation.setTitle("Title");
        documentation.setUid("Documentation");

        LinkedEntity le = AbstractAPI.retrieveAPI(EntityNames.DOCUMENTATION.name()).create(documentation,null,null,null);


        System.out.println(EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(le, Element   .class));
    }

}
