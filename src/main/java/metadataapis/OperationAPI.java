package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.*;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.IriTemplate;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OperationAPI extends AbstractAPI<org.epos.eposdatamodel.Operation> {

    public OperationAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Operation obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());

        String searchInstanceId = obj.getInstanceId();

        List<Operation> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            String requestedInstanceId = obj.getInstanceId();
            String requestedEditorId = obj.getEditorId();
            Operation selectedEntity = returnList.stream()
                    .filter(item -> targetStatus != StatusType.DRAFT
                            && requestedInstanceId != null
                            && requestedInstanceId.equals(item.getInstanceId()))
                    .findFirst()
                    .orElseGet(() -> VersioningStatusAPI.selectVersion(
                            returnList, requestedEditorId, targetStatus, Operation::getVersion));
            oldInstanceId = selectedEntity.getInstanceId();
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());

            if (previousObj == null) {
                previousObj = retrieve(selectedEntity.getInstanceId());
            }
        }

        obj = (org.epos.eposdatamodel.Operation) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        Operation edmobj = new Operation();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setMethod(obj.getMethod());
        edmobj.setTemplate(obj.getTemplate());

        if (isUpdate && !isNewVersion) {
            deleteExistingElements(oldInstanceId);
        }

        // MAPPING
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getMapping(), relationFromUpdate, relationToUpdate,
                OperationMapping.class, Mapping.class,
                "operationInstance", OperationMapping::getMappingInstance, OperationMapping::setOperationInstance, OperationMapping::setMappingInstance,
                obj, previousObj, overrideStatus, false
        );

        // WEBSERVICE
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getWebservice(), relationFromUpdate, relationToUpdate,
                OperationWebservice.class, Webservice.class,
                "operationInstance", OperationWebservice::getWebserviceInstance, OperationWebservice::setOperationInstance, OperationWebservice::setWebserviceInstance,
                obj, previousObj, overrideStatus, true
        );

        // PAYLOAD
        RelationSyncUtil.syncComplexRelation(
                edmobj, edmobj.getInstanceId(), obj.getPayload(), relationFromUpdate, relationToUpdate,
                OperationPayload.class, Payload.class,
                "operationInstance", OperationPayload::getPayloadInstance, OperationPayload::setOperationInstance, OperationPayload::setPayloadInstance,
                obj, previousObj, overrideStatus, true
        );

        // RETURNS (Elements)
        if (isNewVersion && oldInstanceId != null && (obj.getReturns() == null || obj.getReturns().isEmpty())) {
            copyElementsFromPreviousVersion(oldInstanceId, edmobj, ElementType.RETURNS, overrideStatus);
        } else {
            replaceInnerElements(edmobj, obj.getReturns(), ElementType.RETURNS, overrideStatus);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.OPERATION.name(), edmobj);

        
            LinkedEntity result = new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    private void deleteExistingElements(String operationInstanceId) {
        List<OperationElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("operationInstance", operationInstanceId, OperationElement.class);

        if (existingRelations != null) {
            for (OperationElement relation : existingRelations) {
                EposDataModelDAO.getInstance().deleteObject(relation);
                // Also delete the Element entity
                if (relation.getElementInstance() != null) {
                    EposDataModelDAO.getInstance().deleteObject(relation.getElementInstance());
                }
            }
        }
    }

    private void copyElementsFromPreviousVersion(String oldInstanceId, Operation newEdmobj, ElementType elementType, StatusType overrideStatus) {
        List<Object> oldRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("operationInstance", oldInstanceId, OperationElement.class);
        if (oldRelations == null) return;

        for (Object obj : oldRelations) {
            OperationElement oldRelation = (OperationElement) obj;
            Element oldElement = oldRelation.getElementInstance();
            if (oldElement != null && oldElement.getType().equals(elementType.name())) {
                createInnerElement(elementType, oldElement.getValue(), newEdmobj, overrideStatus);
            }
        }
    }

    private void replaceInnerElements(Operation edmobj, List<String> values, ElementType type, StatusType overrideStatus) {
        deleteInnerElementsByType(edmobj.getInstanceId(), type);
        if (values != null && !values.isEmpty()) {
            for (String value : values) {
                createInnerElement(type, value, edmobj, overrideStatus);
            }
        }
    }

    private void deleteInnerElementsByType(String operationInstanceId, ElementType type) {
        List<OperationElement> existingRelations = EposDataModelDAO.getInstance()
                .getJoinEntitiesByRelationField("operationInstance", operationInstanceId, OperationElement.class);
        if (existingRelations != null) {
            for (OperationElement relation : existingRelations) {
                Element element = relation.getElementInstance();
                if (element != null && type.name().equals(element.getType())) {
                    EposDataModelDAO.getInstance().deleteObject(relation);
                    EposDataModelDAO.getInstance().deleteObject(element);
                }
            }
        }
    }

    private void createInnerElement(ElementType elementType, String value, Operation edmobj, StatusType overrideStatus) {
        List<Object> existingRelations = EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationElement.class);
        if (existingRelations != null) {
            for (Object obj : existingRelations) {
                OperationElement relation = (OperationElement) obj;
                Element existingElement = relation.getElementInstance();
                if (existingElement != null && existingElement.getType().equals(elementType.name()) && existingElement.getValue().equals(value)) {
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

        LinkedEntity le = new commonapis.ElementAPI(EntityNames.ELEMENT.name(), Element.class).create(element, overrideStatus, null, null);
        List<Element> el = EposDataModelDAO.getInstance().getOneFromDBByInstanceId(le.getInstanceId(), Element.class);

        if (!el.isEmpty()) {
            OperationElement ce = new OperationElement();
            ce.setOperationInstance(edmobj);
            ce.setElementInstance(el.get(0));
            EposDataModelDAO.getInstance().updateObject(ce);
        }
    }

    

    

    @Override
    public Boolean delete(String instanceId) {
        deleteRelations("operationInstance", instanceId, OperationElement.class);
        deleteRelations("operationInstance", instanceId, OperationMapping.class);
        deleteRelations("operationInstance", instanceId, OperationDistribution.class);
        deleteRelations("operationInstance", instanceId, OperationWebservice.class);
        deleteRelations("operationInstance", instanceId, OperationPayload.class);
        deleteRelations("operationInstance", instanceId, SoftwareapplicationOperation.class);

        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        for (Operation object : elementList) {
            EposDataModelDAO.getInstance().deleteObject(object);
        }
        return true;
    }

    private void deleteRelations(String key, String instanceId, Class<?> clazz) {
        List<Object> list = getDbaccess().getOneFromDBBySpecificKey(key, instanceId, clazz);
        if (list != null) list.forEach(EposDataModelDAO.getInstance()::deleteObject);
    }

    @Override
    public org.epos.eposdatamodel.Operation retrieve(String instanceId) {
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Operation edmobj = elementList.get(0);
        org.epos.eposdatamodel.Operation o = new org.epos.eposdatamodel.Operation();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setMethod(edmobj.getMethod());
        o.setTemplate(edmobj.getTemplate());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationWebservice.class)) {
            OperationWebservice item = (OperationWebservice) object;
            LinkedEntity le = retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveLinkedEntity(item.getWebserviceInstance().getInstanceId());
            o.addWebservice(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationPayload.class)) {
            OperationPayload item = (OperationPayload) object;
            LinkedEntity le = retrieveAPI(EntityNames.PAYLOAD.name()).retrieveLinkedEntity(item.getPayloadInstance().getInstanceId());
            o.addPayload(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationMapping.class)) {
            OperationMapping item = (OperationMapping) object;
            LinkedEntity le = retrieveAPI(EntityNames.MAPPING.name()).retrieveLinkedEntity(item.getMappingInstance().getInstanceId());
            o.addMapping(le);
        }

        IriTemplate iriTemplateObject = new IriTemplate();
        iriTemplateObject.setMappings(o.getMapping());
        iriTemplateObject.setTemplate(o.getTemplate());
        o.setIriTemplateObject(iriTemplateObject);

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("operationInstance", edmobj.getInstanceId(), OperationElement.class)) {
            OperationElement item = (OperationElement) object;
            Element el = item.getElementInstance();
            if (el.getType().equals(ElementType.RETURNS.name())) o.addReturns(el.getValue());
        }

        o = (org.epos.eposdatamodel.Operation) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Operation retrieveByUID(String uid) {
        List<Operation> returnList = getDbaccess().getOneFromDBByUID(uid, Operation.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }

    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Operation.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Operation.class));
    }

    @Override
    public List<org.epos.eposdatamodel.Operation> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Operation.class, status));
    }

    private List<org.epos.eposdatamodel.Operation> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }
        return retrieveBulkInternal(instanceIds);
    }

    /**
     * Bulk retrieval implementation that minimizes database queries.
     */
    private List<org.epos.eposdatamodel.Operation> retrieveBulkInternal(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Step 1: Batch fetch all Operation entities
        Map<String, Operation> operations = getDbaccess().batchFetchByInstanceIds(instanceIds, Operation.class);
        if (operations.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> foundIds = new ArrayList<>(operations.keySet());
        
        // Step 2: Batch fetch ALL join tables
        Map<String, List<OperationWebservice>> webservices = 
                getDbaccess().batchFetchRelationsForMultipleParents("operationInstance", foundIds, OperationWebservice.class);
        Map<String, List<OperationPayload>> payloads = 
                getDbaccess().batchFetchRelationsForMultipleParents("operationInstance", foundIds, OperationPayload.class);
        Map<String, List<OperationMapping>> mappings = 
                getDbaccess().batchFetchRelationsForMultipleParents("operationInstance", foundIds, OperationMapping.class);
        Map<String, List<OperationElement>> elements = 
                getDbaccess().batchFetchRelationsForMultipleParents("operationInstance", foundIds, OperationElement.class);
        
        // Step 3: Collect all target entity IDs
        Set<String> allWebserviceIds = new HashSet<>();
        Set<String> allPayloadIds = new HashSet<>();
        Set<String> allMappingIds = new HashSet<>();
        
        webservices.values().forEach(list -> list.forEach(r -> {
            if (r.getWebserviceInstance() != null) allWebserviceIds.add(r.getWebserviceInstance().getInstanceId());
        }));
        payloads.values().forEach(list -> list.forEach(r -> {
            if (r.getPayloadInstance() != null) allPayloadIds.add(r.getPayloadInstance().getInstanceId());
        }));
        mappings.values().forEach(list -> list.forEach(r -> {
            if (r.getMappingInstance() != null) allMappingIds.add(r.getMappingInstance().getInstanceId());
        }));
        
        // Step 4: Batch fetch all target entities
        Map<String, Webservice> webserviceMap = allWebserviceIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allWebserviceIds), Webservice.class);
        Map<String, Payload> payloadMap = allPayloadIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allPayloadIds), Payload.class);
        Map<String, Mapping> mappingMap = allMappingIds.isEmpty() ? Collections.emptyMap() :
                getDbaccess().batchFetchByInstanceIds(new ArrayList<>(allMappingIds), Mapping.class);
        
        // Step 5: Batch fetch versioning status
        Map<String, Versioningstatus> versioningMap = getDbaccess().batchFetchVersioningStatus(foundIds);
        
        // Step 5b: Batch fetch groups for all entities (by metaId)
        List<String> allMetaIds = operations.values().stream()
                .map(Operation::getMetaId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, List<String>> groupsMap = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(allMetaIds);
        
        // Step 6: Assemble DTOs
        List<org.epos.eposdatamodel.Operation> results = new ArrayList<>(foundIds.size());
        for (String instanceId : foundIds) {
            Operation edmobj = operations.get(instanceId);
            if (edmobj != null) {
                org.epos.eposdatamodel.Operation dto = assembleOperation(
                        instanceId, edmobj, webservices, payloads, mappings, elements,
                        webserviceMap, payloadMap, mappingMap, versioningMap, groupsMap
                );
                results.add(dto);
            }
        }
        
        return results;
    }

    private org.epos.eposdatamodel.Operation assembleOperation(
            String instanceId,
            Operation edmobj,
            Map<String, List<OperationWebservice>> webservices,
            Map<String, List<OperationPayload>> payloads,
            Map<String, List<OperationMapping>> mappings,
            Map<String, List<OperationElement>> elements,
            Map<String, Webservice> webserviceMap,
            Map<String, Payload> payloadMap,
            Map<String, Mapping> mappingMap,
            Map<String, Versioningstatus> versioningMap,
            Map<String, List<String>> groupsMap) {
        
        org.epos.eposdatamodel.Operation o = new org.epos.eposdatamodel.Operation();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setMethod(edmobj.getMethod());
        o.setTemplate(edmobj.getTemplate());
        
        // Webservices
        for (OperationWebservice rel : webservices.getOrDefault(instanceId, Collections.emptyList())) {
            Webservice target = webserviceMap.get(rel.getWebserviceInstance().getInstanceId());
            if (target != null) {
                o.addWebservice(createLinkedEntity(target, EntityNames.WEBSERVICE.name()));
            }
        }
        
        // Payloads
        for (OperationPayload rel : payloads.getOrDefault(instanceId, Collections.emptyList())) {
            Payload target = payloadMap.get(rel.getPayloadInstance().getInstanceId());
            if (target != null) {
                o.addPayload(createLinkedEntity(target, EntityNames.PAYLOAD.name()));
            }
        }
        
        // Mappings
        for (OperationMapping rel : mappings.getOrDefault(instanceId, Collections.emptyList())) {
            Mapping target = mappingMap.get(rel.getMappingInstance().getInstanceId());
            if (target != null) {
                o.addMapping(createLinkedEntity(target, EntityNames.MAPPING.name()));
            }
        }
        
        // Build IriTemplate
        IriTemplate iriTemplateObject = new IriTemplate();
        iriTemplateObject.setMappings(o.getMapping());
        iriTemplateObject.setTemplate(o.getTemplate());
        o.setIriTemplateObject(iriTemplateObject);
        
        // Elements (returns)
        for (OperationElement rel : elements.getOrDefault(instanceId, Collections.emptyList())) {
            Element el = rel.getElementInstance();
            if (el != null && ElementType.RETURNS.name().equals(el.getType())) {
                o.addReturns(el.getValue());
            }
        }
        
        // Apply versioning
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
        List<Operation> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Operation.class);
        if (elementList != null && !elementList.isEmpty()) {
            Operation edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.OPERATION.name());
            return o;
        }
        return null;
    }
}
