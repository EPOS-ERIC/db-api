package abstractapis;

import dao.EposDataModelDAO;
import dao.EposDataModelDAOWithCache;
import org.epos.eposdatamodel.LinkedEntity;

public abstract class AbstractRelationsAPI {

    protected static EposDataModelDAOWithCache dbaccess = new EposDataModelDAOWithCache();

    public static EposDataModelDAOWithCache getDbaccess(){
        return dbaccess;
    }

}
