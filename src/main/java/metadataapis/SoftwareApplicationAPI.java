package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import model.Element;
import model.Identifier;
import model.Operation;
import model.Organization;
import model.Person;
import org.epos.eposdatamodel.*;
import relationsapi.CategoryRelationsAPI;
import relationsapi.ContactPointRelationsAPI;
import relationsapi.RelationSyncUtil;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SoftwareApplicationAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareApplication> {

    private static final Logger LOG = Logger.getLogger(SoftwareApplicationAPI.class.getName());

    public SoftwareApplicationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareApplication obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {

        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId()) != null ? retrieve(obj.getInstanceId()) : null;
        String oldInstanceId = previousObj != null ? previousObj.getInstanceId() : null;

        String searchInstanceId = obj.getInstanceId();

        List<Softwareapplication> returnList = getDbaccess().getOneFromDB(searchInstanceId, obj.getMetaId(), obj.getUid(), null, getEdmClass());

        if (!returnList.isEmpty()) {
            Softwareapplication selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Softwareapplication item : returnList) {
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

        obj = (org.epos.eposdatamodel.SoftwareApplication) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isNewVersion = oldInstanceId != null && !oldInstanceId.equals(obj.getInstanceId());

        Softwareapplication edmobj = new Softwareapplication();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setName(obj.getName());
        edmobj.setDescription(obj.getDescription());
        edmobj.setDownloadurl(obj.getDownloadURL());
        edmobj.setInstallurl(obj.getInstallURL());
        edmobj.setLicenseurl(obj.getLicenseURL());
        edmobj.setMainentityofpage(obj.getMainEntityOfPage());
        edmobj.setRequirements(obj.getRequirements());
        edmobj.setKeywords(obj.getKeywords());
        edmobj.setSoftwareversion(obj.getSoftwareVersion());
        edmobj.setSoftwareStatus(obj.getSoftwareStatus());
        edmobj.setFileSize(obj.getFileSize());
        edmobj.setSpatial(obj.getSpatial());
        edmobj.setTemporal(obj.getTemporal());
        edmobj.setMemoryrequirements(obj.getMemoryrequirements());
        edmobj.setProcessorRequirements(obj.getProcessorRequirements());
        edmobj.setStorageRequirements(obj.getStorageRequirements());
        edmobj.setTimeRequired(obj.getTimeRequired());

        if (obj.getCategory() != null) CategoryRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);
        if (obj.getContactPoint() != null) ContactPointRelationsAPI.createRelation(edmobj, obj, overrideStatus, previousObj);

        if (obj.getIdentifier() != null) {
            RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getIdentifier(), relationFromUpdate, relationToUpdate,
                    SoftwareapplicationIdentifier.class, Identifier.class, "softwareapplicationInstance",
                    SoftwareapplicationIdentifier::getIdentifierInstance, SoftwareapplicationIdentifier::setSoftwareapplicationInstance,
                    SoftwareapplicationIdentifier::setIdentifierInstance, obj, previousObj, overrideStatus, false);
        }
        if (obj.getParameter() != null) {
            RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getParameter(), relationFromUpdate, relationToUpdate,
                    SoftwareapplicationParameter.class, Parameter.class, "softwareapplicationInstance",
                    SoftwareapplicationParameter::getParameterInstance, SoftwareapplicationParameter::setSoftwareapplicationInstance,
                    SoftwareapplicationParameter::setParameterInstance, obj, previousObj, overrideStatus, false);
        }
        if (obj.getRelatedOperation() != null) {
            RelationSyncUtil.syncComplexRelation(edmobj, edmobj.getInstanceId(), obj.getRelatedOperation(), relationFromUpdate, relationToUpdate,
                    SoftwareapplicationOperation.class, Operation.class, "softwareapplicationInstance",
                    SoftwareapplicationOperation::getOperationInstance, SoftwareapplicationOperation::setSoftwareapplicationInstance,
                    SoftwareapplicationOperation::setOperationInstance, obj, previousObj, overrideStatus, true);
        }

        syncElements(edmobj, obj.getCitation(), ElementType.CITATION, overrideStatus, isNewVersion);
        syncElements(edmobj, obj.getOperatingSystem(), ElementType.OPERATINGSYSTEM, overrideStatus, isNewVersion);

        if (obj.getAuthor() != null) {
            syncPolymorphicRelation(obj.getAuthor(), edmobj, SoftwareapplicationAuthor.class, "softwareapplication", overrideStatus, isNewVersion);
        }
        if (obj.getContributor() != null) {
            syncPolymorphicRelation(obj.getContributor(), edmobj, SoftwareapplicationContributor.class, "softwareapplication", overrideStatus, isNewVersion);
        }
            if (obj.getFunder() != null) {
                syncPolymorphicRelation(obj.getFunder(), edmobj, SoftwareapplicationFunder.class, "softwareapplication", overrideStatus, isNewVersion);
            }
            if (obj.getMaintainer() != null) {
                syncPolymorphicRelation(obj.getMaintainer(), edmobj, SoftwareapplicationMaintainer.class, "softwareapplication", overrideStatus, isNewVersion);
            }
            if (obj.getProvider() != null) {
                syncPolymorphicRelation(obj.getProvider(), edmobj, SoftwareapplicationProvider.class, "softwareapplication", overrideStatus, isNewVersion);
            }
            if (obj.getPublisher() != null) {
                syncPolymorphicRelation(obj.getPublisher(), edmobj, SoftwareapplicationPublisher.class, "softwareapplication", overrideStatus, isNewVersion);
            }
            if (obj.getCreator() != null) {
                syncPolymorphicRelation(obj.getCreator(), edmobj, SoftwareapplicationCreator.class, "softwareapplication", overrideStatus, isNewVersion);
            }
        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.SOFTWAREAPPLICATION.name(), edmobj);

        return new LinkedEntity().entityType(entityName).instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid());
    }

    private <T> void syncPolymorphicRelation(List<LinkedEntity> links, Softwareapplication parent, Class<T> joinClass,
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
                            joinClass.getMethod("setSoftwareapplication", Softwareapplication.class).invoke(pi, parent);
                            joinClass.getMethod("setSoftwareapplicationInstanceId", String.class).invoke(pi, parent.getInstanceId());
                            joinClass.getMethod("setResourceEntity", String.class).invoke(pi, link.getEntityType());
                            joinClass.getMethod("setEntityInstanceId", String.class).invoke(pi, targetInstanceId);

                            // FIX: Try update, catch exception if it already exists (race condition)
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
        Class<?> targetClass = EntityNames.ORGANIZATION.name().equals(link.getEntityType()) ? Organization.class :
                EntityNames.PERSON.name().equals(link.getEntityType()) ? Person.class : null;
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
            pending.setProvenance("SOFTWAREAPPLICATION");
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

    private void syncElements(Softwareapplication edmobj, List<String> values, ElementType type, StatusType overrideStatus, boolean isNewVersion) {
        if (values == null) values = new ArrayList<>();
        Map<String, SoftwareapplicationElement> existingElements = new HashMap<>();
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(), SoftwareapplicationElement.class);
        if (existing != null) {
            for (Object obj : existing) {
                SoftwareapplicationElement se = (SoftwareapplicationElement) obj;
                if (se.getElementInstance() != null && type.name().equals(se.getElementInstance().getType()))
                    existingElements.put(se.getElementInstance().getValue(), se);
            }
        }
        if (!isNewVersion) {
            for (Map.Entry<String, SoftwareapplicationElement> entry : existingElements.entrySet()) {
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
                    SoftwareapplicationElement ce = new SoftwareapplicationElement();
                    ce.setSoftwareapplicationInstance(edmobj);
                    ce.setElementInstance(el.get(0));
                    EposDataModelDAO.getInstance().updateObject(ce);
                }
            }
        }
    }

    @Override public Boolean delete(String instanceId) {
        deleteRelations("softwareapplicationInstance", instanceId, SoftwareapplicationContactpoint.class);
        deleteRelations("softwareapplicationInstance", instanceId, SoftwareapplicationIdentifier.class);
        deleteRelations("softwareapplicationInstance", instanceId, SoftwareapplicationCategory.class);
        deleteRelations("softwareapplicationInstance", instanceId, SoftwareapplicationElement.class);
        deleteRelations("softwareapplicationInstance", instanceId, SoftwareapplicationParameter.class);
        deleteRelations("softwareapplicationInstance", instanceId, SoftwareapplicationOperation.class);
        deleteRelations("softwareapplication", instanceId, SoftwareapplicationAuthor.class);
        deleteRelations("softwareapplication", instanceId, SoftwareapplicationContributor.class);
        deleteRelations("softwareapplication", instanceId, SoftwareapplicationFunder.class);
        deleteRelations("softwareapplication", instanceId, SoftwareapplicationMaintainer.class);
        deleteRelations("softwareapplication", instanceId, SoftwareapplicationProvider.class);
        deleteRelations("softwareapplication", instanceId, SoftwareapplicationPublisher.class);
        deleteRelations("softwareapplication", instanceId, SoftwareapplicationCreator.class);
        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        for (Softwareapplication object : elementList) EposDataModelDAO.getInstance().deleteObject(object);
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override public org.epos.eposdatamodel.SoftwareApplication retrieve(String instanceId) {
        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        if (elementList == null || elementList.isEmpty()) return null;
        Softwareapplication edmobj = elementList.get(0);
        org.epos.eposdatamodel.SoftwareApplication o = new org.epos.eposdatamodel.SoftwareApplication();
        o.setInstanceId(edmobj.getInstanceId()); o.setMetaId(edmobj.getMetaId()); o.setUid(edmobj.getUid());
        o.setVersionId(edmobj.getVersion().getVersionId()); o.setName(edmobj.getName()); o.setDescription(edmobj.getDescription());
        o.setDownloadURL(edmobj.getDownloadurl()); o.setInstallURL(edmobj.getInstallurl()); o.addKeywords(edmobj.getKeywords());
        o.setLicenseURL(edmobj.getLicenseurl()); o.setMainEntityOfPage(edmobj.getMainentityofpage()); o.setRequirements(edmobj.getRequirements());
        o.setSoftwareVersion(edmobj.getSoftwareversion()); o.setSoftwareStatus(edmobj.getSoftwareStatus()); o.setFileSize(edmobj.getFileSize());
        o.setSpatial(edmobj.getSpatial()); o.setTemporal(edmobj.getTemporal()); o.setMemoryrequirements(edmobj.getMemoryrequirements());
        o.setProcessorRequirements(edmobj.getProcessorRequirements()); o.setStorageRequirements(edmobj.getStorageRequirements()); o.setTimeRequired(edmobj.getTimeRequired());
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(), SoftwareapplicationCategory.class))
            o.addCategory(retrieveAPI(EntityNames.CATEGORY.name()).retrieveLinkedEntity(((SoftwareapplicationCategory)object).getCategoryInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(), SoftwareapplicationContactpoint.class))
            o.addContactPoint(retrieveAPI(EntityNames.CONTACTPOINT.name()).retrieveLinkedEntity(((SoftwareapplicationContactpoint)object).getContactpointInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(), SoftwareapplicationIdentifier.class))
            o.addIdentifier(retrieveAPI(EntityNames.IDENTIFIER.name()).retrieveLinkedEntity(((SoftwareapplicationIdentifier)object).getIdentifierInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(), SoftwareapplicationParameter.class)) {
            SoftwareapplicationParameter item = (SoftwareapplicationParameter) object;
            if (item.getParameterInstance().getAction() != null && item.getParameterInstance().getAction().equals("OBJECT"))
                o.addInputParameter(retrieveAPI(EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER.name()).retrieveLinkedEntity(item.getParameterInstance().getInstanceId()));
            if (item.getParameterInstance().getAction() != null && item.getParameterInstance().getAction().equals("RESULT"))
                o.addOutputParameter(retrieveAPI(EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER.name()).retrieveLinkedEntity(item.getParameterInstance().getInstanceId()));
        }
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(), SoftwareapplicationOperation.class))
            o.addRelatedOperation(retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(((SoftwareapplicationOperation)object).getOperationInstance().getInstanceId()));
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwareapplicationInstance", edmobj.getInstanceId(), SoftwareapplicationElement.class)) {
            SoftwareapplicationElement item = (SoftwareapplicationElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.CITATION.name())) o.addCitation(el.getValue());
            if (el.getType().equals(ElementType.OPERATINGSYSTEM.name())) o.addOperatingSystem(el.getValue());
        }
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwareapplicationAuthor.class, "addAuthor");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwareapplicationContributor.class, "addContributor");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwareapplicationFunder.class, "addFunder");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwareapplicationMaintainer.class, "addMaintainer");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwareapplicationProvider.class, "addProvider");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwareapplicationPublisher.class, "addPublisher");
        retrievePolymorphicRelations(o, edmobj.getInstanceId(), SoftwareapplicationCreator.class, "addCreator");
        return (org.epos.eposdatamodel.SoftwareApplication) VersioningStatusAPI.retrieveVersion(o);
    }

    private void retrievePolymorphicRelations(org.epos.eposdatamodel.SoftwareApplication o, String id, Class<?> clazz, String methodName) {
        for (Object object : getDbaccess().getOneFromDBBySpecificKey("softwareapplication", id, clazz)) {
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

    @Override public org.epos.eposdatamodel.SoftwareApplication retrieveByUID(String uid) {
        List<Softwareapplication> returnList = getDbaccess().getOneFromDBByUID(uid, Softwareapplication.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override public List<org.epos.eposdatamodel.SoftwareApplication> retrieveBunch(List<String> entities) { return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Softwareapplication.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAll() { return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Softwareapplication.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAllWithStatus(StatusType status) { return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Softwareapplication.class, status)); }
    private List<org.epos.eposdatamodel.SoftwareApplication> retrieveEntities(Function<Void, List<String>> dbFetcher) { return dbFetcher.apply(null).parallelStream().map(this::retrieve).collect(Collectors.toList()); }
    @Override public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        if (elementList != null && !elementList.isEmpty()) {
            Softwareapplication edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.SOFTWAREAPPLICATION.name());
        }
        return null;
    }
}