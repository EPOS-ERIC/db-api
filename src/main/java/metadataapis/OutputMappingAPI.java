package metadataapis;

import abstractapis.AbstractAPI;
import commonapis.EposDataModelEntityIDAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.LinkedEntity;
import relationsapi.RelationSyncUtil;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OutputMappingAPI extends AbstractAPI<org.epos.eposdatamodel.OutputMapping> {

    public OutputMappingAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(org.epos.eposdatamodel.OutputMapping obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        String searchInstanceId = obj.getInstanceId();

        List<OutputMapping> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        String oldInstanceId = null;
        if (!returnList.isEmpty()) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            OutputMapping selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, OutputMapping::getVersion);
            oldInstanceId = selectedEntity.getInstanceId();
            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.OutputMapping) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        boolean isUpdate = oldInstanceId != null && oldInstanceId.equals(obj.getInstanceId());
        boolean isNewVersion = obj.getInstanceChangedId() != null && !isUpdate;

        OutputMapping edmobj = new OutputMapping();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());

        getDbaccess().updateObject(edmobj);

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));

        // For simple fields: if new version and field not provided, copy from previous version
        if (isNewVersion && oldInstanceId != null) {
            OutputMapping oldEntity = (OutputMapping) getDbaccess().getOneFromDBByInstanceId(oldInstanceId, OutputMapping.class).stream().findFirst().orElse(null);
            if (oldEntity != null) {
                edmobj.setLabel(obj.getOutputLabel() != null ? obj.getOutputLabel() : oldEntity.getLabel());
                edmobj.setValuepattern(obj.getOutputValuePattern() != null ? obj.getOutputValuePattern() : oldEntity.getValuepattern());
                edmobj.setRequired(obj.getOutputRequired() != null ? Boolean.parseBoolean(obj.getOutputRequired()) : oldEntity.getRequired());
                edmobj.setRange(obj.getOutputRange() != null ? obj.getOutputRange() : oldEntity.getRange());
                edmobj.setProperty(obj.getOutputProperty() != null ? obj.getOutputProperty() : oldEntity.getProperty());
                edmobj.setVariable(obj.getOutputVariable() != null ? obj.getOutputVariable() : oldEntity.getVariable());
            } else {
                setFieldsFromInput(edmobj, obj);
            }
        } else {
            setFieldsFromInput(edmobj, obj);
        }

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.OUTPUTMAPPING.name(), edmobj);

        
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

    private void setFieldsFromInput(OutputMapping edmobj, org.epos.eposdatamodel.OutputMapping obj) {
        edmobj.setLabel(obj.getOutputLabel());
        edmobj.setValuepattern(obj.getOutputValuePattern());
        edmobj.setRequired(obj.getOutputRequired() != null ? Boolean.parseBoolean(obj.getOutputRequired()) : null);
        edmobj.setRange(obj.getOutputRange());
        edmobj.setProperty(obj.getOutputProperty());
        edmobj.setVariable(obj.getOutputVariable());
    }

    @Override
    public Boolean delete(String instanceId) {
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, OutputMapping.class,
                Map.of(PayloadOutputMapping.class, "outputMappingInstance"));
    }

    @Override
    public org.epos.eposdatamodel.OutputMapping retrieve(String instanceId) {
        List<OutputMapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, OutputMapping.class);
        if (elementList == null || elementList.isEmpty()) return null;

        OutputMapping edmobj = elementList.get(0);
        org.epos.eposdatamodel.OutputMapping o = new org.epos.eposdatamodel.OutputMapping();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setOutputLabel(edmobj.getLabel());
        o.setOutputValuePattern(edmobj.getValuepattern());
        o.setOutputRequired(edmobj.getRequired() != null ? Boolean.toString(edmobj.getRequired()) : null);
        o.setOutputRange(edmobj.getRange());
        o.setOutputProperty(edmobj.getProperty());
        o.setOutputVariable(edmobj.getVariable());

        o = (org.epos.eposdatamodel.OutputMapping) VersioningStatusAPI.retrieveVersion(o);
        return o;
    }

    @Override
    public org.epos.eposdatamodel.OutputMapping retrieveByUID(String uid) {
        List<OutputMapping> returnList = getDbaccess().getOneFromDBByUID(uid, OutputMapping.class);
        return !returnList.isEmpty() ? retrieve(returnList.get(0).getInstanceId()) : null;
    }

    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, OutputMapping.class));
    }

    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(OutputMapping.class));
    }

    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveBunchSummary(List<String> entities) {
        return retrieveSummary(getDbaccess().getListIDsFromDBByInstanceId(entities, OutputMapping.class));
    }
    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveAllSummaryWithStatus(StatusType status) {
        return retrieveSummary(getDbaccess().getAllIDsFromDBWithStatus(OutputMapping.class, status));
    }
    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveAllSummary() {
        return retrieveSummary(getDbaccess().getAllIDsFromDB(OutputMapping.class));
    }
    private List<org.epos.eposdatamodel.OutputMapping> retrieveSummary(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();
        EposDataModelDAO<?> dao = getDbaccess();
        Map<String, EposDataModelDAO.OutputMappingSummaryRow> rows = dao.fetchOutputMappingSummaryRows(instanceIds).stream()
                .collect(Collectors.toMap(EposDataModelDAO.OutputMappingSummaryRow::instanceId, row -> row));
        List<org.epos.eposdatamodel.OutputMapping> results = new ArrayList<>(rows.size());
        for (String id : instanceIds) {
            EposDataModelDAO.OutputMappingSummaryRow row = rows.get(id);
            if (row == null) continue;
            org.epos.eposdatamodel.OutputMapping dto = new org.epos.eposdatamodel.OutputMapping();
            dto.setInstanceId(row.instanceId()); dto.setMetaId(row.metaId()); dto.setUid(row.uid());
            dto.setOutputLabel(row.label()); dto.setOutputValuePattern(row.valuepattern());
            dto.setOutputRequired(row.required() != null ? Boolean.toString(row.required()) : null);
            dto.setOutputRange(row.range()); dto.setOutputProperty(row.property()); dto.setOutputVariable(row.variable());
            VersioningStatusAPI.applyVersion(dto, VersioningStatusAPI.summaryVersion(row.versionId(), row.versionMetaId(),
                    row.changeComment(), row.changeTimestamp(), row.editorId(), row.provenance(), row.version(),
                    row.instanceChangeId(), row.status()), Collections.emptyList());
            results.add(dto);
        }
        return results;
    }

    @Override
    public List<org.epos.eposdatamodel.OutputMapping> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(OutputMapping.class, status));
    }

    private List<org.epos.eposdatamodel.OutputMapping> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> instanceIds = dbFetcher.apply(null);
        return retrieveBulk(instanceIds, OutputMapping.class, this::toDto);
    }

    private org.epos.eposdatamodel.OutputMapping toDto(OutputMapping entity) {
        org.epos.eposdatamodel.OutputMapping dto = new org.epos.eposdatamodel.OutputMapping();
        dto.setInstanceId(entity.getInstanceId());
        dto.setMetaId(entity.getMetaId());
        dto.setUid(entity.getUid());
        dto.setOutputLabel(entity.getLabel());
        dto.setOutputValuePattern(entity.getValuepattern());
        dto.setOutputRequired(entity.getRequired() != null ? Boolean.toString(entity.getRequired()) : null);
        dto.setOutputRange(entity.getRange());
        dto.setOutputProperty(entity.getProperty());
        dto.setOutputVariable(entity.getVariable());
        return dto;
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<OutputMapping> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, OutputMapping.class);
        if (elementList != null && !elementList.isEmpty()) {
            OutputMapping edmobj = elementList.get(0);
            LinkedEntity o = new LinkedEntity();
            o.setInstanceId(edmobj.getInstanceId());
            o.setMetaId(edmobj.getMetaId());
            o.setUid(edmobj.getUid());
            o.setEntityType(EntityNames.OUTPUTMAPPING.name());
            return o;
        }
        return null;
    }
}
