package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class EquipmentAPI extends AbstractAPI<org.epos.eposdatamodel.Equipment> {

    public EquipmentAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        List<Equipment> returnList = getDbaccess().getOneFromDB(
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

        obj = (org.epos.eposdatamodel.Equipment) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Equipment edmobj = new Equipment();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setType(obj.getType());
        edmobj.setIdentifier(obj.getIdentifier());
        edmobj.setDescription(obj.getDescription());
        edmobj.setName(obj.getName());
        edmobj.setKeywords(obj.getKeywords());
        edmobj.setDynamicrange(obj.getDynamicRange());
        edmobj.setFilter(obj.getFilter());
        edmobj.setOrientation(obj.getOrientation());
        edmobj.setResolution(obj.getResolution());
        edmobj.setSampleperiod(obj.getSamplePeriod());
        edmobj.setSerialnumber(obj.getSerialNumber());

        /** TODO: creator **/

        /** CATEGORY **/
        if (obj.getCategory() != null && !obj.getCategory().isEmpty())
            CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** CONTACTPOINT **/
        if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty())
            ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        /** ISPARTOF EQUIPMENT **/
        if (obj.getIsPartOf() != null && !obj.getIsPartOf().isEmpty()) {
            if(relationFromUpdate!=null && obj.getIsPartOf().contains(relationFromUpdate)){
                obj.getIsPartOf().remove(relationFromUpdate);
                obj.getIsPartOf().add(relationToUpdate);
            }

            for(LinkedEntity equipment : obj.getIsPartOf()){

                Equipment equipment1 = (Equipment) RelationChecker.checkRelation(obj, previousObj, null, equipment, overrideStatus, Equipment.class);

                if(equipment1!=null) {
                    EquipmentIspartof pi = new EquipmentIspartof();
                    pi.setEquipment(edmobj);
                    pi.setEquipmentInstanceId(edmobj.getInstanceId());
                    pi.setEntityInstanceId(equipment1.getInstanceId());
                    pi.setResourceEntity(EntityNames.EQUIPMENT.name());
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** ISPARTOF FACILITY **/
        if (obj.getIsPartOf() != null && !obj.getIsPartOf().isEmpty()) {
            if(relationFromUpdate!=null && obj.getIsPartOf().contains(relationFromUpdate)){
                obj.getIsPartOf().remove(relationFromUpdate);
                obj.getIsPartOf().add(relationToUpdate);
            }

            for(LinkedEntity facility : obj.getIsPartOf()){

                Facility facility1 = (Facility) RelationChecker.checkRelation(obj, previousObj, null, facility, overrideStatus, Facility.class);

                if(facility1!=null) {
                    EquipmentIspartof pi = new EquipmentIspartof();
                    pi.setEquipment(edmobj);
                    pi.setEquipmentInstanceId(edmobj.getInstanceId());
                    pi.setEntityInstanceId(facility1.getInstanceId());
                    pi.setResourceEntity(EntityNames.FACILITY.name());
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** SPATIAL **/
        if (obj.getSpatialExtent() != null && !obj.getSpatialExtent().isEmpty()) {
            if(relationFromUpdate!=null && obj.getSpatialExtent().contains(relationFromUpdate)){
                obj.getSpatialExtent().remove(relationFromUpdate);
                obj.getSpatialExtent().add(relationToUpdate);
            }

            for(org.epos.eposdatamodel.LinkedEntity location : obj.getSpatialExtent()){
                Spatial spatial = (Spatial) RelationChecker.checkRelation(obj, previousObj, null, location, overrideStatus, Spatial.class);
                if(spatial!=null){
                    EquipmentSpatial pi = new EquipmentSpatial();
                    pi.setEquipmentInstance(edmobj);
                    pi.setSpatialInstance(spatial);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /** TEMPORAL **/
        if (obj.getTemporalExtent() != null && !obj.getTemporalExtent().isEmpty()) {
            if(relationFromUpdate!=null && obj.getTemporalExtent().contains(relationFromUpdate)){
                obj.getTemporalExtent().remove(relationFromUpdate);
                obj.getTemporalExtent().add(relationToUpdate);
            }
            for(org.epos.eposdatamodel.LinkedEntity periodOfTime : obj.getTemporalExtent()){
                Temporal temporal = (Temporal) RelationChecker.checkRelation(obj, previousObj, null, periodOfTime, overrideStatus, Temporal.class);
                if(temporal!=null){
                    EquipmentTemporal pi = new EquipmentTemporal();
                    pi.setEquipmentInstance(edmobj);
                    pi.setTemporalInstance(temporal);
                    dbaccess.updateObject(pi);
                }
            }
        }

        /* PAGEURL */
        if(obj.getPageURL()!=null && !obj.getPageURL().isEmpty()){
            createInnerElement(ElementType.PAGEURL, obj.getPageURL(), edmobj, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                    .instanceId(edmobj.getInstanceId())
                    .metaId(edmobj.getMetaId())
                    .uid(edmobj.getUid());

    }

    private void createInnerElement(ElementType elementType, String value, Equipment edmobj, StatusType overrideStatus){
        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);
        LinkedEntity le = retrieveAPI(EntityNames.ELEMENT.name()).create(element, overrideStatus, null, null);
        List<Element> el = dbaccess.getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
        EquipmentElement ce = new EquipmentElement();
        ce.setEquipmentInstance(edmobj);
        ce.setElementInstance(el.get(0));
        dbaccess.updateObject(ce);
    }


    @Override
    public org.epos.eposdatamodel.Equipment retrieve(String instanceId) {
        List<Equipment> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Equipment.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Equipment edmobj = elementList.get(0);
            org.epos.eposdatamodel.Equipment o = new org.epos.eposdatamodel.Equipment();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setType(edmobj.getType());
            o.setResolution(edmobj.getResolution());
            o.setDescription(edmobj.getDescription());
            o.setDynamicRange(edmobj.getDynamicrange());
            o.setFilter(edmobj.getFilter());
            o.setIdentifier(edmobj.getIdentifier());
            o.setName(edmobj.getName());
            o.setPageURL(edmobj.getPageurl());
            o.setOrientation(edmobj.getOrientation());
            o.setSamplePeriod(edmobj.getSampleperiod());
            o.setSerialNumber(edmobj.getSerialnumber());
            o.addKeywords(edmobj.getKeywords());

            for (Object object : dbaccess.getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentCategory.class)) {
                EquipmentCategory item = (EquipmentCategory) object;
                if(item.getEquipmentInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId());
                    o.addCategory(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentContactpoint.class)) {
                EquipmentContactpoint item = (EquipmentContactpoint) object;
                if(item.getEquipmentInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId());
                    o.addCategory(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("equipment", edmobj.getInstanceId(),EquipmentIspartof.class)) {
                EquipmentIspartof item = (EquipmentIspartof) object;
                if(item.getEquipment().getInstanceId().equals(edmobj.getInstanceId())) {
                    if(item.getResourceEntity().equals(EntityNames.FACILITY.name())){
                        o.addIsPartOf(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                    }
                    if(item.getResourceEntity().equals(EntityNames.EQUIPMENT.name())){
                        o.addIsPartOf(retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
                    }
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentSpatial.class)) {
                EquipmentSpatial item = (EquipmentSpatial) object;
                if(item.getEquipmentInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId());
                    o.addSpatialExtentItem(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentTemporal.class)) {
                EquipmentTemporal item = (EquipmentTemporal) object;
                if(item.getEquipmentInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    LinkedEntity le = retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveLinkedEntity(item.getTemporalInstance().getInstanceId());
                    o.addTemporalExtent(le);
                }
            }

            for (Object object : dbaccess.getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentElement.class)) {
                EquipmentElement item = (EquipmentElement) object;
                if(item.getEquipmentInstance().getInstanceId().equals(edmobj.getInstanceId())) {
                    Element el = item.getElementInstance();
                    if (el.getType().equals(ElementType.PAGEURL.name())) o.setPageURL(el.getValue());
                }
            }

            o = (org.epos.eposdatamodel.Equipment) VersioningStatusAPI.retrieveVersion(o);

            return o;
        }
        return null;
    }

    @Override
    public Boolean delete(String instanceId) {
        for(Object object : getDbaccess().getAllFromDB(EquipmentContactpoint.class)){
            EquipmentContactpoint item = (EquipmentContactpoint) object;
            if(item.getEquipmentInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentTemporal.class)){
            EquipmentTemporal item = (EquipmentTemporal) object;
            if(item.getEquipmentInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentSpatial.class)){
            EquipmentSpatial item = (EquipmentSpatial) object;
            if(item.getEquipmentInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentRelation.class)){
            EquipmentRelation item = (EquipmentRelation) object;
            if(item.getEquipment().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentIspartof.class)){
            EquipmentIspartof item = (EquipmentIspartof) object;
            if(item.getEquipment().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentElement.class)){
            EquipmentElement item = (EquipmentElement) object;
            if(item.getEquipmentInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        for(Object object : getDbaccess().getAllFromDB(EquipmentCategory.class)){
            EquipmentCategory item = (EquipmentCategory) object;
            if(item.getEquipmentInstance().getInstanceId().equals(instanceId)){
                dbaccess.deleteObject(item);
            }
        }
        List<Equipment> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Equipment.class);
        for(Equipment object : elementList){
            dbaccess.deleteObject(object);
        }

        return true;
    }

    @Override
    public List<org.epos.eposdatamodel.Equipment> retrieveBunch(List<String> entities) {
        List<Equipment> list = getDbaccess().getListFromDBByInstanceId(entities, Equipment.class);
        List<org.epos.eposdatamodel.Equipment> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.Equipment> retrieveAll() {
        List<Equipment> list = getDbaccess().getAllFromDB(Equipment.class);
        List<org.epos.eposdatamodel.Equipment> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public List<org.epos.eposdatamodel.Equipment> retrieveAllWithStatus(StatusType status) {
        List<Equipment> list = getDbaccess().getAllFromDBWithStatus(Equipment.class,status);
        List<org.epos.eposdatamodel.Equipment> returnList = new ArrayList<>();
        list.parallelStream().forEach(item -> {
            returnList.add(retrieve(item.getInstanceId()));
        });
        return returnList;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Equipment> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Equipment.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Equipment edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.EQUIPMENT.name());

            return o;
        }
        return null;
    }

}
