package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.LinkedEntityAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AttributionAPI extends AbstractAPI<org.epos.eposdatamodel.Attribution> {

    public AttributionAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Attribution obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Attribution> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Attribution) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Attribution edmobj = new Attribution();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));

        if (obj.getAgent() != null) {
            LinkedEntity le = LinkedEntityAPI.createFromLinkedEntity(obj.getAgent(), overrideStatus, edmobj.getVersion(), obj.getFileProvenance());
            List<Organization> organizationList = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Organization.class);
            if(!organizationList.isEmpty()) {
                edmobj.setAgentId(organizationList.get(0).getInstanceId());
                edmobj.setAgentType(obj.getAgent().getEntityType());
            }
        }

        if(obj.getRole()!=null) {

            for(Object organizationContactpoint : getDbaccess().getOneFromDBBySpecificKey("attributionInstance", edmobj.getInstanceId(),AttributionRole.class)){
                AttributionRole item = (AttributionRole) organizationContactpoint;
                EposDataModelDAO.getInstance().deleteObject(item);
            }
            for(String role : obj.getRole()) {
                AttributionRole roleobj = new AttributionRole();
                roleobj.setAttributionInstance(edmobj);
                roleobj.setInstanceId(UUID.randomUUID().toString());
                roleobj.setRoletype(role);
                roleobj.setMetaId(UUID.randomUUID().toString());//TODO: fix version
                if(edmobj.getVersion().getEditorId()!=null) roleobj.setEditorId(edmobj.getVersion().getEditorId());
                if(edmobj.getVersion().getProvenance()!=null) roleobj.setFileProvenance(edmobj.getVersion().getProvenance());
                if(edmobj.getVersion().getChangeComment()!=null) roleobj.setChangeComment(edmobj.getVersion().getChangeComment());
                if(edmobj.getVersion().getChangeTimestamp()!=null) roleobj.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

                EposDataModelDAO.getInstance().updateObject(roleobj);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    @Override
    public Boolean delete(String instanceId) {

        List<Attribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Attribution.class);
        for(Attribution object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.Attribution retrieve(String instanceId) {
        List<Attribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Attribution.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
        Attribution edmobj = elementList.get(0);

        org.epos.eposdatamodel.Attribution o = new org.epos.eposdatamodel.Attribution();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());

        for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("attributionInstance", edmobj.getInstanceId(),AttributionRole.class)) {
            AttributionRole item = (AttributionRole) object;
            o.addRole(item.getRoletype());
        }

        if(edmobj.getAgentId()!=null && edmobj.getAgentType()!=null) {
            o.setAgent(AbstractAPI.retrieveAPI(edmobj.getAgentType()).retrieveLinkedEntity(edmobj.getAgentId()));
        }

        return o;
    }

    @Override
    public org.epos.eposdatamodel.Attribution retrieveByUID(String uid) {
        List<Attribution> returnList = getDbaccess().getOneFromDBByUID(uid, Attribution.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.Attribution> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Attribution.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Attribution> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Attribution.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Attribution> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Attribution.class, status));
    }

    private List<org.epos.eposdatamodel.Attribution> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Attribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Attribution.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Attribution edmobj = elementList.get(0);

            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.ATTRIBUTION.name());

            return o;
        }
        return null;
    }

}
