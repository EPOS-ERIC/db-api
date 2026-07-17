package usermanagementapis;

import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.User;
import org.epos.eposdatamodel.UserGroup;

import java.util.*;
import java.util.stream.Collectors;

public class UserGroupManagementAPI {

    private static final java.util.logging.Logger LOG = java.util.logging.Logger.getLogger(UserGroupManagementAPI.class.getName());

    /**
     *
     * USER OPERATIONS
     *
     */

    public static Boolean createUser(User user){
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[USER] Creating user with authIdentifier: {0}", user.getAuthIdentifier());
        }
        Boolean isFirstUser = getDbaccess().countAll(MetadataUser.class) == 0;
        Boolean isAdmin = isFirstUser || user.getIsAdmin();

        MetadataUser user1 = new MetadataUser();
        user1.setAuthIdentifier(user.getAuthIdentifier());
        user1.setEmail(user.getEmail());
        user1.setGivenname(user.getFirstName());
        user1.setFamilyname(user.getLastName());
        user1.setIsadmin(Boolean.toString(isAdmin));

        Boolean result = getDbaccess().updateObject(user1);
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("MetadataUser");
        }
        
        return result;
    }

    public static User retrieveUser(User user){
        if(user==null) return null;
        else return retrieveUserById(user.getAuthIdentifier());
    }

    public static User retrieveUserById(String userId){
        List<MetadataUser> userList = getDbaccess().getOneFromDBBySpecificKeySimple("authIdentifier",userId, MetadataUser.class);
        if(userList.isEmpty()) return null;

        MetadataUser retrievedUser = userList.get(0);
        User user1 = new User(
                retrievedUser.getAuthIdentifier(),
                retrievedUser.getFamilyname(),
                retrievedUser.getGivenname(),
                retrievedUser.getEmail(),
                Boolean.parseBoolean(retrievedUser.getIsadmin())
        );

        // Optimized: Query only for this user's groups instead of loading ALL group-user relationships
        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getOneFromDBBySpecificKeySimple(
                "authIdentifier.authIdentifier", retrievedUser.getAuthIdentifier(), MetadataGroupUser.class);
        for(MetadataGroupUser groupUser : metadataGroupUserList){
            UserGroup userGroup = new UserGroup(RoleType.valueOf(groupUser.getRole()),groupUser.getGroup().getId());
            user1.getGroups().add(userGroup);
        }

        return user1 ;
    }

    public static List<User> retrieveAllUsers(){
        List<MetadataUser> userList = getDbaccess().getAllFromDB(MetadataUser.class);
        if(userList.isEmpty()) return null;

        List<User> returnList = new ArrayList<>();
        for(MetadataUser user : userList){
            returnList.add(retrieveUserById(user.getAuthIdentifier()));
        }

        return returnList ;
    }

    public static Boolean deleteUser(String authIdentfier){
        // Optimized: Query directly by authIdentifier instead of loading all users
        List<MetadataUser> userList = getDbaccess().getOneFromDBBySpecificKeySimple("authIdentifier", authIdentfier, MetadataUser.class);
        if(userList.isEmpty()) return null;
        
        Boolean result = getDbaccess().deleteObject(userList.get(0));
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("MetadataUser");
            getDbaccess().invalidateAllCachesForClass("MetadataGroupUser");
        }
        
        return result;
    }


    /**
     *
     * GROUPS OPERATIONS
     *
     */

    public static Boolean createGroup(Group group){
        MetadataGroup group1 = new MetadataGroup();
        group1.setId(group.getId()==null || group.getId().isBlank()? UUID.randomUUID().toString() : group.getId());
        group1.setName(group.getName());
        group1.setDescription(group.getDescription());
        
        Boolean result = getDbaccess().updateObject(group1);
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("MetadataGroup");
        }
        
        return result;
    }

    public static Group retrieveGroupById(String groupId){
        // Optimized: Query directly by ID instead of loading all groups
        List<MetadataGroup> metadataGroupList = getDbaccess().getOneFromDBBySpecificKeySimple("id", groupId, MetadataGroup.class);
        if(metadataGroupList.isEmpty()) return null;

        MetadataGroup metadataGroup = metadataGroupList.get(0);
        Group group1 = new Group(
                metadataGroup.getId(),
                metadataGroup.getName(),
                metadataGroup.getDescription()
        );

        // Optimized: Query only authorization groups for this specific group
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getOneFromDBBySpecificKeySimple(
                "group.id", groupId, AuthorizationGroup.class);
        for(AuthorizationGroup authorizationGroup : authorizationGroupList){
            group1.getEntities().add(authorizationGroup.getMeta().getMetaId());
        }

        group1.setUsers(new ArrayList<>());

        // Optimized: Query only group-users for this specific group
        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getOneFromDBBySpecificKeySimple(
                "group.id", groupId, MetadataGroupUser.class);
        for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
            HashMap<String,String> items = new HashMap<>();
            items.put("userId",metadataGroupUser.getAuthIdentifier().getAuthIdentifier());
            items.put("role",metadataGroupUser.getRole());
            items.put("requestStatus",metadataGroupUser.getRequestStatus());
            group1.getUsers().add(items);
        }

        return group1;
    }

    public static Group retrieveGroupByName(String groupName){
        // Optimized: Query directly by name instead of loading all groups
        List<MetadataGroup> metadataGroupList = getDbaccess().getOneFromDBBySpecificKeySimple("name", groupName, MetadataGroup.class);
        if(metadataGroupList.isEmpty()) return null;

        MetadataGroup metadataGroup = metadataGroupList.get(0);
        Group group1 = new Group(
                metadataGroup.getId(),
                metadataGroup.getName(),
                metadataGroup.getDescription()
        );

        // Optimized: Query only authorization groups for this specific group
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getOneFromDBBySpecificKeySimple(
                "group.id", metadataGroup.getId(), AuthorizationGroup.class);
        for(AuthorizationGroup authorizationGroup : authorizationGroupList){
            group1.getEntities().add(authorizationGroup.getMeta().getMetaId());
        }

        group1.setUsers(new ArrayList<>());

        // Optimized: Query only group-users for this specific group
        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getOneFromDBBySpecificKeySimple(
                "group.id", metadataGroup.getId(), MetadataGroupUser.class);
        for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
            HashMap<String,String> items = new HashMap<>();
            items.put("userId",metadataGroupUser.getAuthIdentifier().getAuthIdentifier());
            items.put("role",metadataGroupUser.getRole());
            items.put("requestStatus",metadataGroupUser.getRequestStatus());
            group1.getUsers().add(items);
        }

        return group1;
    }

    public static Group retrieveGroup(Group group){
        if(group==null) return null;
        return retrieveGroupById(group.getId());
    }

    public static List<Group> retrieveAllGroups(){
        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        if(metadataGroupList.isEmpty()) return null;

        List<Group> returnList = new ArrayList<>();
        for(MetadataGroup group : metadataGroupList){
            returnList.add(retrieveGroupById(group.getId()));
        }

        return returnList;
    }

    public static Boolean deleteGroup(String groupId){
        // Optimized: Query directly by id instead of loading all groups
        List<MetadataGroup> groupList = getDbaccess().getOneFromDBBySpecificKeySimple("id", groupId, MetadataGroup.class);
        if(groupList.isEmpty()) return null;
        
        Boolean result = getDbaccess().deleteObject(groupList.get(0));
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("MetadataGroup");
            getDbaccess().invalidateAllCachesForClass("MetadataGroupUser");
            getDbaccess().invalidateAllCachesForClass("AuthorizationGroup");
        }
        
        return result;
    }

    /**
     *
     * ASSIGN USER TO GROUP WITH AUTHORIZATIONS
     *
     */

    /**
     * Adds a user to a group with the specified role and request status.
     * If the user is already in the group, this method will UPDATE the existing
     * relationship with the new role and request status values.
     *
     * @param groupId the group ID
     * @param userId the user's auth identifier
     * @param role the role to assign
     * @param requestStatusType the request status
     * @return true if created/updated successfully, false if parameters are null or entities don't exist
     */
    public static Boolean addUserToGroup(String groupId, String userId, RoleType role, RequestStatusType requestStatusType){
        if(groupId==null || userId==null || role==null || requestStatusType==null) return false;
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("group.id", groupId);
        filters.put("authIdentifier.authIdentifier", userId);

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getFromDBByUsingMultipleKeys(filters,MetadataGroupUser.class);

        List<MetadataGroup> groupList = getDbaccess().getOneFromDBBySpecificKeySimple("id",groupId,MetadataGroup.class);
        List<MetadataUser> userList = getDbaccess().getOneFromDBBySpecificKeySimple("authIdentifier",userId,MetadataUser.class);
        
        if(groupList.isEmpty() || userList.isEmpty()) return false;
        
        MetadataGroup group = groupList.get(0);
        MetadataUser user = userList.get(0);

        MetadataGroupUser metadataGroupUser;
        if(!metadataGroupUserList.isEmpty()) {
            // UPDATE existing relationship instead of silently returning
            metadataGroupUser = metadataGroupUserList.get(0);
        } else {
            // CREATE new relationship
            metadataGroupUser = new MetadataGroupUser();
            metadataGroupUser.setId(UUID.randomUUID().toString());
            metadataGroupUser.setGroup(group);
            metadataGroupUser.setAuthIdentifier(user);
        }
        
        metadataGroupUser.setRequestStatus(requestStatusType.name());
        metadataGroupUser.setRole(role.name());

        Boolean result = getDbaccess().updateObject(metadataGroupUser);
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("MetadataGroupUser");
        }
        
        return result;
    }

    /**
     * Updates an existing user-group relationship with new role and request status.
     * Use this method when you need to change the role or status of an existing membership.
     *
     * @param groupId the group ID
     * @param userId the user's auth identifier
     * @param role the new role to assign
     * @param requestStatusType the new request status
     * @return true if updated successfully, false if user is not in group or parameters are null
     */
    public static Boolean updateUserInGroup(String groupId, String userId, RoleType role, RequestStatusType requestStatusType){
        if(groupId == null || userId == null || role == null || requestStatusType == null) return false;

        Map<String, Object> filters = new HashMap<>();
        filters.put("group.id", groupId);
        filters.put("authIdentifier.authIdentifier", userId);

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getFromDBByUsingMultipleKeys(filters, MetadataGroupUser.class);

        if(metadataGroupUserList.isEmpty()) return false; // User not in group

        MetadataGroupUser existing = metadataGroupUserList.get(0);
        existing.setRole(role.name());
        existing.setRequestStatus(requestStatusType.name());

        Boolean result = getDbaccess().updateObject(existing);
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("MetadataGroupUser");
        }
        
        return result;
    }

    public static Boolean removeUserFromGroup(String groupId, String userId){
        if(groupId == null || userId == null) return false;
        
        // Invalidate ALL caches (application + L2) BEFORE querying to ensure we get fresh data
        // This is critical because the user-group relationship may have been 
        // added after a previous query returned an empty (cached) result
        getDbaccess().invalidateAllCachesForClass("MetadataGroupUser");
        getDbaccess().evictL2CacheForUserGroupEntities();
        
        Map<String, Object> filters = new HashMap<>();
        filters.put("group.id", groupId);
        filters.put("authIdentifier.authIdentifier", userId);

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getFromDBByUsingMultipleKeys(filters, MetadataGroupUser.class);
        if(metadataGroupUserList.isEmpty()) return false;

        Boolean result = getDbaccess().deleteObject(metadataGroupUserList.get(0));
        
        // Invalidate caches AFTER deletion to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("MetadataGroupUser");
            getDbaccess().evictL2CacheForUserGroupEntities();
        }
        
        return result;
    }

    public static Boolean addMetadataElementToGroup(String metaId, String groupId){
        if(metaId==null || groupId==null) return false;

        Map<String, Object> filters = new HashMap<>();
        filters.put("group.id", groupId);
        filters.put("meta.metaId", metaId);

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getFromDBByUsingMultipleKeys(filters,AuthorizationGroup.class);

        if(!authorizationGroupList.isEmpty()) return true;

        List<MetadataGroup> groupList = getDbaccess().getOneFromDBBySpecificKeySimple("id",groupId, MetadataGroup.class);
        List<EdmEntityId> metaList = getDbaccess().getOneFromDBBySpecificKeySimple("metaId",metaId, EdmEntityId.class);
        
        if(groupList.isEmpty() || metaList.isEmpty()) return false;

        AuthorizationGroup authorizationGroup = new AuthorizationGroup();
        authorizationGroup.setId(UUID.randomUUID().toString());
        authorizationGroup.setGroup(groupList.get(0));
        authorizationGroup.setMeta(metaList.get(0));
        
        Boolean result = getDbaccess().updateObject(authorizationGroup);
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("AuthorizationGroup");
        }
        
        return result;
    }

    public static List<Group> retrieveGroupsFromMetaId(String metaId){
        List<Group> groups = new ArrayList<>();

        // Optimized: Query only authorization groups for this specific metaId
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getFromDBByUsingMultipleKeys(
                Collections.singletonMap("meta.metaId", metaId), AuthorizationGroup.class);

        for(AuthorizationGroup authorizationGroup : authorizationGroupList){
            groups.add(retrieveGroupById(authorizationGroup.getGroup().getId()));
        }
        return groups;
    }

    public static List<String> retrieveShortGroupsFromMetaId(String metaId){
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[USER GROUPS] Retrieving groups for metaId: {0}", metaId);
        }
        List<String> groups = new ArrayList<>();
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getFromDBByUsingMultipleKeys(
                Collections.singletonMap("meta.metaId", metaId), AuthorizationGroup.class);
        authorizationGroupList.forEach(authorizationGroup -> groups.add(authorizationGroup.getGroup().getId()));
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[USER GROUPS] Found {0} groups for metaId: {1}", new Object[]{groups.size(), metaId});
        }
        return groups;
    }

    /**
     * Batch retrieves groups for multiple metaIds in a single operation.
     * This is optimized for bulk retrieval scenarios to avoid N+1 query problems.
     *
     * @param metaIds the list of metaIds to fetch groups for
     * @return a map from metaId to list of group IDs
     */
    public static Map<String, List<String>> batchRetrieveGroupsFromMetaIds(List<String> metaIds) {
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[USER GROUPS] Batch retrieving groups for {0} metaIds", metaIds.size());
        }
        if (metaIds == null || metaIds.isEmpty()) {
            return Collections.emptyMap();
        }

        // Query only the requested groups; loading the entire authorization table
        // makes bulk metadata retrieval scale with unrelated users and groups.
        List<AuthorizationGroup> allAuthGroups = getDbaccess().getListFromDBBySpecificKey(
                "meta.metaId", metaIds, AuthorizationGroup.class);

        // Build a map from metaId to group IDs
        Map<String, List<String>> result = new HashMap<>();
        for (String metaId : metaIds) {
            result.put(metaId, new ArrayList<>());
        }

        for (AuthorizationGroup ag : allAuthGroups) {
            String metaId = ag.getMeta().getMetaId();
            if (result.containsKey(metaId)) {
                result.get(metaId).add(ag.getGroup().getId());
            }
        }

        return result;
    }

    public static Boolean removeMetadataElementFromGroup(String metaId, String groupId){
        if(metaId == null || groupId == null) return false;

        // Optimized: Query using multiple keys instead of loading all authorization groups
        Map<String, Object> filters = new HashMap<>();
        filters.put("group.id", groupId);
        filters.put("meta.metaId", metaId);

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getFromDBByUsingMultipleKeys(filters, AuthorizationGroup.class);
        if(authorizationGroupList.isEmpty()) return false;

        Boolean result = getDbaccess().deleteObject(authorizationGroupList.get(0));
        
        // Invalidate caches to ensure fresh data on next retrieval
        if(result) {
            getDbaccess().invalidateAllCachesForClass("AuthorizationGroup");
        }
        
        return result;
    }

    public static List<String> retrieveMetaIdsFromGroups(){
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);
        if(authorizationGroupList.isEmpty()) return new ArrayList<>();

        // Use stream to collect unique metaIds efficiently
        return authorizationGroupList.stream()
                .map(group -> group.getMeta().getMetaId())
                .distinct()
                .collect(Collectors.toList());
    }
    /**
     * Checks if a metadata element and a user are in the same group.
     * 
     * @param metaId the metadata element's metaId
     * @param userId the user's auth identifier
     * @return true if they are NOT in the same group (no common groups), 
     *         false if they ARE in at least one common group
     */
    public static Boolean checkIfMetaIdAndUserIdAreInSameGroup(String metaId, String userId){
        Map<String, Object> filters = new HashMap<>();
        filters.put("meta.metaId", metaId);

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getFromDBByUsingMultipleKeys(filters,AuthorizationGroup.class);

        filters = new HashMap<>();
        filters.put("authIdentifier.authIdentifier", userId);

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getFromDBByUsingMultipleKeys(filters,MetadataGroupUser.class);

        // Extract group IDs instead of comparing MetadataGroup objects (which lack equals/hashCode)
        Set<String> metaGroupIds = authorizationGroupList.stream()
                .map(ag -> ag.getGroup().getId())
                .collect(Collectors.toSet());
        
        Set<String> userGroupIds = metadataGroupUserList.stream()
                .map(mgu -> mgu.getGroup().getId())
                .collect(Collectors.toSet());

        // Find common group IDs
        metaGroupIds.retainAll(userGroupIds);

        return metaGroupIds.isEmpty();
    }


    private static EposDataModelDAO getDbaccess() {
        return EposDataModelDAO.getInstance();
    }


}
