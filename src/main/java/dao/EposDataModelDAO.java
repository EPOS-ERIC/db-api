package dao;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.handler.dbapi.service.DatabaseCacheService;
import org.epos.handler.dbapi.service.EntityManagerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class EposDataModelDAO<T> {

    protected static Logger LOG = Logger.getGlobal();
    private final DatabaseCacheService cacheService;

    public EposDataModelDAO() {
        if (EntityManagerService.getInstance() == null) {
            new EntityManagerService.EntityManagerServiceBuilder()
                    .setCacheEnabled("true")
                    .setCacheRefreshInterval("60000") // 1 minute default, adjust as needed
                    .build();
        }
        this.cacheService = EntityManagerService.getCacheService();
    }

    /**
     * Create an object in the database (not cached)
     */
    public Boolean createObject(T entity) {
        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        em.clear();
        try {
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();
            em.close();

            // Invalidate cache for this entity type
            invalidateCacheForEntityType(entity.getClass().getSimpleName());

            return true;
        } catch (Exception exception) {
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }

    /**
     * Get one object from database by specific key with caching
     */
    public List<T> getOneFromDBBySpecificKey(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBBySpecificKey." + key + "." + value;

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();

            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + ".instanceId=:value")
                    .setParameter("value", value)
                    .getResultList();

            em.getTransaction().commit();
            em.close();

            return resultList;
        });
    }

    /**
     * Get one object from database by specific key (simple) with caching
     */
    public List<T> getOneFromDBBySpecificKeySimple(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBBySpecificKeySimple." + key + "." + value;

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();

            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + "=:value")
                    .setParameter("value", value)
                    .getResultList();

            em.getTransaction().commit();
            em.close();

            return resultList;
        });
    }

    /**
     * Get from database using multiple keys with caching
     */
    public List<T> getFromDBByUsingMultipleKeys(Map<String, Object> keyValues, Class<T> obj) {
        // Build a cache key from the map entries
        StringBuilder cacheKeyBuilder = new StringBuilder(obj.getSimpleName() + ".getFromDBByUsingMultipleKeys");
        for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
            cacheKeyBuilder.append(".")
                    .append(entry.getKey())
                    .append("=")
                    .append(entry.getValue());
        }
        String cacheKey = cacheKeyBuilder.toString();

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.getTransaction().begin();

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<T> query = cb.createQuery(obj);
            Root<T> root = query.from(obj);

            List<Predicate> predicates = new ArrayList<>();

            for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
                String[] key = entry.getKey().split("\\.");
                Object value = entry.getValue();

                // Add conditions dynamically
                if (key.length > 1) predicates.add(cb.equal(root.get(key[0]).get(key[1]), value));
                else predicates.add(cb.equal(root.get(key[0]), value));
            }

            // Apply predicates to query
            query.select(root).where(cb.and(predicates.toArray(new Predicate[0])));

            TypedQuery<T> typedQuery = em.createQuery(query);
            List<T> resultList = typedQuery.getResultList();

            em.getTransaction().commit();
            em.close();

            return resultList;
        });
    }

    /**
     * Get from database by specific key (simple) with caching
     */
    public List<T> getFromDBBySpecificKeySimple(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getFromDBBySpecificKeySimple." + key + "." + value;

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();

            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + "=:value")
                    .setParameter("value", value)
                    .getResultList();

            em.getTransaction().commit();
            em.close();

            return resultList;
        });
    }

    /**
     * Get list from database by instance ID with caching
     */
    public List<T> getListFromDBByInstanceId(List<String> instanceIds, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getListFromDBByInstanceId." + String.join(",", instanceIds);

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();
            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId IN :instanceId")
                    .setParameter("instanceId", instanceIds)
                    .getResultList();
            em.getTransaction().commit();
            em.close();
            return resultList;
        });
    }

    /**
     * Get one from database by instance ID with caching
     */
    public List<T> getOneFromDBByInstanceId(String instanceId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByInstanceId." + instanceId;

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();
            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId=:instanceId")
                    .setParameter("instanceId", instanceId)
                    .getResultList();
            em.getTransaction().commit();
            em.close();
            return resultList;
        });
    }

    /**
     * Get one from database by meta ID with caching
     */
    public List<T> getOneFromDBByMetaId(String metaId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByMetaId." + metaId;

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();
            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.metaId LIKE :metaId")
                    .setParameter("metaId", metaId)
                    .getResultList();
            em.getTransaction().commit();
            em.close();
            return resultList;
        });
    }

    /**
     * Get one from database by UID with caching
     */
    public List<T> getOneFromDBByUID(String uid, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByUID." + uid;

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();
            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.uid LIKE :uid")
                    .setParameter("uid", uid)
                    .getResultList();
            em.getTransaction().commit();
            em.close();
            return resultList;
        });
    }

    /**
     * Get one from database by version ID with caching
     */
    public List<T> getOneFromDBByVersionID(String versionId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByVersionID." + versionId;

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();
            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.versionId LIKE :versionId")
                    .setParameter("versionId", versionId)
                    .getResultList();
            em.getTransaction().commit();
            em.close();
            return resultList;
        });
    }

    /**
     * Get one from database with caching
     */
    public List<T> getOneFromDB(String instanceId, String metaId, String uid, String versionId, Class<T> obj) {
        List<T> resultList = new ArrayList<>();
        if (instanceId != null) {
            resultList.addAll(getOneFromDBByInstanceId(instanceId, obj));
            return resultList;
        }
        if (metaId != null) {
            resultList.addAll(getOneFromDBByMetaId(metaId, obj));
            return resultList;
        }
        if (uid != null) {
            resultList.addAll(getOneFromDBByUID(uid, obj));
            return resultList;
        }
        if (versionId != null) {
            resultList.addAll(getOneFromDBByVersionID(versionId, obj));
            return resultList;
        }

        return resultList;
    }

    /**
     * Get one from database by linked entity with caching
     */
    public List<T> getOneFromDBByLinkedEntity(LinkedEntity linkedEntity, Class<T> obj) {
        List<T> resultList = new ArrayList<>();
        if (linkedEntity.getInstanceId() != null) {
            resultList.addAll(getOneFromDBByInstanceId(linkedEntity.getInstanceId(), obj));
            return resultList;
        }
        if (linkedEntity.getMetaId() != null) {
            resultList.addAll(getOneFromDBByMetaId(linkedEntity.getMetaId(), obj));
            return resultList;
        }
        if (linkedEntity.getUid() != null) {
            resultList.addAll(getOneFromDBByUID(linkedEntity.getUid(), obj));
            return resultList;
        }

        return resultList;
    }

    /**
     * Get versions from database by version ID with caching
     */
    public List<Versioningstatus> getVersionsFromDBByVersionId(String versionId) {
        String cacheKey = "Versioningstatus.getVersionsFromDBByVersionId." + versionId;

        return cacheService.getOrCompute(cacheKey, "Versioningstatus", () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.clear();
            em.getTransaction().begin();
            Versioningstatus objReturn = em.find(Versioningstatus.class, versionId);
            em.getTransaction().commit();
            em.close();
            return objReturn == null ? List.of() : List.of(objReturn);
        });
    }

    /**
     * Get all from database with caching
     */
    public List<T> getAllFromDB(Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getAllFromDB";

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.getTransaction().begin();
            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c")
                    .getResultList();
            em.getTransaction().commit();
            em.close();
            return resultList;
        });
    }

    /**
     * Get all from database with status with caching
     */
    public List<T> getAllFromDBWithStatus(Class<T> obj, StatusType status) {
        String cacheKey = obj.getSimpleName() + ".getAllFromDBWithStatus." + status.name();

        return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = EntityManagerService.getInstance().createEntityManager();
            em.setFlushMode(FlushModeType.AUTO);
            em.getTransaction().begin();
            List resultList = em.createQuery(
                            "SELECT c FROM " + obj.getSimpleName() + " c JOIN Versioningstatus v WHERE c.instanceId=v.instanceId AND v.status='" + status.name() + "'")
                    .getResultList();
            em.getTransaction().commit();
            em.close();
            return resultList;
        });
    }

    /**
     * Update an object in the database
     */
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

            // Invalidate cache for this entity type
            invalidateCacheForEntityType(obj.getClass().getSimpleName());

            return true;
        } catch (Exception exception) {
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }

    /**
     * Delete an object from the database
     */
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

            // Invalidate cache for this entity type
            invalidateCacheForEntityType(obj.getClass().getSimpleName());

            return true;
        } catch (Exception exception) {
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }

    /**
     * Delete a list of objects from the database
     */
    public Boolean deleteListOfObjects(List<T> objList) {
        if (objList == null || objList.isEmpty()) return true;

        EntityManager em = EntityManagerService.getInstance().createEntityManager();
        em.setFlushMode(FlushModeType.AUTO);
        try {
            for (T singleObj : objList) {
                em.getTransaction().begin();
                LOG.info(Boolean.toString(em.contains(singleObj)));
                if (!em.contains(singleObj)) {
                    T target = em.merge(singleObj);
                    LOG.info(target.toString());
                    em.remove(target);
                }
                em.getTransaction().commit();
            }
            em.close();

            // Invalidate cache for this entity type
            if (!objList.isEmpty()) {
                invalidateCacheForEntityType(objList.get(0).getClass().getSimpleName());
            }

            return true;
        } catch (Exception exception) {
            LOG.severe(exception.getLocalizedMessage());
            em.getTransaction().rollback();
            em.close();
            return false;
        }
    }

    /**
     * Invalidate cache for a specific entity type
     */
    private void invalidateCacheForEntityType(String entityType) {
        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityType);
            LOG.info("Cache invalidated for entity type: " + entityType);
        }
    }

    /**
     * Clear all cache entries for a specific entity type
     */
    public void clearCache(Class<T> entityClass) {
        invalidateCacheForEntityType(entityClass.getSimpleName());
    }

    /**
     * Clear all cache entries
     */
    public void clearAllCache() {
        if (cacheService != null) {
            cacheService.invalidateAll();
            LOG.info("All cache entries invalidated");
        }
    }

    /**
     * Get cache statistics
     */
    public Map<String, Long> getCacheStatistics() {
        if (cacheService != null) {
            return cacheService.getStatistics();
        }
        return null;
    }
}