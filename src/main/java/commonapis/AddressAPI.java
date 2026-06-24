package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import metadataapis.OrganizationAPI;
import metadataapis.PersonAPI;
import model.*;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AddressAPI extends AbstractAPI<org.epos.eposdatamodel.Address> {

    public AddressAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Address obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        String searchInstanceId = obj.getInstanceId();

        List<Address> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Address selectedEntity = returnList.get(0);

            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);

            for (Address item : returnList) {
                if (item.getVersion() != null &&
                        targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }

            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Address) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Address edmobj = new Address();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setCountry(obj.getCountry());
        edmobj.setCountrycode(obj.getCountryCode());
        edmobj.setStreet(obj.getStreet());
        edmobj.setPostalCode(obj.getPostalCode());
        edmobj.setLocality(obj.getLocality());

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.ADDRESS.name(), edmobj);
        
        // Resolve pending address relations for Person and Organization entities
        // that were created before this Address existed
        PersonAPI.resolvePendingAddressRelationsForAddress(edmobj.getUid(), edmobj.getInstanceId());
        OrganizationAPI.resolvePendingAddressRelationsForAddress(edmobj.getUid(), edmobj.getInstanceId());

        
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
    public org.epos.eposdatamodel.Address retrieve(String instanceId) {
        List<Address> addressList = getDbaccess().getOneFromDBByInstanceId(instanceId, Address.class);
        if (addressList.isEmpty()) {
            return null;
        }

        Address edmobj = addressList.get(0);
        org.epos.eposdatamodel.Address o = new org.epos.eposdatamodel.Address();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setStreet(edmobj.getStreet());
        o.setCountry(edmobj.getCountry());
        o.setPostalCode(edmobj.getPostalCode());
        o.setCountryCode(edmobj.getCountrycode());
        o.setLocality(edmobj.getLocality());

        return (org.epos.eposdatamodel.Address) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override
    public Boolean delete(String instanceId) {
        List<FacilityAddress> facilityAddresses = (List<FacilityAddress>) getDbaccess().getAllFromDB(FacilityAddress.class)
                .stream()
                .filter(item -> ((FacilityAddress) item).getAddressInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(Collections.singletonList(facilityAddresses));

        List<Address> addressesToDelete = (List<Address>) getDbaccess().getAllFromDB(Address.class)
                .stream()
                .filter(item -> ((Address)item).getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(Collections.singletonList(addressesToDelete));

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Address retrieveByUID(String uid) {
        List<Address> returnList = getDbaccess().getOneFromDBByUID(uid, Address.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.Address> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Address.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Address> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Address.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Address> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Address.class, status));
    }

    private List<org.epos.eposdatamodel.Address> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);
        return dbEntities.parallelStream().map(item -> retrieve(item)).collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Address> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Address.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Address edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.ADDRESS.name());

        return o;
    }
}