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

public class UserGroupDoubleGroupManagementTest extends TestcontainersLifecycle {

    static User user;
    static Group group;
    static Group secondGroup;

    static Identifier identifier;
    static LinkedEntity identifierLe;

    @Test
    @Order(1)
    public void testCreateUser() {
        user = new User("testid", "familyname", "givenname", "email@email.email", false);
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
    public void testCreateFirstGroup() {
        group = new Group(UUID.randomUUID().toString(), "Test Group", "Test Decription");
        UserGroupManagementAPI.createGroup(group);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());

        assertNotNull(retrieveGroup);
        assertEquals(group.getId(), retrieveGroup.getId());
        assertEquals(group.getName(), retrieveGroup.getName());
        assertEquals(group.getDescription(), retrieveGroup.getDescription());
    }

    @Test
    @Order(3)
    public void testCreateSecondGroup() {
        secondGroup = new Group(UUID.randomUUID().toString(), "Test Group Two", "Test Decription Two");
        UserGroupManagementAPI.createGroup(secondGroup);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(secondGroup.getId());

        assertNotNull(retrieveGroup);
        assertEquals(secondGroup.getId(), retrieveGroup.getId());
        assertEquals(secondGroup.getName(), retrieveGroup.getName());
        assertEquals(secondGroup.getDescription(), retrieveGroup.getDescription());
    }

    @Test
    @Order(4)
    public void testAddUserToGroup() {

        UserGroupManagementAPI.addUserToGroup(group.getId(),user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());

        System.out.println("MEGATEST: "+retrieveGroup.getId()+" "+group.getId());
        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        System.out.println("MEGATEST: "+retrieveUser.getAuthIdentifier()+" "+user.getAuthIdentifier());

        System.out.println(retrieveGroup);
        System.out.println(retrieveUser);

        System.out.println("ANOTHER TEST: "+retrieveGroup.getUsers().get(0)+" "+retrieveUser.getAuthIdentifier());
        System.out.println("ANOTHER TEST: "+retrieveUser.getGroups().get(0).getGroupId()+" "+retrieveGroup.getId());
        System.out.println("ANOTHER TEST: "+retrieveUser.getGroups().size());

        assertAll(
                () -> assertNotNull(retrieveGroup),
                () -> assertEquals(1, retrieveGroup.getUsers().size()),
                () -> assertEquals(retrieveGroup.getUsers().get(0), retrieveUser.getAuthIdentifier()),
                () -> assertEquals(1, retrieveUser.getGroups().size()),
                () -> assertEquals(retrieveUser.getGroups().get(0).getGroupId(), retrieveGroup.getId()),
                () -> assertEquals(retrieveUser.getGroups().get(0).getRole(), RoleType.EDITOR)
        );
    }

    @Test
    @Order(4)
    public void testAddUserToSecondGroup() {

        UserGroupManagementAPI.addUserToGroup(secondGroup.getId(),user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(secondGroup.getId());

        System.out.println("MEGATEST: "+retrieveGroup.getId()+" "+secondGroup.getId());
        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        System.out.println("MEGATEST: "+retrieveUser.getAuthIdentifier()+" "+user.getAuthIdentifier());

        System.out.println(retrieveGroup);
        System.out.println(retrieveUser);

        System.out.println("ANOTHER TEST: "+retrieveGroup.getUsers().get(0)+" "+retrieveUser.getAuthIdentifier());
        System.out.println("ANOTHER TEST: "+retrieveUser.getGroups().get(0).getGroupId()+" "+retrieveGroup.getId());
        System.out.println("ANOTHER TEST: "+retrieveUser.getGroups().size());

        assertAll(
                () -> assertNotNull(retrieveGroup),
                () -> assertEquals(1, retrieveGroup.getUsers().size()),
                () -> assertEquals(retrieveGroup.getUsers().get(0), retrieveUser.getAuthIdentifier()),
                () -> assertEquals(2, retrieveUser.getGroups().size())
        );
    }

}
