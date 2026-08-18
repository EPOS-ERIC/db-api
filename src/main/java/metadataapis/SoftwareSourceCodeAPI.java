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
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class SoftwareSourceCodeAPI extends AbstractAPI<org.epos.eposdatamodel.SoftwareSourceCode> {

    private static final Logger LOG = Logger.getLogger(SoftwareSourceCodeAPI.class.getName());

    public SoftwareSourceCodeAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(SoftwareSourceCode obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());
        String oldInstanceId = previousObj != null ? previousObj.getInstanceId() : null;

        String searchInstanceId = obj.getInstanceId();

        List<Softwaresourcecode> returnList = getDbaccess().getOneFromDB(searchInstanceId, obj.getMetaId(), obj.getUid(), null, getEdmClass());

        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Softwaresourcecode selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Softwaresourcecode::getVersion);
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

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

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

        
            LinkedEntity result = new LinkedEntity().entityType(entityName).instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    private <T> void syncPolymorphicRelation(List<LinkedEntity> links, Softwaresourcecode parent, Class<T> joinClass,
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
        List<T> newRelations = new ArrayList<>();
        for (LinkedEntity link : links) {
            Object targetEntity = findEntityByLinkedEntity(link, overrideStatus);
            if (targetEntity != null) {
                String targetInstanceId = getInstanceId(targetEntity);
                if (targetInstanceId != null) {
                    String key = link.getEntityType() + ":" + targetInstanceId;
                    newKeys.add(key);
                    if (!existingRelations.containsKey(key)) {
                        try {
                            T pi = joinClass.getDeclaredConstructor().newInstance();
                            // Performance: Use cached reflection for setters
                            utilities.ReflectionCache.invokeSetter(pi, "setSoftwaresourcecode", Softwaresourcecode.class, parent);
                            utilities.ReflectionCache.invokeStringSetter(pi, "setSoftwaresourcecodeInstanceId", parent.getInstanceId());
                            utilities.ReflectionCache.invokeStringSetter(pi, "setResourceEntity", link.getEntityType());
                            utilities.ReflectionCache.invokeStringSetter(pi, "setEntityInstanceId", targetInstanceId);

                            newRelations.add(pi);
                        } catch (Exception e) {
                            LOG.warning("Failed to create polymorphic relation (likely duplicate): " + e.getMessage());
                        }
                    }
                }
            } else {
                createPendingCreatorRelation(parent.getInstanceId(), link, joinClass.getName());
            }
        }

        List<Object> obsoleteRelations = new ArrayList<>();
        if (!isNewVersion) {
            for (Map.Entry<String, Object> entry : existingRelations.entrySet()) {
                if (!newKeys.contains(entry.getKey())) obsoleteRelations.add(entry.getValue());
            }
        }

        if (!EposDataModelDAO.getInstance().deleteListOfObjects(obsoleteRelations)) {
            LOG.warning("Failed to delete obsolete polymorphic relations");
        }
        if (!EposDataModelDAO.getInstance().updateListOfObjects(newRelations)) {
            LOG.warning("Failed to create polymorphic relations (likely duplicate)");
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
        // Performance: Use cached reflection
        return utilities.ReflectionCache.getInstanceId(entity);
    }
    
    private String getVersionStatus(Object entity) {
        // Performance: Use cached reflection
        return utilities.ReflectionCache.getVersionStatus(entity);
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
            List<SoftwaresourcecodeElement> relationsToDelete = new ArrayList<>();
            List<Element> elementsToDelete = new ArrayList<>();
            for (Map.Entry<String, SoftwaresourcecodeElement> entry : existingElements.entrySet()) {
                if (!values.contains(entry.getKey())) {
                    relationsToDelete.add(entry.getValue());
                    if (entry.getValue().getElementInstance() != null) {
                        elementsToDelete.add(entry.getValue().getElementInstance());
                    }
                }
            }
            EposDataModelDAO.getInstance().deleteListOfObjects(relationsToDelete);
            EposDataModelDAO.getInstance().deleteListOfObjects(elementsToDelete);
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
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Softwaresourcecode.class, List.of(
                new EposDataModelDAO.RelationField(SoftwaresourcecodeContactpoint.class, "softwaresourcecodeInstance"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeIdentifier.class, "softwaresourcecodeInstance"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeCategory.class, "softwaresourcecodeInstance"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeElement.class, "softwaresourcecodeInstance"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeAuthor.class, "softwaresourcecode"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeContributor.class, "softwaresourcecode"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeFunder.class, "softwaresourcecode"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeMaintainer.class, "softwaresourcecode"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeProvider.class, "softwaresourcecode"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodePublisher.class, "softwaresourcecode"),
                new EposDataModelDAO.RelationField(SoftwaresourcecodeCreator.class, "softwaresourcecode")));
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

    @Override public org.epos.eposdatamodel.SoftwareSourceCode retrieveByUID(String uid) {
        List<Softwaresourcecode> returnList = getDbaccess().getOneFromDBByUID(uid, Softwaresourcecode.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveBunch(List<String> entities) { return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Softwaresourcecode.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAll() { return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Softwaresourcecode.class)); }
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAllWithStatus(StatusType status) { return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Softwaresourcecode.class, status)); }

    /**
     * Returns list-oriented SoftwareSourceCode records without loading their
     * relationship graph. Use {@link #retrieveAll()} when linked metadata is needed.
     */
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveBunchSummary(List<String> entities) {
        return retrieveSummary(getDbaccess().getListIDsFromDBByInstanceId(entities, Softwaresourcecode.class));
    }
    @Override public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAllSummaryWithStatus(StatusType status) {
        return retrieveSummary(getDbaccess().getAllIDsFromDBWithStatus(Softwaresourcecode.class, status));
    }
    @Override
    public List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveAllSummary() {
        return retrieveSummary(getDbaccess().getAllIDsFromDB(Softwaresourcecode.class));
    }
    private List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveSummary(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();

        EposDataModelDAO<?> dao = getDbaccess();
        Map<String, EposDataModelDAO.SoftwareSourceCodeSummaryRow> rows = dao
                .fetchSoftwareSourceCodeSummaryRows(instanceIds).stream()
                .collect(Collectors.toMap(EposDataModelDAO.SoftwareSourceCodeSummaryRow::instanceId, row -> row));
        List<org.epos.eposdatamodel.SoftwareSourceCode> results = new ArrayList<>(rows.size());
        for (String id : instanceIds) {
            EposDataModelDAO.SoftwareSourceCodeSummaryRow row = rows.get(id);
            if (row == null) continue;
            org.epos.eposdatamodel.SoftwareSourceCode dto = toSummaryDto(row);
            VersioningStatusAPI.applyVersion(dto, VersioningStatusAPI.summaryVersion(row.versionId(), row.versionMetaId(),
                    row.changeComment(), row.changeTimestamp(), row.editorId(), row.provenance(), row.version(),
                    row.instanceChangeId(), row.status()), Collections.emptyList());
            results.add(dto);
        }
        return results;
    }

    private List<org.epos.eposdatamodel.SoftwareSourceCode> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();

        Map<String, Softwaresourcecode> sourceCodes = getDbaccess().batchFetchByInstanceIds(instanceIds, Softwaresourcecode.class);
        if (sourceCodes.isEmpty()) return Collections.emptyList();
        List<String> foundIds = new ArrayList<>(sourceCodes.keySet());

        Map<String, List<SoftwaresourcecodeCategory>> categories = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecodeInstance", foundIds, SoftwaresourcecodeCategory.class);
        Map<String, List<SoftwaresourcecodeContactpoint>> contactPoints = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecodeInstance", foundIds, SoftwaresourcecodeContactpoint.class);
        Map<String, List<SoftwaresourcecodeIdentifier>> identifiers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecodeInstance", foundIds, SoftwaresourcecodeIdentifier.class);
        Map<String, List<SoftwaresourcecodeElement>> elements = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecodeInstance", foundIds, SoftwaresourcecodeElement.class);
        Map<String, List<SoftwaresourcecodeAuthor>> authors = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecode", foundIds, SoftwaresourcecodeAuthor.class);
        Map<String, List<SoftwaresourcecodeContributor>> contributors = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecode", foundIds, SoftwaresourcecodeContributor.class);
        Map<String, List<SoftwaresourcecodeFunder>> funders = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecode", foundIds, SoftwaresourcecodeFunder.class);
        Map<String, List<SoftwaresourcecodeMaintainer>> maintainers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecode", foundIds, SoftwaresourcecodeMaintainer.class);
        Map<String, List<SoftwaresourcecodeProvider>> providers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecode", foundIds, SoftwaresourcecodeProvider.class);
        Map<String, List<SoftwaresourcecodePublisher>> publishers = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecode", foundIds, SoftwaresourcecodePublisher.class);
        Map<String, List<SoftwaresourcecodeCreator>> creators = getDbaccess()
                .batchFetchRelationsForMultipleParents("softwaresourcecode", foundIds, SoftwaresourcecodeCreator.class);

        Set<String> categoryIds = new HashSet<>(), contactPointIds = new HashSet<>(), identifierIds = new HashSet<>(), elementIds = new HashSet<>();
        categories.values().forEach(relations -> relations.forEach(relation -> categoryIds.add(relation.getId().getCategoryInstanceId())));
        contactPoints.values().forEach(relations -> relations.forEach(relation -> contactPointIds.add(relation.getId().getContactpointInstanceId())));
        identifiers.values().forEach(relations -> relations.forEach(relation -> identifierIds.add(relation.getId().getIdentifierInstanceId())));
        elements.values().forEach(relations -> relations.forEach(relation -> elementIds.add(relation.getId().getElementInstanceId())));

        Set<String> personIds = new HashSet<>(), organizationIds = new HashSet<>();
        collectPolymorphicIds(authors.values(), personIds, organizationIds);
        collectPolymorphicIds(contributors.values(), personIds, organizationIds);
        collectPolymorphicIds(funders.values(), personIds, organizationIds);
        collectPolymorphicIds(maintainers.values(), personIds, organizationIds);
        collectPolymorphicIds(providers.values(), personIds, organizationIds);
        collectPolymorphicIds(publishers.values(), personIds, organizationIds);
        collectPolymorphicIds(creators.values(), personIds, organizationIds);

        Map<String, Category> categoryMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(categoryIds), Category.class);
        Map<String, Contactpoint> contactPointMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(contactPointIds), Contactpoint.class);
        Map<String, Identifier> identifierMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(identifierIds), Identifier.class);
        Map<String, Element> elementMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(elementIds), Element.class);
        Map<String, Person> personMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(personIds), Person.class);
        Map<String, Organization> organizationMap = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(organizationIds), Organization.class);
        Map<String, Versioningstatus> versions = getDbaccess().batchFetchVersioningStatus(foundIds);
        List<String> metaIds = sourceCodes.values().stream().map(Softwaresourcecode::getMetaId)
                .filter(Objects::nonNull).distinct().toList();
        Map<String, List<String>> groups = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(metaIds);

        List<org.epos.eposdatamodel.SoftwareSourceCode> results = new ArrayList<>(sourceCodes.size());
        for (String id : instanceIds) {
            Softwaresourcecode entity = sourceCodes.get(id);
            if (entity == null) continue;
            org.epos.eposdatamodel.SoftwareSourceCode dto = toBulkDto(entity);
            for (SoftwaresourcecodeCategory relation : categories.getOrDefault(id, Collections.emptyList()))
                addLinked(dto, "addCategory", categoryMap.get(relation.getId().getCategoryInstanceId()), EntityNames.CATEGORY);
            for (SoftwaresourcecodeContactpoint relation : contactPoints.getOrDefault(id, Collections.emptyList()))
                addLinked(dto, "addContactPoint", contactPointMap.get(relation.getId().getContactpointInstanceId()), EntityNames.CONTACTPOINT);
            for (SoftwaresourcecodeIdentifier relation : identifiers.getOrDefault(id, Collections.emptyList()))
                addLinked(dto, "addIdentifier", identifierMap.get(relation.getId().getIdentifierInstanceId()), EntityNames.IDENTIFIER);
            for (SoftwaresourcecodeElement relation : elements.getOrDefault(id, Collections.emptyList())) {
                Element element = elementMap.get(relation.getId().getElementInstanceId());
                if (element != null && ElementType.PROGRAMMINGLANGUAGE.name().equals(element.getType())) dto.addProgrammingLanguage(element.getValue());
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

    private org.epos.eposdatamodel.SoftwareSourceCode toBulkDto(Softwaresourcecode entity) {
        org.epos.eposdatamodel.SoftwareSourceCode dto = new org.epos.eposdatamodel.SoftwareSourceCode();
        dto.setInstanceId(entity.getInstanceId()); dto.setMetaId(entity.getMetaId()); dto.setUid(entity.getUid());
        dto.setName(entity.getName()); dto.setDescription(entity.getDescription()); dto.setDownloadURL(entity.getDownloadurl());
        dto.addKeywords(entity.getKeywords()); dto.setLicenseURL(entity.getLicenseurl()); dto.setMainEntityofPage(entity.getMainentityofpage());
        dto.setRuntimePlatform(entity.getRuntimeplatform()); dto.setSoftwareVersion(entity.getSoftwareversion());
        dto.setCodeRepository(entity.getCoderepository()); dto.setSoftwareStatus(entity.getSoftwareStatus());
        dto.setSpatial(entity.getSpatial()); dto.setTemporal(entity.getTemporal()); dto.setSize(entity.getFilesize());
        dto.setTimeRequired(entity.getTimerequired()); dto.setSoftwareRequirements(entity.getSoftwarerequirements());
        return dto;
    }

    private org.epos.eposdatamodel.SoftwareSourceCode toSummaryDto(EposDataModelDAO.SoftwareSourceCodeSummaryRow row) {
        org.epos.eposdatamodel.SoftwareSourceCode dto = new org.epos.eposdatamodel.SoftwareSourceCode();
        dto.setInstanceId(row.instanceId()); dto.setMetaId(row.metaId()); dto.setUid(row.uid());
        dto.setName(row.name()); dto.setDescription(row.description()); dto.setDownloadURL(row.downloadUrl());
        dto.addKeywords(row.keywords()); dto.setLicenseURL(row.licenseUrl()); dto.setMainEntityofPage(row.mainEntityOfPage());
        dto.setRuntimePlatform(row.runtimePlatform()); dto.setSoftwareVersion(row.softwareVersion());
        dto.setCodeRepository(row.codeRepository()); dto.setSoftwareStatus(row.softwareStatus());
        dto.setSpatial(row.spatial()); dto.setTemporal(row.temporal()); dto.setSize(row.fileSize());
        dto.setTimeRequired(row.timeRequired()); dto.setSoftwareRequirements(row.softwareRequirements());
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

    private void addPolymorphicRelations(org.epos.eposdatamodel.SoftwareSourceCode dto, Collection<?> relations,
                                         Map<String, Person> people, Map<String, Organization> organizations, String methodName) {
        if (relations == null) return;
        for (Object relation : relations) {
            String type = utilities.ReflectionCache.invokeStringGetter(relation, "getResourceEntity");
            String id = utilities.ReflectionCache.invokeStringGetter(relation, "getEntityInstanceId");
            if (EntityNames.PERSON.name().equals(type)) addLinked(dto, methodName, people.get(id), EntityNames.PERSON);
            if (EntityNames.ORGANIZATION.name().equals(type)) addLinked(dto, methodName, organizations.get(id), EntityNames.ORGANIZATION);
        }
    }

    private void addLinked(org.epos.eposdatamodel.SoftwareSourceCode dto, String methodName, Object entity, EntityNames type) {
        if (entity == null) return;
        LinkedEntity link = new LinkedEntity().instanceId(utilities.ReflectionCache.getInstanceId(entity))
                .metaId(utilities.ReflectionCache.getMetaId(entity)).uid(utilities.ReflectionCache.getUid(entity)).entityType(type.name());
        utilities.ReflectionCache.invokeSetter(dto, methodName, LinkedEntity.class, link);
    }
    @Override public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Softwaresourcecode> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Softwaresourcecode.class);
        if (elementList != null && !elementList.isEmpty()) {
            Softwaresourcecode edmobj = elementList.get(0);
            return new LinkedEntity().instanceId(edmobj.getInstanceId()).metaId(edmobj.getMetaId()).uid(edmobj.getUid()).entityType(EntityNames.SOFTWARESOURCECODE.name());
        }
        return null;
    }
}
