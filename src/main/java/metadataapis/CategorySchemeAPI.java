package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.LinkedEntityAPI;
import commonapis.VersioningStatusAPI;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

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

        if (Objects.nonNull(obj.getTopConcepts())) createTopConcepts(obj.getTopConcepts(), edmobj, overrideStatus);


        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void createTopConcepts(List<LinkedEntity> topConcepts, CategoryScheme edmobj, StatusType overrideStatus){
        for(LinkedEntity topConcept : topConcepts) {

            LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(topConcept, overrideStatus);

            CategoryHastopconcept categoryHastopconcept = new CategoryHastopconcept();
            categoryHastopconcept.setCategorySchemeInstance(edmobj);
            categoryHastopconcept.setCategoryInstance((Category) getDbaccess().getOneFromDBByInstanceId(le.getInstanceId(), Category.class).get(0));

            getDbaccess().updateObject(categoryHastopconcept);
        }
    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(CategoryHastopconcept.class)){
            CategoryHastopconcept item = (CategoryHastopconcept) object;
            if(item.getCategorySchemeInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        for(CategoryScheme object : elementList){
            dbaccess.deleteObject(object);
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.CategoryScheme retrieve(String instanceId) {

        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
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

            ArrayList<LinkedEntity> topConcepts = new ArrayList<>();

            for(Object categoryHastopconcept : dbaccess.getAllFromDB(CategoryHastopconcept.class)){
                CategoryHastopconcept item = (CategoryHastopconcept) categoryHastopconcept;
                if(item.getCategorySchemeInstance().getInstanceId().equals(edmobj.getInstanceId())){
                    topConcepts.add(retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
                }
            }

            o = (org.epos.eposdatamodel.CategoryScheme) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }
    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, CategoryScheme.class));
    }
    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(CategoryScheme.class));
    }
    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(CategoryScheme.class, status));
    }

    private List<org.epos.eposdatamodel.CategoryScheme> retrieveEntities(Function<Void, List<CategoryScheme>> dbFetcher) {
        List<CategoryScheme> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
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
