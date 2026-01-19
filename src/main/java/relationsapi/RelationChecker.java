package relationsapi;

import abstractapis.AbstractAPI;
import commonapis.LinkedEntityAPI;
import commonapis.VersioningStatusAPI;
import dao.EposDataModelDAO;
import metadataapis.EntityNames;
import model.Operation;
import model.StatusType;
import org.eclipse.persistence.internal.jpa.rs.metadata.model.Link;
import org.epos.eposdatamodel.EPOSDataModelEntity;
import org.epos.eposdatamodel.LinkedEntity;
import utilities.OperationWebserviceInDistributionSingleton;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RelationChecker {

    public static Object checkRelation(EPOSDataModelEntity mainEntity, EPOSDataModelEntity oldMainEntity, Class mainEntityClazz, LinkedEntity linkedEntity, StatusType overrideStatus, Class clazz, Boolean enableStore) {
        if(mainEntityClazz == null) mainEntityClazz = mainEntity.getClass();

        LinkedEntity newLinkedEntityMainEntity = mainEntity==null? null : AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName().toUpperCase(Locale.ROOT)).name()).retrieveLinkedEntity(mainEntity.getInstanceId());
        LinkedEntity oldLinkedEntityMainEntity = oldMainEntity==null? null : AbstractAPI.retrieveAPI(EntityNames.valueOf(mainEntityClazz.getSimpleName().toUpperCase(Locale.ROOT)).name()).retrieveLinkedEntity(oldMainEntity.getInstanceId());


        /** RETRIEVE THE EPOS DATA MODEL OBJECT OF INTEREST FROM LINKED ENTITY **/
        EPOSDataModelEntity relationEntity = (EPOSDataModelEntity) LinkedEntityAPI.retrieveFromLinkedEntity(linkedEntity, mainEntity.getFileProvenance());

        /** SETUP THE LINKED ENTITY TO RETURN BACK TO THE MAIN FUNCTION **/
        LinkedEntity obj = null;

        if(relationEntity!=null && newLinkedEntityMainEntity!=null && oldLinkedEntityMainEntity!=null) {
            /** CHANGE THE STATUS ACCORDING WITH THE MAIN ENTITY **/
            if (mainEntity.getStatus()!=null && relationEntity.getStatus()!=null && !mainEntity.getStatus().equals(relationEntity.getStatus())) {
                relationEntity.setStatus(mainEntity.getStatus());
                if(enableStore) obj = OperationWebserviceInDistributionSingleton.getInstance().getTarget(linkedEntity);
                if(obj==null) {
                    obj = AbstractAPI.retrieveAPI(EntityNames.valueOf(linkedEntity.getEntityType().toUpperCase(Locale.ROOT)).name()).create(relationEntity, overrideStatus, oldLinkedEntityMainEntity, newLinkedEntityMainEntity);
                    if(enableStore) OperationWebserviceInDistributionSingleton.getInstance().createRelation(linkedEntity, obj);
                }
            } else {
                obj = linkedEntity;
            }
        } else {
            List<Object> results = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(linkedEntity,clazz);
            if(!results.isEmpty()) obj = linkedEntity;
            else {
                obj = LinkedEntityAPI.createFromLinkedEntity(linkedEntity, mainEntity.getStatus(), VersioningStatusAPI.retrieveVersioningStatus(mainEntity), Objects.nonNull(mainEntity.getFileProvenance())? mainEntity.getFileProvenance() : null );
            }
        }
        List<Object> results = EposDataModelDAO.getInstance().getOneFromDBByLinkedEntity(obj,clazz);
        return results.isEmpty()? null : results.get(0);
    }
}
