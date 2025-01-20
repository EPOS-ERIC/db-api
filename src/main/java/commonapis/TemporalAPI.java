package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.PeriodOfTime;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class TemporalAPI extends AbstractAPI<org.epos.eposdatamodel.PeriodOfTime> {

    public TemporalAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(PeriodOfTime obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Temporal> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.PeriodOfTime) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Temporal edmobj = new Temporal();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setStartdate(obj.getStartDate());
        edmobj.setEnddate(obj.getEndDate());

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(DataproductTemporal.class)){
            DataproductTemporal item = (DataproductTemporal) object;
            if(item.getTemporalInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(WebserviceTemporal.class)){
            WebserviceTemporal item = (WebserviceTemporal) object;
            if(item.getTemporalInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(EquipmentTemporal.class)){
            EquipmentTemporal item = (EquipmentTemporal) object;
            if(item.getTemporalInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(ServiceTemporal.class)){
            ServiceTemporal item = (ServiceTemporal) object;
            if(item.getTemporalInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(Temporal.class)){
            Temporal item = (Temporal) object;
            if(item.getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.PeriodOfTime retrieve(String instanceId) {
        List<Temporal> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Temporal.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Temporal edmobj = elementList.get(0);
            org.epos.eposdatamodel.PeriodOfTime o = new org.epos.eposdatamodel.PeriodOfTime();

            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setStartDate(edmobj.getStartdate());
            o.setEndDate(edmobj.getEnddate());

            o = (org.epos.eposdatamodel.PeriodOfTime) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveBunch(List<String> entities) {
        List<Temporal> list = getDbaccess().getListFromDBByInstanceId(entities, Temporal.class);
        List<org.epos.eposdatamodel.PeriodOfTime> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAll() {
        List<Temporal> list = getDbaccess().getAllFromDB(Temporal.class);
        List<org.epos.eposdatamodel.PeriodOfTime> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAllWithStatus(StatusType status) {
        List<Temporal> list = getDbaccess().getAllFromDBWithStatus(Temporal.class, status);
        List<org.epos.eposdatamodel.PeriodOfTime> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Temporal> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Temporal.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Temporal edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.PERIODOFTIME.name());

            return o;
        }
        return null;
    }


}
