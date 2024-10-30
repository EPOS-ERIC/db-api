package relationsapi;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;

import java.util.List;
import java.util.Locale;

public class RelationChecker {

    protected static EposDataModelDAO dbaccess = new EposDataModelDAO();

    public static Object checkRelation(EPOSDataModelEntity mainEntity, EPOSDataModelEntity oldMainEntity, Class mainEntityClazz, LinkedEntity linkedEntity, StatusType overrideStatus, Class clazz) {

        if(mainEntityClazz == null) mainEntityClazz = mainEntity.getClass();

        LinkedEntity newLinkedEntityMainEntity = mainEntity==null? null : AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName().toUpperCase(Locale.ROOT)).name()).retrieveLinkedEntity(mainEntity.getInstanceId());
        LinkedEntity oldLinkedEntityMainEntity = oldMainEntity==null? null : AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName().toUpperCase(Locale.ROOT)).name()).retrieveLinkedEntity(oldMainEntity.getInstanceId());

        /** RETRIEVE THE EPOS DATA MODEL OBJECT OF INTEREST FROM LINKED ENTITY **/
        EPOSDataModelEntity relationEntity = (EPOSDataModelEntity) LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity);

        /** SETUP THE LINKED ENTITY TO RETURN BACK TO THE MAIN FUNCTION **/
        LinkedEntity obj = null;

        if(relationEntity!=null && newLinkedEntityMainEntity!=null && oldLinkedEntityMainEntity!=null) {
            /** CHANGE THE STATUS ACCORDING WITH THE MAIN ENTITY **/
            if (mainEntity.getStatus()!=null && relationEntity.getStatus()!=null && !mainEntity.getStatus().equals(relationEntity.getStatus())) {
                relationEntity.setStatus(mainEntity.getStatus());
               obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType().toUpperCase(Locale.ROOT)).name()).create(relationEntity, overrideStatus, oldLinkedEntityMainEntity, newLinkedEntityMainEntity);
            } else {
                obj = linkedEntity;
            }
        }
        else {
            List<Object> results = dbaccess.getOneFromDBByLinkedEntity(linkedEntity,clazz);
            if(!results.isEmpty()) obj = linkedEntity;
            else {
                obj = LinkedEntityAPI.createFromLinkedEntity(linkedEntity, mainEntity.getStatus());
            }
        }

        List<Object> results = dbaccess.getOneFromDBByLinkedEntity(obj,clazz);

        return results.isEmpty()? null : results.get(0);
    }

}
