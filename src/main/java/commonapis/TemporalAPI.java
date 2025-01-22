package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.PeriodOfTime;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class TemporalAPI extends AbstractAPI<org.epos.eposdatamodel.PeriodOfTime> {

    public TemporalAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(PeriodOfTime obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Fetch existing record if it already exists
        List<Temporal> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                getEdmClass());

        // Update if the record exists
        if (!returnList.isEmpty()) {
            Temporal existing = returnList.get(0);
            obj.setInstanceId(existing.getInstanceId());
            obj.setMetaId(existing.getMetaId());
            obj.setUid(existing.getUid());
            obj.setVersionId(existing.getVersion().getVersionId());
        }

        // Versioning and entity creation
        obj = (org.epos.eposdatamodel.PeriodOfTime) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Temporal edmobj = new Temporal();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
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
        // Use streams to batch delete related temporal entities
        List<Object> relatedItems = (List<Object>) getDbaccess().getAllFromDB(DataproductTemporal.class).stream()
                .filter(item -> ((DataproductTemporal) item).getTemporalInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(WebserviceTemporal.class).stream()
                .filter(item -> ((WebserviceTemporal) item).getTemporalInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(EquipmentTemporal.class).stream()
                .filter(item -> ((EquipmentTemporal) item).getTemporalInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(ServiceTemporal.class).stream()
                .filter(item -> ((ServiceTemporal) item).getTemporalInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        // Delete the main Temporal record
        List<Temporal> temporalItems = (List<Temporal>) getDbaccess().getAllFromDB(Temporal.class).stream()
                .filter(item -> ((Temporal)item).getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(temporalItems);

        return true;
    }

    @Override
    public org.epos.eposdatamodel.PeriodOfTime retrieve(String instanceId) {
        List<Temporal> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Temporal.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Temporal edmobj = elementList.get(0);
        org.epos.eposdatamodel.PeriodOfTime o = new org.epos.eposdatamodel.PeriodOfTime();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setStartDate(edmobj.getStartdate());
        o.setEndDate(edmobj.getEnddate());

        return (org.epos.eposdatamodel.PeriodOfTime) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveBunch(List<String> entities) {
        List<Temporal> list = getDbaccess().getListFromDBByInstanceId(entities, Temporal.class);

        // Using stream for efficient processing
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAll() {
        List<Temporal> list = getDbaccess().getAllFromDB(Temporal.class);

        // Using stream for efficient processing
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAllWithStatus(StatusType status) {
        List<Temporal> list = getDbaccess().getAllFromDBWithStatus(Temporal.class, status);

        // Using stream for efficient processing
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Temporal> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Temporal.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Temporal edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.PERIODOFTIME.name());

        return o;
    }
}
