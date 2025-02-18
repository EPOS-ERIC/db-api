package commonapis;

import dao.EposDataModelDAO;
import model.*;
import model.Versioningstatus;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.Group;
import usermanagementapis.UserGroupManagementAPI;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static model.StatusType.DRAFT;

public class VersioningStatusAPI {
    
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
                        edmobj.setStatus(obj.getStatus().toString());
                        edmobj.setInstanceChangeId(edmobj.getInstanceId());
                        obj.setInstanceChangedId(edmobj.getInstanceId());

                        edmobj.setInstanceId(UUID.randomUUID().toString());
                        obj.setInstanceId(edmobj.getInstanceId());

                        edmobj.setVersionId(UUID.randomUUID().toString());
                        obj.setVersionId(edmobj.getVersionId());
                    } else {
                        edmobj.setStatus(obj.getStatus().toString());
                        obj.setInstanceChangedId(edmobj.getInstanceChangeId());
                        obj.setInstanceId(edmobj.getInstanceId());
                        obj.setVersionId(edmobj.getVersionId());
                    }
                } else {
                    edmobj.setStatus(obj.getStatus().toString());
                }
            }
            edmobj.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
            edmobj.setChangeComment(obj.getChangeComment());
            edmobj.setEditorId(obj.getEditorId());
            edmobj.setProvenance(obj.getFileProvenance());
            edmobj.setVersion(obj.getVersion());
            getDbaccess().updateObject(edmobj);

            if(obj.getGroups() != null) {
                for(String groupId : obj.getGroups()) {
                    UserGroupManagementAPI.addMetadataElementToGroup(edmobj.getMetaId(), groupId);
                }
            }

            return obj;
        } else {
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

            getDbaccess().updateObject(edmobj);
            if(obj.getGroups() != null) {
                for(String groupId : obj.getGroups()) {
                    UserGroupManagementAPI.addMetadataElementToGroup(edmobj.getMetaId(), groupId);
                }
            }

            return obj;
        }
    }

    public static EPOSDataModelEntity retrieveVersion(EPOSDataModelEntity obj) {
        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                Versioningstatus.class
        );

        if (returnList.isEmpty()) return null;

        Versioningstatus vs = returnList.get(0);

        obj.setChangeComment(vs.getChangeComment());
        obj.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
        obj.setEditorId(vs.getEditorId());
        obj.setFileProvenance(vs.getProvenance());
        obj.setVersion(vs.getVersion());
        obj.setStatus(StatusType.valueOf(vs.getStatus()));

        getDbaccess().updateObject(vs);
        return obj;
    }

    public static Versioningstatus retrieveVersioningStatus(EPOSDataModelEntity obj) {
        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                Versioningstatus.class
        );

        return returnList.isEmpty() ? null : returnList.get(0);
    }

    private static EposDataModelDAO<Versioningstatus> getDbaccess() {
        return new EposDataModelDAO<>();
    }
}
