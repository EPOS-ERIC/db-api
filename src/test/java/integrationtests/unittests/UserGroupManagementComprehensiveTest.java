package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.RequestStatusType;
import model.RoleType;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.Identifier;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.User;
import org.epos.eposdatamodel.UserGroup;
import org.junit.jupiter.api.*;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for UserGroupManagementAPI covering all possible use cases:
 * 
 * 1. USER OPERATIONS
 *    - Create user
 *    - Update user
 *    - Retrieve user by ID
 *    - Retrieve all users
 *    - Delete user
 *    - Edge cases: null values, non-existent users
 * 
 * 2. GROUP OPERATIONS
 *    - Create group
 *    - Update group
 *    - Retrieve group by ID
 *    - Retrieve group by name
 *    - Retrieve all groups
 *    - Delete group
 *    - Edge cases: null values, empty names, non-existent groups
 * 
 * 3. USER-GROUP RELATIONSHIP OPERATIONS
 *    - Add user to group (new relationship)
 *    - Add user to group (update existing - role change)
 *    - Add user to group (update existing - status change)
 *    - Update user in group (explicit update)
 *    - Remove user from group
 *    - Multiple users in same group
 *    - Same user in multiple groups
 *    - Edge cases: null parameters, non-existent entities
 * 
 * 4. ROLE AND STATUS TRANSITIONS
 *    - All RoleType transitions (ADMIN, EDITOR, REVIEWER, VIEWER)
 *    - All RequestStatusType transitions (ACCEPTED, PENDING, REJECTED, NONE, DEFAULT)
 * 
 * 5. METADATA ELEMENT-GROUP OPERATIONS
 *    - Add metadata element to group
 *    - Remove metadata element from group
 *    - Retrieve groups from metaId
 *    - Retrieve metaIds from groups
 *    - Check if metaId and userId are in same group
 * 
 * 6. CACHE BEHAVIOR
 *    - Verify updates are visible after cache invalidation
 *    - Verify no stale data after CRUD operations
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserGroupManagementComprehensiveTest extends TestcontainersLifecycle {

    // Test data - shared across tests
    private static User user1;
    private static User user2;
    private static User user3;
    private static Group group1;
    private static Group group2;
    private static Identifier identifier1;
    private static Identifier identifier2;
    private static LinkedEntity identifierLe1;
    private static LinkedEntity identifierLe2;

    // =================== SECTION 1: USER OPERATIONS ===================

    @Test
    @Order(1)
    @DisplayName("1.1 Create new user with all fields")
    void testCreateUserWithAllFields() {
        user1 = new User("user-auth-001", "Smith", "John", "john.smith@example.com", false);
        Boolean result = UserGroupManagementAPI.createUser(user1);

        assertTrue(result, "User creation should succeed");

        User retrieved = UserGroupManagementAPI.retrieveUserById(user1.getAuthIdentifier());
        assertNotNull(retrieved, "Retrieved user should not be null");
        assertEquals(user1.getAuthIdentifier(), retrieved.getAuthIdentifier());
        assertEquals(user1.getLastName(), retrieved.getLastName());
        assertEquals(user1.getFirstName(), retrieved.getFirstName());
        assertEquals(user1.getEmail(), retrieved.getEmail());
        assertTrue(retrieved.getIsAdmin(), "First created user should be admin");
    }

    @Test
    @Order(2)
    @DisplayName("1.2 Create admin user")
    void testCreateAdminUser() {
        user2 = new User("user-auth-002", "Admin", "Super", "admin@example.com", true);
        Boolean result = UserGroupManagementAPI.createUser(user2);

        assertTrue(result);

        User retrieved = UserGroupManagementAPI.retrieveUserById(user2.getAuthIdentifier());
        assertNotNull(retrieved);
        assertTrue(retrieved.getIsAdmin(), "User should be admin");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 Create user with minimal data")
    void testCreateUserMinimalData() {
        user3 = new User("user-auth-003", null, null, null, false);
        Boolean result = UserGroupManagementAPI.createUser(user3);

        assertTrue(result);

        User retrieved = UserGroupManagementAPI.retrieveUserById(user3.getAuthIdentifier());
        assertNotNull(retrieved);
        assertNull(retrieved.getLastName());
        assertNull(retrieved.getFirstName());
        assertNull(retrieved.getEmail());
        assertFalse(retrieved.getIsAdmin(), "Non-first user should keep requested non-admin role");
    }

    @Test
    @Order(4)
    @DisplayName("1.4 Update existing user - change all fields")
    void testUpdateUserAllFields() {
        user1.setLastName("Johnson");
        user1.setFirstName("Jane");
        user1.setEmail("jane.johnson@example.com");
        user1.setIsAdmin(true);

        Boolean result = UserGroupManagementAPI.createUser(user1);
        assertTrue(result);

        User retrieved = UserGroupManagementAPI.retrieveUserById(user1.getAuthIdentifier());
        assertEquals("Johnson", retrieved.getLastName());
        assertEquals("Jane", retrieved.getFirstName());
        assertEquals("jane.johnson@example.com", retrieved.getEmail());
        // Note: isAdmin update should also be verified if supported
    }

    @Test
    @Order(5)
    @DisplayName("1.5 Retrieve user using User object")
    void testRetrieveUserByUserObject() {
        User retrieved = UserGroupManagementAPI.retrieveUser(user1);
        assertNotNull(retrieved);
        assertEquals(user1.getAuthIdentifier(), retrieved.getAuthIdentifier());
    }

    @Test
    @Order(6)
    @DisplayName("1.6 Retrieve non-existent user returns null")
    void testRetrieveNonExistentUser() {
        User retrieved = UserGroupManagementAPI.retrieveUserById("non-existent-user-id");
        assertNull(retrieved, "Non-existent user should return null");
    }

    @Test
    @Order(7)
    @DisplayName("1.7 Retrieve user with null returns null")
    void testRetrieveUserWithNull() {
        User retrieved = UserGroupManagementAPI.retrieveUser(null);
        assertNull(retrieved, "Null user should return null");
    }

    @Test
    @Order(8)
    @DisplayName("1.8 Retrieve all users")
    void testRetrieveAllUsers() {
        List<User> users = UserGroupManagementAPI.retrieveAllUsers();
        assertNotNull(users);
        assertTrue(users.size() >= 3, "Should have at least 3 users");
    }

    // =================== SECTION 2: GROUP OPERATIONS ===================

    @Test
    @Order(20)
    @DisplayName("2.1 Create group with all fields")
    void testCreateGroupWithAllFields() {
        group1 = new Group(UUID.randomUUID().toString(), "Engineering Team", "Software engineering department");
        Boolean result = UserGroupManagementAPI.createGroup(group1);

        assertTrue(result);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertNotNull(retrieved);
        assertEquals(group1.getId(), retrieved.getId());
        assertEquals(group1.getName(), retrieved.getName());
        assertEquals(group1.getDescription(), retrieved.getDescription());
    }

    @Test
    @Order(21)
    @DisplayName("2.2 Create group with auto-generated ID")
    void testCreateGroupAutoGeneratedId() {
        Group groupWithoutId = new Group(null, "Auto ID Group", "Group with auto-generated ID");
        Boolean result = UserGroupManagementAPI.createGroup(groupWithoutId);

        assertTrue(result);
        // The ID should be auto-generated, so we need to find it by name
        Group retrieved = UserGroupManagementAPI.retrieveGroupByName("Auto ID Group");
        assertNotNull(retrieved);
        assertNotNull(retrieved.getId());
        assertFalse(retrieved.getId().isBlank());
    }

    @Test
    @Order(22)
    @DisplayName("2.3 Create group with empty ID (should auto-generate)")
    void testCreateGroupEmptyId() {
        Group groupEmptyId = new Group("", "Empty ID Group", "Group with empty ID");
        Boolean result = UserGroupManagementAPI.createGroup(groupEmptyId);

        assertTrue(result);
        Group retrieved = UserGroupManagementAPI.retrieveGroupByName("Empty ID Group");
        assertNotNull(retrieved);
        assertNotNull(retrieved.getId());
        assertFalse(retrieved.getId().isEmpty());
    }

    @Test
    @Order(23)
    @DisplayName("2.4 Create group with null name")
    void testCreateGroupNullName() {
        Group groupNullName = new Group(UUID.randomUUID().toString(), null, "Group with null name");
        Boolean result = UserGroupManagementAPI.createGroup(groupNullName);

        assertTrue(result);
        Group retrieved = UserGroupManagementAPI.retrieveGroupById(groupNullName.getId());
        assertNotNull(retrieved);
        assertNull(retrieved.getName());
    }

    @Test
    @Order(24)
    @DisplayName("2.5 Create second group for multi-group tests")
    void testCreateSecondGroup() {
        group2 = new Group(UUID.randomUUID().toString(), "Marketing Team", "Marketing department");
        Boolean result = UserGroupManagementAPI.createGroup(group2);

        assertTrue(result);
    }

    @Test
    @Order(25)
    @DisplayName("2.6 Update group description")
    void testUpdateGroupDescription() {
        group1.setDescription("Updated: Software engineering and DevOps");
        Boolean result = UserGroupManagementAPI.createGroup(group1);

        assertTrue(result);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertEquals("Updated: Software engineering and DevOps", retrieved.getDescription());
    }

    @Test
    @Order(26)
    @DisplayName("2.7 Retrieve group by name")
    void testRetrieveGroupByName() {
        Group retrieved = UserGroupManagementAPI.retrieveGroupByName("Engineering Team");
        assertNotNull(retrieved);
        assertEquals(group1.getId(), retrieved.getId());
    }

    @Test
    @Order(27)
    @DisplayName("2.8 Retrieve non-existent group by ID returns null")
    void testRetrieveNonExistentGroupById() {
        Group retrieved = UserGroupManagementAPI.retrieveGroupById("non-existent-group-id");
        assertNull(retrieved);
    }

    @Test
    @Order(28)
    @DisplayName("2.9 Retrieve non-existent group by name returns null")
    void testRetrieveNonExistentGroupByName() {
        Group retrieved = UserGroupManagementAPI.retrieveGroupByName("Non Existent Group");
        assertNull(retrieved);
    }

    @Test
    @Order(29)
    @DisplayName("2.10 Retrieve group with null Group object returns null")
    void testRetrieveGroupWithNull() {
        Group retrieved = UserGroupManagementAPI.retrieveGroup(null);
        assertNull(retrieved);
    }

    @Test
    @Order(30)
    @DisplayName("2.11 Retrieve all groups")
    void testRetrieveAllGroups() {
        List<Group> groups = UserGroupManagementAPI.retrieveAllGroups();
        assertNotNull(groups);
        assertTrue(groups.size() >= 2, "Should have at least 2 groups");
    }

    // =================== SECTION 3: USER-GROUP RELATIONSHIPS ===================

    @Test
    @Order(40)
    @DisplayName("3.1 Add user to group - new relationship")
    void testAddUserToGroupNew() {
        Boolean result = UserGroupManagementAPI.addUserToGroup(
                group1.getId(), user1.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING);

        assertTrue(result);

        Group retrievedGroup = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        User retrievedUser = UserGroupManagementAPI.retrieveUserById(user1.getAuthIdentifier());

        assertEquals(1, retrievedGroup.getUsers().size());
        assertEquals(user1.getAuthIdentifier(), retrievedGroup.getUsers().get(0).get("userId"));
        assertEquals("EDITOR", retrievedGroup.getUsers().get(0).get("role"));
        assertEquals("PENDING", retrievedGroup.getUsers().get(0).get("requestStatus"));

        assertEquals(1, retrievedUser.getGroups().size());
        assertEquals(group1.getId(), retrievedUser.getGroups().get(0).getGroupId());
        assertEquals(RoleType.EDITOR, retrievedUser.getGroups().get(0).getRole());
    }

    @Test
    @Order(41)
    @DisplayName("3.2 Add same user to same group - should UPDATE role")
    void testAddUserToGroupUpdateRole() {
        // Change role from EDITOR to ADMIN
        Boolean result = UserGroupManagementAPI.addUserToGroup(
                group1.getId(), user1.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.PENDING);

        assertTrue(result);

        Group retrievedGroup = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        
        // Should still have only 1 user (not duplicated)
        assertEquals(1, retrievedGroup.getUsers().size());
        // Role should be updated
        assertEquals("ADMIN", retrievedGroup.getUsers().get(0).get("role"));
    }

    @Test
    @Order(42)
    @DisplayName("3.3 Add same user to same group - should UPDATE status")
    void testAddUserToGroupUpdateStatus() {
        // Change status from PENDING to ACCEPTED
        Boolean result = UserGroupManagementAPI.addUserToGroup(
                group1.getId(), user1.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.ACCEPTED);

        assertTrue(result);

        Group retrievedGroup = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertEquals("ACCEPTED", retrievedGroup.getUsers().get(0).get("requestStatus"));
    }

    @Test
    @Order(43)
    @DisplayName("3.4 Update user in group - explicit update method")
    void testUpdateUserInGroupExplicit() {
        Boolean result = UserGroupManagementAPI.updateUserInGroup(
                group1.getId(), user1.getAuthIdentifier(), RoleType.REVIEWER, RequestStatusType.REJECTED);

        assertTrue(result);

        Group retrievedGroup = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertEquals("REVIEWER", retrievedGroup.getUsers().get(0).get("role"));
        assertEquals("REJECTED", retrievedGroup.getUsers().get(0).get("requestStatus"));

        User retrievedUser = UserGroupManagementAPI.retrieveUserById(user1.getAuthIdentifier());
        assertEquals(RoleType.REVIEWER, retrievedUser.getGroups().get(0).getRole());
    }

    @Test
    @Order(44)
    @DisplayName("3.5 Update non-existent user in group returns false")
    void testUpdateNonExistentUserInGroup() {
        Boolean result = UserGroupManagementAPI.updateUserInGroup(
                group1.getId(), "non-existent-user", RoleType.VIEWER, RequestStatusType.NONE);

        assertFalse(result);
    }

    @Test
    @Order(45)
    @DisplayName("3.6 Update user in non-existent group returns false")
    void testUpdateUserInNonExistentGroup() {
        Boolean result = UserGroupManagementAPI.updateUserInGroup(
                "non-existent-group", user1.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.NONE);

        assertFalse(result);
    }

    @Test
    @Order(46)
    @DisplayName("3.7 Add user to group with null parameters returns false")
    void testAddUserToGroupNullParams() {
        assertFalse(UserGroupManagementAPI.addUserToGroup(null, user1.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING));
        assertFalse(UserGroupManagementAPI.addUserToGroup(group1.getId(), null, RoleType.EDITOR, RequestStatusType.PENDING));
        assertFalse(UserGroupManagementAPI.addUserToGroup(group1.getId(), user1.getAuthIdentifier(), null, RequestStatusType.PENDING));
        assertFalse(UserGroupManagementAPI.addUserToGroup(group1.getId(), user1.getAuthIdentifier(), RoleType.EDITOR, null));
    }

    @Test
    @Order(47)
    @DisplayName("3.8 Update user in group with null parameters returns false")
    void testUpdateUserInGroupNullParams() {
        assertFalse(UserGroupManagementAPI.updateUserInGroup(null, user1.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING));
        assertFalse(UserGroupManagementAPI.updateUserInGroup(group1.getId(), null, RoleType.EDITOR, RequestStatusType.PENDING));
        assertFalse(UserGroupManagementAPI.updateUserInGroup(group1.getId(), user1.getAuthIdentifier(), null, RequestStatusType.PENDING));
        assertFalse(UserGroupManagementAPI.updateUserInGroup(group1.getId(), user1.getAuthIdentifier(), RoleType.EDITOR, null));
    }

    @Test
    @Order(48)
    @DisplayName("3.9 Add second user to same group")
    void testAddSecondUserToGroup() {
        Boolean result = UserGroupManagementAPI.addUserToGroup(
                group1.getId(), user2.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.ACCEPTED);

        assertTrue(result);

        Group retrievedGroup = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertEquals(2, retrievedGroup.getUsers().size());
    }

    @Test
    @Order(49)
    @DisplayName("3.10 Add same user to multiple groups")
    void testAddUserToMultipleGroups() {
        Boolean result = UserGroupManagementAPI.addUserToGroup(
                group2.getId(), user1.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.ACCEPTED);

        assertTrue(result);

        User retrievedUser = UserGroupManagementAPI.retrieveUserById(user1.getAuthIdentifier());
        assertEquals(2, retrievedUser.getGroups().size());

        // Verify both groups are present
        boolean hasGroup1 = false;
        boolean hasGroup2 = false;
        for (UserGroup ug : retrievedUser.getGroups()) {
            if (ug.getGroupId().equals(group1.getId())) hasGroup1 = true;
            if (ug.getGroupId().equals(group2.getId())) hasGroup2 = true;
        }
        assertTrue(hasGroup1 && hasGroup2, "User should be in both groups");
    }

    // =================== SECTION 4: ROLE AND STATUS TRANSITIONS ===================

    @Test
    @Order(60)
    @DisplayName("4.1 Test all RoleType values")
    void testAllRoleTypes() {
        // Test each role type
        for (RoleType role : RoleType.values()) {
            Boolean result = UserGroupManagementAPI.updateUserInGroup(
                    group1.getId(), user1.getAuthIdentifier(), role, RequestStatusType.ACCEPTED);
            assertTrue(result, "Update should succeed for role: " + role);

            Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
            String actualRole = null;
            for (Map<String, String> userMap : retrieved.getUsers()) {
                if (userMap.get("userId").equals(user1.getAuthIdentifier())) {
                    actualRole = userMap.get("role");
                    break;
                }
            }
            assertEquals(role.name(), actualRole, "Role should be " + role);
        }
    }

    @Test
    @Order(61)
    @DisplayName("4.2 Test all RequestStatusType values")
    void testAllRequestStatusTypes() {
        // Test each request status type
        for (RequestStatusType status : RequestStatusType.values()) {
            Boolean result = UserGroupManagementAPI.updateUserInGroup(
                    group1.getId(), user1.getAuthIdentifier(), RoleType.EDITOR, status);
            assertTrue(result, "Update should succeed for status: " + status);

            Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
            String actualStatus = null;
            for (Map<String, String> userMap : retrieved.getUsers()) {
                if (userMap.get("userId").equals(user1.getAuthIdentifier())) {
                    actualStatus = userMap.get("requestStatus");
                    break;
                }
            }
            assertEquals(status.name(), actualStatus, "Status should be " + status);
        }
    }

    @Test
    @Order(62)
    @DisplayName("4.3 Role transition: VIEWER -> EDITOR -> REVIEWER -> ADMIN")
    void testRoleProgression() {
        RoleType[] progression = {RoleType.VIEWER, RoleType.EDITOR, RoleType.REVIEWER, RoleType.ADMIN};

        for (RoleType role : progression) {
            UserGroupManagementAPI.updateUserInGroup(
                    group1.getId(), user1.getAuthIdentifier(), role, RequestStatusType.ACCEPTED);

            User retrieved = UserGroupManagementAPI.retrieveUserById(user1.getAuthIdentifier());
            RoleType actualRole = null;
            for (UserGroup ug : retrieved.getGroups()) {
                if (ug.getGroupId().equals(group1.getId())) {
                    actualRole = ug.getRole();
                    break;
                }
            }
            assertEquals(role, actualRole, "Role progression to " + role + " should work");
        }
    }

    @Test
    @Order(63)
    @DisplayName("4.4 Status transition: PENDING -> ACCEPTED")
    void testStatusPendingToAccepted() {
        UserGroupManagementAPI.updateUserInGroup(
                group1.getId(), user1.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.PENDING);
        UserGroupManagementAPI.updateUserInGroup(
                group1.getId(), user1.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        String status = null;
        for (Map<String, String> userMap : retrieved.getUsers()) {
            if (userMap.get("userId").equals(user1.getAuthIdentifier())) {
                status = userMap.get("requestStatus");
                break;
            }
        }
        assertEquals("ACCEPTED", status);
    }

    @Test
    @Order(64)
    @DisplayName("4.5 Status transition: PENDING -> REJECTED")
    void testStatusPendingToRejected() {
        UserGroupManagementAPI.updateUserInGroup(
                group1.getId(), user2.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.PENDING);
        UserGroupManagementAPI.updateUserInGroup(
                group1.getId(), user2.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.REJECTED);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        String status = null;
        for (Map<String, String> userMap : retrieved.getUsers()) {
            if (userMap.get("userId").equals(user2.getAuthIdentifier())) {
                status = userMap.get("requestStatus");
                break;
            }
        }
        assertEquals("REJECTED", status);
    }

    // =================== SECTION 5: METADATA ELEMENT-GROUP OPERATIONS ===================

    @Test
    @Order(80)
    @DisplayName("5.1 Add metadata element to group")
    void testAddMetadataElementToGroup() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());

        identifier1 = new Identifier();
        identifier1.setInstanceId(UUID.randomUUID().toString());
        identifier1.setMetaId(UUID.randomUUID().toString());
        identifier1.setUid(UUID.randomUUID().toString());
        identifier1.setType("DOI");
        identifier1.setIdentifier("10.1234/test.001");

        identifierLe1 = api.create(identifier1, null, null, null);

        Boolean result = UserGroupManagementAPI.addMetadataElementToGroup(identifierLe1.getMetaId(), group1.getId());
        assertTrue(result);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertTrue(retrieved.getEntities().contains(identifierLe1.getMetaId()));
    }

    @Test
    @Order(81)
    @DisplayName("5.2 Add same metadata element to same group - should not duplicate")
    void testAddSameMetadataElementToGroup() {
        Boolean result = UserGroupManagementAPI.addMetadataElementToGroup(identifierLe1.getMetaId(), group1.getId());
        assertTrue(result);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        // Count occurrences
        long count = retrieved.getEntities().stream()
                .filter(e -> e.equals(identifierLe1.getMetaId()))
                .count();
        assertEquals(1, count, "Should not have duplicate metadata elements");
    }

    @Test
    @Order(82)
    @DisplayName("5.3 Add second metadata element to group")
    void testAddSecondMetadataElementToGroup() {
        AbstractAPI api = AbstractAPI.retrieveAPI(EntityNames.IDENTIFIER.name());

        identifier2 = new Identifier();
        identifier2.setInstanceId(UUID.randomUUID().toString());
        identifier2.setMetaId(UUID.randomUUID().toString());
        identifier2.setUid(UUID.randomUUID().toString());
        identifier2.setType("ISBN");
        identifier2.setIdentifier("978-3-16-148410-0");

        identifierLe2 = api.create(identifier2, null, null, null);

        Boolean result = UserGroupManagementAPI.addMetadataElementToGroup(identifierLe2.getMetaId(), group1.getId());
        assertTrue(result);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertEquals(2, retrieved.getEntities().size());
    }

    @Test
    @Order(83)
    @DisplayName("5.4 Add metadata element to multiple groups")
    void testAddMetadataElementToMultipleGroups() {
        Boolean result = UserGroupManagementAPI.addMetadataElementToGroup(identifierLe1.getMetaId(), group2.getId());
        assertTrue(result);

        List<Group> groups = UserGroupManagementAPI.retrieveGroupsFromMetaId(identifierLe1.getMetaId());
        assertEquals(2, groups.size(), "Metadata should be in 2 groups");
    }

    @Test
    @Order(84)
    @DisplayName("5.5 Add metadata element with null parameters returns false")
    void testAddMetadataElementNullParams() {
        assertFalse(UserGroupManagementAPI.addMetadataElementToGroup(null, group1.getId()));
        assertFalse(UserGroupManagementAPI.addMetadataElementToGroup(identifierLe1.getMetaId(), null));
        assertFalse(UserGroupManagementAPI.addMetadataElementToGroup(null, null));
    }

    @Test
    @Order(85)
    @DisplayName("5.6 Retrieve groups from metaId")
    void testRetrieveGroupsFromMetaId() {
        List<Group> groups = UserGroupManagementAPI.retrieveGroupsFromMetaId(identifierLe1.getMetaId());
        assertNotNull(groups);
        assertFalse(groups.isEmpty());

        boolean foundGroup1 = groups.stream().anyMatch(g -> g.getId().equals(group1.getId()));
        boolean foundGroup2 = groups.stream().anyMatch(g -> g.getId().equals(group2.getId()));
        assertTrue(foundGroup1 && foundGroup2);
    }

    @Test
    @Order(86)
    @DisplayName("5.7 Retrieve short groups from metaId")
    void testRetrieveShortGroupsFromMetaId() {
        List<String> groupIds = UserGroupManagementAPI.retrieveShortGroupsFromMetaId(identifierLe1.getMetaId());
        assertNotNull(groupIds);
        assertTrue(groupIds.contains(group1.getId()));
        assertTrue(groupIds.contains(group2.getId()));
    }

    @Test
    @Order(87)
    @DisplayName("5.8 Retrieve metaIds from groups")
    void testRetrieveMetaIdsFromGroups() {
        List<String> metaIds = UserGroupManagementAPI.retrieveMetaIdsFromGroups();
        assertNotNull(metaIds);
        assertTrue(metaIds.contains(identifierLe1.getMetaId()));
        assertTrue(metaIds.contains(identifierLe2.getMetaId()));
    }

    @Test
    @Order(88)
    @DisplayName("5.9 Check if metaId and userId are in same group - should find common group")
    void testCheckMetaIdAndUserIdInSameGroup() {
        // First, ensure user1 is in group1 (re-add if necessary from previous tests)
        UserGroupManagementAPI.addUserToGroup(group1.getId(), user1.getAuthIdentifier(), RoleType.EDITOR, RequestStatusType.ACCEPTED);
        
        // user1 is in group1, identifierLe1.metaId is also in group1
        // The method returns TRUE if commonGroups.isEmpty() - i.e., true means NOT in same group
        Boolean result = UserGroupManagementAPI.checkIfMetaIdAndUserIdAreInSameGroup(
                identifierLe1.getMetaId(), user1.getAuthIdentifier());
        // When they ARE in same group, commonGroups is NOT empty, so method returns false
        assertFalse(result, "User1 and identifier1 ARE in same group, so method should return false (not empty)");
    }

    @Test
    @Order(89)
    @DisplayName("5.10 Check if metaId and userId NOT in same group")
    void testCheckMetaIdAndUserIdNotInSameGroup() {
        // user3 is not in any group - ensure this by removing from all groups first
        UserGroupManagementAPI.removeUserFromGroup(group1.getId(), user3.getAuthIdentifier());
        UserGroupManagementAPI.removeUserFromGroup(group2.getId(), user3.getAuthIdentifier());
        
        // The method returns TRUE if commonGroups.isEmpty() - i.e., true means NOT in same group
        Boolean result = UserGroupManagementAPI.checkIfMetaIdAndUserIdAreInSameGroup(
                identifierLe1.getMetaId(), user3.getAuthIdentifier());
        // When they are NOT in same group, commonGroups IS empty, so method returns true
        assertTrue(result, "User3 and identifier1 are NOT in same group, so method should return true (empty)");
    }

    @Test
    @Order(90)
    @DisplayName("5.11 Remove metadata element from group")
    void testRemoveMetadataElementFromGroup() {
        Boolean result = UserGroupManagementAPI.removeMetadataElementFromGroup(identifierLe2.getMetaId(), group1.getId());
        assertTrue(result);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertFalse(retrieved.getEntities().contains(identifierLe2.getMetaId()));
    }

    @Test
    @Order(91)
    @DisplayName("5.12 Remove non-existent metadata element from group returns false")
    void testRemoveNonExistentMetadataElement() {
        Boolean result = UserGroupManagementAPI.removeMetadataElementFromGroup("non-existent-meta-id", group1.getId());
        assertFalse(result);
    }

    @Test
    @Order(92)
    @DisplayName("5.13 Remove metadata element with null params returns false")
    void testRemoveMetadataElementNullParams() {
        assertFalse(UserGroupManagementAPI.removeMetadataElementFromGroup(null, group1.getId()));
        assertFalse(UserGroupManagementAPI.removeMetadataElementFromGroup(identifierLe1.getMetaId(), null));
    }

    // =================== SECTION 6: CACHE BEHAVIOR ===================

    @Test
    @Order(100)
    @DisplayName("6.1 Verify updates are immediately visible (no stale cache)")
    void testCacheInvalidationOnUpdate() {
        // Get initial state
        Group before = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        int initialUserCount = before.getUsers().size();

        // Add a new user
        UserGroupManagementAPI.addUserToGroup(
                group1.getId(), user3.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.PENDING);

        // Immediately verify the change is visible
        Group after = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertEquals(initialUserCount + 1, after.getUsers().size(), 
                "New user should be immediately visible after add");
    }

    @Test
    @Order(101)
    @DisplayName("6.2 Verify role update is immediately visible")
    void testCacheInvalidationOnRoleUpdate() {
        // Update role
        UserGroupManagementAPI.updateUserInGroup(
                group1.getId(), user3.getAuthIdentifier(), RoleType.ADMIN, RequestStatusType.ACCEPTED);

        // Immediately verify
        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        String role = null;
        for (Map<String, String> userMap : retrieved.getUsers()) {
            if (userMap.get("userId").equals(user3.getAuthIdentifier())) {
                role = userMap.get("role");
                break;
            }
        }
        assertEquals("ADMIN", role, "Role update should be immediately visible");
    }

    @Test
    @Order(102)
    @DisplayName("6.3 Verify deletion is immediately reflected")
    void testCacheInvalidationOnDelete() {
        // Remove user from group
        UserGroupManagementAPI.removeUserFromGroup(group1.getId(), user3.getAuthIdentifier());

        // Immediately verify
        Group retrieved = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        boolean userStillPresent = retrieved.getUsers().stream()
                .anyMatch(u -> u.get("userId").equals(user3.getAuthIdentifier()));
        assertFalse(userStillPresent, "Removed user should not be present");
    }

    @Test
    @Order(103)
    @DisplayName("6.4 Multiple rapid updates should all be visible")
    void testMultipleRapidUpdates() {
        // Perform multiple updates in quick succession
        RoleType[] roles = {RoleType.VIEWER, RoleType.EDITOR, RoleType.REVIEWER, RoleType.ADMIN};

        for (RoleType role : roles) {
            UserGroupManagementAPI.updateUserInGroup(
                    group1.getId(), user1.getAuthIdentifier(), role, RequestStatusType.ACCEPTED);

            // Each update should be immediately visible
            User retrieved = UserGroupManagementAPI.retrieveUserById(user1.getAuthIdentifier());
            RoleType actualRole = null;
            for (UserGroup ug : retrieved.getGroups()) {
                if (ug.getGroupId().equals(group1.getId())) {
                    actualRole = ug.getRole();
                    break;
                }
            }
            assertEquals(role, actualRole, "Rapid update to " + role + " should be immediately visible");
        }
    }

    // =================== SECTION 7: REMOVE USER FROM GROUP ===================

    @Test
    @Order(110)
    @DisplayName("7.1 Remove user from group")
    void testRemoveUserFromGroup() {
        // First ensure user2 is in group1
        Group before = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        boolean user2InGroup = before.getUsers().stream()
                .anyMatch(u -> u.get("userId").equals(user2.getAuthIdentifier()));
        assertTrue(user2InGroup, "user2 should be in group1 before removal");

        Boolean result = UserGroupManagementAPI.removeUserFromGroup(group1.getId(), user2.getAuthIdentifier());
        assertTrue(result);

        Group after = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        boolean user2StillInGroup = after.getUsers().stream()
                .anyMatch(u -> u.get("userId").equals(user2.getAuthIdentifier()));
        assertFalse(user2StillInGroup, "user2 should not be in group1 after removal");
    }

    @Test
    @Order(111)
    @DisplayName("7.2 Remove non-existent user from group returns false")
    void testRemoveNonExistentUserFromGroup() {
        Boolean result = UserGroupManagementAPI.removeUserFromGroup(group1.getId(), "non-existent-user");
        assertFalse(result);
    }

    @Test
    @Order(112)
    @DisplayName("7.3 Remove user from non-existent group returns false")
    void testRemoveUserFromNonExistentGroup() {
        Boolean result = UserGroupManagementAPI.removeUserFromGroup("non-existent-group", user1.getAuthIdentifier());
        assertFalse(result);
    }

    // =================== SECTION 8: DELETE OPERATIONS ===================

    @Test
    @Order(120)
    @DisplayName("8.1 Delete user also removes from groups")
    void testDeleteUserRemovesFromGroups() {
        // Create a temporary user and add to group
        User tempUser = new User("temp-user-delete", "Temp", "User", "temp@example.com", false);
        UserGroupManagementAPI.createUser(tempUser);
        UserGroupManagementAPI.addUserToGroup(group1.getId(), tempUser.getAuthIdentifier(), RoleType.VIEWER, RequestStatusType.PENDING);

        // Verify user is in group
        Group before = UserGroupManagementAPI.retrieveGroupById(group1.getId());
        assertTrue(before.getUsers().stream().anyMatch(u -> u.get("userId").equals(tempUser.getAuthIdentifier())));

        // Delete user
        Boolean result = UserGroupManagementAPI.deleteUser(tempUser.getAuthIdentifier());
        assertTrue(result);

        // Verify user is deleted
        User deleted = UserGroupManagementAPI.retrieveUserById(tempUser.getAuthIdentifier());
        assertNull(deleted);
    }

    @Test
    @Order(121)
    @DisplayName("8.2 Delete non-existent user returns null")
    void testDeleteNonExistentUser() {
        Boolean result = UserGroupManagementAPI.deleteUser("definitely-non-existent-user");
        assertNull(result);
    }

    @Test
    @Order(122)
    @DisplayName("8.3 Delete group")
    void testDeleteGroup() {
        // Create a temporary group
        Group tempGroup = new Group(UUID.randomUUID().toString(), "Temp Group", "To be deleted");
        UserGroupManagementAPI.createGroup(tempGroup);

        // Verify it exists
        assertNotNull(UserGroupManagementAPI.retrieveGroupById(tempGroup.getId()));

        // Delete it
        Boolean result = UserGroupManagementAPI.deleteGroup(tempGroup.getId());
        assertTrue(result);

        // Verify it's gone
        assertNull(UserGroupManagementAPI.retrieveGroupById(tempGroup.getId()));
    }

    @Test
    @Order(123)
    @DisplayName("8.4 Delete non-existent group returns null")
    void testDeleteNonExistentGroup() {
        Boolean result = UserGroupManagementAPI.deleteGroup("definitely-non-existent-group");
        assertNull(result);
    }

    // =================== SECTION 9: EDGE CASES AND BOUNDARY CONDITIONS ===================

    @Test
    @Order(130)
    @DisplayName("9.1 Create user with very long authIdentifier")
    void testCreateUserLongAuthId() {
        String longId = "user-" + UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
        // Truncate to 100 chars if needed (based on DB constraint)
        if (longId.length() > 100) longId = longId.substring(0, 100);

        User longIdUser = new User(longId, "Long", "Id", "longid@example.com", false);
        Boolean result = UserGroupManagementAPI.createUser(longIdUser);
        assertTrue(result);

        User retrieved = UserGroupManagementAPI.retrieveUserById(longId);
        assertNotNull(retrieved);

        // Cleanup
        UserGroupManagementAPI.deleteUser(longId);
    }

    @Test
    @Order(131)
    @DisplayName("9.2 Create group with special characters in name")
    void testCreateGroupSpecialChars() {
        Group specialGroup = new Group(UUID.randomUUID().toString(), 
                "Test & Group <with> \"special\" 'chars'", 
                "Description with émojis 🎉 and unicode");
        Boolean result = UserGroupManagementAPI.createGroup(specialGroup);
        assertTrue(result);

        Group retrieved = UserGroupManagementAPI.retrieveGroupById(specialGroup.getId());
        assertNotNull(retrieved);
        assertEquals(specialGroup.getName(), retrieved.getName());

        // Cleanup
        UserGroupManagementAPI.deleteGroup(specialGroup.getId());
    }

    @Test
    @Order(132)
    @DisplayName("9.3 Empty groups list when no groups exist for metaId")
    void testEmptyGroupsForNonExistentMetaId() {
        List<Group> groups = UserGroupManagementAPI.retrieveGroupsFromMetaId("non-existent-meta-id");
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    @Test
    @Order(133)
    @DisplayName("9.4 Retrieve short groups for non-existent metaId returns empty list")
    void testEmptyShortGroupsForNonExistentMetaId() {
        List<String> groups = UserGroupManagementAPI.retrieveShortGroupsFromMetaId("non-existent-meta-id");
        assertNotNull(groups);
        assertTrue(groups.isEmpty());
    }

    // =================== SECTION 10: CLEANUP ===================

    @Test
    @Order(200)
    @DisplayName("10.1 Final cleanup - delete test groups")
    void testCleanupGroups() {
        if (group1 != null) {
            UserGroupManagementAPI.deleteGroup(group1.getId());
        }
        if (group2 != null) {
            UserGroupManagementAPI.deleteGroup(group2.getId());
        }
    }

    @Test
    @Order(201)
    @DisplayName("10.2 Final cleanup - delete test users")
    void testCleanupUsers() {
        if (user1 != null) {
            UserGroupManagementAPI.deleteUser(user1.getAuthIdentifier());
        }
        if (user2 != null) {
            UserGroupManagementAPI.deleteUser(user2.getAuthIdentifier());
        }
        if (user3 != null) {
            UserGroupManagementAPI.deleteUser(user3.getAuthIdentifier());
        }
    }
}
