package integrationtests.performance;

import abstractapis.AbstractAPI;
import integrationtests.TestcontainersLifecycle;
import metadataapis.DataProductAPI;
import metadataapis.EntityNames;
import metadataapis.SoftwareApplicationAPI;
import metadataapis.WebServiceAPI;
import org.epos.eposdatamodel.DataProduct;
import org.epos.eposdatamodel.SoftwareApplication;
import org.epos.eposdatamodel.WebService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Manual regression benchmark for the bulk-reader path. Run explicitly with
 * -DrunPerformanceTests=true; it is excluded from ordinary test execution.
 */
@EnabledIfSystemProperty(named = "runPerformanceTests", matches = "true")
class BulkReadPerformanceTest extends TestcontainersLifecycle {

    private static final int[] SIZES = {100, 500, 1_000};
    private static final int MEASUREMENTS = 5;

    @Test
    void dataProductBulkReadScalesWithDatasetSize() {
        DataProductAPI api = (DataProductAPI) AbstractAPI.retrieveAPI(EntityNames.DATAPRODUCT.name());
        int created = 0;

        for (int size : SIZES) {
            while (created < size) {
                DataProduct dataProduct = new DataProduct();
                dataProduct.setUid("performance/dataproduct/" + UUID.randomUUID());
                dataProduct.setType("Dataset");
                dataProduct.addTitle("benchmark");
                api.create(dataProduct, null, null, null);
                created++;
            }

            long[] timings = medianMs(api::retrieveAll, api::retrieveAllSummary, size);
            System.out.println("bulk-read dataproduct size=" + size + " medianMs=" + timings[0]);
            System.out.println("summary-read dataproduct size=" + size + " medianMs=" + timings[1]);
        }
    }

    @Test
    void softwareApplicationSummaryReadScalesWithDatasetSize() {
        SoftwareApplicationAPI api = (SoftwareApplicationAPI) AbstractAPI.retrieveAPI(EntityNames.SOFTWAREAPPLICATION.name());
        int created = 0;

        for (int size : SIZES) {
            while (created < size) {
                SoftwareApplication application = new SoftwareApplication();
                application.setUid("performance/software-application/" + UUID.randomUUID());
                application.setName("benchmark");
                api.create(application, null, null, null);
                created++;
            }

            long[] timings = medianMs(api::retrieveAll, api::retrieveAllSummary, size);
            System.out.println("bulk-read software-application size=" + size + " medianMs=" + timings[0]);
            System.out.println("summary-read software-application size=" + size + " medianMs=" + timings[1]);
        }
    }

    @Test
    void webServiceSummaryReadScalesWithDatasetSize() {
        WebServiceAPI api = (WebServiceAPI) AbstractAPI.retrieveAPI(EntityNames.WEBSERVICE.name());
        int created = 0;

        for (int size : SIZES) {
            while (created < size) {
                WebService webService = new WebService();
                webService.setUid("performance/web-service/" + UUID.randomUUID());
                webService.setName("benchmark");
                api.create(webService, null, null, null);
                created++;
            }

            long[] timings = medianMs(api::retrieveAll, api::retrieveAllSummary, size);
            System.out.println("bulk-read web-service size=" + size + " medianMs=" + timings[0]);
            System.out.println("summary-read web-service size=" + size + " medianMs=" + timings[1]);
        }
    }

    private long[] medianMs(Supplier<List<?>> fullReader, Supplier<List<?>> summaryReader, int expectedSize) {
        // Keep class loading and connection setup out of the measured samples.
        assertEquals(expectedSize, fullReader.get().size());
        assertEquals(expectedSize, summaryReader.get().size());

        long[] fullSamples = new long[MEASUREMENTS];
        long[] summarySamples = new long[MEASUREMENTS];
        for (int i = 0; i < MEASUREMENTS; i++) {
            long startedAt = System.nanoTime();
            List<?> results = (i & 1) == 0 ? fullReader.get() : summaryReader.get();
            long elapsedMs = (System.nanoTime() - startedAt) / 1_000_000;
            assertEquals(expectedSize, results.size());
            if ((i & 1) == 0) {
                fullSamples[i] = elapsedMs;
                startedAt = System.nanoTime();
                results = summaryReader.get();
                summarySamples[i] = (System.nanoTime() - startedAt) / 1_000_000;
            } else {
                summarySamples[i] = elapsedMs;
                startedAt = System.nanoTime();
                results = fullReader.get();
                fullSamples[i] = (System.nanoTime() - startedAt) / 1_000_000;
            }
            assertEquals(expectedSize, results.size());
        }
        Arrays.sort(fullSamples);
        Arrays.sort(summarySamples);
        return new long[]{fullSamples[MEASUREMENTS / 2], summarySamples[MEASUREMENTS / 2]};
    }
}
