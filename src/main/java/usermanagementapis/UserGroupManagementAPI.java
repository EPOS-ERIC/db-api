package usermanagementapis;

import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.UserGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UserGroupManagementAPI {

    /**
     *
     * USER OPERATIONS
     *
     */

    public static Boolean createUser(org.epos.eposdatamodel.User user){
        MetadataUser user1 = new MetadataUser();
        user1.setAuthIdentifier(user.getAuthIdentifier());
        user1.setEmail(user.getEmail());
        user1.setGivenname(user.getFirstName());
        user1.setFamilyname(user.getLastName());
        user1.setIsadmin(Boolean.toString(user.getIsAdmin().booleanValue()));

        return getDbaccess().updateObject(user1);
    }

    public static org.epos.eposdatamodel.User retrieveUser(org.epos.eposdatamodel.User user){
        List<MetadataUser> userList = getDbaccess().getAllFromDB( MetadataUser.class);
        if(userList.isEmpty()) return null;
        for(MetadataUser retrievedUser : userList){
            if(retrievedUser.getAuthIdentifier().equals(user.getAuthIdentifier())){
                org.epos.eposdatamodel.User user1 = new org.epos.eposdatamodel.User(
                        retrievedUser.getAuthIdentifier(),
                        retrievedUser.getFamilyname(),
                        retrievedUser.getGivenname(),
                        retrievedUser.getEmail(),
                        Boolean.parseBoolean(retrievedUser.getIsadmin())
                );
                List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB( MetadataGroupUser.class);
                for(MetadataGroupUser groupUser : metadataGroupUserList){
                    if(groupUser.getAuthIdentifier().getAuthIdentifier().equals(retrievedUser.getAuthIdentifier())) {
                        UserGroup userGroup = new UserGroup(RoleType.valueOf(groupUser.getRole()),groupUser.getGroup().getId());
                        user1.getGroups().add(userGroup);
                    }
                }
                return user1 ;
            }
        }
        return null;
    }

    public static org.epos.eposdatamodel.User retrieveUserById(String userId){
        List<MetadataUser> userList = getDbaccess().getOneFromDBBySpecificKey("auth_identifier",userId, MetadataUser.class);
        if(userList.isEmpty()) return null;

        MetadataUser retrievedUser = userList.get(0);
        org.epos.eposdatamodel.User user1 = new org.epos.eposdatamodel.User(
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

    public static List<org.epos.eposdatamodel.User> retrieveAllUsers(){
        List<MetadataUser> userList = getDbaccess().getAllFromDB(MetadataUser.class);
        if(userList.isEmpty()) return null;

        List<org.epos.eposdatamodel.User> returnList = new ArrayList<>();
        for(MetadataUser user : userList){
            org.epos.eposdatamodel.User user1 = new org.epos.eposdatamodel.User(
                    user.getAuthIdentifier(),
                    user.getFamilyname(),
                    user.getGivenname(),
                    user.getEmail(),
                    Boolean.parseBoolean(user.getIsadmin())
            );

            List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);
            for(MetadataGroupUser groupUser : metadataGroupUserList){
                if(groupUser.getAuthIdentifier().getAuthIdentifier().equals(user.getAuthIdentifier())) {
                    UserGroup userGroup = new UserGroup(RoleType.valueOf(groupUser.getRole()),groupUser.getGroup().getId());
                    user1.getGroups().add(userGroup);
                }
            }
            returnList.add(user1);
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

    public static Boolean createGroup(org.epos.eposdatamodel.Group group){
        MetadataGroup group1 = new MetadataGroup();
        group1.setId(group.getId()==null || group.getId().isBlank()? UUID.randomUUID().toString() : group.getId());
        group1.setName(group.getName());
        group1.setDescription(group.getDescription());
        return getDbaccess().updateObject(group1);
    }

    public static Group retrieveGroup(Group group){
        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        if(metadataGroupList.isEmpty()) return null;
        for(MetadataGroup retrievedGroup : metadataGroupList){
            if(retrievedGroup.getId().equals(group.getId())){
                org.epos.eposdatamodel.Group group1 = new org.epos.eposdatamodel.Group(
                        retrievedGroup.getId(),
                        retrievedGroup.getName(),
                        retrievedGroup.getDescription()
                );
                List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);
                for(AuthorizationGroup authorizationGroup : authorizationGroupList){
                    group1.getEntities().add(authorizationGroup.getMeta().getMetaId());
                }
                List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);
                for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
                    group1.getUsers().add(metadataGroupUser.getAuthIdentifier().getAuthIdentifier());
                }
                return group1;
            }
        }
        return null;
    }

    public static Group retrieveGroupById(String groupId){
        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        if(metadataGroupList.isEmpty()) return null;
        for(MetadataGroup metadataGroup : metadataGroupList){
            if(metadataGroup.getId().equals(groupId)){
                org.epos.eposdatamodel.Group group1 = new org.epos.eposdatamodel.Group(
                        metadataGroup.getId(),
                        metadataGroup.getName(),
                        metadataGroup.getDescription()
                );
                List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);
                for(AuthorizationGroup authorizationGroup : authorizationGroupList){
                    group1.getEntities().add(authorizationGroup.getMeta().getMetaId());
                }
                List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);
                for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
                    group1.getUsers().add(metadataGroupUser.getAuthIdentifier().getAuthIdentifier());
                }
                return group1;
            }
        }
        return null;
    }

    public static List<Group> retrieveAllGroups(){
        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        if(metadataGroupList.isEmpty()) return null;

        List<Group> returnList = new ArrayList<>();
        for(MetadataGroup group : metadataGroupList){
            org.epos.eposdatamodel.Group group1 = new org.epos.eposdatamodel.Group(
                    group.getId(),
                    group.getName(),
                    group.getDescription()
            );

            List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);

            for(AuthorizationGroup authorizationGroup : authorizationGroupList){
                group1.getEntities().add(authorizationGroup.getMeta().getMetaId());
            }

            List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);

            for(MetadataGroupUser metadataGroupUser : metadataGroupUserList){
                group1.getUsers().add(metadataGroupUser.getAuthIdentifier().getAuthIdentifier());
            }

            returnList.add(group1);
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

        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        List<MetadataUser> userList = getDbaccess().getAllFromDB(MetadataUser.class);
        List<MetadataGroupUser> metadataGroupUserList = getDbaccess().getAllFromDB(MetadataGroupUser.class);

        if(metadataGroupList.isEmpty()) return null;
        if(userList.isEmpty()) return null;

        MetadataGroup selectedGroup = null;
        MetadataUser selectedUser = null;
        for(MetadataGroup retrievedGroup : metadataGroupList){
            if(retrievedGroup.getId().equals(groupId)) {
                selectedGroup = retrievedGroup;
            }
        }

        for(MetadataUser retrievedUser : userList){
            if(retrievedUser.getAuthIdentifier().equals(userId)){
                selectedUser = retrievedUser;
            }
        }

        if(selectedGroup != null && selectedUser != null){
            MetadataGroupUser metadataGroupUser = new MetadataGroupUser();
            metadataGroupUser.setId(UUID.randomUUID().toString());
            for(MetadataGroupUser metadataGroupUser1 : metadataGroupUserList){
                if(metadataGroupUser1.getGroup().getId().equals(selectedGroup.getId())
                        && metadataGroupUser1.getAuthIdentifier().getAuthIdentifier().equals(selectedUser.getAuthIdentifier())){
                    metadataGroupUser = metadataGroupUser1;
                }
            }
            metadataGroupUser.setGroup(selectedGroup);
            metadataGroupUser.setAuthIdentifier(selectedUser);
            metadataGroupUser.setRequestStatus(requestStatusType.name());
            metadataGroupUser.setRole(role.name());

            return getDbaccess().updateObject(metadataGroupUser);
        }
        return null;
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

        List<MetadataGroup> metadataGroupList = getDbaccess().getAllFromDB(MetadataGroup.class);
        if(metadataGroupList.isEmpty()) return null;

        List<EdmEntityId> edmEntityIdList = getDbaccess().getAllFromDB(EdmEntityId.class);
        if(edmEntityIdList.isEmpty()) return null;

        List<AuthorizationGroup> authorizationGroupList = getDbaccess().getAllFromDB(AuthorizationGroup.class);

        MetadataGroup selectedGroup = null;
        EdmEntityId selectedEdmEntityId = null;

        for(MetadataGroup metadataGroup : metadataGroupList){
            if(metadataGroup.getId().equals(groupId)){
                selectedGroup = metadataGroup;
            }
        }
        for(EdmEntityId edmEntityId : edmEntityIdList){
            if(edmEntityId.getMetaId().equals(metaId)){
                selectedEdmEntityId = edmEntityId;
            }
        }

        if(selectedEdmEntityId!=null && selectedGroup!=null) {
            AuthorizationGroup authorizationGroup = new AuthorizationGroup();
            authorizationGroup.setId(UUID.randomUUID().toString());
            for(AuthorizationGroup authorizationGroup1 : authorizationGroupList){
                if(authorizationGroup1.getGroup().getId().equals(groupId)
                        && authorizationGroup1.getMeta().getMetaId().equals(metaId)){
                    authorizationGroup = authorizationGroup1;
                }
            }
            authorizationGroup.setGroup(selectedGroup);
            authorizationGroup.setMeta(selectedEdmEntityId);

            return getDbaccess().updateObject(authorizationGroup);
        }
        else return false;
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

    private static EposDataModelDAO getDbaccess() {
        return new EposDataModelDAO();
    }


}
