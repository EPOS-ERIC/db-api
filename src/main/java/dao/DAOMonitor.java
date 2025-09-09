package dao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Advanced monitoring and alerting system for EPOS DAO performance
 *
 * Features:
 * - Real-time performance metrics collection
 * - Threshold-based alerting
 * - Performance degradation detection
 * - Automated reporting
 * - SLA monitoring
 * - Predictive analytics
 */
public class DAOMonitor {

    private static final Logger LOG = Logger.getLogger(DAOMonitor.class.getName());

    // Metrics collection
    private static final Map<String, AtomicLong> operationCounts = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> operationTotalTime = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> operationErrors = new ConcurrentHashMap<>();
    private static final Queue<PerformanceSnapshot> performanceHistory = new ConcurrentLinkedQueue<>();

    // Thresholds and configuration
    private static final long SLOW_OPERATION_THRESHOLD_MS = 1000;
    private static final long ERROR_RATE_THRESHOLD_PERCENT = 5;
    private static final int MAX_HISTORY_SIZE = 1000;

    // Alerting
    private static final List<AlertListener> alertListeners = new ArrayList<>();
    private static volatile boolean monitoringEnabled = true;

    // =================== PERFORMANCE METRICS COLLECTION ===================

    /**
     * Performance snapshot for trend analysis
     */
    public static class PerformanceSnapshot {
        private final LocalDateTime timestamp;
        private final Map<String, Double> avgResponseTimes;
        private final Map<String, Long> operationCounts;
        private final Map<String, Double> errorRates;
        private final Map<String, Object> cacheStats;

        public PerformanceSnapshot() {
            this.timestamp = LocalDateTime.now();
            this.avgResponseTimes = calculateAverageResponseTimes();
            this.operationCounts = getCurrentOperationCounts();
            this.errorRates = calculateErrorRates();
            this.cacheStats = EposDataModelDAO.getInstance().getDetailedCacheStats();
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public Map<String, Double> getAvgResponseTimes() { return avgResponseTimes; }
        public Map<String, Long> getOperationCounts() { return operationCounts; }
        public Map<String, Double> getErrorRates() { return errorRates; }
        public Map<String, Object> getCacheStats() { return cacheStats; }
    }

    /**
     * Alert interface for custom alerting implementations
     */
    public interface AlertListener {
        void onAlert(AlertType type, String message, Map<String, Object> context);
    }

    /**
     * Alert types for different monitoring scenarios
     */
    public enum AlertType {
        PERFORMANCE_DEGRADATION,
        HIGH_ERROR_RATE,
        CACHE_MISS_SPIKE,
        CONNECTION_POOL_EXHAUSTION,
        MEMORY_LEAK_DETECTED,
        SLA_VIOLATION
    }

    // =================== MONITORING METHODS ===================

    /**
     * Records operation execution for monitoring
     */
    public static void recordOperation(String operation, long durationMs, boolean success) {
        if (!monitoringEnabled) return;

        try {
            // Update counters
            operationCounts.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
            operationTotalTime.computeIfAbsent(operation, k -> new AtomicLong(0)).addAndGet(durationMs);

            if (!success) {
                operationErrors.computeIfAbsent(operation, k -> new AtomicLong(0)).incrementAndGet();
            }

            // Check for performance issues
            if (durationMs > SLOW_OPERATION_THRESHOLD_MS) {
                triggerAlert(AlertType.PERFORMANCE_DEGRADATION,
                        "Slow operation detected: " + operation + " took " + durationMs + "ms",
                        Map.of("operation", operation, "duration", durationMs, "threshold", SLOW_OPERATION_THRESHOLD_MS));
            }

            // Check error rates
            checkErrorRateThreshold(operation);

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error recording operation metrics", e);
        }
    }

    /**
     * Takes a performance snapshot for trend analysis
     */
    public static void takePerformanceSnapshot() {
        try {
            PerformanceSnapshot snapshot = new PerformanceSnapshot();

            // Add to history
            performanceHistory.offer(snapshot);

            // Maintain history size
            while (performanceHistory.size() > MAX_HISTORY_SIZE) {
                performanceHistory.poll();
            }

            // Analyze trends
            analyzeTrends();

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error taking performance snapshot", e);
        }
    }

    /**
     * Checks if error rate exceeds threshold
     */
    private static void checkErrorRateThreshold(String operation) {
        long totalOps = operationCounts.getOrDefault(operation, new AtomicLong(0)).get();
        long errorOps = operationErrors.getOrDefault(operation, new AtomicLong(0)).get();

        if (totalOps > 100) { // Only check after sufficient sample size
            double errorRate = (errorOps * 100.0) / totalOps;

            if (errorRate > ERROR_RATE_THRESHOLD_PERCENT) {
                triggerAlert(AlertType.HIGH_ERROR_RATE,
                        "High error rate detected: " + operation + " has " + String.format("%.2f", errorRate) + "% errors",
                        Map.of("operation", operation, "errorRate", errorRate, "threshold", ERROR_RATE_THRESHOLD_PERCENT));
            }
        }
    }

    /**
     * Analyzes performance trends for predictive alerting
     */
    private static void analyzeTrends() {
        if (performanceHistory.size() < 10) return; // Need minimum data points

        try {
            // Analyze cache hit rate trends
            analyzeCacheHitRateTrend();

            // Analyze response time trends
            analyzeResponseTimeTrends();

            // Analyze memory usage trends
            analyzeMemoryTrends();

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Error analyzing performance trends", e);
        }
    }

    /**
     * Analyzes cache hit rate trends
     */
    private static void analyzeCacheHitRateTrend() {
        List<PerformanceSnapshot> recent = new ArrayList<>(performanceHistory);
        if (recent.size() < 5) return;

        // Get last 5 snapshots
        List<PerformanceSnapshot> lastFive = recent.subList(Math.max(0, recent.size() - 5), recent.size());

        // Calculate average hit rates
        double avgHitRate = lastFive.stream()
                .mapToDouble(snapshot -> {
                    Map<String, Object> cacheStats = snapshot.getCacheStats();
                    Map<String, Object> queryCache = (Map<String, Object>) cacheStats.get("queryCache");
                    return queryCache != null ? (Double) queryCache.getOrDefault("hitRate", 0.0) : 0.0;
                })
                .average()
                .orElse(0.0);

        // Alert if hit rate drops significantly
        if (avgHitRate < 50.0) {
            triggerAlert(AlertType.CACHE_MISS_SPIKE,
                    "Cache hit rate dropped to " + String.format("%.2f", avgHitRate) + "%",
                    Map.of("hitRate", avgHitRate, "threshold", 50.0));
        }
    }

    /**
     * Analyzes response time trends for degradation detection
     */
    private static void analyzeResponseTimeTrends() {
        List<PerformanceSnapshot> recent = new ArrayList<>(performanceHistory);
        if (recent.size() < 10) return;

        // Compare current average with historical baseline
        PerformanceSnapshot current = recent.get(recent.size() - 1);
        List<PerformanceSnapshot> baseline = recent.subList(0, Math.min(recent.size() - 5, 5));

        for (String operation : current.getAvgResponseTimes().keySet()) {
            double currentAvg = current.getAvgResponseTimes().get(operation);

            double baselineAvg = baseline.stream()
                    .mapToDouble(snapshot -> snapshot.getAvgResponseTimes().getOrDefault(operation, 0.0))
                    .filter(time -> time > 0)
                    .average()
                    .orElse(currentAvg);

            // Alert if current performance is significantly worse than baseline
            if (currentAvg > baselineAvg * 2.0 && currentAvg > 100) {
                triggerAlert(AlertType.PERFORMANCE_DEGRADATION,
                        "Performance degradation detected for " + operation +
                                ": current=" + String.format("%.2f", currentAvg) + "ms, baseline=" + String.format("%.2f", baselineAvg) + "ms",
                        Map.of("operation", operation, "currentAvg", currentAvg, "baselineAvg", baselineAvg));
            }
        }
    }

    /**
     * Analyzes memory usage trends for leak detection
     */
    private static void analyzeMemoryTrends() {
        List<PerformanceSnapshot> recent = new ArrayList<>(performanceHistory);
        if (recent.size() < 20) return;

        // Check if cache sizes are growing consistently
        List<Long> cacheSizes = recent.stream()
                .map(snapshot -> {
                    Map<String, Object> cacheStats = snapshot.getCacheStats();
                    Long querySize = (Long) cacheStats.getOrDefault("queryCacheSize", 0L);
                    Long entitySize = (Long) cacheStats.getOrDefault("entityCacheSize", 0L);
                    return querySize + entitySize;
                })
                .collect(java.util.stream.Collectors.toList());

        // Simple trend detection: check if last 10 values are consistently increasing
        boolean consistentGrowth = true;
        for (int i = cacheSizes.size() - 10; i < cacheSizes.size() - 1; i++) {
            if (i < 0) continue;
            if (cacheSizes.get(i + 1) <= cacheSizes.get(i)) {
                consistentGrowth = false;
                break;
            }
        }

        if (consistentGrowth && cacheSizes.get(cacheSizes.size() - 1) > 50000) {
            triggerAlert(AlertType.MEMORY_LEAK_DETECTED,
                    "Potential memory leak detected: cache size growing consistently to " + cacheSizes.get(cacheSizes.size() - 1) + " entries",
                    Map.of("currentSize", cacheSizes.get(cacheSizes.size() - 1), "trend", "increasing"));
        }
    }

    // =================== ALERTING SYSTEM ===================

    /**
     * Triggers an alert to all registered listeners
     */
    private static void triggerAlert(AlertType type, String message, Map<String, Object> context) {
        try {
            LOG.warning("ALERT [" + type + "]: " + message);

            for (AlertListener listener : alertListeners) {
                try {
                    listener.onAlert(type, message, context);
                } catch (Exception e) {
                    LOG.log(Level.WARNING, "Error notifying alert listener", e);
                }
            }
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error triggering alert", e);
        }
    }

    /**
     * Registers an alert listener
     */
    public static void addAlertListener(AlertListener listener) {
        alertListeners.add(listener);
    }

    /**
     * Default console alert listener
     */
    public static class ConsoleAlertListener implements AlertListener {
        @Override
        public void onAlert(AlertType type, String message, Map<String, Object> context) {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            System.err.println("ALERT [" + timestamp + "] " + type + ": " + message);
            if (!context.isEmpty()) {
                System.err.println("Context: " + context);
            }
        }
    }

    /**
     * Email alert listener (example implementation)
     */
    public static class EmailAlertListener implements AlertListener {
        private final String recipientEmail;

        public EmailAlertListener(String recipientEmail) {
            this.recipientEmail = recipientEmail;
        }

        @Override
        public void onAlert(AlertType type, String message, Map<String, Object> context) {
            // Implement email sending logic here
            //LOG.info("Would send email alert to " + recipientEmail + ": " + message);

            // Example: Use JavaMail or your preferred email service
            // sendEmail(recipientEmail, "EPOS DAO Alert: " + type, message + "\n\nContext: " + context);
        }
    }

    // =================== REPORTING AND ANALYTICS ===================

    /**
     * Generates comprehensive performance report
     */
    public static String generatePerformanceReport() {
        StringBuilder report = new StringBuilder();
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        report.append("=== EPOS DAO PERFORMANCE REPORT ===\n");
        report.append("Generated: ").append(timestamp).append("\n\n");

        // Operation statistics
        report.append("OPERATION STATISTICS:\n");
        operationCounts.forEach((operation, count) -> {
            long totalTime = operationTotalTime.getOrDefault(operation, new AtomicLong(0)).get();
            long errors = operationErrors.getOrDefault(operation, new AtomicLong(0)).get();

            double avgTime = count.get() > 0 ? (double) totalTime / count.get() : 0;
            double errorRate = count.get() > 0 ? (errors * 100.0) / count.get() : 0;

            report.append(String.format("  %s: %d calls, %.2fms avg, %.2f%% errors\n",
                    operation, count.get(), avgTime, errorRate));
        });

        // Cache statistics
        report.append("\nCACHE STATISTICS:\n");
        Map<String, Object> cacheStats = EposDataModelDAO.getInstance().getDetailedCacheStats();
        cacheStats.forEach((key, value) -> {
            if (value instanceof Map) {
                report.append("  ").append(key).append(":\n");
                ((Map<?, ?>) value).forEach((subKey, subValue) ->
                        report.append("    ").append(subKey).append(": ").append(subValue).append("\n"));
            } else {
                report.append("  ").append(key).append(": ").append(value).append("\n");
            }
        });

        // Recent alerts
        report.append("\nRECENT PERFORMANCE TRENDS:\n");
        if (performanceHistory.size() >= 2) {
            List<PerformanceSnapshot> recent = new ArrayList<>(performanceHistory);
            PerformanceSnapshot latest = recent.get(recent.size() - 1);
            PerformanceSnapshot previous = recent.get(recent.size() - 2);

            // Compare cache hit rates
            Map<String, Object> latestCache = latest.getCacheStats();
            Map<String, Object> previousCache = previous.getCacheStats();

            Map<String, Object> latestQuery = (Map<String, Object>) latestCache.get("queryCache");
            Map<String, Object> previousQuery = (Map<String, Object>) previousCache.get("queryCache");

            if (latestQuery != null && previousQuery != null) {
                double latestHitRate = (Double) latestQuery.getOrDefault("hitRate", 0.0);
                double previousHitRate = (Double) previousQuery.getOrDefault("hitRate", 0.0);
                double hitRateChange = latestHitRate - previousHitRate;

                report.append(String.format("  Cache hit rate: %.2f%% (%+.2f%% change)\n",
                        latestHitRate, hitRateChange));
            }
        }

        report.append("\n===============================");

        return report.toString();
    }

    /**
     * Exports performance data for external analysis
     */
    public static Map<String, Object> exportPerformanceData() {
        Map<String, Object> data = new HashMap<>();

        // Current metrics
        data.put("operationCounts", getCurrentOperationCounts());
        data.put("averageResponseTimes", calculateAverageResponseTimes());
        data.put("errorRates", calculateErrorRates());
        data.put("cacheStatistics", EposDataModelDAO.getInstance().getDetailedCacheStats());

        // Historical data
        data.put("performanceHistory", new ArrayList<>(performanceHistory));
        data.put("monitoringEnabled", monitoringEnabled);
        data.put("exportTimestamp", LocalDateTime.now());

        return data;
    }

    // =================== SLA MONITORING ===================

    /**
     * SLA configuration and monitoring
     */
    public static class SLAMonitor {
        private static final Map<String, SLATarget> slaTargets = new HashMap<>();

        public static class SLATarget {
            private final String operation;
            private final double maxAvgResponseTimeMs;
            private final double maxErrorRatePercent;
            private final double minAvailabilityPercent;

            public SLATarget(String operation, double maxAvgResponseTimeMs,
                             double maxErrorRatePercent, double minAvailabilityPercent) {
                this.operation = operation;
                this.maxAvgResponseTimeMs = maxAvgResponseTimeMs;
                this.maxErrorRatePercent = maxErrorRatePercent;
                this.minAvailabilityPercent = minAvailabilityPercent;
            }

            // Getters
            public String getOperation() { return operation; }
            public double getMaxAvgResponseTimeMs() { return maxAvgResponseTimeMs; }
            public double getMaxErrorRatePercent() { return maxErrorRatePercent; }
        }

        /**
         * Defines SLA targets for operations
         */
        public static void defineSLA(String operation, double maxAvgResponseTimeMs,
                                     double maxErrorRatePercent, double minAvailabilityPercent) {
            slaTargets.put(operation, new SLATarget(operation, maxAvgResponseTimeMs,
                    maxErrorRatePercent, minAvailabilityPercent));
        }

        /**
         * Checks SLA compliance and triggers alerts if violated
         */
        public static void checkSLACompliance() {
            Map<String, Double> avgResponseTimes = calculateAverageResponseTimes();
            Map<String, Double> errorRates = calculateErrorRates();

            for (SLATarget target : slaTargets.values()) {
                String operation = target.getOperation();

                // Check response time SLA
                double avgResponseTime = avgResponseTimes.getOrDefault(operation, 0.0);
                if (avgResponseTime > target.getMaxAvgResponseTimeMs()) {
                    triggerAlert(AlertType.SLA_VIOLATION,
                            "SLA violation: " + operation + " response time " +
                                    String.format("%.2f", avgResponseTime) + "ms exceeds target " +
                                    String.format("%.2f", target.getMaxAvgResponseTimeMs()) + "ms",
                            Map.of("operation", operation, "actual", avgResponseTime,
                                    "target", target.getMaxAvgResponseTimeMs(), "metric", "responseTime"));
                }

                // Check error rate SLA
                double errorRate = errorRates.getOrDefault(operation, 0.0);
                if (errorRate > target.getMaxErrorRatePercent()) {
                    triggerAlert(AlertType.SLA_VIOLATION,
                            "SLA violation: " + operation + " error rate " +
                                    String.format("%.2f", errorRate) + "% exceeds target " +
                                    String.format("%.2f", target.getMaxErrorRatePercent()) + "%",
                            Map.of("operation", operation, "actual", errorRate,
                                    "target", target.getMaxErrorRatePercent(), "metric", "errorRate"));
                }
            }
        }
    }

    // =================== UTILITY METHODS ===================

    private static Map<String, Long> getCurrentOperationCounts() {
        Map<String, Long> counts = new HashMap<>();
        operationCounts.forEach((op, count) -> counts.put(op, count.get()));
        return counts;
    }

    private static Map<String, Double> calculateAverageResponseTimes() {
        Map<String, Double> avgTimes = new HashMap<>();
        operationCounts.forEach((operation, count) -> {
            long totalTime = operationTotalTime.getOrDefault(operation, new AtomicLong(0)).get();
            double avgTime = count.get() > 0 ? (double) totalTime / count.get() : 0;
            avgTimes.put(operation, avgTime);
        });
        return avgTimes;
    }

    private static Map<String, Double> calculateErrorRates() {
        Map<String, Double> errorRates = new HashMap<>();
        operationCounts.forEach((operation, count) -> {
            long errors = operationErrors.getOrDefault(operation, new AtomicLong(0)).get();
            double errorRate = count.get() > 0 ? (errors * 100.0) / count.get() : 0;
            errorRates.put(operation, errorRate);
        });
        return errorRates;
    }

    // =================== CONTROL METHODS ===================

    /**
     * Enables or disables monitoring
     */
    public static void setMonitoringEnabled(boolean enabled) {
        monitoringEnabled = enabled;
        //LOG.info("Monitoring " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Resets all collected metrics
     */
    public static void resetMetrics() {
        operationCounts.clear();
        operationTotalTime.clear();
        operationErrors.clear();
        performanceHistory.clear();
        //LOG.info("All metrics reset");
    }

    /**
     * Initializes monitoring with default configuration
     */
    public static void initialize() {
        // Add default console alert listener
        addAlertListener(new ConsoleAlertListener());

        // Define default SLAs
        SLAMonitor.defineSLA("createObject", 500, 1.0, 99.9);
        SLAMonitor.defineSLA("getOneFromDBByInstanceId", 100, 0.5, 99.95);
        SLAMonitor.defineSLA("getAllFromDB", 1000, 2.0, 99.0);

        // Start periodic SLA checks
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            try {
                takePerformanceSnapshot();
                SLAMonitor.checkSLACompliance();
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Error in periodic monitoring", e);
            }
        }, 1, 5, TimeUnit.MINUTES);

        LOG.info("Advanced DAO monitoring initialized");
    }
}
