package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.util.List;
import java.util.Map;
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
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Identifier selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Identifier::getVersion);

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
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Identifier.class, Map.of(
                DataproductIdentifier.class, "identifierInstance",
                WebserviceIdentifier.class, "identifierInstance",
                OrganizationIdentifier.class, "identifierInstance",
                PersonIdentifier.class, "identifierInstance"));
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
        return retrieveBulk(dbEntities, Identifier.class, entity -> {
            org.epos.eposdatamodel.Identifier dto = new org.epos.eposdatamodel.Identifier();
            dto.setInstanceId(entity.getInstanceId());
            dto.setMetaId(entity.getMetaId());
            dto.setUid(entity.getUid());
            dto.setType(entity.getType());
            dto.setIdentifier(entity.getValue());
            return dto;
        });
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
