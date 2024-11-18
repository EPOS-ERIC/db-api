package dao;

import jakarta.persistence.*;
import model.Versioningstatus;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.handler.dbapi.service.EntityManagerService;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class EposDataModelDAO<T> {

    protected static Logger LOG = Logger.getGlobal();

    public  EposDataModelDAO(){
        if(EntityManagerService.getInstance()==null) new EntityManagerService.EntityManagerServiceBuilder().build();
    }

    public Boolean createObject(T entity) {
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            em.close();
            return true;
        }catch (Exception exception){
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }

    public List<T> getOneFromDBBySpecificKey(String key, String value, Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        em.getTransaction().begin();

        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c WHERE c."+key+".instanceId=:value")
                .setParameter("value", value)
                .getResultList();

        em.getTransaction().commit();
        em.close();

        return resultList;
    }

    public List<T> getOneFromDBBySpecificKeySimple(String key, String value, Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        em.getTransaction().begin();

        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c WHERE c."+key+"=:value")
                .setParameter("value", value)
                .getResultList();

        em.getTransaction().commit();
        em.close();

        return resultList;
    }

    public List<T> getListFromDBByInstanceId(List<String> instanceIds, Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        em.getTransaction().begin();
        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c WHERE c.instanceId IN :instanceId")
                .setParameter("instanceId", instanceIds)
                .getResultList();
        em.getTransaction().commit();
        em.close();
        return resultList;
    }

    public List<T> getOneFromDBByInstanceId(String instanceId, Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        em.getTransaction().begin();
        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c WHERE c.instanceId=:instanceId")
                .setParameter("instanceId", instanceId)
                .getResultList();
        em.getTransaction().commit();
        em.close();
        return resultList;
    }

    public List<T> getOneFromDBByMetaId(String metaId, Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        em.getTransaction().begin();
        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c WHERE c.metaId LIKE :metaId")
                .setParameter("metaId", metaId)
                .getResultList();
        em.getTransaction().commit();
        em.close();
        return resultList;
    }

    public List<T> getOneFromDBByUID(String uid, Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        em.getTransaction().begin();
        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c WHERE c.uid LIKE :uid")
                .setParameter("uid", uid)
                .getResultList();
        em.getTransaction().commit();
        em.close();
        return resultList;
    }

    public List<T> getOneFromDBByVersionID(String versionId, Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        em.getTransaction().begin();
        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c WHERE c.versionId LIKE :versionId")
                .setParameter("versionId", versionId)
                .getResultList();
        em.getTransaction().commit();
        em.close();
        return resultList;
    }

    public List<T> getOneFromDB(String instanceId, String metaId, String uid, String versionId, Class<T> obj){
        List resultList = new ArrayList<T>();
        if(instanceId!=null) {
            resultList.addAll(getOneFromDBByInstanceId(instanceId,obj));
            return resultList;
        }
        if(metaId!=null) {
            resultList.addAll(getOneFromDBByMetaId(metaId,obj));
            return resultList;
        }
        if(uid!=null) {
            resultList.addAll(getOneFromDBByUID(uid,obj));
            return resultList;
        }
        if(versionId!=null) {
            resultList.addAll(getOneFromDBByVersionID(versionId,obj));
            return resultList;
        }

        return resultList;
    }

    public List<T> getOneFromDBByLinkedEntity(LinkedEntity linkedEntity, Class<T> obj){
        List resultList = new ArrayList<T>();
        if(linkedEntity.getInstanceId()!=null) {
            resultList.addAll(getOneFromDBByInstanceId(linkedEntity.getInstanceId(),obj));
            return resultList;
        }
        if(linkedEntity.getMetaId()!=null) {
            resultList.addAll(getOneFromDBByMetaId(linkedEntity.getMetaId(),obj));
            return resultList;
        }
        if(linkedEntity.getUid()!=null) {
            resultList.addAll(getOneFromDBByUID(linkedEntity.getUid(),obj));
            return resultList;
        }

        return resultList;
    }

    public List<Versioningstatus> getVersionsFromDBByVersionId(String versionId){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        Versioningstatus objReturn = em.find(Versioningstatus.class, versionId);
        em.getTransaction().commit();
        em.close();
        return objReturn==null? List.of() : List.of(objReturn);
    }

    public List<T> getAllFromDB(Class<T> obj){
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.getTransaction().begin();
        List resultList = em.createQuery(
                        "SELECT c FROM "+obj.getSimpleName()+" c")
                .getResultList();
        em.getTransaction().commit();
        em.close();
        return resultList;
    }

    public Boolean updateObject(T obj) {
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        if (obj == null) return false;
        try {
            em.getTransaction().begin();
            em.merge(obj);
            em.getTransaction().commit();
            em.close();
            return true;
        }catch(Exception exception){
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }

    public Boolean deleteObject(T obj) {
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        try {
            LOG.info(Boolean.toString(em.contains(obj)));
            if (!em.contains(obj)) {
                em.getTransaction().begin();
                T target = em.merge(obj);
                LOG.info(target.toString());
                em.remove(target);
                em.getTransaction().commit();
                em.close();
            }
            return true;
        }catch(Exception exception){
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }

    public Boolean deleteListOfObjects(List<T> obj) {
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        try {
            for(T singleObj : obj) {
                em.getTransaction().begin();
                LOG.info(Boolean.toString(em.contains(singleObj)));
                if (!em.contains(singleObj)) {
                    T target = em.merge(singleObj);
                    System.out.println(target.toString());
                    LOG.info(target.toString());
                    em.remove(target);
                }
                em.getTransaction().commit();
            }
            em.close();
            return true;
        }catch(Exception exception){
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }
}