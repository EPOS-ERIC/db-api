package utilities;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.util.List;
import java.util.Locale;

/**
 * Builds compact runtime memory snapshots for application logs.
 */
public final class MemoryMonitor {

    private MemoryMonitor() {
    }

    public static String snapshot() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();

        long gcCount = 0;
        long gcTimeMs = 0;
        List<GarbageCollectorMXBean> gcBeans = ManagementFactory.getGarbageCollectorMXBeans();
        for (GarbageCollectorMXBean gcBean : gcBeans) {
            long count = gcBean.getCollectionCount();
            if (count >= 0) {
                gcCount += count;
            }
            long time = gcBean.getCollectionTime();
            if (time >= 0) {
                gcTimeMs += time;
            }
        }

        return String.format(Locale.ROOT,
                "heapUsed=%s heapCommitted=%s heapMax=%s nonHeapUsed=%s gcCount=%d gcTimeMs=%d reflectionCache=%d",
                formatBytes(heap.getUsed()),
                formatBytes(heap.getCommitted()),
                formatBytes(heap.getMax()),
                formatBytes(nonHeap.getUsed()),
                gcCount,
                gcTimeMs,
                ReflectionCache.getCacheSize());
    }

    private static String formatBytes(long bytes) {
        if (bytes < 0) {
            return "n/a";
        }
        double mb = bytes / (1024.0 * 1024.0);
        return String.format(Locale.ROOT, "%.1fMB", mb);
    }
}
