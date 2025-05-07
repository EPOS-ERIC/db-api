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
import java.util.logging.Logger;

/**
 * Extension of the EposDataModelDAO class with caching capabilities.
 * This class maintains the same interface as EposDataModelDAO but adds caching.
 */
public class EposDataModelDAOWithCache<T> extends EposDataModelDAO<T> {

    protected static Logger LOG = Logger.getGlobal();
    private final DatabaseCacheService cacheService;

    public EposDataModelDAOWithCache() {
        super(); // Initialize the parent class

        // Initialize cache if not already done
        if (EntityManagerService.getInstance() == null) {
            new EntityManagerService.EntityManagerServiceBuilder()
                    .setCacheEnabled("true")
                    .setCacheRefreshInterval("60000") // 1 minute default
                    .build();
        }

        this.cacheService = EntityManagerService.getCacheService();
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

    /**
     * Clear cache for a specific entity type
     */
    public void clearCache(Class<T> entityClass) {
        if (cacheService != null) {
            cacheService.invalidateByEntityType(entityClass.getSimpleName());
            LOG.info("Cache cleared for entity type: " + entityClass.getSimpleName());
        }
    }

    /**
     * Clear all cache
     */
    public void clearAllCache() {
        if (cacheService != null) {
            cacheService.invalidateAll();
            LOG.info("All cache entries cleared");
        }
    }

    /**
     * Override methods that modify data to invalidate cache
     */
    @Override
    public Boolean createObject(T entity) {
        Boolean result = super.createObject(entity);

        // Invalidate cache on successful operation
        if (result && entity != null && cacheService != null) {
            cacheService.invalidateByEntityType(entity.getClass().getSimpleName());
        }

        return result;
    }

    @Override
    public Boolean updateObject(T obj) {
        Boolean result = super.updateObject(obj);

        // Invalidate cache on successful operation
        if (result && obj != null && cacheService != null) {
            cacheService.invalidateByEntityType(obj.getClass().getSimpleName());
        }

        return result;
    }

    @Override
    public Boolean deleteObject(T obj) {
        Boolean result = super.deleteObject(obj);

        // Invalidate cache on successful operation
        if (result && obj != null && cacheService != null) {
            cacheService.invalidateByEntityType(obj.getClass().getSimpleName());
        }

        return result;
    }

    @Override
    public Boolean deleteListOfObjects(List<T> objList) {
        Boolean result = super.deleteListOfObjects(objList);

        // Invalidate cache on successful operation
        if (result && objList != null && !objList.isEmpty() && cacheService != null) {
            cacheService.invalidateByEntityType(objList.get(0).getClass().getSimpleName());
        }

        return result;
    }

    /**
     * Override read methods to use cache
     */
    @Override
    public List<T> getOneFromDBBySpecificKey(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBBySpecificKey." + key + "." + value;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getOneFromDBBySpecificKey(key, value, obj)
            );
        }

        return super.getOneFromDBBySpecificKey(key, value, obj);
    }

    @Override
    public List<T> getOneFromDBBySpecificKeySimple(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBBySpecificKeySimple." + key + "." + value;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getOneFromDBBySpecificKeySimple(key, value, obj)
            );
        }

        return super.getOneFromDBBySpecificKeySimple(key, value, obj);
    }

    @Override
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

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getFromDBByUsingMultipleKeys(keyValues, obj)
            );
        }

        return super.getFromDBByUsingMultipleKeys(keyValues, obj);
    }

    @Override
    public List<T> getFromDBBySpecificKeySimple(String key, String value, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getFromDBBySpecificKeySimple." + key + "." + value;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getFromDBBySpecificKeySimple(key, value, obj)
            );
        }

        return super.getFromDBBySpecificKeySimple(key, value, obj);
    }

    @Override
    public List<T> getListFromDBByInstanceId(List<String> instanceIds, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getListFromDBByInstanceId." + String.join(",", instanceIds);

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getListFromDBByInstanceId(instanceIds, obj)
            );
        }

        return super.getListFromDBByInstanceId(instanceIds, obj);
    }

    @Override
    public List<T> getOneFromDBByInstanceId(String instanceId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByInstanceId." + instanceId;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getOneFromDBByInstanceId(instanceId, obj)
            );
        }

        return super.getOneFromDBByInstanceId(instanceId, obj);
    }

    @Override
    public List<T> getOneFromDBByMetaId(String metaId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByMetaId." + metaId;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getOneFromDBByMetaId(metaId, obj)
            );
        }

        return super.getOneFromDBByMetaId(metaId, obj);
    }

    @Override
    public List<T> getOneFromDBByUID(String uid, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByUID." + uid;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getOneFromDBByUID(uid, obj)
            );
        }

        return super.getOneFromDBByUID(uid, obj);
    }

    @Override
    public List<T> getOneFromDBByVersionID(String versionId, Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getOneFromDBByVersionID." + versionId;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getOneFromDBByVersionID(versionId, obj)
            );
        }

        return super.getOneFromDBByVersionID(versionId, obj);
    }

    @Override
    public List<Versioningstatus> getVersionsFromDBByVersionId(String versionId) {
        String cacheKey = "Versioningstatus.getVersionsFromDBByVersionId." + versionId;

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, "Versioningstatus", () ->
                    super.getVersionsFromDBByVersionId(versionId)
            );
        }

        return super.getVersionsFromDBByVersionId(versionId);
    }

    @Override
    public List<T> getAllFromDB(Class<T> obj) {
        String cacheKey = obj.getSimpleName() + ".getAllFromDB";

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getAllFromDB(obj)
            );
        }

        return super.getAllFromDB(obj);
    }

    @Override
    public List<T> getAllFromDBWithStatus(Class<T> obj, StatusType status) {
        String cacheKey = obj.getSimpleName() + ".getAllFromDBWithStatus." + status.name();

        if (cacheService != null) {
            return cacheService.getOrCompute(cacheKey, obj.getSimpleName(), () ->
                    super.getAllFromDBWithStatus(obj, status)
            );
        }

        return super.getAllFromDBWithStatus(obj, status);
    }
}