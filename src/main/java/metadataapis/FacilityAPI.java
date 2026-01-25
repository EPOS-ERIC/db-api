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

import java.lang.reflect.Field;
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

        // Capture if fields were explicitly set BEFORE any processing
        boolean categoryExplicitlySet = isFieldExplicitlySet(obj, "category");
        boolean contactPointExplicitlySet = isFieldExplicitlySet(obj, "contactPoint");
        boolean addressExplicitlySet = isFieldExplicitlySet(obj, "address");
        boolean isPartOfExplicitlySet = isFieldExplicitlySet(obj, "isPartOf");
        boolean spatialExtentExplicitlySet = isFieldExplicitlySet(obj, "spatialExtent");
        boolean pageURLExplicitlySet = isFieldExplicitlySet(obj, "pageURL");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();

        List<Facility> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Facility selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Facility item : returnList) {
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

        obj = (org.epos.eposdatamodel.Facility) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        Facility edmobj = new Facility();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setType(obj.getType());
        edmobj.setIdentifier(obj.getIdentifier());
        edmobj.setDescription(obj.getDescription());
        edmobj.setTitle(obj.getTitle());
        if (obj.getKeywords() != null) edmobj.setKeywords(String.join(",", obj.getKeywords()));

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }

        // CATEGORY
        if (categoryExplicitlySet || !isNewVersion) {
            if (obj.getCategory() != null && !obj.getCategory().isEmpty()) {
                CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyFacilityCategoryRelations(oldInstanceId, edmobj);
        }

        // CONTACTPOINT
        if (contactPointExplicitlySet || !isNewVersion) {
            if (obj.getContactPoint() != null && !obj.getContactPoint().isEmpty()) {
                ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyFacilityContactPointRelations(oldInstanceId, edmobj);
        }

        // ADDRESS - Enable Store = True to allow creation of missing addresses
        if (addressExplicitlySet || !isNewVersion) {
            if (obj.getAddress() != null && !obj.getAddress().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getAddress(), relationFromUpdate, relationToUpdate,
                        FacilityAddress.class, Address.class,
                        "facilityInstance", FacilityAddress::getAddressInstance, FacilityAddress::setFacilityInstance, FacilityAddress::setAddressInstance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    FacilityAddress.class, Address.class,
                    "facilityInstance", FacilityAddress::getAddressInstance, FacilityAddress::setFacilityInstance, FacilityAddress::setAddressInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        // ISPARTOF (Facility -> Facility) - Enable Store = True to allow creation of missing facilities
        if (isPartOfExplicitlySet || !isNewVersion) {
            if (obj.getIsPartOf() != null && !obj.getIsPartOf().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getIsPartOf(), relationFromUpdate, relationToUpdate,
                        FacilityIspartof.class, Facility.class,
                        "facility1Instance", FacilityIspartof::getFacility2Instance, FacilityIspartof::setFacility1Instance, FacilityIspartof::setFacility2Instance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    FacilityIspartof.class, Facility.class,
                    "facility1Instance", FacilityIspartof::getFacility2Instance, FacilityIspartof::setFacility1Instance, FacilityIspartof::setFacility2Instance,
                    obj, previousObj, overrideStatus, true
            );
        }

        // SPATIAL - Enable Store = True
        if (spatialExtentExplicitlySet || !isNewVersion) {
            if (obj.getSpatialExtent() != null && !obj.getSpatialExtent().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                        FacilitySpatial.class, Spatial.class,
                        "facilityInstance", FacilitySpatial::getSpatialInstance, FacilitySpatial::setFacilityInstance, FacilitySpatial::setSpatialInstance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    FacilitySpatial.class, Spatial.class,
                    "facilityInstance", FacilitySpatial::getSpatialInstance, FacilitySpatial::setFacilityInstance, FacilitySpatial::setSpatialInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        // PAGE URL (list of strings)
        if (pageURLExplicitlySet || !isNewVersion) {
            if (obj.getPageURL() != null && !obj.getPageURL().isEmpty()) {
                for (String pageurl : obj.getPageURL()) {
                    createInnerElement(ElementType.PAGEURL, pageurl, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.PAGEURL, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.FACILITY.name(), edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void copyFacilityCategoryRelations(String oldInstanceId, Facility newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId("facilityInstance", oldInstanceId, FacilityCategory.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            FacilityCategory oldRel = (FacilityCategory) obj;
            FacilityCategory newRel = new FacilityCategory();
            newRel.setFacilityInstance(newEdmobj);
            newRel.setCategoryInstance(oldRel.getCategoryInstance());
            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    private void copyFacilityContactPointRelations(String oldInstanceId, Facility newEdmobj) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId("facilityInstance", oldInstanceId, FacilityContactpoint.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            FacilityContactpoint oldRel = (FacilityContactpoint) obj;
            FacilityContactpoint newRel = new FacilityContactpoint();
            newRel.setFacilityInstance(newEdmobj);
            newRel.setContactpointInstance(oldRel.getContactpointInstance());
            EposDataModelDAO.getInstance().updateObject(newRel);
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Facility newEdmobj, ElementType elementType, StatusType overrideStatus) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByParentId("facilityInstance", oldInstanceId, FacilityElement.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            FacilityElement oldRelation = (FacilityElement) obj;
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus);
            }
        }
    }

    private void deleteExistingElements(String facilityInstanceId) {
        // FIX: Use getJoinEntitiesByRelationField which queries the @ManyToOne relationship field
        List<FacilityElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("facilityInstance", facilityInstanceId, FacilityElement.class);

        if (existingRelations != null) {
            for (FacilityElement relation : existingRelations) {
                EposDataModelDAO.getInstance().deleteObject(relation);
                // Also delete the Element entity
                if (relation.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(relation.getElementInstance());
                }
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Facility edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("facilityInstance", edmobj.getInstanceId(), FacilityElement.class);
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
        if (edmobj.getVersion().getEditorId() != null) element.setEditorId(edmobj.getVersion().getEditorId());
        if (edmobj.getVersion().getProvenance() != null) element.setFileProvenance(edmobj.getVersion().getProvenance());
        if (edmobj.getVersion().getChangeComment() != null) element.setChangeComment(edmobj.getVersion().getChangeComment());
        if (edmobj.getVersion().getChangeTimestamp() != null) element.setChangeTimestamp(edmobj.getVersion().getChangeTimestamp().toLocalDateTime());

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            FacilityElement ce = new FacilityElement();
            ce.setFacilityInstance(edmobj);
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
        deleteRelations("facilityInstance", instanceId, FacilityContactpoint.class);
        deleteRelations("facility1Instance", instanceId, FacilityIspartof.class);
        deleteRelations("facilityInstance", instanceId, FacilityElement.class);
        deleteRelations("facilityInstance", instanceId, FacilitySpatial.class);
        deleteRelations("facilityInstance", instanceId, FacilityCategory.class);
        deleteRelations("facilityInstance", instanceId, FacilityAddress.class);

        List<Facility> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Facility.class);
        for (Facility object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getJoinEntitiesByParentId(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
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
        if (edmobj.getKeywords() != null && !edmobj.getKeywords().isEmpty())
            o.setKeywords(Arrays.asList(edmobj.getKeywords().split(",")));

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(), FacilityCategory.class)) {
            FacilityCategory item = (FacilityCategory) object;
            o.addCategory(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(), FacilityContactpoint.class)) {
            FacilityContactpoint item = (FacilityContactpoint) object;
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(item.getContactpointInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(), FacilityAddress.class)) {
            FacilityAddress item = (FacilityAddress) object;
            o.addAddress(retrieveAPI(EntityNames.ADDRESS.name()).retrieveLinkedEntity(item.getAddressInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facility1Instance", edmobj.getInstanceId(), FacilityIspartof.class)) {
            FacilityIspartof item = (FacilityIspartof) object;
            o.addIsPartOf(retrieveAPI(EntityNames.FACILITY.name()).retrieveLinkedEntity(item.getFacility2Instance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(), FacilitySpatial.class)) {
            FacilitySpatial item = (FacilitySpatial) object;
            o.addSpatialExtent(retrieveAPI(EntityNames.LOCATION.name()).retrieveLinkedEntity(item.getSpatialInstance().getInstanceId()));
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("facilityInstance", edmobj.getInstanceId(), FacilityElement.class)) {
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
        if (elementList != null && !elementList.isEmpty()) {
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