package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;
import usermanagementapis.UserGroupManagementAPI;

import java.util.ArrayList;
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

        List<Facility> returnList = getDbaccess().getOneFromDB(
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
        edmobj.setKeywords(obj.getKeywords());

        /** CATEGORY **/
        if (obj.getCategory() != null)
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null)
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);


        /** ADDRESS **/
        if (obj.getAddress() != null) {
            if(relationFromUpdate!=null && obj.getAddress().contains(relationFromUpdate)){
                obj.getAddress().remove(relationFromUpdate);
                obj.getAddress().add(relationToUpdate);
            }
            for(LinkedEntity address : obj.getAddress()){
                Address address1 = (Address) RelationChecker.checkRelation(obj, previousObj, null, address, overrideStatus, Address.class, false);
                if(address1 != null){
                    FacilityAddress pi = new FacilityAddress();
                    pi.setFacilityInstance(edmobj);
                    pi.setAddressInstance(address1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** ISPARTOF **/
        if (obj.getIsPartOf() != null) {
            if(relationFromUpdate!=null && obj.getIsPartOf().contains(relationFromUpdate)){
                obj.getIsPartOf().remove(relationFromUpdate);
                obj.getIsPartOf().add(relationToUpdate);
            }
            for(LinkedEntity facility : obj.getIsPartOf()){
                Facility facility1 = (Facility) RelationChecker.checkRelation(obj, previousObj, null, facility, overrideStatus, Facility.class, false);
                if(facility1 != null){
                    FacilityIspartof pi = new FacilityIspartof();
                    pi.setFacility1Instance(edmobj);
                    pi.setFacility2Instance(facility1);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** SPATIAL **/
        if (obj.getSpatialExtent() != null) {
            if(relationFromUpdate!=null && obj.getSpatialExtent().contains(relationFromUpdate)){
                obj.getSpatialExtent().remove(relationFromUpdate);
                obj.getSpatialExtent().add(relationToUpdate);
            }
            for(org.epos.eposdatamodel.LinkedEntity location : obj.getSpatialExtent()){
                Spatial spatial = (Spatial) RelationChecker.checkRelation(obj, previousObj, null, location, overrideStatus, Spatial.class, false);
                if(spatial != null){
                    FacilitySpatial pi = new FacilitySpatial();
                    pi.setFacilityInstance(edmobj);
                    pi.setSpatialInstance(spatial);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /* PAGEURL */
        if(obj.getPageURL()!=null){
            for(String pageurl : obj.getPageURL()) {
                createInnerElement(ElementType.PAGEURL, pageurl, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Facility edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);

        if(edmobj.getVersion().getEditorId()!=null) element.setEditorId(edmobj.getVersion().getEditorId());
        if(edmobj.getVersion().getProvenance()!=null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if(edmobj.getVersion().getChangeComment()!=null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if(edmobj.getVersion().getChangeTimestamp()!=null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        FacilityElement ce = new FacilityElement();
        ce.setFacilityInstance(edmobj);
        ce.setElementInstance(el.get(0));
        dbaccess.updateObject(ce);
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(FacilityContactpoint.class)){
            FacilityContactpoint item = (FacilityContactpoint) object;
            if(item.getFacilityInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilityIspartof.class)){
            FacilityIspartof item = (FacilityIspartof) object;
            if(item.getFacility1Instance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilityElement.class)){
            FacilityElement item = (FacilityElement) object;
            if(item.getFacilityInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilitySpatial.class)){
            FacilitySpatial item = (FacilitySpatial) object;
            if(item.getFacilityInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilityCategory.class)){
            FacilityCategory item = (FacilityCategory) object;
            if(item.getFacilityInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(FacilityAddress.class)){
            FacilityAddress item = (FacilityAddress) object;
            if(item.getFacilityInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        List<Facility> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Facility.class);
        for(Facility object : elementList){
            dbaccess.deleteObject(object);
        }

        return true;
    }

    @Override
    public org.epos.eposdatamodel.Facility retrieve(String instanceId) {
        List<Facility> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Facility.class);
        if (elementList == null || elementList.isEmpty()) {
            return null;
        }
            Facility edmobj = elementList.get(0);
            org.epos.eposdatamodel.Facility o = new org.epos.eposdatamodel.Facility();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setType(edmobj.getType());
            o.setIdentifier(edmobj.getIdentifier());
            o.setDescription(edmobj.getDescription());
            o.setTitle(edmobj.getTitle());
            o.setKeywords(edmobj.getKeywords());

            for (Object object : dbaccess.getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityCategory.class)) {
                FacilityCategory item = (FacilityCategory) object;
                //if(item.getFacilityInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityContactpoint.class)) {
                FacilityContactpoint item = (FacilityContactpoint) object;
                //if(item.getFacilityInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addContactPoint(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityAddress.class)) {
                FacilityAddress item = (FacilityAddress) object;
                //if(item.getFacilityInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(item.getAddressInstance().getInstanceId());
                    o.addAddress(le);
                // }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("facility1Instance", edmobj.getInstanceId(),FacilityIspartof.class)) {
                FacilityIspartof item = (FacilityIspartof) object;
                // if(item.getFacility1Instance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getFacility2Instance().getInstanceId());
                    o.addIsPartOf(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilitySpatial.class)) {
                FacilitySpatial item = (FacilitySpatial) object;
                //if(item.getFacilityInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId());
                    o.addIsPartOf(le);
                //}
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(),FacilityElement.class)) {
                FacilityElement item = (FacilityElement) object;
                //if(item.getFacilityInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.PAGEURL.name())) o.addPageURL(el.getValue());
                //}
            }

            o = (org.epos.eposdatamodel.Facility) VersioningStatusAPI.retrieveVersion(o);

            return o;
    }
    @Override
    public List<org.epos.eposdatamodel.Facility> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListFromDBByInstanceId(entities, Facility.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Facility> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllFromDB(Facility.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Facility> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllFromDBWithStatus(Facility.class, status));
    }

    private List<org.epos.eposdatamodel.Facility> retrieveEntities(Function<Void, List<Facility>> dbFetcher) {
        List<Facility> dbEntities = dbFetcher.apply(null);

        return dbEntities.parallelStream()
                .map(item -> retrieve(item.getInstanceId()))
                .collect(Collectors.toList());
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
