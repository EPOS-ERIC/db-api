package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationChecker;
import relationsapi.RelationSyncUtil;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EquipmentAPI extends AbstractAPI<org.epos.eposdatamodel.Equipment> {

    public EquipmentAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Equipment> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Equipment selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Equipment item : returnList) {
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
        if (obj.getKeywords() != null) edmobj.setKeywords(String.join(",", obj.getKeywords()));
        edmobj.setDynamicrange(obj.getDynamicRange());
        edmobj.setFilter(obj.getFilter());
        edmobj.setOrientation(obj.getOrientation());
        edmobj.setResolution(obj.getResolution());
        edmobj.setSampleperiod(obj.getSamplePeriod());
        edmobj.setSerialnumber(obj.getSerialNumber());

        if (obj.getCategory() != null) CategoryRelationsAPI.createRelation(edmobj,obj, overrideStatus);
        if (obj.getContactPoint() != null) ContactPointRelationsAPI.createRelation(edmobj,obj, overrideStatus);

        if (obj.getIsPartOf() != null) {
            if(relationFromUpdate!=null && obj.getIsPartOf().contains(relationFromUpdate)){
                obj.getIsPartOf().remove(relationFromUpdate);
                obj.getIsPartOf().add(relationToUpdate);
            }

            List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("equipment", edmobj.getInstanceId(), EquipmentIspartof.class);
            Map<String, EquipmentIspartof> existingMap = new HashMap<>();
            if(existing!=null) {
                for(Object o : existing) {
                    EquipmentIspartof rel = (EquipmentIspartof) o;
                    existingMap.put(rel.getEntityInstanceId(), rel);
                }
            }
            Set<String> processedIds = new HashSet<>();

            for(LinkedEntity item : obj.getIsPartOf()){
                // Check if it matches EQUIPMENT or FACILITY using RelationChecker
                Class<?> targetClass = EntityNames.FACILITY.name().equals(item.getEntityType()) ? Facility.class : Equipment.class;

                EPOSDataModelEntity resolved = (EPOSDataModelEntity) RelationChecker.checkRelation(obj, previousObj, null, item, overrideStatus, targetClass, false);

                if(resolved != null) {
                    processedIds.add(resolved.getInstanceId());
                    if(!existingMap.containsKey(resolved.getInstanceId())) {
                        EquipmentIspartof pi = new EquipmentIspartof();
                        pi.setEquipment(edmobj);
                        pi.setEquipmentInstanceId(edmobj.getInstanceId());
                        pi.setEntityInstanceId(resolved.getInstanceId());
                        pi.setResourceEntity(targetClass == Facility.class ? EntityNames.FACILITY.name() : EntityNames.EQUIPMENT.name());
                        EposDataModelDAO.getInstance().updateObject(pi);
                    }
                }
            }
            // Delete orphans
            for(Map.Entry<String, EquipmentIspartof> entry : existingMap.entrySet()){
                if(!processedIds.contains(entry.getKey())){
                    EposDataModelDAO.getInstance().deleteObject(entry.getValue());
                }
            }
        }

        // SPATIAL EXTENT
        if (obj.getSpatialExtent() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                    EquipmentSpatial.class, Spatial.class,
                    "equipmentInstance", EquipmentSpatial::getSpatialInstance, EquipmentSpatial::setEquipmentInstance, EquipmentSpatial::setSpatialInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // TEMPORAL EXTENT
        if (obj.getTemporalExtent() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getTemporalExtent(), relationFromUpdate, relationToUpdate,
                    EquipmentTemporal.class, Temporal.class,
                    "equipmentInstance", EquipmentTemporal::getTemporalInstance, EquipmentTemporal::setEquipmentInstance, EquipmentTemporal::setTemporalInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        if(obj.getPageURL()!=null){
            createInnerElement(ElementType.PAGEURL, obj.getPageURL(), edmobj, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void createInnerElement(ElementType elementType, String value, Equipment edmobj, StatusType overrideStatus){
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(), EquipmentElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                EquipmentElement relation = (EquipmentElement) obj;
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
            EquipmentElement ce = new EquipmentElement();
            ce.setEquipmentInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }


    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("equipmentInstance", instanceId, EquipmentContactpoint.class);
        deleteRelations("equipmentInstance", instanceId, EquipmentTemporal.class);
        deleteRelations("equipmentInstance", instanceId, EquipmentSpatial.class);
        deleteRelations("equipment", instanceId, EquipmentRelation.class);
        deleteRelations("equipment", instanceId, EquipmentIspartof.class);
        deleteRelations("equipmentInstance", instanceId, EquipmentElement.class);
        deleteRelations("equipmentInstance", instanceId, EquipmentCategory.class);

        List<Equipment> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Equipment.class);
        for(Equipment object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if(list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Equipment retrieve(String instanceId) {
        List<Equipment> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Equipment.class);
        if (elementList == null || elementList.isEmpty()) return null;

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
        if(edmobj.getKeywords()!=null && !edmobj.getKeywords().isEmpty())
            o.setKeywords(Arrays.asList(edmobj.getKeywords().split(",")));

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentCategory.class)) {
            EquipmentCategory item = (EquipmentCategory) object;
            o.addCategory(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentContactpoint.class)) {
            EquipmentContactpoint item = (EquipmentContactpoint) object;
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipment", edmobj.getInstanceId(),EquipmentIspartof.class)) {
            EquipmentIspartof item = (EquipmentIspartof) object;
            if(EntityNames.FACILITY.name().equals(item.getResourceEntity())){
                o.addIsPartOf(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if(EntityNames.EQUIPMENT.name().equals(item.getResourceEntity())){
                o.addIsPartOf(retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentSpatial.class)) {
            EquipmentSpatial item = (EquipmentSpatial) object;
            o.addSpatialExtentItem(retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentTemporal.class)) {
            EquipmentTemporal item = (EquipmentTemporal) object;
            o.addTemporalExtent(retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveLinkedEntity(item.getTemporalInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(),EquipmentElement.class)) {
            EquipmentElement item = (EquipmentElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.PAGEURL.name())) o.setPageURL(el.getValue());
        }

        o = (org.epos.eposdatamodel.Equipment) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Equipment retrieveByUID(String uid) {
        List<Equipment> returnList = getDbaccess().getOneFromDBByUID(uid, Equipment.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.Equipment> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Equipment.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Equipment> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Equipment.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Equipment> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Equipment.class, status));
    }
    private List<org.epos.eposdatamodel.Equipment> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
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