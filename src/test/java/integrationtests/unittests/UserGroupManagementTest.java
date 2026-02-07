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

public class UserGroupManagementTest extends TestcontainersLifecycle {

    static User user;
    static Group group;

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
    @Order(2)
    public void testUpdateUser() {

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
    public void testUpdateGroup() {

        group.setDescription("Test updated description");

        UserGroupManagementAPI.createGroup(group);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());

        assertNotNull(retrieveGroup);
        assertEquals(group.getId(), retrieveGroup.getId());
        assertEquals(group.getName(), retrieveGroup.getName());
        assertEquals(group.getDescription(), retrieveGroup.getDescription());
    }

    @Test
    @Order(5)
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
                () -> assertEquals(retrieveGroup.getUsers().get(0).get("userId"), retrieveUser.getAuthIdentifier()),
                () -> assertEquals(1, retrieveUser.getGroups().size()),
                () -> assertEquals(retrieveUser.getGroups().get(0).getGroupId(), retrieveGroup.getId()),
                () -> assertEquals(retrieveUser.getGroups().get(0).getRole(), RoleType.EDITOR)
        );
    }

    @Test
    @Order(6)
    public void testAddSameUserToGroup() {

        UserGroupManagementAPI.addUserToGroup(group.getId(),user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());
        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

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
    @Order(7)
    public void testUpdateUserRoleInGroup() {
        // Update user role from EDITOR to ADMIN
        UserGroupManagementAPI.updateUserInGroup(group.getId(), user.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.ACCEPTED);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());
        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        System.out.println("Updated group: " + retrieveGroup);
        System.out.println("Updated user: " + retrieveUser);

        assertAll(
                () -> assertNotNull(retrieveGroup),
                () -> assertEquals(1, retrieveGroup.getUsers().size()),
                () -> assertEquals("ADMIN", retrieveGroup.getUsers().get(0).get("role")),
                () -> assertEquals("ACCEPTED", retrieveGroup.getUsers().get(0).get("requestStatus")),
                () -> assertEquals(1, retrieveUser.getGroups().size()),
                () -> assertEquals(RoleType.ADMIN, retrieveUser.getGroups().get(0).getRole())
        );
    }

    @Test
    @Order(8)
    public void testUpdateUserViaAddUserToGroup() {
        // Test that addUserToGroup updates existing membership instead of silently returning
        UserGroupManagementAPI.addUserToGroup(group.getId(), user.getAuthIdentifier(), RoleType.REVIEWER, RequestStatusType.PENDING);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());
        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        System.out.println("Re-added to group: " + retrieveGroup);
        System.out.println("Re-added user: " + retrieveUser);

        assertAll(
                () -> assertNotNull(retrieveGroup),
                () -> assertEquals(1, retrieveGroup.getUsers().size()), // Still only 1 user, not duplicated
                () -> assertEquals("REVIEWER", retrieveGroup.getUsers().get(0).get("role")),
                () -> assertEquals("PENDING", retrieveGroup.getUsers().get(0).get("requestStatus")),
                () -> assertEquals(1, retrieveUser.getGroups().size()),
                () -> assertEquals(RoleType.REVIEWER, retrieveUser.getGroups().get(0).getRole())
        );
    }

    @Test
    @Order(9)
    public void testUpdateNonExistentUserInGroup() {
        // Test that updateUserInGroup returns false for non-existent user-group relationship
        Boolean result = UserGroupManagementAPI.updateUserInGroup(group.getId(), "non-existent-user", RoleType.VIEWER, RequestStatusType.NONE);

        assertFalse(result);
    }

    @Test
    @Order(10)
    public void testDeleteUser() {
        UserGroupManagementAPI.deleteUser(user.getAuthIdentifier());

        User retrieveUser = UserGroupManagementAPI.retrieveUser(user);

        assertNull(retrieveUser);
    }

    @Test
    @Order(11)
    public void testDeleteGroup() {

        UserGroupManagementAPI.deleteGroup(group.getId());

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroup(group);

        assertNull(retrieveGroup);
    }


    @Test
    @Order(12)
    public void testCreateGroupWithoutName() {
        Group group = new Group(UUID.randomUUID().toString(), null, "Test Decription");
        UserGroupManagementAPI.createGroup(group);

        Group retrieveGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());

        assertNotNull(retrieveGroup);
        assertEquals(group.getId(), retrieveGroup.getId());
        assertEquals(group.getName(), retrieveGroup.getName());
        assertEquals(group.getDescription(), retrieveGroup.getDescription());
    }

    @Test
    @Order(13)
    public void testAddEntityToGroup() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());

        identifier = new Identifier();
        identifier.setInstanceId(UUID.randomUUID().toString());
        identifier.setMetaId(UUID.randomUUID().toString());
        identifier.setUid(UUID.randomUUID().toString());
        identifier.setType("TYPE");
        identifier.setIdentifier("012345678900");

        identifierLe = api.create(identifier, null, null, null);

        Identifier retrievedIdentifier = (Identifier) api.retrieve(identifierLe.getInstanceId());

        Group metadataGroup = new Group();
        metadataGroup.setId("test");
        metadataGroup.setDescription("test");
        metadataGroup.setName("test");
        UserGroupManagementAPI.createGroup(metadataGroup);

        Boolean response = UserGroupManagementAPI.addMetadataElementToGroup(identifierLe.getMetaId(), metadataGroup.getId());

        Group returnGroup = UserGroupManagementAPI.retrieveGroupById(metadataGroup.getId());
        System.out.println(returnGroup);

        assertAll(
                () -> assertEquals(identifier.getType(), retrievedIdentifier.getType()),
                () -> assertEquals(identifier.getIdentifier(), retrievedIdentifier.getIdentifier()),
                () -> assertEquals(identifier.getUid(), retrievedIdentifier.getUid()),
                () -> assertEquals(identifier.getInstanceId(), retrievedIdentifier.getInstanceId()),
                () -> assertEquals(identifier.getMetaId(), retrievedIdentifier.getMetaId()),
                () -> assertTrue(response),
                () -> assertEquals(returnGroup.getEntities().size(), 1)
        );
    }

    @Test
    @Order(14)
    public void testAddSameEntityToGroup() {

        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());

        Identifier retrievedIdentifier = (Identifier) api.retrieve(identifierLe.getInstanceId());

        Group metadataGroup = new Group();
        metadataGroup.setId("test");
        metadataGroup.setDescription("test");
        metadataGroup.setName("test");
        UserGroupManagementAPI.createGroup(metadataGroup);

        Boolean response = UserGroupManagementAPI.addMetadataElementToGroup(identifierLe.getMetaId(), metadataGroup.getId());

        System.out.println(UserGroupManagementAPI.retrieveGroupsFromMetaId(identifierLe.getMetaId()));

        Group returnGroup = UserGroupManagementAPI.retrieveGroupById(metadataGroup.getId());
        System.out.println(returnGroup);

        assertAll(
                () -> assertEquals(identifier.getType(), retrievedIdentifier.getType()),
                () -> assertEquals(identifier.getIdentifier(), retrievedIdentifier.getIdentifier()),
                () -> assertEquals(identifier.getUid(), retrievedIdentifier.getUid()),
                () -> assertEquals(identifier.getInstanceId(), retrievedIdentifier.getInstanceId()),
                () -> assertEquals(identifier.getMetaId(), retrievedIdentifier.getMetaId()),
                () -> assertTrue(response),
                () -> assertEquals(returnGroup.getEntities().size(), 1)
        );
    }
}
