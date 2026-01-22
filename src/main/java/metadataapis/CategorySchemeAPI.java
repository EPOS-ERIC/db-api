package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CategorySchemeAPI extends AbstractAPI<org.epos.eposdatamodel.CategoryScheme> {

    public CategorySchemeAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.CategoryScheme obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<CategoryScheme> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            CategoryScheme selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (CategoryScheme item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            obj.setVersionId(selectedEntity.getVersion().getVersionId());
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

        if (Objects.nonNull(obj.getTopConcepts())) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getTopConcepts(), relationFromUpdate, relationToUpdate,
                    CategoryHastopconcept.class, Category.class,
                    "categorySchemeInstance",
                    CategoryHastopconcept::getCategoryInstance,
                    CategoryHastopconcept::setCategorySchemeInstance,
                    CategoryHastopconcept::setCategoryInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public Boolean delete(String instanceId) {
        List<Object> rels = getDbaccess().getOneFromDBBySpecificKey("categorySchemeInstance", instanceId, CategoryHastopconcept.class);
        if(rels != null) rels.forEach(EposDataModelDAO.getInstance()::deleteObject);

        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        for(CategoryScheme object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.CategoryScheme retrieve(String instanceId) {
        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        if (elementList == null || elementList.isEmpty()) return null;

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

        for(Object obj : getDbaccess().getOneFromDBBySpecificKey("categorySchemeInstance", edmobj.getInstanceId(),CategoryHastopconcept.class)){
            CategoryHastopconcept item = (CategoryHastopconcept) obj;
            o.addTopConcepts(AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
        }

        o = (org.epos.eposdatamodel.CategoryScheme) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }
    @Override
    public org.epos.eposdatamodel.CategoryScheme retrieveByUID(String uid) {
        List<CategoryScheme> returnList = getDbaccess().getOneFromDBByUID(uid, CategoryScheme.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, CategoryScheme.class));
    }
    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(CategoryScheme.class));
    }
    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(CategoryScheme.class, status));
    }
    private List<org.epos.eposdatamodel.CategoryScheme> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
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