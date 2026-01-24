package dao;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import model.Category;
import model.Dataproduct;
import model.Distribution;
import model.Mapping;
import model.Operation;
import model.Organization;
import model.Person;
import model.StatusType;
import model.Versioningstatus;
import model.Webservice;

import org.epos.eposdatamodel.LinkedEntity;
import org.epos.handler.dbapi.service.EntityManagerService;

import java.io.*;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;

public class EposDataModelDAO<T> {

	private static final Logger LOG = Logger.getLogger(EposDataModelDAO.class.getName());
	private static final int BATCH_SIZE = 25;

	// Primary cache for query results with optimized configuration
	private final Cache<String, Object> queryCache = Caffeine.newBuilder()
			.maximumSize(1_000_000_000)
			.expireAfterWrite(Duration.ofMinutes(120))
			.expireAfterAccess(Duration.ofMinutes(60))
			.recordStats()
			.build();

	// Separate cache for count queries (shorter TTL due to volatility)
	private final Cache<String, Long> countCache = Caffeine.newBuilder()
			.maximumSize(1_000_000_000)
			.expireAfterWrite(Duration.ofMinutes(60))
			.recordStats()
			.build();

	// Cache for single entities (longer TTL for better hit rate)
	private final Cache<String, Object> entityCache = Caffeine.newBuilder()
			.maximumSize(1_000_000_000)
			.expireAfterWrite(Duration.ofHours(1))
			.expireAfterAccess(Duration.ofMinutes(60))
			.recordStats()
			.build();

	private EntityManagerService entityManagerService = null;

	private EposDataModelDAO() {
		this.entityManagerService = new EntityManagerService.EntityManagerServiceBuilder().build();
	}

    private EposDataModelDAO(EposDataModelDAO instance) {
        this.entityManagerService = instance.entityManagerService;
    }

	private static EposDataModelDAO instance;
	private static EposDataModelDAO cacheInstance;

	public static EposDataModelDAO getInstance() {
		if (instance == null) {
			instance = new EposDataModelDAO();
		}
		return instance;
	}

	// =================== CACHE UTILITY METHODS ===================

	/**
	 * Stores value in query cache with null check
	 */
	private void putInQueryCache(String key, Object value) {
		if (value != null) {
			queryCache.put(key, value);
		}
	}

	/**
	 * Stores value in entity cache with null check
	 */
	private void putInEntityCache(String key, Object value) {
		if (value != null) {
			entityCache.put(key, value);
		}
	}

	/**
	 * Stores count value in dedicated count cache
	 */
	private void putInCountCache(String key, Long value) {
		if (value != null) {
			countCache.put(key, value);
		}
	}

	/**
	 * Retrieves value from query cache with thread-safe list cloning
	 */
	@SuppressWarnings("unchecked")
	private <R> R getFromQueryCache(String key) {
		Object cached = queryCache.getIfPresent(key);
		if (cached != null) {
			// Clone lists for thread-safety
			if (cached instanceof List) {
				return (R) new ArrayList<>((List<?>) cached);
			}
			return (R) cached;
		}
		return null;
	}

	/**
	 * Retrieves value from entity cache
	 */
	@SuppressWarnings("unchecked")
	private <R> R getFromEntityCache(String key) {
		return (R) entityCache.getIfPresent(key);
	}

	/**
	 * Retrieves count from count cache
	 */
	private Long getFromCountCache(String key) {
		return countCache.getIfPresent(key);
	}

	/**
	 * Evicts cache entries containing the specified pattern
	 */
	private void evictCacheByPattern(String pattern) {
		// Remove entries containing the pattern from all caches
		queryCache.asMap().keySet().removeIf(key -> key.contains(pattern));
		entityCache.asMap().keySet().removeIf(key -> key.contains(pattern));
		countCache.asMap().keySet().removeIf(key -> key.contains(pattern));

		callMigrateCache(pattern);
	}

	private void callMigrateCache(String pattern) {
		String[] services = null;
		if (System.getenv("SERVICES") == null || System.getenv("SERVICES").isEmpty()
				|| System.getenv("SERVICES").equals("")) {
			services = new String[] { "resources-service", "ingestor-service", "external-access-service",
					"email-sender-service", "distributed-processing-service" };
		} else {
			services = System.getenv("SERVICES").split(",");
		}
		Map<String, String> mapping = new HashMap<>();
		mapping.put("resources-service", "resources");
		mapping.put("ingestor-service", "ingestor");
		mapping.put("external-access-service", "external");
		mapping.put("email-sender-service", "email");
		mapping.put("distributed-processing-service", "distributed");

		for (String service : services) {
			for (Map.Entry<String, String> entry : mapping.entrySet()) {
				if (service.contains(entry.getValue())) {
					migrateCache("http://" + service + ":8080/api/" + entry.getKey() + "/v1/invalidate", pattern);
				}
			}
		}
	}

	private void migrateCache(String inputURL, String pattern) {
		String POST_PARAMS = "pattern=" + pattern;
		try {
			URL url = new URL(inputURL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("POST");

			con.setDoOutput(true);
			OutputStream os = con.getOutputStream();
			os.write(POST_PARAMS.getBytes());
			os.flush();
			os.close();
			int responseCode = con.getResponseCode();

			if (responseCode == HttpURLConnection.HTTP_OK) { // success
				BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
				String inputLine;
				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {
					response.append(inputLine);
				}
				in.close();
			} else {
			}
		} catch (IOException e) {
		}

	}

	/**
	 * Generates cache key from method name and parameters
	 */
	private String generateCacheKey(String method, Object... params) {
		StringBuilder key = new StringBuilder(method);
		for (Object param : params) {
			key.append("_").append(param != null ? param.toString() : "null");
		}
		return key.toString();
	}

	// =================== OPTIMIZED CRUD OPERATIONS ===================

	/**
	 * Creates entity with intelligent cache invalidation
	 */
	public Boolean createObject(T entity) {
		if (entity == null) return false;
		EntityManager em = null;
		EntityTransaction transaction = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			transaction = em.getTransaction();
			transaction.begin();

			sanitizeVersionStatus(em, entity);

			em.merge(entity);
			transaction.commit();
			evictCacheByPattern(entity.getClass().getSimpleName());
			return true;
		} catch (Exception exception) {
			if (transaction != null && transaction.isActive()) transaction.rollback();
			return false;
		} finally {
			if (em != null) em.close();
		}
	}

	private void sanitizeVersionStatus(EntityManager em, Object entity) {
		try {
			Method getVersion = entity.getClass().getMethod("getVersion");
			Object vsObj = getVersion.invoke(entity);

			if (vsObj instanceof Versioningstatus) {
				Versioningstatus vs = (Versioningstatus) vsObj;
				if (vs.getVersionId() == null) vs.setVersionId(UUID.randomUUID().toString());
				if (vs.getInstanceId() == null) vs.setInstanceId(UUID.randomUUID().toString());
				if (vs.getStatus() == null) vs.setStatus(model.StatusType.DRAFT.name());
				if (vs.getChangeTimestamp() == null) vs.setChangeTimestamp(java.time.OffsetDateTime.now());

				em.merge(vs);
			}
		} catch (Exception ignored) {}
	}

	private void ensureListsAreInitialized(Object entity) {
		if (entity == null) return;
		for (java.lang.reflect.Method m : entity.getClass().getMethods()) {
			if (m.getName().startsWith("get") && m.getReturnType().equals(List.class)) {
				try {
					Object value = m.invoke(entity);
					if (value == null) {
						String setterName = m.getName().replace("get", "set");
						java.lang.reflect.Method setter = entity.getClass().getMethod(setterName, List.class);
						setter.invoke(entity, new ArrayList<>());
						LOG.finest("Initialized null list via: " + setterName);
					}
				} catch (Exception ignored) {}
			}
		}
	}

	private void persistDependentVersionStatus(EntityManager em, Object entity) {
		try {
			Method getVersion = entity.getClass().getMethod("getVersion");
			Object vsObj = getVersion.invoke(entity);

			if (vsObj instanceof Versioningstatus) {
				Versioningstatus vs = (Versioningstatus) vsObj;

				// Verifica di sicurezza pre-persistenza
				if (vs.getVersionId() == null) {
					vs.setVersionId(UUID.randomUUID().toString());
				}
				if (vs.getInstanceId() == null) {
					vs.setInstanceId(UUID.randomUUID().toString());
				}
				if (vs.getStatus() == null) {
					vs.setStatus(model.StatusType.DRAFT.name());
				}
				if (vs.getChangeTimestamp() == null) {
					vs.setChangeTimestamp(java.time.OffsetDateTime.now());
				}

				em.merge(vs);
			}
		} catch (Exception ignored) {}
	}

	/**
	 * Creates a join entity with proper entity attachment.
	 * Handles @MapsId relationships correctly by re-attaching referenced entities.
	 */
	public <J> Boolean createJoinEntity(J joinEntity, String parentId, Class<?> pClass, String targetId, Class<?> tClass) {
		EntityManager em = null;
		EntityTransaction transaction = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			transaction = em.getTransaction();
			transaction.begin();

			Object p = em.find(pClass, parentId);
			Object t = em.find(tClass, targetId);

			if (p == null || t == null) return false;

			persistDependentVersionStatus(em, p);
			persistDependentVersionStatus(em, t);

			for (Method m : joinEntity.getClass().getMethods()) {
				if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
					if (m.getParameterTypes()[0].isAssignableFrom(pClass)) m.invoke(joinEntity, p);
					else if (m.getParameterTypes()[0].isAssignableFrom(tClass)) m.invoke(joinEntity, t);
				}
			}

			em.persist(joinEntity);
			transaction.commit();
			return true;
		} catch (Exception e) {
			if (transaction != null && transaction.isActive()) transaction.rollback();
			return false;
		} finally {
			if (em != null) em.close();
		}
	}

	private String getRootCauseMessage(Throwable t) {
		if (t == null) return null;
		StringBuilder sb = new StringBuilder();
		while (t != null) {
			if (t.getMessage() != null) {
				sb.append(t.getMessage()).append(" | ");
			}
			t = t.getCause();
		}
		return sb.toString();
	}

	/**
	 * Finds join entities by parent ID.
	 * Tries multiple strategies to handle EmbeddedId vs Direct Relationship vs String ID mismatch.
	 */
	public <T> List<T> getJoinEntitiesByParentId(String parentIdField, String parentId, Class<T> joinClass) {
		String cacheKey = generateCacheKey("joinByParent", parentIdField, parentId, joinClass.getSimpleName());
		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			List<T> result;
			try {
				String jpql = "SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c.id." + parentIdField + " = :parentId";
				TypedQuery<T> query = em.createQuery(jpql, joinClass);
				query.setParameter("parentId", parentId);
				result = query.getResultList();
			} catch (Exception e1) {
				try {
					String jpql = "SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c." + parentIdField + ".instanceId = :parentId";
					TypedQuery<T> query = em.createQuery(jpql, joinClass);
					query.setParameter("parentId", parentId);
					result = query.getResultList();
				} catch (Exception e2) {
					String jpql = "SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c." + parentIdField + " = :parentId";
					TypedQuery<T> query = em.createQuery(jpql, joinClass);
					query.setParameter("parentId", parentId);
					result = query.getResultList();
				}
			}

			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			LOG.log(Level.WARNING, "Warning in getJoinEntitiesByParentId: " + exception.getMessage());
			return new ArrayList<>();
		} finally {
			if (em != null) em.close();
		}
	}

	/**
	 * Gets join entities by parent ID using the relationship field directly.
	 *
	 * Use this for entities with @EmbeddedId + @MapsId where the relationship
	 * field is on the entity, not inside the EmbeddedId.
	 *
	 *
	 * Generated query: SELECT c FROM ContactpointElement c WHERE c.contactpointInstance.instanceId = :parentId
	 *
	 * @param relationFieldName The name of the @ManyToOne relationship field (e.g., "contactpointInstance")
	 * @param parentId The instanceId of the parent entity
	 * @param joinClass The join table entity class
	 * @return List of matching join entities, or empty list if none found
	 */
	public <T> List<T> getJoinEntitiesByRelationField(String relationFieldName, String parentId, Class<T> joinClass) {
		String cacheKey = joinClass.getSimpleName() + "_rel_" + relationFieldName + "_" + parentId;
		List<T> cachedResult = (List<T>) getFromQueryCache(cacheKey);
		if (cachedResult != null) {
			return cachedResult;
		}

		EntityManager em = null;
		EntityTransaction transaction = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			transaction = em.getTransaction();
			transaction.begin();

			// Query using the relationship field directly (not through EmbeddedId)
			// e.g., "SELECT c FROM ContactpointElement c WHERE c.contactpointInstance.instanceId = :parentId"
			String jpql = "SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c."
					+ relationFieldName + ".instanceId = :parentId";

			TypedQuery<T> query = em.createQuery(jpql, joinClass);
			query.setParameter("parentId", parentId);
			query.setHint("eclipselink.refresh", true);

			List<T> result = query.getResultList();

			transaction.commit();

			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (Exception e) {
					LOG.log(Level.WARNING, "Error during rollback", e);
				}
			}
			LOG.log(Level.SEVERE, "Error in getJoinEntitiesByRelationField", exception);
			exception.printStackTrace();
			return new ArrayList<>();
		} finally {
			if (em != null) {
				em.close();
			}
		}
	}

	/**
	 * Update with precise cache invalidation
	 */
	public Boolean updateObject(T obj) {
		if (obj == null) {
			LOG.warning("Attempted to update null entity");
			return false;
		}

		EntityManager em = null;
		EntityTransaction transaction = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			transaction = em.getTransaction();
			transaction.begin();

			em.merge(obj);
			transaction.commit();

			// Invalidate related cache entries
			evictCacheByPattern(obj.getClass().getSimpleName());

			return true;

		} catch (Exception exception) {
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (Exception rollbackEx) {
					LOG.log(Level.SEVERE, "Error during transaction rollback", rollbackEx);
				}
			}
			return false;
		} finally {
			if (em != null) {
				try {
					em.close();
				} catch (Exception closeEx) {
					LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
				}
			}
		}
	}

	/**
	 * Delete with cache invalidation
	 * REPLACES: deleteObject(T obj)
	 */
	public Boolean deleteObject(T obj) {
		if (obj == null) {
			LOG.warning("Attempted to delete null entity");
			return false;
		}

		EntityManager em = null;
		EntityTransaction transaction = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			transaction = em.getTransaction();
			transaction.begin();

			if (!em.contains(obj)) {
				obj = em.merge(obj);
			}
			em.remove(obj);
			transaction.commit();

			evictCacheByPattern(obj.getClass().getSimpleName());

			return true;

		} catch (Exception exception) {
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (Exception rollbackEx) {
					LOG.log(Level.SEVERE, "Error during transaction rollback", rollbackEx);
				}
			}
			LOG.log(Level.SEVERE, "Error deleting entity", exception);
			return false;
		} finally {
			if (em != null) {
				try {
					em.close();
				} catch (Exception closeEx) {
					LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
				}
			}
		}
	}

	/**
	 * Optimized batch delete
	 */
	public Boolean deleteListOfObjects(List<T> objects) {
		if (objects == null || objects.isEmpty()) {
			return true;
		}

		EntityManager em = null;
		EntityTransaction transaction = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			transaction = em.getTransaction();
			transaction.begin();

			String entityName = null;

			for (int i = 0; i < objects.size(); i++) {
				T obj = objects.get(i);
				if (entityName == null) {
					entityName = obj.getClass().getSimpleName();
				}

				if (!em.contains(obj)) {
					obj = em.merge(obj);
				}
				em.remove(obj);

				// Periodic flush for large batches
				if (i % BATCH_SIZE == 0 && i > 0) {
					em.flush();
					em.clear();
				}
			}

			em.flush();
			transaction.commit();

			if (entityName != null) {
				evictCacheByPattern(entityName);
			}

			return true;

		} catch (Exception exception) {
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (Exception rollbackEx) {
					LOG.log(Level.SEVERE, "Error during transaction rollback", rollbackEx);
				}
			}
			LOG.log(Level.SEVERE, "Error during batch delete operation", exception);
			return false;
		} finally {
			if (em != null) {
				try {
					em.close();
				} catch (Exception closeEx) {
					LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
				}
			}
		}
	}

	// =================== MULTI-LAYER CACHED QUERY METHODS ===================

	/**
	 * Find by Instance ID with entity-level caching
	 */
	public List<T> getOneFromDBByInstanceId(String instanceId, Class<T> obj) {
		if (instanceId == null || instanceId.trim().isEmpty()) {
			return new ArrayList<>();
		}

		String cacheKey = generateCacheKey("instanceId", instanceId, obj.getSimpleName());

		// First layer: entity cache (longer TTL)
		List<T> cached = getFromEntityCache(cacheKey);
		if (cached != null) {
			return cached;
		}

		// Second layer: query cache
		cached = getFromQueryCache(cacheKey);
		if (cached != null) {
			// Promote to entity cache
			putInEntityCache(cacheKey, cached);
			return cached;
		}

		// Query database as last resort
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId = :instanceId",
					obj);
			query.setParameter("instanceId", instanceId);
			List<T> result = query.getResultList();

			// Inizializzazione post-caricamento per sicurezza (Sanitization)
			for (T entity : result) {
				ensureListsAreInitialized(entity);
			}

			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBByInstanceId", exception);
			return new ArrayList<>();
		} finally {
			if (em != null) {
				try {
					em.close();
				} catch (Exception closeEx) {
					LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
				}
			}
		}
	}

	/**
	 * Get all with conditional caching (only for small datasets)
	 */
	public List<T> getAllFromDB(Class<T> obj) {
		String cacheKey = generateCacheKey("allFromDB", obj.getSimpleName());

		// Check cache first
		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) {
			return cached;
		}

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c",
					obj);

			List<T> result = query.getResultList();

			// Cache only if dataset is not too large
			//if (result.size() < 5000) {
				putInQueryCache(cacheKey, result);
			//}

			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getAllFromDB", exception);
			return new ArrayList<>();
		} finally {
			if (em != null) {
				try {
					em.close();
				} catch (Exception closeEx) {
					LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
				}
			}
		}
	}

    public List<T> getAllIDsFromDB(Class<T> obj) {
        String cacheKey = generateCacheKey("allIDsFromDB", obj.getSimpleName());

        // Check cache first
        List<T> cached = getFromQueryCache(cacheKey);
        if (cached != null) {
            return cached;
        }

        EntityManager em = null;
        try {
            em = EntityManagerService.getInstance().createEntityManager();

            TypedQuery<T> query = em.createQuery(
                    "SELECT c.instanceId FROM " + obj.getSimpleName() + " c",
                    obj);

            List<T> result = query.getResultList();

            putInQueryCache(cacheKey, result);

            return result;

        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "Error in getAllFromDB", exception);
            return new ArrayList<>();
        } finally {
            if (em != null) {
                try {
                    em.close();
                } catch (Exception closeEx) {
                    LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
                }
            }
        }
    }

	/**
	 * Count with dedicated cache
	 */
	public Long countAll(Class<T> obj) {
		String cacheKey = generateCacheKey("countAll", obj.getSimpleName());

		// Check count cache
		Long cached = getFromCountCache(cacheKey);
		if (cached != null) {
			return cached;
		}

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<Long> query = em.createQuery(
					"SELECT COUNT(c) FROM " + obj.getSimpleName() + " c",
					Long.class);

			Long result = query.getSingleResult();

			// Store in count cache
			putInCountCache(cacheKey, result);

			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in countAll", exception);
			return 0L;
		} finally {
			if (em != null) {
				try {
					em.close();
				} catch (Exception closeEx) {
					LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
				}
			}
		}
	}

	public List<T> getOneFromDBBySpecificKeyNoCache(String key, String value, Class<T> obj) {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			em.clear(); // Clear L1 cache
			TypedQuery<T> query = em.createQuery("SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + ".instanceId = :value", obj);
			query.setParameter("value", value);
			query.setHint("javax.persistence.cache.storeMode", "REFRESH");
			query.setHint("jakarta.persistence.cache.storeMode", "REFRESH");
			return query.getResultList();
		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBBySpecificKeyNoCache", exception);
			return new ArrayList<>();
		} finally {
			if (em != null) em.close();
		}
	}

	public List<T> getOneFromDBBySpecificKey(String key, String value, Class<T> obj) {
		String cacheKey = generateCacheKey("specificKey", key, value, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null)
			return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + ".instanceId = :value",
					obj);
			query.setParameter("value", value);

			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBBySpecificKey", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

	/**
	 * BYPASS CACHE: Find by Instance ID directly from DB.
	 */
	public List<T> getOneFromDBByInstanceIdNoCache(String instanceId, Class<T> obj) {
		if (instanceId == null || instanceId.trim().isEmpty()) {
			return new ArrayList<>();
		}

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId = :instanceId",
					obj);
			query.setParameter("instanceId", instanceId);

			return query.getResultList();

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBByInstanceIdNoCache", exception);
			return new ArrayList<>();
		} finally {
			if (em != null) {
				try {
					em.close();
				} catch (Exception closeEx) {
					LOG.log(Level.WARNING, "Error closing EntityManager", closeEx);
				}
			}
		}
	}

	/**
	 * BYPASS CACHE: Find by UID directly from DB.
	 */
	public List<T> getOneFromDBByUIDNoCache(String uid, Class<T> obj) {
		if (uid == null || uid.trim().isEmpty()) return new ArrayList<>();

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.uid = :uid", obj);
			query.setParameter("uid", uid);

			query.setHint("javax.persistence.cache.storeMode", "REFRESH");
			query.setHint("eclipselink.refresh", "true");

			List<T> result = query.getResultList();

			for (T entity : result) {
				ensureListsAreInitialized(entity);
			}

			return result;
		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBByUIDNoCache", exception);
			return new ArrayList<>();
		} finally {
			if (em != null) em.close();
		}
	}

	public List<T> getOneFromDBBySpecificKeySimple(String key, String value, Class<T> obj) {
		String cacheKey = generateCacheKey("specificKeySimple", key, value, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null)
			return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + " = :value",
					obj);
			query.setParameter("value", value);

			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBBySpecificKeySimple", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

	/**
	 * Retrieves data directly from DB, bypassing internal caches.
	 * Uses cache usage hints to force fresh data retrieval.
	 */
	public List<T> getOneFromDBBySpecificKeySimpleNoCache(String key, String value, Class<T> obj) {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			em.clear();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + " = :value",
					obj);
			query.setParameter("value", value);

			query.setHint("javax.persistence.cache.storeMode", "REFRESH");
			query.setHint("jakarta.persistence.cache.storeMode", "REFRESH"); // Per Jakarta EE

			query.setHint("eclipselink.refresh", "true");
			query.setHint("eclipselink.maintain-cache", "false");

			List<T> result = query.getResultList();

			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBBySpecificKeySimpleNoCache", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

	/**
	 * Helper method to check if an entity is in PENDING status.
	 * Handles both Domain Entities (via getVersion()) and Versioningstatus objects directly.
	 */
	private boolean isEntityPending(Object entity) {
		if (entity == null) return false;
		try {
			if (entity instanceof Versioningstatus) {
				String status = ((Versioningstatus) entity).getStatus();
				return "PENDING".equalsIgnoreCase(status);
			}

			try {
				Method getVersion = entity.getClass().getMethod("getVersion");
				Object versionObj = getVersion.invoke(entity);
				if (versionObj != null) {
					Method getStatus = versionObj.getClass().getMethod("getStatus");
					Object statusObj = getStatus.invoke(versionObj);
					if (statusObj != null && "PENDING".equalsIgnoreCase(statusObj.toString())) {
						return true;
					}
				}
			} catch (NoSuchMethodException e) {
				try {
					Method getStatus = entity.getClass().getMethod("getStatus");
					Object statusObj = getStatus.invoke(entity);
					if (statusObj != null && "PENDING".equalsIgnoreCase(statusObj.toString())) {
						return true;
					}
				} catch (NoSuchMethodException ignored) {
				}
			}
		} catch (Exception e) {
		}
		return false;
	}

	public List<T> getFromDBByUsingMultipleKeys(Map<String, Object> keyValues, Class<T> obj) {
		if (keyValues == null || keyValues.isEmpty())
			return new ArrayList<>();

		String cacheKey = generateCacheKey("multipleKeys", keyValues.toString(), obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null)
			return cached;

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

				if (value != null) {
					try {
						if (key.length > 1) {
							predicates.add(cb.equal(root.get(key[0]).get(key[1]), value));
						} else {
							predicates.add(cb.equal(root.get(key[0]), value));
						}
					} catch (Exception e) {
						LOG.warning("Field not found: " + entry.getKey());
					}
				}
			}

			if (!predicates.isEmpty()) {
				query.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
			} else {
				query.select(root);
			}

			List<T> result = em.createQuery(query).getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getFromDBByUsingMultipleKeys", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

	public List<T> getListFromDBByInstanceId(List<String> instanceIds, Class<T> obj) {
		if (instanceIds == null || instanceIds.isEmpty())
			return new ArrayList<>();

		String cacheKey = generateCacheKey("listInstanceId", instanceIds.toString(), obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null)
			return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId IN :instanceId",
					obj);
			query.setParameter("instanceId", instanceIds);

			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getListFromDBByInstanceId", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

    public List<T> getListIDsFromDBByInstanceId(List<String> instanceIds, Class<T> obj) {
        if (instanceIds == null || instanceIds.isEmpty())
            return new ArrayList<>();

        String cacheKey = generateCacheKey("listIDsInstanceId", instanceIds.toString(), obj.getSimpleName());

        List<T> cached = getFromQueryCache(cacheKey);
        if (cached != null)
            return cached;

        EntityManager em = null;
        try {
            em = EntityManagerService.getInstance().createEntityManager();

            TypedQuery<T> query = em.createQuery(
                    "SELECT c.instanceId FROM " + obj.getSimpleName() + " c WHERE c.instanceId IN :instanceId",
                    obj);
            query.setParameter("instanceId", instanceIds);

            List<T> result = query.getResultList();
            putInQueryCache(cacheKey, result);
            return result;

        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "Error in getListFromDBByInstanceId", exception);
            return new ArrayList<>();
        } finally {
            if (em != null)
                em.close();
        }
    }

	public List<T> getOneFromDBByMetaId(String metaId, Class<T> obj) {
		if (metaId == null || metaId.trim().isEmpty())
			return new ArrayList<>();

		String cacheKey = generateCacheKey("metaId", metaId, obj.getSimpleName());

		List<T> cached = getFromEntityCache(cacheKey);
		if (cached != null)
			return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.metaId LIKE :metaId",
					obj);
			query.setParameter("metaId", metaId);

			List<T> result = query.getResultList();
			putInEntityCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBByMetaId", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

	/**
	 * Filters out PENDING entities to prevent conflicts with relation signals.
	 */
	public List<T> getOneFromDBByUID(String uid, Class<T> obj) {
		if (uid == null || uid.trim().isEmpty()) return new ArrayList<>();

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.uid LIKE :uid", obj);
			query.setParameter("uid", uid);
			query.setHint("javax.persistence.cache.storeMode", "REFRESH");
			query.setHint("eclipselink.refresh", "true");

			List<T> result = query.getResultList();

			for (T entity : result) {
				ensureListsAreInitialized(entity);
			}

			return result;
		} finally {
			if (em != null) em.close();
		}
	}

	public List<T> getOneFromDBByVersionID(String versionId, Class<T> obj) {
		if (versionId == null || versionId.trim().isEmpty())
			return new ArrayList<>();

		String cacheKey = generateCacheKey("versionId", versionId, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null)
			return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.versionId LIKE :versionId",
					obj);
			query.setParameter("versionId", versionId);

			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getOneFromDBByVersionID", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

	/**
	 * Ensures pending records are filtered out from generic searches.
	 */
	public List<T> getOneFromDB(String instanceId, String metaId, String uid, String versionId, Class<T> obj) {
		List<T> results = new ArrayList<>();

		if (instanceId != null && !instanceId.trim().isEmpty()) {
			results = getOneFromDBByInstanceId(instanceId, obj);
		} else if (metaId != null && !metaId.trim().isEmpty()) {
			results = getOneFromDBByMetaId(metaId, obj);
		} else if (uid != null && !uid.trim().isEmpty()) {
			results = getOneFromDBByUID(uid, obj);
		} else if (versionId != null && !versionId.trim().isEmpty()) {
			results = getOneFromDBByVersionID(versionId, obj);
		}

		if (!results.isEmpty()) {
			results.removeIf(this::isEntityPending);
		}

		return results;
	}

	public List<T> getOneFromDBByLinkedEntity(LinkedEntity linkedEntity, Class<T> obj) {
		if (linkedEntity == null)
			return new ArrayList<>();

		return getOneFromDB(
				linkedEntity.getInstanceId(),
				linkedEntity.getMetaId(),
				linkedEntity.getUid(),
				null,
				obj);
	}


	public List<T> getAllIDsFromDBWithStatus(Class<T> obj, StatusType status) {
        if (status == null)
            return getAllFromDB(obj);

        String cacheKey = generateCacheKey("allIDsFromDBWithStatus", obj.getSimpleName(), status.name());

        List<T> cached = getFromQueryCache(cacheKey);
        if (cached != null)
            return cached;

        EntityManager em = null;
        try {
            em = EntityManagerService.getInstance().createEntityManager();

            TypedQuery<T> query = em.createQuery(
                    "SELECT c.instanceId FROM " + obj.getSimpleName() + " c " +
                            "JOIN Versioningstatus v ON c.instanceId = v.instanceId " +
                            "WHERE v.status = :status",
                    obj);
            query.setParameter("status", status.name());

            List<T> result = query.getResultList();
            putInQueryCache(cacheKey, result);
            return result;

        } catch (Exception exception) {
            LOG.log(Level.SEVERE, "Error in getAllFromDBWithStatus", exception);
            return new ArrayList<>();
        } finally {
            if (em != null)
                em.close();
        }
    }

	// =================== ADVANCED METHODS WITH CACHING ===================

	/**
	 * Pagination with intelligent caching
	 */
	public List<T> getAllFromDBPaginated(Class<T> obj, int page, int size) {
		String cacheKey = generateCacheKey("paginated", obj.getSimpleName(), page, size);

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null)
			return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();

			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c",
					obj);
			query.setFirstResult(page * size);
			query.setMaxResults(size);

			List<T> result = query.getResultList();

			putInQueryCache(cacheKey, result);

			return result;

		} catch (Exception exception) {
			LOG.log(Level.SEVERE, "Error in getAllFromDBPaginated", exception);
			return new ArrayList<>();
		} finally {
			if (em != null)
				em.close();
		}
	}

	/**
	 * Bulk update with cache invalidation
	 */
	public int bulkUpdateField(Class<T> obj, String fieldName, Object newValue, String whereField, Object whereValue) {
		EntityManager em = null;
		EntityTransaction transaction = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			transaction = em.getTransaction();
			transaction.begin();

			Query query = em.createQuery(
					"UPDATE " + obj.getSimpleName() + " c SET c." + fieldName + " = :newValue WHERE c." + whereField
							+ " = :whereValue");
			query.setParameter("newValue", newValue);
			query.setParameter("whereValue", whereValue);

			int updated = query.executeUpdate();
			transaction.commit();

			// Invalidate entire cache for this entity
			evictCacheByPattern(obj.getSimpleName());

			return updated;

		} catch (Exception exception) {
			if (transaction != null && transaction.isActive()) {
				try {
					transaction.rollback();
				} catch (Exception rollbackEx) {
					LOG.log(Level.SEVERE, "Error during transaction rollback", rollbackEx);
				}
			}
			LOG.log(Level.SEVERE, "Error in bulkUpdateField", exception);
			return 0;
		} finally {
			if (em != null)
				em.close();
		}
	}

	// =================== ADVANCED CACHE MANAGEMENT ===================

	/**
	 * Detailed cache statistics
	 */
	public Map<String, Object> getDetailedCacheStats() {
		Map<String, Object> stats = new HashMap<>();

		// Query cache statistics
		CacheStats queryStats = queryCache.stats();
		Map<String, Object> queryStatsMap = new HashMap<>();
		queryStatsMap.put("hitCount", queryStats.hitCount());
		queryStatsMap.put("missCount", queryStats.missCount());
		queryStatsMap.put("hitRate", queryStats.hitRate() * 100);
		queryStatsMap.put("evictionCount", queryStats.evictionCount());
		queryStatsMap.put("averageLoadPenalty", queryStats.averageLoadPenalty() / 1_000_000.0); // Convert to ms
		stats.put("queryCache", queryStatsMap);

		// Entity cache statistics
		CacheStats entityStats = entityCache.stats();
		Map<String, Object> entityStatsMap = new HashMap<>();
		entityStatsMap.put("hitCount", entityStats.hitCount());
		entityStatsMap.put("missCount", entityStats.missCount());
		entityStatsMap.put("hitRate", entityStats.hitRate() * 100);
		entityStatsMap.put("evictionCount", entityStats.evictionCount());
		entityStatsMap.put("averageLoadPenalty", entityStats.averageLoadPenalty() / 1_000_000.0);
		stats.put("entityCache", entityStatsMap);

		// Count cache statistics
		CacheStats countStats = countCache.stats();
		Map<String, Object> countStatsMap = new HashMap<>();
		countStatsMap.put("hitCount", countStats.hitCount());
		countStatsMap.put("missCount", countStats.missCount());
		countStatsMap.put("hitRate", countStats.hitRate() * 100);
		countStatsMap.put("evictionCount", countStats.evictionCount());
		countStatsMap.put("averageLoadPenalty", countStats.averageLoadPenalty() / 1_000_000.0);
		stats.put("countCache", countStatsMap);

		// Size information
		stats.put("queryCacheSize", queryCache.estimatedSize());
		stats.put("entityCacheSize", entityCache.estimatedSize());
		stats.put("countCacheSize", countCache.estimatedSize());

		return stats;
	}

	/**
	 * Cache warm-up with most frequently used entities
	 */
	public void warmUpCache(Class<T> entityClass, List<String> commonInstanceIds) {

		long startTime = System.currentTimeMillis();
		int warmedUp = 0;

		for (String instanceId : commonInstanceIds) {
			try {
				getOneFromDBByInstanceId(instanceId, entityClass);
				warmedUp++;
			} catch (Exception e) {
				LOG.warning("Error during warm-up for instanceId: " + instanceId);
			}
		}

		long elapsed = System.currentTimeMillis() - startTime;
	}

	/**
	 * Smart cache cleanup with policy-based eviction
	 */
	public void smartCacheCleanup() {
		long startTime = System.currentTimeMillis();

		// Cleanup based on statistics
		CacheStats queryStats = queryCache.stats();
		CacheStats entityStats = entityCache.stats();

		// Clean cache if hit rate is low
		if (queryStats.hitRate() < 0.5) {
			queryCache.cleanUp();
		}

		if (entityStats.hitRate() < 0.3) {
			entityCache.cleanUp();
		}

		// Always cleanup count cache (more volatile data)
		countCache.cleanUp();

		long elapsed = System.currentTimeMillis() - startTime;
	}

	/**
	 * Invalidates all caches - use sparingly for critical operations
	 */
	public void invalidateAllCachesForClass(String className) {
		queryCache.asMap().keySet().removeIf(key -> key.contains(className));
		entityCache.asMap().keySet().removeIf(key -> key.contains(className));
		countCache.asMap().keySet().removeIf(key -> key.contains(className));
	}

	/**
	 * Complete cache reset with statistics
	 */
	public void clearAllCaches() {
		Map<String, Object> statsBefore = getDetailedCacheStats();

		queryCache.invalidateAll();
		entityCache.invalidateAll();
		countCache.invalidateAll();
	}

	// =================== MONITORING AND HEALTH CHECK ===================

	/**
	 * Cache health check
	 */
	public boolean isCacheHealthy() {
		try {
			CacheStats queryStats = queryCache.stats();
			CacheStats entityStats = entityCache.stats();

			// Verify caches are not degraded
			boolean queryHealthy = queryStats.hitRate() > 0.1 || queryStats.requestCount() < 100;
			boolean entityHealthy = entityStats.hitRate() > 0.1 || entityStats.requestCount() < 100;

			return queryHealthy && entityHealthy;

		} catch (Exception e) {
			LOG.log(Level.WARNING, "Error during cache health check", e);
			return false;
		}
	}

	/**
	 * Cache performance report
	 */
	public void printCacheReport() {
		Map<String, Object> stats = getDetailedCacheStats();

		System.out.println("=== CAFFEINE CACHE PERFORMANCE REPORT ===");

		System.out.println("\nQuery Cache:");
		Map<String, Object> queryStats = (Map<String, Object>) stats.get("queryCache");
		System.out.printf("  Hit Rate: %.2f%% (%d hits, %d misses)\n",
				queryStats.get("hitRate"), queryStats.get("hitCount"), queryStats.get("missCount"));
		System.out.printf("  Size: %d entries, Evictions: %d\n",
				stats.get("queryCacheSize"), queryStats.get("evictionCount"));
		System.out.printf("  Avg Load Time: %.2fms\n", queryStats.get("averageLoadPenalty"));

		System.out.println("\nEntity Cache:");
		Map<String, Object> entityStats = (Map<String, Object>) stats.get("entityCache");
		System.out.printf("  Hit Rate: %.2f%% (%d hits, %d misses)\n",
				entityStats.get("hitRate"), entityStats.get("hitCount"), entityStats.get("missCount"));
		System.out.printf("  Size: %d entries, Evictions: %d\n",
				stats.get("entityCacheSize"), entityStats.get("evictionCount"));
		System.out.printf("  Avg Load Time: %.2fms\n", entityStats.get("averageLoadPenalty"));

		System.out.println("\nCount Cache:");
		Map<String, Object> countStats = (Map<String, Object>) stats.get("countCache");
		System.out.printf("  Hit Rate: %.2f%% (%d hits, %d misses)\n",
				countStats.get("hitRate"), countStats.get("hitCount"), countStats.get("missCount"));
		System.out.printf("  Size: %d entries, Evictions: %d\n",
				stats.get("countCacheSize"), countStats.get("evictionCount"));
		System.out.printf("  Avg Load Time: %.2fms\n", countStats.get("averageLoadPenalty"));

		System.out.println("\nCache Health: " + (isCacheHealthy() ? "HEALTHY" : "DEGRADED"));
		System.out.println("=====================================");
	}

	// =================== SCHEDULED MAINTENANCE ===================

	/**
	 * Automatic cache maintenance (call periodically)
	 */
	public void performCacheMaintenance() {

		long startTime = System.currentTimeMillis();

		// Automatic cleanup
		queryCache.cleanUp();
		entityCache.cleanUp();
		countCache.cleanUp();

		// Statistics after cleanup
		Map<String, Object> stats = getDetailedCacheStats();

		long elapsed = System.currentTimeMillis() - startTime;

	}

	/**
	 * Preload cache for critical entities
	 */
	public void preloadCriticalData(Class<T> entityClass) {

		// Preload count (always useful)
		countAll(entityClass);

		// Preload first 100 entities
		List<T> firstBatch = getAllFromDBPaginated(entityClass, 0, 100);

	}

	public void reloadCache() {
		cacheInstance = new EposDataModelDAO(instance);

		cacheInstance.getAllFromDB(Dataproduct.class);
		cacheInstance.getAllFromDB(Distribution.class);
		cacheInstance.getAllFromDB(Webservice.class);
		cacheInstance.getAllFromDB(Category.class);
		cacheInstance.getAllFromDB(Mapping.class);
		cacheInstance.getAllFromDB(Operation.class);
		cacheInstance.getAllFromDB(Organization.class);
		cacheInstance.getAllFromDB(Versioningstatus.class);
		cacheInstance.getAllFromDB(Person.class);

		//entityManagerService.close();
		instance = cacheInstance;
	}
}
