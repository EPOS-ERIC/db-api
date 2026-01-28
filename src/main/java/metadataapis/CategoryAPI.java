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

        // IN SCHEME
        if (inSchemeExplicitlySet || !isNewVersion) {
            if (obj.getInScheme() != null) {
                createInscheme(obj.getInScheme(), edmobj, overrideStatus);
            }
        } else if (isNewVersion && oldInstanceId != null && previousObj instanceof org.epos.eposdatamodel.Category) {
            org.epos.eposdatamodel.Category oldPojo = (org.epos.eposdatamodel.Category) previousObj;
            if (oldPojo.getInScheme() != null) {
                createInscheme(oldPojo.getInScheme(), edmobj, overrideStatus);
            }
        }

        // BROADER
        if (broaderExplicitlySet || !isNewVersion) {
            syncBroaderRelations(edmobj, obj.getBroader(), overrideStatus);
        } else if (isNewVersion && oldInstanceId != null) {
            copyBroaderFromPrevious(oldInstanceId, edmobj);
        }

        // NARROWER
        if (narrowerExplicitlySet || !isNewVersion) {
            syncNarrowerRelations(edmobj, obj.getNarrower(), overrideStatus);
        } else if (isNewVersion && oldInstanceId != null) {
            copyNarrowerFromPrevious(oldInstanceId, edmobj);
        }

        getDbaccess().updateObject(edmobj);
        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.CATEGORY.name(), edmobj);

        return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.CATEGORY.name());
    }

    private void createInscheme(LinkedEntity inscheme, Category edmobj, StatusType overrideStatus) {
        List<CategoryScheme> categorySchemeList = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(inscheme, CategoryScheme.class);

        if (!categorySchemeList.isEmpty()) {
            edmobj.setInScheme(categorySchemeList.get(0));
        } else {
            org.epos.eposdatamodel.CategoryScheme childObj = new org.epos.eposdatamodel.CategoryScheme();

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

            LinkedEntity le = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(childObj, overrideStatus, null, null);

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

                CategoryIspartofId id = new CategoryIspartofId();
                id.setCategory1InstanceId(childEntity.getInstanceId());
                id.setCategory2InstanceId(parentEntity.getInstanceId());
                relation.setId(id);

                relation.setCategory1Instance(childEntity);
                relation.setCategory2Instance(parentEntity);

                LOG.log(Level.FINE, "Creating broader relation: {0} -> {1}",
                        new Object[]{childEntity.getUid(), parentEntity.getUid()});
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

                CategoryIspartofId id = new CategoryIspartofId();
                id.setCategory1InstanceId(childEntity.getInstanceId());
                id.setCategory2InstanceId(parentEntity.getInstanceId());
                relation.setId(id);

                relation.setCategory1Instance(childEntity);
                relation.setCategory2Instance(parentEntity);

                LOG.log(Level.FINE, "Creating narrower relation: {0} -> {1}",
                        new Object[]{parentEntity.getUid(), childEntity.getUid()});
                getDbaccess().createObject(relation);
            }
        }
    }

    private Category findOrCreateStub(LinkedEntity link, StatusType status) {
        List<Category> found = getDbaccess().getOneFromDBByUID(link.getUid(), Category.class);
        if (!found.isEmpty()) return found.get(0);

        String instanceId = UUID.randomUUID().toString();
        String metaId = UUID.randomUUID().toString();
        String versionId = UUID.randomUUID().toString();
        StatusType finalStatus = status != null ? status : StatusType.DRAFT;

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

        LOG.log(Level.FINE, "Created stub Category: {0}", link.getUid());
        return stub;
    }

    private void copyBroaderFromPrevious(String oldId, Category newEntity) {
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("category1Instance", oldId, CategoryIspartof.class);
        if (existing != null) {
            for (Object o : existing) {
                CategoryIspartof oldRel = (CategoryIspartof) o;
                CategoryIspartof newRel = new CategoryIspartof();

                CategoryIspartofId id = new CategoryIspartofId();
                id.setCategory1InstanceId(newEntity.getInstanceId());
                id.setCategory2InstanceId(oldRel.getCategory2Instance().getInstanceId());
                newRel.setId(id);

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

                CategoryIspartofId id = new CategoryIspartofId();
                id.setCategory1InstanceId(oldRel.getCategory1Instance().getInstanceId());
                id.setCategory2InstanceId(newEntity.getInstanceId());
                newRel.setId(id);

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

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("category1Instance", edmobj.getInstanceId(), CategoryIspartof.class)) {
            CategoryIspartof item = (CategoryIspartof) object;
            if (item.getCategory2Instance() != null)
                o.addBroader(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(item.getCategory2Instance().getInstanceId()));
        }

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