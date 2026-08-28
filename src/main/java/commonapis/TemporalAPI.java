package commonapis;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.*;
import org.epos.eposdatamodel.Group;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.PeriodOfTime;
import relationsapi.RelationSyncUtil;
import usermanagementapis.UserGroupManagementAPI;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

public class TemporalAPI extends AbstractAPI<org.epos.eposdatamodel.PeriodOfTime> {

    public TemporalAPI(String entityName, Class<?> edmClass) {
        super(entityName, edmClass);
    }

    @Override
    public LinkedEntity create(PeriodOfTime obj, StatusType overrideStatus, LinkedEntity relationFromUpdate, LinkedEntity relationToUpdate) {
        logCreateStart(obj, overrideStatus);
        try {


        String searchInstanceId = obj.getInstanceId();
        List<Temporal> returnList = getDbaccess().getOneFromDB(
                searchInstanceId,
                obj.getMetaId(),
                obj.getUid(),
                null,
                getEdmClass());

        if(!returnList.isEmpty()){
            StatusType targetStatus = overrideStatus != null ? overrideStatus : (obj.getStatus() != null ? obj.getStatus() : StatusType.DRAFT);
            Temporal selectedEntity = VersioningStatusAPI.selectVersion(
                    returnList, obj.getEditorId(), targetStatus, Temporal::getVersion);
            if (selectedEntity == null) {
                selectedEntity = returnList.get(0);
            }

            obj.setInstanceId(selectedEntity.getInstanceId());
            obj.setMetaId(selectedEntity.getMetaId());
            obj.setUid(selectedEntity.getUid());
            if (selectedEntity.getVersion() != null) obj.setVersionId(selectedEntity.getVersion().getVersionId());
        }

        obj = (org.epos.eposdatamodel.PeriodOfTime) VersioningStatusAPI.checkVersion(obj, overrideStatus);

        if (obj.getInstanceId() == null) {
            obj.setInstanceId(UUID.randomUUID().toString());
        }
        if (obj.getMetaId() == null) {
            obj.setMetaId(UUID.randomUUID().toString());
        }

        EposDataModelEntityIDAPI.addEntityToEDMEntityID(obj.getMetaId(), entityName);

        Temporal edmobj = new Temporal();
        edmobj.setVersion(VersioningStatusAPI.retrieveVersioningStatus(obj));
        edmobj.setInstanceId(obj.getInstanceId());
        edmobj.setMetaId(obj.getMetaId());
        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(getEdmClass().getSimpleName() + "/" + UUID.randomUUID().toString()));
        edmobj.setStartdate(obj.getStartDate());
        edmobj.setEnddate(obj.getEndDate());

        getDbaccess().updateObject(edmobj);

        RelationSyncUtil.resolvePendingRelations(edmobj.getUid(), EntityNames.PERIODOFTIME.name(), edmobj);

        
            LinkedEntity result = new LinkedEntity().entityType(entityName)
                .instanceId(edmobj.getInstanceId())
                .metaId(edmobj.getMetaId())
                .uid(edmobj.getUid());
            repointPublishedVersion(obj, null, Temporal.class);
            logCreateEnd(result, null);
            return result;
        } catch (Throwable t) {
            logCreateEnd(null, t);
            throw t;
        }
    }

    @Override
    public Boolean delete(String instanceId) {
        return getDbaccess().deleteByInstanceIdWithRelations(instanceId, Temporal.class, Map.of(
                DataproductTemporal.class, "temporalInstance",
                WebserviceTemporal.class, "temporalInstance",
                EquipmentTemporal.class, "temporalInstance",
                ServiceTemporal.class, "temporalInstance"));
    }

    @Override
    public org.epos.eposdatamodel.PeriodOfTime retrieve(String instanceId) {
        List<Temporal> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Temporal.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Temporal edmobj = elementList.get(0);
        org.epos.eposdatamodel.PeriodOfTime o = new org.epos.eposdatamodel.PeriodOfTime();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setStartDate(edmobj.getStartdate());
        o.setEndDate(edmobj.getEnddate());

        return (org.epos.eposdatamodel.PeriodOfTime) VersioningStatusAPI.retrieveVersion(o);
    }
    @Override
    public org.epos.eposdatamodel.PeriodOfTime retrieveByUID(String uid) {
        List<Temporal> returnList = getDbaccess().getOneFromDBByUID(uid, Temporal.class);
        if (!returnList.isEmpty()) {
            return retrieve(returnList.get(0).getInstanceId());
        }
        return null;
    }

    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveBunch(List<String> entities) {
        return retrieveEntities(db -> getDbaccess().getListIDsFromDBByInstanceId(entities, Temporal.class));
    }
    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAll() {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDB(Temporal.class));
    }
    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveBunchSummary(List<String> entities) {
        return retrieveSummary(getDbaccess().getListIDsFromDBByInstanceId(entities, Temporal.class));
    }
    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAllSummaryWithStatus(StatusType status) {
        return retrieveSummary(getDbaccess().getAllIDsFromDBWithStatus(Temporal.class, status));
    }
    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAllSummary() {
        return retrieveSummary(getDbaccess().getAllIDsFromDB(Temporal.class));
    }
    private List<org.epos.eposdatamodel.PeriodOfTime> retrieveSummary(List<String> instanceIds) {
        if (instanceIds == null || instanceIds.isEmpty()) return Collections.emptyList();
        EposDataModelDAO<?> dao = getDbaccess();
        Map<String, EposDataModelDAO.TemporalSummaryRow> rows = dao.fetchTemporalSummaryRows(instanceIds).stream()
                .collect(Collectors.toMap(EposDataModelDAO.TemporalSummaryRow::instanceId, row -> row));
        List<org.epos.eposdatamodel.PeriodOfTime> results = new java.util.ArrayList<>(rows.size());
        for (String id : instanceIds) {
            EposDataModelDAO.TemporalSummaryRow row = rows.get(id);
            if (row == null) continue;
            org.epos.eposdatamodel.PeriodOfTime dto = new org.epos.eposdatamodel.PeriodOfTime();
            dto.setInstanceId(row.instanceId()); dto.setMetaId(row.metaId()); dto.setUid(row.uid());
            dto.setStartDate(row.startdate()); dto.setEndDate(row.enddate());
            VersioningStatusAPI.applyVersion(dto, VersioningStatusAPI.summaryVersion(row.versionId(), row.versionMetaId(),
                    row.changeComment(), row.changeTimestamp(), row.editorId(), row.provenance(), row.version(),
                    row.instanceChangeId(), row.status()), Collections.emptyList());
            results.add(dto);
        }
        return results;
    }
    @Override
    public List<org.epos.eposdatamodel.PeriodOfTime> retrieveAllWithStatus(StatusType status) {
        return retrieveEntities(db -> getDbaccess().getAllIDsFromDBWithStatus(Temporal.class, status));
    }

    private List<org.epos.eposdatamodel.PeriodOfTime> retrieveEntities(Function<Void, List<String>> dbFetcher) {
        List<String> dbEntities = dbFetcher.apply(null);
        return retrieveBulk(dbEntities, Temporal.class, entity -> {
            org.epos.eposdatamodel.PeriodOfTime dto = new org.epos.eposdatamodel.PeriodOfTime();
            dto.setInstanceId(entity.getInstanceId());
            dto.setMetaId(entity.getMetaId());
            dto.setUid(entity.getUid());
            dto.setStartDate(entity.getStartdate());
            dto.setEndDate(entity.getEnddate());
            return dto;
        });
    }

    @Override
    public LinkedEntity retrieveLinkedEntity(String instanceId) {
        List<Temporal> elementList = getDbaccess().getOneFromDBByInstanceId(instanceId, Temporal.class);
        if (elementList.isEmpty()) {
            return null;
        }

        Temporal edmobj = elementList.get(0);
        LinkedEntity o = new LinkedEntity();
        o.setInstanceId(edmobj.getInstanceId());
        o.setMetaId(edmobj.getMetaId());
        o.setUid(edmobj.getUid());
        o.setEntityType(EntityNames.PERIODOFTIME.name());

        return o;
    }
}
