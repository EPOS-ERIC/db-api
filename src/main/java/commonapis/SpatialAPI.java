package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.Location;
import usermanagementapis.UserGroupManagementAPI;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class SpatialAPI extends AbstractAPI<org.epos.eposdatamodel.Location> {

    public SpatialAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(Location obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Check if the object already exists in the database
        List<Spatial> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                getEdmClass());

        // If object exists, update its details
        if (!returnList.isEmpty()) {
            Spatial existing = returnList.get(0);
            obj.setInstanceId(existing.getInstanceId());
            obj.setMetaId(existing.getMetaId());
            obj.setUid(existing.getUid());
            obj.setVersionId(existing.getVersion().getVersionId());
        }

        // Versioning and entity creation
        obj = (org.epos.eposdatamodel.Location) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);


        Spatial edmobj = new Spatial();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setLocation(Optional.ofNullable(obj.getLocation()).orElse(null));

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public Boolean delete(String instanceId) {
        // Use streams to batch delete spatial-related entities
        List<Object> relatedItems = (List<Object>) getDbaccess().getAllFromDB(DataproductSpatial.class).stream()
                .filter(item -> ((DataproductSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(WebserviceSpatial.class).stream()
                .filter(item -> ((WebserviceSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(FacilitySpatial.class).stream()
                .filter(item -> ((FacilitySpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(EquipmentSpatial.class).stream()
                .filter(item -> ((EquipmentSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        relatedItems = (List<Object>) getDbaccess().getAllFromDB(ServiceSpatial.class).stream()
                .filter(item -> ((ServiceSpatial) item).getSpatialInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(relatedItems);

        // Delete Spatial itself
        List<Spatial> spatialItems = (List<Spatial>) getDbaccess().getAllFromDB(Spatial.class).stream()
                .filter(item -> ((Spatial)item).getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        dbaccess.deleteListOfObjects(spatialItems);

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Location retrieve(String instanceId) {
        // Fetch the Spatial record by instanceId
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
        o.setGroups(UserGroupManagementAPI.retrieveShortGroupsFromMetaId(edmobj.getMetaId()));

        return (org.epos.eposdatamodel.Location) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public List<org.epos.eposdatamodel.Location> retrieveBunch(List<String> entities) {
        // Retrieve a list of Spatial entities by their instance IDs
        List<Spatial> list = getDbaccess().getListFromDBByInstanceId(entities, Spatial.class);

        // Use streams for efficient processing of the list
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.Location> retrieveAll() {
        // Retrieve all Spatial entities
        List<Spatial> list = getDbaccess().getAllFromDB(Spatial.class);

        // Use streams for efficient processing of the list
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.Location> retrieveAllWithStatus(StatusType status) {
        // Retrieve all Spatial entities with a given status
        List<Spatial> list = getDbaccess().getAllFromDBWithStatus(Spatial.class, status);

        // Use streams for efficient processing of the list
        return list.stream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        // Retrieve the Spatial entity and return a LinkedEntity
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
