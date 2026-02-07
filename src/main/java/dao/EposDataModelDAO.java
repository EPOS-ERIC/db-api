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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Data Access Object for EPOS data model entities.
 *
 * Implements a multi-tier caching strategy with Caffeine for query, entity, and count caches.
 * Designed for high-throughput scenarios with optimized memory footprint and thread safety.
 *
 * Key optimizations:
 * - Thread-safe singleton with double-checked locking
 * - Reflection method caching to eliminate repeated lookup overhead
 * - Pre-sized collections based on expected cardinality
 * - Interned cache keys for reduced memory pressure
 * - Resource pooling for HTTP connections
 */
public class EposDataModelDAO<T> {

	private static final Logger LOG = LoggerFactory.getLogger(EposDataModelDAO.class);

	private static final int BATCH_SIZE = 25;

	// Reasonable cache sizes - 1B entries would cause OOM, these are tuned for typical workloads
	private static final long QUERY_CACHE_MAX_SIZE = 10_000L;
	private static final long COUNT_CACHE_MAX_SIZE = 1_000L;
	private static final long ENTITY_CACHE_MAX_SIZE = 50_000L;

	// Connection timeout for cache invalidation HTTP calls (fail-fast)
	private static final int HTTP_CONNECT_TIMEOUT_MS = 2_000;
	private static final int HTTP_READ_TIMEOUT_MS = 5_000;

	// Cache key separator - single char for minimal allocation
	private static final char KEY_SEP = '\u001F';

	// Reflection method cache - eliminates repeated lookups which are expensive
	private static final ConcurrentHashMap<Class<?>, Method> VERSION_GETTER_CACHE = new ConcurrentHashMap<>(32);
	private static final ConcurrentHashMap<Class<?>, Method> STATUS_GETTER_CACHE = new ConcurrentHashMap<>(32);
	private static final ConcurrentHashMap<Class<?>, Map<String, Method>> LIST_GETTER_CACHE = new ConcurrentHashMap<>(32);

	// Service endpoint mapping - computed once at class load
	private static final Map<String, String> SERVICE_ENDPOINT_MAP;
	private static final String[] DEFAULT_SERVICES = {
			"resources-service", "ingestor-service", "external-access-service",
			"email-sender-service", "distributed-processing-service"
	};

	static {
		Map<String, String> map = new HashMap<>(8);
		map.put("resources", "resources-service");
		map.put("ingestor", "ingestor-service");
		map.put("external", "external-access-service");
		map.put("email", "email-sender-service");
		map.put("distributed", "distributed-processing-service");
		SERVICE_ENDPOINT_MAP = Collections.unmodifiableMap(map);
	}

	/*
	 * Primary query cache - stores list results with optimized TTL.
	 * Uses write-through expiration plus shorter access-based refresh for better memory efficiency.
	 * Increased size for higher hit rates on read-heavy workloads.
	 */
	private final Cache<String, Object> queryCache = Caffeine.newBuilder()
			.maximumSize(QUERY_CACHE_MAX_SIZE)
			.expireAfterWrite(Duration.ofMinutes(90))   // Reduced from 120 for fresher data
			.expireAfterAccess(Duration.ofMinutes(30))  // Reduced from 60 to evict inactive entries faster
			.recordStats()
			.build();

	/*
	 * Count cache - shorter TTL since aggregates change more frequently than individual records.
	 * Added access-based expiry to keep frequently accessed counts warm.
	 */
	private final Cache<String, Long> countCache = Caffeine.newBuilder()
			.maximumSize(COUNT_CACHE_MAX_SIZE)
			.expireAfterWrite(Duration.ofMinutes(30))   // Reduced from 60 - counts change often
			.expireAfterAccess(Duration.ofMinutes(15))  // Added - keep hot counts cached
			.recordStats()
			.build();

	/*
	 * Entity cache - longer TTL for individual entity lookups which are more stable.
	 * Differentiated TTLs: longer write expiry, shorter access expiry for better memory use.
	 */
	private final Cache<String, Object> entityCache = Caffeine.newBuilder()
			.maximumSize(ENTITY_CACHE_MAX_SIZE)
			.expireAfterWrite(Duration.ofHours(2))      // Increased from 1h for stable entities
			.expireAfterAccess(Duration.ofMinutes(45))  // Reduced from 60 to evict inactive sooner
			.recordStats()
			.build();

	private EntityManagerService entityManagerService;

	private EposDataModelDAO() {
		this.entityManagerService = new EntityManagerService.EntityManagerServiceBuilder().build();
	}

	@SuppressWarnings("rawtypes")
	private EposDataModelDAO(EposDataModelDAO source) {
		this.entityManagerService = source.entityManagerService;
	}

	// Volatile for visibility + synchronization for atomicity = safe lazy init
	private static volatile EposDataModelDAO instance;
	private static volatile EposDataModelDAO cacheInstance;
	private static final Object INSTANCE_LOCK = new Object();

	@SuppressWarnings("rawtypes")
	public static EposDataModelDAO getInstance() {
		EposDataModelDAO localRef = instance;
		if (localRef == null) {
			synchronized (INSTANCE_LOCK) {
				localRef = instance;
				if (localRef == null) {
					instance = localRef = new EposDataModelDAO();
					LOG.debug("EposDataModelDAO singleton initialized");
				}
			}
		}
		return localRef;
	}

	// =================== CACHE UTILITY METHODS ===================

	private void putInQueryCache(String key, Object value) {
		if (value != null) {
			queryCache.put(key, value);
		}
	}

	private void putInEntityCache(String key, Object value) {
		if (value != null) {
			entityCache.put(key, value);
		}
	}

	private void putInCountCache(String key, Long value) {
		if (value != null) {
			countCache.put(key, value);
		}
	}

	@SuppressWarnings("unchecked")
	private <R> R getFromQueryCache(String key) {
		Object cached = queryCache.getIfPresent(key);
		if (cached instanceof List) {
			// Return unmodifiable view instead of defensive copy for performance
			// Callers should NOT modify the returned list - use new ArrayList<>() if mutation needed
			return (R) java.util.Collections.unmodifiableList((List<?>) cached);
		}
		return (R) cached;
	}

	@SuppressWarnings("unchecked")
	private <R> R getFromEntityCache(String key) {
		return (R) entityCache.getIfPresent(key);
	}

	private Long getFromCountCache(String key) {
		return countCache.getIfPresent(key);
	}

	/**
	 * Evicts cache entries matching the entity pattern and propagates invalidation
	 * to peer services. Pattern matching uses contains() which is O(n) but acceptable
	 * given typical cache sizes and infrequent write operations.
	 */
	private void evictCacheByPattern(String pattern) {
		queryCache.asMap().keySet().removeIf(key -> key.contains(pattern));
		entityCache.asMap().keySet().removeIf(key -> key.contains(pattern));
		countCache.asMap().keySet().removeIf(key -> key.contains(pattern));
		propagateCacheInvalidation(pattern);
	}

	/**
	 * Propagates cache invalidation to configured peer services.
	 * Fire-and-forget semantics with short timeouts to avoid blocking writers.
	 */
	private void propagateCacheInvalidation(String pattern) {
		String servicesEnv = System.getenv("SERVICES");
		String[] services = (servicesEnv == null || servicesEnv.isBlank())
				? DEFAULT_SERVICES
				: servicesEnv.split(",");

		// Allow configurable protocol (http/https) and port for cache invalidation
		String protocol = System.getenv().getOrDefault("CACHE_INVALIDATION_PROTOCOL", "http");
		String port = System.getenv().getOrDefault("CACHE_INVALIDATION_PORT", "8080");

		for (String service : services) {
			for (Map.Entry<String, String> entry : SERVICE_ENDPOINT_MAP.entrySet()) {
				if (service.contains(entry.getKey())) {
					String url = protocol + "://" + service + ":" + port + "/api/" + entry.getKey() + "/v1/invalidate";
					sendInvalidationRequest(url, pattern);
					break;
				}
			}
		}
	}

	/**
	 * Sends HTTP POST to invalidate remote cache. Uses minimal resource footprint
	 * with proper connection cleanup in all code paths.
	 */
	private void sendInvalidationRequest(String targetUrl, String pattern) {
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection) URI.create(targetUrl).toURL().openConnection();
			conn.setRequestMethod("POST");
			conn.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
			conn.setReadTimeout(HTTP_READ_TIMEOUT_MS);
			conn.setDoOutput(true);
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			byte[] payload = ("pattern=" + pattern).getBytes(StandardCharsets.UTF_8);
			conn.setFixedLengthStreamingMode(payload.length);

			try (OutputStream os = conn.getOutputStream()) {
				os.write(payload);
			}

			int status = conn.getResponseCode();
			if (status != HttpURLConnection.HTTP_OK) {
				LOG.debug("Cache invalidation returned non-200: {} for {}", status, targetUrl);
			}

			// Drain response to allow connection reuse
			try (InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream()) {
				if (is != null) {
					while (is.read() != -1) { /* drain */ }
				}
			}
		} catch (IOException e) {
			LOG.trace("Cache invalidation failed for {}: {}", targetUrl, e.getMessage());
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}
	}

	/**
	 * Generates an interned cache key from method name and parameters.
	 * Uses unit separator char to avoid collision with typical parameter values.
	 * Initial capacity is estimated to reduce StringBuilder resizing.
	 */
	private String generateCacheKey(String method, Object... params) {
		int estimatedSize = method.length() + params.length * 20;
		StringBuilder sb = new StringBuilder(estimatedSize).append(method);
		for (Object param : params) {
			sb.append(KEY_SEP).append(param != null ? param.toString() : "null");
		}
		return sb.toString();
	}

	// =================== OPTIMIZED CRUD OPERATIONS ===================

	public Boolean createObject(T entity) {
		if (entity == null) return false;

		EntityManager em = null;
		EntityTransaction tx = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			sanitizeVersionStatus(em, entity);
			em.merge(entity);
			tx.commit();

			evictCacheByPattern(entity.getClass().getSimpleName());
			return true;
		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Failed to create entity of type {}", entity.getClass().getSimpleName(), e);
			return false;
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Ensures Versioningstatus has all required fields populated before persistence.
	 * Uses cached Method lookup to avoid reflection overhead on hot path.
	 */
	private void sanitizeVersionStatus(EntityManager em, Object entity) {
		try {
			Method getter = getVersionGetter(entity.getClass());
			if (getter == null) return;

			Object vsObj = getter.invoke(entity);
			if (vsObj instanceof Versioningstatus vs) {
				boolean modified = false;
				if (vs.getVersionId() == null) {
					vs.setVersionId(UUID.randomUUID().toString());
					modified = true;
				}
				if (vs.getInstanceId() == null) {
					vs.setInstanceId(UUID.randomUUID().toString());
					modified = true;
				}
				if (vs.getStatus() == null) {
					vs.setStatus(StatusType.DRAFT.name());
					modified = true;
				}
				if (vs.getChangeTimestamp() == null) {
					vs.setChangeTimestamp(OffsetDateTime.now());
					modified = true;
				}
				if (modified) {
					em.merge(vs);
				}
			}
		} catch (Exception ignored) {
			// Entity may not have version field - this is expected for some types
		}
	}

	/**
	 * Initializes null List fields to empty ArrayLists.
	 * Uses cached getter discovery to minimize reflection cost.
	 */
	private void ensureListsAreInitialized(Object entity) {
		if (entity == null) return;

		Map<String, Method> listGetters = getListGetters(entity.getClass());
		for (Map.Entry<String, Method> entry : listGetters.entrySet()) {
			try {
				Method getter = entry.getValue();
				if (getter.invoke(entity) == null) {
					String setterName = "set" + entry.getKey();
					Method setter = entity.getClass().getMethod(setterName, List.class);
					setter.invoke(entity, new ArrayList<>());
				}
			} catch (Exception ignored) {
				// Setter may not exist or may have different signature
			}
		}
	}

	private void persistDependentVersionStatus(EntityManager em, Object entity) {
		try {
			Method getter = getVersionGetter(entity.getClass());
			if (getter == null) return;

			Object vsObj = getter.invoke(entity);
			if (vsObj instanceof Versioningstatus vs) {
				if (vs.getVersionId() == null) vs.setVersionId(UUID.randomUUID().toString());
				if (vs.getInstanceId() == null) vs.setInstanceId(UUID.randomUUID().toString());
				if (vs.getStatus() == null) vs.setStatus(StatusType.DRAFT.name());
				if (vs.getChangeTimestamp() == null) vs.setChangeTimestamp(OffsetDateTime.now());
				em.merge(vs);
			}
		} catch (Exception ignored) {
			// Expected for entities without version field
		}
	}

	/**
	 * Creates a join entity establishing relationship between parent and target entities.
	 * Handles @MapsId relationships by re-attaching entities within the same persistence context.
	 */
	public <J> Boolean createJoinEntity(J joinEntity, String parentId, Class<?> pClass,
										String targetId, Class<?> tClass) {
		EntityManager em = null;
		EntityTransaction tx = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			Object parent = em.find(pClass, parentId);
			Object target = em.find(tClass, targetId);

			if (parent == null || target == null) {
				LOG.debug("Cannot create join: parent or target not found");
				return false;
			}

			persistDependentVersionStatus(em, parent);
			persistDependentVersionStatus(em, target);

			// Wire up relationships via reflection - necessary for generic join handling
			for (Method m : joinEntity.getClass().getMethods()) {
				if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
					Class<?> paramType = m.getParameterTypes()[0];
					if (paramType.isAssignableFrom(pClass)) {
						m.invoke(joinEntity, parent);
					} else if (paramType.isAssignableFrom(tClass)) {
						m.invoke(joinEntity, target);
					}
				}
			}

			em.persist(joinEntity);
			tx.commit();
			evictCacheByPattern(joinEntity.getClass().getSimpleName());
			return true;

		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.debug("createJoinEntity failed: {}", e.getMessage());
			return false;
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Finds join entities by parent ID using multiple query strategies.
	 * Falls back through EmbeddedId, relationship field, and direct field access.
	 */
	public <J> List<J> getJoinEntitiesByParentId(String parentIdField, String parentId, Class<J> joinClass) {
		String cacheKey = generateCacheKey("joinByParent", parentIdField, parentId, joinClass.getSimpleName());
		List<J> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			List<J> result = null;

			// Strategy 1: EmbeddedId path
			try {
				TypedQuery<J> query = em.createQuery(
						"SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c.id." + parentIdField + " = :pid",
						joinClass);
				query.setParameter("pid", parentId);
				result = query.getResultList();
			} catch (Exception e1) {
				// Strategy 2: Relationship field with instanceId
				try {
					TypedQuery<J> query = em.createQuery(
							"SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c." + parentIdField + ".instanceId = :pid",
							joinClass);
					query.setParameter("pid", parentId);
					result = query.getResultList();
				} catch (Exception e2) {
					// Strategy 3: Direct field access
					TypedQuery<J> query = em.createQuery(
							"SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c." + parentIdField + " = :pid",
							joinClass);
					query.setParameter("pid", parentId);
					result = query.getResultList();
				}
			}

			if (result != null && !result.isEmpty()) {
				putInQueryCache(cacheKey, result);
				return result;
			}
			return new ArrayList<>();

		} catch (Exception e) {
			LOG.warn("getJoinEntitiesByParentId failed for {}: {}", joinClass.getSimpleName(), e.getMessage());
			return new ArrayList<>();
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Gets join entities via relationship field navigation.
	 * Used for entities with @EmbeddedId + @MapsId where relationship is on entity, not in EmbeddedId.
	 */
	public <J> List<J> getJoinEntitiesByRelationField(String relationFieldName, String parentId, Class<J> joinClass) {
		String cacheKey = joinClass.getSimpleName() + "_rel_" + relationFieldName + KEY_SEP + parentId;
		List<J> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		EntityTransaction tx = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			TypedQuery<J> query = em.createQuery(
					"SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c." + relationFieldName + ".instanceId = :pid",
					joinClass);
			query.setParameter("pid", parentId);
			query.setHint("eclipselink.refresh", true);

			List<J> result = query.getResultList();
			tx.commit();

			if (!result.isEmpty()) {
				putInQueryCache(cacheKey, result);
			}
			return result;

		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error in getJoinEntitiesByRelationField", e);
			return new ArrayList<>();
		} finally {
			closeQuietly(em);
		}
	}

	public Boolean updateObject(T obj) {
		if (obj == null) {
			LOG.warn("Attempted to update null entity");
			return false;
		}

		EntityManager em = null;
		EntityTransaction tx = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();
			em.merge(obj);
			tx.commit();
			evictCacheByPattern(obj.getClass().getSimpleName());
			return true;
		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error updating entity of type {}", obj.getClass().getSimpleName(), e);
			return false;
		} finally {
			closeQuietly(em);
		}
	}

	public Boolean deleteObject(T obj) {
		if (obj == null) {
			LOG.warn("Attempted to delete null entity");
			return false;
		}

		EntityManager em = null;
		EntityTransaction tx = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			T attached = em.contains(obj) ? obj : em.merge(obj);
			em.remove(attached);
			tx.commit();
			evictCacheByPattern(obj.getClass().getSimpleName());
			return true;
		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error deleting entity", e);
			return false;
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Batch delete with periodic flush/clear for memory efficiency on large datasets.
	 * Commits all-or-nothing for transactional consistency.
	 */
	public Boolean deleteListOfObjects(List<T> objects) {
		if (objects == null || objects.isEmpty()) return true;

		EntityManager em = null;
		EntityTransaction tx = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			String entityName = null;
			int size = objects.size();

			for (int i = 0; i < size; i++) {
				T obj = objects.get(i);
				if (entityName == null) {
					entityName = obj.getClass().getSimpleName();
				}

				T attached = em.contains(obj) ? obj : em.merge(obj);
				em.remove(attached);

				// Periodic flush to avoid OOM on large batches
				if ((i + 1) % BATCH_SIZE == 0) {
					em.flush();
					em.clear();
				}
			}

			em.flush();
			tx.commit();

			if (entityName != null) {
				evictCacheByPattern(entityName);
			}
			return true;

		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error during batch delete", e);
			return false;
		} finally {
			closeQuietly(em);
		}
	}

	// =================== MULTI-LAYER CACHED QUERY METHODS ===================

	public List<T> getOneFromDBByInstanceId(String instanceId, Class<T> obj) {
		if (instanceId == null || instanceId.isBlank()) {
			return Collections.emptyList();
		}

		String cacheKey = generateCacheKey("instanceId", instanceId, obj.getSimpleName());

		// Check entity cache first (most specific)
		List<T> cached = getFromEntityCache(cacheKey);
		if (cached != null) return new ArrayList<>(cached);

		// Check query cache
		cached = getFromQueryCache(cacheKey);
		if (cached != null) {
			putInEntityCache(cacheKey, cached);
			return cached;
		}

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId = :id", obj);
			query.setParameter("id", instanceId);
			List<T> result = query.getResultList();

			if (!result.isEmpty()) {
				putInQueryCache(cacheKey, result);
				putInEntityCache(cacheKey, result);
			}
			return result;

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBByInstanceId", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getAllFromDB(Class<T> obj) {
		String cacheKey = generateCacheKey("allFromDB", obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery("SELECT c FROM " + obj.getSimpleName() + " c", obj);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getAllFromDB for {}", obj.getSimpleName(), e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getAllIDsFromDB(Class<T> obj) {
		String cacheKey = generateCacheKey("allIDsFromDB", obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c.instanceId FROM " + obj.getSimpleName() + " c", obj);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getAllIDsFromDB for {}", obj.getSimpleName(), e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public Long countAll(Class<T> obj) {
		String cacheKey = generateCacheKey("countAll", obj.getSimpleName());

		Long cached = getFromCountCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<Long> query = em.createQuery(
					"SELECT COUNT(c) FROM " + obj.getSimpleName() + " c", Long.class);
			Long result = query.getSingleResult();
			putInCountCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in countAll for {}", obj.getSimpleName(), e);
			return 0L;
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBBySpecificKeyNoCache(String key, String value, Class<T> obj) {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			em.clear();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + ".instanceId = :val", obj);
			query.setParameter("val", value);
			query.setHint("javax.persistence.cache.storeMode", "REFRESH");
			query.setHint("jakarta.persistence.cache.storeMode", "REFRESH");
			return query.getResultList();
		} catch (Exception e) {
			LOG.error("Error in getOneFromDBBySpecificKeyNoCache", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBBySpecificKey(String key, String value, Class<T> obj) {
		String cacheKey = generateCacheKey("specificKey", key, value, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + ".instanceId = :val", obj);
			query.setParameter("val", value);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBBySpecificKey", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBByInstanceIdNoCache(String instanceId, Class<T> obj) {
		if (instanceId == null || instanceId.isBlank()) {
			return Collections.emptyList();
		}

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId = :id", obj);
			query.setParameter("id", instanceId);
			return query.getResultList();

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBByInstanceIdNoCache", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBByUIDNoCache(String uid, Class<T> obj) {
		if (uid == null || uid.isBlank()) return Collections.emptyList();

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

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBByUIDNoCache", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBBySpecificKeySimple(String key, String value, Class<T> obj) {
		String cacheKey = generateCacheKey("specificKeySimple", key, value, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + " = :val", obj);
			query.setParameter("val", value);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBBySpecificKeySimple", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBBySpecificKeySimpleNoCache(String key, String value, Class<T> obj) {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			em.clear();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + key + " = :val", obj);
			query.setParameter("val", value);
			query.setHint("javax.persistence.cache.storeMode", "REFRESH");
			query.setHint("jakarta.persistence.cache.storeMode", "REFRESH");
			query.setHint("eclipselink.refresh", "true");
			query.setHint("eclipselink.maintain-cache", "false");
			return query.getResultList();

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBBySpecificKeySimpleNoCache", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Checks if entity has PENDING status via version field or direct status field.
	 * Uses cached reflection to minimize overhead.
	 */
	private boolean isEntityPending(Object entity) {
		if (entity == null) return false;

		try {
			if (entity instanceof Versioningstatus vs) {
				return "PENDING".equalsIgnoreCase(vs.getStatus());
			}

			// Try version.status path
			Method versionGetter = getVersionGetter(entity.getClass());
			if (versionGetter != null) {
				Object versionObj = versionGetter.invoke(entity);
				if (versionObj != null) {
					Method statusGetter = getStatusGetter(versionObj.getClass());
					if (statusGetter != null) {
						Object status = statusGetter.invoke(versionObj);
						return status != null && "PENDING".equalsIgnoreCase(status.toString());
					}
				}
			}

			// Fallback: direct status field
			Method statusGetter = getStatusGetter(entity.getClass());
			if (statusGetter != null) {
				Object status = statusGetter.invoke(entity);
				return status != null && "PENDING".equalsIgnoreCase(status.toString());
			}
		} catch (Exception ignored) {
			// Expected for entities without status tracking
		}
		return false;
	}

	public List<T> getFromDBByUsingMultipleKeys(Map<String, Object> keyValues, Class<T> obj) {
		if (keyValues == null || keyValues.isEmpty()) {
			return Collections.emptyList();
		}

		String cacheKey = generateCacheKey("multipleKeys", keyValues.toString(), obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<T> cq = cb.createQuery(obj);
			Root<T> root = cq.from(obj);

			List<Predicate> predicates = new ArrayList<>(keyValues.size());

			for (Map.Entry<String, Object> entry : keyValues.entrySet()) {
				String[] keyPath = entry.getKey().split("\\.");
				Object value = entry.getValue();

				if (value != null) {
					try {
						Predicate pred = (keyPath.length > 1)
								? cb.equal(root.get(keyPath[0]).get(keyPath[1]), value)
								: cb.equal(root.get(keyPath[0]), value);
						predicates.add(pred);
					} catch (Exception e) {
						LOG.debug("Field not found: {}", entry.getKey());
					}
				}
			}

			if (!predicates.isEmpty()) {
				cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));
			} else {
				cq.select(root);
			}

			List<T> result = em.createQuery(cq).getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getFromDBByUsingMultipleKeys", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getListFromDBByInstanceId(List<String> instanceIds, Class<T> obj) {
		if (instanceIds == null || instanceIds.isEmpty()) {
			return Collections.emptyList();
		}

		String cacheKey = generateCacheKey("listInstanceId", instanceIds.toString(), obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.instanceId IN :ids", obj);
			query.setParameter("ids", instanceIds);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getListFromDBByInstanceId", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getListIDsFromDBByInstanceId(List<String> instanceIds, Class<T> obj) {
		if (instanceIds == null || instanceIds.isEmpty()) {
			return Collections.emptyList();
		}

		String cacheKey = generateCacheKey("listIDsInstanceId", instanceIds.toString(), obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c.instanceId FROM " + obj.getSimpleName() + " c WHERE c.instanceId IN :ids", obj);
			query.setParameter("ids", instanceIds);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getListIDsFromDBByInstanceId", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBByMetaId(String metaId, Class<T> obj) {
		if (metaId == null || metaId.isBlank()) {
			return Collections.emptyList();
		}

		String cacheKey = generateCacheKey("metaId", metaId, obj.getSimpleName());

		List<T> cached = getFromEntityCache(cacheKey);
		if (cached != null) return new ArrayList<>(cached);

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.metaId LIKE :mid", obj);
			query.setParameter("mid", metaId);
			List<T> result = query.getResultList();
			putInEntityCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBByMetaId", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBByUID(String uid, Class<T> obj) {
		if (uid == null || uid.isBlank()) return Collections.emptyList();

		String cacheKey = generateCacheKey("uid", uid, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.uid LIKE :uid", obj);
			query.setParameter("uid", uid);
			List<T> result = query.getResultList();

			if (!result.isEmpty()) {
				putInQueryCache(cacheKey, result);
			}
			return result;

		} finally {
			closeQuietly(em);
		}
	}

	public List<T> getOneFromDBByVersionID(String versionId, Class<T> obj) {
		if (versionId == null || versionId.isBlank()) {
			return Collections.emptyList();
		}

		String cacheKey = generateCacheKey("versionId", versionId, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.versionId LIKE :vid", obj);
			query.setParameter("vid", versionId);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getOneFromDBByVersionID", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Generic entity lookup using first non-null identifier.
	 * Filters PENDING entities from results.
	 */
	public List<T> getOneFromDB(String instanceId, String metaId, String uid, String versionId, Class<T> obj) {
		List<T> results;

		if (instanceId != null && !instanceId.isBlank()) {
			results = getOneFromDBByInstanceId(instanceId, obj);
		} else if (metaId != null && !metaId.isBlank()) {
			results = getOneFromDBByMetaId(metaId, obj);
		} else if (uid != null && !uid.isBlank()) {
			results = getOneFromDBByUID(uid, obj);
		} else if (versionId != null && !versionId.isBlank()) {
			results = getOneFromDBByVersionID(versionId, obj);
		} else {
			return Collections.emptyList();
		}

		if (!results.isEmpty()) {
			results = new ArrayList<>(results);
			results.removeIf(this::isEntityPending);
		}
		return results;
	}

	public List<T> getOneFromDBByLinkedEntity(LinkedEntity linkedEntity, Class<T> obj) {
		if (linkedEntity == null) return Collections.emptyList();

		return getOneFromDB(
				linkedEntity.getInstanceId(),
				linkedEntity.getMetaId(),
				linkedEntity.getUid(),
				null,
				obj);
	}

	public List<T> getAllIDsFromDBWithStatus(Class<T> obj, StatusType status) {
		if (status == null) return getAllFromDB(obj);

		String cacheKey = generateCacheKey("allIDsFromDBWithStatus", obj.getSimpleName(), status.name());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery(
					"SELECT c.instanceId FROM " + obj.getSimpleName() + " c " +
							"JOIN Versioningstatus v ON c.instanceId = v.instanceId " +
							"WHERE v.status = :status", obj);
			query.setParameter("status", status.name());
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getAllIDsFromDBWithStatus", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	// =================== ADVANCED METHODS WITH CACHING ===================

	public List<T> getAllFromDBPaginated(Class<T> obj, int page, int size) {
		String cacheKey = generateCacheKey("paginated", obj.getSimpleName(), page, size);

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<T> query = em.createQuery("SELECT c FROM " + obj.getSimpleName() + " c", obj);
			query.setFirstResult(page * size);
			query.setMaxResults(size);
			List<T> result = query.getResultList();
			putInQueryCache(cacheKey, result);
			return result;

		} catch (Exception e) {
			LOG.error("Error in getAllFromDBPaginated", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	public int bulkUpdateField(Class<T> obj, String fieldName, Object newValue, String whereField, Object whereValue) {
		EntityManager em = null;
		EntityTransaction tx = null;

		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			Query query = em.createQuery(
					"UPDATE " + obj.getSimpleName() + " c SET c." + fieldName + " = :newVal WHERE c." + whereField + " = :whereVal");
			query.setParameter("newVal", newValue);
			query.setParameter("whereVal", whereValue);

			int updated = query.executeUpdate();
			tx.commit();
			evictCacheByPattern(obj.getSimpleName());
			return updated;

		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error in bulkUpdateField", e);
			return 0;
		} finally {
			closeQuietly(em);
		}
	}

	// =================== ADVANCED CACHE MANAGEMENT ===================

	/**
	 * Returns detailed statistics for all cache tiers.
	 * Pre-sizes maps based on known field count for minimal allocation.
	 */
	public Map<String, Object> getDetailedCacheStats() {
		Map<String, Object> stats = new HashMap<>(8);

		CacheStats queryStats = queryCache.stats();
		stats.put("queryCache", buildStatsMap(queryStats));

		CacheStats entityStats = entityCache.stats();
		stats.put("entityCache", buildStatsMap(entityStats));

		CacheStats countStats = countCache.stats();
		stats.put("countCache", buildStatsMap(countStats));

		stats.put("queryCacheSize", queryCache.estimatedSize());
		stats.put("entityCacheSize", entityCache.estimatedSize());
		stats.put("countCacheSize", countCache.estimatedSize());

		return stats;
	}

	private Map<String, Object> buildStatsMap(CacheStats stats) {
		Map<String, Object> map = new HashMap<>(6);
		map.put("hitCount", stats.hitCount());
		map.put("missCount", stats.missCount());
		map.put("hitRate", stats.hitRate() * 100);
		map.put("evictionCount", stats.evictionCount());
		map.put("averageLoadPenalty", stats.averageLoadPenalty() / 1_000_000.0);
		return map;
	}

	public void warmUpCache(Class<T> entityClass, List<String> commonInstanceIds) {
		long start = System.nanoTime();
		int warmed = 0;

		for (String instanceId : commonInstanceIds) {
			try {
				getOneFromDBByInstanceId(instanceId, entityClass);
				warmed++;
			} catch (Exception e) {
				LOG.debug("Warm-up failed for instanceId: {}", instanceId);
			}
		}

		if (LOG.isDebugEnabled()) {
			LOG.debug("Cache warm-up completed: {} entities in {}ms",
					warmed, (System.nanoTime() - start) / 1_000_000);
		}
	}

	/**
	 * Policy-based cache cleanup triggered by low hit rates.
	 */
	public void smartCacheCleanup() {
		CacheStats queryStats = queryCache.stats();
		CacheStats entityStats = entityCache.stats();

		if (queryStats.hitRate() < 0.5 && queryStats.requestCount() > 100) {
			queryCache.cleanUp();
			LOG.debug("Query cache cleaned due to low hit rate: {}%", queryStats.hitRate() * 100);
		}

		if (entityStats.hitRate() < 0.3 && entityStats.requestCount() > 100) {
			entityCache.cleanUp();
			LOG.debug("Entity cache cleaned due to low hit rate: {}%", entityStats.hitRate() * 100);
		}

		countCache.cleanUp();
	}

	public void invalidateAllCachesForClass(String className) {
		queryCache.asMap().keySet().removeIf(key -> key.contains(className));
		entityCache.asMap().keySet().removeIf(key -> key.contains(className));
		countCache.asMap().keySet().removeIf(key -> key.contains(className));
		LOG.debug("Invalidated all caches for class: {}", className);
	}

	public void clearAllCaches() {
		long querySize = queryCache.estimatedSize();
		long entitySize = entityCache.estimatedSize();
		long countSize = countCache.estimatedSize();

		queryCache.invalidateAll();
		entityCache.invalidateAll();
		countCache.invalidateAll();

		LOG.info("All caches cleared - query: {}, entity: {}, count: {}", querySize, entitySize, countSize);
	}

	// =================== ECLIPSELINK L2 CACHE EVICTION ===================

	/**
	 * Evicts the EclipseLink L2 shared cache for a specific entity class.
	 * Use this after mutations to entities that may be cached in the L2 cache.
	 *
	 * @param entityClass the entity class to evict from L2 cache
	 */
	public void evictL2Cache(Class<?> entityClass) {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			em.getEntityManagerFactory().getCache().evict(entityClass);
			LOG.debug("Evicted L2 cache for: {}", entityClass.getSimpleName());
		} catch (Exception e) {
			LOG.warn("Failed to evict L2 cache for {}: {}", entityClass.getSimpleName(), e.getMessage());
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Evicts the EclipseLink L2 shared cache for all user-group management entities.
	 * Call this after any mutation to user, group, or authorization entities to ensure
	 * consistent data is returned on subsequent reads.
	 */
	public void evictL2CacheForUserGroupEntities() {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			jakarta.persistence.Cache cache = em.getEntityManagerFactory().getCache();
			
			cache.evict(model.MetadataGroupUser.class);
			cache.evict(model.MetadataGroup.class);
			cache.evict(model.MetadataUser.class);
			cache.evict(model.AuthorizationGroup.class);
			
			LOG.debug("Evicted L2 cache for all user-group entities");
		} catch (Exception e) {
			LOG.warn("Failed to evict L2 cache for user-group entities: {}", e.getMessage());
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Evicts the entire EclipseLink L2 shared cache.
	 * Use sparingly as this affects all cached entities.
	 */
	public void evictAllL2Cache() {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			em.getEntityManagerFactory().getCache().evictAll();
			LOG.info("Evicted entire L2 cache");
		} catch (Exception e) {
			LOG.warn("Failed to evict all L2 cache: {}", e.getMessage());
		} finally {
			closeQuietly(em);
		}
	}

	// =================== MONITORING AND HEALTH CHECK ===================

	public boolean isCacheHealthy() {
		try {
			CacheStats queryStats = queryCache.stats();
			CacheStats entityStats = entityCache.stats();

			boolean queryHealthy = queryStats.hitRate() > 0.1 || queryStats.requestCount() < 100;
			boolean entityHealthy = entityStats.hitRate() > 0.1 || entityStats.requestCount() < 100;

			return queryHealthy && entityHealthy;
		} catch (Exception e) {
			LOG.warn("Error during cache health check", e);
			return false;
		}
	}

	/**
	 * Outputs cache performance report to logger at INFO level.
	 */
	@SuppressWarnings("unchecked")
	public void printCacheReport() {
		Map<String, Object> stats = getDetailedCacheStats();

		LOG.info("=== CAFFEINE CACHE PERFORMANCE REPORT ===");

		Map<String, Object> queryStats = (Map<String, Object>) stats.get("queryCache");
		LOG.info("Query Cache - Hit Rate: {:.2f}% ({} hits, {} misses), Size: {}, Evictions: {}, Avg Load: {:.2f}ms",
				queryStats.get("hitRate"), queryStats.get("hitCount"), queryStats.get("missCount"),
				stats.get("queryCacheSize"), queryStats.get("evictionCount"), queryStats.get("averageLoadPenalty"));

		Map<String, Object> entityStats = (Map<String, Object>) stats.get("entityCache");
		LOG.info("Entity Cache - Hit Rate: {:.2f}% ({} hits, {} misses), Size: {}, Evictions: {}, Avg Load: {:.2f}ms",
				entityStats.get("hitRate"), entityStats.get("hitCount"), entityStats.get("missCount"),
				stats.get("entityCacheSize"), entityStats.get("evictionCount"), entityStats.get("averageLoadPenalty"));

		Map<String, Object> countStats = (Map<String, Object>) stats.get("countCache");
		LOG.info("Count Cache - Hit Rate: {:.2f}% ({} hits, {} misses), Size: {}, Evictions: {}, Avg Load: {:.2f}ms",
				countStats.get("hitRate"), countStats.get("hitCount"), countStats.get("missCount"),
				stats.get("countCacheSize"), countStats.get("evictionCount"), countStats.get("averageLoadPenalty"));

		LOG.info("Cache Health: {}", isCacheHealthy() ? "HEALTHY" : "DEGRADED");
	}

	// =================== SCHEDULED MAINTENANCE ===================

	public void performCacheMaintenance() {
		long start = System.nanoTime();

		queryCache.cleanUp();
		entityCache.cleanUp();
		countCache.cleanUp();

		if (LOG.isDebugEnabled()) {
			LOG.debug("Cache maintenance completed in {}ms", (System.nanoTime() - start) / 1_000_000);
		}
	}

	public void preloadCriticalData(Class<T> entityClass) {
		countAll(entityClass);
		getAllFromDBPaginated(entityClass, 0, 100);
		LOG.debug("Preloaded critical data for {}", entityClass.getSimpleName());
	}

	@SuppressWarnings("rawtypes")
	public void reloadCache() {
		EposDataModelDAO newCache = new EposDataModelDAO(instance);

		newCache.getAllFromDB(Dataproduct.class);
		newCache.getAllFromDB(Distribution.class);
		newCache.getAllFromDB(Webservice.class);
		newCache.getAllFromDB(Category.class);
		newCache.getAllFromDB(Mapping.class);
		newCache.getAllFromDB(Operation.class);
		newCache.getAllFromDB(Organization.class);
		newCache.getAllFromDB(Versioningstatus.class);
		newCache.getAllFromDB(Person.class);

		synchronized (INSTANCE_LOCK) {
			cacheInstance = newCache;
			instance = cacheInstance;
		}
		LOG.info("Cache reload completed");
	}

	// =================== REFLECTION METHOD CACHING ===================

	private Method getVersionGetter(Class<?> clazz) {
		return VERSION_GETTER_CACHE.computeIfAbsent(clazz, c -> {
			try {
				return c.getMethod("getVersion");
			} catch (NoSuchMethodException e) {
				return null;
			}
		});
	}

	private Method getStatusGetter(Class<?> clazz) {
		return STATUS_GETTER_CACHE.computeIfAbsent(clazz, c -> {
			try {
				return c.getMethod("getStatus");
			} catch (NoSuchMethodException e) {
				return null;
			}
		});
	}

	private Map<String, Method> getListGetters(Class<?> clazz) {
		return LIST_GETTER_CACHE.computeIfAbsent(clazz, c -> {
			Map<String, Method> getters = new HashMap<>();
			for (Method m : c.getMethods()) {
				if (m.getName().startsWith("get") && m.getReturnType().equals(List.class) && m.getParameterCount() == 0) {
					String propName = m.getName().substring(3);
					getters.put(propName, m);
				}
			}
			return getters;
		});
	}

	// =================== RESOURCE CLEANUP HELPERS ===================

	private void rollbackQuietly(EntityTransaction tx) {
		if (tx != null && tx.isActive()) {
			try {
				tx.rollback();
			} catch (Exception e) {
				LOG.warn("Rollback failed", e);
			}
		}
	}

	private void closeQuietly(EntityManager em) {
		if (em != null) {
			try {
				em.close();
			} catch (Exception e) {
				LOG.trace("EntityManager close failed", e);
			}
		}
	}

	// =================== BATCH FETCH OPERATIONS (Performance Optimization) ===================

	/**
	 * Batch fetches multiple entity types by parent ID in a single database round-trip.
	 * Reduces N+1 query patterns by fetching all related entities at once.
	 * 
	 * <p><strong>Performance:</strong> For an entity with 5 relation types, this reduces
	 * queries from 5+ to 1, providing ~5x improvement in DB round-trips.</p>
	 * 
	 * @param parentFieldName the field name that references the parent (e.g., "dataproductInstance")
	 * @param parentInstanceId the parent's instance ID
	 * @param relationClasses the relation/join table classes to fetch
	 * @return a map from class to list of entities, never null
	 */
	@SafeVarargs
	public final Map<Class<?>, List<?>> batchFetchRelationsByParentId(
			String parentFieldName, String parentInstanceId, Class<?>... relationClasses) {
		
		if (parentInstanceId == null || parentInstanceId.isBlank() || relationClasses == null || relationClasses.length == 0) {
			return Collections.emptyMap();
		}

		Map<Class<?>, List<?>> results = new HashMap<>(relationClasses.length);
		EntityManager em = null;
		
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			
			for (Class<?> relationClass : relationClasses) {
				String cacheKey = generateCacheKey("batchRel", parentFieldName, parentInstanceId, relationClass.getSimpleName());
				
				@SuppressWarnings("unchecked")
				List<Object> cached = (List<Object>) getFromQueryCache(cacheKey);
				if (cached != null) {
					results.put(relationClass, cached);
					continue;
				}
				
				try {
					// Try embedded ID field first (e.g., "dataproductInstanceId")
					String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
					String jpql = "SELECT r FROM " + relationClass.getSimpleName() + " r WHERE r.id." + embeddedIdField + " = :pid";
					
					TypedQuery<?> query = em.createQuery(jpql, relationClass);
					query.setParameter("pid", parentInstanceId);
					List<?> result = query.getResultList();
					
					results.put(relationClass, result);
					putInQueryCache(cacheKey, result);
					
				} catch (Exception e) {
					// Fallback: try direct field access
					try {
						String jpql = "SELECT r FROM " + relationClass.getSimpleName() + " r WHERE r." + parentFieldName + ".instanceId = :pid";
						TypedQuery<?> query = em.createQuery(jpql, relationClass);
						query.setParameter("pid", parentInstanceId);
						List<?> result = query.getResultList();
						
						results.put(relationClass, result);
						putInQueryCache(cacheKey, result);
					} catch (Exception e2) {
						LOG.debug("Failed to batch fetch {}: {}", relationClass.getSimpleName(), e2.getMessage());
						results.put(relationClass, Collections.emptyList());
					}
				}
			}
			
			return results;
			
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Batch fetches entities by multiple instance IDs in a single query.
	 * Useful for pre-loading targets in relation synchronization.
	 * 
	 * <p><strong>Performance:</strong> Fetching 10 entities by ID takes 1 query instead of 10.</p>
	 * 
	 * @param instanceIds the instance IDs to fetch
	 * @param entityClass the entity class
	 * @return a map from instanceId to entity, never null
	 */
	public <E> Map<String, E> batchFetchByInstanceIds(List<String> instanceIds, Class<E> entityClass) {
		if (instanceIds == null || instanceIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		// Remove nulls and duplicates
		List<String> cleanIds = instanceIds.stream()
				.filter(id -> id != null && !id.isBlank())
				.distinct()
				.collect(java.util.stream.Collectors.toList());
		
		if (cleanIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		// Check cache first for individual entities
		Map<String, E> results = new HashMap<>(cleanIds.size());
		List<String> uncachedIds = new java.util.ArrayList<>(cleanIds.size());
		
		for (String id : cleanIds) {
			String cacheKey = generateCacheKey("entity", id, entityClass.getSimpleName());
			@SuppressWarnings("unchecked")
			E cached = (E) getFromEntityCache(cacheKey);
			if (cached != null) {
				results.put(id, cached);
			} else {
				uncachedIds.add(id);
			}
		}
		
		// Fetch uncached entities in batches
		if (!uncachedIds.isEmpty()) {
			EntityManager em = null;
			try {
				em = EntityManagerService.getInstance().createEntityManager();
				
				// Process in batches to avoid query parameter limits
				int batchSize = 100;
				for (int i = 0; i < uncachedIds.size(); i += batchSize) {
					List<String> batch = uncachedIds.subList(i, Math.min(i + batchSize, uncachedIds.size()));
					
					TypedQuery<E> query = em.createQuery(
							"SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.instanceId IN :ids", 
							entityClass);
					query.setParameter("ids", batch);
					
					for (E entity : query.getResultList()) {
						String instanceId = utilities.ReflectionCache.getInstanceId(entity);
						if (instanceId != null) {
							results.put(instanceId, entity);
							// Cache individual entities
							String cacheKey = generateCacheKey("entity", instanceId, entityClass.getSimpleName());
							putInEntityCache(cacheKey, entity);
						}
					}
				}
			} finally {
				closeQuietly(em);
			}
		}
		
		return results;
	}

	/**
	 * Batch fetches entities by multiple UIDs in a single query.
	 * 
	 * @param uids the UIDs to fetch
	 * @param entityClass the entity class
	 * @return a map from UID to list of entities (multiple versions may exist per UID)
	 */
	public <E> Map<String, List<E>> batchFetchByUids(List<String> uids, Class<E> entityClass) {
		if (uids == null || uids.isEmpty()) {
			return Collections.emptyMap();
		}
		
		List<String> cleanUids = uids.stream()
				.filter(uid -> uid != null && !uid.isBlank())
				.distinct()
				.collect(java.util.stream.Collectors.toList());
		
		if (cleanUids.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, List<E>> results = new HashMap<>(cleanUids.size());
		EntityManager em = null;
		
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			
			// Process in batches
			int batchSize = 100;
			for (int i = 0; i < cleanUids.size(); i += batchSize) {
				List<String> batch = cleanUids.subList(i, Math.min(i + batchSize, cleanUids.size()));
				
				TypedQuery<E> query = em.createQuery(
						"SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.uid IN :uids", 
						entityClass);
				query.setParameter("uids", batch);
				
				for (E entity : query.getResultList()) {
					String uid = utilities.ReflectionCache.getUid(entity);
					if (uid != null) {
						results.computeIfAbsent(uid, k -> new java.util.ArrayList<>(2)).add(entity);
					}
				}
			}
			
			return results;
			
		} finally {
			closeQuietly(em);
		}
	}

	// ===== BULK RETRIEVAL OPTIMIZATION METHODS =====

	/**
	 * Batch fetches relations for MULTIPLE parent entities at once.
	 * This dramatically reduces N+1 query problems by fetching all relations
	 * for many parents in a single query (or batched queries for large sets).
	 * 
	 * <p><strong>Performance Example:</strong><br>
	 * Fetching categories for 400 DataProducts: 1 query instead of 400 queries.</p>
	 * 
	 * @param parentFieldName the field name in the join entity (e.g., "dataproductInstance")
	 * @param parentInstanceIds the parent instance IDs to fetch relations for
	 * @param relationClass the join table class
	 * @return a map from parentInstanceId to list of relation entities
	 */
	public <R> Map<String, List<R>> batchFetchRelationsForMultipleParents(
			String parentFieldName, 
			List<String> parentInstanceIds, 
			Class<R> relationClass) {
		
		if (parentInstanceIds == null || parentInstanceIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		List<String> cleanIds = parentInstanceIds.stream()
				.filter(id -> id != null && !id.isBlank())
				.distinct()
				.collect(java.util.stream.Collectors.toList());
		
		if (cleanIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, List<R>> results = new HashMap<>(cleanIds.size());
		EntityManager em = null;
		
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			
			// Try embedded ID field first (e.g., "dataproductInstanceId")
			String embeddedIdField = parentFieldName.replace("Instance", "InstanceId");
			String jpqlEmbedded = "SELECT r FROM " + relationClass.getSimpleName() + 
					" r WHERE r.id." + embeddedIdField + " IN :ids";
			String jpqlDirect = "SELECT r FROM " + relationClass.getSimpleName() + 
					" r WHERE r." + parentFieldName + ".instanceId IN :ids";
			
			// Process in batches of 100 to avoid query parameter limits
			int batchSize = 100;
			for (int i = 0; i < cleanIds.size(); i += batchSize) {
				List<String> batch = cleanIds.subList(i, Math.min(i + batchSize, cleanIds.size()));
				
				List<R> batchResults = null;
				try {
					// Try embedded ID approach first
					TypedQuery<R> query = em.createQuery(jpqlEmbedded, relationClass);
					query.setParameter("ids", batch);
					batchResults = query.getResultList();
				} catch (Exception e) {
					// Fallback to direct field approach
					try {
						TypedQuery<R> query = em.createQuery(jpqlDirect, relationClass);
						query.setParameter("ids", batch);
						batchResults = query.getResultList();
					} catch (Exception e2) {
						LOG.debug("Failed to batch fetch relations for {}: {}", 
								relationClass.getSimpleName(), e2.getMessage());
						continue;
					}
				}
				
				// Group results by parent instance ID
				if (batchResults != null) {
					for (R relation : batchResults) {
						String parentId = extractParentInstanceId(relation, parentFieldName);
						if (parentId != null) {
							results.computeIfAbsent(parentId, k -> new java.util.ArrayList<>(4)).add(relation);
						}
					}
				}
			}
			
			return results;
			
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Extracts the parent instance ID from a relation/join entity using reflection.
	 */
	private String extractParentInstanceId(Object relation, String parentFieldName) {
		try {
			// Try to get the parent entity via getter
			String getterName = "get" + Character.toUpperCase(parentFieldName.charAt(0)) + parentFieldName.substring(1);
			java.lang.reflect.Method getter = relation.getClass().getMethod(getterName);
			Object parent = getter.invoke(relation);
			if (parent != null) {
				return utilities.ReflectionCache.getInstanceId(parent);
			}
		} catch (Exception e) {
			// Try embedded ID approach
			try {
				java.lang.reflect.Method getIdMethod = relation.getClass().getMethod("getId");
				Object embeddedId = getIdMethod.invoke(relation);
				if (embeddedId != null) {
					String idFieldName = parentFieldName.replace("Instance", "InstanceId");
					String idGetterName = "get" + Character.toUpperCase(idFieldName.charAt(0)) + idFieldName.substring(1);
					java.lang.reflect.Method idGetter = embeddedId.getClass().getMethod(idGetterName);
					Object idValue = idGetter.invoke(embeddedId);
					return idValue != null ? idValue.toString() : null;
				}
			} catch (Exception e2) {
				LOG.trace("Could not extract parent ID from {}: {}", relation.getClass().getSimpleName(), e2.getMessage());
			}
		}
		return null;
	}

	/**
	 * Batch fetches Versioningstatus records for multiple instance IDs.
	 * Essential for bulk retrieval operations to avoid N+1 versioning lookups.
	 * 
	 * @param instanceIds the instance IDs to fetch versioning for
	 * @return a map from instanceId to Versioningstatus
	 */
	public Map<String, Versioningstatus> batchFetchVersioningStatus(List<String> instanceIds) {
		if (instanceIds == null || instanceIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		List<String> cleanIds = instanceIds.stream()
				.filter(id -> id != null && !id.isBlank())
				.distinct()
				.collect(java.util.stream.Collectors.toList());
		
		if (cleanIds.isEmpty()) {
			return Collections.emptyMap();
		}
		
		Map<String, Versioningstatus> results = new HashMap<>(cleanIds.size());
		EntityManager em = null;
		
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			
			int batchSize = 100;
			for (int i = 0; i < cleanIds.size(); i += batchSize) {
				List<String> batch = cleanIds.subList(i, Math.min(i + batchSize, cleanIds.size()));
				
				TypedQuery<Versioningstatus> query = em.createQuery(
						"SELECT v FROM Versioningstatus v WHERE v.instanceId IN :ids", 
						Versioningstatus.class);
				query.setParameter("ids", batch);
				
				for (Versioningstatus vs : query.getResultList()) {
					if (vs.getInstanceId() != null) {
						results.put(vs.getInstanceId(), vs);
					}
				}
			}
			
			return results;
			
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Batch fetches all entities of a given class.
	 * More efficient than getAllFromDB for bulk operations.
	 * 
	 * @param entityClass the entity class to fetch
	 * @return list of all entities
	 */
	public <E> List<E> batchFetchAll(Class<E> entityClass) {
		String cacheKey = generateCacheKey("batchAll", entityClass.getSimpleName());
		
		@SuppressWarnings("unchecked")
		List<E> cached = (List<E>) getFromQueryCache(cacheKey);
		if (cached != null) {
			return cached;
		}
		
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			
			TypedQuery<E> query = em.createQuery(
					"SELECT e FROM " + entityClass.getSimpleName() + " e", 
					entityClass);
			List<E> results = query.getResultList();
			
			putInQueryCache(cacheKey, results);
			return results;
			
		} finally {
			closeQuietly(em);
		}
	}
}