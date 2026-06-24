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

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * CategorySchemeAPI - Manages CategoryScheme entities with proper support for:
 * - CategoryScheme-Category relations (topConcepts) via CategoryHastopconcept
 * - REFERENCE_ENTITY logic: CategorySchemes are shared entities
 */
public class CategorySchemeAPI extends AbstractAPI<org.epos.eposdatamodel.CategoryScheme> {

    private static final Logger LOG = Logger.getLogger(CategorySchemeAPI.class.getName());

    public CategorySchemeAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.CategoryScheme obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Capture if fields were explicitly set BEFORE any processing
        boolean topConceptsExplicitlySet = isFieldExplicitlySet(obj, "topConcepts");

        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

        String searchInstanceId = obj.getInstanceId();

        List<CategoryScheme> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            CategoryScheme selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (CategoryScheme item : returnList) {
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

        obj = (org.epos.eposdatamodel.CategoryScheme) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        CategoryScheme edmobj = new CategoryScheme();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setName(Optional.ofNullable(obj.getTitle()).orElse(""));
        edmobj.setDescription(Optional.ofNullable(obj.getDescription()).orElse(""));
        edmobj.setCode(Optional.ofNullable(obj.getCode()).orElse(""));
        edmobj.setColor(Optional.ofNullable(obj.getColor()).orElse(""));
        edmobj.setHomepage(Optional.ofNullable(obj.getHomepage()).orElse(""));
        edmobj.setOrderitemnumber(Optional.ofNullable(obj.getOrderitemnumber()).orElse(""));
        edmobj.setLogo(Optional.ofNullable(obj.getLogo()).orElse(""));

        // Determine if we should use REFERENCE_ENTITY logic for Categories
        boolean useReferenceEntityLogic = shouldApplyReferenceEntityLogic(obj);

        // TOP CONCEPTS (relation to Category - which is a REFERENCE_ENTITY)
        if (topConceptsExplicitlySet || !isNewVersion) {
            if (Objects.nonNull(obj.getTopConcepts()) && !obj.getTopConcepts().isEmpty()) {
                syncTopConceptsRelations(edmobj, obj.getTopConcepts(), overrideStatus, useReferenceEntityLogic);
            } else if (!isNewVersion) {
                // Empty list explicitly set - clear relations
                clearTopConceptsRelations(edmobj.getInstanceId());
            }
        } else if (isNewVersion && oldInstanceId != null) {
            // New version without explicit topConcepts - copy from previous
            copyTopConceptsFromPrevious(oldInstanceId, edmobj, useReferenceEntityLogic);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.CATEGORYSCHEME.name(), edmobj);

        
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
     * Determines if REFERENCE_ENTITY logic should be applied.
     */
    private boolean shouldApplyReferenceEntityLogic(org.epos.eposdatamodel.CategoryScheme obj) {
        if (obj == null) return true;
        String editorId = obj.getEditorId();
        if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) {
            LOG.log(Level.FINE, "[CategorySchemeAPI] INGESTOR MODE: Treating Categories as normal entities");
            return false;
        }
        return true;
    }

    /**
     * Syncs topConcepts relations (CategoryScheme -> Categories).
     * For REFERENCE_ENTITY mode: uses PUBLISHED versions of Categories.
     */
    private void syncTopConceptsRelations(CategoryScheme schemeEntity, List<LinkedEntity> topConceptLinks,
                                          StatusType overrideStatus, boolean useReferenceEntityLogic) {
        // Delete existing topConcepts relations
        clearTopConceptsRelations(schemeEntity.getInstanceId());

        if (topConceptLinks == null || topConceptLinks.isEmpty()) return;

        for (LinkedEntity link : topConceptLinks) {
            Category categoryEntity = findOrCreateCategoryForRelation(link, overrideStatus, useReferenceEntityLogic);

            if (categoryEntity != null) {
                CategoryHastopconcept relation = new CategoryHastopconcept();

                CategoryHastopconceptId id = new CategoryHastopconceptId();
                id.setCategorySchemeInstanceId(schemeEntity.getInstanceId());
                id.setCategoryInstanceId(categoryEntity.getInstanceId());
                relation.setId(id);

                relation.setCategorySchemeInstance(schemeEntity);
                relation.setCategoryInstance(categoryEntity);

                LOG.log(Level.FINE, "[CategorySchemeAPI] Creating topConcept relation: {0} -> {1}",
                        new Object[]{schemeEntity.getUid(), categoryEntity.getUid()});

                try {
                    getDbaccess().createObject(relation);
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                            e.getMessage().contains("already exists"))) {
                        LOG.log(Level.FINE, "[CategorySchemeAPI] TopConcept relation already exists, skipping");
                    } else {
                        LOG.log(Level.WARNING, "[CategorySchemeAPI] Error creating topConcept relation: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Clears all topConcepts relations for a CategoryScheme.
     */
    private void clearTopConceptsRelations(String schemeInstanceId) {
        List<Object> existing = getDbaccess().getJoinEntitiesByParentId("categorySchemeInstanceId", schemeInstanceId, CategoryHastopconcept.class);
        if (existing != null) {
            for (Object o : existing) getDbaccess().deleteObject(o);
        }
    }

    /**
     * Finds or creates a Category for relation purposes.
     * For REFERENCE_ENTITY mode: always returns the PUBLISHED version.
     */
    private Category findOrCreateCategoryForRelation(LinkedEntity link, StatusType status, boolean useReferenceEntityLogic) {

        // First, try to find by UID
        if (link.getUid() != null) {
            List<Category> byUid = getDbaccess().getOneFromDBByUID(link.getUid(), Category.class);
            if (!byUid.isEmpty()) {
                if (useReferenceEntityLogic) {
                    // Find PUBLISHED version
                    Category published = findPublishedCategory(link.getUid());
                    if (published != null) {
                        LOG.log(Level.FINE, "[CategorySchemeAPI] REFERENCE_ENTITY: Using PUBLISHED Category for uid=" + link.getUid());
                        return published;
                    }
                    LOG.log(Level.WARNING, "[CategorySchemeAPI] No PUBLISHED Category found for uid=" + link.getUid() +
                            ", using existing version");
                    return byUid.get(0);
                } else {
                    // Ingestor mode: find matching status
                    for (Category c : byUid) {
                        if (c.getVersion() != null && status != null &&
                                status.name().equals(c.getVersion().getStatus())) {
                            return c;
                        }
                    }
                    return byUid.get(0);
                }
            }
        }

        // Try by instanceId
        if (link.getInstanceId() != null) {
            List<Category> byInstanceId = getDbaccess().getOneFromDBByInstanceId(link.getInstanceId(), Category.class);
            if (!byInstanceId.isEmpty()) {
                Category found = byInstanceId.get(0);
                if (useReferenceEntityLogic && found.getUid() != null) {
                    Category published = findPublishedCategory(found.getUid());
                    if (published != null) {
                        return published;
                    }
                }
                return found;
            }
        }

        // Try by LinkedEntity lookup
        List<Category> found = getDbaccess().getOneFromDBByLinkedEntity(link, Category.class);
        if (!found.isEmpty()) {
            if (useReferenceEntityLogic && found.get(0).getUid() != null) {
                Category published = findPublishedCategory(found.get(0).getUid());
                if (published != null) {
                    return published;
                }
            }
            return found.get(0);
        }

        // Category doesn't exist - create a stub
        LOG.log(Level.FINE, "[CategorySchemeAPI] Creating stub Category for uid=" + link.getUid());
        return createCategoryStub(link, status, useReferenceEntityLogic);
    }

    /**
     * Finds the PUBLISHED version of a Category by UID.
     */
    private Category findPublishedCategory(String uid) {
        if (uid == null) return null;

        List<Category> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, Category.class);

        for (Category cat : allVersions) {
            if (cat.getVersion() != null && StatusType.PUBLISHED.name().equals(cat.getVersion().getStatus())) {
                return cat;
            }
        }
        return null;
    }

    /**
     * Creates a stub Category entity.
     * For REFERENCE_ENTITY mode: always creates with PUBLISHED status.
     */
    private Category createCategoryStub(LinkedEntity link, StatusType status, boolean useReferenceEntityLogic) {
        String instanceId = UUID.randomUUID().toString();
        String metaId = UUID.randomUUID().toString();
        String versionId = UUID.randomUUID().toString();

        // REFERENCE_ENTITY: Always create as PUBLISHED
        StatusType finalStatus;
        if (useReferenceEntityLogic) {
            finalStatus = StatusType.PUBLISHED;
            LOG.log(Level.FINE, "[CategorySchemeAPI] REFERENCE_ENTITY: Creating stub Category as PUBLISHED");
        } else {
            finalStatus = status != null ? status : StatusType.DRAFT;
        }

        model.Versioningstatus vs = new model.Versioningstatus();
        vs.setInstanceId(instanceId);
        vs.setMetaId(metaId);
        vs.setVersionId(versionId);
        vs.setUid(link.getUid());
        vs.setStatus(finalStatus.toString());
        vs.setChangeTimestamp(java.time.OffsetDateTime.now());
        vs.setChangeComment("Auto-created stub for pending relation");

        getDbaccess().createObject(vs);

        Category stub = new Category();
        stub.setUid(link.getUid());
        stub.setInstanceId(instanceId);
        stub.setMetaId(metaId);
        stub.setVersion(vs);

        getDbaccess().createObject(stub);

        commonapis.EposDataModelEntityIDAPI.addEntityToEDMEntityID(metaId, EntityNames.CATEGORY.name());

        LOG.log(Level.FINE, "[CategorySchemeAPI] Created stub Category: {0} with status {1}",
                new Object[]{link.getUid(), finalStatus});
        return stub;
    }

    /**
     * Copies topConcepts relations from a previous version.
     */
    private void copyTopConceptsFromPrevious(String oldSchemeId, CategoryScheme newScheme, boolean useReferenceEntityLogic) {
        List<Object> existing = getDbaccess().getJoinEntitiesByParentId("categorySchemeInstanceId", oldSchemeId, CategoryHastopconcept.class);
        if (existing == null || existing.isEmpty()) return;

        for (Object o : existing) {
            CategoryHastopconcept oldRel = (CategoryHastopconcept) o;
            Category categoryEntity = oldRel.getCategoryInstance();

            // For REFERENCE_ENTITY: ensure we're using the PUBLISHED version
            if (useReferenceEntityLogic && categoryEntity.getUid() != null) {
                Category published = findPublishedCategory(categoryEntity.getUid());
                if (published != null) {
                    categoryEntity = published;
                }
            }

            CategoryHastopconcept newRel = new CategoryHastopconcept();

            CategoryHastopconceptId id = new CategoryHastopconceptId();
            id.setCategorySchemeInstanceId(newScheme.getInstanceId());
            id.setCategoryInstanceId(categoryEntity.getInstanceId());
            newRel.setId(id);

            newRel.setCategorySchemeInstance(newScheme);
            newRel.setCategoryInstance(categoryEntity);

            try {
                getDbaccess().createObject(newRel);
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                        e.getMessage().contains("already exists"))) {
                    LOG.log(Level.FINE, "[CategorySchemeAPI] TopConcept relation already exists during copy, skipping");
                } else {
                    LOG.log(Level.WARNING, "[CategorySchemeAPI] Error copying topConcept relation: " + e.getMessage());
                }
            }
        }
    }

    

    

    @Override
    public Boolean delete(String instanceId) {

        List<org.epos.eposdatamodel.Category> categories = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name()).retrieveAll();
        for(org.epos.eposdatamodel.Category category : categories) {
            if(category.getInScheme() != null && category.getInScheme().getInstanceId().equals(instanceId)) {
                category.setInScheme(null);
                AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name()).create(category,null,null,null);
            }
        }

        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        for (CategoryScheme object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    @Override
    public org.epos.eposdatamodel.CategoryScheme retrieve(String instanceId) {
        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        if (elementList == null || elementList.isEmpty()) return null;

        CategoryScheme edmobj = elementList.get(0);
        org.epos.eposdatamodel.CategoryScheme o = new org.epos.eposdatamodel.CategoryScheme();

        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setTitle(edmobj.getName());
        o.setDescription(edmobj.getDescription());
        o.setCode(edmobj.getCode());
        o.setHomepage(edmobj.getHomepage());
        o.setLogo(edmobj.getLogo());
        o.setColor(edmobj.getColor());
        o.setOrderitemnumber(edmobj.getOrderitemnumber());

        // Retrieve topConcepts
        List<Object> topConceptRels = getDbaccess().getJoinEntitiesByParentId("categorySchemeInstanceId", edmobj.getInstanceId(), CategoryHastopconcept.class);
        if (topConceptRels != null) {
            for (Object obj : topConceptRels) {
                CategoryHastopconcept item = (CategoryHastopconcept) obj;
                if (item.getCategoryInstance() != null) {
                    o.addTopConcepts(AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name())
                            .retrieveLinkedEntity(item.getCategoryInstance().getInstanceId()));
                }
            }
        }

        o = (org.epos.eposdatamodel.CategoryScheme) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.CategoryScheme retrieveByUID(String uid) {
        List<CategoryScheme> returnList = getDbaccess().getOneFromDBByUID(uid, CategoryScheme.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }

    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, CategoryScheme.class));
    }

    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(CategoryScheme.class));
    }

    @Override
    public List<org.epos.eposdatamodel.CategoryScheme> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(CategoryScheme.class, status));
    }

    private List<org.epos.eposdatamodel.CategoryScheme> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    private List<org.epos.eposdatamodel.CategoryScheme> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, model.CategoryScheme> schemes = getDbaccess().batchFetchByInstanceIds(instanceIds, model.CategoryScheme.class);
        if (schemes.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> foundIds = new ArrayList<>(schemes.keySet());
        
        Map<String, List<CategoryHastopconcept>> topConcepts = 
                getDbaccess().batchFetchRelationsForMultipleParents("categorySchemeInstance", foundIds, CategoryHastopconcept.class);
        
        Set<String> allCategoryIds = new HashSet<>();
        topConcepts.values().forEach(list -> list.forEach(r -> {
            if (r.getCategoryInstance() != null) allCategoryIds.add(r.getCategoryInstance().getInstanceId());
        }));
        
        Map<String, Category> categoryMap = allCategoryIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allCategoryIds), Category.class);
        
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = schemes.values().stream()
                .map(CategoryScheme::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        List<org.epos.eposdatamodel.CategoryScheme> results = new ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            model.CategoryScheme edmobj = schemes.get(instanceId);
            if (edmobj != null) {
                results.add(assembleCategoryScheme(instanceId, edmobj, topConcepts, categoryMap, versioningMap, groupsMap));
            }
        }
        
        return results;
    }

    private org.epos.eposdatamodel.CategoryScheme assembleCategoryScheme(
            String instanceId, model.CategoryScheme edmobj,
            Map<String, List<CategoryHastopconcept>> topConcepts,
            Map<String, Category> categoryMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
        org.epos.eposdatamodel.CategoryScheme o = new org.epos.eposdatamodel.CategoryScheme();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setTitle(edmobj.getName());
        o.setDescription(edmobj.getDescription());
        o.setCode(edmobj.getCode());
        o.setHomepage(edmobj.getHomepage());
        o.setLogo(edmobj.getLogo());
        o.setColor(edmobj.getColor());
        o.setOrderitemnumber(edmobj.getOrderitemnumber());
        
        for (CategoryHastopconcept rel : topConcepts.getOrDefault(instanceId, Collections.emptyList())) {
            Category target = categoryMap.get(rel.getCategoryInstance().getInstanceId());
            if (target != null) {
                o.addTopConcepts(createLinkedEntity(target, EntityNames.CATEGORY.name()));
            }
        }
        
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
        List<CategoryScheme> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, CategoryScheme.class);
        if (elementList != null && !elementList.isEmpty()) {
            CategoryScheme edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.CATEGORYSCHEME.name());
            return o;
        }
        return null;
    }
}