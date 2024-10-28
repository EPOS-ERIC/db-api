package commonapis;

import dao.EposDataModelDAO;
import model.*;
import org.epos.eposdatamodel.EPOSDataModelEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.*;

import static model.StatusType.DRAFT;

public class VersioningStatusAPI {

    public static Boolean updateStatus(String instanceId, StatusType status){
        List<Versioningstatus> returnList = getDbaccess().getOneFromDBByInstanceId(instanceId,Versioningstatus.class);
        returnList.get(0).setStatus(status.toString());
        return getDbaccess().updateObject(returnList.get(0));
    }

    public static EPOSDataModelEntity checkVersion(EPOSDataModelEntity obj, StatusType overrideStatus) {

        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                obj.getInstanceId(),
                obj.getMetaId(),
                obj.getUid(),
                obj.getVersionId(),
                Versioningstatus.class
        );

        if(!returnList.isEmpty()){

            Versioningstatus edmobj = returnList.get(0);

            if(overrideStatus!=null) edmobj.setStatus(overrideStatus.toString());
            else {

                if (obj.getStatus() == null) obj.setStatus(DRAFT);

                switch (obj.getStatus()) {
                    case DRAFT:
                        if (!edmobj.getStatus().equals(DRAFT.name())) {
                            /**
                             * CREATING A NEW DRAFT FROM A NON-DRAFT SAVED ENTITY
                             *
                             * STATUS --> NEW STATUS
                             * InstanceChangeId --> OLD InstanceId
                             * InstanceId --> NEW InstanceId
                             * VersionId --> NEW VersionId
                             */
                            edmobj.setStatus(obj.getStatus().toString());

                            edmobj.setInstanceChangeId(edmobj.getInstanceId());
                            obj.setInstanceChangedId(edmobj.getInstanceId());

                            edmobj.setInstanceId(UUID.randomUUID().toString());
                            obj.setInstanceId(edmobj.getInstanceId());

                            edmobj.setVersionId(UUID.randomUUID().toString());
                            obj.setVersionId(edmobj.getVersionId());
                        } else {
                            /**
                             * UPDATING A DRAFT FROM A DRAFT SAVED ENTITY
                             *
                             * STATUS --> OLD STATUS
                             * InstanceChangeId --> OLD InstanceChangeId if not null
                             * InstanceId --> OLD InstanceId
                             * VersionId --> OLD VersionId
                             */
                            edmobj.setStatus(obj.getStatus().toString());
                            obj.setInstanceChangedId(edmobj.getInstanceChangeId());
                            obj.setInstanceId(edmobj.getInstanceId());
                            obj.setVersionId(edmobj.getVersionId());
                        }
                        break;
                    case ARCHIVED:
                        if (edmobj.getStatus().equals(DRAFT.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.PUBLISHED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.SUBMITTED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.DISCARDED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                    case DISCARDED:
                        if (edmobj.getStatus().equals(DRAFT.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.PUBLISHED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.SUBMITTED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.ARCHIVED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                    case PUBLISHED:
                        if (edmobj.getStatus().equals(StatusType.SUBMITTED.name())) {
                            edmobj.setStatus(obj.getStatus().toString());
                        }
                        break;
                    case SUBMITTED:
                        if (edmobj.getStatus().equals(DRAFT.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.PUBLISHED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.DISCARDED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        if (edmobj.getStatus().equals(StatusType.ARCHIVED.name()))
                            edmobj.setStatus(obj.getStatus().toString());
                        break;
                }
            }

            getDbaccess().updateObject(edmobj);

            return obj;
        } else {
            /**
             *
             * CREATING A NEW VERSIONING STATUS ENTITY
             *
             */
            Versioningstatus edmobj = new Versioningstatus();
            if(obj.getStatus()!=null) edmobj.setStatus(obj.getStatus().toString());
            else edmobj.setStatus(DRAFT.toString());
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

            return obj;
        }
    }

    private static EposDataModelDAO<Versioningstatus> getDbaccess() {
        return new EposDataModelDAO<>();
    }

    public static EPOSDataModelEntity updateVersion(EPOSDataModelEntity obj, Versioningstatus vs) {

        vs.setChangeComment(obj.getChangeComment());
        vs.setChangeTimestamp(OffsetDateTime.from(ZonedDateTime.now()));
        vs.setChangeComment(obj.getChangeComment());
        vs.setEditorId(obj.getEditorId());
        vs.setProvenance(obj.getFileProvenance());
        vs.setVersion(obj.getVersion());

        getDbaccess().updateObject(vs);

        return obj;
    }

    public static EPOSDataModelEntity retrieveVersion(EPOSDataModelEntity obj) {

        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                Optional.ofNullable(obj.getInstanceId()).orElse(null),
                Optional.ofNullable(obj.getMetaId()).orElse(null),
                Optional.ofNullable(obj.getUid()).orElse(null),
                Optional.ofNullable(obj.getVersionId()).orElse(null),
                Versioningstatus.class
        );

        if(returnList.isEmpty()) return null;

        Versioningstatus vs = returnList.get(0);

        obj.setChangeComment(vs.getChangeComment());
        obj.setChangeTimestamp(vs.getChangeTimestamp().toLocalDateTime());
        obj.setChangeComment(Optional.ofNullable(vs.getChangeComment()).orElse(null));
        obj.setEditorId(Optional.ofNullable(vs.getEditorId()).orElse(null));
        obj.setFileProvenance(Optional.ofNullable(vs.getProvenance()).orElse(null));
        obj.setVersion(Optional.ofNullable(vs.getVersion()).orElse(null));
        obj.setStatus(StatusType.valueOf(vs.getStatus()));

        getDbaccess().updateObject(vs);

        return obj;
    }

    public static Versioningstatus retrieveVersioningStatus(EPOSDataModelEntity obj) {

        List<Versioningstatus> returnList = getDbaccess().getOneFromDB(
                Optional.ofNullable(obj.getInstanceId()).orElse(null),
                Optional.ofNullable(obj.getMetaId()).orElse(null),
                Optional.ofNullable(obj.getUid()).orElse(null),
                Optional.ofNullable(obj.getVersionId()).orElse(null),
                Versioningstatus.class
        );

        if(returnList.isEmpty()) return null;

        for(Versioningstatus versioningstatus : returnList){
            if(versioningstatus.getInstanceId().equals(obj.getInstanceId())){
                return versioningstatus;
            }
        }

        return null;
    }

}
