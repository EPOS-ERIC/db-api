package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import org.epos.eposdatamodel.Address;
import org.epos.eposdatamodel.User;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserManagementTest extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testCreateUser() {
        User user = new User("testid", "familyname", "givenname", "email@email.email", false);
        UserGroupManagementAPI.createUser(user);

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        assertNotNull(retrieveUser);
        assertEquals(user.getAuthIdentifier(), retrieveUser.getAuthIdentifier());
        assertEquals(user.getLastName(), retrieveUser.getLastName());
        assertEquals(user.getFirstName(), retrieveUser.getFirstName());
        assertEquals(user.getEmail(), retrieveUser.getEmail());
    }

    @Test
    @Order(2)
    public void testUpdateUser() {
        User user = new User("testid", "familyname", "givenname", "email@email.email", false);
        UserGroupManagementAPI.createUser(user);

        user.setEmail("newemail@email.email");
        user.setLastName("newfamilyname");
        user.setFirstName("newgivenname");

        UserGroupManagementAPI.createUser(user);

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        assertNotNull(retrieveUser);
        assertEquals(user.getAuthIdentifier(), retrieveUser.getAuthIdentifier());
        assertEquals(user.getLastName(), retrieveUser.getLastName());
        assertEquals(user.getFirstName(), retrieveUser.getFirstName());
        assertEquals(user.getEmail(), retrieveUser.getEmail());
    }

    @Test
    @Order(3)
    public void testDeleteUser() {
        User user = new User("testid", "familyname", "givenname", "email@email.email", false);
        UserGroupManagementAPI.createUser(user);

        UserGroupManagementAPI.deleteUser(user.getAuthIdentifier());

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        assertNull(retrieveUser);
    }

}
