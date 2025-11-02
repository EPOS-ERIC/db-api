package commonapis;

import dao.EposDataModelDAO;
import model.*;
import model.Versioningstatus;
import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

import static model.StatusType.DRAFT;

public class VersioningStatusAPI {

    static Logger LOG = Logger.getLogger(VersioningStatusAPI.class.getName());

    public static EPOSDataModelEntity checkVersion(EPOSDataModelEntity obj, StatusType overrideStatus) {

        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                Versioningstatus.class
        );

        if (!returnList.isEmpty()) {
            Versioningstatus edmobj = returnList.get(0);

            if (overrideStatus != null) {
                edmobj.setStatus(overrideStatus.toString());
                obj.setStatus(overrideStatus);
            } else {
                if (obj.getStatus() == null) obj.setStatus(DRAFT);
                if (obj.getStatus().equals(DRAFT)) {
                    if (!edmobj.getStatus().equals(DRAFT.name())) {
                        // Creating a new version - need to create new entity, not update existing
                        Versioningstatus newVersionEntity = new Versioningstatus();

                        // Set up the new version
                        newVersionEntity.setStatus(obj.getStatus().toString());
                        newVersionEntity.setInstanceChangeId(edmobj.getInstanceId()); // Reference to previous version
                        obj.setInstanceChangedId(edmobj.getInstanceId());

                        // Generate new IDs for the new version
                        String newInstanceId = UUID.randomUUID().toString();
                        String newVersionId = UUID.randomUUID().toString();

                        newVersionEntity.setInstanceId(newInstanceId);
                        obj.setInstanceId(newInstanceId);

                        newVersionEntity.setVersionId(newVersionId);
                        obj.setVersionId(newVersionId);

                        // Copy other fields
                        newVersionEntity.setMetaId(edmobj.getMetaId()); // Keep same metaId
                        newVersionEntity.setUid(edmobj.getUid()); // Keep same uid
                        newVersionEntity.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
                        newVersionEntity.setChangeComment(obj.getChangeComment());
                        newVersionEntity.setEditorId(obj.getEditorId());
                        newVersionEntity.setProvenance(obj.getFileProvenance());
                        newVersionEntity.setVersion(obj.getVersion());

                        // Create the new entity instead of updating existing
                        getDbaccess().createObject(newVersionEntity);

                    } else {
                        // Same status (DRAFT), just update existing entity without changing primary keys
                        edmobj.setStatus(obj.getStatus().toString());
                        obj.setInstanceChangedId(edmobj.getInstanceChangeId());
                        obj.setInstanceId(edmobj.getInstanceId());
                        obj.setVersionId(edmobj.getVersionId());

                        // Update other fields
                        edmobj.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
                        edmobj.setChangeComment(obj.getChangeComment());
                        edmobj.setEditorId(obj.getEditorId());
                        edmobj.setProvenance(obj.getFileProvenance());
                        edmobj.setVersion(obj.getVersion());

                        getDbaccess().updateObject(edmobj);
                    }
                } else {
                    // Different status, but not DRAFT - update existing without changing primary keys
                    edmobj.setStatus(obj.getStatus().toString());
                    edmobj.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
                    edmobj.setChangeComment(obj.getChangeComment()!=null?obj.getChangeComment():"unknown");
                    edmobj.setEditorId(obj.getEditorId()!=null?obj.getEditorId():"unknown");
                    edmobj.setProvenance(obj.getFileProvenance()!=null?obj.getFileProvenance():"unknown");
                    edmobj.setVersion(obj.getVersion()!=null?obj.getVersion():"unknown");

                    getDbaccess().updateObject(edmobj);
                }
            }
            return obj;
        } else {
            // No existing version found - create new one
            if (overrideStatus != null) {
                obj.setStatus(overrideStatus);
            }

            // Create new Versioningstatus entity
            Versioningstatus edmobj = new Versioningstatus();
            edmobj.setStatus(Optional.ofNullable(obj.getStatus()).map(Enum::toString).orElse(DRAFT.toString()));
            edmobj.setInstanceId(UUID.randomUUID().toString());
            obj.setInstanceId(edmobj.getInstanceId());
            edmobj.setMetaId(UUID.randomUUID().toString());
            obj.setMetaId(edmobj.getMetaId());
            edmobj.setVersionId(UUID.randomUUID().toString());
            obj.setVersionId(edmobj.getVersionId());
            edmobj.setUid(obj.getUid());
            edmobj.setInstanceChangeId(null);
            edmobj.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
            edmobj.setChangeComment(obj.getChangeComment());
            edmobj.setEditorId(obj.getEditorId());
            edmobj.setProvenance(obj.getFileProvenance());
            edmobj.setVersion(obj.getVersion());

            getDbaccess().createObject(edmobj); // Use createObject for new entities

            return obj;
        }
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
        else
            returnList = getDbaccess().getOneFromDBByInstanceId(obj.getInstanceId(), Versioningstatus.class);

        if (returnList.isEmpty()) return null;

        Versioningstatus vs = returnList.get(0);

        obj.setChangeComment(vs.getChangeComment());
        obj.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
        obj.setEditorId(vs.getEditorId());
        obj.setFileProvenance(vs.getProvenance());
        obj.setVersion(vs.getVersion());
        obj.setStatus(StatusType.valueOf(vs.getStatus()));

        //getDbaccess().updateObject(vs); // Commented out as this was just reading data
        return obj;
    }

    public static Versioningstatus retrieveVersioningStatus(EPOSDataModelEntity obj) {
        List<Versioningstatus> returnList = null;
        if(obj.getInstanceId() == null)
            returnList = getDbaccess().getOneFromDB(
                    obj.getInstanceId(),
                    obj.getMetaId(),
                    obj.getUid(),
                    obj.getVersionId(),
                    Versioningstatus.class
            );
        else
            returnList = getDbaccess().getOneFromDBByInstanceId(obj.getInstanceId(), Versioningstatus.class);

        return returnList.isEmpty() ? null : returnList.get(0);
    }

    private static EposDataModelDAO<Versioningstatus> getDbaccess() {
        return EposDataModelDAO.getInstance();
    }
}