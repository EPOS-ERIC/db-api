package org.epos.handler.dbapi.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.epos.handler.dbapi.service.DatabaseCacheService;
import org.epos.handler.dbapi.service.EntityManagerService;

import java.util.List;
import java.util.logging.Logger;

/**
 * Abstract repository class that provides caching functionality for database queries.
 * Extend this class for specific entity repositories.
 *
 * @param <T> The entity type
 */
public abstract class CachedRepository<T> {
    private static final Logger LOGGER = Logger.getLogger(CachedRepository.class.getName());

    protected final EntityManagerFactory emf;
    protected final Class<T> entityClass;
    protected final DatabaseCacheService cacheService;
    protected final String entityName;

    protected CachedRepository(Class<T> entityClass) {
        this.entityClass = entityClass;
        this.emf = EntityManagerService.getInstance();
        this.cacheService = EntityManagerService.getCacheService();
        this.entityName = entityClass.getSimpleName();
    }

    /**
     * Find an entity by ID with caching
     *
     * @param id The entity ID
     * @return The entity or null if not found
     */
    public T findById(Object id) {
        String cacheKey = entityName + ".findById." + id;

        return cacheService.getOrCompute(cacheKey, entityName, () -> {
            EntityManager em = emf.createEntityManager();
            try {
                return em.find(entityClass, id);
            } finally {
                em.close();
            }
        });
    }

    /**
     * Execute a named query with caching
     *
     * @param queryName The named query name
     * @param params The query parameters as key-value pairs
     * @return The query result list
     */
    @SuppressWarnings("unchecked")
    public List<T> executeNamedQuery(String queryName, Object... params) {
        StringBuilder cacheKeyBuilder = new StringBuilder(entityName + "." + queryName);

        // Add parameters to cache key
        for (Object param : params) {
            cacheKeyBuilder.append(".").append(param != null ? param.toString() : "null");
        }

        String cacheKey = cacheKeyBuilder.toString();

        return cacheService.getOrCompute(cacheKey, entityName, () -> {
            EntityManager em = emf.createEntityManager();
            try {
                Query query = em.createNamedQuery(queryName);

                // Set parameters
                for (int i = 0; i < params.length; i += 2) {
                    if (i + 1 < params.length) {
                        String paramName = params[i].toString();
                        Object paramValue = params[i + 1];
                        query.setParameter(paramName, paramValue);
                    }
                }

                return query.getResultList();
            } finally {
                em.close();
            }
        });
    }

    /**
     * Execute a JPQL query with caching
     *
     * @param jpql The JPQL query string
     * @param params The query parameters as key-value pairs
     * @return The query result list
     */
    public List<T> executeQuery(String jpql, Object... params) {
        StringBuilder cacheKeyBuilder = new StringBuilder(entityName + ".query." + jpql.hashCode());

        // Add parameters to cache key
        for (Object param : params) {
            cacheKeyBuilder.append(".").append(param != null ? param.toString() : "null");
        }

        String cacheKey = cacheKeyBuilder.toString();

        return cacheService.getOrCompute(cacheKey, entityName, () -> {
            EntityManager em = emf.createEntityManager();
            try {
                TypedQuery<T> query = em.createQuery(jpql, entityClass);

                // Set parameters
                for (int i = 0; i < params.length; i += 2) {
                    if (i + 1 < params.length) {
                        String paramName = params[i].toString();
                        Object paramValue = params[i + 1];
                        query.setParameter(paramName, paramValue);
                    }
                }

                return query.getResultList();
            } finally {
                em.close();
            }
        });
    }

    /**
     * Save (persist or merge) an entity
     *
     * @param entity The entity to save
     * @return The saved entity
     */
    public T save(T entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            T result = em.merge(entity);
            em.getTransaction().commit();
            return result;
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.severe("Error saving entity: " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Delete an entity
     *
     * @param entity The entity to delete
     */
    public void delete(T entity) {
        EntityManager em = emf.createEntityManager();
        try {
            em.getTransaction().begin();
            em.remove(em.contains(entity) ? entity : em.merge(entity));
            em.getTransaction().commit();
        } catch (Exception e) {
            if (em.getTransaction().isActive()) {
                em.getTransaction().rollback();
            }
            LOGGER.severe("Error deleting entity: " + e.getMessage());
            throw e;
        } finally {
            em.close();
        }
    }

    /**
     * Clear the cache for this entity type
     */
    public void clearCache() {
        cacheService.invalidateByEntityType(entityName);
    }
}