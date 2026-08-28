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
        // Keep the legacy default for direct API calls; lifecycle callers pass
        // an explicit override when they need a non-published address version.
        if (overrideStatus == null
                || (overrideStatus == StatusType.DRAFT && obj.getEditorId() == null)) {
            obj.setStatus(StatusType.PUBLISHED);
            overrideStatus = StatusType.PUBLISHED;
        }


        String searchInstanceId = obj.getInstanceId();

        List<Address> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Address selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Address::getVersion);

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
            repointPublishedVersion(obj, null, Address.class);
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
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Address.class,
                Map.of(FacilityAddress.class, "addressInstance"), List.of(
                        new EposDataModelDAO.RelationField(Organization.class, "address"),
                        new EposDataModelDAO.RelationField(Person.class, "address")));
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
    public List<org.epos.eposdatamodel.Address> retrieveBunchSummary(List<String> entities) {
        return retrieveSummary(getDbaccess().getListIDsFromDBByInstanceId(entities, Address.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Address> retrieveAllSummaryWithStatus(StatusType status) {
        return retrieveSummary(getDbaccess().getAllIDsFromDBWithStatus(Address.class, status));
    }
    @Override
    public List<org.epos.eposdatamodel.Address> retrieveAllSummary() {
        return retrieveSummary(getDbaccess().getAllIDsFromDB(Address.class));
    }
    private List<org.epos.eposdatamodel.Address> retrieveSummary(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();
        EposDataModelDAO<?> dao = getDbaccess();
        Map<String, EposDataModelDAO.AddressSummaryRow> rows = dao.fetchAddressSummaryRows(instanceIds).stream()
                .collect(Collectors.toMap(EposDataModelDAO.AddressSummaryRow::instanceId, row -> row));
        List<org.epos.eposdatamodel.Address> results = new ArrayList<>(rows.size());
        for (String id : instanceIds) {
            EposDataModelDAO.AddressSummaryRow row = rows.get(id);
            if (row == null) continue;
            org.epos.eposdatamodel.Address dto = new org.epos.eposdatamodel.Address();
            dto.setInstanceId(row.instanceId()); dto.setMetaId(row.metaId()); dto.setUid(row.uid());
            dto.setStreet(row.street()); dto.setCountry(row.country()); dto.setPostalCode(row.postalCode());
            dto.setCountryCode(row.countrycode()); dto.setLocality(row.locality());
            VersioningStatusAPI.applyVersion(dto, VersioningStatusAPI.summaryVersion(row.versionId(), row.versionMetaId(),
                    row.changeComment(), row.changeTimestamp(), row.editorId(), row.provenance(), row.version(),
                    row.instanceChangeId(), row.status()), Collections.emptyList());
            results.add(dto);
        }
        return results;
    }
    @Override
    public List<org.epos.eposdatamodel.Address> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Address.class, status));
    }

    private List<org.epos.eposdatamodel.Address> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);
        return retrieveBulk(dbEntities, Address.class, entity -> {
            org.epos.eposdatamodel.Address dto = new org.epos.eposdatamodel.Address();
            dto.setInstanceId(entity.getInstanceId());
            dto.setMetaId(entity.getMetaId());
            dto.setUid(entity.getUid());
            dto.setStreet(entity.getStreet());
            dto.setCountry(entity.getCountry());
            dto.setPostalCode(entity.getPostalCode());
            dto.setCountryCode(entity.getCountrycode());
            dto.setLocality(entity.getLocality());
            return dto;
        });
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
