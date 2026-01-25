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
        // === FIX: Esclude i record che sono marker di relazioni pending ===
        // I marker di relazione pending usano la stessa tabella Versioningstatus
        // ma hanno metaId = nome classe (es. "model.DistributionDataproduct")
        // invece di un UUID. Questi devono essere saltati.
        if (edmobj == null && obj.getUid() != null) {
            List<Versioningstatus> list = getDbaccess().getOneFromDBByUIDNoCache(obj.getUid(), Versioningstatus.class);
            for (Versioningstatus vs : list) {
                // Salta i record che sono marker di relazioni pending
                if (isPendingRelationMarker(vs)) {
                    LOG.fine("Skipping pending relation marker for UID: " + obj.getUid() +
                            " (metaId=" + vs.getMetaId() + ")");
                    continue;
                }
                edmobj = vs;
                break;
            }
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
            // Default a DRAFT per il normale flow di versioning
            StatusType initialStatus = overrideStatus != null ? overrideStatus :
                    (obj.getStatus() != null ? obj.getStatus() : DRAFT);

            createFirstVersion(obj, initialStatus);
            return obj;
        }
    }

    /**
     * Verifica se un record Versioningstatus è un marker di relazione pending
     * piuttosto che un vero record di versioning di un'entità.
     *
     * I marker di relazione pending vengono creati da RelationSyncUtil.createPendingRelation()
     * e hanno le seguenti caratteristiche:
     * - status = PENDING
     * - metaId = nome classe Java completo (es. "model.DistributionDataproduct")
     *
     * I record di versioning normali invece hanno:
     * - metaId = UUID (es. "a1b2c3d4-e5f6-...")
     *
     * @param vs il record Versioningstatus da verificare
     * @return true se è un marker di relazione pending, false se è un record di versioning normale
     */
    private static boolean isPendingRelationMarker(Versioningstatus vs) {
        if (vs == null) return false;

        // Se non è PENDING, è sicuramente un record di versioning normale
        if (!StatusType.PENDING.name().equals(vs.getStatus())) {
            return false;
        }

        // Se il metaId contiene ".", è probabilmente un nome classe Java
        // (es. "model.DistributionDataproduct")
        // I metaId normali sono UUID che non contengono "."
        String metaId = vs.getMetaId();
        if (metaId != null && metaId.contains(".")) {
            return true;
        }

        // Ulteriore controllo: i marker hanno anche provenance e changeComment popolati
        // con tipi di entità (es. "DATAPRODUCT", "DISTRIBUTION")
        // mentre i record normali non li usano per questo scopo
        String provenance = vs.getProvenance();
        String changeComment = vs.getChangeComment();
        if (provenance != null && changeComment != null) {
            // Se sembrano tipi di entità (tutto maiuscolo), probabilmente è un marker
            if (provenance.equals(provenance.toUpperCase()) &&
                    changeComment.equals(changeComment.toUpperCase()) &&
                    provenance.length() > 3 && changeComment.length() > 3) {
                return true;
            }
        }

        return false;
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

            // === FIX: Non archiviare i marker di relazioni pending ===
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

        edmobj.setUid(Optional.ofNullable(obj.getUid()).orElse("entity/" + UUID.randomUUID().toString()));
        edmobj.setInstanceChangeId(null);
        edmobj.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
        edmobj.setChangeComment(obj.getChangeComment());
        edmobj.setEditorId(obj.getEditorId());
        edmobj.setProvenance(obj.getFileProvenance());
        edmobj.setVersion(obj.getVersion());

        getDbaccess().createObject(edmobj);
    }

    public static EPOSDataModelEntity retrieveVersion(EPOSDataModelEntity obj) {
        Versioningstatus vs = null;

        // 1. Prima prova per instanceId
        if (obj.getInstanceId() != null) {
            List<Versioningstatus> returnList = getDbaccess().getOneFromDBByInstanceId(obj.getInstanceId(), Versioningstatus.class);
            if (returnList.isEmpty()) {
                returnList = getDbaccess().getOneFromDBByInstanceIdNoCache(obj.getInstanceId(), Versioningstatus.class);
            }

            // Cerca un Versioningstatus valido (non PENDING marker)
            for (Versioningstatus candidate : returnList) {
                if (!isPendingRelationMarker(candidate)) {
                    vs = candidate;
                    break;
                }
            }
        }

        // 2. Se non trovato per instanceId, prova per UID
        if (vs == null && obj.getUid() != null) {
            List<Versioningstatus> uidList = getDbaccess().getOneFromDBByUIDNoCache(obj.getUid(), Versioningstatus.class);
            for (Versioningstatus candidate : uidList) {
                if (!isPendingRelationMarker(candidate)) {
                    vs = candidate;
                    break;
                }
            }
        }

        // 3. Se non trovato, prova con metodo generico
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

        // 4. FIX CRITICO: Se non trova Versioningstatus, restituisci l'oggetto con status default
        //    invece di null, per evitare NullPointerException nei chiamanti
        if (vs == null) {
            LOG.warning("[VersioningStatusAPI] No Versioningstatus found for entity - " +
                    "instanceId=" + obj.getInstanceId() + ", uid=" + obj.getUid() +
                    ". Returning entity with default status.");
            if (obj.getStatus() == null) {
                obj.setStatus(DRAFT);
            }
            return obj;
        }

        obj.setChangeComment(vs.getChangeComment());
        if (vs.getChangeTimestamp() != null) {
            obj.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
        }
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