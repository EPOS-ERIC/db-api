package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import model.Distribution;
import model.Element;
import model.Operation;
import org.epos.eposdatamodel.*;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class DistributionAPI extends AbstractAPI<org.epos.eposdatamodel.Distribution> {

    private static final Logger LOG = Logger.getLogger(DistributionAPI.class.getName());

    public DistributionAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Distribution obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

        String searchInstanceId = obj.getInstanceId();
        boolean lookupByUidOnly = searchInstanceId == null && obj.getUid() != null;
        final String requestedInstanceId = searchInstanceId;

        List<Distribution> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            final StatusType requestedStatus = targetStatus;
            final String requestedEditorId = obj.getEditorId();
            Distribution selectedEntity = returnList.stream()
                    .filter(item -> requestedStatus != StatusType.DRAFT
                            && requestedInstanceId != null && requestedInstanceId.equals(item.getInstanceId()))
                    .findFirst()
                    .orElseGet(() -> VersioningStatusAPI.selectVersion(
                            returnList, requestedEditorId, requestedStatus, Distribution::getVersion));
            oldInstanceId = selectedEntity.getInstanceId();
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());

            if (previousObj == null) {
                previousObj = retrieve(selectedEntity.getInstanceId());
            }
            if (lookupByUidOnly && targetStatus == StatusType.DRAFT) {
                obj.setInstanceId(null);
            }
        }

        obj = (org.epos.eposdatamodel.Distribution) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceChangedId() != null && obj.getAccessService() == null) {
            org.epos.eposdatamodel.Distribution previousVersion = retrieve(obj.getInstanceChangedId());
            if (previousVersion != null && previousVersion.getAccessService() != null) {
                obj.setAccessService(previousVersion.getAccessService());
            }
        }

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        Distribution edmobj = new Distribution();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setFormat(obj.getFormat());
        edmobj.setLicense(obj.getLicence());
        edmobj.setType(obj.getType());
        edmobj.setDatapolicy(obj.getDataPolicy());
        edmobj.setByteSize(obj.getByteSize());
        edmobj.setMaturity(obj.getMaturity());
        edmobj.setMediaType(obj.getMediaType());

        if (obj.getModified() != null) edmobj.setModified(obj.getModified());
        if (obj.getIssued() != null) edmobj.setIssued(obj.getIssued());

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }

        /** TITLE **/
        RelationSyncUtil.syncSimpleOneToManyWithVersionFallback(
                edmobj, edmobj.getInstanceId(), obj.getTitle(), model.DistributionTitle.class,
                "distributionInstance", "Title",
                model.DistributionTitle::getTitle, model.DistributionTitle::setTitle, model.DistributionTitle::setDistributionInstance,
                obj, oldInstanceId
        );

        /** DESCRIPTION **/
        RelationSyncUtil.syncSimpleOneToManyWithVersionFallback(
                edmobj, edmobj.getInstanceId(), obj.getDescription(), model.DistributionDescription.class,
                "distributionInstance", "Description",
                model.DistributionDescription::getDescription, model.DistributionDescription::setDescription, model.DistributionDescription::setDistributionInstance,
                obj, oldInstanceId
        );

        // A DataProduct owns its Distribution collection. This inverse is exposed
        // on reads but must not replace DistributionDataproduct rows from here.

        /** ACCESSSERVICE **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getAccessService(),
                RelationSyncUtil.toLinkedEntity(previousObj), RelationSyncUtil.toLinkedEntity(obj),
                WebserviceDistribution.class, Webservice.class,
                "distributionInstance",
                WebserviceDistribution::getWebserviceInstance,
                WebserviceDistribution::setDistributionInstance,
                WebserviceDistribution::setWebserviceInstance,
                obj, previousObj, overrideStatus, true
        );

        /** SUPPORTEDOPERATION **/
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getSupportedOperation(),
                RelationSyncUtil.toLinkedEntity(previousObj), RelationSyncUtil.toLinkedEntity(obj),
                OperationDistribution.class, Operation.class,
                "distributionInstance",
                OperationDistribution::getOperationInstance,
                OperationDistribution::setDistributionInstance,
                OperationDistribution::setOperationInstance,
                obj, previousObj, overrideStatus, false
        );

        /** ACCESSURL **/
        if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.ACCESSURL, overrideStatus);
        } else {
            replaceInnerElements(edmobj, obj.getAccessURL(), ElementType.ACCESSURL, overrideStatus);
        }

        /** DOWNLOADURL **/
        if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.DOWNLOADURL, overrideStatus);
        } else {
            replaceInnerElements(edmobj, obj.getDownloadURL(), ElementType.DOWNLOADURL, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.DISTRIBUTION.name(), edmobj);

        
            LinkedEntity result = new LinkedEntity()
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid())
                .entityType(EntityNames.DISTRIBUTION.name());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    private void deleteExistingElements(String instanceId) {
        List<Object> existingElements = getDbaccess().getOneFromDBBySpecificKey("distributionInstance", instanceId, DistributionElement.class);
        if (existingElements != null) {
            for (Object obj : existingElements) {
                DistributionElement de = (DistributionElement) obj;
                EposDataModelDAO.getInstance().deleteObject(de);
                if (de.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(de.getElementInstance());
                }
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Distribution newEdmobj, ElementType type, StatusType overrideStatus) {
        List<Object> oldElements = getDbaccess().getOneFromDBBySpecificKey("distributionInstance", oldInstanceId, DistributionElement.class);
        Set<ElementValue> existingElements = getExistingElementValues(newEdmobj.getInstanceId());
        if (oldElements != null) {
            for (Object obj : oldElements) {
                DistributionElement oldDe = (DistributionElement) obj;
                if (oldDe.getElementInstance() != null && type.name().equals(oldDe.getElementInstance().getType())) {
                    createInnerElement(type, oldDe.getElementInstance().getValue(), newEdmobj, overrideStatus, existingElements);
                }
            }
        }
    }

    private Set<ElementValue> getExistingElementValues(String distributionInstanceId) {
        List<DistributionElement> relations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("distributionInstance", distributionInstanceId, DistributionElement.class);
        Set<ElementValue> values = new HashSet<>();
        if (relations != null) {
            for (DistributionElement relation : relations) {
                Element element = relation.getElementInstance();
                if (element != null && element.getType() != null) {
                    values.add(new ElementValue(element.getType(), element.getValue()));
                }
            }
        }
        return values;
    }

    private void createInnerElement(ElementType elementType, String value, Distribution edmobj, StatusType overrideStatus,
                                    Set<ElementValue> existingElements) {
        ElementValue elementValue = new ElementValue(elementType.name(), value);
        if (!existingElements.add(elementValue)) {
            return;
        }

        org.epos.eposdatamodel.Element element = new org.epos.eposdatamodel.Element();
        element.setType(elementType);
        element.setValue(value);

        Versioningstatus version = edmobj.getVersion();
        if (version != null) {
            if (version.getEditorId() != null)
                element.setEditorId(version.getEditorId());
            if (version.getProvenance() != null)
                element.setFileProvenance(version.getProvenance());
            if (version.getChangeComment() != null)
                element.setChangeComment(version.getChangeComment());
            if (version.getChangeTimestamp() != null)
                element.setChangeTimestamp(version.getChangeTimestamp().toLocalDateTime());
        }

        LinkedEntity le = new ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            DistributionElement ce = new DistributionElement();
            ce.setDistributionInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        } else {
            existingElements.remove(elementValue);
        }
    }

    private void replaceInnerElements(Distribution edmobj, List<String> values, ElementType elementType, StatusType overrideStatus) {
        deleteInnerElementsByType(edmobj.getInstanceId(), elementType);
        if (values != null && !values.isEmpty()) {
            Set<ElementValue> existingElements = getExistingElementValues(edmobj.getInstanceId());
            for (String value : values) {
                createInnerElement(elementType, value, edmobj, overrideStatus, existingElements);
            }
        }
    }

    private record ElementValue(String type, String value) {
    }

    private void deleteInnerElementsByType(String distributionInstanceId, ElementType type) {
        List<DistributionElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("distributionInstance", distributionInstanceId, DistributionElement.class);
        if (existingRelations != null) {
            for (DistributionElement relation : existingRelations) {
                Element element = relation.getElementInstance();
                if (element != null && type.name().equals(element.getType())) {
                    EposDataModelDAO.getInstance().deleteObject(relation);
                    EposDataModelDAO.getInstance().deleteObject(element);
                }
            }
        }
    }

    

    

    @Override
    public Boolean delete(String instanceId) {
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Distribution.class, Map.of(
                DistributionTitle.class, "distributionInstance",
                DistributionElement.class, "distributionInstance",
                DistributionDescription.class, "distributionInstance",
                DistributionDataproduct.class, "distributionInstance",
                OperationDistribution.class, "distributionInstance",
                WebserviceDistribution.class, "distributionInstance"));
    }

    @Override
    public org.epos.eposdatamodel.Distribution retrieve(String instanceId) {
        List<Distribution> elementList = getDbaccess().getOneFromDBByInstanceIdNoCache(instanceId, Distribution.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Distribution edmobj = elementList.get(0);
        org.epos.eposdatamodel.Distribution o = new org.epos.eposdatamodel.Distribution();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setFormat(edmobj.getFormat());
        o.setLicence(edmobj.getLicense());
        o.setDataPolicy(edmobj.getDatapolicy());
        o.setIssued(edmobj.getIssued());
        o.setModified(edmobj.getModified());
        o.setByteSize(edmobj.getByteSize());
        o.setMaturity(edmobj.getMaturity());
        o.setMediaType(edmobj.getMediaType());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), DistributionDescription.class)) {
            DistributionDescription item = (DistributionDescription) object;
            o.addDescription(item.getDescription());
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), DistributionTitle.class)) {
            DistributionTitle item = (DistributionTitle) object;
            o.addTitle(item.getTitle());
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), DistributionDataproduct.class)) {
            DistributionDataproduct item = (DistributionDataproduct) object;
            LinkedEntity le = retrieveAPI(EntityNames.DATAPRODUCT.name()).retrieveLinkedEntity(item.getDataproductInstance().getInstanceId());
            o.addDataproduct(le);
        }

        List<Object> rawAccessServiceRelations = getDbaccess()
                .getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), WebserviceDistribution.class);
        List<WebserviceDistribution> accessServiceRelations = rawAccessServiceRelations
                .stream()
                .map(WebserviceDistribution.class::cast)
                .collect(Collectors.toList());
        for (WebserviceDistribution item : selectAccessServiceRelations(accessServiceRelations, edmobj.getVersion())) {
            LinkedEntity le = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveLinkedEntity(item.getWebserviceInstance().getInstanceId());
            o.addAccessService(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), OperationDistribution.class)) {
            OperationDistribution item = (OperationDistribution) object;
            LinkedEntity le = retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getOperationInstance().getInstanceId());
            o.addSupportedOperation(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), DistributionElement.class)) {
            DistributionElement item = (DistributionElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.ACCESSURL.name())) o.addAccessURL(el.getValue());
            if (el.getType().equals(ElementType.DOWNLOADURL.name())) o.addDownloadURL(el.getValue());
        }

        o = (org.epos.eposdatamodel.Distribution) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Distribution retrieveByUID(String uid) {
        List<Distribution> returnList = getDbaccess().getOneFromDBByUID(uid, Distribution.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }

    @Override
    public List<org.epos.eposdatamodel.Distribution> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Distribution.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Distribution> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Distribution.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Distribution> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Distribution.class, status));
    }

    private List<org.epos.eposdatamodel.Distribution> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    /**
     * Bulk retrieval implementation that minimizes database queries.
     * Instead of N+1 queries per entity, this fetches all data in batches.
     */
    private List<org.epos.eposdatamodel.Distribution> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Batch fetch all Distribution entities
        Map<String, Distribution> distributions = getDbaccess().batchFetchByInstanceIds(instanceIds, Distribution.class);
        
        if (distributions.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> foundIds = new ArrayList<>(distributions.keySet());
        
        // Step 2: Batch fetch ALL join tables for ALL distributions at once
        Map<String, List<DistributionDescription>> descriptions = 
                getDbaccess().batchFetchRelationsForMultipleParents("distributionInstance", foundIds, DistributionDescription.class);
        Map<String, List<DistributionTitle>> titles = 
                getDbaccess().batchFetchRelationsForMultipleParents("distributionInstance", foundIds, DistributionTitle.class);
        Map<String, List<DistributionDataproduct>> dataproducts = 
                getDbaccess().batchFetchRelationsForMultipleParents("distributionInstance", foundIds, DistributionDataproduct.class);
        Map<String, List<WebserviceDistribution>> webservices = 
                getDbaccess().batchFetchRelationsForMultipleParents("distributionInstance", foundIds, WebserviceDistribution.class);
        Map<String, List<OperationDistribution>> operations = 
                getDbaccess().batchFetchRelationsForMultipleParents("distributionInstance", foundIds, OperationDistribution.class);
        Map<String, List<DistributionElement>> elements = 
                getDbaccess().batchFetchRelationsForMultipleParents("distributionInstance", foundIds, DistributionElement.class);
        
        // Step 3: Collect all target entity IDs for batch fetching
        Set<String> allDataproductIds = new HashSet<>();
        Set<String> allWebserviceIds = new HashSet<>();
        Set<String> allOperationIds = new HashSet<>();
        
        dataproducts.values().forEach(list -> list.forEach(r -> {
            if (r.getDataproductInstance() != null) allDataproductIds.add(r.getDataproductInstance().getInstanceId());
        }));
        webservices.values().forEach(list -> list.forEach(r -> {
            if (r.getWebserviceInstance() != null) allWebserviceIds.add(r.getWebserviceInstance().getInstanceId());
        }));
        operations.values().forEach(list -> list.forEach(r -> {
            if (r.getOperationInstance() != null) allOperationIds.add(r.getOperationInstance().getInstanceId());
        }));
        
        // Step 4: Batch fetch all target entities
        Map<String, Dataproduct> dataproductMap = allDataproductIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allDataproductIds), Dataproduct.class);
        Map<String, Webservice> webserviceMap = allWebserviceIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allWebserviceIds), Webservice.class);
        Map<String, Operation> operationMap = allOperationIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allOperationIds), Operation.class);
        
        // Step 5: Batch fetch versioning status
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Step 5b: Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = distributions.values().stream()
                .map(Distribution::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        // Step 6: Assemble all DTOs from pre-fetched data
        List<org.epos.eposdatamodel.Distribution> results = new ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            Distribution edmobj = distributions.get(instanceId);
            if (edmobj != null) {
                org.epos.eposdatamodel.Distribution dto = assembleDistribution(
                        instanceId, edmobj,
                        descriptions, titles, dataproducts, webservices, operations, elements,
                        dataproductMap, webserviceMap, operationMap, versioningMap, groupsMap
                );
                results.add(dto);
            }
        }
        
        return results;
    }

    /**
     * Assembles a Distribution DTO from pre-fetched data without additional queries.
     */
    private org.epos.eposdatamodel.Distribution assembleDistribution(
            String instanceId,
            Distribution edmobj,
            Map<String, List<DistributionDescription>> descriptions,
            Map<String, List<DistributionTitle>> titles,
            Map<String, List<DistributionDataproduct>> dataproducts,
            Map<String, List<WebserviceDistribution>> webservices,
            Map<String, List<OperationDistribution>> operations,
            Map<String, List<DistributionElement>> elements,
            Map<String, Dataproduct> dataproductMap,
            Map<String, Webservice> webserviceMap,
            Map<String, Operation> operationMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
        org.epos.eposdatamodel.Distribution o = new org.epos.eposdatamodel.Distribution();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setType(edmobj.getType());
        o.setFormat(edmobj.getFormat());
        o.setLicence(edmobj.getLicense());
        o.setDataPolicy(edmobj.getDatapolicy());
        o.setIssued(edmobj.getIssued());
        o.setModified(edmobj.getModified());
        o.setByteSize(edmobj.getByteSize());
        o.setMaturity(edmobj.getMaturity());
        o.setMediaType(edmobj.getMediaType());
        
        // Add descriptions
        for (DistributionDescription item : descriptions.getOrDefault(instanceId, Collections.emptyList())) {
            o.addDescription(item.getDescription());
        }
        
        // Add titles
        for (DistributionTitle item : titles.getOrDefault(instanceId, Collections.emptyList())) {
            o.addTitle(item.getTitle());
        }
        
        // Add dataproduct relations
        for (DistributionDataproduct rel : dataproducts.getOrDefault(instanceId, Collections.emptyList())) {
            Dataproduct target = dataproductMap.get(rel.getDataproductInstance().getInstanceId());
            if (target != null) {
                o.addDataproduct(createLinkedEntity(target, EntityNames.DATAPRODUCT.name()));
            }
        }
        
        // Add webservice (accessService) relations
        for (WebserviceDistribution rel : selectAccessServiceRelations(
                webservices.getOrDefault(instanceId, Collections.emptyList()), versioningMap.get(instanceId))) {
            Webservice target = webserviceMap.get(rel.getWebserviceInstance().getInstanceId());
            if (target != null) {
                o.addAccessService(createLinkedEntity(target, EntityNames.WEBSERVICE.name()));
            }
        }
        
        // Add operation (supportedOperation) relations
        for (OperationDistribution rel : operations.getOrDefault(instanceId, Collections.emptyList())) {
            Operation target = operationMap.get(rel.getOperationInstance().getInstanceId());
            if (target != null) {
                o.addSupportedOperation(createLinkedEntity(target, EntityNames.OPERATION.name()));
            }
        }
        
        // Add element data (accessURL, downloadURL)
        for (DistributionElement item : elements.getOrDefault(instanceId, Collections.emptyList())) {
            Element el = item.getElementInstance();
            if (el != null) {
                if (ElementType.ACCESSURL.name().equals(el.getType())) o.addAccessURL(el.getValue());
                if (ElementType.DOWNLOADURL.name().equals(el.getType())) o.addDownloadURL(el.getValue());
            }
        }
        
        // Apply versioning from pre-fetched data
        Versioningstatus vs = versioningMap.get(instanceId);
        if (vs != null) {
            o.setVersionId(vs.getVersionId());
            o.setInstanceChangedId(vs.getInstanceChangeId());
            if (vs.getChangeTimestamp() != null) {
                o.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
            }
            o.setEditorId(vs.getEditorId());
            o.setChangeComment(vs.getChangeComment());
            o.setVersion(vs.getVersion());
            if (vs.getStatus() != null) {
                try {
                    o.setStatus(StatusType.valueOf(vs.getStatus()));
                } catch (Exception e) {
                    // Ignore invalid status
                }
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

    /**
     * Legacy data can contain joins to both a published WebService and a draft
     * of the same logical WebService. A Distribution version may expose only one
     * access service per logical UID; prefer the version compatible with the
     * Distribution status and draft editor.
     */
    private List<WebserviceDistribution> selectAccessServiceRelations(
            Collection<WebserviceDistribution> relations, Versioningstatus distributionVersion) {
        Map<String, WebserviceDistribution> selected = new LinkedHashMap<>();
        for (WebserviceDistribution relation : relations) {
            if (relation == null || relation.getWebserviceInstance() == null) {
                continue;
            }
            Webservice candidate = relation.getWebserviceInstance();
            String key = candidate.getUid() != null ? candidate.getUid() : candidate.getInstanceId();
            WebserviceDistribution current = selected.get(key);
            if (current == null || isPreferredAccessService(candidate, current.getWebserviceInstance(), distributionVersion)) {
                selected.put(key, relation);
            }
        }
        return new ArrayList<>(selected.values());
    }

    private boolean isPreferredAccessService(Webservice candidate, Webservice current,
                                             Versioningstatus distributionVersion) {
        if (candidate == null || candidate.getVersion() == null) {
            return false;
        }
        if (current == null || current.getVersion() == null) {
            return true;
        }
        String parentStatus = distributionVersion != null ? distributionVersion.getStatus() : null;
        String candidateStatus = candidate.getVersion().getStatus();
        String currentStatus = current.getVersion().getStatus();
        if (StatusType.DRAFT.name().equals(parentStatus)) {
            String editorId = distributionVersion.getEditorId();
            boolean candidateOwnedDraft = StatusType.DRAFT.name().equals(candidateStatus)
                    && editorId != null && editorId.equalsIgnoreCase(candidate.getVersion().getEditorId());
            boolean currentOwnedDraft = StatusType.DRAFT.name().equals(currentStatus)
                    && editorId != null && editorId.equalsIgnoreCase(current.getVersion().getEditorId());
            if (candidateOwnedDraft != currentOwnedDraft) {
                return candidateOwnedDraft;
            }
        }
        boolean candidateMatchesParent = parentStatus != null && parentStatus.equals(candidateStatus);
        boolean currentMatchesParent = parentStatus != null && parentStatus.equals(currentStatus);
        if (candidateMatchesParent != currentMatchesParent) {
            return candidateMatchesParent;
        }
        return StatusType.PUBLISHED.name().equals(candidateStatus)
                && !StatusType.PUBLISHED.name().equals(currentStatus);
    }

    /**
     * Creates a LinkedEntity from a JPA entity.
     */
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
        List<Distribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Distribution.class);
        if (elementList != null && !elementList.isEmpty()) {
            Distribution edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.DISTRIBUTION.name());
            return o;
        }
        return null;
    }
}
