package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.QuantitativeValue;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class QuantitativeValueAPI extends AbstractAPI<org.epos.eposdatamodel.QuantitativeValue> {

    public QuantitativeValueAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(QuantitativeValue obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Fetch existing record if exists
        List<Quantitativevalue> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                getEdmClass());

        // If record exists, update the entity with the existing details
        if (!returnList.isEmpty()) {
            Quantitativevalue existing = returnList.get(0);
            obj.setInstanceId(existing.getInstanceId());
            obj.setMetaId(existing.getMetaId());
            obj.setUid(existing.getUid());
            obj.setVersionId(existing.getVersion().getVersionId());
        }

        // Check version and handle versioning status
        obj = (org.epos.eposdatamodel.QuantitativeValue) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        // Create a new Quantitativevalue entity
        Quantitativevalue edmobj = new Quantitativevalue();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setUnitcode(obj.getUnit());
        edmobj.setValue(obj.getValue());

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public Boolean delete(String instanceId) {
        // Batch delete Quantitativevalue entities
        List<Quantitativevalue> itemsToDelete = (List<Quantitativevalue>) getDbaccess().getAllFromDB(Quantitativevalue.class).stream()
                .filter(item -> ((Quantitativevalue)item).getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(itemsToDelete);
        return true;
    }

    @Override
    public org.epos.eposdatamodel.QuantitativeValue retrieve(String instanceId) {
        List<Quantitativevalue> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Quantitativevalue.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Quantitativevalue edmobj = elementList.get(0);
        org.epos.eposdatamodel.QuantitativeValue o = new org.epos.eposdatamodel.QuantitativeValue();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setUnit(edmobj.getUnitcode());
        o.setValue(edmobj.getValue());

        return (org.epos.eposdatamodel.QuantitativeValue) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public List<org.epos.eposdatamodel.QuantitativeValue> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Quantitativevalue.class));
    }
    @Override
    public List<org.epos.eposdatamodel.QuantitativeValue> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Quantitativevalue.class));
    }
    @Override
    public List<org.epos.eposdatamodel.QuantitativeValue> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Quantitativevalue.class, status));
    }

    private List<org.epos.eposdatamodel.QuantitativeValue> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Quantitativevalue> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Quantitativevalue.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Quantitativevalue edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.QUANTITATIVEVALUE.name());

        return o;
    }
}
