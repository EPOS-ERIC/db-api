package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.LinkedEntityAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CategoryAPI extends AbstractAPI<org.epos.eposdatamodel.Category> {

    public CategoryAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Category obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Category> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Category) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Category edmobj = new Category();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setName(Optional.ofNullable(obj.getName()).orElse(""));
        edmobj.setDescription(Optional.ofNullable(obj.getDescription()).orElse(""));

        if (Objects.nonNull(obj.getInScheme())) createInscheme(obj.getInScheme(), edmobj, overrideStatus);
        if (Objects.nonNull(obj.getBroader())) createBroaders(obj.getBroader(), edmobj, overrideStatus);
        if (Objects.nonNull(obj.getNarrower())) createNarrowers(obj.getNarrower(), edmobj, overrideStatus);

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInscheme(LinkedEntity inscheme, Category edmobj, StatusType overrideStatus){

        List<CategoryScheme> categorySchemeList = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(inscheme,CategoryScheme.class);
        if(!categorySchemeList.isEmpty()) {
            edmobj.setInScheme(categorySchemeList.get(0));
        } else{
            org.epos.eposdatamodel.CategoryScheme childObj = new org.epos.eposdatamodel.CategoryScheme();
            childObj.setInstanceId(inscheme.getInstanceId());
            childObj.setMetaId(inscheme.getMetaId());
            childObj.setUid(inscheme.getUid());
            childObj.setStatus(StatusType.valueOf(edmobj.getVersion().getStatus()));
            if(edmobj.getVersion().getEditorId()!=null) childObj.setEditorId(edmobj.getVersion().getEditorId());
            if(edmobj.getVersion().getProvenance()!=null) childObj.setFileProvenance(edmobj.getVersion().getProvenance());
            if(edmobj.getVersion().getChangeComment()!=null) childObj.setChangeComment(edmobj.getVersion().getChangeComment());
            if(edmobj.getVersion().getChangeTimestamp()!=null) childObj.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());
            LinkedEntity le = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(childObj, overrideStatus, null, null);
            edmobj.setInScheme((CategoryScheme) getDbaccess().getOneFromDBByLinkedEntity(le, CategoryScheme.class).get(0));
        }
    }

    private void createBroaders(List<LinkedEntity> broaders, Category edmobj, StatusType overrideStatus){
        for(LinkedEntity broader : broaders) {

            LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(broader, overrideStatus, edmobj.getVersion());

            CategoryIspartof categoryIspartof = new CategoryIspartof();
            categoryIspartof.setCategory1Instance((Category) getDbaccess().getOneFromDBByInstanceId(le.getInstanceId(), Category.class).get(0));
            categoryIspartof.setCategory2Instance(edmobj);

            getDbaccess().updateObject(categoryIspartof);
        }
    }

    private void createNarrowers(List<LinkedEntity> narrowers, Category edmobj, StatusType overrideStatus){
        for(LinkedEntity narrower : narrowers) {

            LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(narrower, overrideStatus, edmobj.getVersion());

            CategoryIspartof categoryIspartof = new CategoryIspartof();
            categoryIspartof.setCategory1Instance(edmobj);
            categoryIspartof.setCategory2Instance((Category) getDbaccess().getOneFromDBByInstanceId(le.getInstanceId(), Category.class).get(0));

            getDbaccess().updateObject(categoryIspartof);
        }
    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(CategoryHastopconcept.class)){
            CategoryHastopconcept item = (CategoryHastopconcept) object;
            if(item.getCategoryInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(CategoryIspartof.class)){
            CategoryIspartof item = (CategoryIspartof) object;
            if(item.getCategory1Instance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(DataproductCategory.class)){
            DataproductCategory item = (DataproductCategory) object;
            if(item.getCategoryInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceCategory.class)){
            WebserviceCategory item = (WebserviceCategory) object;
            if(item.getCategoryInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwareapplicationCategory.class)){
            SoftwareapplicationCategory item = (SoftwareapplicationCategory) object;
            if(item.getCategoryInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(SoftwaresourcecodeCategory.class)){
            SoftwaresourcecodeCategory item = (SoftwaresourcecodeCategory) object;
            if(item.getCategoryInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilityCategory.class)){
            FacilityCategory item = (FacilityCategory) object;
            if(item.getCategoryInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentCategory.class)){
            EquipmentCategory item = (EquipmentCategory) object;
            if(item.getCategoryInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        for(Category object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.Category retrieve(String instanceId) {
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Category edmobj = elementList.get(0);

            org.epos.eposdatamodel.Category o = new org.epos.eposdatamodel.Category();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setName(edmobj.getName());
            o.setDescription(edmobj.getDescription());
            if (edmobj.getInScheme() != null) {
                o.setInScheme(retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveLinkedEntity(edmobj.getInScheme().getInstanceId()));
            }

            ArrayList<LinkedEntity> broaders = new ArrayList<>();
            ArrayList<LinkedEntity> narrowers = new ArrayList<>();

        for(Object obj : getDbaccess().getOneFromDBBySpecificKey("category1Instance", edmobj.getInstanceId(),CategoryIspartof.class)){
            CategoryIspartof item = (CategoryIspartof) obj;
            narrowers.add(retrieveLinkedEntity(item.getCategory2Instance().getInstanceId()));
        }

        for(Object obj : getDbaccess().getOneFromDBBySpecificKey("category2Instance", edmobj.getInstanceId(),CategoryIspartof.class)){
            CategoryIspartof item = (CategoryIspartof) obj;
            broaders.add(retrieveLinkedEntity(item.getCategory1Instance().getInstanceId()));
        }

            o.setBroader(broaders);
            o.setNarrower(narrowers);
            o = (org.epos.eposdatamodel.Category) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }
    @Override
    public List<org.epos.eposdatamodel.Category> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Category.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Category> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Category.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Category> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Category.class, status));
    }

    private List<org.epos.eposdatamodel.Category> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Category edmobj = elementList.get(0);

            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.CATEGORY.name());

            return o;
        }
        return null;
    }

}
