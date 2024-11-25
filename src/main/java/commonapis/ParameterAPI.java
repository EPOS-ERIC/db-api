package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplicationParameter;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ParameterAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareApplicationParameter> {

    public ParameterAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareApplicationParameter obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Parameter> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.SoftwareApplicationParameter) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Parameter edmobj = new Parameter();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setEncodingformat(obj.getEncodingformat());
        edmobj.setConformsto(obj.getConformsto());
        edmobj.setAction(obj.getAction());

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public org.epos.eposdatamodel.SoftwareApplicationParameter retrieve(String instanceId) {
        List<Parameter> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Parameter.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Parameter edmobj = elementList.get(0);
            org.epos.eposdatamodel.SoftwareApplicationParameter o = new org.epos.eposdatamodel.SoftwareApplicationParameter();

            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEncodingformat(edmobj.getEncodingformat());
            o.setConformsto(edmobj.getConformsto());
            o.setAction(edmobj.getAction());

            o = (org.epos.eposdatamodel.SoftwareApplicationParameter) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public Boolean delete(String instanceId) {
        return true;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveBunch(List<String> entities) {
        List<Parameter> list = getDbaccess().getListFromDBByInstanceId(entities, Parameter.class);
        List<org.epos.eposdatamodel.SoftwareApplicationParameter> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveAll() {
        List<Parameter> list = getDbaccess().getAllFromDB(Parameter.class);
        List<org.epos.eposdatamodel.SoftwareApplicationParameter> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Parameter> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Parameter.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Parameter edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.PARAMETER.name());

            return o;
        }
        return null;
    }


}
