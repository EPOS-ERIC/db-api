package utilities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MemoryMonitorTest {

    @Test
    void snapshotContainsCoreFields() {
        String snapshot = MemoryMonitor.snapshot();

        assertTrue(snapshot.contains("heapUsed="));
        assertTrue(snapshot.contains("heapCommitted="));
        assertTrue(snapshot.contains("gcCount="));
        assertTrue(snapshot.contains("reflectionCache="));
    }
}
