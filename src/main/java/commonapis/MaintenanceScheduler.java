package commonapis;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Explicitly controlled scheduler for maintenance tasks owned by the host application. */
public final class MaintenanceScheduler {
    private static final AtomicReference<ScheduledExecutorService> EXECUTOR = new AtomicReference<>();
    private static volatile Duration interval = Duration.ofMinutes(10);
    private static volatile Runnable task;

    private MaintenanceScheduler() {}

    public static synchronized void start(Duration requestedInterval, Runnable maintenanceTask) {
        Objects.requireNonNull(maintenanceTask, "maintenanceTask");
        if (EXECUTOR.get() != null) return;
        interval = requestedInterval == null || requestedInterval.isZero() || requestedInterval.isNegative()
                ? Duration.ofMinutes(10) : requestedInterval;
        task = maintenanceTask;
        ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "metadata-maintenance");
            thread.setDaemon(true);
            return thread;
        });
        EXECUTOR.set(executor);
        executor.scheduleWithFixedDelay(MaintenanceScheduler::runSafely,
                interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    public static synchronized void stop() {
        ScheduledExecutorService executor = EXECUTOR.getAndSet(null);
        task = null;
        if (executor != null) executor.shutdownNow();
    }

    public static void runNow() {
        if (task != null) runSafely();
    }

    public static boolean isRunning() {
        return EXECUTOR.get() != null;
    }

    public static Duration getInterval() {
        return interval;
    }

    private static void runSafely() {
        try {
            Runnable currentTask = task;
            if (currentTask != null) currentTask.run();
        } catch (Throwable ignored) {
            // A failed maintenance pass must not terminate future scheduled passes.
        }
    }
}
