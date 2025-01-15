package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.Quantitativevalue;
import model.StatusType;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.QuantitativeValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class QuantitativeValueAPI extends AbstractAPI<org.epos.eposdatamodel.QuantitativeValue> {

    public QuantitativeValueAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(QuantitativeValue obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Quantitativevalue> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.QuantitativeValue) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Quantitativevalue edmobj = new Quantitativevalue();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
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
        return true;
    }

    @Override
    public org.epos.eposdatamodel.QuantitativeValue retrieve(String instanceId) {
        List<Quantitativevalue> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Quantitativevalue.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Quantitativevalue edmobj = elementList.get(0);
            org.epos.eposdatamodel.QuantitativeValue o = new org.epos.eposdatamodel.QuantitativeValue();

            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setUnit(edmobj.getUnitcode());
            o.setValue(edmobj.getValue());

            o = (org.epos.eposdatamodel.QuantitativeValue) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.QuantitativeValue> retrieveBunch(List<String> entities) {
        List<Quantitativevalue> list = getDbaccess().getListFromDBByInstanceId(entities, Quantitativevalue.class);
        List<org.epos.eposdatamodel.QuantitativeValue> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.QuantitativeValue> retrieveAll() {
        List<Quantitativevalue> list = getDbaccess().getAllFromDB(Quantitativevalue.class);
        List<org.epos.eposdatamodel.QuantitativeValue> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.QuantitativeValue> retrieveAllWithStatus(StatusType status) {
        List<Quantitativevalue> list = getDbaccess().getAllFromDBWithStatus(Quantitativevalue.class, status);
        List<org.epos.eposdatamodel.QuantitativeValue> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Quantitativevalue> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Quantitativevalue.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Quantitativevalue edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.QUANTITATIVEVALUE.name());

            return o;
        }
        return null;
    }
}
