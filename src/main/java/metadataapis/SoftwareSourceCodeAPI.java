package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.SoftwareSourceCode;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationSyncUtil;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SoftwareSourceCodeAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareSourceCode> {

    private static final Logger LOG = Logger.getLogger(SoftwareSourceCodeAPI.class.getName());

    public SoftwareSourceCodeAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareSourceCode obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;
        String oldInstanceId = previousObj != null ? previousObj.getInstanceId() : null;

        String searchInstanceId = obj.getInstanceId();
        if (obj.getUid() != null) searchInstanceId = null;

        List<Softwaresourcecode> returnList = getDbaccess().getOneFromDB(searchInstanceId, obj.getMetaId(), obj.getUid(), null, getEdmClass());

        if (!returnList.isEmpty()) {
            Softwaresourcecode selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Softwaresourcecode item : returnList) {
                if (item.getVersion() != null && targetStatus.toString().equals(item.getVersion().getStatus())) {
                    selectedEntity = item;
                    break;
                }
            }
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.SoftwareSourceCode) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isNewVersion = oldInstanceId != null && !oldInstanceId.equals(obj.getInstanceId());

        Softwaresourcecode edmobj = new Softwaresourcecode();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());
        edmobj.setDownloadurl(obj.getDownloadURL());
        edmobj.setKeywords(obj.getKeywords());
        edmobj.setLicenseurl(obj.getLicenseURL());
        edmobj.setMainentityofpage(obj.getMainEntityofPage());
        edmobj.setRuntimeplatform(obj.getRuntimePlatform());
        edmobj.setSoftwareversion(obj.getSoftwareVersion());
        edmobj.setCoderepository(obj.getCodeRepository());
        edmobj.setSoftwareStatus(obj.getSoftwareStatus());
        edmobj.setSpatial(obj.getSpatial());
        edmobj.setTemporal(obj.getTemporal());
        edmobj.setFilesize(obj.getSize());
        edmobj.setTimerequired(obj.getTimeRequired());
        edmobj.setSoftwarerequirements(obj.getSoftwareRequirements());

        if (obj.getCategory() != null) CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
        if (obj.getContactPoint() != null) ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);

        if (obj.getIdentifier() != null) {
            RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                    SoftwaresourcecodeIdentifier.class, Identifier.class, "softwaresourcecodeInstance",
                    SoftwaresourcecodeIdentifier::getIdentifierInstance, SoftwaresourcecodeIdentifier::setSoftwaresourcecodeInstance,
                    SoftwaresourcecodeIdentifier::setIdentifierInstance, obj, previousObj, overrideStatus, false);
        }

        syncElements(edmobj, obj.getProgrammingLanguage(), ElementType.PROGRAMMINGLANGUAGE, overrideStatus, isNewVersion);

        if (obj.getAuthor() != null) {
            syncPolymorphicRelation(obj.getAuthor(), edmobj, SoftwaresourcecodeAuthor.class, "softwaresourcecode", overrideStatus, isNewVersion);
        }
        if (obj.getContributor() != null) {
            syncPolymorphicRelation(obj.getContributor(), edmobj, SoftwaresourcecodeContributor.class, "softwaresourcecode", overrideStatus, isNewVersion);
        }
        if (obj.getFunder() != null) {
            syncPolymorphicRelation(obj.getFunder(), edmobj, SoftwaresourcecodeFunder.class, "softwaresourcecode", overrideStatus, isNewVersion);
        }
        if (obj.getMaintainer() != null) {
            syncPolymorphicRelation(obj.getMaintainer(), edmobj, SoftwaresourcecodeMaintainer.class, "softwaresourcecode", overrideStatus, isNewVersion);
        }
        if (obj.getProvider() != null) {
            syncPolymorphicRelation(obj.getProvider(), edmobj, SoftwaresourcecodeProvider.class, "softwaresourcecode", overrideStatus, isNewVersion);
        }
        if (obj.getPublisher() != null) {
            syncPolymorphicRelation(obj.getPublisher(), edmobj, SoftwaresourcecodePublisher.class, "softwaresourcecode", overrideStatus, isNewVersion);
        }
        if (obj.getCreator() != null) {
            syncPolymorphicRelation(obj.getCreator(), edmobj, SoftwaresourcecodeCreator.class, "softwaresourcecode", overrideStatus, isNewVersion);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.SOFTWARESOURCECODE.name(), edmobj);

        return new LinkedEntity().entityType(entityName).instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid());
    }

    private <T> void syncPolymorphicRelation(List<LinkedEntity> links, Softwaresourcecode parent, Class<T> joinClass,
                                             String parentKey, StatusType overrideStatus, boolean isNewVersion) {
        if (links == null) links = new ArrayList<>();
        Map<String, Object> existingRelations = new HashMap<>();
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey(parentKey, parent.getInstanceId(), joinClass);
        if (existing != null) {
            for (Object obj : existing) {
                try {
                    String entityInstanceId = (String) joinClass.getMethod("getEntityInstanceId").invoke(obj);
                    String resourceEntity = (String) joinClass.getMethod("getResourceEntity").invoke(obj);
                    existingRelations.put(resourceEntity + ":" + entityInstanceId, obj);
                } catch (Exception e) { e.printStackTrace(); }
            }
        }

        Set<String> newKeys = new HashSet<>();
        for (LinkedEntity link : links) {
            Object targetEntity = findEntityByLinkedEntity(link, overrideStatus);
            if (targetEntity != null) {
                String targetInstanceId = getInstanceId(targetEntity);
                if (targetInstanceId != null) newKeys.add(link.getEntityType() + ":" + targetInstanceId);
            }
        }

        if (!isNewVersion) {
            for (Map.Entry<String, Object> entry : existingRelations.entrySet()) {
                if (!newKeys.contains(entry.getKey())) EposDataModelDAO.getInstance().deleteObject(entry.getValue());
            }
        }

        for (LinkedEntity link : links) {
            Object targetEntity = findEntityByLinkedEntity(link, overrideStatus);
            if (targetEntity != null) {
                String targetInstanceId = getInstanceId(targetEntity);
                if (targetInstanceId != null) {
                    String key = link.getEntityType() + ":" + targetInstanceId;
                    if (!existingRelations.containsKey(key)) {
                        try {
                            T pi = joinClass.getDeclaredConstructor().newInstance();
                            joinClass.getMethod("softwaresourcecode", Softwaresourcecode.class).invoke(pi, parent);
                            joinClass.getMethod("setSoftwaresourcecodeInstanceId", String.class).invoke(pi, parent.getInstanceId());
                            joinClass.getMethod("setResourceEntity", String.class).invoke(pi, link.getEntityType());
                            joinClass.getMethod("setEntityInstanceId", String.class).invoke(pi, targetInstanceId);

                            EposDataModelDAO.getInstance().updateObject(pi);

                        } catch (Exception e) {
                            // Swallow duplicates or log warning
                            LOG.warning("Failed to create polymorphic relation (likely duplicate): " + e.getMessage());
                        }
                    }
                }
            } else {
                createPendingCreatorRelation(parent.getInstanceId(), link, joinClass.getName());
            }
        }
    }

    private Object findEntityByLinkedEntity(LinkedEntity link, StatusType targetStatus) {
        if (link == null) return null;
        Class<?> targetClass = "ORGANIZATION".equals(link.getEntityType()) ? Organization.class :
                "PERSON".equals(link.getEntityType()) ? Person.class : null;
        if (targetClass == null) return null;
        if (link.getInstanceId() != null) {
            List<?> byInstance = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(link.getInstanceId(), targetClass);
            if (!byInstance.isEmpty()) return byInstance.get(0);
        }
        if (link.getUid() != null) {
            List<?> byUid = EposDataModelDAO.getInstance().getOneFromDBByUID(link.getUid(), targetClass);
            if (!byUid.isEmpty()) {
                for (Object entity : byUid) {
                    String status = getVersionStatus(entity);
                    if (targetStatus != null && targetStatus.toString().equals(status)) return entity;
                }
                return byUid.get(0);
            }
        }
        if (link.getMetaId() != null) {
            List<?> byMeta = EposDataModelDAO.getInstance().getOneFromDBByMetaId(link.getMetaId(), targetClass);
            if (!byMeta.isEmpty()) return byMeta.get(0);
        }
        return null;
    }

    private void createPendingCreatorRelation(String parentInstanceId, LinkedEntity targetLink, String joinClassName) {
        try {
            List<Versioningstatus> existing = EposDataModelDAO.getInstance().getOneFromDBBySpecificKeySimple("status", StatusType.PENDING.name(), Versioningstatus.class);
            if (existing != null) {
                for (Versioningstatus vs : existing) {
                    if (parentInstanceId.equals(vs.getInstanceId()) && targetLink.getUid() != null &&
                            targetLink.getUid().equals(vs.getUid()) && joinClassName.equals(vs.getMetaId())) return;
                }
            }
            Versioningstatus pending = new Versioningstatus();
            pending.setVersionId(UUID.randomUUID().toString());
            pending.setInstanceId(parentInstanceId);
            pending.setUid(targetLink.getUid());
            pending.setMetaId(joinClassName);
            pending.setStatus(StatusType.PENDING.name());
            pending.setProvenance("SOFTWARESOURCECODE");
            pending.setChangeComment(targetLink.getEntityType());
            pending.setChangeTimestamp(java.time.OffsetDateTime.now());
            EposDataModelDAO.getInstance().createObject(pending);
        } catch (Exception e) { LOG.warning("Error creating pending creator: " + e.getMessage()); }
    }

    private String getInstanceId(Object entity) {
        try { return (String) entity.getClass().getMethod("getInstanceId").invoke(entity); } catch (Exception e) { return null; }
    }
    private String getVersionStatus(Object entity) {
        try {
            Object version = entity.getClass().getMethod("getVersion").invoke(entity);
            if (version != null) return (String) version.getClass().getMethod("getStatus").invoke(version);
        } catch (Exception e) { }
        return null;
    }

    private void syncElements(Softwaresourcecode edmobj, List<String> values, ElementType type, StatusType overrideStatus, boolean isNewVersion) {
        if (values == null) values = new ArrayList<>();
        Map<String, SoftwaresourcecodeElement> existingElements = new HashMap<>();
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeElement.class);
        if (existing != null) {
            for (Object obj : existing) {
                SoftwaresourcecodeElement se = (SoftwaresourcecodeElement) obj;
                if (se.getElementInstance() != null && type.name().equals(se.getElementInstance().getType()))
                    existingElements.put(se.getElementInstance().getValue(), se);
            }
        }
        if (!isNewVersion) {
            for (Map.Entry<String, SoftwaresourcecodeElement> entry : existingElements.entrySet()) {
                if (!values.contains(entry.getKey())) {
                    if (entry.getValue().getElementInstance() != null) EposDataModelDAO.getInstance().deleteObject(entry.getValue().getElementInstance());
                    EposDataModelDAO.getInstance().deleteObject(entry.getValue());
                }
            }
        }
        for (String value : values) {
            if (!existingElements.containsKey(value)) {
                org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
                element.setType(type);
                element.setValue(value);
                LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
                List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);
                if (!el.isEmpty()) {
                    SoftwaresourcecodeElement ce = new SoftwaresourcecodeElement();
                    ce.setSoftwaresourcecodeInstance(edmobj);
                    ce.setElementInstance(el.get(0));
                    EposDataModelDAO.getInstance().updateObject(ce);
                }
            }
        }
    }

    @Override public Boolean delete(String instanceId) {
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeContactpoint.class);
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeIdentifier.class);
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeCategory.class);
        deleteRelations("softwaresourcecodeInstance", instanceId, SoftwaresourcecodeElement.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeAuthor.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeContributor.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeFunder.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeMaintainer.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeProvider.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodePublisher.class);
        deleteRelations("softwaresourcecode", instanceId, SoftwaresourcecodeCreator.class);
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        for (Softwaresourcecode object : elementList) EposDataModelDAO.getInstance().deleteObject(object);
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override public org.epos.eposdatamodel.SoftwareSourceCode retrieve(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if (elementList == null || elementList.isEmpty()) return null;
        Softwaresourcecode edmobj = elementList.get(0);
        org.epos.eposdatamodel.SoftwareSourceCode o = new org.epos.eposdatamodel.SoftwareSourceCode();
        o.setInstanceId(edmobj.getInstanceId()); o.setMetaId(edmobj.getMetaId()); o.setUid(edmobj.getUid());
        o.setName(edmobj.getName()); o.setDescription(edmobj.getDescription()); o.setDownloadURL(edmobj.getDownloadurl());
        o.addKeywords(edmobj.getKeywords()); o.setLicenseURL(edmobj.getLicenseurl()); o.setMainEntityofPage(edmobj.getMainentityofpage());
        o.setRuntimePlatform(edmobj.getRuntimeplatform()); o.setSoftwareVersion(edmobj.getSoftwareversion());
        o.setCodeRepository(edmobj.getCoderepository()); o.setSoftwareStatus(edmobj.getSoftwareStatus());
        o.setSpatial(edmobj.getSpatial()); o.setTemporal(edmobj.getTemporal());
        o.setSize(edmobj.getFilesize()); o.setTimeRequired(edmobj.getTimerequired()); o.setSoftwareRequirements(edmobj.getSoftwarerequirements());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeCategory.class))
            o.addCategory(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(((SoftwaresourcecodeCategory)object).getCategoryInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeContactpoint.class))
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(((SoftwaresourcecodeContactpoint)object).getContactpointInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeIdentifier.class))
            o.addIdentifier(retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(((SoftwaresourcecodeIdentifier)object).getIdentifierInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecodeInstance", edmobj.getInstanceId(), SoftwaresourcecodeElement.class)) {
            SoftwaresourcecodeElement item = (SoftwaresourcecodeElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.PROGRAMMINGLANGUAGE.name())) o.addProgrammingLanguage(el.getValue());
        }
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeAuthor.class, "addAuthor");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeContributor.class, "addContributor");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeFunder.class, "addFunder");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeMaintainer.class, "addMaintainer");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeProvider.class, "addProvider");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodePublisher.class, "addPublisher");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwaresourcecodeCreator.class, "addCreator");
        return (org.epos.eposdatamodel.SoftwareSourceCode) VersioningStatusAPI.retrieveVersion(o);
    }

    private void retrievePolymorphicRelations(org.epos.eposdatamodel.SoftwareSourceCode o, String id, Class<?> clazz, String methodName) {
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwaresourcecode", id, clazz)) {
            try {
                String resourceEntity = (String) clazz.getMethod("getResourceEntity").invoke(object);
                String entityInstanceId = (String) clazz.getMethod("getEntityInstanceId").invoke(object);
                LinkedEntity le = null;
                if (EntityNames.PERSON.name().equals(resourceEntity)) le = retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(entityInstanceId);
                else if (EntityNames.ORGANIZATION.name().equals(resourceEntity)) le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(entityInstanceId);
                if (le != null) o.getClass().getMethod(methodName, LinkedEntity.class).invoke(o, le);
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    @Override public org.epos.eposdatamodel.SoftwareSourceCode retrieveByUID(String uid) {
        List<Softwaresourcecode> returnList = getDbaccess().getOneFromDBByUID(uid, Softwaresourcecode.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveBunch(List<String> entities) { return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Softwaresourcecode.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAll() { return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Softwaresourcecode.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAllWithStatus(StatusType status) { return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Softwaresourcecode.class, status)); }
    private List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveEntities(Function<Void, List<String>> dbFetcher) { return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList()); }
    @Override public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if (elementList != null && !elementList.isEmpty()) {
            Softwaresourcecode edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.SOFTWARESOURCECODE.name());
        }
        return null;
    }
}