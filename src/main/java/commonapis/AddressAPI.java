package commonapis;

import abstractapis.AbstractAPI;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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

        if(!returnList.isEmpty()){
            obj.setInstanceId(returnList.get(0).getInstanceId());
            obj.setMetaId(returnList.get(0).getMetaId());
            obj.setUid(returnList.get(0).getUid());
            obj.setVersionId(returnList.get(0).getVersion().getVersionId());
        }

        VersioningStatusAPI.checkVersion(obj, overrideStatus);

        System.out.println(obj.getVersionId()+" "+obj.getStatus());

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        System.out.println(VersioningStatusAPI.retrieveVersioningStatus(obj).getVersionId()+" "+VersioningStatusAPI.retrieveVersioningStatus(obj).getStatus());

        Address edmobj = new Address();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
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
        if(addressList!=null && !addressList.isEmpty()) {
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

            o = (org.epos.eposdatamodel.Address) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public Boolean delete(String instanceId) {

        for(Object object : getDbaccess().getAllFromDB(FacilityAddress.class)){
            FacilityAddress item = (FacilityAddress) object;
            if(item.getAddressInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        for(Object object : getDbaccess().getAllFromDB(Address.class)){
            Address item = (Address) object;
            if(item.getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }

        return true;
    }

    @Override
    public List<org.epos.eposdatamodel.Address> retrieveBunch(List<String> entities) {
        List<Address> list = getDbaccess().getListFromDBByInstanceId(entities, Address.class);
        List<org.epos.eposdatamodel.Address> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.Address> retrieveAll() {
        List<Address> list = getDbaccess().getAllFromDB(Address.class);
        List<org.epos.eposdatamodel.Address> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.Address> retrieveAllWithStatus(StatusType status) {
        List<Address> list = getDbaccess().getAllFromDBWithStatus(Address.class, status);
        List<org.epos.eposdatamodel.Address> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Address> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Address.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Address edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.ADDRESS.name());

            return o;
        }
        return null;
    }


}
