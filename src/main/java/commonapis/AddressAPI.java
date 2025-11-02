package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

public class AddressAPI extends AbstractAPI<org.epos.eposdatamodel.Address> {

    public AddressAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Address obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        List<Address> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Address) VersioningStatusAPI.checkVersion(obj, overrideStatus);

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

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
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

        // Batch deletion for FacilityAddress and Address
        List<FacilityAddress> facilityAddresses = (List<FacilityAddress>) getDbaccess().getAllFromDB(FacilityAddress.class)
                .stream()
                .filter(item -> ((FacilityAddress) item).getAddressInstance().getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(facilityAddresses);

        List<Address> addressesToDelete = (List<Address>) getDbaccess().getAllFromDB(Address.class)
                .stream()
                .filter(item -> ((Address)item).getInstanceId().equals(instanceId))
                .collect(Collectors.toList());
        EposDataModelDAO.getInstance().deleteListOfObjects(addressesToDelete);

        return true;
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

        return dbEntities.parallelStream()
                .map(item -> retrieve(item))
                .collect(Collectors.toList());
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
