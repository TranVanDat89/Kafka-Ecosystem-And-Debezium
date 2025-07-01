package com.dattran.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceMonitor {
    private final AtomicLong processedRecords = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);
    private final long startTime;
    private final ScheduledExecutorService scheduler;

    public PerformanceMonitor() {
        this.startTime = System.currentTimeMillis();
        this.scheduler = Executors.newScheduledThreadPool(1);
        startMonitoring();
    }

    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            long processed = processedRecords.get();
            long errors = errorCount.get();
            long elapsed = System.currentTimeMillis() - startTime;
            if (processed > 0) {
                double rate = (double) processed / (elapsed / 1000.0);
                double errorRate = (double) errors / processed * 100;
                System.out.printf("[MONITOR] Processed: %,d | Rate: %.2f/sec | Errors: %,d (%.2f%%) | Elapsed: %s%n",
                        processed, rate, errors, errorRate, formatDuration(elapsed));
                // Memory monitoring
                Runtime runtime = Runtime.getRuntime();
                long totalMemory = runtime.totalMemory();
                long freeMemory = runtime.freeMemory();
                long usedMemory = totalMemory - freeMemory;
                double memoryUsage = (double) usedMemory / totalMemory * 100;
                System.out.printf("[MEMORY] Used: %s / %s (%.1f%%)%n",
                        formatBytes(usedMemory), formatBytes(totalMemory), memoryUsage);
                if (memoryUsage > 85) {
                    System.out.println("[WARNING] High memory usage detected!");
                    System.gc(); // Suggest garbage collection
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    public void recordProcessed(long count) {
        processedRecords.addAndGet(count);
    }

    public void recordError() {
        errorCount.incrementAndGet();
    }

    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }

        // Print final statistics
        long elapsed = System.currentTimeMillis() - startTime;
        long processed = processedRecords.get();
        long errors = errorCount.get();

        System.out.println("\n=== FINAL STATISTICS ===");
        System.out.printf("Total Processed: %,d records%n", processed);
        System.out.printf("Total Errors: %,d records%n", errors);
        System.out.printf("Success Rate: %.2f%%%n", (double)(processed - errors) / processed * 100);
        System.out.printf("Average Rate: %.2f records/sec%n", (double) processed / (elapsed / 1000.0));
        System.out.printf("Total Time: %s%n", formatDuration(elapsed));
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;

        if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        return String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024));
    }
}
