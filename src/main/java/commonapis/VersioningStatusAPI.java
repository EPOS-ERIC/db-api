package commonapis;

import dao.EposDataModelDAO;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static model.StatusType.*;

public class VersioningStatusAPI {

    static Logger LOG = Logger.getLogger(VersioningStatusAPI.class.getName());

    public static EPOSDataModelEntity checkVersion(EPOSDataModelEntity obj, StatusType overrideStatus) {

        Versioningstatus edmobj = null;

        // 1. Recupero stato attuale (Cache -> Fallback NoCache)
        // Qui usiamo la cache per performance in lettura, ma con fallback se non troviamo nulla
        if (obj.getInstanceId() != null) {
            List<Versioningstatus> list = getDbaccess().getOneFromDBByInstanceId(obj.getInstanceId(), Versioningstatus.class);
            if (!list.isEmpty()) {
                edmobj = list.get(0);
            } else {
                list = getDbaccess().getOneFromDBByInstanceIdNoCache(obj.getInstanceId(), Versioningstatus.class);
                if (!list.isEmpty()) edmobj = list.get(0);
            }
        }

        // 2. Fallback UID (Solo NoCache per sicurezza)
        if (edmobj == null && obj.getUid() != null) {
            List<Versioningstatus> list = getDbaccess().getOneFromDBByUIDNoCache(obj.getUid(), Versioningstatus.class);
            if (!list.isEmpty()) edmobj = list.get(0);
        }

        if (edmobj != null) {
            StatusType currentDbStatus = StatusType.valueOf(edmobj.getStatus());
            StatusType targetStatus = overrideStatus != null ? overrideStatus : obj.getStatus();
            if (targetStatus == null) targetStatus = DRAFT;

            System.out.println("DEBUG checkVersion: Found ID=" + edmobj.getInstanceId() +
                    " Status=" + currentDbStatus + " -> Target=" + targetStatus);

            if (targetStatus == DRAFT && currentDbStatus != DRAFT) {
                createNewVersion(obj, edmobj, targetStatus);
            }
            else {
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

            createFirstVersion(obj, initialStatus);
            return obj;
        }
    }

    private static boolean isValidTransition(StatusType oldStatus, StatusType newStatus) {
        if (oldStatus == newStatus) return true;
        if (oldStatus == DRAFT && newStatus == SUBMITTED) return true;
        if (oldStatus == SUBMITTED && newStatus == PUBLISHED) return true;
        if (oldStatus == DRAFT && newStatus == PUBLISHED) return true;
        if (oldStatus == PUBLISHED && newStatus == ARCHIVED) return true;
        if (oldStatus == PUBLISHED && newStatus == DRAFT) return true;
        return false;
    }

    private static void archiveOldPublishedVersions(String uid, String currentVersionId) {
        if (uid == null) return;

        List<Versioningstatus> allVersionsRaw = getDbaccess().getOneFromDBByUIDNoCache(uid, Versioningstatus.class);

        for (Versioningstatus rawVs : allVersionsRaw) {
            if (rawVs.getVersionId().equals(currentVersionId)) continue;

            if (PUBLISHED.toString().equals(rawVs.getStatus())) {
                rawVs.setStatus(ARCHIVED.toString());
                rawVs.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
                rawVs.setChangeComment("Auto-archived");
                getDbaccess().updateObject(rawVs);
            }
        }
    }

    private static void createNewVersion(EPOSDataModelEntity obj, Versioningstatus previousVer, StatusType newStatus) {
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
    }

    private static void updateExistingVersion(EPOSDataModelEntity obj, Versioningstatus currentVer, StatusType newStatus) {
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
        Versioningstatus edmobj = new Versioningstatus();
        edmobj.setStatus(status.toString());

        edmobj.setInstanceId(Optional.ofNullable(obj.getInstanceId()).orElse(UUID.randomUUID().toString()));
        obj.setInstanceId(edmobj.getInstanceId());

        edmobj.setMetaId(Optional.ofNullable(obj.getMetaId()).orElse(UUID.randomUUID().toString()));
        obj.setMetaId(edmobj.getMetaId());

        edmobj.setVersionId(UUID.randomUUID().toString());
        obj.setVersionId(edmobj.getVersionId());

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse(UUID.randomUUID().toString()));
        edmobj.setInstanceChangeId(null);
        edmobj.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
        edmobj.setChangeComment(obj.getChangeComment());
        edmobj.setEditorId(obj.getEditorId());
        edmobj.setProvenance(obj.getFileProvenance());
        edmobj.setVersion(obj.getVersion());

        getDbaccess().createObject(edmobj);
    }

    public static EPOSDataModelEntity retrieveVersion(EPOSDataModelEntity obj) {
        List<Versioningstatus> returnList = null;
        if(obj.getInstanceId() == null)
            returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(),
                    obj.getMetaId(),
                    obj.getUid(),
                    obj.getVersionId(),
                    Versioningstatus.class
            );
        else {
            returnList = getDbaccess().getOneFromDBByInstanceId(obj.getInstanceId(), Versioningstatus.class);
            if (returnList.isEmpty()) {
                returnList = getDbaccess().getOneFromDBByInstanceIdNoCache(obj.getInstanceId(), Versioningstatus.class);
            }
        }

        if (returnList.isEmpty()) return null;

        Versioningstatus vs = returnList.get(0);
        obj.setChangeComment(vs.getChangeComment());
        obj.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
        obj.setEditorId(vs.getEditorId());
        obj.setFileProvenance(vs.getProvenance());
        obj.setVersion(vs.getVersion());
        obj.setInstanceChangedId(vs.getInstanceChangeId());

        try {
            obj.setStatus(StatusType.valueOf(vs.getStatus()));
        } catch (IllegalArgumentException e) {
            obj.setStatus(DRAFT);
        }
        return obj;
    }

    // *** FIX FINALE: Usa SEMPRE NoCache se abbiamo l'ID ***
    public static Versioningstatus retrieveVersioningStatus(EPOSDataModelEntity obj) {
        List<Versioningstatus> returnList = null;

        if(obj.getInstanceId() == null) {
            returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(), obj.getMetaId(), obj.getUid(), obj.getVersionId(), Versioningstatus.class
            );
        } else {
            // BYPASS CACHE OBBLIGATORIO:
            // Assicura che DataProductAPI riceva l'oggetto appena aggiornato a PUBLISHED
            // e non una copia vecchia (DRAFT) dalla cache.
            returnList = getDbaccess().getOneFromDBByInstanceIdNoCache(obj.getInstanceId(), Versioningstatus.class);
        }

        return returnList.isEmpty() ? null : returnList.get(0);
    }

    private static EposDataModelDAO<Versioningstatus> getDbaccess() {
        return EposDataModelDAO.getInstance();
    }
}