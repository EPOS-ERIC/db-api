package utilities;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Centralized reflection caching utility for performance-critical paths.
 * 
 * <p>Provides ~10x speedup over raw reflection for repeated invocations by caching
 * Method and MethodHandle lookups. Thread-safe via ConcurrentHashMap.</p>
 * 
 * <p><strong>Usage:</strong></p>
 * <pre>
 * // Instead of:
 * String id = (String) obj.getClass().getMethod("getInstanceId").invoke(obj);
 * 
 * // Use:
 * String id = ReflectionCache.getInstanceId(obj);
 * </pre>
 * 
 * <p><strong>Performance Notes:</strong></p>
 * <ul>
 *   <li>First call per class incurs reflection lookup cost (~1-5ms)</li>
 *   <li>Subsequent calls use cached MethodHandle (~0.01-0.1ms)</li>
 *   <li>MethodHandles provide better JIT optimization than Method.invoke()</li>
 * </ul>
 */
public final class ReflectionCache {

    private static final Logger LOG = Logger.getLogger(ReflectionCache.class.getName());
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    // MethodHandle caches for common entity getters (fastest invocation)
    private static final ConcurrentHashMap<Class<?>, MethodHandle> INSTANCE_ID_HANDLES = new ConcurrentHashMap<>(64);
    private static final ConcurrentHashMap<Class<?>, MethodHandle> META_ID_HANDLES = new ConcurrentHashMap<>(64);
    private static final ConcurrentHashMap<Class<?>, MethodHandle> UID_HANDLES = new ConcurrentHashMap<>(64);
    private static final ConcurrentHashMap<Class<?>, MethodHandle> VERSION_HANDLES = new ConcurrentHashMap<>(64);
    private static final ConcurrentHashMap<Class<?>, MethodHandle> STATUS_HANDLES = new ConcurrentHashMap<>(64);

    // Generic method cache for polymorphic relations and less common methods
    private static final ConcurrentHashMap<String, Method> METHOD_CACHE = new ConcurrentHashMap<>(256);
    
    // Sentinel for methods that don't exist (avoid repeated NoSuchMethodException)
    private static final Method NO_SUCH_METHOD;
    static {
        try {
            NO_SUCH_METHOD = ReflectionCache.class.getDeclaredMethod("noSuchMethodSentinel");
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
    @SuppressWarnings("unused")
    private static void noSuchMethodSentinel() {}

    private ReflectionCache() {
        // Utility class - no instantiation
    }

    // ========================================================================
    // High-Performance Entity Property Getters (using MethodHandles)
    // ========================================================================

    /**
     * Gets the instanceId property from any entity object.
     * Uses cached MethodHandle for optimal performance.
     * 
     * @param obj the entity object
     * @return the instanceId value, or null if not found or obj is null
     */
    public static String getInstanceId(Object obj) {
        return invokeStringHandle(obj, INSTANCE_ID_HANDLES, "getInstanceId");
    }

    /**
     * Gets the metaId property from any entity object.
     */
    public static String getMetaId(Object obj) {
        return invokeStringHandle(obj, META_ID_HANDLES, "getMetaId");
    }

    /**
     * Gets the uid property from any entity object.
     */
    public static String getUid(Object obj) {
        return invokeStringHandle(obj, UID_HANDLES, "getUid");
    }

    /**
     * Gets the version object from any entity.
     * 
     * @param obj the entity object
     * @return the Versioningstatus object, or null
     */
    public static Object getVersion(Object obj) {
        return invokeHandle(obj, VERSION_HANDLES, "getVersion");
    }

    /**
     * Gets the status from any object (typically Versioningstatus or StatusType).
     * 
     * @param obj the object containing status
     * @return the status value as Object, or null
     */
    public static Object getStatus(Object obj) {
        return invokeHandle(obj, STATUS_HANDLES, "getStatus");
    }

    /**
     * Gets the version status string from an entity.
     * Combines getVersion() and getStatus() in one call.
     * 
     * @param obj the entity object
     * @return the status string, or null if version/status is null
     */
    public static String getVersionStatus(Object obj) {
        Object version = getVersion(obj);
        if (version == null) return null;
        Object status = getStatus(version);
        return status != null ? status.toString() : null;
    }

    // ========================================================================
    // Generic Method Caching (for polymorphic relations)
    // ========================================================================

    /**
     * Gets a cached Method object for the given class and method signature.
     * Returns null if the method doesn't exist (cached negative result).
     * 
     * @param clazz the class to search
     * @param methodName the method name
     * @param paramTypes the parameter types
     * @return the Method, or null if not found
     */
    public static Method getMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
        String key = buildMethodKey(clazz, methodName, paramTypes);
        Method cached = METHOD_CACHE.get(key);
        
        if (cached != null) {
            return cached == NO_SUCH_METHOD ? null : cached;
        }
        
        return METHOD_CACHE.computeIfAbsent(key, k -> {
            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                return NO_SUCH_METHOD;
            }
        }) == NO_SUCH_METHOD ? null : METHOD_CACHE.get(key);
    }

    /**
     * Invokes a cached getter method and returns the result as String.
     * 
     * @param obj the target object
     * @param methodName the getter method name (e.g., "getEntityInstanceId")
     * @return the result as String, or null
     */
    public static String invokeStringGetter(Object obj, String methodName) {
        if (obj == null) return null;
        Method method = getMethod(obj.getClass(), methodName);
        if (method == null) return null;
        try {
            Object result = method.invoke(obj);
            return result != null ? result.toString() : null;
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Failed to invoke {0} on {1}: {2}",
                    new Object[]{methodName, obj.getClass().getSimpleName(), e.getMessage()});
            return null;
        }
    }

    /**
     * Invokes a cached getter method and returns the raw result.
     * 
     * @param obj the target object
     * @param methodName the getter method name
     * @return the result object, or null
     */
    public static Object invokeGetter(Object obj, String methodName) {
        if (obj == null) return null;
        Method method = getMethod(obj.getClass(), methodName);
        if (method == null) return null;
        try {
            return method.invoke(obj);
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Failed to invoke {0} on {1}: {2}",
                    new Object[]{methodName, obj.getClass().getSimpleName(), e.getMessage()});
            return null;
        }
    }

    /**
     * Invokes a cached setter method with a single String parameter.
     * 
     * @param obj the target object
     * @param methodName the setter method name (e.g., "setEntityInstanceId")
     * @param value the value to set
     * @return true if successful, false otherwise
     */
    public static boolean invokeStringSetter(Object obj, String methodName, String value) {
        if (obj == null) return false;
        Method method = getMethod(obj.getClass(), methodName, String.class);
        if (method == null) return false;
        try {
            method.invoke(obj, value);
            return true;
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Failed to invoke {0} on {1}: {2}",
                    new Object[]{methodName, obj.getClass().getSimpleName(), e.getMessage()});
            return false;
        }
    }

    /**
     * Invokes a cached setter method with a single typed parameter.
     * 
     * @param obj the target object
     * @param methodName the setter method name
     * @param paramType the parameter type
     * @param value the value to set
     * @return true if successful, false otherwise
     */
    public static boolean invokeSetter(Object obj, String methodName, Class<?> paramType, Object value) {
        if (obj == null) return false;
        Method method = getMethod(obj.getClass(), methodName, paramType);
        if (method == null) return false;
        try {
            method.invoke(obj, value);
            return true;
        } catch (Exception e) {
            LOG.log(Level.FINEST, "Failed to invoke {0} on {1}: {2}",
                    new Object[]{methodName, obj.getClass().getSimpleName(), e.getMessage()});
            return false;
        }
    }

    // ========================================================================
    // Internal MethodHandle Operations
    // ========================================================================

    private static String invokeStringHandle(Object target, ConcurrentHashMap<Class<?>, MethodHandle> cache, 
                                             String methodName) {
        Object result = invokeHandle(target, cache, methodName);
        return result != null ? result.toString() : null;
    }

    private static Object invokeHandle(Object target, ConcurrentHashMap<Class<?>, MethodHandle> cache,
                                       String methodName) {
        if (target == null) return null;

        Class<?> clazz = target.getClass();
        MethodHandle handle = cache.computeIfAbsent(clazz, c -> {
            try {
                Method method = c.getMethod(methodName);
                return LOOKUP.unreflect(method);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                LOG.log(Level.FINEST, "Method {0} not found on {1}", 
                        new Object[]{methodName, c.getSimpleName()});
                return null;
            }
        });

        if (handle == null) return null;

        try {
            return handle.invoke(target);
        } catch (Throwable t) {
            if (t instanceof Error) throw (Error) t;
            LOG.log(Level.FINEST, "MethodHandle invocation failed: {0}", t.getMessage());
            return null;
        }
    }

    private static String buildMethodKey(Class<?> clazz, String methodName, Class<?>[] paramTypes) {
        if (paramTypes == null || paramTypes.length == 0) {
            return clazz.getName() + '#' + methodName;
        }
        // Pre-size StringBuilder for efficiency
        int estimatedSize = clazz.getName().length() + methodName.length() + paramTypes.length * 30;
        StringBuilder sb = new StringBuilder(estimatedSize);
        sb.append(clazz.getName()).append('#').append(methodName);
        for (Class<?> pt : paramTypes) {
            sb.append('#').append(pt.getName());
        }
        return sb.toString();
    }

    // ========================================================================
    // Cache Statistics (for monitoring/debugging)
    // ========================================================================

    /**
     * Returns the total number of cached method lookups.
     */
    public static int getCacheSize() {
        return METHOD_CACHE.size() + INSTANCE_ID_HANDLES.size() + META_ID_HANDLES.size() 
               + UID_HANDLES.size() + VERSION_HANDLES.size() + STATUS_HANDLES.size();
    }

    /**
     * Clears all caches. Use only for testing or after hot-reloading classes.
     */
    public static void clearCaches() {
        METHOD_CACHE.clear();
        INSTANCE_ID_HANDLES.clear();
        META_ID_HANDLES.clear();
        UID_HANDLES.clear();
        VERSION_HANDLES.clear();
        STATUS_HANDLES.clear();
    }
}
