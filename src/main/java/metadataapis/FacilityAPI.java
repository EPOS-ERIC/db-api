package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationSyncUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FacilityAPI extends AbstractAPI<org.epos.eposdatamodel.Facility> {

    public FacilityAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Facility obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Facility> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Facility selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Facility item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Facility) VersioningStatusAPI.checkVersion(obj, overrideStatus);
        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Facility edmobj = new Facility();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setType(obj.getType());
        edmobj.setIdentifier(obj.getIdentifier());
        edmobj.setDescription(obj.getDescription());
        edmobj.setTitle(obj.getTitle());
        if (obj.getKeywords() != null) edmobj.setKeywords(String.join(",", obj.getKeywords()));

        if (obj.getCategory() != null) CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);
        if (obj.getContactPoint() != null) ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        // ADDRESS
        if (obj.getAddress() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getAddress(), relationFromUpdate, relationToUpdate,
                    FacilityAddress.class, Address.class,
                    "facilityInstance", FacilityAddress::getAddressInstance, FacilityAddress::setFacilityInstance, FacilityAddress::setAddressInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // ISPARTOF (Facility -> Facility)
        if (obj.getIsPartOf() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getIsPartOf(), relationFromUpdate, relationToUpdate,
                    FacilityIspartof.class, Facility.class,
                    "facility1Instance", FacilityIspartof::getFacility2Instance, FacilityIspartof::setFacility1Instance, FacilityIspartof::setFacility2Instance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // SPATIAL
        if (obj.getSpatialExtent() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                    FacilitySpatial.class, Spatial.class,
                    "facilityInstance", FacilitySpatial::getSpatialInstance, FacilitySpatial::setFacilityInstance, FacilitySpatial::setSpatialInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        if(obj.getPageURL()!=null){
            for(String pageurl : obj.getPageURL()) createInnerElement(ElementType.PAGEURL, pageurl, edmobj, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void createInnerElement(ElementType elementType, String value, Facility edmobj, StatusType overrideStatus){
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(), FacilityElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                FacilityElement relation = (FacilityElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null && existingElement.getType().equals(elementType.name()) && existingElement.getValue().equals(value)) {
                    return;
                }
            }
        }

        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if(!el.isEmpty()) {
            FacilityElement ce = new FacilityElement();
            ce.setFacilityInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("facilityInstance", instanceId, FacilityContactpoint.class);
        deleteRelations("facility1Instance", instanceId, FacilityIspartof.class);
        deleteRelations("facilityInstance", instanceId, FacilityElement.class);
        deleteRelations("facilityInstance", instanceId, FacilitySpatial.class);
        deleteRelations("facilityInstance", instanceId, FacilityCategory.class);
        deleteRelations("facilityInstance", instanceId, FacilityAddress.class);

        List<Facility> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Facility.class);
        for(Facility object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if(list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Facility retrieve(String instanceId) {
        List<Facility> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Facility.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Facility edmobj = elementList.get(0);
        org.epos.eposdatamodel.Facility o = new org.epos.eposdatamodel.Facility();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setIdentifier(edmobj.getIdentifier());
        o.setDescription(edmobj.getDescription());
        o.setTitle(edmobj.getTitle());
        if(edmobj.getKeywords()!=null && !edmobj.getKeywords().isEmpty())
            o.setKeywords(Arrays.asList(edmobj.getKeywords().split(",")));

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityCategory.class)) {
            FacilityCategory item = (FacilityCategory) object;
            o.addCategory(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityContactpoint.class)) {
            FacilityContactpoint item = (FacilityContactpoint) object;
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityAddress.class)) {
            FacilityAddress item = (FacilityAddress) object;
            o.addAddress(retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(item.getAddressInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facility1Instance", edmobj.getInstanceId(),FacilityIspartof.class)) {
            FacilityIspartof item = (FacilityIspartof) object;
            o.addIsPartOf(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getFacility2Instance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilitySpatial.class)) {
            FacilitySpatial item = (FacilitySpatial) object;
            o.addSpatialExtent(retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityElement.class)) {
            FacilityElement item = (FacilityElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.PAGEURL.name())) o.addPageURL(el.getValue());
        }

        o = (org.epos.eposdatamodel.Facility) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Facility retrieveByUID(String uid) {
        List<Facility> returnList = getDbaccess().getOneFromDBByUID(uid, Facility.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.Facility> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Facility.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Facility> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Facility.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Facility> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Facility.class, status));
    }
    private List<org.epos.eposdatamodel.Facility> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Facility> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Facility.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Facility edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.FACILITY.name());
            return o;
        }
        return null;
    }
}