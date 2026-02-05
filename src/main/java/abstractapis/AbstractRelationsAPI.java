package abstractapis;

import dao.EposDataModelDAO;
import org.epos.eposdatamodel.LinkedEntity;

public abstract class AbstractRelationsAPI {

    protected static EposDataModelDAO dbaccess = EposDataModelDAO.getInstance();

    public static EposDataModelDAO getDbaccess(){
        return dbaccess;
    }

}