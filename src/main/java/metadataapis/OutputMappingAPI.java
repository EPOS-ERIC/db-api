package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutputMappingAPI extends AbstractAPI<org.epos.eposdatamodel.OutputMapping> {

    public OutputMappingAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.OutputMapping obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        List<OutputMapping> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.OutputMapping) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        OutputMapping edmobj = new OutputMapping();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setLabel(obj.getOutputLabel());
        edmobj.setValuepattern(obj.getOutputValuePattern());
        edmobj.setRequired(obj.getOutputRequired()!=null? Boolean.parseBoolean(obj.getOutputRequired()) : null);
        edmobj.setRange(obj.getOutputRange());
        edmobj.setProperty(obj.getOutputProperty());
        edmobj.setVariable(obj.getOutputVariable());

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
            if(item.getOutputMappingInstance().getInstanceId().equals(instanceId)){
                EposDataModelDAO.getInstance().deleteObject(item);
            }
        }
        List<OutputMapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, OutputMapping.class);
        for(OutputMapping object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.OutputMapping retrieve(String instanceId) {
        List<OutputMapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, OutputMapping.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            OutputMapping edmobj = elementList.get(0);
            org.epos.eposdatamodel.OutputMapping o = new org.epos.eposdatamodel.OutputMapping();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setOutputLabel(edmobj.getLabel());
            o.setOutputValuePattern(edmobj.getValuepattern());
            o.setOutputRequired(edmobj.getRequired()!=null? Boolean.toString(edmobj.getRequired()) : null);
            o.setOutputRange(edmobj.getRange());
            o.setOutputProperty(edmobj.getProperty());
            o.setOutputVariable(edmobj.getVariable());

            o = (org.epos.eposdatamodel.OutputMapping) VersioningStatusAPI.retrieveVersion(o);

            return o;

    }
    @Override
    public org.epos.eposdatamodel.OutputMapping retrieveByUID(String uid) {
        List<OutputMapping> returnList = getDbaccess().getOneFromDBByUID(uid, OutputMapping.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }
    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, OutputMapping.class));
    }
    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(OutputMapping.class));
    }
    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(OutputMapping.class, status));
    }

    private List<org.epos.eposdatamodel.OutputMapping> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }


    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<OutputMapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, OutputMapping.class);
        if(elementList!=null && !elementList.isEmpty()) {
            OutputMapping edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.OUTPUTMAPPING.name());

            return o;
        }
        return null;
    }

}
