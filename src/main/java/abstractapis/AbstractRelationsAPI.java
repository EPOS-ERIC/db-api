package abstractapis;

import dao.EposDataModelDAO;

public abstract class AbstractRelationsAPI {

    protected static EposDataModelDAO dbaccess = new EposDataModelDAO();

    public static EposDataModelDAO getDbaccess(){
        return dbaccess;
    }

}
