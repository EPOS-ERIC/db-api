package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * CategoryAPI - Manages Category entities with proper support for:
 * - Category-Category relations (broader/narrower) via CategoryIspartof
 * - Category-CategoryScheme relations (inScheme)
 * - REFERENCE_ENTITY logic: Categories are shared entities, always use PUBLISHED versions
 *   for relations unless editorId is "ingestor"
 */
public class CategoryAPI extends AbstractAPI<org.epos.eposdatamodel.Category> {

    private static final Logger LOG = Logger.getLogger(CategoryAPI.class.getName());

    public CategoryAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Category obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        boolean broaderExplicitlySet = isFieldExplicitlySet(obj, "broader");
        boolean narrowerExplicitlySet = isFieldExplicitlySet(obj, "narrower");
        boolean inSchemeExplicitlySet = isFieldExplicitlySet(obj, "inScheme");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

        String searchInstanceId = obj.getInstanceId();

        List<Category> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Category selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Category item : returnList) {
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

        obj = (org.epos.eposdatamodel.Category) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        Category edmobj = new Category();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());

        // Determine if we should use REFERENCE_ENTITY logic
        // (always use PUBLISHED for Category relations unless ingestor)
        boolean useReferenceEntityLogic = shouldApplyReferenceEntityLogic(obj);

        // IN SCHEME (CategoryScheme is also a REFERENCE_ENTITY)
        if (inSchemeExplicitlySet || !isNewVersion) {
            if (obj.getInScheme() != null) {
                createInscheme(obj.getInScheme(), edmobj, overrideStatus, useReferenceEntityLogic);
            }
        } else if (isNewVersion && oldInstanceId != null && previousObj instanceof org.epos.eposdatamodel.Category) {
            org.epos.eposdatamodel.Category oldPojo = (org.epos.eposdatamodel.Category) previousObj;
            if (oldPojo.getInScheme() != null) {
                createInscheme(oldPojo.getInScheme(), edmobj, overrideStatus, useReferenceEntityLogic);
            }
        }

        // BROADER (Category-to-Category relation)
        // This entity OWNS its broader relations (where it is the child pointing to parent)
        if (broaderExplicitlySet) {
            // Explicitly set: sync the relations (delete old, create new)
            syncBroaderRelations(edmobj, obj.getBroader(), overrideStatus, useReferenceEntityLogic, true);
        } else if (isUpdate && !isNewVersion) {
            // Updating existing entity without explicit broader: keep existing relations
            // (don't delete relations that may have been set previously)
        } else if (isNewVersion && oldInstanceId != null) {
            // New version: copy relations from previous version
            copyBroaderFromPrevious(oldInstanceId, edmobj, useReferenceEntityLogic);
        }
        // For first-time creation without explicit broader: nothing to do (no relations exist yet)

        // NARROWER (Category-to-Category relation - inverse of broader)
        // This entity does NOT own narrower relations - children own them via their broader
        // Only sync if explicitly set - otherwise don't touch relations created by children
        if (narrowerExplicitlySet) {
            // Explicitly set: sync the relations
            syncNarrowerRelations(edmobj, obj.getNarrower(), overrideStatus, useReferenceEntityLogic, true);
        } else if (isNewVersion && oldInstanceId != null) {
            // New version: copy relations from previous version
            copyNarrowerFromPrevious(oldInstanceId, edmobj, useReferenceEntityLogic);
        }
        // IMPORTANT: For first-time creation or update without explicit narrower,
        // do NOT touch narrower relations - they are managed by child categories via their broader

        getDbaccess().updateObject(edmobj);
        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.CATEGORY.name(), edmobj);

        return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.CATEGORY.name());
    }

    /**
     * Determines if REFERENCE_ENTITY logic should be applied.
     * When true: use existing PUBLISHED versions for relations (shared entities)
     * When false: cascade/create new versions normally (ingestor mode)
     */
    private boolean shouldApplyReferenceEntityLogic(org.epos.eposdatamodel.Category obj) {
        if (obj == null) return true;
        String editorId = obj.getEditorId();
        if (editorId != null && "ingestor".equalsIgnoreCase(editorId.trim())) {
            LOG.log(Level.FINE, "[CategoryAPI] INGESTOR MODE: Treating Categories as normal entities (cascade enabled)");
            return false;
        }
        return true;
    }

    /**
     * Creates/updates the inScheme relationship.
     * For REFERENCE_ENTITY mode: always uses the PUBLISHED version of CategoryScheme.
     */
    private void createInscheme(LinkedEntity inscheme, Category edmobj, StatusType overrideStatus, boolean useReferenceEntityLogic) {

        // First, try to find existing CategoryScheme
        List<CategoryScheme> categorySchemeList = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(inscheme, CategoryScheme.class);

        CategoryScheme targetScheme = null;

        if (!categorySchemeList.isEmpty()) {
            if (useReferenceEntityLogic) {
                // REFERENCE_ENTITY: Find the PUBLISHED version
                targetScheme = findPublishedCategoryScheme(inscheme.getUid());
                if (targetScheme == null) {
                    // No PUBLISHED exists, use what we have but log warning
                    targetScheme = categorySchemeList.get(0);
                    LOG.log(Level.WARNING, "[CategoryAPI] No PUBLISHED CategoryScheme found for uid=" + inscheme.getUid() +
                            ", using existing version with status=" +
                            (targetScheme.getVersion() != null ? targetScheme.getVersion().getStatus() : "unknown"));
                }
            } else {
                // Ingestor mode: use matching version
                targetScheme = categorySchemeList.get(0);
            }
        } else {
            // CategoryScheme doesn't exist - create it
            org.epos.eposdatamodel.CategoryScheme childObj = new org.epos.eposdatamodel.CategoryScheme();

            String instanceId = inscheme.getInstanceId() != null ? inscheme.getInstanceId() : UUID.randomUUID().toString();
            String metaId = inscheme.getMetaId() != null ? inscheme.getMetaId() : UUID.randomUUID().toString();

            childObj.setInstanceId(instanceId);
            childObj.setMetaId(metaId);
            childObj.setUid(inscheme.getUid());

            // REFERENCE_ENTITY: Always create as PUBLISHED
            StatusType status;
            if (useReferenceEntityLogic) {
                status = StatusType.PUBLISHED;
                LOG.log(Level.FINE, "[CategoryAPI] REFERENCE_ENTITY: Creating CategoryScheme as PUBLISHED");
            } else {
                status = overrideStatus;
                if (status == null && edmobj.getVersion() != null && edmobj.getVersion().getStatus() != null) {
                    try {
                        status = StatusType.valueOf(edmobj.getVersion().getStatus());
                    } catch (Exception e) {
                        status = StatusType.DRAFT;
                    }
                }
                if (status == null) status = StatusType.DRAFT;
            }
            childObj.setStatus(status);

            LinkedEntity le = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(childObj, status, null, null);

            List<CategoryScheme> created = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(le, CategoryScheme.class);
            if (!created.isEmpty()) {
                targetScheme = created.get(0);
            }
        }

        if (targetScheme != null) {
            edmobj.setInScheme(targetScheme);
        }
    }

    /**
     * Finds the PUBLISHED version of a CategoryScheme by UID.
     */
    private CategoryScheme findPublishedCategoryScheme(String uid) {
        if (uid == null) return null;

        List<CategoryScheme> allVersions = EposDataModelDAO.getInstance().getOneFromDBByUIDNoCache(uid, CategoryScheme.class);

        for (CategoryScheme cs : allVersions) {
            if (cs.getVersion() != null && StatusType.PUBLISHED.name().equals(cs.getVersion().getStatus())) {
                return cs;
            }
        }
        return null;
    }

    /**
     * Syncs broader relations (Category -> parent Category).
     * CategoryIspartof: category1 = child (this), category2 = parent (broader)
     *
     * @param deleteExisting if true, delete existing broader relations before creating new ones
     */
    private void syncBroaderRelations(Category childEntity, List<LinkedEntity> broaderLinks, StatusType overrideStatus, boolean useReferenceEntityLogic, boolean deleteExisting) {
        // Only delete existing broader relations if requested
        if (deleteExisting) {
            List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category1Instance", childEntity.getInstanceId(), CategoryIspartof.class);
            if (existing != null) {
                for (Object o : existing) getDbaccess().deleteObject(o);
            }
        }

        if (broaderLinks == null || broaderLinks.isEmpty()) return;

        for (LinkedEntity link : broaderLinks) {
            Category parentEntity = findOrCreateCategoryForRelation(link, overrideStatus, useReferenceEntityLogic);

            if (parentEntity != null) {
                // Prevent self-reference
                if (parentEntity.getInstanceId().equals(childEntity.getInstanceId())) {
                    LOG.log(Level.WARNING, "[CategoryAPI] Skipping self-reference in broader for category: " + childEntity.getUid());
                    continue;
                }

                CategoryIspartof relation = new CategoryIspartof();

                CategoryIspartofId id = new CategoryIspartofId();
                id.setCategory1InstanceId(childEntity.getInstanceId());
                id.setCategory2InstanceId(parentEntity.getInstanceId());
                relation.setId(id);

                relation.setCategory1Instance(childEntity);
                relation.setCategory2Instance(parentEntity);

                LOG.log(Level.FINE, "[CategoryAPI] Creating broader relation: {0} -> {1}",
                        new Object[]{childEntity.getUid(), parentEntity.getUid()});

                try {
                    getDbaccess().createObject(relation);
                } catch (Exception e) {
                    // Handle duplicate key gracefully
                    if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                            e.getMessage().contains("already exists"))) {
                        LOG.log(Level.FINE, "[CategoryAPI] Broader relation already exists, skipping");
                    } else {
                        LOG.log(Level.WARNING, "[CategoryAPI] Error creating broader relation: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Syncs narrower relations (parent Category -> this Category as child).
     * CategoryIspartof: category1 = narrower (child), category2 = this (parent)
     *
     * @param deleteExisting if true, delete existing narrower relations before creating new ones
     */
    private void syncNarrowerRelations(Category parentEntity, List<LinkedEntity> narrowerLinks, StatusType overrideStatus, boolean useReferenceEntityLogic, boolean deleteExisting) {
        // Only delete existing narrower relations if requested
        if (deleteExisting) {
            List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category2Instance", parentEntity.getInstanceId(), CategoryIspartof.class);
            if (existing != null) {
                for (Object o : existing) getDbaccess().deleteObject(o);
            }
        }

        if (narrowerLinks == null || narrowerLinks.isEmpty()) return;

        for (LinkedEntity link : narrowerLinks) {
            Category childEntity = findOrCreateCategoryForRelation(link, overrideStatus, useReferenceEntityLogic);

            if (childEntity != null) {
                // Prevent self-reference
                if (childEntity.getInstanceId().equals(parentEntity.getInstanceId())) {
                    LOG.log(Level.WARNING, "[CategoryAPI] Skipping self-reference in narrower for category: " + parentEntity.getUid());
                    continue;
                }

                CategoryIspartof relation = new CategoryIspartof();

                CategoryIspartofId id = new CategoryIspartofId();
                id.setCategory1InstanceId(childEntity.getInstanceId());
                id.setCategory2InstanceId(parentEntity.getInstanceId());
                relation.setId(id);

                relation.setCategory1Instance(childEntity);
                relation.setCategory2Instance(parentEntity);

                LOG.log(Level.FINE, "[CategoryAPI] Creating narrower relation: {0} <- {1}",
                        new Object[]{parentEntity.getUid(), childEntity.getUid()});

                try {
                    getDbaccess().createObject(relation);
                } catch (Exception e) {
                    if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                            e.getMessage().contains("already exists"))) {
                        LOG.log(Level.FINE, "[CategoryAPI] Narrower relation already exists, skipping");
                    } else {
                        LOG.log(Level.WARNING, "[CategoryAPI] Error creating narrower relation: " + e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Finds or creates a Category for relation purposes.
     * For REFERENCE_ENTITY mode: always returns the PUBLISHED version.
     */
    private Category findOrCreateCategoryForRelation(LinkedEntity link, StatusType status, boolean useReferenceEntityLogic) {

        // First, try to find by UID (most reliable for finding all versions)
        if (link.getUid() != null) {
            List<Category> byUid = getDbaccess().getOneFromDBByUID(link.getUid(), Category.class);
            if (!byUid.isEmpty()) {
                if (useReferenceEntityLogic) {
                    // Find PUBLISHED version
                    Category published = findPublishedCategory(link.getUid());
                    if (published != null) {
                        LOG.log(Level.FINE, "[CategoryAPI] REFERENCE_ENTITY: Using PUBLISHED Category for uid=" + link.getUid());
                        return published;
                    }
                    // No PUBLISHED - return first found but log warning
                    LOG.log(Level.WARNING, "[CategoryAPI] No PUBLISHED Category found for uid=" + link.getUid() +
                            ", using existing version");
                    return byUid.get(0);
                } else {
                    // Ingestor mode: find matching status or first
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
                    // Still need to check if there's a PUBLISHED version
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
        LOG.log(Level.FINE, "[CategoryAPI] Creating stub Category for uid=" + link.getUid());
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
            LOG.log(Level.FINE, "[CategoryAPI] REFERENCE_ENTITY: Creating stub Category as PUBLISHED");
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

        LOG.log(Level.FINE, "[CategoryAPI] Created stub Category: {0} with status {1}",
                new Object[]{link.getUid(), finalStatus});
        return stub;
    }

    /**
     * Copies broader relations from a previous version.
     */
    private void copyBroaderFromPrevious(String oldId, Category newEntity, boolean useReferenceEntityLogic) {
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category1Instance", oldId, CategoryIspartof.class);
        if (existing == null || existing.isEmpty()) return;

        for (Object o : existing) {
            CategoryIspartof oldRel = (CategoryIspartof) o;
            Category parentCategory = oldRel.getCategory2Instance();

            // For REFERENCE_ENTITY: ensure we're using the PUBLISHED version
            if (useReferenceEntityLogic && parentCategory.getUid() != null) {
                Category published = findPublishedCategory(parentCategory.getUid());
                if (published != null) {
                    parentCategory = published;
                }
            }

            CategoryIspartof newRel = new CategoryIspartof();

            CategoryIspartofId id = new CategoryIspartofId();
            id.setCategory1InstanceId(newEntity.getInstanceId());
            id.setCategory2InstanceId(parentCategory.getInstanceId());
            newRel.setId(id);

            newRel.setCategory1Instance(newEntity);
            newRel.setCategory2Instance(parentCategory);

            try {
                getDbaccess().createObject(newRel);
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                        e.getMessage().contains("already exists"))) {
                    LOG.log(Level.FINE, "[CategoryAPI] Broader relation already exists during copy, skipping");
                } else {
                    LOG.log(Level.WARNING, "[CategoryAPI] Error copying broader relation: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Copies narrower relations from a previous version.
     */
    private void copyNarrowerFromPrevious(String oldId, Category newEntity, boolean useReferenceEntityLogic) {
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category2Instance", oldId, CategoryIspartof.class);
        if (existing == null || existing.isEmpty()) return;

        for (Object o : existing) {
            CategoryIspartof oldRel = (CategoryIspartof) o;
            Category childCategory = oldRel.getCategory1Instance();

            // For REFERENCE_ENTITY: ensure we're using the PUBLISHED version
            if (useReferenceEntityLogic && childCategory.getUid() != null) {
                Category published = findPublishedCategory(childCategory.getUid());
                if (published != null) {
                    childCategory = published;
                }
            }

            CategoryIspartof newRel = new CategoryIspartof();

            CategoryIspartofId id = new CategoryIspartofId();
            id.setCategory1InstanceId(childCategory.getInstanceId());
            id.setCategory2InstanceId(newEntity.getInstanceId());
            newRel.setId(id);

            newRel.setCategory1Instance(childCategory);
            newRel.setCategory2Instance(newEntity);

            try {
                getDbaccess().createObject(newRel);
            } catch (Exception e) {
                if (e.getMessage() != null && (e.getMessage().contains("duplicate key") ||
                        e.getMessage().contains("already exists"))) {
                    LOG.log(Level.FINE, "[CategoryAPI] Narrower relation already exists during copy, skipping");
                } else {
                    LOG.log(Level.WARNING, "[CategoryAPI] Error copying narrower relation: " + e.getMessage());
                }
            }
        }
    }

    private boolean isFieldExplicitlySet(Object obj, String fieldName) {
        try {
            Field field = findField(obj.getClass(), fieldName);
            if (field != null) {
                field.setAccessible(true);
                return field.get(obj) != null;
            }
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Error checking field: {0}", e.getMessage());
        }
        return false;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("category1Instance", instanceId, CategoryIspartof.class);
        deleteRelations("category2Instance", instanceId, CategoryIspartof.class);

        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        for (Category object : elementList) EposDataModelDAO.getInstance().deleteObject(object);
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
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

        // Retrieve broader relations (this category as child - category1)
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("category1Instance", edmobj.getInstanceId(), CategoryIspartof.class)) {
            CategoryIspartof item = (CategoryIspartof) object;
            if (item.getCategory2Instance() != null)
                o.addBroader(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategory2Instance().getInstanceId()));
        }

        // Retrieve narrower relations (this category as parent - category2)
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("category2Instance", edmobj.getInstanceId(), CategoryIspartof.class)) {
            CategoryIspartof item = (CategoryIspartof) object;
            if (item.getCategory1Instance() != null)
                o.addNarrower(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategory1Instance().getInstanceId()));
        }

        if (edmobj.getInScheme() != null) {
            LinkedEntity schemeLe = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveLinkedEntity(edmobj.getInScheme().getInstanceId());
            o.setInScheme(schemeLe);
        }

        return (org.epos.eposdatamodel.Category) VersioningStatusAPI.retrieveVersion(o);
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
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if (elementList != null && !elementList.isEmpty()) {
            Category edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.CATEGORY.name());
        }
        return null;
    }
}