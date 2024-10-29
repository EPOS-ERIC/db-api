package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.EdmEntityId;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Person;
import org.epos.eposdatamodel.User;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EDMEntityIdTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testEDMEntity() {
        EdmEntityId edmEntityId = new EdmEntityId();
        edmEntityId.setMetaId("TESTMETAID");
        edmEntityId.setTableName("PERSON");

        EposDataModelDAO dataModelDAO = new EposDataModelDAO();

        dataModelDAO.updateObject(edmEntityId);
    }

    @Test
    @Order(1)
    public void testEDMEntityPerson() {

        LinkedEntity address = new LinkedEntity();
        address.setUid("_:635bdd482ca2ce50a79b1caf3a5af5fe");
        address.setEntityType("ADDRESS");

        LinkedEntity affiliation = new LinkedEntity();
        affiliation.setUid("PIC:000518944");
        affiliation.setEntityType("ORGANIZATION");

        LinkedEntity contactPoint = new LinkedEntity();
        contactPoint.setUid("http://orcid.org/0000-0002-6250-0000/contactPoint");
        contactPoint.setEntityType("CONTACTPOINT");

        LinkedEntity identifier = new LinkedEntity();
        identifier.setUid("http://orcid.org/0000-0002-6250-0000");
        identifier.setEntityType("PERSON");

        Person person = new Person();
        person.setUid("TESTUID");
        person.setEmail(List.of("test2@email.it"));
        person.setFamilyName("Surname2");
        person.setGivenName("Name2");
        person.setQualifications(List.of("IT"));
        person.setTelephone(List.of("+39001584663"));
        person.setAddress(address);
        person.setAffiliation(List.of(affiliation));
        person.setContactPoint(List.of(contactPoint));
        person.setIdentifier(List.of(identifier));

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.PERSON.name());
        api.create(person, null,  null, null);
    }


}
