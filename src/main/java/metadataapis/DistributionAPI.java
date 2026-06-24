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


        boolean accessServiceExplicitlySet = isFieldExplicitlySet(obj, "accessService");
        boolean supportedOperationExplicitlySet = isFieldExplicitlySet(obj, "supportedOperation");
        boolean dataProductExplicitlySet = isFieldExplicitlySet(obj, "dataProduct");
        boolean accessURLExplicitlySet = isFieldExplicitlySet(obj, "accessURL");
        boolean downloadURLExplicitlySet = isFieldExplicitlySet(obj, "downloadURL");

        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

        String searchInstanceId = obj.getInstanceId();

        List<Distribution> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            Distribution selectedEntity = returnList.get(0);
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            for (Distribution item : returnList) {
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

        obj = (org.epos.eposdatamodel.Distribution) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        String newInstanceId = obj.getInstanceId();

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
        List<String> titles = obj.getTitle();
        if (titles != null && !titles.isEmpty()) {
            RelationSyncUtil.syncSimpleOneToMany(
                    edmobj, edmobj.getInstanceId(), titles, model.DistributionTitle.class,
                    "distributionInstance", "Title",
                    model.DistributionTitle::getTitle, model.DistributionTitle::setTitle, model.DistributionTitle::setDistributionInstance
            );
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.copySimpleOneToMany(
                    oldInstanceId, edmobj, newInstanceId, model.DistributionTitle.class,
                    "distributionInstance", "Title",
                    model.DistributionTitle::getTitle, model.DistributionTitle::setTitle, model.DistributionTitle::setDistributionInstance
            );
        }

        /** DESCRIPTION **/
        List<String> descriptions = obj.getDescription();
        if (descriptions != null && !descriptions.isEmpty()) {
            RelationSyncUtil.syncSimpleOneToMany(
                    edmobj, edmobj.getInstanceId(), descriptions, model.DistributionDescription.class,
                    "distributionInstance", "Description",
                    model.DistributionDescription::getDescription, model.DistributionDescription::setDescription, model.DistributionDescription::setDistributionInstance
            );
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.copySimpleOneToMany(
                    oldInstanceId, edmobj, newInstanceId, model.DistributionDescription.class,
                    "distributionInstance", "Description",
                    model.DistributionDescription::getDescription, model.DistributionDescription::setDescription, model.DistributionDescription::setDistributionInstance
            );
        }

        /** DATAPRODUCT **/
        if (dataProductExplicitlySet || !isNewVersion) {
            List<LinkedEntity> dataProducts = obj.getDataProduct();
            if (dataProducts != null && !dataProducts.isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), dataProducts, relationFromUpdate, relationToUpdate,
                        DistributionDataproduct.class, Dataproduct.class,
                        "distributionInstance",
                        DistributionDataproduct::getDataproductInstance,
                        DistributionDataproduct::setDistributionInstance,
                        DistributionDataproduct::setDataproductInstance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    DistributionDataproduct.class, Dataproduct.class,
                    "distributionInstance",
                    DistributionDataproduct::getDataproductInstance,
                    DistributionDataproduct::setDistributionInstance,
                    DistributionDataproduct::setDataproductInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        /** ACCESSSERVICE **/
        if (accessServiceExplicitlySet || !isNewVersion) {
            List<LinkedEntity> accessServices = obj.getAccessService();
            if (accessServices != null && !accessServices.isEmpty()) {
                // System.out.println("AccessService: " + accessServices);
                // System.out.println(overrideStatus);
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), accessServices, relationFromUpdate, relationToUpdate,
                        WebserviceDistribution.class, Webservice.class,
                        "distributionInstance",
                        WebserviceDistribution::getWebserviceInstance,
                        WebserviceDistribution::setDistributionInstance,
                        WebserviceDistribution::setWebserviceInstance,
                        obj, previousObj, overrideStatus, true
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    WebserviceDistribution.class, Webservice.class,
                    "distributionInstance",
                    WebserviceDistribution::getWebserviceInstance,
                    WebserviceDistribution::setDistributionInstance,
                    WebserviceDistribution::setWebserviceInstance,
                    obj, previousObj, overrideStatus, true
            );
        }

        /** SUPPORTEDOPERATION **/
        if (supportedOperationExplicitlySet || !isNewVersion) {
            List<LinkedEntity> operations = obj.getSupportedOperation();
            if (operations != null && !operations.isEmpty()) {
                RelationSyncUtil.syncComplexRelation(
                        edmobj, edmobj.getInstanceId(), operations, relationFromUpdate, relationToUpdate,
                        OperationDistribution.class, Operation.class,
                        "distributionInstance",
                        OperationDistribution::getOperationInstance,
                        OperationDistribution::setDistributionInstance,
                        OperationDistribution::setOperationInstance,
                        obj, previousObj, overrideStatus, false
                );
            }
        } else if (isNewVersion && oldInstanceId != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), null, relationFromUpdate, relationToUpdate,
                    OperationDistribution.class, Operation.class,
                    "distributionInstance",
                    OperationDistribution::getOperationInstance,
                    OperationDistribution::setDistributionInstance,
                    OperationDistribution::setOperationInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        /** ACCESSURL **/
        if (accessURLExplicitlySet || !isNewVersion) {
            List<String> accessURLs = obj.getAccessURL();
            if (accessURLs != null && !accessURLs.isEmpty()) {
                for (String url : accessURLs) {
                    createInnerElement(ElementType.ACCESSURL, url, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.ACCESSURL, overrideStatus);
        }

        /** DOWNLOADURL **/
        if (downloadURLExplicitlySet || !isNewVersion) {
            List<String> downloadURLs = obj.getDownloadURL();
            if (downloadURLs != null && !downloadURLs.isEmpty()) {
                for (String url : downloadURLs) {
                    createInnerElement(ElementType.DOWNLOADURL, url, edmobj, overrideStatus);
                }
            }
        } else if (isNewVersion && oldInstanceId != null) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.DOWNLOADURL, overrideStatus);
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
        if (oldElements != null) {
            for (Object obj : oldElements) {
                DistributionElement oldDe = (DistributionElement) obj;
                if (oldDe.getElementInstance() != null && type.name().equals(oldDe.getElementInstance().getType())) {
                    createInnerElement(type, oldDe.getElementInstance().getValue(), newEdmobj, overrideStatus);
                }
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Distribution edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = getDbaccess().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), DistributionElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                DistributionElement relation = (DistributionElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null &&
                        existingElement.getType().equals(elementType.name()) &&
                        existingElement.getValue().equals(value)) {
                    return;
                }
            }
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
        }
    }

    

    

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("distributionInstance", instanceId, DistributionTitle.class);
        deleteRelations("distributionInstance", instanceId, DistributionElement.class);
        deleteRelations("distributionInstance", instanceId, DistributionDescription.class);
        deleteRelations("distributionInstance", instanceId, DistributionDataproduct.class);
        deleteRelations("distributionInstance", instanceId, OperationDistribution.class);
        deleteRelations("distributionInstance", instanceId, WebserviceDistribution.class);

        List<Distribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Distribution.class);
        for (Distribution object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Distribution retrieve(String instanceId) {
        List<Distribution> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Distribution.class);
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

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("distributionInstance", edmobj.getInstanceId(), WebserviceDistribution.class)) {
            WebserviceDistribution item = (WebserviceDistribution) object;
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
        for (WebserviceDistribution rel : webservices.getOrDefault(instanceId, Collections.emptyList())) {
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