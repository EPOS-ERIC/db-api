package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareApplicationParameter;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ParameterAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareApplicationParameter> {

    public ParameterAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareApplicationParameter obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Fetch the parameter from DB if it already exists
        List<Parameter> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                Parameter.class);

        // Update if it already exists
        if (!returnList.isEmpty()) {
            Parameter existing = returnList.get(0);
            obj.setInstanceId(existing.getInstanceId());
            obj.setMetaId(existing.getMetaId());
            obj.setUid(existing.getUid());
            obj.setVersionId(existing.getVersion().getVersionId());
        }

        // Check version and ensure versioning status
        obj = (org.epos.eposdatamodel.SoftwareApplicationParameter) VersioningStatusAPI.checkVersion(obj, overrideStatus);
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

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
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
    public Boolean delete(String instanceId) {
        // Batch delete for SoftwareApplicationParameter and Parameter
        List<SoftwareapplicationParameter> parameterItemsToDelete = (List<SoftwareapplicationParameter>) getDbaccess().getAllFromDB(SoftwareapplicationParameter.class).stream()
                .filter(item -> ((SoftwareapplicationParameter) item).getParameterInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(parameterItemsToDelete);

        List<Parameter> parameterListToDelete = (List<Parameter>) getDbaccess().getAllFromDB(Parameter.class).stream()
                .filter(item -> ((Parameter)item).getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(parameterListToDelete);

        return true;
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveBunch(List<String> entities) {
        List<Parameter> list = getDbaccess().getListFromDBByInstanceId(entities, Parameter.class);

        // Using streams for batch processing
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveAll() {
        List<Parameter> list = getDbaccess().getAllFromDB(Parameter.class);

        // Using streams for batch processing
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.SoftwareApplicationParameter> retrieveAllWithStatus(StatusType status) {
        List<Parameter> list = getDbaccess().getAllFromDBWithStatus(Parameter.class, status);

        // Using streams for batch processing
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
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
