package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CategorySchemeAPI extends AbstractAPI<org.epos.eposdatamodel.CategoryScheme> {

    public CategorySchemeAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.CategoryScheme obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<CategoryScheme> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                getEdmClass());

        if(!returnList.isEmpty()){
            obj.setInstanceId(returnList.get(0).getInstanceId());
            obj.setMetaId(returnList.get(0).getMetaId());
            obj.setUid(returnList.get(0).getUid());
            obj.setVersionId(returnList.get(0).getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.CategoryScheme) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        CategoryScheme edmobj = new CategoryScheme();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setName(Optional.ofNullable(obj.getTitle()).orElse(""));
        edmobj.setDescription(Optional.ofNullable(obj.getDescription()).orElse(""));
        edmobj.setCode(Optional.ofNullable(obj.getCode()).orElse(""));
        edmobj.setColor(Optional.ofNullable(obj.getColor()).orElse(""));
        edmobj.setHomepage(Optional.ofNullable(obj.getHomepage()).orElse(""));
        edmobj.setOrderitemnumber(Optional.ofNullable(obj.getOrderitemnumber()).orElse(""));
        edmobj.setLogo(Optional.ofNullable(obj.getLogo()).orElse(""));

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public Boolean delete(String instanceId) {
        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        for(CategoryScheme object : elementList){
            dbaccess.deleteObject(object);
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.CategoryScheme retrieve(String instanceId) {

        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        if(elementList!=null && !elementList.isEmpty()) {
            CategoryScheme edmobj = elementList.get(0);
            org.epos.eposdatamodel.CategoryScheme o = new org.epos.eposdatamodel.CategoryScheme();


            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setTitle(edmobj.getName());
            o.setDescription(edmobj.getDescription());
            o.setCode(edmobj.getCode());
            o.setHomepage(edmobj.getHomepage());
            o.setLogo(edmobj.getLogo());
            o.setColor(edmobj.getColor());
            o.setOrderitemnumber(edmobj.getOrderitemnumber());

            o = (org.epos.eposdatamodel.CategoryScheme) VersioningStatusAPI.retrieveVersion(o);
            o.setGroups(UserGroupManagementAPI.retrieveShortGroupsFromMetaId(edmobj.getMetaId()));

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveBunch(List<String> entities) {
        List<CategoryScheme> list = getDbaccess().getListFromDBByInstanceId(entities, CategoryScheme.class);
        List<org.epos.eposdatamodel.CategoryScheme> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAll() {
        List<CategoryScheme> list = getDbaccess().getAllFromDB(CategoryScheme.class);
        List<org.epos.eposdatamodel.CategoryScheme> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAllWithStatus(StatusType status) {
        List<CategoryScheme> list = getDbaccess().getAllFromDBWithStatus(CategoryScheme.class, status);
        List<org.epos.eposdatamodel.CategoryScheme> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        if(elementList!=null && !elementList.isEmpty()) {
            CategoryScheme edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.CATEGORYSCHEME.name());

            return o;
        }
        return null;
    }
}
