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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class EquipmentAPI extends AbstractAPI<org.epos.eposdatamodel.Equipment> {

    public EquipmentAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Equipment obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Capture if fields were explicitly set BEFORE any processing
        boolean categoryExplicitlySet = isFieldExplicitlySet(obj, "category");
        boolean contactPointExplicitlySet = isFieldExplicitlySet(obj, "contactPoint");
        boolean isPartOfExplicitlySet = isFieldExplicitlySet(obj, "isPartOf");
        boolean spatialExtentExplicitlySet = isFieldExplicitlySet(obj, "spatialExtent");
        boolean temporalExtentExplicitlySet = isFieldExplicitlySet(obj, "temporalExtent");
        boolean pageURLExplicitlySet = isFieldExplicitlySet(obj, "pageURL");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

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

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Equipment selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Equipment item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            oldInstanceId = selectedEntity.getInstanceId();
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());

            if (previousObj == null) {
                previousObj = retrieve(selectedEntity.getInstanceId());
            }
        }

        obj = (org.epos.eposdatamodel.Equipment) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isNewVersion = obj.getInstanceChangedId() != null;
        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());

        Equipment edmobj = new Equipment();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
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

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }


        // CATEGORY
        if (categoryExplicitlySet || !isNewVersion) {
            if (obj.getCategory() != null && !obj.getCategory().isEmpty()) {
                CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyEquipmentCategoryRelations(oldInstanceId, edmobj);
        }

        // CONTACTPOINT
        if (contactPointExplicitlySet || !isNewVersion) {
            if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
                ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyEquipmentContactPointRelations(oldInstanceId, edmobj);
        }

        // ISPARTOF (special handling - can be Equipment or Facility)
        if (isPartOfExplicitlySet || !isNewVersion) {
            if (obj.getIsPartOf() != null && !obj.getIsPartOf().isEmpty()) {
                if (relationFromUpdate != null && obj.getIsPartOf().contains(relationFromUpdate)) {
                    obj.getIsPartOf().remove(relationFromUpdate);
                    obj.getIsPartOf().add(relationToUpdate);
                }

                List<Object> existing = getDbaccess().getJoinEntitiesByRelationField("equipment", edmobj.getInstanceId(), EquipmentIspartof.class);
                Map<String, EquipmentIspartof> existingMap = new HashMap<>();
                if (existing != null) {
                    for (Object o : existing) {
                        EquipmentIspartof rel = (EquipmentIspartof) o;
                        existingMap.put(rel.getEntityInstanceId(), rel);
                    }
                }
                Set<String> processedIds = new HashSet<>();

                for (LinkedEntity item : obj.getIsPartOf()) {
                    Class<?> targetClass = EntityNames.FACILITY.name().equals(item.getEntityType()) ? Facility.class : Equipment.class;
                    Object resolved = RelationChecker.checkRelation(obj, previousObj, null, item, overrideStatus, targetClass, false);
                    if (resolved != null) {
                        String resolvedInstanceId = extractInstanceId(resolved);
                        if (resolvedInstanceId != null) {
                            processedIds.add(resolvedInstanceId);
                            if (!existingMap.containsKey(resolvedInstanceId)) {
                                EquipmentIspartof pi = new EquipmentIspartof();
                                pi.setEquipment(edmobj);
                                pi.setEquipmentInstanceId(edmobj.getInstanceId());
                                pi.setEntityInstanceId(resolvedInstanceId);
                                pi.setResourceEntity(targetClass == Facility.class ? EntityNames.FACILITY.name() : EntityNames.EQUIPMENT.name());
                                EposDataModelDAO.getInstance().updateObject(pi);
                            }
                        }
                    }
                }
                // Delete orphans
                for (Map.Entry<String, EquipmentIspartof> entry : existingMap.entrySet()) {
                    if (!processedIds.contains(entry.getKey())) {
                        EposDataModelDAO.getInstance().deleteObject(entry.getValue());
                    }
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyEquipmentIsPartOfRelations(oldInstanceId, edmobj);
        }

        // SPATIAL EXTENT
        if (spatialExtentExplicitlySet || !isNewVersion) {
            if (obj.getSpatialExtent() != null && !obj.getSpatialExtent().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                        EquipmentSpatial.class, Spatial.class,
                        "equipmentInstance", EquipmentSpatial::getSpatialInstance, EquipmentSpatial::setEquipmentInstance, EquipmentSpatial::setSpatialInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    EquipmentSpatial.class, Spatial.class,
                    "equipmentInstance", EquipmentSpatial::getSpatialInstance, EquipmentSpatial::setEquipmentInstance, EquipmentSpatial::setSpatialInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // TEMPORAL EXTENT
        if (temporalExtentExplicitlySet || !isNewVersion) {
            if (obj.getTemporalExtent() != null && !obj.getTemporalExtent().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getTemporalExtent(), relationFromUpdate, relationToUpdate,
                        EquipmentTemporal.class, Temporal.class,
                        "equipmentInstance", EquipmentTemporal::getTemporalInstance, EquipmentTemporal::setEquipmentInstance, EquipmentTemporal::setTemporalInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    EquipmentTemporal.class, Temporal.class,
                    "equipmentInstance", EquipmentTemporal::getTemporalInstance, EquipmentTemporal::setEquipmentInstance, EquipmentTemporal::setTemporalInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // PAGE URL
        if (pageURLExplicitlySet || !isNewVersion) {
            if (obj.getPageURL() != null) {
                createInnerElement(ElementType.PAGEURL, obj.getPageURL(), edmobj, overrideStatus);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.PAGEURL, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.EQUIPMENT.name(), edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void copyEquipmentCategoryRelations(String oldInstanceId, Equipment newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId("equipmentInstance", oldInstanceId, EquipmentCategory.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            EquipmentCategory oldRel = (EquipmentCategory) obj;
            EquipmentCategory newRel = new EquipmentCategory();
            newRel.setEquipmentInstance(newEdmobj);
            newRel.setCategoryInstance(oldRel.getCategoryInstance());
            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    private void copyEquipmentContactPointRelations(String oldInstanceId, Equipment newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId("equipmentInstance", oldInstanceId, EquipmentContactpoint.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            EquipmentContactpoint oldRel = (EquipmentContactpoint) obj;
            EquipmentContactpoint newRel = new EquipmentContactpoint();
            newRel.setEquipmentInstance(newEdmobj);
            newRel.setContactpointInstance(oldRel.getContactpointInstance());
            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    /**
     * Extracts instanceId from either a JPA entity (model.*) or a DTO (org.epos.eposdatamodel.*).
     * This is needed because RelationChecker can return either type.
     */
    private String extractInstanceId(Object obj) {
        if (obj == null) return null;

        try {
            // Try getInstanceId() method - works for both JPA entities and DTOs
            Method getInstanceId = obj.getClass().getMethod("getInstanceId");
            Object result = getInstanceId.invoke(obj);
            return result != null ? result.toString() : null;
        } catch (NoSuchMethodException e) {
            // Try alternative methods
            try {
                // Some entities might have different accessor names
                for (Method method : obj.getClass().getMethods()) {
                    if (method.getName().equals("getInstanceId") && method.getParameterCount() == 0) {
                        Object result = method.invoke(obj);
                        return result != null ? result.toString() : null;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void copyEquipmentIsPartOfRelations(String oldInstanceId, Equipment newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId("equipment", oldInstanceId, EquipmentIspartof.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            EquipmentIspartof oldRel = (EquipmentIspartof) obj;
            EquipmentIspartof newRel = new EquipmentIspartof();
            newRel.setEquipment(newEdmobj);
            newRel.setEquipmentInstanceId(newEdmobj.getInstanceId());
            newRel.setEntityInstanceId(oldRel.getEntityInstanceId());
            newRel.setResourceEntity(oldRel.getResourceEntity());
            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    private void deleteExistingElements(String equipmentInstanceId) {
        // FIX: Use getJoinEntitiesByRelationField which queries the @ManyToOne relationship field
        List<EquipmentElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("equipmentInstance", equipmentInstanceId, EquipmentElement.class);

        if (existingRelations != null) {
            for (EquipmentElement relation : existingRelations) {
                EposDataModelDAO.getInstance().deleteObject(relation);
                // Also delete the Element entity
                if (relation.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(relation.getElementInstance());
                }
                EposDataModelDAO.getInstance().deleteObject(relation);
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Equipment newEdmobj, ElementType elementType, StatusType overrideStatus) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("equipmentInstance", oldInstanceId, EquipmentElement.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            EquipmentElement oldRelation = (EquipmentElement) obj;
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus);
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Equipment edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("equipmentInstance", edmobj.getInstanceId(), EquipmentElement.class);
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
        if (edmobj.getVersion().getEditorId() != null) element.setEditorId(edmobj.getVersion().getEditorId());
        if (edmobj.getVersion().getProvenance() != null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if (edmobj.getVersion().getChangeComment() != null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if (edmobj.getVersion().getChangeTimestamp() != null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            EquipmentElement ce = new EquipmentElement();
            ce.setEquipmentInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    private boolean isFieldExplicitlySet(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                Object value = field.get(obj);
                return value != null;
            }
        } catch (Exception e) {
            // Fallback: assume not explicitly set
        }
        return false;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try {
                return clazz.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                clazz = clazz.getSuperclass();
            }
        }
        return null;
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
        for (Equipment object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getJoinEntitiesByParentId(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
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
        if (edmobj.getKeywords() != null && !edmobj.getKeywords().isEmpty())
            o.setKeywords(Arrays.asList(edmobj.getKeywords().split(",")));

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(), EquipmentCategory.class)) {
            EquipmentCategory item = (EquipmentCategory) object;
            o.addCategory(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(), EquipmentContactpoint.class)) {
            EquipmentContactpoint item = (EquipmentContactpoint) object;
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipment", edmobj.getInstanceId(), EquipmentIspartof.class)) {
            EquipmentIspartof item = (EquipmentIspartof) object;
            if (EntityNames.FACILITY.name().equals(item.getResourceEntity())) {
                o.addIsPartOf(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
            if (EntityNames.EQUIPMENT.name().equals(item.getResourceEntity())) {
                o.addIsPartOf(retrieveAPI(EntityNames.EQUIPMENT.name()).retrieveLinkedEntity(item.getEntityInstanceId()));
            }
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(), EquipmentSpatial.class)) {
            EquipmentSpatial item = (EquipmentSpatial) object;
            o.addSpatialExtentItem(retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(), EquipmentTemporal.class)) {
            EquipmentTemporal item = (EquipmentTemporal) object;
            o.addTemporalExtent(retrieveAPI(EntityNames.PERIODOFTIME.name()).retrieveLinkedEntity(item.getTemporalInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("equipmentInstance", edmobj.getInstanceId(), EquipmentElement.class)) {
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
        if (elementList != null && !elementList.isEmpty()) {
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