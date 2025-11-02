package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationChecker;

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

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        List<Payload> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Payload) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Payload edmobj = new Payload();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        //edmobj.setName(obj.get());
        //edmobj.setTemplate(obj.getTemplate());

        if (obj.getOutputMapping() != null) {
            if(relationFromUpdate!=null && obj.getOutputMapping().contains(relationFromUpdate)){
                obj.getOutputMapping().remove(relationFromUpdate);
                obj.getOutputMapping().add(relationToUpdate);
            }
            for(LinkedEntity outputMapping : obj.getOutputMapping()){
                OutputMapping mapping1 = (OutputMapping) RelationChecker.checkRelation(obj, previousObj, null, outputMapping, overrideStatus, OutputMapping.class, false);
                if(mapping1!=null){
                    PayloadOutputMapping pi = new PayloadOutputMapping();
                    pi.setPayloadInstance(edmobj);
                    pi.setOutputMappingInstance(mapping1);
                    EposDataModelDAO.getInstance().updateObject(pi);
                }
            }
        }

        if (obj.getSupportedOperation() != null) {
            Operation operation = (Operation) RelationChecker.checkRelation(obj, previousObj, null, obj.getSupportedOperation(), overrideStatus, Operation.class, true);
            if(operation!=null){
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
        for(Object object : getDbaccess().getAllFromDB(PayloadOutputMapping.class)){
            PayloadOutputMapping item = (PayloadOutputMapping) object;
            if(item.getPayloadInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(OperationPayload.class)){
            OperationPayload item = (OperationPayload) object;
            if(item.getPayloadInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        for(Payload object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }


    @Override
    public org.epos.eposdatamodel.Payload retrieve(String instanceId) {
        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Payload edmobj = elementList.get(0);
            org.epos.eposdatamodel.Payload o = new org.epos.eposdatamodel.Payload();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("payloadInstance", edmobj.getInstanceId(),OperationPayload.class)) {
                OperationPayload item = (OperationPayload) object;
                //if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
                    o.setSupportedOperation(le);
                //}
            }

            for (Object object : EposDataModelDAO.getInstance().getOneFromDBBySpecificKey("payloadInstance", edmobj.getInstanceId(),PayloadOutputMapping.class)) {
                PayloadOutputMapping item = (PayloadOutputMapping) object;
                //if(item.getOperationInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                LinkedEntity le = retrieveAPI(EntityNames.OUTPUTMAPPING.name()).retrieveLinkedEntity(item.getOutputMappingInstance().getInstanceId());
                o.addOutputMapping(le);
                //}
            }

            o = (org.epos.eposdatamodel.Payload) VersioningStatusAPI.retrieveVersion(o);

            return o;
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
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        if(elementList!=null && !elementList.isEmpty()) {
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
