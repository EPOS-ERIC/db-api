package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class CategoryAPI extends AbstractAPI<org.epos.eposdatamodel.Category> {

    private static final Logger LOG = Logger.getLogger(CategoryAPI.class.getName());

    public CategoryAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Category obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId())!=null?retrieve(obj.getInstanceId()):null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) {
            searchInstanceId = null;
        }

        List<Category> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            Category selectedEntity = returnList.get(0);

            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);

            for (Category item : returnList) {
                if (item.getVersion() != null &&
                        targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }

            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Category) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Category edmobj = new Category();

        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName()+"/"+UUID.randomUUID().toString()));
        edmobj.setName(Optional.ofNullable(obj.getName()).orElse(""));
        edmobj.setDescription(Optional.ofNullable(obj.getDescription()).orElse(""));

        if (Objects.nonNull(obj.getInScheme())) createInscheme(obj.getInScheme(), edmobj, overrideStatus, obj.getFileProvenance());

        // =======================================================================
        // FIX: Filter self-references BEFORE processing relations
        // A category cannot be its own broader or narrower
        // =======================================================================
        List<LinkedEntity> safeBroader = filterSelfReferences(obj.getBroader(), edmobj.getUid(), edmobj.getInstanceId());
        List<LinkedEntity> safeNarrower = filterSelfReferences(obj.getNarrower(), edmobj.getUid(), edmobj.getInstanceId());

        // =======================================================================
        // BROADER (Complex Relation)
        // Convention in CategoryIspartof:
        //   - category1_instance_id = CHILD (the entity that has a broader)
        //   - category2_instance_id = PARENT (the broader category)
        //
        // When processing BROADER: edmobj is the CHILD, target is the PARENT
        // So: edmobj → category1, target → category2
        // =======================================================================
        if (Objects.nonNull(safeBroader) && !safeBroader.isEmpty()) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), safeBroader, relationFromUpdate, relationToUpdate,
                    CategoryIspartof.class, Category.class,
                    "category1Instance", // edmobj (child) field
                    CategoryIspartof::getCategory2Instance, // Target Getter (get parent/broader)
                    CategoryIspartof::setCategory1Instance, // Set edmobj as child
                    CategoryIspartof::setCategory2Instance, // Set target as parent/broader
                    obj, previousObj, overrideStatus, false
            );
        }

        // =======================================================================
        // NARROWER (Complex Relation)
        // When processing NARROWER: edmobj is the PARENT, target is the CHILD
        // So: edmobj → category2, target → category1
        // =======================================================================
        if (Objects.nonNull(safeNarrower) && !safeNarrower.isEmpty()) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), safeNarrower, relationFromUpdate, relationToUpdate,
                    CategoryIspartof.class, Category.class,
                    "category2Instance", // edmobj (parent) field
                    CategoryIspartof::getCategory1Instance, // Target Getter (get child/narrower)
                    CategoryIspartof::setCategory2Instance, // Set edmobj as parent
                    CategoryIspartof::setCategory1Instance, // Set target as child/narrower
                    obj, previousObj, overrideStatus, false
            );
        }

        getDbaccess().updateObject(edmobj);

        
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

    /**
     * Filters out self-references from a list of LinkedEntities.
     * A category cannot be broader or narrower of itself.
     */
    private List<LinkedEntity> filterSelfReferences(List<LinkedEntity> links, String selfUid, String selfInstanceId) {
        if (links == null || links.isEmpty()) {
            return links;
        }

        List<LinkedEntity> filtered = new ArrayList<>();
        for (LinkedEntity link : links) {
            boolean isSelfReference = false;

            // Check by UID
            if (selfUid != null && selfUid.equals(link.getUid())) {
                isSelfReference = true;
                LOG.warning("[CategoryAPI] Filtering self-reference by UID: " + selfUid);
            }
            // Check by instanceId
            else if (selfInstanceId != null && selfInstanceId.equals(link.getInstanceId())) {
                isSelfReference = true;
                LOG.warning("[CategoryAPI] Filtering self-reference by instanceId: " + selfInstanceId);
            }

            if (!isSelfReference) {
                filtered.add(link);
            }
        }
        return filtered;
    }

    private void createInscheme(LinkedEntity inscheme, Category edmobj, StatusType overrideStatus, String provenance){
        List<CategoryScheme> categorySchemeList = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(inscheme,CategoryScheme.class);
        if(!categorySchemeList.isEmpty()) {
            edmobj.setInScheme(categorySchemeList.get(0));
        } else{
            org.epos.eposdatamodel.CategoryScheme childObj = new org.epos.eposdatamodel.CategoryScheme();
            childObj.setInstanceId(inscheme.getInstanceId());
            childObj.setMetaId(inscheme.getMetaId());
            childObj.setUid(inscheme.getUid());
            if (edmobj.getVersion() != null && edmobj.getVersion().getStatus() != null) {
                childObj.setStatus(StatusType.valueOf(edmobj.getVersion().getStatus()));
            }
            // ... copy metadata ...
            LinkedEntity le = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(childObj, overrideStatus, null, null);
            edmobj.setInScheme((CategoryScheme) getDbaccess().getOneFromDBByLinkedEntity(le, CategoryScheme.class).get(0));
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("categoryInstance", instanceId, CategoryHastopconcept.class);
        deleteRelations("category1Instance", instanceId, CategoryIspartof.class);
        deleteRelations("category2Instance", instanceId, CategoryIspartof.class);
        deleteRelations("categoryInstance", instanceId, DataproductCategory.class);
        deleteRelations("categoryInstance", instanceId, WebserviceCategory.class);
        deleteRelations("categoryInstance", instanceId, SoftwareapplicationCategory.class);
        deleteRelations("categoryInstance", instanceId, SoftwaresourcecodeCategory.class);
        deleteRelations("categoryInstance", instanceId, FacilityCategory.class);
        deleteRelations("categoryInstance", instanceId, EquipmentCategory.class);

        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        for(Category object : elementList){
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if(list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Category retrieve(String instanceId) {
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Category edmobj = elementList.get(0);
        org.epos.eposdatamodel.Category o = new org.epos.eposdatamodel.Category();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setName(edmobj.getName());
        o.setDescription(edmobj.getDescription());
        if (edmobj.getInScheme() != null) {
            o.setInScheme(retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveLinkedEntity(edmobj.getInScheme().getInstanceId()));
        }

        ArrayList<LinkedEntity> broaders = new ArrayList<>();
        ArrayList<LinkedEntity> narrowers = new ArrayList<>();

        // =======================================================================
        // FIX: Corrected broader/narrower retrieval logic
        //
        // In CategoryIspartof table:
        //   - category1_instance_id = child (the entity that HAS a broader)
        //   - category2_instance_id = parent (the broader category)
        //
        // So to find BROADER: query where this entity is category1 (child),
        //                     return category2 (the parent/broader)
        //
        // And to find NARROWER: query where this entity is category2 (parent),
        //                       return category1 (the child/narrower)
        // =======================================================================

        // Find BROADER (parents): where this entity is the CHILD (category1Instance)
        // Return category2 which is the parent/broader
        for(Object obj : getDbaccess().getOneFromDBBySpecificKey("category1Instance", edmobj.getInstanceId(), CategoryIspartof.class)){
            CategoryIspartof item = (CategoryIspartof) obj;
            if (item.getCategory2Instance() != null) {
                LinkedEntity le = retrieveLinkedEntity(item.getCategory2Instance().getInstanceId());
                if (le != null) {
                    broaders.add(le);
                }
            }
        }

        // Find NARROWER (children): where this entity is the PARENT (category2Instance)
        // Return category1 which is the child/narrower
        for(Object obj : getDbaccess().getOneFromDBBySpecificKey("category2Instance", edmobj.getInstanceId(), CategoryIspartof.class)){
            CategoryIspartof item = (CategoryIspartof) obj;
            if (item.getCategory1Instance() != null) {
                LinkedEntity le = retrieveLinkedEntity(item.getCategory1Instance().getInstanceId());
                if (le != null) {
                    narrowers.add(le);
                }
            }
        }

        o.setBroader(broaders);
        o.setNarrower(narrowers);
        o = (org.epos.eposdatamodel.Category) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Category retrieveByUID(String uid) {
        List<Category> returnList = getDbaccess().getOneFromDBByUID(uid, Category.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.Category> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Category.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Category> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Category.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Category> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Category.class, status));
    }
    private List<org.epos.eposdatamodel.Category> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    private List<org.epos.eposdatamodel.Category> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, Category> categories = getDbaccess().batchFetchByInstanceIds(instanceIds, Category.class);
        if (categories.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> foundIds = new ArrayList<>(categories.keySet());
        
        // Broader: where this entity is category1 (child), return category2 (parent)
        Map<String, List<CategoryIspartof>> broaderRels = 
                getDbaccess().batchFetchRelationsForMultipleParents("category1Instance", foundIds, CategoryIspartof.class);
        // Narrower: where this entity is category2 (parent), return category1 (child)
        Map<String, List<CategoryIspartof>> narrowerRels = 
                getDbaccess().batchFetchRelationsForMultipleParents("category2Instance", foundIds, CategoryIspartof.class);
        
        Set<String> allRelatedCategoryIds = new HashSet<>();
        Set<String> allSchemeIds = new HashSet<>();
        
        broaderRels.values().forEach(list -> list.forEach(r -> {
            if (r.getCategory2Instance() != null) allRelatedCategoryIds.add(r.getCategory2Instance().getInstanceId());
        }));
        narrowerRels.values().forEach(list -> list.forEach(r -> {
            if (r.getCategory1Instance() != null) allRelatedCategoryIds.add(r.getCategory1Instance().getInstanceId());
        }));
        for (Category cat : categories.values()) {
            if (cat.getInScheme() != null) allSchemeIds.add(cat.getInScheme().getInstanceId());
        }
        
        Map<String, Category> relatedCategoryMap = allRelatedCategoryIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allRelatedCategoryIds), Category.class);
        Map<String, CategoryScheme> schemeMap = allSchemeIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allSchemeIds), CategoryScheme.class);
        
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = categories.values().stream()
                .map(Category::getMetaId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        List<org.epos.eposdatamodel.Category> results = new ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            Category edmobj = categories.get(instanceId);
            if (edmobj != null) {
                results.add(assembleCategory(instanceId, edmobj, broaderRels, narrowerRels, relatedCategoryMap, schemeMap, versioningMap, groupsMap));
            }
        }
        
        return results;
    }

    private org.epos.eposdatamodel.Category assembleCategory(
            String instanceId, Category edmobj,
            Map<String, List<CategoryIspartof>> broaderRels,
            Map<String, List<CategoryIspartof>> narrowerRels,
            Map<String, Category> relatedCategoryMap,
            Map<String, CategoryScheme> schemeMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
        org.epos.eposdatamodel.Category o = new org.epos.eposdatamodel.Category();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setName(edmobj.getName());
        o.setDescription(edmobj.getDescription());
        
        if (edmobj.getInScheme() != null) {
            CategoryScheme scheme = schemeMap.get(edmobj.getInScheme().getInstanceId());
            if (scheme != null) {
                o.setInScheme(createLinkedEntity(scheme, EntityNames.CATEGORYSCHEME.name()));
            }
        }
        
        ArrayList<LinkedEntity> broaders = new ArrayList<>();
        ArrayList<LinkedEntity> narrowers = new ArrayList<>();
        
        for (CategoryIspartof rel : broaderRels.getOrDefault(instanceId, Collections.emptyList())) {
            if (rel.getCategory2Instance() != null) {
                Category target = relatedCategoryMap.get(rel.getCategory2Instance().getInstanceId());
                if (target != null) {
                    broaders.add(createLinkedEntity(target, EntityNames.CATEGORY.name()));
                }
            }
        }
        
        for (CategoryIspartof rel : narrowerRels.getOrDefault(instanceId, Collections.emptyList())) {
            if (rel.getCategory1Instance() != null) {
                Category target = relatedCategoryMap.get(rel.getCategory1Instance().getInstanceId());
                if (target != null) {
                    narrowers.add(createLinkedEntity(target, EntityNames.CATEGORY.name()));
                }
            }
        }
        
        o.setBroader(broaders);
        o.setNarrower(narrowers);
        
        Versioningstatus vs = versioningMap.get(instanceId);
        if (vs != null) {
            o.setVersionId(vs.getVersionId());
            o.setInstanceChangedId(vs.getInstanceChangeId());
            if (vs.getChangeTimestamp() != null) o.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
            o.setEditorId(vs.getEditorId());
            o.setChangeComment(vs.getChangeComment());
            o.setVersion(vs.getVersion());
            if (vs.getStatus() != null) {
                try { o.setStatus(StatusType.valueOf(vs.getStatus())); } catch (Exception e) {}
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
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if(elementList!=null && !elementList.isEmpty()) {
            Category edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.CATEGORY.name());
            return o;
        }
        return null;
    }
}