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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class CategoryAPI extends AbstractAPI<org.epos.eposdatamodel.Category> {

    public CategoryAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Category obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        // Capture if fields were explicitly set BEFORE any processing
        boolean broaderExplicitlySet = isFieldExplicitlySet(obj, "broader");
        boolean narrowerExplicitlySet = isFieldExplicitlySet(obj, "narrower");
        boolean inSchemeExplicitlySet = isFieldExplicitlySet(obj, "inScheme");

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;

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
        edmobj.setName(Optional.ofNullable(obj.getName()).orElse(""));
        edmobj.setDescription(Optional.ofNullable(obj.getDescription()).orElse(""));

        // IN SCHEME
        if (inSchemeExplicitlySet || !isNewVersion) {
            if (Objects.nonNull(obj.getInScheme())) {
                createInscheme(obj.getInScheme(), edmobj, overrideStatus, obj.getFileProvenance());
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyInSchemeFromPreviousVersion(oldInstanceId, edmobj);
        }

        // BROADER
        if (broaderExplicitlySet || !isNewVersion) {
            if (Objects.nonNull(obj.getBroader()) && !obj.getBroader().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getBroader(), relationFromUpdate, relationToUpdate,
                        CategoryIspartof.class, Category.class,
                        "category2Instance",
                        CategoryIspartof::getCategory1Instance,
                        CategoryIspartof::setCategory2Instance,
                        CategoryIspartof::setCategory1Instance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    CategoryIspartof.class, Category.class,
                    "category2Instance",
                    CategoryIspartof::getCategory1Instance,
                    CategoryIspartof::setCategory2Instance,
                    CategoryIspartof::setCategory1Instance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // NARROWER
        if (narrowerExplicitlySet || !isNewVersion) {
            if (Objects.nonNull(obj.getNarrower()) && !obj.getNarrower().isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), obj.getNarrower(), relationFromUpdate, relationToUpdate,
                        CategoryIspartof.class, Category.class,
                        "category1Instance",
                        CategoryIspartof::getCategory2Instance,
                        CategoryIspartof::setCategory1Instance,
                        CategoryIspartof::setCategory2Instance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    CategoryIspartof.class, Category.class,
                    "category1Instance",
                    CategoryIspartof::getCategory2Instance,
                    CategoryIspartof::setCategory1Instance,
                    CategoryIspartof::setCategory2Instance,
                    obj, previousObj, overrideStatus, false
            );
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.CATEGORY.name(), edmobj);

        return new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
    }

    private void copyInSchemeFromPreviousVersion(String oldInstanceId, Category newEdmobj) {
        List<Category> oldList = getDbaccess().getOneFromDBByInstanceId(oldInstanceId, Category.class);
        if (oldList != null && !oldList.isEmpty()) {
            Category oldCategory = oldList.get(0);
            if (oldCategory.getInScheme() != null) {
                newEdmobj.setInScheme(oldCategory.getInScheme());
            }
        }
    }

    private void createInscheme(LinkedEntity inscheme, Category edmobj, StatusType overrideStatus, String provenance) {
        List<CategoryScheme> categorySchemeList = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(inscheme, CategoryScheme.class);
        if (!categorySchemeList.isEmpty()) {
            edmobj.setInScheme(categorySchemeList.get(0));
        } else {
            org.epos.eposdatamodel.CategoryScheme childObj = new org.epos.eposdatamodel.CategoryScheme();
            childObj.setInstanceId(inscheme.getInstanceId());
            childObj.setMetaId(inscheme.getMetaId());
            childObj.setUid(inscheme.getUid());
            childObj.setStatus(StatusType.valueOf(edmobj.getVersion().getStatus()));
            LinkedEntity le = retrieveAPI(EntityNames.CATEGORYSCHEME.name()).create(childObj, overrideStatus, null, null);
            edmobj.setInScheme((CategoryScheme) getDbaccess().getOneFromDBByLinkedEntity(le, CategoryScheme.class).get(0));
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
        for (Category object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getJoinEntitiesByParentId(key, instanceId, clazz);
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
        if (edmobj.getInScheme() != null) {
            o.setInScheme(retrieveAPI(EntityNames.CATEGORYSCHEME.name()).retrieveLinkedEntity(edmobj.getInScheme().getInstanceId()));
        }

        ArrayList<LinkedEntity> broaders = new ArrayList<>();
        ArrayList<LinkedEntity> narrowers = new ArrayList<>();

        for (Object obj : getDbaccess().getOneFromDBBySpecificKey("category1Instance", edmobj.getInstanceId(), CategoryIspartof.class)) {
            CategoryIspartof item = (CategoryIspartof) obj;
            narrowers.add(retrieveLinkedEntity(item.getCategory2Instance().getInstanceId()));
        }

        for (Object obj : getDbaccess().getOneFromDBBySpecificKey("category2Instance", edmobj.getInstanceId(), CategoryIspartof.class)) {
            CategoryIspartof item = (CategoryIspartof) obj;
            broaders.add(retrieveLinkedEntity(item.getCategory1Instance().getInstanceId()));
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
        return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList());
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Category> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Category.class);
        if (elementList != null && !elementList.isEmpty()) {
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