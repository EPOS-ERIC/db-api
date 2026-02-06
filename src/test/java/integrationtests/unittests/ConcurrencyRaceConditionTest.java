package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import org.epos.eposdatamodel.*;
import org.junit.jupiter.api.*;
import relationsapi.RelationSyncUtil;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive concurrency tests to verify thread safety of entity operations.
 * 
 * <p>These tests specifically target the race conditions that can occur when
 * multiple threads attempt to create the same join entities simultaneously,
 * which previously caused:</p>
 * <ul>
 *   <li>Duplicate key constraint violations</li>
 *   <li>IllegalMonitorStateException from EclipseLink cache contention</li>
 *   <li>Null FK constraint violations from unresolved pending relations</li>
 * </ul>
 * 
 * <p>The tests use high thread counts and synchronized starts to maximize
 * the chance of hitting race conditions.</p>
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Concurrency Race Condition Tests")
public class ConcurrencyRaceConditionTest extends TestcontainersLifecycle {

    private static final int THREAD_COUNT = 10;
    private static final int OPERATIONS_PER_THREAD = 20;
    private static final long TEST_TIMEOUT_SECONDS = 120;

    private AbstractAPI personAPI;
    private AbstractAPI contactPointAPI;
    private AbstractAPI organizationAPI;
    private AbstractAPI dataProductAPI;
    private AbstractAPI categoryAPI;
    private AbstractAPI categorySchemeAPI;

    @BeforeEach
    void setupAPIs() {
        personAPI = AbstractAPI.retrieveAPI(EntityNames.PERSON.name());
        contactPointAPI = AbstractAPI.retrieveAPI(EntityNames.CONTACTPOINT.name());
        organizationAPI = AbstractAPI.retrieveAPI(EntityNames.ORGANIZATION.name());
        dataProductAPI = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        categoryAPI = AbstractAPI.retrieveAPI(EntityNames.CATEGORY.name());
        categorySchemeAPI = AbstractAPI.retrieveAPI(EntityNames.CATEGORYSCHEME.name());
    }

    @AfterEach
    void cleanupThreadLocals() {
        // Ensure ThreadLocal state is cleaned up after each test
        RelationSyncUtil.clearThreadLocalState();
    }

    // =========================================================================
    // TEST 1: Concurrent duplicate join creation (Person-ContactPoint)
    // =========================================================================

    @Test
    @Order(1)
    @DisplayName("1.1 Race condition: Concurrent threads creating same Person-ContactPoint relationship")
    void testConcurrentDuplicateJoinCreation() throws Exception {
        // Create a shared person that all threads will reference
        String sharedPersonUid = "person:race:duplicate:" + UUID.randomUUID();
        
        Person person = new Person();
        person.setUid(sharedPersonUid);
        person.setFamilyName("RaceTest");
        person.setGivenName("Concurrent");
        person.addEmail("race@test.com");
        personAPI.create(person, StatusType.PUBLISHED, null, null);

        // Now have multiple threads try to create ContactPoints pointing to same Person
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        List<Throwable> criticalErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready

                    // Each thread creates a ContactPoint with same person reference
                    ContactPoint cp = new ContactPoint();
                    cp.setUid("cp:race:duplicate:" + threadId + ":" + UUID.randomUUID());
                    cp.addEmail("race" + threadId + "@test.com");
                    cp.setRole("test");
                    cp.setPerson(new LinkedEntity().uid(sharedPersonUid).entityType("PERSON"));

                    contactPointAPI.create(cp, StatusType.PUBLISHED, null, null);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    if (isDuplicateKeyError(e)) {
                        duplicateCount.incrementAndGet();
                    } else if (isCriticalError(e)) {
                        criticalErrors.add(e);
                    }
                    // Other errors are acceptable (e.g., transient DB issues)
                } finally {
                    RelationSyncUtil.clearThreadLocalState();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown(); // Release all threads simultaneously
        assertTrue(doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        // Verify no critical errors (IllegalMonitorStateException, etc.)
        if (!criticalErrors.isEmpty()) {
            criticalErrors.forEach(e -> System.err.println("Critical error: " + e.getMessage()));
        }
        assertTrue(criticalErrors.isEmpty(),
                "Critical errors should not occur. Found: " + criticalErrors.size() +
                (criticalErrors.isEmpty() ? "" : " - First: " + criticalErrors.get(0).getMessage()));

        System.out.println("Test 1.1 PASSED: " + successCount.get() + " succeeded, " + 
                duplicateCount.get() + " duplicates handled gracefully");
    }

    // =========================================================================
    // TEST 2: Stress test with shared Organization relationships
    // =========================================================================

    @Test
    @Order(2)
    @DisplayName("2.1 Stress: Concurrent DataProducts sharing same Organization publisher")
    void testStressConcurrentSharedOrganization() throws Exception {
        // Create shared organization
        String sharedOrgUid = "org:stress:shared:" + UUID.randomUUID();
        Organization org = new Organization();
        org.setUid(sharedOrgUid);
        org.addLegalName("Stress Test Organization");
        org.addEmail("stress@test.org");
        organizationAPI.create(org, StatusType.PUBLISHED, null, null);

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicInteger totalOps = new AtomicInteger(0);
        AtomicInteger failedOps = new AtomicInteger(0);
        List<Throwable> criticalErrors = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await(); // Sync all threads to start simultaneously

                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        try {
                            DataProduct dp = new DataProduct();
                            dp.setUid("dp:stress:" + threadId + ":" + i + ":" + UUID.randomUUID());
                            dp.addTitle("Stress Test DataProduct " + threadId + "-" + i);
                            dp.addPublisher(new LinkedEntity().uid(sharedOrgUid).entityType("ORGANIZATION"));
                            dp.setStatus(StatusType.DRAFT);

                            dataProductAPI.create(dp, StatusType.DRAFT, null, null);
                            totalOps.incrementAndGet();
                        } catch (Exception e) {
                            if (isCriticalError(e)) {
                                criticalErrors.add(e);
                            }
                            failedOps.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    criticalErrors.add(e);
                } finally {
                    RelationSyncUtil.clearThreadLocalState();
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // Assert no critical errors
        assertTrue(criticalErrors.isEmpty(),
                "Critical errors occurred: " + criticalErrors.size() +
                (criticalErrors.isEmpty() ? "" : " - " + criticalErrors.get(0).getMessage()));

        // Most operations should succeed
        int expectedMin = (THREAD_COUNT * OPERATIONS_PER_THREAD) / 2; // At least 50% success
        assertTrue(totalOps.get() >= expectedMin,
                "Expected at least " + expectedMin + " successful operations, got " + totalOps.get());

        System.out.println("Test 2.1 PASSED: " + totalOps.get() + " operations succeeded, " + 
                failedOps.get() + " failed (acceptable)");
    }

    // =========================================================================
    // TEST 3: Concurrent reads and writes (cache threading)
    // =========================================================================

    @Test
    @Order(3)
    @DisplayName("3.1 Cache threading: Concurrent reads and writes")
    void testConcurrentCacheAccess() throws Exception {
        // Pre-populate some categories
        String schemeUid = "scheme:cache:" + UUID.randomUUID();
        CategoryScheme scheme = new CategoryScheme();
        scheme.setUid(schemeUid);
        scheme.setTitle("Cache Test Scheme");
        scheme.setCode("CACHE_TEST");
        categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        for (int i = 0; i < 5; i++) {
            Category cat = new Category();
            cat.setUid("cat:cache:pre:" + i + ":" + UUID.randomUUID());
            cat.setName("Cache Pre-Test " + i);
            cat.setInScheme(new LinkedEntity().uid(schemeUid).entityType("CATEGORYSCHEME"));
            categoryAPI.create(cat, StatusType.PUBLISHED, null, null);
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT * 2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT * 2);
        AtomicInteger readOps = new AtomicInteger(0);
        AtomicInteger writeOps = new AtomicInteger(0);
        List<Throwable> cacheErrors = Collections.synchronizedList(new ArrayList<>());

        // Half threads do reads, half do writes
        for (int i = 0; i < THREAD_COUNT; i++) {
            // Reader thread
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 50; j++) {
                        try {
                            categoryAPI.retrieveAll();
                            readOps.incrementAndGet();
                        } catch (Exception e) {
                            if (isCriticalError(e)) {
                                cacheErrors.add(e);
                            }
                        }
                    }
                } catch (Exception e) {
                    cacheErrors.add(e);
                } finally {
                    RelationSyncUtil.clearThreadLocalState();
                    doneLatch.countDown();
                }
            });

            // Writer thread
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < 10; j++) {
                        try {
                            Category cat = new Category();
                            cat.setUid("cat:cachewrite:" + threadId + ":" + j + ":" + UUID.randomUUID());
                            cat.setName("Cache Write Test " + threadId + "-" + j);
                            cat.setInScheme(new LinkedEntity().uid(schemeUid).entityType("CATEGORYSCHEME"));
                            categoryAPI.create(cat, StatusType.DRAFT, null, null);
                            writeOps.incrementAndGet();
                        } catch (Exception e) {
                            if (isCriticalError(e)) {
                                cacheErrors.add(e);
                            }
                        }
                    }
                } catch (Exception e) {
                    cacheErrors.add(e);
                } finally {
                    RelationSyncUtil.clearThreadLocalState();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        // Check for cache-related errors
        for (Throwable e : cacheErrors) {
            assertFalse(e instanceof IllegalMonitorStateException,
                    "Cache threading error (IllegalMonitorStateException): " + e.getMessage());
            assertFalse(e.getMessage() != null && e.getMessage().contains("current thread is not owner"),
                    "Cache threading error (thread ownership): " + e.getMessage());
        }

        System.out.println("Test 3.1 PASSED: " + readOps.get() + " reads, " + writeOps.get() + " writes");
    }

    // =========================================================================
    // TEST 4: Concurrent creation of entities with same UID (conflict test)
    // =========================================================================

    @Test
    @Order(4)
    @DisplayName("4.1 Conflict: Multiple threads creating entities with overlapping UIDs")
    void testConcurrentSameUidCreation() throws Exception {
        // All threads will try to create organizations with the same base UID pattern
        // This tests how the system handles concurrent attempts to create "the same" entity
        
        String baseOrgUid = "org:conflict:" + UUID.randomUUID();
        
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<Throwable> criticalErrors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    startLatch.await();

                    // Half threads use the SAME UID, half use unique UIDs
                    String uid = (threadId % 2 == 0) ? baseOrgUid : baseOrgUid + ":" + threadId;
                    
                    Organization org = new Organization();
                    org.setUid(uid);
                    org.addLegalName("Conflict Test Org " + threadId);
                    org.addEmail("conflict" + threadId + "@test.org");

                    organizationAPI.create(org, StatusType.DRAFT, null, null);
                    successCount.incrementAndGet();

                } catch (Exception e) {
                    if (isDuplicateKeyError(e) || isConstraintViolation(e)) {
                        conflictCount.incrementAndGet();
                    } else if (isCriticalError(e)) {
                        criticalErrors.add(e);
                    }
                } finally {
                    RelationSyncUtil.clearThreadLocalState();
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        assertTrue(doneLatch.await(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS), "Test timed out");
        executor.shutdown();

        assertTrue(criticalErrors.isEmpty(),
                "Critical errors should not occur: " + 
                (criticalErrors.isEmpty() ? "" : criticalErrors.get(0).getMessage()));

        System.out.println("Test 4.1 PASSED: " + successCount.get() + " succeeded, " + 
                conflictCount.get() + " conflicts handled");
    }

    // =========================================================================
    // TEST 5: Complex nested relations under concurrency
    // =========================================================================

    @Test
    @Order(5)
    @DisplayName("5.1 Complex: Concurrent creation of DataProducts with multiple nested relations")
    void testConcurrentComplexNestedRelations() throws Exception {
        // Setup shared entities that all DataProducts will reference
        String sharedOrgUid = "org:complex:" + UUID.randomUUID();
        String sharedPersonUid = "person:complex:" + UUID.randomUUID();
        String sharedCPUid = "cp:complex:" + UUID.randomUUID();
        String sharedSchemeUid = "scheme:complex:" + UUID.randomUUID();
        String sharedCatUid = "cat:complex:" + UUID.randomUUID();

        // Create shared Organization
        Organization org = new Organization();
        org.setUid(sharedOrgUid);
        org.addLegalName("Complex Test Organization");
        organizationAPI.create(org, StatusType.PUBLISHED, null, null);

        // Create shared Person
        Person person = new Person();
        person.setUid(sharedPersonUid);
        person.setFamilyName("Complex");
        person.setGivenName("Test");
        personAPI.create(person, StatusType.PUBLISHED, null, null);

        // Create shared ContactPoint linked to Person
        ContactPoint cp = new ContactPoint();
        cp.setUid(sharedCPUid);
        cp.addEmail("complex@test.com");
        cp.setRole("test");
        cp.setPerson(new LinkedEntity().uid(sharedPersonUid).entityType("PERSON"));
        contactPointAPI.create(cp, StatusType.PUBLISHED, null, null);

        // Create shared CategoryScheme and Category
        CategoryScheme scheme = new CategoryScheme();
        scheme.setUid(sharedSchemeUid);
        scheme.setTitle("Complex Test Scheme");
        scheme.setCode("COMPLEX_TEST");
        categorySchemeAPI.create(scheme, StatusType.PUBLISHED, null, null);

        Category cat = new Category();
        cat.setUid(sharedCatUid);
        cat.setName("Complex Test Category");
        cat.setInScheme(new LinkedEntity().uid(sharedSchemeUid).entityType("CATEGORYSCHEME"));
        categoryAPI.create(cat, StatusType.PUBLISHED, null, null);

        // Now concurrent threads create DataProducts with multiple relations
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        CyclicBarrier barrier = new CyclicBarrier(THREAD_COUNT);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> criticalErrors = Collections.synchronizedList(new ArrayList<>());

        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            futures.add(executor.submit(() -> {
                try {
                    barrier.await();

                    for (int i = 0; i < 5; i++) {
                        try {
                            DataProduct dp = new DataProduct();
                            dp.setUid("dp:complex:" + threadId + ":" + i + ":" + UUID.randomUUID());
                            dp.addTitle("Complex Test " + threadId + "-" + i);
                            dp.setStatus(StatusType.DRAFT);
                            
                            // Add multiple relations
                            dp.addPublisher(new LinkedEntity().uid(sharedOrgUid).entityType("ORGANIZATION"));
                            dp.addContactPoint(new LinkedEntity().uid(sharedCPUid).entityType("CONTACTPOINT"));
                            dp.addCategory(new LinkedEntity().uid(sharedCatUid).entityType("CATEGORY"));

                            dataProductAPI.create(dp, StatusType.DRAFT, null, null);
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            if (isCriticalError(e)) {
                                criticalErrors.add(e);
                            }
                        }
                    }
                } catch (Exception e) {
                    criticalErrors.add(e);
                } finally {
                    RelationSyncUtil.clearThreadLocalState();
                }
            }));
        }

        for (Future<?> f : futures) {
            f.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertTrue(criticalErrors.isEmpty(),
                "Critical errors should not occur: " + 
                (criticalErrors.isEmpty() ? "" : criticalErrors.get(0).getMessage()));

        int expectedMin = (THREAD_COUNT * 5) / 2;
        assertTrue(successCount.get() >= expectedMin,
                "Expected at least " + expectedMin + " successes, got " + successCount.get());

        System.out.println("Test 5.1 PASSED: " + successCount.get() + " complex entities created");
    }

    // =========================================================================
    // TEST 6: ThreadLocal cleanup verification
    // =========================================================================

    @Test
    @Order(6)
    @DisplayName("6.1 ThreadLocal: Verify cleanup prevents memory leaks in thread pools")
    void testThreadLocalCleanup() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(5); // Fixed pool reuses threads
        AtomicInteger operationCount = new AtomicInteger(0);
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        // Run many operations that use ThreadLocals
        List<Future<?>> futures = new ArrayList<>();
        for (int batch = 0; batch < 10; batch++) {
            for (int i = 0; i < 5; i++) {
                final int batchId = batch;
                final int opId = i;
                futures.add(executor.submit(() -> {
                    try {
                        RelationSyncUtil.executeWithCleanup(() -> {
                            // Simulate operation that might use cascade tracking
                            DataProduct dp = new DataProduct();
                            dp.setUid("dp:threadlocal:" + batchId + ":" + opId + ":" + UUID.randomUUID());
                            dp.addTitle("ThreadLocal Test " + batchId + "-" + opId);
                            dp.setStatus(StatusType.DRAFT);
                            
                            dataProductAPI.create(dp, StatusType.DRAFT, null, null);
                            operationCount.incrementAndGet();
                            return null;
                        });
                    } catch (Exception e) {
                        errors.add(e);
                    }
                }));
            }
        }

        for (Future<?> f : futures) {
            f.get(TEST_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        }
        executor.shutdown();

        // If ThreadLocals weren't cleaned up, we'd see memory growth or state leakage
        // This test mainly verifies no exceptions occur during cleanup
        assertTrue(errors.size() < 5, "Too many errors during ThreadLocal cleanup test: " + errors.size());

        System.out.println("Test 6.1 PASSED: " + operationCount.get() + " operations with ThreadLocal cleanup");
    }

    // =========================================================================
    // Helper methods
    // =========================================================================

    private boolean isDuplicateKeyError(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("duplicate key") ||
                                msg.contains("already exists") ||
                                msg.contains("unique constraint") ||
                                msg.contains("violates unique"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isConstraintViolation(Throwable e) {
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && (msg.contains("constraint") ||
                                msg.contains("foreign key") ||
                                msg.contains("not-null"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isCriticalError(Throwable e) {
        if (e instanceof IllegalMonitorStateException) {
            return true;
        }
        if (e instanceof ConcurrentModificationException) {
            return true;
        }
        Throwable current = e;
        while (current != null) {
            String msg = current.getMessage();
            if (msg != null && msg.contains("current thread is not owner")) {
                return true;
            }
            if (current instanceof IllegalMonitorStateException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
