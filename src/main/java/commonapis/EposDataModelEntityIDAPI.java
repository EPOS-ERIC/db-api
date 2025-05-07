package commonapis;

import dao.EposDataModelDAO;
import dao.EposDataModelDAOWithCache;
import model.EdmEntityId;

public class EposDataModelEntityIDAPI {

    private static EposDataModelDAOWithCache<EdmEntityId> getDbaccess() {
        return new EposDataModelDAOWithCache();
    }

    public static Boolean addEntityToEDMEntityID(String metaId, String entityName){
        EdmEntityId edmEntityId = new EdmEntityId();
        edmEntityId.setMetaId(metaId);
        edmEntityId.setTableName(entityName);

        return getDbaccess().updateObject(edmEntityId);
    }

    public static Boolean removeEntityToEDMEntityID(String metaId, String entityName){
        
        EdmEntityId edmEntityId = new EdmEntityId();
        edmEntityId.setMetaId(metaId);
        edmEntityId.setTableName(entityName);

        return getDbaccess().deleteObject(edmEntityId);
    }



}
