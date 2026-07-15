package commonapis;

import dao.EposDataModelDAO;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import usermanagementapis.UserGroupManagementAPI;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import static model.StatusType.*;

public class VersioningStatusAPI {

    static Logger LOG = Logger.getLogger(VersioningStatusAPI.class.getName());

    public static EPOSDataModelEntity checkVersion(EPOSDataModelEntity obj, StatusType overrideStatus) {
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[VERSION CHECK START] Checking version for entity: uid={0}, instanceId={1}, metaId={2}, status={3}, overrideStatus={4}",
                    new Object[]{obj.getUid(), obj.getInstanceId(), obj.getMetaId(), obj.getStatus(), overrideStatus});
        }

        Versioningstatus edmobj = null;

        if (obj.getInstanceId() != null) {
            List<Versioningstatus> list = getDbaccess().getOneFromDBByInstanceId(obj.getInstanceId(), Versioningstatus.class);
            if (!list.isEmpty()) {
                edmobj = list.get(0);
            } else {
                list = getDbaccess().getOneFromDBByInstanceIdNoCache(obj.getInstanceId(), Versioningstatus.class);
                if (!list.isEmpty()) edmobj = list.get(0);
            }
        }

        if (edmobj == null && obj.getUid() != null) {
            List<Versioningstatus> list = getDbaccess().getOneFromDBByUIDNoCache(obj.getUid(), Versioningstatus.class);
            edmobj = selectVersionForTransition(list, obj, overrideStatus);
        }

        if (edmobj != null) {
            StatusType targetStatus = overrideStatus != null ? overrideStatus : obj.getStatus();
            if (targetStatus == null) targetStatus = DRAFT;
            boolean editorWasOmitted = obj.getEditorId() == null;
            // Requests may omit version metadata on updates. Keep the persisted
            // editor so draft ownership and relation resolution remain stable.
            if (obj.getEditorId() == null) {
                obj.setEditorId(edmobj.getEditorId());
            }
            StatusType currentDbStatus = StatusType.valueOf(edmobj.getStatus());
            boolean reuseExistingDraft = targetStatus == DRAFT
                    && currentDbStatus == DRAFT
                    && (editorWasOmitted || sameEditor(obj.getEditorId(), edmobj.getEditorId()));

            if (LOG.isLoggable(java.util.logging.Level.FINE)) {
                LOG.log(java.util.logging.Level.FINE, "[VERSION CHECK] Existing version found. currentDbStatus={0}, targetStatus={1}, reuseExistingDraft={2}",
                        new Object[]{currentDbStatus, targetStatus, reuseExistingDraft});
            }

            if (targetStatus == DRAFT && !reuseExistingDraft) {
                createNewVersion(obj, edmobj, targetStatus);
            } else {
                if (targetStatus == PUBLISHED && currentDbStatus != PUBLISHED) {
                    String searchUid = edmobj.getUid() != null ? edmobj.getUid() : obj.getUid();
                    archiveOldPublishedVersions(searchUid, edmobj.getVersionId());
                }
                updateExistingVersion(obj, edmobj, targetStatus);
            }

            obj.setStatus(targetStatus);
            return obj;

        } else {
            StatusType initialStatus = overrideStatus != null ? overrideStatus :
                    (obj.getStatus() != null ? obj.getStatus() : DRAFT);

            if (LOG.isLoggable(java.util.logging.Level.FINE)) {
                LOG.log(java.util.logging.Level.FINE, "[VERSION CHECK] No existing version found. Creating first version with status {0}", initialStatus);
            }

            createFirstVersion(obj, initialStatus);
            return obj;
        }
    }

    private static Versioningstatus selectVersionForTransition(List<Versioningstatus> versions, EPOSDataModelEntity obj, StatusType overrideStatus) {
        if (versions == null || versions.isEmpty()) {
            return null;
        }

        StatusType targetStatus = overrideStatus != null ? overrideStatus : obj.getStatus();
        if (targetStatus == null) {
            targetStatus = DRAFT;
        }

        if (targetStatus == DRAFT) {
            Versioningstatus sameEditorDraft = findDraftForEditor(versions, obj.getEditorId());
            if (sameEditorDraft != null) {
                return sameEditorDraft;
            }

            Versioningstatus published = findPublishedVersion(versions);
            if (published != null) {
                return published;
            }

            return findFirstNonPending(versions);
        }

        Versioningstatus matching = findByStatus(versions, targetStatus);
        if (matching != null) {
            return matching;
        }

        return findFirstNonPending(versions);
    }

    /**
     * Selects the source version before an API mutates the incoming DTO.
     * Drafts are scoped by editor; another editor's draft is never a source
     * when a published version is available.
     */
    public static <T> T selectVersion(List<T> versions, String editorId, StatusType targetStatus,
                                      Function<T, Versioningstatus> versionGetter) {
        if (versions == null || versions.isEmpty()) return null;

        if (targetStatus == DRAFT && editorId != null) {
            for (T entity : versions) {
                Versioningstatus version = versionGetter.apply(entity);
                if (version != null && DRAFT.toString().equals(version.getStatus())
                        && sameEditor(editorId, version.getEditorId())) {
                    return entity;
                }
            }
        }

        if (targetStatus != null) {
            for (T entity : versions) {
                Versioningstatus version = versionGetter.apply(entity);
                if (version != null && targetStatus.toString().equals(version.getStatus())) {
                    if (targetStatus != DRAFT) return entity;
                }
            }
        }

        if (targetStatus == DRAFT) {
            for (T entity : versions) {
                Versioningstatus version = versionGetter.apply(entity);
                if (version != null && PUBLISHED.toString().equals(version.getStatus())) {
                    return entity;
                }
            }
        }

        return versions.get(0);
    }

    private static Versioningstatus findDraftForEditor(List<Versioningstatus> versions, String editorId) {
        for (Versioningstatus vs : versions) {
            if (vs == null || isPendingRelationMarker(vs)) {
                continue;
            }
            if (DRAFT.toString().equals(vs.getStatus()) && sameEditor(editorId, vs.getEditorId())) {
                return vs;
            }
        }
        return null;
    }

    private static Versioningstatus findPublishedVersion(List<Versioningstatus> versions) {
        return findByStatus(versions, PUBLISHED);
    }

    private static Versioningstatus findByStatus(List<Versioningstatus> versions, StatusType status) {
        if (status == null) {
            return null;
        }
        for (Versioningstatus vs : versions) {
            if (vs == null || isPendingRelationMarker(vs)) {
                continue;
            }
            if (status.toString().equals(vs.getStatus())) {
                return vs;
            }
        }
        return null;
    }

    private static Versioningstatus findFirstNonPending(List<Versioningstatus> versions) {
        for (Versioningstatus vs : versions) {
            if (vs != null && !isPendingRelationMarker(vs)) {
                return vs;
            }
        }
        return null;
    }

    private static boolean sameEditor(String left, String right) {
        if (left == null || right == null) return false;
        return left.trim().equalsIgnoreCase(right.trim());
    }

    private static boolean isPendingRelationMarker(Versioningstatus vs) {
        if (vs == null) return false;

        if (!StatusType.PENDING.name().equals(vs.getStatus())) {
            return false;
        }

        String metaId = vs.getMetaId();
        if (metaId != null && metaId.contains(".")) {
            return true;
        }

        String provenance = vs.getProvenance();
        String changeComment = vs.getChangeComment();
        if (provenance != null && changeComment != null) {
            if (provenance.equals(provenance.toUpperCase()) &&
                    changeComment.equals(changeComment.toUpperCase()) &&
                    provenance.length() > 3 && changeComment.length() > 3) {
                return true;
            }
        }

        return false;
    }

    private static void archiveOldPublishedVersions(String uid, String currentVersionId) {
        if (uid == null) return;

        List<Versioningstatus> allVersionsRaw = getDbaccess().getOneFromDBByUIDNoCache(uid, Versioningstatus.class);

        for (Versioningstatus rawVs : allVersionsRaw) {
            if (rawVs.getVersionId().equals(currentVersionId)) continue;

            if (isPendingRelationMarker(rawVs)) continue;

            if (PUBLISHED.toString().equals(rawVs.getStatus())) {
                rawVs.setStatus(ARCHIVED.toString());
                rawVs.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
                rawVs.setChangeComment("Auto-archived");
                getDbaccess().updateObject(rawVs);
            }
        }
    }

    private static void createNewVersion(EPOSDataModelEntity obj, Versioningstatus previousVer, StatusType newStatus) {
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[VERSIONING] Creating new version from previousVer instanceId: {0}, newStatus={1}",
                    new Object[]{previousVer.getInstanceId(), newStatus});
        }
        Versioningstatus newVersionEntity = new Versioningstatus();
        newVersionEntity.setStatus(newStatus.toString());

        newVersionEntity.setInstanceChangeId(previousVer.getInstanceId());
        obj.setInstanceChangedId(previousVer.getInstanceId());

        String newInstanceId = UUID.randomUUID().toString();
        String newVersionId = UUID.randomUUID().toString();

        newVersionEntity.setInstanceId(newInstanceId);
        obj.setInstanceId(newInstanceId);

        newVersionEntity.setVersionId(newVersionId);
        obj.setVersionId(newVersionId);

        newVersionEntity.setMetaId(previousVer.getMetaId());
        newVersionEntity.setUid(previousVer.getUid());
        newVersionEntity.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
        newVersionEntity.setChangeComment(obj.getChangeComment());
        newVersionEntity.setEditorId(obj.getEditorId());
        newVersionEntity.setProvenance(obj.getFileProvenance());
        newVersionEntity.setVersion(obj.getVersion());

        getDbaccess().createObject(newVersionEntity);
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[VERSIONING] Created new version entity successfully: instanceId={0}, versionId={1}",
                    new Object[]{newInstanceId, newVersionId});
        }
    }

    private static void updateExistingVersion(EPOSDataModelEntity obj, Versioningstatus currentVer, StatusType newStatus) {
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[VERSIONING] Updating existing version instanceId: {0} to status={1}",
                    new Object[]{currentVer.getInstanceId(), newStatus});
        }
        currentVer.setStatus(newStatus.toString());

        obj.setInstanceChangedId(currentVer.getInstanceChangeId());
        obj.setInstanceId(currentVer.getInstanceId());
        obj.setVersionId(currentVer.getVersionId());

        currentVer.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
        if (obj.getChangeComment() != null) currentVer.setChangeComment(obj.getChangeComment());
        if (obj.getEditorId() != null) currentVer.setEditorId(obj.getEditorId());
        if (obj.getFileProvenance() != null) currentVer.setProvenance(obj.getFileProvenance());
        if (obj.getVersion() != null) currentVer.setVersion(obj.getVersion());

        getDbaccess().updateObject(currentVer);
    }

    private static void createFirstVersion(EPOSDataModelEntity obj, StatusType status) {
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[VERSIONING] Creating first version for uid={0} with status={1}",
                    new Object[]{obj.getUid(), status});
        }
        Versioningstatus edmobj = new Versioningstatus();
        edmobj.setStatus(status.toString());

        edmobj.setInstanceId(Optional.ofNullable(obj.getInstanceId()).orElse(UUID.randomUUID().toString()));
        obj.setInstanceId(edmobj.getInstanceId());

        edmobj.setMetaId(Optional.ofNullable(obj.getMetaId()).orElse(UUID.randomUUID().toString()));
        obj.setMetaId(edmobj.getMetaId());

        edmobj.setVersionId(UUID.randomUUID().toString());
        obj.setVersionId(edmobj.getVersionId());

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse("entity/" + UUID.randomUUID().toString()));
        edmobj.setInstanceChangeId(null);
        edmobj.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
        edmobj.setChangeComment(obj.getChangeComment());
        edmobj.setEditorId(obj.getEditorId());
        edmobj.setProvenance(obj.getFileProvenance());
        edmobj.setVersion(obj.getVersion());

        getDbaccess().createObject(edmobj);
        if (LOG.isLoggable(java.util.logging.Level.FINE)) {
            LOG.log(java.util.logging.Level.FINE, "[VERSIONING] Created first version entity successfully: instanceId={0}, metaId={1}, versionId={2}",
                    new Object[]{edmobj.getInstanceId(), edmobj.getMetaId(), edmobj.getVersionId()});
        }
    }

    public static EPOSDataModelEntity retrieveVersion(EPOSDataModelEntity obj) {

        Versioningstatus vs = null;

        if (obj.getInstanceId() != null) {
            List<Versioningstatus> returnList = getDbaccess().getOneFromDBByInstanceIdNoCache(obj.getInstanceId(), Versioningstatus.class);

            for (Versioningstatus candidate : returnList) {
                if (!isPendingRelationMarker(candidate)) {
                    vs = candidate;
                    break;
                }
            }
        }

        if (vs == null && obj.getUid() != null) {
            List<Versioningstatus> uidList = getDbaccess().getOneFromDBByUIDNoCache(obj.getUid(), Versioningstatus.class);
            for (Versioningstatus candidate : uidList) {
                if (!isPendingRelationMarker(candidate)) {
                    vs = candidate;
                    break;
                }
            }
        }

        if (vs == null) {
            List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(),
                    obj.getMetaId(),
                    obj.getUid(),
                    obj.getVersionId(),
                    Versioningstatus.class
            );
            for (Versioningstatus candidate : returnList) {
                if (!isPendingRelationMarker(candidate)) {
                    vs = candidate;
                    break;
                }
            }
        }

        if (vs == null) {
            LOG.warning("[VersioningStatusAPI] No Versioningstatus found for entity - " +
                    "instanceId=" + obj.getInstanceId() + ", uid=" + obj.getUid() +
                    ". Returning entity with default status.");
            if (obj.getStatus() == null) {
                obj.setStatus(DRAFT);
            }
            return obj;
        }

        obj.setVersionId(vs.getVersionId());
        obj.setChangeComment(vs.getChangeComment());
        if (vs.getChangeTimestamp() != null) {
            obj.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
        }
        obj.setEditorId(vs.getEditorId());
        obj.setFileProvenance(vs.getProvenance());
        obj.setVersion(vs.getVersion());
        obj.setInstanceChangedId(vs.getInstanceChangeId());

        obj.setGroups(UserGroupManagementAPI.retrieveShortGroupsFromMetaId(obj.getMetaId()));

        //TODO: reviewerid and reviewercomment

        try {
            obj.setStatus(StatusType.valueOf(vs.getStatus()));
        } catch (IllegalArgumentException e) {
            obj.setStatus(DRAFT);
        }
        return obj;
    }

    public static Versioningstatus retrieveVersioningStatus(EPOSDataModelEntity obj) {
        List<Versioningstatus> returnList = null;

        if(obj.getInstanceId() == null) {
            returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(), obj.getMetaId(), obj.getUid(), obj.getVersionId(), Versioningstatus.class
            );
        } else {
            returnList = getDbaccess().getOneFromDBByInstanceIdNoCache(obj.getInstanceId(), Versioningstatus.class);
        }

        return returnList.isEmpty() ? null : returnList.get(0);
    }

    private static EposDataModelDAO<Versioningstatus> getDbaccess() {
        return EposDataModelDAO.getInstance();
    }
}
