package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationChecker;
import relationsapi.RelationSyncUtil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class PayloadAPI extends AbstractAPI<org.epos.eposdatamodel.Payload> {

    public PayloadAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Payload obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Payload> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if (!returnList.isEmpty()) {
            Payload selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Payload item : returnList) {
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

        obj = (org.epos.eposdatamodel.Payload) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Payload edmobj = new Payload();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));

        // OUTPUT MAPPING
        if (obj.getOutputMapping() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getOutputMapping(), relationFromUpdate, relationToUpdate,
                    PayloadOutputMapping.class, OutputMapping.class,
                    "payloadInstance", PayloadOutputMapping::getOutputMappingInstance, PayloadOutputMapping::setPayloadInstance, PayloadOutputMapping::setOutputMappingInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // SUPPORTED OPERATION
        if (obj.getSupportedOperation() != null) {
            // Treat single operation as a list of 1 for sync utility or keep simplified if 1-to-1/1-to-many from other side
            // Payload -> Operation seems to be Many-to-One or One-to-One? In model, getSupportedOperation returns LinkedEntity (singular).
            // But OperationPayload table suggests link.
            // If singular, we handle it directly.
            Operation operation = (Operation) RelationChecker.checkRelation(obj, previousObj, null, obj.getSupportedOperation(), overrideStatus, Operation.class, true);
            if(operation != null){
                // Assuming singular link, we should clear previous or check if update needed
                // For simplicity/safety with current DB structure:
                List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("payloadInstance", edmobj.getInstanceId(), OperationPayload.class);
                if(existing != null) existing.forEach(EposDataModelDAO.getInstance()::deleteObject);

                OperationPayload pi = new OperationPayload();
                pi.setPayloadInstance(edmobj);
                pi.setOperationInstance(operation);
                EposDataModelDAO.getInstance().updateObject(pi);
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
        deleteRelations("payloadInstance", instanceId, PayloadOutputMapping.class);
        deleteRelations("payloadInstance", instanceId, OperationPayload.class);

        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        for (Payload object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Payload retrieve(String instanceId) {
        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Payload edmobj = elementList.get(0);
        org.epos.eposdatamodel.Payload o = new org.epos.eposdatamodel.Payload();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("payloadInstance", edmobj.getInstanceId(), OperationPayload.class)) {
            OperationPayload item = (OperationPayload) object;
            LinkedEntity le = retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
            o.setSupportedOperation(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("payloadInstance", edmobj.getInstanceId(), PayloadOutputMapping.class)) {
            PayloadOutputMapping item = (PayloadOutputMapping) object;
            LinkedEntity le = retrieveAPI(EntityNames.OUTPUTMAPPING.name()).retrieveLinkedEntity(item.getOutputMappingInstance().getInstanceId());
            o.addOutputMapping(le);
        }

        o = (org.epos.eposdatamodel.Payload) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Payload retrieveByUID(String uid) {
        List<Payload> returnList = getDbaccess().getOneFromDBByUID(uid, Payload.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Payload.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Payload.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Payload.class, status));
    }
    private List<org.epos.eposdatamodel.Payload> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        if (elementList != null && !elementList.isEmpty()) {
            Payload edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.PAYLOAD.name());
            return o;
        }
        return null;
    }
}