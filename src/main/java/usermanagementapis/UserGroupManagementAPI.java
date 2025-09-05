package usermanagementapis;

import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.User;
import org.epos.eposdatamodel.UserGroup;

import java.util.*;
import java.util.stream.Collectors;

public class UserGroupManagementAPI {

    /**
     *
     * USER OPERATIONS
     *
     */

    public static Boolean createUser(User user){
        MetadataUser user1 = new MetadataUser();
        user1.setAuthIdentifier(user.getAuthIdentifier());
        user1.setEmail(user.getEmail());
        user1.setGivenname(user.getFirstName());
        user1.setFamilyname(user.getLastName());
        user1.setIsadmin(Boolean.toString(user.getIsAdmin().booleanValue()));

        return getDbaccess().updateObject(user1);
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

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);
        for(MetadataGroupUser groupUser : metadataGroupUserList){
            if(groupUser.getAuthIdentifier().getAuthIdentifier().equals(retrievedUser.getAuthIdentifier())) {
                UserGroup userGroup = new UserGroup(RoleType.valueOf(groupUser.getRole()),groupUser.getGroup().getId());
                user1.getGroups().add(userGroup);
            }
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
        List<MetadataUser> userList = getDbaccess().getAllFromDB(MetadataUser.class);
        for(MetadataUser user : userList){
            if(user.getAuthIdentifier().equals(authIdentfier)){
                return getDbaccess().deleteObject(user);
            }
        }
        return null;
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
        return getDbaccess().updateObject(group1);
    }

    public static Group retrieveGroupById(String groupId){
        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);
        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);

        if(metadataGroupList.isEmpty()) return null;

        for(MetadataGroup metadataGroup : metadataGroupList){
            if(metadataGroup.getId().equals(groupId)){
                Group group1 = new Group(
                        metadataGroup.getId(),
                        metadataGroup.getName(),
                        metadataGroup.getDescription()
                );
                for(AuthorizationGroup authorizationGroup : authorizationGroupList){
                    if(authorizationGroup.getGroup().getId().equals(groupId)){
                        group1.getEntities().add(authorizationGroup.getMeta().getMetaId());
                    }
                }
                group1.setUsers(new ArrayList<>());

                for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
                    if(metadataGroupUser.getGroup().getId().equals(metadataGroup.getId())){
                        HashMap<String,String> items = new HashMap<>();
                        items.put("userId",metadataGroupUser.getAuthIdentifier().getAuthIdentifier());
                        items.put("role",metadataGroupUser.getRole());
                        items.put("requestStatus",metadataGroupUser.getRequestStatus());
                        group1.getUsers().add(items);
                    }
                }
                return group1;
            }
        }
        return null;
    }

    public static Group retrieveGroupByName(String groupName){
        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);
        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);

        if(metadataGroupList.isEmpty()) return null;

        for(MetadataGroup metadataGroup : metadataGroupList){
            if(metadataGroup.getName().equals(groupName)){
                Group group1 = new Group(
                        metadataGroup.getId(),
                        metadataGroup.getName(),
                        metadataGroup.getDescription()
                );
                for(AuthorizationGroup authorizationGroup : authorizationGroupList){
                    if(authorizationGroup.getGroup().getName().equals(groupName)){
                        group1.getEntities().add(authorizationGroup.getMeta().getMetaId());
                    }
                }
                group1.setUsers(new ArrayList<>());

                for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
                    if(metadataGroupUser.getGroup().getId().equals(metadataGroup.getId())){
                        HashMap<String,String> items = new HashMap<>();
                        items.put("userId",metadataGroupUser.getAuthIdentifier().getAuthIdentifier());
                        items.put("role",metadataGroupUser.getRole());
                        items.put("requestStatus",metadataGroupUser.getRequestStatus());
                        group1.getUsers().add(items);
                    }
                }
                return group1;
            }
        }
        return null;
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
        List<MetadataGroup> groupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        for(MetadataGroup group : groupList){
            if(group.getId().equals(groupId)){
                return getDbaccess().deleteObject(group);
            }
        }
        return null;
    }

    /**
     *
     * ASSIGN USER TO GROUP WITH AUTHORIZATIONS
     *
     */

    public static Boolean addUserToGroup(String groupId, String userId, RoleType role, RequestStatusType requestStatusType){
        if(groupId==null || userId==null || role==null || requestStatusType==null) return false;
        Map<String, Object> filters = new HashMap<>();
        filters.put("group.id", groupId);
        filters.put("authIdentifier.authIdentifier", userId);

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getFromDBByUsingMultipleKeys(filters,MetadataGroupUser.class);

        MetadataGroup group = (MetadataGroup) getDbaccess().getOneFromDBBySpecificKeySimple("id",groupId,MetadataGroup.class).get(0);
        MetadataUser user = (MetadataUser) getDbaccess().getOneFromDBBySpecificKeySimple("authIdentifier",userId,MetadataUser.class).get(0);

        //System.out.println(group);
        //System.out.println(user);

        if(!metadataGroupUserList.isEmpty()) return true;

        MetadataGroupUser metadataGroupUser = new MetadataGroupUser();
        metadataGroupUser.setId(UUID.randomUUID().toString());
        metadataGroupUser.setGroup(group);
        metadataGroupUser.setAuthIdentifier(user);
        metadataGroupUser.setRequestStatus(requestStatusType.name());
        metadataGroupUser.setRole(role.name());

        return getDbaccess().updateObject(metadataGroupUser);
    }

    public static Boolean removeUserFromGroup(String groupId, String userId){

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);
        if(metadataGroupUserList.isEmpty()) return null;

        for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
            if(metadataGroupUser.getGroup().getId().equals(groupId) &&
                    metadataGroupUser.getAuthIdentifier().getAuthIdentifier().equals(userId)){
                return getDbaccess().deleteObject(metadataGroupUser);
            }
        }
        return false;
    }

    public static Boolean addMetadataElementToGroup(String metaId, String groupId){
        if(metaId==null || groupId==null) return false;

        Map<String, Object> filters = new HashMap<>();
        filters.put("group.id", groupId);
        filters.put("meta.metaId", metaId);

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getFromDBByUsingMultipleKeys(filters,AuthorizationGroup.class);

        if(!authorizationGroupList.isEmpty()) return true;

        AuthorizationGroup authorizationGroup = new AuthorizationGroup();
        authorizationGroup.setId(UUID.randomUUID().toString());
        authorizationGroup.setGroup((MetadataGroup) getDbaccess().getOneFromDBBySpecificKeySimple("id",groupId, MetadataGroup.class).get(0));
        authorizationGroup.setMeta((EdmEntityId) getDbaccess().getOneFromDBBySpecificKeySimple("metaId",metaId, EdmEntityId.class).get(0));
        return getDbaccess().updateObject(authorizationGroup);
    }

    public static List<Group> retrieveGroupsFromMetaId(String metaId){

        List<Group> groups = new ArrayList<>();

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);

        for(AuthorizationGroup authorizationGroup : authorizationGroupList){
            if(authorizationGroup.getMeta().getMetaId().equals(metaId)){
                groups.add(retrieveGroupById(authorizationGroup.getGroup().getId()));
            }
        }
        return groups;
    }

    public static List<String> retrieveShortGroupsFromMetaId(String metaId){

        List<String> groups = new ArrayList<>();
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getOneFromDBBySpecificKeySimple("meta.metaId", metaId, AuthorizationGroup.class);
        authorizationGroupList.forEach(authorizationGroup -> groups.add(authorizationGroup.getGroup().getId()));
        return groups;
    }

    public static Boolean removeMetadataElementFromGroup(String metaId, String groupId){

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);
        if(authorizationGroupList.isEmpty()) return null;

        for(AuthorizationGroup authorizationGroup : authorizationGroupList){
            if(authorizationGroup.getGroup().getId().equals(groupId) &&
                    authorizationGroup.getMeta().getMetaId().equals(metaId)){
                return getDbaccess().deleteObject(authorizationGroup);
            }
        }
        return false;
    }

    public static List<String> retrieveMetaIdsFromGroups(){
        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);
        if(authorizationGroupList.isEmpty()) return new ArrayList<>();

        List<String> returnList = new ArrayList<>();
        for(AuthorizationGroup group : authorizationGroupList){
            returnList.add(group.getMeta().getMetaId());
        }

        return returnList;
    }
    //I NEED TO CHECK IF METAID GROUP IS IN USER AVAILABLE GROUPS
    public static Boolean checkIfMetaIdAndUserIdAreInSameGroup(String metaId, String userId){
        Map<String, Object> filters = new HashMap<>();
        filters.put("meta.metaId", metaId);

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getFromDBByUsingMultipleKeys(filters,AuthorizationGroup.class);

        filters = new HashMap<>();
        filters.put("authIdentifier.authIdentifier", userId);

        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getFromDBByUsingMultipleKeys(filters,MetadataGroupUser.class);

        List<MetadataGroup> groupList1 = authorizationGroupList.stream().map(AuthorizationGroup::getGroup).collect(Collectors.toList());
        List<MetadataGroup> groupList2 = metadataGroupUserList.stream().map(MetadataGroupUser::getGroup).collect(Collectors.toList());

        Set<MetadataGroup> groupSet = new HashSet<>(groupList2);
        List<MetadataGroup> commonGroups = groupList1.stream()
                .filter(groupSet::contains)
                .collect(Collectors.toList());

        return commonGroups.isEmpty();
    }


    private static EposDataModelDAO getDbaccess() {
        return EposDataModelDAO.getInstance();
    }


}