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
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SoftwareApplicationAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareApplication> {

    private static final Logger LOG = Logger.getLogger(SoftwareApplicationAPI.class.getName());

    public SoftwareApplicationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareApplication obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());
        String oldInstanceId = previousObj != null ? previousObj.getInstanceId() : null;

        String searchInstanceId = obj.getInstanceId();

        List<Softwareapplication> returnList = getDbaccess().getOneFromDB(searchInstanceId, obj.getMetaId(), obj.getUid(), null, getEdmClass());

        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Softwareapplication selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Softwareapplication::getVersion);
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

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

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

        
            LinkedEntity result = new LinkedEntity().entityType(entityName).instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    private <T> void syncPolymorphicRelation(List<LinkedEntity> links, Softwareapplication parent, Class<T> joinClass,
                                             String parentKey, StatusType overrideStatus, boolean isNewVersion) {
        if (links == null) links = new ArrayList<>();
        Map<String, Object> existingRelations = new HashMap<>();
        List<Object> existing = getDbaccess().getOneFromDBBySpecificKey(parentKey, parent.getInstanceId(), joinClass);
        if (existing != null) {
            for (Object obj : existing) {
                // Performance: Use cached reflection instead of repeated getMethod calls
                String entityInstanceId = utilities.ReflectionCache.invokeStringGetter(obj, "getEntityInstanceId");
                String resourceEntity = utilities.ReflectionCache.invokeStringGetter(obj, "getResourceEntity");
                if (entityInstanceId != null && resourceEntity != null) {
                    existingRelations.put(resourceEntity + ":" + entityInstanceId, obj);
                }
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
                            // Performance: Use cached reflection for setters
                            utilities.ReflectionCache.invokeSetter(pi, "setSoftwareapplication", Softwareapplication.class, parent);
                            utilities.ReflectionCache.invokeStringSetter(pi, "setSoftwareapplicationInstanceId", parent.getInstanceId());
                            utilities.ReflectionCache.invokeStringSetter(pi, "setResourceEntity", link.getEntityType());
                            utilities.ReflectionCache.invokeStringSetter(pi, "setEntityInstanceId", targetInstanceId);

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
        // Performance: Use cached reflection
        return utilities.ReflectionCache.getInstanceId(entity);
    }
    
    private String getVersionStatus(Object entity) {
        // Performance: Use cached reflection
        return utilities.ReflectionCache.getVersionStatus(entity);
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
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Softwareapplication.class, List.of(
                new EposDataModelDAO.RelationField(SoftwareapplicationContactpoint.class, "softwareapplicationInstance"),
                new EposDataModelDAO.RelationField(SoftwareapplicationIdentifier.class, "softwareapplicationInstance"),
                new EposDataModelDAO.RelationField(SoftwareapplicationCategory.class, "softwareapplicationInstance"),
                new EposDataModelDAO.RelationField(SoftwareapplicationElement.class, "softwareapplicationInstance"),
                new EposDataModelDAO.RelationField(SoftwareapplicationParameter.class, "softwareapplicationInstance"),
                new EposDataModelDAO.RelationField(SoftwareapplicationOperation.class, "softwareapplicationInstance"),
                new EposDataModelDAO.RelationField(SoftwareapplicationAuthor.class, "softwareapplication"),
                new EposDataModelDAO.RelationField(SoftwareapplicationContributor.class, "softwareapplication"),
                new EposDataModelDAO.RelationField(SoftwareapplicationFunder.class, "softwareapplication"),
                new EposDataModelDAO.RelationField(SoftwareapplicationMaintainer.class, "softwareapplication"),
                new EposDataModelDAO.RelationField(SoftwareapplicationProvider.class, "softwareapplication"),
                new EposDataModelDAO.RelationField(SoftwareapplicationPublisher.class, "softwareapplication"),
                new EposDataModelDAO.RelationField(SoftwareapplicationCreator.class, "softwareapplication")));
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
            // Performance: Use cached reflection
            String resourceEntity = utilities.ReflectionCache.invokeStringGetter(object, "getResourceEntity");
            String entityInstanceId = utilities.ReflectionCache.invokeStringGetter(object, "getEntityInstanceId");
            if (resourceEntity == null || entityInstanceId == null) continue;
            
            LinkedEntity le = null;
            if (EntityNames.PERSON.name().equals(resourceEntity)) {
                le = retrieveAPI(EntityNames.PERSON.name()).retrieveLinkedEntity(entityInstanceId);
            } else if (EntityNames.ORGANIZATION.name().equals(resourceEntity)) {
                le = retrieveAPI(EntityNames.ORGANIZATION.name()).retrieveLinkedEntity(entityInstanceId);
            }
            if (le != null) {
                utilities.ReflectionCache.invokeSetter(o, methodName, LinkedEntity.class, le);
            }
        }
    }

    @Override public org.epos.eposdatamodel.SoftwareApplication retrieveByUID(String uid) {
        List<Softwareapplication> returnList = getDbaccess().getOneFromDBByUID(uid, Softwareapplication.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override public List<org.epos.eposdatamodel.SoftwareApplication> retrieveBunch(List<String> entities) { return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Softwareapplication.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAll() { return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Softwareapplication.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareApplication> retrieveAllWithStatus(StatusType status) { return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Softwareapplication.class, status)); }
    private List<org.epos.eposdatamodel.SoftwareApplication> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();

        Map<String, Softwareapplication> applications = getDbaccess().batchFetchByInstanceIds(instanceIds, Softwareapplication.class);
        if (applications.isEmpty()) return Collections.emptyList();
        List<String> foundIds = new ArrayList<>(applications.keySet());

        Map<String, List<SoftwareapplicationCategory>> categories = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplicationInstance", foundIds, SoftwareapplicationCategory.class);
        Map<String, List<SoftwareapplicationContactpoint>> contactPoints = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplicationInstance", foundIds, SoftwareapplicationContactpoint.class);
        Map<String, List<SoftwareapplicationIdentifier>> identifiers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplicationInstance", foundIds, SoftwareapplicationIdentifier.class);
        Map<String, List<SoftwareapplicationParameter>> parameters = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplicationInstance", foundIds, SoftwareapplicationParameter.class);
        Map<String, List<SoftwareapplicationOperation>> operations = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplicationInstance", foundIds, SoftwareapplicationOperation.class);
        Map<String, List<SoftwareapplicationElement>> elements = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplicationInstance", foundIds, SoftwareapplicationElement.class);
        Map<String, List<SoftwareapplicationAuthor>> authors = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplication", foundIds, SoftwareapplicationAuthor.class);
        Map<String, List<SoftwareapplicationContributor>> contributors = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplication", foundIds, SoftwareapplicationContributor.class);
        Map<String, List<SoftwareapplicationFunder>> funders = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplication", foundIds, SoftwareapplicationFunder.class);
        Map<String, List<SoftwareapplicationMaintainer>> maintainers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplication", foundIds, SoftwareapplicationMaintainer.class);
        Map<String, List<SoftwareapplicationProvider>> providers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplication", foundIds, SoftwareapplicationProvider.class);
        Map<String, List<SoftwareapplicationPublisher>> publishers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplication", foundIds, SoftwareapplicationPublisher.class);
        Map<String, List<SoftwareapplicationCreator>> creators = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwareapplication", foundIds, SoftwareapplicationCreator.class);

        Set<String> categoryIds = new HashSet<>(), contactPointIds = new HashSet<>(), identifierIds = new HashSet<>();
        Set<String> parameterIds = new HashSet<>(), operationIds = new HashSet<>(), elementIds = new HashSet<>();
        categories.values().forEach(rs -> rs.forEach(r -> categoryIds.add(r.getCategoryInstance().getInstanceId())));
        contactPoints.values().forEach(rs -> rs.forEach(r -> contactPointIds.add(r.getContactpointInstance().getInstanceId())));
        identifiers.values().forEach(rs -> rs.forEach(r -> identifierIds.add(r.getIdentifierInstance().getInstanceId())));
        parameters.values().forEach(rs -> rs.forEach(r -> parameterIds.add(r.getParameterInstance().getInstanceId())));
        operations.values().forEach(rs -> rs.forEach(r -> operationIds.add(r.getOperationInstance().getInstanceId())));
        elements.values().forEach(rs -> rs.forEach(r -> elementIds.add(r.getElementInstance().getInstanceId())));

        Set<String> personIds = new HashSet<>(), organizationIds = new HashSet<>();
        collectPolymorphicIds(authors.values(), personIds, organizationIds);
        collectPolymorphicIds(contributors.values(), personIds, organizationIds);
        collectPolymorphicIds(funders.values(), personIds, organizationIds);
        collectPolymorphicIds(maintainers.values(), personIds, organizationIds);
        collectPolymorphicIds(providers.values(), personIds, organizationIds);
        collectPolymorphicIds(publishers.values(), personIds, organizationIds);
        collectPolymorphicIds(creators.values(), personIds, organizationIds);

        Map<String, model.Category> categoryMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(categoryIds), model.Category.class);
        Map<String, Contactpoint> contactPointMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(contactPointIds), Contactpoint.class);
        Map<String, Identifier> identifierMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(identifierIds), Identifier.class);
        Map<String, Parameter> parameterMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(parameterIds), Parameter.class);
        Map<String, Operation> operationMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(operationIds), Operation.class);
        Map<String, Element> elementMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(elementIds), Element.class);
        Map<String, Person> personMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(personIds), Person.class);
        Map<String, Organization> organizationMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(organizationIds), Organization.class);
        Map<String, Versioningstatus> versions = getDbaccess().batchFetchVersioningStatus(foundIds);
        List<String> metaIds = applications.values().stream().map(Softwareapplication::getMetaId)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, List<String>> groups = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(metaIds);

        List<org.epos.eposdatamodel.SoftwareApplication> results = new ArrayList<>(applications.size());
        for (String id : instanceIds) {
            Softwareapplication entity = applications.get(id);
            if (entity == null) continue;
            org.epos.eposdatamodel.SoftwareApplication dto = toBulkDto(entity);
            for (SoftwareapplicationCategory relation : categories.getOrDefault(id, Collections.emptyList()))
                addLinked(dto, "addCategory", categoryMap.get(relation.getCategoryInstance().getInstanceId()), EntityNames.CATEGORY);
            for (SoftwareapplicationContactpoint relation : contactPoints.getOrDefault(id, Collections.emptyList()))
                addLinked(dto, "addContactPoint", contactPointMap.get(relation.getContactpointInstance().getInstanceId()), EntityNames.CONTACTPOINT);
            for (SoftwareapplicationIdentifier relation : identifiers.getOrDefault(id, Collections.emptyList()))
                addLinked(dto, "addIdentifier", identifierMap.get(relation.getIdentifierInstance().getInstanceId()), EntityNames.IDENTIFIER);
            for (SoftwareapplicationParameter relation : parameters.getOrDefault(id, Collections.emptyList())) {
                Parameter parameter = parameterMap.get(relation.getParameterInstance().getInstanceId());
                if (parameter != null && "OBJECT".equals(parameter.getAction())) addLinked(dto, "addInputParameter", parameter, EntityNames.SOFTWAREAPPLICATIONINPUTPARAMETER);
                if (parameter != null && "RESULT".equals(parameter.getAction())) addLinked(dto, "addOutputParameter", parameter, EntityNames.SOFTWAREAPPLICATIONOUTPUTPARAMETER);
            }
            for (SoftwareapplicationOperation relation : operations.getOrDefault(id, Collections.emptyList()))
                addLinked(dto, "addRelatedOperation", operationMap.get(relation.getOperationInstance().getInstanceId()), EntityNames.OPERATION);
            for (SoftwareapplicationElement relation : elements.getOrDefault(id, Collections.emptyList())) {
                Element element = elementMap.get(relation.getElementInstance().getInstanceId());
                if (element != null && ElementType.CITATION.name().equals(element.getType())) dto.addCitation(element.getValue());
                if (element != null && ElementType.OPERATINGSYSTEM.name().equals(element.getType())) dto.addOperatingSystem(element.getValue());
            }
            addPolymorphicRelations(dto, authors.get(id), personMap, organizationMap, "addAuthor");
            addPolymorphicRelations(dto, contributors.get(id), personMap, organizationMap, "addContributor");
            addPolymorphicRelations(dto, funders.get(id), personMap, organizationMap, "addFunder");
            addPolymorphicRelations(dto, maintainers.get(id), personMap, organizationMap, "addMaintainer");
            addPolymorphicRelations(dto, providers.get(id), personMap, organizationMap, "addProvider");
            addPolymorphicRelations(dto, publishers.get(id), personMap, organizationMap, "addPublisher");
            addPolymorphicRelations(dto, creators.get(id), personMap, organizationMap, "addCreator");
            VersioningStatusAPI.applyVersion(dto, versions.get(id), groups.get(dto.getMetaId()));
            results.add(dto);
        }
        return results;
    }

    private org.epos.eposdatamodel.SoftwareApplication toBulkDto(Softwareapplication entity) {
        org.epos.eposdatamodel.SoftwareApplication dto = new org.epos.eposdatamodel.SoftwareApplication();
        dto.setInstanceId(entity.getInstanceId()); dto.setMetaId(entity.getMetaId()); dto.setUid(entity.getUid());
        dto.setName(entity.getName()); dto.setDescription(entity.getDescription()); dto.setDownloadURL(entity.getDownloadurl());
        dto.setInstallURL(entity.getInstallurl()); dto.addKeywords(entity.getKeywords()); dto.setLicenseURL(entity.getLicenseurl());
        dto.setMainEntityOfPage(entity.getMainentityofpage()); dto.setRequirements(entity.getRequirements());
        dto.setSoftwareVersion(entity.getSoftwareversion()); dto.setSoftwareStatus(entity.getSoftwareStatus()); dto.setFileSize(entity.getFileSize());
        dto.setSpatial(entity.getSpatial()); dto.setTemporal(entity.getTemporal()); dto.setMemoryrequirements(entity.getMemoryrequirements());
        dto.setProcessorRequirements(entity.getProcessorRequirements()); dto.setStorageRequirements(entity.getStorageRequirements());
        dto.setTimeRequired(entity.getTimeRequired());
        return dto;
    }

    private void collectPolymorphicIds(Collection<? extends Collection<?>> relationGroups, Set<String> personIds, Set<String> organizationIds) {
        for (Collection<?> relations : relationGroups) {
            for (Object relation : relations) {
                String type = utilities.ReflectionCache.invokeStringGetter(relation, "getResourceEntity");
                String id = utilities.ReflectionCache.invokeStringGetter(relation, "getEntityInstanceId");
                if (EntityNames.PERSON.name().equals(type)) personIds.add(id);
                if (EntityNames.ORGANIZATION.name().equals(type)) organizationIds.add(id);
            }
        }
    }

    private void addPolymorphicRelations(org.epos.eposdatamodel.SoftwareApplication dto, Collection<? extends Object> relations,
                                         Map<String, Person> people, Map<String, Organization> organizations, String methodName) {
        if (relations == null) return;
        for (Object relation : relations) {
            String type = utilities.ReflectionCache.invokeStringGetter(relation, "getResourceEntity");
            String id = utilities.ReflectionCache.invokeStringGetter(relation, "getEntityInstanceId");
            if (EntityNames.PERSON.name().equals(type)) addLinked(dto, methodName, people.get(id), EntityNames.PERSON);
            if (EntityNames.ORGANIZATION.name().equals(type)) addLinked(dto, methodName, organizations.get(id), EntityNames.ORGANIZATION);
        }
    }

    private void addLinked(org.epos.eposdatamodel.SoftwareApplication dto, String methodName, Object entity, EntityNames type) {
        if (entity == null) return;
        LinkedEntity link = new LinkedEntity().instanceId(utilities.ReflectionCache.getInstanceId(entity))
                .metaId(utilities.ReflectionCache.getMetaId(entity)).uid(utilities.ReflectionCache.getUid(entity)).entityType(type.name());
        utilities.ReflectionCache.invokeSetter(dto, methodName, LinkedEntity.class, link);
    }
    @Override public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Softwareapplication> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwareapplication.class);
        if (elementList != null && !elementList.isEmpty()) {
            Softwareapplication edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.SOFTWAREAPPLICATION.name());
        }
        return null;
    }
}
