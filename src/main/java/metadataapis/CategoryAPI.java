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
import java.util.stream.Collectors;

public class CategoryAPI extends AbstractAPI<org.epos.eposdatamodel.Category> {

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

        boolean isNewVersion = obj.getInstanceChangedId() != null;

        Category edmobj = new Category();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());

        // --- RELATIONS ---

        // 1. IN SCHEME (Direct @ManyToOne Relationship)
        if (inSchemeExplicitlySet || !isNewVersion) {
            if (obj.getInScheme() != null) {
                createInscheme(obj.getInScheme(), edmobj, overrideStatus);
            }
        } else if (isNewVersion && oldInstanceId != null && previousObj instanceof org.epos.eposdatamodel.Category) {
            // Copy from previous version
            org.epos.eposdatamodel.Category oldPojo = (org.epos.eposdatamodel.Category) previousObj;
            if (oldPojo.getInScheme() != null) {
                createInscheme(oldPojo.getInScheme(), edmobj, overrideStatus);
            }
        }

        // 2. BROADER (Recursive Relation via CategoryIspartof)
        if (broaderExplicitlySet || !isNewVersion) {
            syncBroaderRelations(edmobj, obj.getBroader(), overrideStatus);
        } else if (isNewVersion && oldInstanceId != null) {
            copyBroaderFromPrevious(oldInstanceId, edmobj);
        }

        // 3. NARROWER (Recursive Relation via CategoryIspartof)
        if (narrowerExplicitlySet || !isNewVersion) {
            syncNarrowerRelations(edmobj, obj.getNarrower(), overrideStatus);
        } else if (isNewVersion && oldInstanceId != null) {
            copyNarrowerFromPrevious(oldInstanceId, edmobj);
        }

        getDbaccess().updateObject(edmobj);
        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.CATEGORY.name(), edmobj);

        return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.CATEGORY.name());
    }

    // Logic to handle direct @ManyToOne relationship for Scheme
    private void createInscheme(LinkedEntity inscheme, Category edmobj, StatusType overrideStatus) {
        List<CategoryScheme> categorySchemeList = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(inscheme, CategoryScheme.class);

        if (!categorySchemeList.isEmpty()) {
            edmobj.setInScheme(categorySchemeList.get(0));
        } else {
            // Implicit Stub Creation for CategoryScheme
            org.epos.eposdatamodel.CategoryScheme childObj = new org.epos.eposdatamodel.CategoryScheme();

            // Generate IDs for stub if missing in linked entity
            String instanceId = inscheme.getInstanceId() != null ? inscheme.getInstanceId() : UUID.randomUUID().toString();
            String metaId = inscheme.getMetaId() != null ? inscheme.getMetaId() : UUID.randomUUID().toString();

            childObj.setInstanceId(instanceId);
            childObj.setMetaId(metaId);
            childObj.setUid(inscheme.getUid());

            StatusType status = overrideStatus;
            if (status == null && edmobj.getVersion() != null && edmobj.getVersion().getStatus() != null) {
                try {
                    status = StatusType.valueOf(edmobj.getVersion().getStatus());
                } catch (Exception e) {
                    status = StatusType.DRAFT;
                }
            }
            if (status == null) status = StatusType.DRAFT;
            childObj.setStatus(status);

            // Create the Scheme Stub using its API
            LinkedEntity le = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(childObj, overrideStatus, null, null);

            // Retrieve and set
            List<CategoryScheme> created = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(le, CategoryScheme.class);
            if (!created.isEmpty()) {
                edmobj.setInScheme(created.get(0));
            }
        }
    }

    private void syncBroaderRelations(Category childEntity, List<LinkedEntity> broaderLinks, StatusType overrideStatus) {
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category1Instance", childEntity.getInstanceId(), CategoryIspartof.class);
        if (existing != null) {
            for (Object o : existing) getDbaccess().deleteObject(o);
        }

        if (broaderLinks == null) return;

        for (LinkedEntity link : broaderLinks) {
            Category parentEntity = findOrCreateStub(link, overrideStatus);
            if (parentEntity != null) {
                CategoryIspartof relation = new CategoryIspartof();
                relation.setCategory1Instance(childEntity); // ME (Child)
                relation.setCategory2Instance(parentEntity); // TARGET (Parent)
                getDbaccess().createObject(relation);
            }
        }
    }

    private void syncNarrowerRelations(Category parentEntity, List<LinkedEntity> narrowerLinks, StatusType overrideStatus) {
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category2Instance", parentEntity.getInstanceId(), CategoryIspartof.class);
        if (existing != null) {
            for (Object o : existing) getDbaccess().deleteObject(o);
        }

        if (narrowerLinks == null) return;

        for (LinkedEntity link : narrowerLinks) {
            Category childEntity = findOrCreateStub(link, overrideStatus);
            if (childEntity != null) {
                CategoryIspartof relation = new CategoryIspartof();
                relation.setCategory1Instance(childEntity); // TARGET (Child)
                relation.setCategory2Instance(parentEntity); // ME (Parent)
                getDbaccess().createObject(relation);
            }
        }
    }

    private Category findOrCreateStub(LinkedEntity link, StatusType status) {
        List<Category> found = getDbaccess().getOneFromDBByUID(link.getUid(), Category.class);
        if (!found.isEmpty()) return found.get(0);

        Category stub = new Category();
        stub.setUid(link.getUid());
        stub.setInstanceId(UUID.randomUUID().toString());
        stub.setMetaId(UUID.randomUUID().toString());
        model.Versioningstatus vs = new model.Versioningstatus();
        vs.setStatus(status != null ? status.toString() : StatusType.DRAFT.toString());
        stub.setVersion(vs);

        getDbaccess().createObject(stub);
        return stub;
    }

    private void copyBroaderFromPrevious(String oldId, Category newEntity) {
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category1Instance", oldId, CategoryIspartof.class);
        if (existing != null) {
            for (Object o : existing) {
                CategoryIspartof oldRel = (CategoryIspartof) o;
                CategoryIspartof newRel = new CategoryIspartof();
                newRel.setCategory1Instance(newEntity);
                newRel.setCategory2Instance(oldRel.getCategory2Instance());
                getDbaccess().createObject(newRel);
            }
        }
    }

    private void copyNarrowerFromPrevious(String oldId, Category newEntity) {
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category2Instance", oldId, CategoryIspartof.class);
        if (existing != null) {
            for (Object o : existing) {
                CategoryIspartof oldRel = (CategoryIspartof) o;
                CategoryIspartof newRel = new CategoryIspartof();
                newRel.setCategory1Instance(oldRel.getCategory1Instance());
                newRel.setCategory2Instance(newEntity);
                getDbaccess().createObject(newRel);
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
        } catch (Exception e) {}
        return false;
    }

    private Field findField(Class<?> clazz, String fieldName) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(fieldName); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    @Override public Boolean delete(String instanceId) {
        // Only delete related JOIN tables
        deleteRelations("category1Instance", instanceId, CategoryIspartof.class);
        deleteRelations("category2Instance", instanceId, CategoryIspartof.class);

        // NO deletion of CategoryCategoryscheme join because it doesn't exist.
        // The relation is inside the Category table itself.

        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        for (Category object : elementList) EposDataModelDAO.getInstance().deleteObject(object);
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override public org.epos.eposdatamodel.Category retrieve(String instanceId) {
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Category edmobj = elementList.get(0);
        org.epos.eposdatamodel.Category o = new org.epos.eposdatamodel.Category();
        o.setInstanceId(edmobj.getInstanceId()); o.setMetaId(edmobj.getMetaId()); o.setUid(edmobj.getUid());
        o.setName(edmobj.getName()); o.setDescription(edmobj.getDescription());

        // Retrieve BROADER
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("category1Instance", edmobj.getInstanceId(), CategoryIspartof.class)) {
            CategoryIspartof item = (CategoryIspartof) object;
            if(item.getCategory2Instance() != null)
                o.addBroader(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategory2Instance().getInstanceId()));
        }

        // Retrieve NARROWER
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("category2Instance", edmobj.getInstanceId(), CategoryIspartof.class)) {
            CategoryIspartof item = (CategoryIspartof) object;
            if(item.getCategory1Instance() != null)
                o.addNarrower(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategory1Instance().getInstanceId()));
        }

        // Retrieve IN SCHEME (Direct Field)
        if (edmobj.getInScheme() != null) {
            LinkedEntity schemeLe = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveLinkedEntity(edmobj.getInScheme().getInstanceId());
            o.setInScheme(schemeLe);
        }

        return (org.epos.eposdatamodel.Category) VersioningStatusAPI.retrieveVersion(o);
    }

    @Override public org.epos.eposdatamodel.Category retrieveByUID(String uid) {
        List<Category> returnList = getDbaccess().getOneFromDBByUID(uid, Category.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override public List<org.epos.eposdatamodel.Category> retrieveBunch(List<String> entities) { return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Category.class)); }
    @Override public List<org.epos.eposdatamodel.Category> retrieveAll() { return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Category.class)); }
    @Override public List<org.epos.eposdatamodel.Category> retrieveAllWithStatus(StatusType status) { return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Category.class, status)); }
    private List<org.epos.eposdatamodel.Category> retrieveEntities(Function<Void, List<String>> dbFetcher) { return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList()); }
    @Override public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if (elementList != null && !elementList.isEmpty()) {
            Category edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.CATEGORY.name());
        }
        return null;
    }
}