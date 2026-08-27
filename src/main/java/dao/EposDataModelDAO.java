package dao;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EmbeddableType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;
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
import model.AuthorizationGroup;
import model.EdmEntityId;
import model.MetadataGroup;

import org.epos.eposdatamodel.LinkedEntity;
import org.epos.handler.dbapi.service.EntityManagerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * High-performance Data Access Object for EPOS data model entities.
 *
 * Queries the database directly so reads always observe the latest committed state.
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
	// PostgreSQL supports far more bind parameters; larger read batches remove most
	// network round-trips when retrieving hundreds of metadata records.
	private static final int READ_BATCH_SIZE = 1_000;
	private static final char KEY_SEP = '\u001F';

	// Reflection method cache - eliminates repeated lookups which are expensive
	private static final ConcurrentHashMap<Class<?>, Method> VERSION_GETTER_CACHE = new ConcurrentHashMap<>(32);
	private static final ConcurrentHashMap<Class<?>, Method> STATUS_GETTER_CACHE = new ConcurrentHashMap<>(32);
	private static final ConcurrentHashMap<Class<?>, Map<String, Method>> LIST_GETTER_CACHE = new ConcurrentHashMap<>(32);
	private static final ConcurrentHashMap<RelationAccessorKey, Optional<EmbeddedIdAccess>> EMBEDDED_ID_ACCESS_CACHE = new ConcurrentHashMap<>(64);
	private static final ConcurrentHashMap<RelationAccessorKey, Optional<Method>> PARENT_ACCESSOR_CACHE = new ConcurrentHashMap<>(64);

	private record RelationAccessorKey(Class<?> relationClass, String parentFieldName) {}
	private record EmbeddedIdAccess(Method relationIdGetter, Method parentIdGetter) {}

	public static record DataProductSummaryRow(String instanceId, String metaId, String uid,
			String type, String accrualPeriodicity, java.time.LocalDateTime created,
			java.time.LocalDateTime issued, java.time.LocalDateTime modified, String versionInfo,
			String documentation, String qualityAssurance, String accessRight, String keywords,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record DataProductTitleRow(String dataProductInstanceId, String title) {}

	/** Lightweight authorization relation used where neither side needs hydration. */
    public static record AuthorizationGroupRow(String metaId, String groupId, String entityType) {}

	/** Lightweight group membership relation used while assembling full group DTOs. */
	public static record MetadataGroupUserRow(String groupId, String authIdentifier, String role, String requestStatus) {}

	public static record WebServiceSummaryRow(String instanceId, String metaId, String uid,
			java.time.LocalDateTime dateModified, java.time.LocalDateTime datePublished, String description,
			String entryPoint, String license, String name, String aaaiTypes, String keywords,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record SoftwareApplicationSummaryRow(String instanceId, String metaId, String uid,
			String name, String description, String downloadUrl, String installUrl, String keywords,
			String licenseUrl, String mainEntityOfPage, String requirements, String softwareVersion,
			String softwareStatus, String fileSize, String spatial, String temporal,
			String memoryRequirements, String processorRequirements, String storageRequirements,
			String timeRequired, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record OrganizationSummaryRow(String instanceId, String metaId, String uid,
			String acronym, String legalname, String leicode, String logo, String url, String type,
			String maturity, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record DistributionSummaryRow(String instanceId, String metaId, String uid,
			String type, String format, String license, String datapolicy, java.time.LocalDateTime issued,
			java.time.LocalDateTime modified, String byteSize, String maturity, String mediaType,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record PersonSummaryRow(String instanceId, String metaId, String uid,
			String familyname, String givenname, String cvurl, String qualifications, String versionId,
			String versionMetaId, String changeComment, OffsetDateTime changeTimestamp, String editorId,
			String provenance, String version, String instanceChangeId, String status) {}

	public static record ContactPointSummaryRow(String instanceId, String metaId, String uid, String role,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record SoftwareSourceCodeSummaryRow(String instanceId, String metaId, String uid,
			String name, String description, String downloadUrl, String keywords, String licenseUrl,
			String mainEntityOfPage, String runtimePlatform, String softwareVersion, String codeRepository,
			String softwareStatus, String spatial, String temporal, String fileSize, String timeRequired,
			String softwareRequirements, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record EquipmentSummaryRow(String instanceId, String metaId, String uid, String type,
			String resolution, String description, String dynamicRange, String filter, String identifier,
			String name, String pageUrl, String orientation, String samplePeriod, String serialNumber,
			String keywords, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record FacilitySummaryRow(String instanceId, String metaId, String uid, String type,
			String identifier, String description, String title, String keywords, String versionId,
			String versionMetaId, String changeComment, OffsetDateTime changeTimestamp, String editorId,
			String provenance, String version, String instanceChangeId, String status) {}

	public static record MappingSummaryRow(String instanceId, String metaId, String uid, String label,
			String valuepattern, String defaultvalue, String maxvalue, String minvalue, String multipleValues,
			String readOnlyValue, Boolean required, String range, String property, String variable,
			String healthcheckvalue, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record OperationSummaryRow(String instanceId, String metaId, String uid, String method,
			String template, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record PayloadSummaryRow(String instanceId, String metaId, String uid, String versionId,
			String versionMetaId, String changeComment, OffsetDateTime changeTimestamp, String editorId,
			String provenance, String version, String instanceChangeId, String status) {}

	public static record AttributionSummaryRow(String instanceId, String metaId, String uid, String versionId,
			String versionMetaId, String changeComment, OffsetDateTime changeTimestamp, String editorId,
			String provenance, String version, String instanceChangeId, String status) {}

	public static record CategorySummaryRow(String instanceId, String metaId, String uid, String name,
			String description, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record CategorySchemeSummaryRow(String instanceId, String metaId, String uid, String name,
			String description, String code, String homepage, String logo, String color, String orderitemnumber,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record AddressSummaryRow(String instanceId, String metaId, String uid, String street, String country,
			String postalCode, String countrycode, String locality, String versionId, String versionMetaId,
			String changeComment, OffsetDateTime changeTimestamp, String editorId, String provenance, String version,
			String instanceChangeId, String status) {}

	public static record ElementSummaryRow(String instanceId, String metaId, String uid, String type, String value,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record IdentifierSummaryRow(String instanceId, String metaId, String uid, String type, String value,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record SpatialSummaryRow(String instanceId, String metaId, String uid, String location,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record TemporalSummaryRow(String instanceId, String metaId, String uid, java.time.LocalDateTime startdate,
			java.time.LocalDateTime enddate,
			String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record ParameterSummaryRow(String instanceId, String metaId, String uid, String encodingformat,
			String conformsto, String action, String versionId, String versionMetaId, String changeComment,
			OffsetDateTime changeTimestamp, String editorId, String provenance, String version, String instanceChangeId,
			String status) {}

	public static record QuantitativeValueSummaryRow(String instanceId, String metaId, String uid, String unitcode,
			String value, String versionId, String versionMetaId, String changeComment, OffsetDateTime changeTimestamp,
			String editorId, String provenance, String version, String instanceChangeId, String status) {}

	public static record OutputMappingSummaryRow(String instanceId, String metaId, String uid, String label,
			String valuepattern, Boolean required, String range, String property, String variable, String versionId,
			String versionMetaId, String changeComment, OffsetDateTime changeTimestamp, String editorId,
			String provenance, String version, String instanceChangeId, String status) {}

	/*
	 * Primary query cache - stores list results with optimized TTL.
	 * Uses write-through expiration plus shorter access-based refresh for better memory efficiency.
	 * Increased size for higher hit rates on read-heavy workloads.
	 */
	/*
	 * Count cache - shorter TTL since aggregates change more frequently than individual records.
	 * Added access-based expiry to keep frequently accessed counts warm.
	 */
	/*
	 * Entity cache - longer TTL for individual entity lookups which are more stable.
	 * Differentiated TTLs: longer write expiry, shorter access expiry for better memory use.
	 */
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

	private void putInQueryCache(String key, Object value) { }
	private void putInEntityCache(String key, Object value) { }
	private void putInCountCache(String key, Long value) { }
	private <R> R getFromQueryCache(String key) { return null; }
	private <R> R getFromEntityCache(String key) { return null; }
	private Long getFromCountCache(String key) { return null; }
	private void evictCacheByPattern(String pattern) { }
	private String generateCacheKey(String method, Object... params) { return method; }

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
		} catch (Exception e) {
			throw new IllegalStateException("Unable to sanitize version status for "
					+ entity.getClass().getSimpleName(), e);
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
		} catch (Exception e) {
			throw new IllegalStateException("Unable to persist dependent version status for "
					+ entity.getClass().getSimpleName(), e);
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
			String path = joinParentPath(em, joinClass, parentIdField);
			TypedQuery<J> query = em.createQuery(
					"SELECT c FROM " + joinClass.getSimpleName() + " c WHERE c." + path + " = :pid",
					joinClass);
			query.setParameter("pid", parentId);
			List<J> result = query.getResultList();

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

	private String joinParentPath(EntityManager em, Class<?> joinClass, String parentIdField) {
		ManagedType<?> managedType = em.getMetamodel().managedType(joinClass);
		if (managedType instanceof IdentifiableType<?> identifiableType) {
			if (identifiableType.getIdType() instanceof EmbeddableType<?> idType) {
				try {
					idType.getAttribute(parentIdField);
					return "id." + parentIdField;
				} catch (IllegalArgumentException ignored) {
					// The parent key is represented by an entity attribute instead.
				}
			}
		}
		return specificKeyPath(em, joinClass, parentIdField);
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
	 * Batch update with periodic flush/clear for memory efficiency on large datasets.
	 * Commits all-or-nothing for transactional consistency.
	 */
	public Boolean updateListOfObjects(List<T> objects) {
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

				em.merge(obj);

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
			LOG.error("Error during batch update", e);
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

	/**
	 * Deletes a principal entity and its referencing join rows in one transaction.
	 * The relation fields are model property names and are supplied only by internal
	 * API classes, so they are not derived from request input.
	 */
	public Boolean deleteByInstanceIdWithRelations(String instanceId, Class<?> entityClass,
			Map<Class<?>, String> relationFields) {
		return deleteByInstanceIdWithRelations(instanceId, entityClass, relationFields, Collections.emptyList());
	}

	/**
	 * Deletes a principal entity after removing join rows and nullifying direct references to it.
	 * All configured fields are internal model properties, never request input.
	 */
	public Boolean deleteByInstanceIdWithRelations(String instanceId, Class<?> entityClass,
			Map<Class<?>, String> relationFields, List<RelationField> referenceFields) {
		if (instanceId == null || instanceId.isBlank() || entityClass == null) {
			return false;
		}

		EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			if (relationFields != null) {
				for (Map.Entry<Class<?>, String> relation : relationFields.entrySet()) {
					em.createQuery("DELETE FROM " + relation.getKey().getSimpleName()
							+ " r WHERE r." + relation.getValue() + ".instanceId = :id")
							.setParameter("id", instanceId)
							.executeUpdate();
					evictCacheByPattern(relation.getKey().getSimpleName());
				}
			}

			if (referenceFields != null) {
				for (RelationField reference : referenceFields) {
					em.createQuery("UPDATE " + reference.entityClass().getSimpleName()
							+ " r SET r." + reference.parentField() + " = NULL WHERE r."
							+ reference.parentField() + ".instanceId = :id")
							.setParameter("id", instanceId)
							.executeUpdate();
					evictCacheByPattern(reference.entityClass().getSimpleName());
				}
			}

			int deleted = em.createQuery("DELETE FROM " + entityClass.getSimpleName() + " e WHERE e.instanceId = :id")
					.setParameter("id", instanceId)
					.executeUpdate();
			if (deleted == 0) {
				tx.rollback();
				return false;
			}
			tx.commit();
			evictCacheByPattern(entityClass.getSimpleName());
			return true;
		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error deleting {} with relations", entityClass.getSimpleName(), e);
			return false;
		} finally {
			closeQuietly(em);
		}
	}

	/** A join entity and the parent-side property used for a targeted bulk delete. */
	public record RelationField(Class<?> entityClass, String parentField) {
	}

	/**
	 * Variant for principals referenced through multiple fields of the same join
	 * entity, such as bidirectional self-relations.
	 */
	public Boolean deleteByInstanceIdWithRelations(String instanceId, Class<?> entityClass,
			List<RelationField> relationFields) {
		if (instanceId == null || instanceId.isBlank() || entityClass == null) {
			return false;
		}

		EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();

			if (relationFields != null) {
				for (RelationField relation : relationFields) {
					String path = relation.parentField().startsWith("id.")
							? "r." + relation.parentField()
							: "r." + relation.parentField() + ".instanceId";
					em.createQuery("DELETE FROM " + relation.entityClass().getSimpleName()
							+ " r WHERE " + path + " = :id")
							.setParameter("id", instanceId).executeUpdate();
					evictCacheByPattern(relation.entityClass().getSimpleName());
				}
			}

			em.createQuery("DELETE FROM " + entityClass.getSimpleName() + " e WHERE e.instanceId = :id")
					.setParameter("id", instanceId)
					.executeUpdate();
			tx.commit();
			evictCacheByPattern(entityClass.getSimpleName());
			return true;
		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error deleting {} with relations", entityClass.getSimpleName(), e);
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

	/**
	 * Returns distinct scalar values without materializing entity rows or their
	 * eager associations. The property path is supplied only by internal APIs.
	 */
	public List<String> getDistinctStringValues(Class<?> entityClass, String propertyPath) {
		if (entityClass == null || propertyPath == null || propertyPath.isBlank()) {
			return Collections.emptyList();
		}

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			validatePath(em, entityClass, propertyPath);
			TypedQuery<String> query = em.createQuery(
					"SELECT DISTINCT e." + propertyPath + " FROM " + entityClass.getSimpleName()
							+ " e WHERE e." + propertyPath + " IS NOT NULL", String.class);
			return query.getResultList();
		} catch (Exception e) {
			LOG.error("Error retrieving distinct {} from {}", propertyPath, entityClass.getSimpleName(), e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/** Fetches authorization keys without materializing eager AuthorizationGroup associations. */
	public List<AuthorizationGroupRow> getAuthorizationGroupRowsByMetaIds(List<String> metaIds) {
		if (metaIds == null || metaIds.isEmpty()) return Collections.emptyList();

		List<AuthorizationGroupRow> rows = new ArrayList<>();
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			for (int i = 0; i < metaIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = metaIds.subList(i, Math.min(i + READ_BATCH_SIZE, metaIds.size()));
				rows.addAll(em.createQuery("SELECT NEW dao.EposDataModelDAO$AuthorizationGroupRow(ag.meta.metaId, ag.group.id, ag.meta.tableName) "
						+ "FROM AuthorizationGroup ag WHERE ag.meta.metaId IN :metaIds", AuthorizationGroupRow.class)
						.setParameter("metaIds", batch).getResultList());
			}
			return rows;
		} catch (Exception e) {
			LOG.error("Error retrieving authorization group rows by metadata IDs", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/** Fetches authorization keys for groups without loading metadata entities. */
	public List<AuthorizationGroupRow> getAuthorizationGroupRowsByGroupIds(List<String> groupIds) {
		if (groupIds == null || groupIds.isEmpty()) return Collections.emptyList();

		List<AuthorizationGroupRow> rows = new ArrayList<>();
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			for (int i = 0; i < groupIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = groupIds.subList(i, Math.min(i + READ_BATCH_SIZE, groupIds.size()));
				rows.addAll(em.createQuery("SELECT NEW dao.EposDataModelDAO$AuthorizationGroupRow(ag.meta.metaId, ag.group.id, ag.meta.tableName) "
						+ "FROM AuthorizationGroup ag WHERE ag.group.id IN :groupIds", AuthorizationGroupRow.class)
						.setParameter("groupIds", batch).getResultList());
			}
			return rows;
		} catch (Exception e) {
			LOG.error("Error retrieving authorization group rows by group IDs", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/** Fetches membership values without loading eager user or group associations. */
	public List<MetadataGroupUserRow> getMetadataGroupUserRowsByGroupIds(List<String> groupIds) {
		if (groupIds == null || groupIds.isEmpty()) return Collections.emptyList();

		List<MetadataGroupUserRow> rows = new ArrayList<>();
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			for (int i = 0; i < groupIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = groupIds.subList(i, Math.min(i + READ_BATCH_SIZE, groupIds.size()));
				rows.addAll(em.createQuery("SELECT NEW dao.EposDataModelDAO$MetadataGroupUserRow(mgu.group.id, mgu.authIdentifier.authIdentifier, mgu.role, mgu.requestStatus) "
						+ "FROM MetadataGroupUser mgu WHERE mgu.group.id IN :groupIds", MetadataGroupUserRow.class)
						.setParameter("groupIds", batch).getResultList());
			}
			return rows;
		} catch (Exception e) {
			LOG.error("Error retrieving group membership rows", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/** Fetches membership values for users without loading eager user or group associations. */
	public List<MetadataGroupUserRow> getMetadataGroupUserRowsByUserIds(List<String> userIds) {
		if (userIds == null || userIds.isEmpty()) return Collections.emptyList();

		List<MetadataGroupUserRow> rows = new ArrayList<>();
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			for (int i = 0; i < userIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = userIds.subList(i, Math.min(i + READ_BATCH_SIZE, userIds.size()));
				rows.addAll(em.createQuery("SELECT NEW dao.EposDataModelDAO$MetadataGroupUserRow(mgu.group.id, mgu.authIdentifier.authIdentifier, mgu.role, mgu.requestStatus) "
						+ "FROM MetadataGroupUser mgu WHERE mgu.authIdentifier.authIdentifier IN :userIds", MetadataGroupUserRow.class)
						.setParameter("userIds", batch).getResultList());
			}
			return rows;
		} catch (Exception e) {
			LOG.error("Error retrieving group membership rows by user IDs", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/** Fetches all authorization keys without materializing eager AuthorizationGroup associations. */
	public List<AuthorizationGroupRow> getAllAuthorizationGroupRows() {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			return em.createQuery("SELECT NEW dao.EposDataModelDAO$AuthorizationGroupRow(ag.meta.metaId, ag.group.id, ag.meta.tableName) "
					+ "FROM AuthorizationGroup ag", AuthorizationGroupRow.class).getResultList();
		} catch (Exception e) {
			LOG.error("Error retrieving all authorization group rows", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/** Fetches all membership values without loading eager user or group associations. */
	public List<MetadataGroupUserRow> getAllMetadataGroupUserRows() {
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			return em.createQuery("SELECT NEW dao.EposDataModelDAO$MetadataGroupUserRow(mgu.group.id, mgu.authIdentifier.authIdentifier, mgu.role, mgu.requestStatus) "
					+ "FROM MetadataGroupUser mgu", MetadataGroupUserRow.class).getResultList();
		} catch (Exception e) {
			LOG.error("Error retrieving all group membership rows", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/** Fetches groups by ID in bounded queries; MetadataGroup has no eager relations. */
	public List<MetadataGroup> getMetadataGroupsByIds(List<String> groupIds) {
		if (groupIds == null || groupIds.isEmpty()) return Collections.emptyList();

		List<MetadataGroup> groups = new ArrayList<>();
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			for (int i = 0; i < groupIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = groupIds.subList(i, Math.min(i + READ_BATCH_SIZE, groupIds.size()));
				groups.addAll(em.createQuery("SELECT g FROM MetadataGroup g WHERE g.id IN :groupIds", MetadataGroup.class)
						.setParameter("groupIds", batch).getResultList());
			}
			return groups;
		} catch (Exception e) {
			LOG.error("Error retrieving metadata groups by IDs", e);
			return Collections.emptyList();
		} finally {
			closeQuietly(em);
		}
	}

	/**
	 * Creates missing group-metadata associations in one transaction. Null and unknown metadata IDs are ignored;
	 * the return value is the number of rows inserted.
	 */
	public int addMetadataElementsToGroup(List<String> metaIds, String groupId) {
		if (groupId == null || metaIds == null || metaIds.isEmpty()) return 0;
		List<String> requestedIds = metaIds.stream().filter(Objects::nonNull).distinct().toList();
		if (requestedIds.isEmpty()) return 0;

		EntityManager em = null;
		EntityTransaction tx = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			tx = em.getTransaction();
			tx.begin();
			if (em.find(MetadataGroup.class, groupId) == null) {
				tx.commit();
				return 0;
			}

			Set<String> validIds = new LinkedHashSet<>();
			Set<String> existingIds = new HashSet<>();
			for (int i = 0; i < requestedIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = requestedIds.subList(i, Math.min(i + READ_BATCH_SIZE, requestedIds.size()));
				validIds.addAll(em.createQuery("SELECT e.metaId FROM EdmEntityId e WHERE e.metaId IN :metaIds", String.class)
						.setParameter("metaIds", batch).getResultList());
				existingIds.addAll(em.createQuery("SELECT ag.meta.metaId FROM AuthorizationGroup ag "
						+ "WHERE ag.group.id = :groupId AND ag.meta.metaId IN :metaIds", String.class)
						.setParameter("groupId", groupId).setParameter("metaIds", batch).getResultList());
			}

			int inserted = 0;
			MetadataGroup group = em.getReference(MetadataGroup.class, groupId);
			for (String metaId : validIds) {
				if (existingIds.contains(metaId)) continue;
				AuthorizationGroup authorization = new AuthorizationGroup();
				authorization.setId(UUID.randomUUID().toString());
				authorization.setGroup(group);
				authorization.setMeta(em.getReference(EdmEntityId.class, metaId));
				em.persist(authorization);
				inserted++;
				if (inserted % BATCH_SIZE == 0) {
					em.flush();
					em.clear();
					group = em.getReference(MetadataGroup.class, groupId);
				}
			}
			em.flush();
			tx.commit();
			evictCacheByPattern(AuthorizationGroup.class.getSimpleName());
			return inserted;
		} catch (Exception e) {
			rollbackQuietly(tx);
			LOG.error("Error adding metadata elements to group {}", groupId, e);
			return 0;
		} finally {
			closeQuietly(em);
		}
	}

	public List<String> getAllIDsFromDB(Class<T> obj) {
		String cacheKey = generateCacheKey("allIDsFromDB", obj.getSimpleName());

		List<String> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<String> query = em.createQuery(
					"SELECT c.instanceId FROM " + obj.getSimpleName() + " c", String.class);
			List<String> result = query.getResultList();
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
			String path = specificKeyPath(em, obj, key);
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + path + " = :val", obj);
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

	/**
	 * Resolves the historical relation-key convention without appending
	 * instanceId to scalar or already-qualified paths.
	 */
	private String specificKeyPath(EntityManager em, Class<?> entityClass, String key) {
		if (key == null || key.isBlank()) {
			throw new IllegalArgumentException("Lookup key must not be blank");
		}
		validatePath(em, entityClass, key);
		if (key.contains(".")) return key;

		Attribute<?, ?> attribute = em.getMetamodel().managedType(entityClass).getAttribute(key);
		Attribute.PersistentAttributeType type = attribute.getPersistentAttributeType();
		boolean association = type == Attribute.PersistentAttributeType.MANY_TO_ONE
				|| type == Attribute.PersistentAttributeType.ONE_TO_ONE
				|| type == Attribute.PersistentAttributeType.MANY_TO_MANY
				|| type == Attribute.PersistentAttributeType.ONE_TO_MANY;
		return association ? key + ".instanceId" : key;
	}

	private void validatePath(EntityManager em, Class<?> entityClass, String path) {
		String[] segments = path.split("\\.");
		ManagedType<?> current = em.getMetamodel().managedType(entityClass);
		for (int i = 0; i < segments.length; i++) {
			Attribute<?, ?> attribute = current.getAttribute(segments[i]);
			if (i < segments.length - 1) {
				current = em.getMetamodel().managedType(attribute.getJavaType());
			}
		}
	}

	public List<T> getOneFromDBBySpecificKey(String key, String value, Class<T> obj) {
		String cacheKey = generateCacheKey("specificKey", key, value, obj.getSimpleName());

		List<T> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			String path = specificKeyPath(em, obj, key);
			TypedQuery<T> query = em.createQuery(
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c." + path + " = :val", obj);
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
            query.setHint("javax.persistence.cache.storeMode", "REFRESH");
            query.setHint("jakarta.persistence.cache.storeMode", "REFRESH");
            query.setHint("eclipselink.refresh", "true");
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
			validatePath(em, obj, key);
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
			validatePath(em, obj, key);
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

	/** Fetches rows whose validated scalar or association path matches any supplied value. */
	public <E> List<E> getListFromDBBySpecificKey(String key, List<?> values, Class<E> entityClass) {
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			String path = specificKeyPath(em, entityClass, key);
			TypedQuery<E> query = em.createQuery(
					"SELECT c FROM " + entityClass.getSimpleName() + " c WHERE c." + path + " IN :values", entityClass);
			query.setParameter("values", values);
			return query.getResultList();
		} catch (Exception e) {
			LOG.error("Error in getListFromDBBySpecificKey", e);
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

			if (predicates.isEmpty()) {
				throw new IllegalArgumentException("No valid predicates for " + obj.getSimpleName());
			}
			cq.select(root).where(cb.and(predicates.toArray(new Predicate[0])));

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
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.metaId = :mid", obj);
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
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.uid = :uid", obj);
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
					"SELECT c FROM " + obj.getSimpleName() + " c WHERE c.versionId = :vid", obj);
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

	public List<String> getAllIDsFromDBWithStatus(Class<T> obj, StatusType status) {
		if (status == null) return getAllIDsFromDB(obj);

		String cacheKey = generateCacheKey("allIDsFromDBWithStatus", obj.getSimpleName(), status.name());

		List<String> cached = getFromQueryCache(cacheKey);
		if (cached != null) return cached;

		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			TypedQuery<String> query = em.createQuery(
					"SELECT c.instanceId FROM " + obj.getSimpleName() + " c " +
							"JOIN Versioningstatus v ON c.instanceId = v.instanceId " +
							"WHERE v.status = :status", String.class);
			query.setParameter("status", status.name());
			List<String> result = query.getResultList();
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

	// =================== CACHE COMPATIBILITY API ===================

	public Map<String, Object> getDetailedCacheStats() {
		Map<String, Object> stats = new HashMap<>();
		stats.put("queryCache", Collections.emptyMap());
		stats.put("entityCache", Collections.emptyMap());
		stats.put("countCache", Collections.emptyMap());
		stats.put("queryCacheSize", 0L);
		stats.put("entityCacheSize", 0L);
		stats.put("countCacheSize", 0L);
		return stats;
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
	}

	public void invalidateAllCachesForClass(String className) {
	}

	public void clearAllCaches() {
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
		return true;
	}

	/**
	 * Outputs cache performance report to logger at INFO level.
	 */
    public void printCacheReport() {
		LOG.info("Caching disabled; all DAO reads query the database directly");
	}

	// =================== SCHEDULED MAINTENANCE ===================

	public void performCacheMaintenance() {
	}

	public void preloadCriticalData(Class<T> entityClass) {
		countAll(entityClass);
		getAllFromDBPaginated(entityClass, 0, 100);
		LOG.debug("Preloaded critical data for {}", entityClass.getSimpleName());
	}

	@SuppressWarnings("rawtypes")
	public void reloadCache() {
		LOG.debug("Caching disabled; reload ignored");
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
				LOG.warn("Initiating transaction rollback due to active transaction failure");
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
				for (int i = 0; i < uncachedIds.size(); i += READ_BATCH_SIZE) {
					List<String> batch = uncachedIds.subList(i, Math.min(i + READ_BATCH_SIZE, uncachedIds.size()));
					
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

	/** Scalar projections for summary readers; avoids hydrating eager entity associations. */
	public List<DataProductSummaryRow> fetchDataProductSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$DataProductSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.type, d.accrualperiodicity, d.created, d.issued, d.modified, "
				+ "d.versioninfo, d.documentation, d.qualityassurance, d.accessright, d.keywords, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM Dataproduct d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", DataProductSummaryRow.class);
	}

	public List<DataProductTitleRow> fetchDataProductSummaryTitles(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$DataProductTitleRow("
				+ "t.dataproductInstance.instanceId, t.title) FROM DataproductTitle t "
				+ "WHERE t.dataproductInstance.instanceId IN :ids", DataProductTitleRow.class);
	}

	public List<WebServiceSummaryRow> fetchWebServiceSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$WebServiceSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.datamodified, d.datapublished, d.description, d.entrypoint, "
				+ "d.license, d.name, d.aaaitypes, d.keywords, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Webservice d LEFT JOIN d.version v WHERE d.instanceId IN :ids", WebServiceSummaryRow.class);
	}

	public List<SoftwareApplicationSummaryRow> fetchSoftwareApplicationSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$SoftwareApplicationSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.name, d.description, d.downloadurl, d.installurl, d.keywords, "
				+ "d.licenseurl, d.mainentityofpage, d.requirements, d.softwareversion, d.softwareStatus, d.fileSize, "
				+ "d.spatial, d.temporal, d.memoryrequirements, d.processorRequirements, d.storageRequirements, "
				+ "d.timeRequired, v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, "
				+ "v.provenance, v.version, v.instanceChangeId, v.status) FROM Softwareapplication d "
				+ "LEFT JOIN d.version v WHERE d.instanceId IN :ids", SoftwareApplicationSummaryRow.class);
	}

	public List<OrganizationSummaryRow> fetchOrganizationSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$OrganizationSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.acronym, d.legalname, d.leicode, d.logo, d.url, d.type, d.maturity, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM Organization d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", OrganizationSummaryRow.class);
	}

	public List<DistributionSummaryRow> fetchDistributionSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$DistributionSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.type, d.format, d.license, d.datapolicy, d.issued, d.modified, "
				+ "d.byteSize, d.maturity, d.mediaType, v.versionId, v.metaId, v.changeComment, v.changeTimestamp, "
				+ "v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) FROM Distribution d "
				+ "LEFT JOIN d.version v WHERE d.instanceId IN :ids", DistributionSummaryRow.class);
	}

	public List<PersonSummaryRow> fetchPersonSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$PersonSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.familyname, d.givenname, d.cvurl, d.qualifications, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM Person d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", PersonSummaryRow.class);
	}

	public List<ContactPointSummaryRow> fetchContactPointSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$ContactPointSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.role, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Contactpoint d LEFT JOIN d.version v WHERE d.instanceId IN :ids", ContactPointSummaryRow.class);
	}

	public List<SoftwareSourceCodeSummaryRow> fetchSoftwareSourceCodeSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$SoftwareSourceCodeSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.name, d.description, d.downloadurl, d.keywords, d.licenseurl, "
				+ "d.mainentityofpage, d.runtimeplatform, d.softwareversion, d.coderepository, d.softwareStatus, "
				+ "d.spatial, d.temporal, d.filesize, d.timerequired, d.softwarerequirements, v.versionId, "
				+ "v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, v.version, "
				+ "v.instanceChangeId, v.status) FROM Softwaresourcecode d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", SoftwareSourceCodeSummaryRow.class);
	}

	public List<EquipmentSummaryRow> fetchEquipmentSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$EquipmentSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.type, d.resolution, d.description, d.dynamicrange, d.filter, "
				+ "d.identifier, d.name, d.pageurl, d.orientation, d.sampleperiod, d.serialnumber, d.keywords, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM Equipment d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", EquipmentSummaryRow.class);
	}

	public List<FacilitySummaryRow> fetchFacilitySummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$FacilitySummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.type, d.identifier, d.description, d.title, d.keywords, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM Facility d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", FacilitySummaryRow.class);
	}

	public List<MappingSummaryRow> fetchMappingSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$MappingSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.label, d.valuepattern, d.defaultvalue, d.maxvalue, d.minvalue, "
				+ "d.multipleValues, d.readOnlyValue, d.required, d.range, d.property, d.variable, d.healthcheckvalue, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM Mapping d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", MappingSummaryRow.class);
	}

	public List<OperationSummaryRow> fetchOperationSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$OperationSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.method, d.template, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Operation d LEFT JOIN d.version v WHERE d.instanceId IN :ids", OperationSummaryRow.class);
	}

	public List<PayloadSummaryRow> fetchPayloadSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$PayloadSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, v.versionId, v.metaId, v.changeComment, v.changeTimestamp, "
				+ "v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) FROM Payload d "
				+ "LEFT JOIN d.version v WHERE d.instanceId IN :ids", PayloadSummaryRow.class);
	}

	public List<AttributionSummaryRow> fetchAttributionSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$AttributionSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, v.versionId, v.metaId, v.changeComment, v.changeTimestamp, "
				+ "v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) FROM Attribution d "
				+ "LEFT JOIN d.version v WHERE d.instanceId IN :ids", AttributionSummaryRow.class);
	}

	public List<CategorySummaryRow> fetchCategorySummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$CategorySummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.name, d.description, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Category d LEFT JOIN d.version v WHERE d.instanceId IN :ids", CategorySummaryRow.class);
	}

	public List<CategorySchemeSummaryRow> fetchCategorySchemeSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$CategorySchemeSummaryRow("
				+ "d.instanceId, d.metaId, d.uid, d.name, d.description, d.code, d.homepage, d.logo, d.color, "
				+ "d.orderitemnumber, v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, "
				+ "v.provenance, v.version, v.instanceChangeId, v.status) FROM CategoryScheme d LEFT JOIN d.version v "
				+ "WHERE d.instanceId IN :ids", CategorySchemeSummaryRow.class);
	}

	public List<AddressSummaryRow> fetchAddressSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$AddressSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.street, e.country, e.postalCode, e.countrycode, e.locality, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM Address e LEFT JOIN e.version v "
				+ "WHERE e.instanceId IN :ids", AddressSummaryRow.class);
	}

	public List<ElementSummaryRow> fetchElementSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$ElementSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.type, e.value, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Element e LEFT JOIN e.version v WHERE e.instanceId IN :ids", ElementSummaryRow.class);
	}

	public List<IdentifierSummaryRow> fetchIdentifierSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$IdentifierSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.type, e.value, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Identifier e LEFT JOIN e.version v WHERE e.instanceId IN :ids", IdentifierSummaryRow.class);
	}

	public List<SpatialSummaryRow> fetchSpatialSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$SpatialSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.location, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Spatial e LEFT JOIN e.version v WHERE e.instanceId IN :ids", SpatialSummaryRow.class);
	}

	public List<TemporalSummaryRow> fetchTemporalSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$TemporalSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.startdate, e.enddate, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Temporal e LEFT JOIN e.version v WHERE e.instanceId IN :ids", TemporalSummaryRow.class);
	}

	public List<ParameterSummaryRow> fetchParameterSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$ParameterSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.encodingformat, e.conformsto, e.action, v.versionId, v.metaId, "
				+ "v.changeComment, v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Parameter e LEFT JOIN e.version v WHERE e.instanceId IN :ids", ParameterSummaryRow.class);
	}

	public List<QuantitativeValueSummaryRow> fetchQuantitativeValueSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$QuantitativeValueSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.unitcode, e.value, v.versionId, v.metaId, v.changeComment, "
				+ "v.changeTimestamp, v.editorId, v.provenance, v.version, v.instanceChangeId, v.status) "
				+ "FROM Quantitativevalue e LEFT JOIN e.version v WHERE e.instanceId IN :ids", QuantitativeValueSummaryRow.class);
	}

	public List<OutputMappingSummaryRow> fetchOutputMappingSummaryRows(List<String> ids) {
		return fetchSummaryRows(ids, "SELECT NEW dao.EposDataModelDAO$OutputMappingSummaryRow("
				+ "e.instanceId, e.metaId, e.uid, e.label, e.valuepattern, e.required, e.range, e.property, e.variable, "
				+ "v.versionId, v.metaId, v.changeComment, v.changeTimestamp, v.editorId, v.provenance, "
				+ "v.version, v.instanceChangeId, v.status) FROM OutputMapping e LEFT JOIN e.version v "
				+ "WHERE e.instanceId IN :ids", OutputMappingSummaryRow.class);
	}

	private <R> List<R> fetchSummaryRows(List<String> ids, String jpql, Class<R> rowClass) {
		if (ids == null || ids.isEmpty()) return Collections.emptyList();
		List<String> cleanIds = ids.stream().filter(id -> id != null && !id.isBlank()).distinct().toList();
		if (cleanIds.isEmpty()) return Collections.emptyList();

		List<R> rows = new ArrayList<>(cleanIds.size());
		EntityManager em = null;
		try {
			em = EntityManagerService.getInstance().createEntityManager();
			for (int i = 0; i < cleanIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = cleanIds.subList(i, Math.min(i + READ_BATCH_SIZE, cleanIds.size()));
				TypedQuery<R> query = em.createQuery(jpql, rowClass);
				query.setParameter("ids", batch);
				rows.addAll(query.getResultList());
			}
			return rows;
		} finally {
			closeQuietly(em);
		}
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
			for (int i = 0; i < cleanUids.size(); i += READ_BATCH_SIZE) {
				List<String> batch = cleanUids.subList(i, Math.min(i + READ_BATCH_SIZE, cleanUids.size()));
				
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
			
			for (int i = 0; i < cleanIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = cleanIds.subList(i, Math.min(i + READ_BATCH_SIZE, cleanIds.size()));
				
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
		RelationAccessorKey key = new RelationAccessorKey(relation.getClass(), parentFieldName);
		try {
			// Composite join IDs contain the parent key, avoiding initialization of a
			// lazy parent association while grouping a bulk result.
			Optional<EmbeddedIdAccess> access = EMBEDDED_ID_ACCESS_CACHE.computeIfAbsent(key, ignored -> {
				try {
					Method relationIdGetter = relation.getClass().getMethod("getId");
					String idFieldName = parentFieldName.replace("Instance", "InstanceId");
					String idGetterName = "get" + Character.toUpperCase(idFieldName.charAt(0)) + idFieldName.substring(1);
					return Optional.of(new EmbeddedIdAccess(relationIdGetter,
							relationIdGetter.getReturnType().getMethod(idGetterName)));
				} catch (ReflectiveOperationException e) {
					return Optional.empty();
				}
			});
			Object embeddedId = access.map(value -> {
				try {
					return value.relationIdGetter().invoke(relation);
				} catch (ReflectiveOperationException e) {
					return null;
				}
			}).orElse(null);
			if (embeddedId != null) {
				Object idValue = access.orElseThrow().parentIdGetter().invoke(embeddedId);
				if (idValue != null) return idValue.toString();
			}
		} catch (Exception ignored) {
			// Non-composite relations use the parent association below.
		}
		try {
			// Try to get the parent entity via getter
			Optional<Method> getter = PARENT_ACCESSOR_CACHE.computeIfAbsent(key, ignored -> {
				try {
					String getterName = "get" + Character.toUpperCase(parentFieldName.charAt(0)) + parentFieldName.substring(1);
					return Optional.of(relation.getClass().getMethod(getterName));
				} catch (ReflectiveOperationException e) {
					return Optional.empty();
				}
			});
			Object parent = getter.map(method -> {
				try {
					return method.invoke(relation);
				} catch (ReflectiveOperationException e) {
					return null;
				}
			}).orElse(null);
			if (parent != null) {
				return utilities.ReflectionCache.getInstanceId(parent);
			}
		} catch (Exception e) {
			LOG.trace("Could not extract parent ID from {}: {}", relation.getClass().getSimpleName(), e.getMessage());
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
			
			for (int i = 0; i < cleanIds.size(); i += READ_BATCH_SIZE) {
				List<String> batch = cleanIds.subList(i, Math.min(i + READ_BATCH_SIZE, cleanIds.size()));
				
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
