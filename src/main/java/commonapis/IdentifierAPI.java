package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class IdentifierAPI extends AbstractAPI<org.epos.eposdatamodel.Identifier> {

    public IdentifierAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Identifier obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        String searchInstanceId = obj.getInstanceId();

        List<Identifier> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Identifier selectedEntity = returnList.get(0);

            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);

            for (Identifier item : returnList) {
                if (item.getVersion() != null &&
                        targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }

            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Identifier) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Identifier edmobj = new Identifier();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setType(Optional.ofNullable(obj.getType()).orElse(null));
        edmobj.setValue(Optional.ofNullable(obj.getIdentifier()).orElse(null));

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.IDENTIFIER.name(), edmobj);

        
            LinkedEntity result = new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(DataproductIdentifier.class)){
            DataproductIdentifier item = (DataproductIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(WebserviceIdentifier.class)){
            WebserviceIdentifier item = (WebserviceIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OrganizationIdentifier.class)){
            OrganizationIdentifier item = (OrganizationIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(PersonIdentifier.class)){
            PersonIdentifier item = (PersonIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        List<Identifier> identifierList = getDbaccess().getAllFromDB(Identifier.class);
        identifierList.stream()
                .filter(item -> item.getInstanceId().equals(instanceId))
                .forEach(item -> EposDataModelDAO.getInstance().deleteObject(item));

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Identifier retrieve(String instanceId) {
        List<Identifier> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Identifier.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Identifier edmobj = elementList.get(0);
        org.epos.eposdatamodel.Identifier o = new org.epos.eposdatamodel.Identifier();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setIdentifier(edmobj.getValue());

        return (org.epos.eposdatamodel.Identifier) VersioningStatusAPI.retrieveVersion(o);
    }
    @Override
    public org.epos.eposdatamodel.Identifier retrieveByUID(String uid) {
        List<Identifier> returnList = getDbaccess().getOneFromDBByUID(uid, Identifier.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }
    @Override
    public List<org.epos.eposdatamodel.Identifier> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Identifier.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Identifier> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Identifier.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Identifier> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Identifier.class, status));
    }

    private List<org.epos.eposdatamodel.Identifier> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);
        return dbEntities.parallelStream().map(item -> retrieve(item)).collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Identifier> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Identifier.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Identifier edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.IDENTIFIER.name());

        return o;
    }
}