package integrationtests.unittests;

import static org.junit.jupiter.api.Assertions.*;

import integrationtests.TestcontainersLifecycle;
import metadataapis.DataProductAPI;
import model.StatusType;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.UUID;

class VersioningCollisionTest extends TestcontainersLifecycle {

    private DataProductAPI dataProductAPI;

    @BeforeEach
    void setUp() {
        // Ensure the test DB is clean or use rollback transactions
        dataProductAPI = new DataProductAPI("DATAPRODUCT", model.Dataproduct.class);
    }

    @Test
    @Order(1)
    @DisplayName("Scenario 1: Insert DRAFT with existing UID of a PUBLISHED -> New Version")
    void testInsertDraftOnExistingPublished() {
        String fixedUid = "DATAPRODUCT/" + UUID.randomUUID().toString();

        // 1. SETUP: Create V1 PUBLISHED
        org.epos.eposdatamodel.DataProduct v1 = createDummyDataProduct();
        v1.setUid(fixedUid);
        v1.setStatus(StatusType.PUBLISHED);
        v1.setTitle(Collections.singletonList("Version 1 Original"));

        LinkedEntity leV1 = dataProductAPI.create(v1, StatusType.PUBLISHED, null, null);
        String v1InstanceId = leV1.getInstanceId();

        assertNotNull(v1InstanceId);

        // 2. ACTION: Insert a NEW object with SAME UID but DRAFT status (without instanceId)
        org.epos.eposdatamodel.DataProduct v2Draft = createDummyDataProduct();
        v2Draft.setUid(fixedUid); // Same UID
        v2Draft.setStatus(StatusType.DRAFT); // Different status
        v2Draft.setTitle(Collections.singletonList("Version 2 Draft"));
        // NOTE: instanceId and metaId are NOT set, simulating an external insertion

        LinkedEntity leV2 = dataProductAPI.create(v2Draft, StatusType.DRAFT, null, null);
        String v2InstanceId = leV2.getInstanceId();

        // 3. VERIFICATION
        // They must be two different entities
        assertNotEquals(v1InstanceId, v2InstanceId, "A new InstanceId should have been created");

        // Retrieve V1 and V2
        var retrievedV1 = dataProductAPI.retrieve(v1InstanceId);
        var retrievedV2 = dataProductAPI.retrieve(v2InstanceId);

        // V1 must still be PUBLISHED and unchanged
        assertEquals(StatusType.PUBLISHED, retrievedV1.getStatus());
        assertEquals("Version 1 Original", retrievedV1.getTitle().get(0));

        // V2 must be DRAFT and point to V1
        assertEquals(StatusType.DRAFT, retrievedV2.getStatus());
        assertEquals("Version 2 Draft", retrievedV2.getTitle().get(0));
        assertEquals(v1InstanceId, retrievedV2.getInstanceChangedId(),
                "V2 must reference V1 as its parent");
    }

    @Test
    @Order(2)
    @DisplayName("Scenario 2: Insert PUBLISHED with existing UID of a PUBLISHED -> Update (Overwrite)")
    void testInsertPublishedOnExistingPublished() {
        String fixedUid = "DATAPRODUCT/" + UUID.randomUUID().toString();

        // 1. SETUP: Create V1 PUBLISHED
        org.epos.eposdatamodel.DataProduct v1 = createDummyDataProduct();
        v1.setUid(fixedUid);
        v1.setStatus(StatusType.PUBLISHED);
        v1.setTitle(Collections.singletonList("Title Original"));

        LinkedEntity leV1 = dataProductAPI.create(v1, StatusType.PUBLISHED, null, null);
        String v1InstanceId = leV1.getInstanceId();

        // 2. ACTION: Insert a NEW object with SAME UID and PUBLISHED status (without instanceId)
        org.epos.eposdatamodel.DataProduct v1Update = createDummyDataProduct();
        v1Update.setUid(fixedUid); // Same UID
        v1Update.setStatus(StatusType.PUBLISHED); // Same status
        v1Update.setTitle(Collections.singletonList("Title Updated")); // Content change

        LinkedEntity leUpdate = dataProductAPI.create(v1Update, StatusType.PUBLISHED, null, null);
        String updateInstanceId = leUpdate.getInstanceId();

        // 3. VERIFICATION
        // The ID must not change (same object)
        assertEquals(v1InstanceId, updateInstanceId,
                "The InstanceId should NOT change (this is an update)");

        // Retrieve the object
        var retrievedObj = dataProductAPI.retrieve(v1InstanceId);

        // Must be PUBLISHED
        assertEquals(StatusType.PUBLISHED, retrievedObj.getStatus());

        // Title must be updated (overwrite)
        assertEquals("Title Updated", retrievedObj.getTitle().get(0),
                "The title should have been updated");

        // No other versions should exist (implicit check via retrieve)
    }

    // Helper
    private org.epos.eposdatamodel.DataProduct createDummyDataProduct() {
        org.epos.eposdatamodel.DataProduct dp = new org.epos.eposdatamodel.DataProduct();
        dp.setDescription(Collections.singletonList("Test Description"));
        dp.setIssued(OffsetDateTime.now().toLocalDateTime());
        dp.setModified(OffsetDateTime.now().toLocalDateTime());
        return dp;
    }
}
