package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.RequestStatusType;
import model.RoleType;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.Identifier;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.User;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import usermanagementapis.UserGroupManagementAPI;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class UserGroupMultipleManagementTest extends TestcontainersLifecycle {

    static User user1;
    static User user2;
    static Group group;

    static Identifier identifier;
    static LinkedEntity identifierLe;

    @Test
    @Order(1)
    public void testCreateUser1() {
        user1 = new User("testid1", "familyname", "givenname", "email@email.email", false);
        UserGroupManagementAPI.createUser(user1);

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user1);

        assertNotNull(retrieveUser);
        assertEquals(user1.getAuthIdentifier(), retrieveUser.getAuthIdentifier());
        assertEquals(user1.getLastName(), retrieveUser.getLastName());
        assertEquals(user1.getFirstName(), retrieveUser.getFirstName());
        assertEquals(user1.getEmail(), retrieveUser.getEmail());
    }

    @Test
    @Order(2)
    public void testCreateUser2() {
        user2 = new User("testid2", "familyname", "givenname", "email@email.email", false);
        UserGroupManagementAPI.createUser(user2);

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user2);

        assertNotNull(retrieveUser);
        assertEquals(user2.getAuthIdentifier(), retrieveUser.getAuthIdentifier());
        assertEquals(user2.getLastName(), retrieveUser.getLastName());
        assertEquals(user2.getFirstName(), retrieveUser.getFirstName());
        assertEquals(user2.getEmail(), retrieveUser.getEmail());
    }

    @Test
    @Order(3)
    public void testCreateGroup() {
        group = new Group(UUID.randomUUID().toString(), "Test Group", "Test Decription");
        UserGroupManagementAPI.createGroup(group);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());

        assertNotNull(retrieveGroup);
        assertEquals(group.getId(), retrieveGroup.getId());
        assertEquals(group.getName(), retrieveGroup.getName());
        assertEquals(group.getDescription(), retrieveGroup.getDescription());
    }

    @Test
    @Order(4)
    public void testAddUser1ToGroup() {

        UserGroupManagementAPI.addUserToGroup(group.getId(),user1.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user1);

        System.out.println(retrieveGroup);
        System.out.println(retrieveUser);


        assertAll(
                () -> assertNotNull(retrieveGroup),
                () -> assertEquals(1, retrieveGroup.getUsers().size()),
                () -> assertEquals(retrieveGroup.getUsers().get(0).get("userId"), retrieveUser.getAuthIdentifier()),
                () -> assertEquals(1, retrieveUser.getGroups().size()),
                () -> assertEquals(retrieveUser.getGroups().get(0).getGroupId(), retrieveGroup.getId()),
                () -> assertEquals(retrieveUser.getGroups().get(0).getRole(), RoleType.EDITOR)
        );
    }

    @Test
    @Order(5)
    public void testAddUser2ToGroup() {

        UserGroupManagementAPI.addUserToGroup(group.getId(),user2.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user2);


        System.out.println(retrieveGroup);
        System.out.println(retrieveUser);
        assertAll(
                () -> assertNotNull(retrieveGroup),
                () -> assertEquals(2, retrieveGroup.getUsers().size()),
                () -> assertEquals(1, retrieveUser.getGroups().size())
        );
    }
}
