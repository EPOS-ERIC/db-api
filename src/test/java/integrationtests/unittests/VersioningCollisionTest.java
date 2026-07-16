package integrationtests.unittests;

import static org.junit.jupiter.api.Assertions.*;

import integrationtests.TestcontainersLifecycle;
import metadataapis.DataProductAPI;
import metadataapis.DistributionAPI;
import metadataapis.OperationAPI;
import metadataapis.WebServiceAPI;
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
    private DistributionAPI distributionAPI;

    @BeforeEach
    void setUp() {
        // Ensure the test DB is clean or use rollback transactions
        dataProductAPI = new DataProductAPI("DATAPRODUCT", model.Dataproduct.class);
        distributionAPI = new DistributionAPI("DISTRIBUTION", model.Distribution.class);
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

    @Test
    @Order(3)
    @DisplayName("Scenario 3: DRAFTs are isolated per editor")
    void testDraftsAreIsolatedPerEditor() {
        String fixedUid = "DISTRIBUTION/" + UUID.randomUUID();

        org.epos.eposdatamodel.Distribution published = createDummyDistribution();
        published.setUid(fixedUid);
        published.setStatus(StatusType.PUBLISHED);
        published.addTitle("Published Distribution");

        LinkedEntity publishedLE = distributionAPI.create(published, StatusType.PUBLISHED, null, null);
        String publishedInstanceId = publishedLE.getInstanceId();

        org.epos.eposdatamodel.Distribution beatrizDraft = distributionAPI.retrieve(publishedInstanceId);
        beatrizDraft.setStatus(StatusType.DRAFT);
        beatrizDraft.setEditorId("beatriz");
        beatrizDraft.setTitle(Collections.singletonList("Beatriz Draft Distribution"));

        LinkedEntity beatrizDraftLE = distributionAPI.create(beatrizDraft, StatusType.DRAFT, null, null);
        String beatrizDraftInstanceId = beatrizDraftLE.getInstanceId();

        org.epos.eposdatamodel.Distribution userDraft = distributionAPI.retrieve(publishedInstanceId);
        userDraft.setStatus(StatusType.DRAFT);
        userDraft.setEditorId("valerio");
        userDraft.setTitle(Collections.singletonList("Valerio Draft Distribution"));

        LinkedEntity userDraftLE = distributionAPI.create(userDraft, StatusType.DRAFT, null, null);
        String userDraftInstanceId = userDraftLE.getInstanceId();

        assertNotEquals(beatrizDraftInstanceId, userDraftInstanceId, "Different editors must get different DRAFT instances");

        org.epos.eposdatamodel.Distribution retrievedBeatriz = distributionAPI.retrieve(beatrizDraftInstanceId);
        org.epos.eposdatamodel.Distribution retrievedUser = distributionAPI.retrieve(userDraftInstanceId);

        assertEquals(StatusType.DRAFT, retrievedBeatriz.getStatus());
        assertEquals(StatusType.DRAFT, retrievedUser.getStatus());
        assertEquals("Beatriz Draft Distribution", retrievedBeatriz.getTitle().get(0));
        assertEquals("Valerio Draft Distribution", retrievedUser.getTitle().get(0));

        org.epos.eposdatamodel.Distribution beatrizDraftUpdate = distributionAPI.retrieve(beatrizDraftInstanceId);
        beatrizDraftUpdate.setStatus(StatusType.DRAFT);
        beatrizDraftUpdate.setEditorId("beatriz");
        beatrizDraftUpdate.setTitle(Collections.singletonList("Beatriz Draft Updated"));

        LinkedEntity beatrizDraftUpdateLE = distributionAPI.create(beatrizDraftUpdate, StatusType.DRAFT, null, null);
        assertEquals(beatrizDraftInstanceId, beatrizDraftUpdateLE.getInstanceId(),
                "Same editor should reuse the existing DRAFT instance");

        org.epos.eposdatamodel.Distribution retrievedPublished = distributionAPI.retrieve(publishedInstanceId);
        assertEquals(StatusType.PUBLISHED, retrievedPublished.getStatus());
        assertEquals("Published Distribution", retrievedPublished.getTitle().get(0));
    }

    @Test
    @DisplayName("Relations do not reuse another editor's draft")
    void testRelationsUsePublishedWhenEditorDraftDoesNotExist() {
        String distributionUid = "DISTRIBUTION/" + UUID.randomUUID();
        String dataProductUid = "DATAPRODUCT/" + UUID.randomUUID();

        org.epos.eposdatamodel.Distribution publishedDistribution = createDummyDistribution();
        publishedDistribution.setUid(distributionUid);
        publishedDistribution.setStatus(StatusType.PUBLISHED);
        publishedDistribution.addTitle("Published Distribution");
        LinkedEntity publishedDistributionLE = distributionAPI.create(
                publishedDistribution, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.DataProduct publishedDataProduct = createDummyDataProduct();
        publishedDataProduct.setUid(dataProductUid);
        publishedDataProduct.setStatus(StatusType.PUBLISHED);
        publishedDataProduct.addDistribution(publishedDistributionLE);
        LinkedEntity publishedDataProductLE = dataProductAPI.create(
                publishedDataProduct, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.DataProduct userOneDataProduct =
                dataProductAPI.retrieve(publishedDataProductLE.getInstanceId());
        userOneDataProduct.setStatus(StatusType.DRAFT);
        userOneDataProduct.setEditorId("user-one");
        dataProductAPI.create(userOneDataProduct, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution userOneDistribution =
                distributionAPI.retrieve(userOneDataProduct.getDistribution().get(0).getInstanceId());
        userOneDistribution.setStatus(StatusType.DRAFT);
        userOneDistribution.setEditorId("user-one");
        userOneDistribution.setTitle(Collections.singletonList("User One Distribution"));
        distributionAPI.create(userOneDistribution, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.DataProduct userTwoDataProduct =
                dataProductAPI.retrieve(publishedDataProductLE.getInstanceId());
        userTwoDataProduct.setStatus(StatusType.DRAFT);
        userTwoDataProduct.setEditorId("user-two");
        LinkedEntity userTwoDataProductLE = dataProductAPI.create(
                userTwoDataProduct, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.DataProduct retrievedUserTwo =
                dataProductAPI.retrieve(userTwoDataProductLE.getInstanceId());
        assertNotNull(retrievedUserTwo.getDistribution());
        assertFalse(retrievedUserTwo.getDistribution().isEmpty());

        org.epos.eposdatamodel.Distribution retrievedUserTwoDistribution =
                distributionAPI.retrieve(retrievedUserTwo.getDistribution().get(0).getInstanceId());
        assertEquals("user-two", retrievedUserTwoDistribution.getEditorId());
        assertEquals("Published Distribution", retrievedUserTwoDistribution.getTitle().get(0));
    }

    @Test
    @DisplayName("Distribution relations cascade to the current editor's drafts")
    void testDistributionRelationsUsePublishedForWebServiceAndOperation() {
        WebServiceAPI webServiceAPI = new WebServiceAPI("WEBSERVICE", model.Webservice.class);
        OperationAPI operationAPI = new OperationAPI("OPERATION", model.Operation.class);

        org.epos.eposdatamodel.WebService publishedWebService = new org.epos.eposdatamodel.WebService();
        publishedWebService.setUid("WEBSERVICE/" + UUID.randomUUID());
        publishedWebService.setStatus(StatusType.PUBLISHED);
        publishedWebService.setName("Published WebService");
        LinkedEntity publishedWebServiceLE = webServiceAPI.create(
                publishedWebService, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.Operation publishedOperation = new org.epos.eposdatamodel.Operation();
        publishedOperation.setUid("OPERATION/" + UUID.randomUUID());
        publishedOperation.setStatus(StatusType.PUBLISHED);
        publishedOperation.setMethod("GET");
        publishedOperation.setTemplate("published-template");
        LinkedEntity publishedOperationLE = operationAPI.create(
                publishedOperation, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.Distribution publishedDistribution = createDummyDistribution();
        publishedDistribution.setUid("DISTRIBUTION/" + UUID.randomUUID());
        publishedDistribution.setStatus(StatusType.PUBLISHED);
        publishedDistribution.addTitle("Published Distribution");
        publishedDistribution.addAccessService(publishedWebServiceLE);
        publishedDistribution.addSupportedOperation(publishedOperationLE);
        LinkedEntity publishedDistributionLE = distributionAPI.create(
                publishedDistribution, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.Distribution userOneDistribution =
                distributionAPI.retrieve(publishedDistributionLE.getInstanceId());
        userOneDistribution.setStatus(StatusType.DRAFT);
        userOneDistribution.setEditorId("user-one");
        LinkedEntity userOneDistributionLE = distributionAPI.create(
                userOneDistribution, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution userOneDraft =
                distributionAPI.retrieve(userOneDistributionLE.getInstanceId());
        org.epos.eposdatamodel.WebService userOneWebService =
                webServiceAPI.retrieve(userOneDraft.getAccessService().get(0).getInstanceId());
        userOneWebService.setStatus(StatusType.DRAFT);
        userOneWebService.setEditorId("user-one");
        userOneWebService.setName("User One WebService");
        LinkedEntity userOneWebServiceLE = webServiceAPI.create(
                userOneWebService, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution retrievedUserOneDistribution =
                distributionAPI.retrieve(userOneDistributionLE.getInstanceId());
        assertNotNull(retrievedUserOneDistribution.getAccessService());
        assertEquals(1, retrievedUserOneDistribution.getAccessService().size());
        assertEquals(userOneWebServiceLE.getInstanceId(),
                retrievedUserOneDistribution.getAccessService().get(0).getInstanceId());

        org.epos.eposdatamodel.Distribution userOneDistributionUpdate =
                distributionAPI.retrieve(userOneDistributionLE.getInstanceId());
        userOneDistributionUpdate.setStatus(StatusType.DRAFT);
        userOneDistributionUpdate.setEditorId("user-one");
        userOneDistributionUpdate.setTitle(Collections.singletonList("User One Distribution Updated"));
        distributionAPI.create(userOneDistributionUpdate, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution retrievedUserOneDistributionAfterUpdate =
                distributionAPI.retrieve(userOneDistributionLE.getInstanceId());
        assertNotNull(retrievedUserOneDistributionAfterUpdate.getAccessService());
        assertEquals(1, retrievedUserOneDistributionAfterUpdate.getAccessService().size());
        assertEquals(userOneWebServiceLE.getInstanceId(),
                retrievedUserOneDistributionAfterUpdate.getAccessService().get(0).getInstanceId());
        assertEquals("User One Distribution Updated",
                retrievedUserOneDistributionAfterUpdate.getTitle().get(0));

        org.epos.eposdatamodel.Distribution retrievedPublishedDistribution =
                distributionAPI.retrieve(publishedDistributionLE.getInstanceId());
        assertNotNull(retrievedPublishedDistribution.getAccessService());
        assertEquals(1, retrievedPublishedDistribution.getAccessService().size());
        assertEquals(publishedWebServiceLE.getInstanceId(),
                retrievedPublishedDistribution.getAccessService().get(0).getInstanceId());

        org.epos.eposdatamodel.Operation userOneOperation =
                operationAPI.retrieve(userOneDraft.getSupportedOperation().get(0).getInstanceId());
        userOneOperation.setStatus(StatusType.DRAFT);
        userOneOperation.setEditorId("user-one");
        userOneOperation.setTemplate("user-one-template");
        operationAPI.create(userOneOperation, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution userTwoDistribution =
                distributionAPI.retrieve(publishedDistributionLE.getInstanceId());
        userTwoDistribution.setStatus(StatusType.DRAFT);
        userTwoDistribution.setEditorId("user-two");
        LinkedEntity userTwoDistributionLE = distributionAPI.create(
                userTwoDistribution, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution retrievedUserTwo =
                distributionAPI.retrieve(userTwoDistributionLE.getInstanceId());
        org.epos.eposdatamodel.WebService retrievedUserTwoWebService =
                webServiceAPI.retrieve(retrievedUserTwo.getAccessService().get(0).getInstanceId());
        org.epos.eposdatamodel.Operation retrievedUserTwoOperation =
                operationAPI.retrieve(retrievedUserTwo.getSupportedOperation().get(0).getInstanceId());

        assertEquals("user-two", retrievedUserTwoWebService.getEditorId());
        assertEquals("Published WebService", retrievedUserTwoWebService.getName());
        assertEquals("user-two", retrievedUserTwoOperation.getEditorId());
        assertEquals("published-template", retrievedUserTwoOperation.getTemplate());
    }

    @Test
    @DisplayName("Updating a draft without editor metadata keeps the same draft")
    void testDraftUpdatePreservesEditorAndTitle() {
        String uid = "DISTRIBUTION/" + UUID.randomUUID();

        org.epos.eposdatamodel.Distribution published = createDummyDistribution();
        published.setUid(uid);
        published.setStatus(StatusType.PUBLISHED);
        published.addTitle("Published Distribution");
        LinkedEntity publishedLE = distributionAPI.create(published, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.Distribution draft = distributionAPI.retrieve(publishedLE.getInstanceId());
        draft.setStatus(StatusType.DRAFT);
        draft.setEditorId("user-one");
        draft.setTitle(Collections.singletonList("First Draft Title"));
        LinkedEntity draftLE = distributionAPI.create(draft, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution firstUpdate = new org.epos.eposdatamodel.Distribution();
        firstUpdate.setInstanceId(draftLE.getInstanceId());
        firstUpdate.setMetaId(draftLE.getMetaId());
        firstUpdate.setUid(draftLE.getUid());
        firstUpdate.setStatus(StatusType.DRAFT);
        firstUpdate.setTitle(Collections.singletonList("Updated Draft Title"));
        LinkedEntity firstUpdateLE = distributionAPI.create(firstUpdate, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.Distribution secondUpdate = new org.epos.eposdatamodel.Distribution();
        secondUpdate.setInstanceId(firstUpdateLE.getInstanceId());
        secondUpdate.setMetaId(firstUpdateLE.getMetaId());
        secondUpdate.setUid(firstUpdateLE.getUid());
        secondUpdate.setStatus(StatusType.DRAFT);
        secondUpdate.setTitle(Collections.singletonList("Second Updated Draft Title"));
        LinkedEntity secondUpdateLE = distributionAPI.create(secondUpdate, StatusType.DRAFT, null, null);

        assertEquals(draftLE.getInstanceId(), firstUpdateLE.getInstanceId());
        assertEquals(draftLE.getInstanceId(), secondUpdateLE.getInstanceId());

        org.epos.eposdatamodel.Distribution retrieved = distributionAPI.retrieve(draftLE.getInstanceId());
        assertEquals("user-one", retrieved.getEditorId());
        assertEquals("Second Updated Draft Title", retrieved.getTitle().get(0));
    }

    @Test
    @DisplayName("A stale published relation is resolved to the current editor draft")
    void testStalePublishedRelationDoesNotReplaceDistributionDraft() {
        String distributionUid = "DISTRIBUTION/" + UUID.randomUUID();
        String dataProductUid = "DATAPRODUCT/" + UUID.randomUUID();

        org.epos.eposdatamodel.Distribution publishedDistribution = createDummyDistribution();
        publishedDistribution.setUid(distributionUid);
        publishedDistribution.setStatus(StatusType.PUBLISHED);
        publishedDistribution.addTitle("Published Distribution");
        LinkedEntity publishedDistributionLE = distributionAPI.create(
                publishedDistribution, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.DataProduct publishedDataProduct = createDummyDataProduct();
        publishedDataProduct.setUid(dataProductUid);
        publishedDataProduct.setStatus(StatusType.PUBLISHED);
        publishedDataProduct.addDistribution(publishedDistributionLE);
        LinkedEntity publishedDataProductLE = dataProductAPI.create(
                publishedDataProduct, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.DataProduct draftDataProduct =
                dataProductAPI.retrieve(publishedDataProductLE.getInstanceId());
        draftDataProduct.setStatus(StatusType.DRAFT);
        draftDataProduct.setEditorId("user-one");
        LinkedEntity draftDataProductLE = dataProductAPI.create(
                draftDataProduct, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.DataProduct storedDraft =
                dataProductAPI.retrieve(draftDataProductLE.getInstanceId());
        String draftDistributionId = storedDraft.getDistribution().get(0).getInstanceId();
        assertNotEquals(publishedDistributionLE.getInstanceId(), draftDistributionId);
        assertFalse(dao.EposDataModelDAO.getInstance()
                        .getOneFromDBBySpecificKeyNoCache("dataproductInstance",
                                draftDataProductLE.getInstanceId(), model.DistributionDataproduct.class)
                        .isEmpty(),
                "The DataProduct draft relation must exist before the update");

        org.epos.eposdatamodel.DataProduct staleUpdate = new org.epos.eposdatamodel.DataProduct();
        staleUpdate.setInstanceId(draftDataProductLE.getInstanceId());
        staleUpdate.setMetaId(draftDataProductLE.getMetaId());
        staleUpdate.setUid(draftDataProductLE.getUid());
        staleUpdate.setStatus(StatusType.DRAFT);
        staleUpdate.setTitle(Collections.singletonList("Updated DataProduct"));
        // Simulate a client payload that still contains the published relation.
        staleUpdate.addDistribution(publishedDistributionLE);

        dataProductAPI.create(staleUpdate, StatusType.DRAFT, null, null);

        var updatedRelations = dao.EposDataModelDAO.getInstance()
                .getOneFromDBBySpecificKeyNoCache("dataproductInstance",
                        draftDataProductLE.getInstanceId(), model.DistributionDataproduct.class);
        assertFalse(updatedRelations.isEmpty(), "The DataProduct draft relation must remain persisted");

        org.epos.eposdatamodel.DataProduct updatedDataProduct =
                dataProductAPI.retrieve(draftDataProductLE.getInstanceId());
        assertEquals(draftDistributionId,
                updatedDataProduct.getDistribution().get(0).getInstanceId());
        assertEquals("Updated DataProduct", updatedDataProduct.getTitle().get(0));

        org.epos.eposdatamodel.Distribution draftDistribution =
                distributionAPI.retrieve(draftDistributionId);
        org.epos.eposdatamodel.Distribution distributionUpdate =
                new org.epos.eposdatamodel.Distribution();
        distributionUpdate.setInstanceId(draftDistribution.getInstanceId());
        distributionUpdate.setMetaId(draftDistribution.getMetaId());
        distributionUpdate.setUid(draftDistribution.getUid());
        distributionUpdate.setStatus(StatusType.DRAFT);
        distributionUpdate.setTitle(Collections.singletonList("Updated Distribution"));

        distributionAPI.create(distributionUpdate, StatusType.DRAFT, null, null);

        assertEquals("Updated Distribution",
                distributionAPI.retrieve(draftDistributionId).getTitle().get(0));
        assertEquals("Published Distribution",
                distributionAPI.retrieve(publishedDistributionLE.getInstanceId()).getTitle().get(0));
    }

    @Test
    @DisplayName("Resaving a draft does not create a duplicate cascaded distribution")
    void testResavingDraftDoesNotCreateDuplicateCascade() {
        String distributionUid = "DISTRIBUTION/" + UUID.randomUUID();
        String dataProductUid = "DATAPRODUCT/" + UUID.randomUUID();

        org.epos.eposdatamodel.Distribution publishedDistribution = createDummyDistribution();
        publishedDistribution.setUid(distributionUid);
        publishedDistribution.setStatus(StatusType.PUBLISHED);
        publishedDistribution.addTitle("Published Distribution");
        LinkedEntity publishedDistributionLE = distributionAPI.create(
                publishedDistribution, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.DataProduct publishedDataProduct = createDummyDataProduct();
        publishedDataProduct.setUid(dataProductUid);
        publishedDataProduct.setStatus(StatusType.PUBLISHED);
        publishedDataProduct.addDistribution(publishedDistributionLE);
        LinkedEntity publishedDataProductLE = dataProductAPI.create(
                publishedDataProduct, StatusType.PUBLISHED, null, null);

        org.epos.eposdatamodel.DataProduct userOne =
                dataProductAPI.retrieve(publishedDataProductLE.getInstanceId());
        userOne.setStatus(StatusType.DRAFT);
        userOne.setEditorId("user-one");
        LinkedEntity userOneLE = dataProductAPI.create(userOne, StatusType.DRAFT, null, null);

        org.epos.eposdatamodel.DataProduct userTwo =
                dataProductAPI.retrieve(publishedDataProductLE.getInstanceId());
        userTwo.setStatus(StatusType.DRAFT);
        userTwo.setEditorId("user-two");
        LinkedEntity userTwoLE = dataProductAPI.create(userTwo, StatusType.DRAFT, null, null);

        String userOneDistributionId = dataProductAPI.retrieve(userOneLE.getInstanceId())
                .getDistribution().get(0).getInstanceId();
        String userTwoDistributionId = dataProductAPI.retrieve(userTwoLE.getInstanceId())
                .getDistribution().get(0).getInstanceId();
        assertNotEquals(userOneDistributionId, userTwoDistributionId);

        org.epos.eposdatamodel.DataProduct resave = new org.epos.eposdatamodel.DataProduct();
        resave.setInstanceId(userTwoLE.getInstanceId());
        resave.setMetaId(userTwoLE.getMetaId());
        resave.setUid(userTwoLE.getUid());
        resave.setStatus(StatusType.DRAFT);
        // Simulate the stale published relation on a second save.
        resave.addDistribution(publishedDistributionLE);

        LinkedEntity resavedLE = dataProductAPI.create(resave, StatusType.DRAFT, null, null);

        assertEquals(userTwoLE.getInstanceId(), resavedLE.getInstanceId());
        org.epos.eposdatamodel.DataProduct resaved = dataProductAPI.retrieve(resavedLE.getInstanceId());
        assertEquals(userTwoDistributionId, resaved.getDistribution().get(0).getInstanceId());
        assertEquals(StatusType.DRAFT,
                distributionAPI.retrieve(userTwoDistributionId).getStatus());
    }

    // Helper
    private org.epos.eposdatamodel.DataProduct createDummyDataProduct() {
        org.epos.eposdatamodel.DataProduct dp = new org.epos.eposdatamodel.DataProduct();
        dp.setDescription(Collections.singletonList("Test Description"));
        dp.setIssued(OffsetDateTime.now().toLocalDateTime());
        dp.setModified(OffsetDateTime.now().toLocalDateTime());
        return dp;
    }

    private org.epos.eposdatamodel.Distribution createDummyDistribution() {
        org.epos.eposdatamodel.Distribution dist = new org.epos.eposdatamodel.Distribution();
        dist.setFormat("application/json");
        dist.setIssued(OffsetDateTime.now().toLocalDateTime());
        dist.setModified(OffsetDateTime.now().toLocalDateTime());
        return dist;
    }
}
