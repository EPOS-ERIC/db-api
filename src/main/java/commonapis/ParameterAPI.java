package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplicationParameter;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParameterAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareApplicationParameter> {

    public ParameterAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareApplicationParameter obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        String searchInstanceId = obj.getInstanceId();

        List<Parameter> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                Parameter.class);

        if(!returnList.isEmpty()){
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Parameter selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Parameter::getVersion);

            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.SoftwareApplicationParameter) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        // Create the new parameter entity
        Parameter edmobj = new Parameter();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setEncodingformat(obj.getEncodingformat());
        edmobj.setConformsto(obj.getConformsto());
        edmobj.setAction(obj.getAction());

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name(), edmobj);

        
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
    public org.epos.eposdatamodel.SoftwareApplicationParameter retrieve(String instanceId) {
        List<Parameter> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Parameter.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Parameter edmobj = elementList.get(0);
        org.epos.eposdatamodel.SoftwareApplicationParameter o = new org.epos.eposdatamodel.SoftwareApplicationParameter();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEncodingformat(edmobj.getEncodingformat());
        o.setConformsto(edmobj.getConformsto());
        o.setAction(edmobj.getAction());

        return (org.epos.eposdatamodel.SoftwareApplicationParameter) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public org.epos.eposdatamodel.SoftwareApplicationParameter retrieveByUID(String uid) {
        List<Parameter> returnList = getDbaccess().getOneFromDBByUID(uid, Parameter.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }
    @Override
    public Boolean delete(String instanceId) {
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Parameter.class,
                java.util.Map.of(SoftwareapplicationParameter.class, "parameterInstance"));
    }


    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Parameter.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Parameter.class));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Parameter.class, status));
    }

    private List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);
        return retrieveBulk(dbEntities, Parameter.class, entity -> {
            org.epos.eposdatamodel.SoftwareApplicationParameter dto =
                    new org.epos.eposdatamodel.SoftwareApplicationParameter();
            dto.setInstanceId(entity.getInstanceId());
            dto.setMetaId(entity.getMetaId());
            dto.setUid(entity.getUid());
            dto.setEncodingformat(entity.getEncodingformat());
            dto.setConformsto(entity.getConformsto());
            dto.setAction(entity.getAction());
            return dto;
        });
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Parameter> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Parameter.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Parameter edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(edmobj.getAction().equals("OBJECT")
                ? EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name()
                : EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name());

        return o;
    }
}
