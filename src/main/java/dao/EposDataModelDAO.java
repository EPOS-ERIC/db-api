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
import org.epos.handler.dbapi.service.RabbitMQCacheInvalidationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

public class EposDataModelDAO<T> {

    protected static Logger LOG = Logger.getGlobal();
    private final DatabaseCacheService cacheService;
    private final RabbitMQCacheInvalidationManager invalidationManager;
    private final boolean publishInvalidations;

    public EposDataModelDAO() {
        this(Boolean.parseBoolean(System.getenv().getOrDefault("ENABLE_CACHE_INVALIDATION_PUBLISHING", "true")));
    }

    public EposDataModelDAO(boolean publishInvalidations) {
        if (EntityManagerService.getInstance() == null) {
            new EntityManagerService.EntityManagerServiceBuilder()
                    .setCacheEnabled(System.getenv().getOrDefault("CACHE_ENABLED", "true"))
                    .setCacheRefreshInterval(System.getenv().getOrDefault("CACHE_REFRESH_INTERVAL", "60000"))
                    .build();
        }
        this.cacheService = EntityManagerService.getCacheService();
        this.invalidationManager = RabbitMQCacheInvalidationManager.getInstance();
        this.publishInvalidations = publishInvalidations;

        // Subscribe to invalidation events
        if (this.invalidationManager != null) {
            this.invalidationManager.subscribeToInvalidationEvents();
        }
    }

    /**
     * Create an object in the database (not cached)
     */
    public Boolean createObject(T entity) {
        EntityManager em = null;
        try {
            em = EntityManagerService.getInstance().createEntityManager();
            em.getTransaction().begin();
            em.persist(entity);
            em.getTransaction().commit();

            // Invalidate cache for this entity type
            invalidateCacheForEntityType(entity.getClass().getSimpleName());

            return true;
        } catch (Exception exception) {
            LOG.severe("Error creating object: " + exception.getLocalizedMessage());
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Get one object from database by specific key with caching
     */
    public List<T> getOneFromDBBySpecificKey(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBBySpecificKey." + key + "." + value;

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + ".instanceId=:value",
                        obj);
                query.setParameter("value", value);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get one object from database by specific key (simple) with caching
     */
    public List<T> getOneFromDBBySpecificKeySimple(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBBySpecificKeySimple." + key + "." + value;

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + "=:value",
                        obj);
                query.setParameter("value", value);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get from database using multiple keys with caching
     */
    public List<T> getFromDBByUsingMultipleKeys(Map<String, Object> keyValues, Class<T> obj) {
        // Build a cache key from the map entries
        StringBuilder cacheKeyBuilder = new StringBuilder(obj.getSimpleName() + ".getFromDBByUsingMultipleKeys");
        keyValues.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // Ensure consistent key ordering
                .forEach(entry -> cacheKeyBuilder.append(".")
                        .append(entry.getKey())
                        .append("=")
                        .append(entry.getValue()));
        String cacheKey = cacheKeyBuilder.toString();

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<T> query = cb.createQuery(obj);
                Root<T> root = query.from(obj);

                List<Predicate> predicates = new ArrayList<>();

                for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
                    String[] key = entry.getKey().split("\\.");
                    Object value = entry.getValue();

                    if (key.length > 1) {
                        predicates.add(cb.equal(root.get(key[0]).get(key[1]), value));
                    } else {
                        predicates.add(cb.equal(root.get(key[0]), value));
                    }
                }

                query.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
                TypedQuery<T> typedQuery = em.createQuery(query);
                return typedQuery.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get list from database by instance ID with caching
     */
    public List<T> getListFromDBByInstanceId(List<String> instanceIds, Class<T> obj) {
        if (instanceIds == null || instanceIds.isEmpty()) {
            return new ArrayList<>();
        }

        String cacheKey = obj.getSimpleName() + ".getListFromDBByInstanceId." + String.join(",", instanceIds);

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId IN :instanceId",
                        obj);
                query.setParameter("instanceId", instanceIds);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get one from database by instance ID with caching
     */
    public List<T> getOneFromDBByInstanceId(String instanceId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByInstanceId." + instanceId;

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId=:instanceId",
                        obj);
                query.setParameter("instanceId", instanceId);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get one from database by meta ID with caching
     */
    public List<T> getOneFromDBByMetaId(String metaId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByMetaId." + metaId;

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.metaId LIKE :metaId",
                        obj);
                query.setParameter("metaId", metaId);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get one from database by UID with caching
     */
    public List<T> getOneFromDBByUID(String uid, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByUID." + uid;

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.uid LIKE :uid",
                        obj);
                query.setParameter("uid", uid);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get one from database by version ID with caching
     */
    public List<T> getOneFromDBByVersionID(String versionId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByVersionID." + versionId;

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c WHERE c.versionId LIKE :versionId",
                        obj);
                query.setParameter("versionId", versionId);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
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

        return executeWithCache(cacheKey, "Versioningstatus", () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                Versioningstatus objReturn = em.find(Versioningstatus.class, versionId);
                return objReturn == null ? List.of() : List.of(objReturn);
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get all from database with caching
     */
    public List<T> getAllFromDB(Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getAllFromDB";

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c",
                        obj);
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Get all from database with status with caching
     */
    public List<T> getAllFromDBWithStatus(Class<T> obj, StatusType status) {
        String cacheKey = obj.getSimpleName() + ".getAllFromDBWithStatus." + status.name();

        return executeWithCache(cacheKey, obj.getSimpleName(), () -> {
            EntityManager em = null;
            try {
                em = EntityManagerService.getInstance().createEntityManager();
                TypedQuery<T> query = em.createQuery(
                        "SELECT c FROM " + obj.getSimpleName() + " c JOIN Versioningstatus v WHERE c.instanceId=v.instanceId AND v.status=:status",
                        obj);
                query.setParameter("status", status.name());
                return query.getResultList();
            } finally {
                if (em != null) {
                    em.close();
                }
            }
        });
    }

    /**
     * Update an object in the database
     */
    public Boolean updateObject(T obj) {
        if (obj == null) return false;

        EntityManager em = null;
        try {
            em = EntityManagerService.getInstance().createEntityManager();
            em.getTransaction().begin();
            em.merge(obj);
            em.getTransaction().commit();

            // Invalidate cache for this entity type
            invalidateCacheForEntityType(obj.getClass().getSimpleName());

            return true;
        } catch (Exception exception) {
            LOG.severe("Error updating object: " + exception.getLocalizedMessage());
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Delete an object from the database
     */
    public Boolean deleteObject(T obj) {
        if (obj == null) return false;

        EntityManager em = null;
        try {
            em = EntityManagerService.getInstance().createEntityManager();
            em.getTransaction().begin();

            if (!em.contains(obj)) {
                T target = em.merge(obj);
                em.remove(target);
            } else {
                em.remove(obj);
            }

            em.getTransaction().commit();

            // Invalidate cache for this entity type
            invalidateCacheForEntityType(obj.getClass().getSimpleName());

            return true;
        } catch (Exception exception) {
            LOG.severe("Error deleting object: " + exception.getLocalizedMessage());
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Delete a list of objects from the database
     */
    public Boolean deleteListOfObjects(List<T> objList) {
        if (objList == null || objList.isEmpty()) return true;

        EntityManager em = null;
        try {
            em = EntityManagerService.getInstance().createEntityManager();
            em.getTransaction().begin();

            for (T singleObj : objList) {
                if (!em.contains(singleObj)) {
                    T target = em.merge(singleObj);
                    em.remove(target);
                } else {
                    em.remove(singleObj);
                }
            }

            em.getTransaction().commit();

            // Invalidate cache for this entity type
            invalidateCacheForEntityType(objList.get(0).getClass().getSimpleName());

            return true;
        } catch (Exception exception) {
            LOG.severe("Error deleting objects: " + exception.getLocalizedMessage());
            if (em != null && em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            return false;
        } finally {
            if (em != null) {
                em.close();
            }
        }
    }

    /**
     * Execute query with cache support
     */
    @SuppressWarnings("unchecked")
    private <R> R executeWithCache(String cacheKey, String entityType, Supplier<R> supplier) {
        if (cacheService != null) {
            return (R) cacheService.getOrCompute(cacheKey, entityType, supplier);
        }
        return supplier.get();
    }

    /**
     * Invalidate cache for a specific entity type
     */
    private void invalidateCacheForEntityType(String entityType) {
        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityType);
            LOG.info("Cache invalidated for entity type: " + entityType);
        }

        // Publish invalidation event if enabled
        if (publishInvalidations && invalidationManager != null) {
            invalidationManager.publishInvalidationEvent(entityType)
                    .thenRun(() -> LOG.info("Published cache invalidation event for: " + entityType))
                    .exceptionally(ex -> {
                        LOG.severe("Failed to publish cache invalidation event: " + ex.getMessage());
                        return null;
                    });
        }
    }

    /**
     * Clear cache for a specific entity type
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