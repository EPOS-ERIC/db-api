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
import usermanagementapis.UserGroupManagementAPI;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class FacilityAPI extends AbstractAPI<org.epos.eposdatamodel.Facility> {

    public FacilityAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Facility obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Capture if fields were explicitly set BEFORE any processing
        boolean categoryExplicitlySet = isFieldExplicitlySet(obj, "category");
        boolean contactPointExplicitlySet = isFieldExplicitlySet(obj, "contactPoint");
        boolean addressExplicitlySet = isFieldExplicitlySet(obj, "address");
        boolean isPartOfExplicitlySet = isFieldExplicitlySet(obj, "isPartOf");
        boolean spatialExtentExplicitlySet = isFieldExplicitlySet(obj, "spatialExtent");
        boolean pageURLExplicitlySet = isFieldExplicitlySet(obj, "pageURL");

        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

        String searchInstanceId = obj.getInstanceId();

        List<Facility> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Facility selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Facility::getVersion);
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
        CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);

        // CONTACTPOINT
        ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);

        // ADDRESS - Enable Store = True to allow creation of missing addresses
        if (obj.getAddress() != null && !obj.getAddress().isEmpty()) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getAddress(), relationFromUpdate, relationToUpdate,
                    FacilityAddress.class, Address.class,
                    "facilityInstance", FacilityAddress::getAddressInstance, FacilityAddress::setFacilityInstance, FacilityAddress::setAddressInstance,
                    obj, previousObj, overrideStatus, true
            );
        } else {
            deleteRelations("facilityInstance", edmobj.getInstanceId(), FacilityAddress.class);
        }

        // ISPARTOF (Facility -> Facility) - Enable Store = True to allow creation of missing facilities
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getIsPartOf(), relationFromUpdate, relationToUpdate,
                FacilityIspartof.class, Facility.class,
                "facility1Instance", FacilityIspartof::getFacility2Instance, FacilityIspartof::setFacility1Instance, FacilityIspartof::setFacility2Instance,
                obj, previousObj, overrideStatus, true
        );

        // SPATIAL - Enable Store = True
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getSpatialExtent(), relationFromUpdate, relationToUpdate,
                FacilitySpatial.class, Spatial.class,
                "facilityInstance", FacilitySpatial::getSpatialInstance, FacilitySpatial::setFacilityInstance, FacilitySpatial::setSpatialInstance,
                obj, previousObj, overrideStatus, true
        );

        // PAGE URL (list of strings)
        deleteExistingElements(edmobj.getInstanceId());
        if (obj.getPageURL() != null && !obj.getPageURL().isEmpty()) {
            for (String pageurl : obj.getPageURL()) {
                createInnerElement(ElementType.PAGEURL, pageurl, edmobj, overrideStatus);
            }
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.FACILITY.name(), edmobj);

        
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
        Versioningstatus version = edmobj.getVersion();
        if (version != null) {
            if (version.getEditorId() != null) element.setEditorId(version.getEditorId());
            if (version.getProvenance() != null) element.setFileProvenance(version.getProvenance());
            if (version.getChangeComment() != null) element.setChangeComment(version.getChangeComment());
            if (version.getChangeTimestamp() != null) element.setChangeTimestamp(version.getChangeTimestamp().toLocalDateTime());
        }

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            FacilityElement ce = new FacilityElement();
            ce.setFacilityInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    

    

    @Override
    public Boolean delete(String instanceId) {
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Facility.class, List.of(
                new EposDataModelDAO.RelationField(FacilityContactpoint.class, "facilityInstance"),
                new EposDataModelDAO.RelationField(FacilityIspartof.class, "facility1Instance"),
                new EposDataModelDAO.RelationField(FacilityElement.class, "facilityInstance"),
                new EposDataModelDAO.RelationField(FacilitySpatial.class, "facilityInstance"),
                new EposDataModelDAO.RelationField(FacilityCategory.class, "facilityInstance"),
                new EposDataModelDAO.RelationField(FacilityAddress.class, "facilityInstance")));
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
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    /**
     * Bulk retrieval implementation that minimizes database queries.
     */
    private List<org.epos.eposdatamodel.Facility> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Batch fetch all Facility entities
        Map<String, Facility> facilities = getDbaccess().batchFetchByInstanceIds(instanceIds, Facility.class);
        if (facilities.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> foundIds = new ArrayList<>(facilities.keySet());
        
        // Step 2: Batch fetch ALL join tables
        Map<String, List<FacilityCategory>> categories = 
                getDbaccess().batchFetchRelationsForMultipleParents("facilityInstance", foundIds, FacilityCategory.class);
        Map<String, List<FacilityContactpoint>> contactPoints = 
                getDbaccess().batchFetchRelationsForMultipleParents("facilityInstance", foundIds, FacilityContactpoint.class);
        Map<String, List<FacilityAddress>> addresses = 
                getDbaccess().batchFetchRelationsForMultipleParents("facilityInstance", foundIds, FacilityAddress.class);
        Map<String, List<FacilityIspartof>> isPartOfs = 
                getDbaccess().batchFetchRelationsForMultipleParents("facility1Instance", foundIds, FacilityIspartof.class);
        Map<String, List<FacilitySpatial>> spatials = 
                getDbaccess().batchFetchRelationsForMultipleParents("facilityInstance", foundIds, FacilitySpatial.class);
        Map<String, List<FacilityElement>> elements = 
                getDbaccess().batchFetchRelationsForMultipleParents("facilityInstance", foundIds, FacilityElement.class);
        
        // Step 3: Collect all target entity IDs
        Set<String> allCategoryIds = new HashSet<>();
        Set<String> allContactPointIds = new HashSet<>();
        Set<String> allAddressIds = new HashSet<>();
        Set<String> allParentFacilityIds = new HashSet<>();
        Set<String> allSpatialIds = new HashSet<>();
        
        categories.values().forEach(list -> list.forEach(r -> {
            if (r.getCategoryInstance() != null) allCategoryIds.add(r.getCategoryInstance().getInstanceId());
        }));
        contactPoints.values().forEach(list -> list.forEach(r -> {
            if (r.getContactpointInstance() != null) allContactPointIds.add(r.getContactpointInstance().getInstanceId());
        }));
        addresses.values().forEach(list -> list.forEach(r -> {
            if (r.getAddressInstance() != null) allAddressIds.add(r.getAddressInstance().getInstanceId());
        }));
        isPartOfs.values().forEach(list -> list.forEach(r -> {
            if (r.getFacility2Instance() != null) allParentFacilityIds.add(r.getFacility2Instance().getInstanceId());
        }));
        spatials.values().forEach(list -> list.forEach(r -> {
            if (r.getSpatialInstance() != null) allSpatialIds.add(r.getSpatialInstance().getInstanceId());
        }));
        
        // Step 4: Batch fetch all target entities
        Map<String, Category> categoryMap = allCategoryIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allCategoryIds), Category.class);
        Map<String, Contactpoint> contactPointMap = allContactPointIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allContactPointIds), Contactpoint.class);
        Map<String, Address> addressMap = allAddressIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allAddressIds), Address.class);
        Map<String, Facility> parentFacilityMap = allParentFacilityIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allParentFacilityIds), Facility.class);
        Map<String, Spatial> spatialMap = allSpatialIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allSpatialIds), Spatial.class);
        
        // Step 5: Batch fetch versioning status
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Step 5b: Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = facilities.values().stream()
                .map(Facility::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        // Step 6: Assemble DTOs
        List<org.epos.eposdatamodel.Facility> results = new ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            Facility edmobj = facilities.get(instanceId);
            if (edmobj != null) {
                org.epos.eposdatamodel.Facility dto = assembleFacility(
                        instanceId, edmobj, categories, contactPoints, addresses, isPartOfs, spatials, elements,
                        categoryMap, contactPointMap, addressMap, parentFacilityMap, spatialMap, versioningMap, groupsMap
                );
                results.add(dto);
            }
        }
        
        return results;
    }

    private org.epos.eposdatamodel.Facility assembleFacility(
            String instanceId,
            Facility edmobj,
            Map<String, List<FacilityCategory>> categories,
            Map<String, List<FacilityContactpoint>> contactPoints,
            Map<String, List<FacilityAddress>> addresses,
            Map<String, List<FacilityIspartof>> isPartOfs,
            Map<String, List<FacilitySpatial>> spatials,
            Map<String, List<FacilityElement>> elements,
            Map<String, Category> categoryMap,
            Map<String, Contactpoint> contactPointMap,
            Map<String, Address> addressMap,
            Map<String, Facility> parentFacilityMap,
            Map<String, Spatial> spatialMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
        org.epos.eposdatamodel.Facility o = new org.epos.eposdatamodel.Facility();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setIdentifier(edmobj.getIdentifier());
        o.setDescription(edmobj.getDescription());
        o.setTitle(edmobj.getTitle());
        if (edmobj.getKeywords() != null && !edmobj.getKeywords().isEmpty()) {
            o.setKeywords(Arrays.asList(edmobj.getKeywords().split(",")));
        }
        
        // Categories
        for (FacilityCategory rel : categories.getOrDefault(instanceId, Collections.emptyList())) {
            Category target = categoryMap.get(rel.getCategoryInstance().getInstanceId());
            if (target != null) {
                o.addCategory(createLinkedEntity(target, EntityNames.CATEGORY.name()));
            }
        }
        
        // ContactPoints
        for (FacilityContactpoint rel : contactPoints.getOrDefault(instanceId, Collections.emptyList())) {
            Contactpoint target = contactPointMap.get(rel.getContactpointInstance().getInstanceId());
            if (target != null) {
                o.addContactPoint(createLinkedEntity(target, EntityNames.CONTACTPOINT.name()));
            }
        }
        
        // Addresses
        for (FacilityAddress rel : addresses.getOrDefault(instanceId, Collections.emptyList())) {
            Address target = addressMap.get(rel.getAddressInstance().getInstanceId());
            if (target != null) {
                o.addAddress(createLinkedEntity(target, EntityNames.ADDRESS.name()));
            }
        }
        
        // IsPartOf
        for (FacilityIspartof rel : isPartOfs.getOrDefault(instanceId, Collections.emptyList())) {
            Facility target = parentFacilityMap.get(rel.getFacility2Instance().getInstanceId());
            if (target != null) {
                o.addIsPartOf(createLinkedEntity(target, EntityNames.FACILITY.name()));
            }
        }
        
        // Spatials
        for (FacilitySpatial rel : spatials.getOrDefault(instanceId, Collections.emptyList())) {
            Spatial target = spatialMap.get(rel.getSpatialInstance().getInstanceId());
            if (target != null) {
                o.addSpatialExtent(createLinkedEntity(target, EntityNames.LOCATION.name()));
            }
        }
        
        // Elements (pageURL)
        for (FacilityElement rel : elements.getOrDefault(instanceId, Collections.emptyList())) {
            Element el = rel.getElementInstance();
            if (el != null && ElementType.PAGEURL.name().equals(el.getType())) {
                o.addPageURL(el.getValue());
            }
        }
        
        // Apply versioning
        Versioningstatus vs = versioningMap.get(instanceId);
        if (vs != null) {
            o.setVersionId(vs.getVersionId());
            o.setInstanceChangedId(vs.getInstanceChangeId());
            if (vs.getChangeTimestamp() != null) {
                o.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
            }
            o.setEditorId(vs.getEditorId());
            o.setChangeComment(vs.getChangeComment());
            o.setVersion(vs.getVersion());
            if (vs.getStatus() != null) {
                try {
                    o.setStatus(StatusType.valueOf(vs.getStatus()));
                } catch (Exception e) {
                    // Ignore invalid status
                }
            }
            o.setFileProvenance(vs.getProvenance());
        }
        
        // Apply groups from pre-fetched data
        if (o.getMetaId() != null && groupsMap != null) {
            List<String> groups = groupsMap.get(o.getMetaId());
            o.setGroups(groups != null ? groups : Collections.emptyList());
        }
        
        return o;
    }

    private LinkedEntity createLinkedEntity(Object entity, String entityType) {
        LinkedEntity le = new LinkedEntity();
        le.setInstanceId(utilities.ReflectionCache.getInstanceId(entity));
        le.setMetaId(utilities.ReflectionCache.getMetaId(entity));
        le.setUid(utilities.ReflectionCache.getUid(entity));
        le.setEntityType(entityType);
        return le;
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
