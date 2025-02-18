package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class IdentifierAPI extends AbstractAPI<org.epos.eposdatamodel.Identifier> {

    public IdentifierAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Identifier obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Identifier> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                getEdmClass());

        if (!returnList.isEmpty()) {
            obj.setInstanceId(returnList.get(0).getInstanceId());
            obj.setMetaId(returnList.get(0).getMetaId());
            obj.setUid(returnList.get(0).getUid());
            obj.setVersionId(returnList.get(0).getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Identifier) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Identifier edmobj = new Identifier();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setType(Optional.ofNullable(obj.getType()).orElse(null));
        edmobj.setValue(Optional.ofNullable(obj.getIdentifier()).orElse(null));

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(DataproductIdentifier.class)){
            DataproductIdentifier item = (DataproductIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(WebserviceIdentifier.class)){
            WebserviceIdentifier item = (WebserviceIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(OrganizationIdentifier.class)){
            OrganizationIdentifier item = (OrganizationIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(PersonIdentifier.class)){
            PersonIdentifier item = (PersonIdentifier) object;
            if(item.getIdentifierInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        // Delete Identifier itself
        List<Identifier> identifierList = getDbaccess().getAllFromDB(Identifier.class);
        identifierList.stream()
                .filter(item -> item.getInstanceId().equals(instanceId))
                .forEach(dbaccess::deleteObject);

        return true;
    }

    @Override
    public List<org.epos.eposdatamodel.Identifier> retrieveBunch(List<String> entities) {
        List<Identifier> list = getDbaccess().getListFromDBByInstanceId(entities, Identifier.class);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Identifier>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public org.epos.eposdatamodel.Identifier retrieve(String instanceId) {
        List<Identifier> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Identifier.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Identifier edmobj = elementList.get(0);
        org.epos.eposdatamodel.Identifier o = new org.epos.eposdatamodel.Identifier();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setIdentifier(edmobj.getValue());
        o.setGroups(UserGroupManagementAPI.retrieveShortGroupsFromMetaId(edmobj.getMetaId()));

        return (org.epos.eposdatamodel.Identifier) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public List<org.epos.eposdatamodel.Identifier> retrieveAll() {
        List<Identifier> list = getDbaccess().getAllFromDB(Identifier.class);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Identifier>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public List<org.epos.eposdatamodel.Identifier> retrieveAllWithStatus(StatusType status) {
        List<Identifier> list = getDbaccess().getAllFromDBWithStatus(Identifier.class, status);

        // Using CompletableFuture for parallel retrieval
        List<CompletableFuture<org.epos.eposdatamodel.Identifier>> futures = list.stream()
                .map(item -> CompletableFuture.supplyAsync(() -> retrieve(item.getInstanceId())))
                .collect(Collectors.toList());

        // Collecting results after all futures complete
        return futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Identifier> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Identifier.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Identifier edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.IDENTIFIER.name());

        return o;
    }
}
