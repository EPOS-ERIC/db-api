package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Location;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class SpatialAPI extends AbstractAPI<org.epos.eposdatamodel.Location> {

    public SpatialAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(Location obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        String searchInstanceId = obj.getInstanceId();

        List<Spatial> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Spatial selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Spatial::getVersion);
            if (selectedEntity == null) {
                selectedEntity = returnList.get(0);
            }

            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Location) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Spatial edmobj = new Spatial();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setLocation(Optional.ofNullable(obj.getLocation()).orElse(null));

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.LOCATION.name(), edmobj);

        
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
    public Boolean delete(String instanceId) {
        List<Object> relatedItems = (List<Object>) getDbaccess().getAllFromDB(DataproductSpatial.class).stream()
                .filter(item -> ((DataproductSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(WebserviceSpatial.class).stream()
                .filter(item -> ((WebserviceSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(FacilitySpatial.class).stream()
                .filter(item -> ((FacilitySpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(EquipmentSpatial.class).stream()
                .filter(item -> ((EquipmentSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(ServiceSpatial.class).stream()
                .filter(item -> ((ServiceSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(relatedItems);

        List<Spatial> spatialItems = (List<Spatial>) getDbaccess().getAllFromDB(Spatial.class).stream()
                .filter(item -> ((Spatial)item).getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(spatialItems);

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Location retrieve(String instanceId) {
        List<Spatial> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Spatial.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Spatial edmobj = elementList.get(0);
        org.epos.eposdatamodel.Location o = new org.epos.eposdatamodel.Location();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setLocation(edmobj.getLocation());

        return (org.epos.eposdatamodel.Location) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public org.epos.eposdatamodel.Location retrieveByUID(String uid) {
        List<Spatial> returnList = getDbaccess().getOneFromDBByUID(uid, Spatial.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }
    @Override
    public List<org.epos.eposdatamodel.Location> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Spatial.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Location> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Spatial.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Location> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Spatial.class, status));
    }

    private List<org.epos.eposdatamodel.Location> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);
        return dbEntities.parallelStream().map(item -> retrieve(item)).collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Spatial> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Spatial.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Spatial edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.LOCATION.name());

        return o;
    }
}
