package integrationtests.unittests;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import jakarta.persistence.EntityManager;
import metadataapis.EntityNames;
import model.StatusType;
import model.Webservice;
import org.epos.eposdatamodel.Distribution;
import org.epos.eposdatamodel.LinkedEntity;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NewRelationsTest  extends TestcontainersLifecycle {

    @Test
    @Order(1)
    public void testRandomicInsert() {

        LinkedEntity le = new LinkedEntity();
        le.setUid("testws");
        le.setEntityType(EntityNames.WEBSERVICE.name());

        Distribution distribution = new Distribution();
        distribution.setUid("test");
        distribution.setEditorId("ingestor");
        distribution.setFileProvenance("prov1");
        distribution.addAccessService(le);

        WebService webservice = new WebService();
        webservice.setUid("testws");
        webservice.setEditorId("ingestor");
        webservice.setFileProvenance("prov1");

        AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(distribution,null,null,null);

        // ========== DEBUG: Check dopo create Distribution ==========
        System.out.println("[DEBUG TEST] After Distribution.create(), checking WebserviceDistribution...");
        EntityManager emTest = org.epos.handler.dbapi.service.EntityManagerService.getInstance().createEntityManager();
        jakarta.persistence.EntityTransaction txTest = emTest.getTransaction();
        try {
            txTest.begin();
            Long count = emTest.createQuery("SELECT COUNT(c) FROM WebserviceDistribution c", Long.class)
                    .setHint("eclipselink.refresh", true)
                    .getSingleResult();
            System.out.println("[DEBUG TEST] WebserviceDistribution count after Distribution.create(): " + count);
            txTest.commit();
        } finally {
            if (txTest.isActive()) txTest.rollback();
            emTest.close();
        }

        AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).create(webservice,null,null,null);

        // ========== DEBUG: Check DOPO WebserviceAPI.create() ==========
        System.out.println("[DEBUG TEST] After WebService.create(), checking WebserviceDistribution...");
        EntityManager emTest2 = org.epos.handler.dbapi.service.EntityManagerService.getInstance().createEntityManager();
        jakarta.persistence.EntityTransaction txTest2 = emTest2.getTransaction();
        try {
            txTest2.begin();
            Long count = emTest2.createQuery("SELECT COUNT(c) FROM WebserviceDistribution c", Long.class)
                    .setHint("eclipselink.refresh", true)
                    .getSingleResult();
            System.out.println("[DEBUG TEST] WebserviceDistribution count after WebService.create(): " + count);
            txTest2.commit();
        } finally {
            if (txTest2.isActive()) txTest2.rollback();
            emTest2.close();
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieveAll()){
            System.out.println(object);
        }

        for(Object object : AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name()).retrieveAll()){
            System.out.println(object);
        }

    }

    @Test
    @Order(2)
    public void testPublishedToDraft() {
        Distribution dist = new Distribution();
        dist.setUid("test-versioning");
        dist.setEditorId("ingestor");
        dist.addAccessService(new LinkedEntity().uid("testws").entityType("WEBSERVICE"));

        AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(dist, StatusType.PUBLISHED, null, null);

        Distribution draftRequest = new Distribution();
        draftRequest.setUid("test-versioning");  // Solo UID!
        draftRequest.setStatus(StatusType.DRAFT);
        draftRequest.setEditorId("ingestor");

        LinkedEntity result = AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).create(draftRequest, null, null, null);

        Distribution draft = (Distribution) AbstractAPI.retrieveAPI(EntityNames.DISTRIBUTION.name()).retrieve(result.getInstanceId());

        assertNotNull(draft.getAccessService());
        assertFalse(draft.getAccessService().isEmpty());
        assertEquals("testws", draft.getAccessService().get(0).getUid());
    }
}
