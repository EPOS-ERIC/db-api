package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.LinkedEntityAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationChecker;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.util.*;
import java.util.function.Function;
import java.util.logging.Logger;

public class PayloadAPI extends AbstractAPI<org.epos.eposdatamodel.Payload> {

    private static final Logger LOG = Logger.getLogger(PayloadAPI.class.getName());

    public PayloadAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.Payload obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        // Performance: Single retrieve call instead of potentially calling twice
        EPOSDataModelEntity previousObj = retrieve(obj.getInstanceId());
        String oldInstanceId = previousObj != null ? previousObj.getInstanceId() : null;

        String searchInstanceId = obj.getInstanceId();

        List<Payload> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Payload selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Payload::getVersion);
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.Payload) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        Payload edmobj = new Payload();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));

        // OUTPUT MAPPING
        if (obj.getOutputMapping() != null) {
            RelationSyncUtil.syncComplexRelation(
                    edmobj, edmobj.getInstanceId(), obj.getOutputMapping(), relationFromUpdate, relationToUpdate,
                    PayloadOutputMapping.class, OutputMapping.class,
                    "payloadInstance", PayloadOutputMapping::getOutputMappingInstance, PayloadOutputMapping::setPayloadInstance, PayloadOutputMapping::setOutputMappingInstance,
                    obj, previousObj, overrideStatus, false
            );
        }

        // An Operation owns its payload collection. This inverse is derived from
        // OperationPayload and is intentionally read-only.

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.PAYLOAD.name(), edmobj);

        
            LinkedEntity result = new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
            repointPublishedVersion(obj, oldInstanceId, edmobj.getClass());
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    /**
     * Extract instanceId from an object that could be either a DTO or a JPA entity
     */
    private String extractInstanceId(Object obj) {
        if (obj == null) return null;

        try {
            // Try DTO method first (getInstanceId)
            java.lang.reflect.Method getInstanceId = obj.getClass().getMethod("getInstanceId");
            Object result = getInstanceId.invoke(obj);
            if (result != null) return result.toString();
        } catch (Exception e) {
            // Ignore and try next approach
        }

        try {
            // Try JPA entity method (instanceId field)
            java.lang.reflect.Field field = obj.getClass().getDeclaredField("instanceId");
            field.setAccessible(true);
            Object result = field.get(obj);
            if (result != null) return result.toString();
        } catch (Exception e) {
            LOG.warning("Could not extract instanceId from object: " + obj.getClass().getName());
        }

        return null;
    }

    @Override
    public Boolean delete(String instanceId) {
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Payload.class, Map.of(
                PayloadOutputMapping.class, "payloadInstance",
                OperationPayload.class, "payloadInstance"));
    }

    @Override
    public org.epos.eposdatamodel.Payload retrieve(String instanceId) {
        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        if (elementList == null || elementList.isEmpty()) return null;

        Payload edmobj = elementList.get(0);
        org.epos.eposdatamodel.Payload o = new org.epos.eposdatamodel.Payload();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("payloadInstance", edmobj.getInstanceId(), OperationPayload.class)) {
            OperationPayload item = (OperationPayload) object;
            LinkedEntity le = retrieveAPI(EntityNames.OPERATION.name()).retrieveLinkedEntity(item.getId().getOperationInstanceId());
            o.setSupportedOperation(le);
        }

        for (Object object : getDbaccess().getOneFromDBBySpecificKey("payloadInstance", edmobj.getInstanceId(), PayloadOutputMapping.class)) {
            PayloadOutputMapping item = (PayloadOutputMapping) object;
            LinkedEntity le = retrieveAPI(EntityNames.OUTPUTMAPPING.name()).retrieveLinkedEntity(item.getId().getOutputMappingInstanceId());
            o.addOutputMapping(le);
        }

        o = (org.epos.eposdatamodel.Payload) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.Payload retrieveByUID(String uid) {
        List<Payload> returnList = getDbaccess().getOneFromDBByUID(uid, Payload.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Payload.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Payload.class));
    }

    /** Returns list-oriented records without loading relations or groups. */
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveBunchSummary(List<String> entities) {
        return retrieveSummary(getDbaccess().getListIDsFromDBByInstanceId(entities, Payload.class));
    }
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveAllSummaryWithStatus(StatusType status) {
        return retrieveSummary(getDbaccess().getAllIDsFromDBWithStatus(Payload.class, status));
    }
    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveAllSummary() {
        return retrieveSummary(getDbaccess().getAllIDsFromDB(Payload.class));
    }
    private List<org.epos.eposdatamodel.Payload> retrieveSummary(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();

        EposDataModelDAO<?> dao = getDbaccess();
        Map<String, EposDataModelDAO.PayloadSummaryRow> rows = dao.fetchPayloadSummaryRows(instanceIds)
                .stream().collect(java.util.stream.Collectors.toMap(EposDataModelDAO.PayloadSummaryRow::instanceId, row -> row));
        List<org.epos.eposdatamodel.Payload> results = new ArrayList<>(rows.size());
        for (String id : instanceIds) {
            EposDataModelDAO.PayloadSummaryRow row = rows.get(id);
            if (row == null) continue;
            org.epos.eposdatamodel.Payload dto = new org.epos.eposdatamodel.Payload();
            dto.setInstanceId(row.instanceId()); dto.setMetaId(row.metaId()); dto.setUid(row.uid());
            VersioningStatusAPI.applyVersion(dto, VersioningStatusAPI.summaryVersion(row.versionId(), row.versionMetaId(),
                    row.changeComment(), row.changeTimestamp(), row.editorId(), row.provenance(), row.version(),
                    row.instanceChangeId(), row.status()), Collections.emptyList());
            results.add(dto);
        }
        return results;
    }

    @Override
    public List<org.epos.eposdatamodel.Payload> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Payload.class, status));
    }
    private List<org.epos.eposdatamodel.Payload> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();

        Map<String, Payload> payloads = getDbaccess().batchFetchByInstanceIds(instanceIds, Payload.class);
        if (payloads.isEmpty()) return Collections.emptyList();
        List<String> foundIds = new ArrayList<>(payloads.keySet());
        Map<String, List<OperationPayload>> operationRelations = getDbaccess()
                .batchFetchRelationsForMultipleParents("payloadInstance", foundIds, OperationPayload.class);
        Map<String, List<PayloadOutputMapping>> mappingRelations = getDbaccess()
                .batchFetchRelationsForMultipleParents("payloadInstance", foundIds, PayloadOutputMapping.class);

        Set<String> operationIds = new HashSet<>();
        operationRelations.values().forEach(relations -> relations.forEach(relation -> {
            if (relation.getId() != null) operationIds.add(relation.getId().getOperationInstanceId());
        }));
        Set<String> mappingIds = new HashSet<>();
        mappingRelations.values().forEach(relations -> relations.forEach(relation -> {
            if (relation.getId() != null) mappingIds.add(relation.getId().getOutputMappingInstanceId());
        }));

        Map<String, Operation> operations = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(operationIds), Operation.class);
        Map<String, OutputMapping> mappings = getDbaccess().batchFetchByInstanceIds(new ArrayList<>(mappingIds), OutputMapping.class);
        Map<String, Versioningstatus> versions = getDbaccess().batchFetchVersioningStatus(foundIds);
        List<String> metaIds = payloads.values().stream().map(Payload::getMetaId).filter(Objects::nonNull).distinct().toList();
        Map<String, List<String>> groups = UserGroupManagementAPI.batchRetrieveGroupsFromMetaIds(metaIds);

        List<org.epos.eposdatamodel.Payload> results = new ArrayList<>(payloads.size());
        for (String id : instanceIds) {
            Payload entity = payloads.get(id);
            if (entity == null) continue;
            org.epos.eposdatamodel.Payload dto = new org.epos.eposdatamodel.Payload();
            dto.setInstanceId(entity.getInstanceId());
            dto.setMetaId(entity.getMetaId());
            dto.setUid(entity.getUid());
            for (OperationPayload relation : operationRelations.getOrDefault(id, Collections.emptyList())) {
                Operation operation = relation.getId() == null ? null : operations.get(relation.getId().getOperationInstanceId());
                if (operation != null) dto.setSupportedOperation(toLinkedEntity(operation, EntityNames.OPERATION));
            }
            for (PayloadOutputMapping relation : mappingRelations.getOrDefault(id, Collections.emptyList())) {
                OutputMapping mapping = relation.getId() == null ? null : mappings.get(relation.getId().getOutputMappingInstanceId());
                if (mapping != null) dto.addOutputMapping(toLinkedEntity(mapping, EntityNames.OUTPUTMAPPING));
            }
            VersioningStatusAPI.applyVersion(dto, versions.get(id), groups.get(dto.getMetaId()));
            results.add(dto);
        }
        return results;
    }

    private LinkedEntity toLinkedEntity(Object entity, EntityNames type) {
        return new LinkedEntity().instanceId(utilities.ReflectionCache.getInstanceId(entity))
                .metaId(utilities.ReflectionCache.getMetaId(entity))
                .uid(utilities.ReflectionCache.getUid(entity)).entityType(type.name());
    }
    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Payload> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Payload.class);
        if (elementList != null && !elementList.isEmpty()) {
            Payload edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.PAYLOAD.name());
            return o;
        }
        return null;
    }
}
