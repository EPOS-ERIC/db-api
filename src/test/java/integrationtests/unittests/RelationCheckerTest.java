package integrationtests.unittests;

import integrationtests.TestcontainersLifecycle;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.*;

import relationsapi.RelationChecker;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;


class RelationCheckerTest extends TestcontainersLifecycle {

    private static final String ENTITY_TYPE = "DataProduct";
    private static final String UID = "dataproduct/test-entity-123";
    private static final String META_ID = "meta-id-456";

    @BeforeEach
    void setUp() {
        // Clear ThreadLocal before each test
        clearProcessingEntities();
    }

    @AfterEach
    void tearDown() {
        // Clear ThreadLocal after each test
        clearProcessingEntities();
    }

    /**
     * Clears the ThreadLocal processingEntities using reflection.
     */
    private void clearProcessingEntities() {
        try {
            Field field = RelationChecker.class.getDeclaredField("processingEntities");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ThreadLocal<Set<String>> processingEntities =
                    (ThreadLocal<Set<String>>) field.get(null);
            processingEntities.remove();
        } catch (Exception e) {
            // Ignore if cleanup fails
        }
    }

    /**
     * Invokes the private method getEntityKey using reflection.
     */
    private String invokeGetEntityKey(LinkedEntity linkedEntity) throws Exception {
        Method method =
                RelationChecker.class.getDeclaredMethod("getEntityKey", LinkedEntity.class);
        method.setAccessible(true);
        return (String) method.invoke(null, linkedEntity);
    }

    @Nested
    @DisplayName("getEntityKey tests - Key generation for cycle detection")
    class GetEntityKeyTests {

        @Test
        @DisplayName("Should use UID as the primary identifier")
        void shouldUseUidAsPrimaryIdentifier() throws Exception {
            // Arrange
            LinkedEntity entity = new LinkedEntity();
            entity.setEntityType(ENTITY_TYPE);
            entity.setUid(UID);
            entity.setInstanceId("instance-id-will-be-ignored");
            entity.setMetaId(META_ID);

            // Act
            String key = invokeGetEntityKey(entity);

            // Assert
            assertEquals(ENTITY_TYPE + ":" + UID, key);
            assertFalse(key.contains("instance-id"),
                    "The key must NOT contain instanceId when uid is available");
        }

        @Test
        @DisplayName("Should use metaId as fallback when uid is null")
        void shouldUseMetaIdAsFallbackWhenUidIsNull() throws Exception {
            // Arrange
            LinkedEntity entity = new LinkedEntity();
            entity.setEntityType(ENTITY_TYPE);
            entity.setUid(null);
            entity.setMetaId(META_ID);
            entity.setInstanceId("instance-id-fallback");

            // Act
            String key = invokeGetEntityKey(entity);

            // Assert
            assertEquals(ENTITY_TYPE + ":" + META_ID, key);
        }

        @Test
        @DisplayName("Should use metaId as fallback when uid is empty")
        void shouldUseMetaIdAsFallbackWhenUidIsEmpty() throws Exception {
            // Arrange
            LinkedEntity entity = new LinkedEntity();
            entity.setEntityType(ENTITY_TYPE);
            entity.setUid("");
            entity.setMetaId(META_ID);
            entity.setInstanceId("instance-id-fallback");

            // Act
            String key = invokeGetEntityKey(entity);

            // Assert
            assertEquals(ENTITY_TYPE + ":" + META_ID, key);
        }

        @Test
        @DisplayName("Should use instanceId only as last resort")
        void shouldUseInstanceIdAsLastResort() throws Exception {
            // Arrange
            String instanceId = "last-resort-instance-id";
            LinkedEntity entity = new LinkedEntity();
            entity.setEntityType(ENTITY_TYPE);
            entity.setUid(null);
            entity.setMetaId(null);
            entity.setInstanceId(instanceId);

            // Act
            String key = invokeGetEntityKey(entity);

            // Assert
            assertEquals(ENTITY_TYPE + ":" + instanceId, key);
        }

        @Test
        @DisplayName("Two versions of the same entity must generate the SAME key")
        void twoVersionsOfSameEntityShouldGenerateSameKey() throws Exception {
            // Arrange - Simulate PUBLISHED and DRAFT versions
            LinkedEntity publishedVersion = new LinkedEntity();
            publishedVersion.setEntityType(ENTITY_TYPE);
            publishedVersion.setUid(UID);
            publishedVersion.setMetaId(META_ID);
            publishedVersion.setInstanceId("published-instance-id-111");

            LinkedEntity draftVersion = new LinkedEntity();
            draftVersion.setEntityType(ENTITY_TYPE);
            draftVersion.setUid(UID);
            draftVersion.setMetaId(META_ID);
            draftVersion.setInstanceId("draft-instance-id-222");

            // Act
            String keyPublished = invokeGetEntityKey(publishedVersion);
            String keyDraft = invokeGetEntityKey(draftVersion);

            // Assert
            assertEquals(keyPublished, keyDraft,
                    "CRITICAL: Different versions of the same entity must generate the same key");
        }
    }
}
