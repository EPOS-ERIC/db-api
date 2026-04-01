package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.RequestStatusType;
import model.RoleType;
import model.StatusType;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.Identifier;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.User;
import org.junit.jupiter.api.*;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite specifically for verifying the cache invalidation fix in removeUserFromGroup.
 * 
 * This test reproduces the exact scenario that was causing issues:
 * 1. Add a user to a group
 * 2. Immediately try to remove the user from the group
 * 
 * The issue was that getFromDBByUsingMultipleKeys() was returning cached (empty) results,
 * causing the removal to fail with "user not found in group" even though the user was
 * just added.
 * 
 * The fix invalidates both application cache and L2 EclipseLink cache BEFORE querying
 * to ensure fresh data is retrieved.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RemoveUserFromGroupCacheTest extends TestcontainersLifecycle {

    private static User user;
    private static Group group;

    @BeforeAll
    static void setupTestData() {
        // Create a user for testing
        user = new User("cache-test-user-" + UUID.randomUUID().toString(), "CacheTest", "User", "cachetest@example.com", false);
        Boolean userCreated = UserGroupManagementAPI.createUser(user);
        assertTrue(userCreated, "User should be created successfully");

        // Create a group for testing
        group = new Group(UUID.randomUUID().toString(), "Cache Test Group", "Group for testing cache invalidation");
        Boolean groupCreated = UserGroupManagementAPI.createGroup(group);
        assertTrue(groupCreated, "Group should be created successfully");
    }

    @AfterAll
    static void cleanupTestData() {
        // Cleanup
        if (user != null) {
            UserGroupManagementAPI.deleteUser(user.getAuthIdentifier());
        }
        if (group != null) {
            UserGroupManagementAPI.deleteGroup(group.getId());
        }
    }

    @Test
    @Order(1)
    @DisplayName("Add user and immediately remove - should succeed (cache fix verification)")
    void testAddAndImmediatelyRemoveUser() {
        // This is the exact scenario that was failing before the fix
        
        // Step 1: Add user to group
        Boolean addResult = UserGroupManagementAPI.addUserToGroup(
                group.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED);
        assertTrue(addResult, "Adding user to group should succeed");

        // Verify user is in the group
        Group retrievedGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());
        assertNotNull(retrievedGroup, "Group should be retrievable");
        assertEquals(1, retrievedGroup.getUsers().size(), "Group should have 1 user after add");
        assertEquals(user.getAuthIdentifier(), retrievedGroup.getUsers().get(0).get("userId"), 
                "User in group should match the added user");

        // Step 2: Immediately remove user from group (this was failing due to cache)
        Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier());
        assertTrue(removeResult, "Removing user from group should succeed immediately after add");

        // Verify user is no longer in the group
        Group retrievedGroupAfter = UserGroupManagementAPI.retrieveGroupById(group.getId());
        assertNotNull(retrievedGroupAfter, "Group should still be retrievable");
        assertEquals(0, retrievedGroupAfter.getUsers().size(), "Group should have 0 users after removal");
    }

    @Test
    @Order(2)
    @DisplayName("Rapid add-remove-add-remove cycle should all succeed")
    void testRapidAddRemoveCycle() {
        // Test multiple rapid cycles of add/remove to stress-test cache invalidation
        
        for (int i = 0; i < 5; i++) {
            // Add user
            Boolean addResult = UserGroupManagementAPI.addUserToGroup(
                    group.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED);
            assertTrue(addResult, "Add #" + (i + 1) + " should succeed");

            // Verify add
            Group groupAfterAdd = UserGroupManagementAPI.retrieveGroupById(group.getId());
            assertEquals(1, groupAfterAdd.getUsers().size(), "Group should have 1 user after add #" + (i + 1));

            // Remove user
            Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier());
            assertTrue(removeResult, "Remove #" + (i + 1) + " should succeed");

            // Verify remove
            Group groupAfterRemove = UserGroupManagementAPI.retrieveGroupById(group.getId());
            assertEquals(0, groupAfterRemove.getUsers().size(), "Group should have 0 users after remove #" + (i + 1));
        }
    }

    @Test
    @Order(3)
    @DisplayName("Add with different roles and remove should all succeed")
    void testAddWithDifferentRolesAndRemove() {
        RoleType[] roles = {RoleType.VIEWER, RoleType.EDITOR, RoleType.REVIEWER, RoleType.ADMIN};
        RequestStatusType[] statuses = {RequestStatusType.PENDING, RequestStatusType.ACCEPTED, RequestStatusType.REJECTED};

        for (RoleType role : roles) {
            for (RequestStatusType status : statuses) {
                // Add with specific role and status
                Boolean addResult = UserGroupManagementAPI.addUserToGroup(
                        group.getId(), user.getAuthIdentifier(), role, status);
                assertTrue(addResult, "Add with role=" + role + " status=" + status + " should succeed");

                // Verify add
                Group groupAfterAdd = UserGroupManagementAPI.retrieveGroupById(group.getId());
                assertEquals(1, groupAfterAdd.getUsers().size());
                assertEquals(role.name(), groupAfterAdd.getUsers().get(0).get("role"));
                assertEquals(status.name(), groupAfterAdd.getUsers().get(0).get("requestStatus"));

                // Remove
                Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier());
                assertTrue(removeResult, "Remove after role=" + role + " status=" + status + " should succeed");

                // Verify remove
                Group groupAfterRemove = UserGroupManagementAPI.retrieveGroupById(group.getId());
                assertEquals(0, groupAfterRemove.getUsers().size());
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("Update user role and then remove should succeed")
    void testUpdateThenRemove() {
        // Add user as VIEWER
        Boolean addResult = UserGroupManagementAPI.addUserToGroup(
                group.getId(), user.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.PENDING);
        assertTrue(addResult, "Initial add should succeed");

        // Update to EDITOR
        Boolean updateResult1 = UserGroupManagementAPI.updateUserInGroup(
                group.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED);
        assertTrue(updateResult1, "Update to EDITOR should succeed");

        // Update to ADMIN
        Boolean updateResult2 = UserGroupManagementAPI.updateUserInGroup(
                group.getId(), user.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.ACCEPTED);
        assertTrue(updateResult2, "Update to ADMIN should succeed");

        // Verify the updates
        Group groupAfterUpdates = UserGroupManagementAPI.retrieveGroupById(group.getId());
        assertEquals("ADMIN", groupAfterUpdates.getUsers().get(0).get("role"));
        assertEquals("ACCEPTED", groupAfterUpdates.getUsers().get(0).get("requestStatus"));

        // Now remove - this should work
        Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier());
        assertTrue(removeResult, "Remove after multiple updates should succeed");

        // Verify removal
        Group groupAfterRemove = UserGroupManagementAPI.retrieveGroupById(group.getId());
        assertEquals(0, groupAfterRemove.getUsers().size(), "Group should have 0 users after removal");
    }

    @Test
    @Order(5)
    @DisplayName("Multiple users - add all then remove one by one")
    void testMultipleUsersAddThenRemove() {
        // Create additional users
        User user2 = new User("cache-test-user2-" + UUID.randomUUID().toString(), "CacheTest2", "User", "cachetest2@example.com", false);
        User user3 = new User("cache-test-user3-" + UUID.randomUUID().toString(), "CacheTest3", "User", "cachetest3@example.com", false);
        
        UserGroupManagementAPI.createUser(user2);
        UserGroupManagementAPI.createUser(user3);

        try {
            // Add all users to the group
            assertTrue(UserGroupManagementAPI.addUserToGroup(group.getId(), user.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.ACCEPTED));
            assertTrue(UserGroupManagementAPI.addUserToGroup(group.getId(), user2.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));
            assertTrue(UserGroupManagementAPI.addUserToGroup(group.getId(), user3.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.PENDING));

            // Verify all 3 users are in the group
            Group groupWith3Users = UserGroupManagementAPI.retrieveGroupById(group.getId());
            assertEquals(3, groupWith3Users.getUsers().size(), "Group should have 3 users");

            // Remove user2 (middle one)
            Boolean remove2 = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user2.getAuthIdentifier());
            assertTrue(remove2, "Remove user2 should succeed");

            Group groupWith2Users = UserGroupManagementAPI.retrieveGroupById(group.getId());
            assertEquals(2, groupWith2Users.getUsers().size(), "Group should have 2 users after removing user2");

            // Remove user3
            Boolean remove3 = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user3.getAuthIdentifier());
            assertTrue(remove3, "Remove user3 should succeed");

            Group groupWith1User = UserGroupManagementAPI.retrieveGroupById(group.getId());
            assertEquals(1, groupWith1User.getUsers().size(), "Group should have 1 user after removing user3");

            // Remove original user
            Boolean remove1 = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier());
            assertTrue(remove1, "Remove original user should succeed");

            Group emptyGroup = UserGroupManagementAPI.retrieveGroupById(group.getId());
            assertEquals(0, emptyGroup.getUsers().size(), "Group should have 0 users after removing all");

        } finally {
            // Cleanup additional users
            UserGroupManagementAPI.deleteUser(user2.getAuthIdentifier());
            UserGroupManagementAPI.deleteUser(user3.getAuthIdentifier());
        }
    }

    @Test
    @Order(6)
    @DisplayName("Remove user from group with null parameters returns false")
    void testRemoveUserNullParameters() {
        assertFalse(UserGroupManagementAPI.removeUserFromGroup(null, user.getAuthIdentifier()),
                "Remove with null groupId should return false");
        assertFalse(UserGroupManagementAPI.removeUserFromGroup(group.getId(), null),
                "Remove with null userId should return false");
        assertFalse(UserGroupManagementAPI.removeUserFromGroup(null, null),
                "Remove with both null should return false");
    }

    @Test
    @Order(7)
    @DisplayName("Remove non-existent user from group returns false")
    void testRemoveNonExistentUser() {
        Boolean result = UserGroupManagementAPI.removeUserFromGroup(group.getId(), "non-existent-user-id");
        assertFalse(result, "Removing non-existent user should return false");
    }

    @Test
    @Order(8)
    @DisplayName("Remove user from non-existent group returns false")
    void testRemoveUserFromNonExistentGroup() {
        Boolean result = UserGroupManagementAPI.removeUserFromGroup("non-existent-group-id", user.getAuthIdentifier());
        assertFalse(result, "Removing from non-existent group should return false");
    }

    @Test
    @Order(9)
    @DisplayName("Remove user twice - second removal should return false")
    void testRemoveUserTwice() {
        // Add user
        assertTrue(UserGroupManagementAPI.addUserToGroup(
                group.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));

        // First removal should succeed
        Boolean firstRemove = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier());
        assertTrue(firstRemove, "First removal should succeed");

        // Second removal should fail (user is no longer in group)
        Boolean secondRemove = UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier());
        assertFalse(secondRemove, "Second removal should return false (user already removed)");
    }

    @Test
    @Order(10)
    @DisplayName("Add user to multiple groups and remove from each")
    void testAddToMultipleGroupsAndRemove() {
        // Create additional groups
        Group group2 = new Group(UUID.randomUUID().toString(), "Cache Test Group 2", "Second test group");
        Group group3 = new Group(UUID.randomUUID().toString(), "Cache Test Group 3", "Third test group");
        
        UserGroupManagementAPI.createGroup(group2);
        UserGroupManagementAPI.createGroup(group3);

        try {
            // Add user to all groups
            assertTrue(UserGroupManagementAPI.addUserToGroup(group.getId(), user.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.ACCEPTED));
            assertTrue(UserGroupManagementAPI.addUserToGroup(group2.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));
            assertTrue(UserGroupManagementAPI.addUserToGroup(group3.getId(), user.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.PENDING));

            // Verify user is in all groups
            User retrievedUser = UserGroupManagementAPI.retrieveUserById(user.getAuthIdentifier());
            assertEquals(3, retrievedUser.getGroups().size(), "User should be in 3 groups");

            // Remove from group2
            assertTrue(UserGroupManagementAPI.removeUserFromGroup(group2.getId(), user.getAuthIdentifier()));
            
            User userAfterRemove1 = UserGroupManagementAPI.retrieveUserById(user.getAuthIdentifier());
            assertEquals(2, userAfterRemove1.getGroups().size(), "User should be in 2 groups after first removal");

            // Remove from group3
            assertTrue(UserGroupManagementAPI.removeUserFromGroup(group3.getId(), user.getAuthIdentifier()));
            
            User userAfterRemove2 = UserGroupManagementAPI.retrieveUserById(user.getAuthIdentifier());
            assertEquals(1, userAfterRemove2.getGroups().size(), "User should be in 1 group after second removal");

            // Remove from original group
            assertTrue(UserGroupManagementAPI.removeUserFromGroup(group.getId(), user.getAuthIdentifier()));
            
            User userAfterRemove3 = UserGroupManagementAPI.retrieveUserById(user.getAuthIdentifier());
            assertEquals(0, userAfterRemove3.getGroups().size(), "User should be in 0 groups after third removal");

        } finally {
            // Cleanup additional groups
            UserGroupManagementAPI.deleteGroup(group2.getId());
            UserGroupManagementAPI.deleteGroup(group3.getId());
        }
    }

    // =================== SECTION 2: REMOVE USER FROM GROUP WITH ENTITIES ===================
    // These tests verify the scenario where the user being removed is associated with entities in the group

    @Test
    @Order(11)
    @DisplayName("Remove user from group that has entities - should succeed")
    void testRemoveUserFromGroupWithEntities() {
        // Create a new group for this test
        Group groupWithEntities = new Group(UUID.randomUUID().toString(), "Group With Entities", "Test group with entities");
        UserGroupManagementAPI.createGroup(groupWithEntities);

        try {
            // Add user to the group as EDITOR
            assertTrue(UserGroupManagementAPI.addUserToGroup(
                    groupWithEntities.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));

            // Create an entity (Identifier) and add it to the group
            AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());
            Identifier identifier = new Identifier();
            identifier.setInstanceId(UUID.randomUUID().toString());
            identifier.setMetaId(UUID.randomUUID().toString());
            identifier.setUid(UUID.randomUUID().toString());
            identifier.setType("DOI");
            identifier.setIdentifier("10.1234/test-remove-user");

            LinkedEntity identifierLe = api.create(identifier, null, null, null);
            assertNotNull(identifierLe, "Identifier should be created");

            // Add the entity to the group
            assertTrue(UserGroupManagementAPI.addMetadataElementToGroup(identifierLe.getMetaId(), groupWithEntities.getId()));

            // Verify setup: user is in group AND group has entity
            Group groupBefore = UserGroupManagementAPI.retrieveGroupById(groupWithEntities.getId());
            assertEquals(1, groupBefore.getUsers().size(), "Group should have 1 user");
            assertEquals(1, groupBefore.getEntities().size(), "Group should have 1 entity");
            assertTrue(groupBefore.getEntities().contains(identifierLe.getMetaId()), "Group should contain the identifier");

            // Now remove the user - THIS IS THE KEY TEST
            Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(groupWithEntities.getId(), user.getAuthIdentifier());
            assertTrue(removeResult, "Should be able to remove user from group even when group has entities");

            // Verify the user is removed but entity remains
            Group groupAfter = UserGroupManagementAPI.retrieveGroupById(groupWithEntities.getId());
            assertEquals(0, groupAfter.getUsers().size(), "Group should have 0 users after removal");
            assertEquals(1, groupAfter.getEntities().size(), "Group should still have 1 entity after user removal");

        } finally {
            UserGroupManagementAPI.deleteGroup(groupWithEntities.getId());
        }
    }

    @Test
    @Order(12)
    @DisplayName("Remove EDITOR user from group with multiple entities - should succeed")
    void testRemoveEditorFromGroupWithMultipleEntities() {
        // Create a new group
        Group groupWithMultipleEntities = new Group(UUID.randomUUID().toString(), "Group With Multiple Entities", "Test");
        UserGroupManagementAPI.createGroup(groupWithMultipleEntities);

        try {
            // Add user as EDITOR with ACCEPTED status
            assertTrue(UserGroupManagementAPI.addUserToGroup(
                    groupWithMultipleEntities.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));

            // Create multiple entities and add them to the group
            AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());
            
            for (int i = 0; i < 3; i++) {
                Identifier identifier = new Identifier();
                identifier.setInstanceId(UUID.randomUUID().toString());
                identifier.setMetaId(UUID.randomUUID().toString());
                identifier.setUid(UUID.randomUUID().toString());
                identifier.setType("DOI");
                identifier.setIdentifier("10.1234/test-" + i);

                LinkedEntity le = api.create(identifier, null, null, null);
                assertTrue(UserGroupManagementAPI.addMetadataElementToGroup(le.getMetaId(), groupWithMultipleEntities.getId()));
            }

            // Verify setup
            Group groupBefore = UserGroupManagementAPI.retrieveGroupById(groupWithMultipleEntities.getId());
            assertEquals(1, groupBefore.getUsers().size(), "Group should have 1 user");
            assertEquals(3, groupBefore.getEntities().size(), "Group should have 3 entities");

            // Remove the EDITOR user
            Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(
                    groupWithMultipleEntities.getId(), user.getAuthIdentifier());
            assertTrue(removeResult, "Should be able to remove EDITOR user from group with multiple entities");

            // Verify
            Group groupAfter = UserGroupManagementAPI.retrieveGroupById(groupWithMultipleEntities.getId());
            assertEquals(0, groupAfter.getUsers().size(), "Group should have 0 users");
            assertEquals(3, groupAfter.getEntities().size(), "Group should still have 3 entities");

        } finally {
            UserGroupManagementAPI.deleteGroup(groupWithMultipleEntities.getId());
        }
    }

    @Test
    @Order(13)
    @DisplayName("Add user, add entity, then immediately remove user - cache stress test")
    void testAddUserAddEntityThenRemoveUser() {
        // This tests the cache invalidation when entities are involved
        Group testGroup = new Group(UUID.randomUUID().toString(), "Cache Stress Test Group", "Test");
        UserGroupManagementAPI.createGroup(testGroup);

        try {
            // Rapid sequence: add user -> add entity -> remove user
            assertTrue(UserGroupManagementAPI.addUserToGroup(
                    testGroup.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));

            AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());
            Identifier identifier = new Identifier();
            identifier.setInstanceId(UUID.randomUUID().toString());
            identifier.setMetaId(UUID.randomUUID().toString());
            identifier.setUid(UUID.randomUUID().toString());
            identifier.setType("DOI");
            identifier.setIdentifier("10.1234/cache-stress");

            LinkedEntity le = api.create(identifier, null, null, null);
            assertTrue(UserGroupManagementAPI.addMetadataElementToGroup(le.getMetaId(), testGroup.getId()));

            // Immediately remove user (no delay)
            Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(testGroup.getId(), user.getAuthIdentifier());
            assertTrue(removeResult, "Immediate removal after adding entity should succeed");

            // Verify
            Group groupAfter = UserGroupManagementAPI.retrieveGroupById(testGroup.getId());
            assertEquals(0, groupAfter.getUsers().size());
            assertEquals(1, groupAfter.getEntities().size());

        } finally {
            UserGroupManagementAPI.deleteGroup(testGroup.getId());
        }
    }

    @Test
    @Order(14)
    @DisplayName("Remove user from group with DataProduct entity - should succeed")
    void testRemoveUserFromGroupWithDataProduct() {
        // This specifically tests with DataProduct which was mentioned in the original issue
        Group groupWithDataProduct = new Group(UUID.randomUUID().toString(), "Group With DataProduct", "Test");
        UserGroupManagementAPI.createGroup(groupWithDataProduct);

        try {
            // Add user as EDITOR with ACCEPTED status
            assertTrue(UserGroupManagementAPI.addUserToGroup(
                    groupWithDataProduct.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));

            // Create a DataProduct and add it to the group
            AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
            DataProduct dataProduct = new DataProduct();
            dataProduct.setInstanceId(UUID.randomUUID().toString());
            dataProduct.setMetaId(UUID.randomUUID().toString());
            dataProduct.setUid(UUID.randomUUID().toString());
            dataProduct.setTitle(List.of("Test DataProduct for Remove User Test"));
            dataProduct.setDescription(List.of("Test Description"));

            LinkedEntity dataProductLe = api.create(dataProduct, StatusType.DRAFT, null, null);
            assertNotNull(dataProductLe, "DataProduct should be created");

            // Add the DataProduct to the group
            assertTrue(UserGroupManagementAPI.addMetadataElementToGroup(dataProductLe.getMetaId(), groupWithDataProduct.getId()));

            // Verify setup
            Group groupBefore = UserGroupManagementAPI.retrieveGroupById(groupWithDataProduct.getId());
            assertEquals(1, groupBefore.getUsers().size(), "Group should have 1 user");
            assertTrue(groupBefore.getEntities().size() >= 1, "Group should have at least 1 entity (DataProduct)");

            // Remove the user - THIS TESTS THE ORIGINAL ISSUE SCENARIO
            Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(
                    groupWithDataProduct.getId(), user.getAuthIdentifier());
            assertTrue(removeResult, "Should be able to remove EDITOR user from group with DataProduct");

            // Verify user is removed
            Group groupAfter = UserGroupManagementAPI.retrieveGroupById(groupWithDataProduct.getId());
            assertEquals(0, groupAfter.getUsers().size(), "Group should have 0 users after removal");

        } finally {
            UserGroupManagementAPI.deleteGroup(groupWithDataProduct.getId());
        }
    }

    @Test
    @Order(15)
    @DisplayName("Remove user from group with DRAFT DataProduct - exact issue scenario")
    void testRemoveEditorUserFromGroupWithDraftDataProduct() {
        // This reproduces the EXACT scenario from the original bug:
        // 1. User is EDITOR with ACCEPTED status in a group
        // 2. Group has a DRAFT DataProduct
        // 3. Try to remove the user from the group
        
        Group groupWithDraft = new Group(UUID.randomUUID().toString(), "Group With DRAFT DataProduct", "Test");
        UserGroupManagementAPI.createGroup(groupWithDraft);
        
        // Create a separate editor user (non-admin)
        User editorUser = new User("editor-" + UUID.randomUUID().toString(), "Editor", "User", "editor@test.com", false);
        UserGroupManagementAPI.createUser(editorUser);

        try {
            // Add editor user to the group as EDITOR with ACCEPTED status
            assertTrue(UserGroupManagementAPI.addUserToGroup(
                    groupWithDraft.getId(), editorUser.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED));

            // Create a DRAFT DataProduct
            AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
            DataProduct dataProduct = new DataProduct();
            dataProduct.setInstanceId(UUID.randomUUID().toString());
            dataProduct.setMetaId(UUID.randomUUID().toString());
            dataProduct.setUid(UUID.randomUUID().toString());
            dataProduct.setTitle(List.of("DRAFT DataProduct"));
            dataProduct.setDescription(List.of("This is a DRAFT"));

            // Create as DRAFT status
            LinkedEntity dataProductLe = api.create(dataProduct, StatusType.DRAFT, null, null);
            assertNotNull(dataProductLe, "DRAFT DataProduct should be created");

            // Add to the group
            assertTrue(UserGroupManagementAPI.addMetadataElementToGroup(dataProductLe.getMetaId(), groupWithDraft.getId()));

            // Verify the editor user and entity are in the same group
            Group groupBefore = UserGroupManagementAPI.retrieveGroupById(groupWithDraft.getId());
            System.out.println("Before removal - Users: " + groupBefore.getUsers());
            System.out.println("Before removal - Entities: " + groupBefore.getEntities());
            
            assertEquals(1, groupBefore.getUsers().size(), "Group should have 1 user (editor)");
            assertEquals(editorUser.getAuthIdentifier(), groupBefore.getUsers().get(0).get("userId"));
            assertEquals("EDITOR", groupBefore.getUsers().get(0).get("role"));
            assertTrue(groupBefore.getEntities().size() >= 1, "Group should have the DRAFT DataProduct");

            // Check that user and entity are in the same group
            Boolean notInSameGroup = UserGroupManagementAPI.checkIfMetaIdAndUserIdAreInSameGroup(
                    dataProductLe.getMetaId(), editorUser.getAuthIdentifier());
            assertFalse(notInSameGroup, "Editor and DataProduct SHOULD be in the same group (method returns false when they ARE)");

            // NOW THE KEY TEST: Remove the editor user from the group
            Boolean removeResult = UserGroupManagementAPI.removeUserFromGroup(
                    groupWithDraft.getId(), editorUser.getAuthIdentifier());
            assertTrue(removeResult, 
                    "Should be able to remove EDITOR user from group even when group has DRAFT DataProduct");

            // Verify
            Group groupAfter = UserGroupManagementAPI.retrieveGroupById(groupWithDraft.getId());
            System.out.println("After removal - Users: " + groupAfter.getUsers());
            System.out.println("After removal - Entities: " + groupAfter.getEntities());
            
            assertEquals(0, groupAfter.getUsers().size(), "Group should have 0 users after removal");
            assertTrue(groupAfter.getEntities().size() >= 1, "Group should still have the DataProduct");

        } finally {
            UserGroupManagementAPI.deleteUser(editorUser.getAuthIdentifier());
            UserGroupManagementAPI.deleteGroup(groupWithDraft.getId());
        }
    }

    @Test
    @Order(16)
    @DisplayName("Multiple add-entity-remove cycles with same user")
    void testMultipleAddEntityRemoveCycles() {
        Group cycleGroup = new Group(UUID.randomUUID().toString(), "Cycle Test Group", "Test");
        UserGroupManagementAPI.createGroup(cycleGroup);

        try {
            AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());

            for (int cycle = 0; cycle < 3; cycle++) {
                // Add user
                assertTrue(UserGroupManagementAPI.addUserToGroup(
                        cycleGroup.getId(), user.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED),
                        "Add user cycle " + cycle + " should succeed");

                // Add a new entity
                Identifier identifier = new Identifier();
                identifier.setInstanceId(UUID.randomUUID().toString());
                identifier.setMetaId(UUID.randomUUID().toString());
                identifier.setUid(UUID.randomUUID().toString());
                identifier.setType("DOI");
                identifier.setIdentifier("10.1234/cycle-" + cycle);

                LinkedEntity le = api.create(identifier, null, null, null);
                assertTrue(UserGroupManagementAPI.addMetadataElementToGroup(le.getMetaId(), cycleGroup.getId()));

                // Verify
                Group groupMid = UserGroupManagementAPI.retrieveGroupById(cycleGroup.getId());
                assertEquals(1, groupMid.getUsers().size());
                assertEquals(cycle + 1, groupMid.getEntities().size(), "Should have " + (cycle + 1) + " entities");

                // Remove user
                assertTrue(UserGroupManagementAPI.removeUserFromGroup(cycleGroup.getId(), user.getAuthIdentifier()),
                        "Remove user cycle " + cycle + " should succeed");

                // Verify user removed, entities remain
                Group groupEnd = UserGroupManagementAPI.retrieveGroupById(cycleGroup.getId());
                assertEquals(0, groupEnd.getUsers().size());
                assertEquals(cycle + 1, groupEnd.getEntities().size());
            }

        } finally {
            UserGroupManagementAPI.deleteGroup(cycleGroup.getId());
        }
    }
}
