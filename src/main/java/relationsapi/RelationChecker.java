package relationsapi;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.List;
import java.util.Locale;

public class RelationChecker {

    protected static EposDataModelDAO dbaccess = new EposDataModelDAO();

    public static Object checkRelation(LinkedEntity linkedEntity, StatusType overrideStatus, Class clazz) {
        List list = dbaccess.getOneFromDBByInstanceId(linkedEntity.getInstanceId(),clazz);

        EPOSDataModelEntity versioningstatus = (EPOSDataModelEntity) LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);
        LinkedEntity obj = null;
        if(list.isEmpty()) obj = LinkedEntityAPI.createFromLinkedEntity(linkedEntity, overrideStatus);
        else {
            if(!versioningstatus.getStatus().equals(overrideStatus)){
                obj = LinkedEntityAPI.createFromLinkedEntity(linkedEntity, overrideStatus);
            }
            else obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(clazz.getSimpleName().toUpperCase(Locale.ROOT)).name()).retrieveLinkedEntity(((EPOSDataModelEntity) list.get(0)).getInstanceId());
        }
        List<Object> results = dbaccess.getOneFromDBByLinkedEntity(obj,clazz);
        return results.isEmpty()? null : results.get(0);
    }

//    public static Object checkRelation(LinkedEntity linkedEntity, StatusType overrideStatus, Class clazz) {
//        System.out.println("LINKED ENTITY "+linkedEntity);
//        EPOSDataModelEntity entity = (EPOSDataModelEntity) LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);
//        System.out.println("ENTITY FOUND "+entity);
//        LinkedEntity obj = null;
//
//        if(entity!=null && !entity.getStatus().equals(overrideStatus)){
//            entity.setStatus(overrideStatus);
//            obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(entity.getClass().getSimpleName().toUpperCase(Locale.ROOT)).name()).create(entity,overrideStatus);
//        }
//        else {
//            if(dbaccess.getOneFromDBByLinkedEntity(linkedEntity,clazz).isEmpty()){
//                obj = LinkedEntityAPI.createFromLinkedEntity(linkedEntity,overrideStatus);
//            }
//            else obj = linkedEntity;
//        }
//
//        List<Object> results = dbaccess.getOneFromDBByLinkedEntity(obj,clazz);
//
//        System.out.println(results);
//
//        return results.isEmpty()? null : results.get(0);
//    }
}
