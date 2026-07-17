package integrationtests.unittests;

import abstractapis.AbstractAPI;
import dao.EposDataModelDAO;
import integrationtests.TestcontainersLifecycle;
import metadataapis.EntityNames;
import model.StatusType;
import model.Versioningstatus;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.LinkedEntity;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class VersionLifecycleSelectionTest extends TestcontainersLifecycle {

    private final AbstractAPI dataProductApi = AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());

    @Test
    void createsPublishedVersion() {
        DataProduct published = createPublished();

        assertEquals(StatusType.PUBLISHED, published.getStatus());
    }

    @Test
    void createsDraftFromPublishedVersion() {
        DataProduct published = createPublished();
        DataProduct draft = createDraftFrom(published);

        assertNotEquals(published.getInstanceId(), draft.getInstanceId());
        assertEquals(published.getMetaId(), draft.getMetaId());
        assertEquals(StatusType.PUBLISHED, retrieve(published).getStatus());
        assertEquals(StatusType.DRAFT, draft.getStatus());
    }

    @Test
    void submitsTheExactDraftInsteadOfPublishedSibling() {
        DataProduct published = createPublished();
        DataProduct draft = createDraftFrom(published);

        draft.setStatus(StatusType.SUBMITTED);
        LinkedEntity submittedLink = dataProductApi.create(draft, null, null, null);
        DataProduct submitted = retrieve(submittedLink);

        assertEquals(draft.getInstanceId(), submitted.getInstanceId());
        assertEquals(StatusType.SUBMITTED, submitted.getStatus());
        assertEquals(StatusType.PUBLISHED, retrieve(published).getStatus());
        assertPersistedVersions(published.getUid(), published.getInstanceId(), draft.getInstanceId(), StatusType.SUBMITTED);
    }

    @Test
    void publishesTheExactSubmittedDraftAndArchivesPreviousPublished() {
        DataProduct published = createPublished();
        DataProduct draft = createDraftFrom(published);
        draft.setStatus(StatusType.SUBMITTED);
        DataProduct submitted = retrieve(dataProductApi.create(draft, null, null, null));

        submitted.setStatus(StatusType.PUBLISHED);
        DataProduct republished = retrieve(dataProductApi.create(submitted, null, null, null));

        assertEquals(submitted.getInstanceId(), republished.getInstanceId());
        assertEquals(StatusType.PUBLISHED, republished.getStatus());
        assertEquals(StatusType.ARCHIVED, retrieve(published).getStatus());
        assertPersistedVersions(published.getUid(), published.getInstanceId(), submitted.getInstanceId(), StatusType.PUBLISHED);
    }

    @Test
    void archivesTheExactPublishedVersion() {
        DataProduct published = createPublished();
        published.setStatus(StatusType.ARCHIVED);

        DataProduct archived = retrieve(dataProductApi.create(published, null, null, null));

        assertEquals(published.getInstanceId(), archived.getInstanceId());
        assertEquals(StatusType.ARCHIVED, archived.getStatus());
    }

    private DataProduct createPublished() {
        DataProduct dataProduct = new DataProduct();
        dataProduct.setUid("dataproduct/lifecycle/" + UUID.randomUUID());
        dataProduct.setTitle(List.of("Lifecycle test"));
        return retrieve(dataProductApi.create(dataProduct, StatusType.PUBLISHED, null, null));
    }

    private DataProduct createDraftFrom(DataProduct published) {
        DataProduct draftRequest = retrieve(published);
        draftRequest.setStatus(StatusType.DRAFT);
        return retrieve(dataProductApi.create(draftRequest, null, null, null));
    }

    private DataProduct retrieve(LinkedEntity link) {
        return (DataProduct) dataProductApi.retrieve(link.getInstanceId());
    }

    private DataProduct retrieve(DataProduct dataProduct) {
        return (DataProduct) dataProductApi.retrieve(dataProduct.getInstanceId());
    }

    private void assertPersistedVersions(String uid, String previousPublishedInstanceId,
                                         String transitionedInstanceId, StatusType transitionedStatus) {
        List<Versioningstatus> versions = EposDataModelDAO.getInstance()
                .getOneFromDBByUIDNoCache(uid, Versioningstatus.class);

        assertEquals(2, versions.size());
        assertEquals(1, versions.stream()
                .filter(version -> transitionedInstanceId.equals(version.getInstanceId()))
                .filter(version -> transitionedStatus.name().equals(version.getStatus()))
                .count());
        assertEquals(1, versions.stream()
                .filter(version -> previousPublishedInstanceId.equals(version.getInstanceId()))
                .count());
    }
}
