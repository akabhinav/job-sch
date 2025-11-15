package com.enterprise.scheduler.monitoring.analytics;

import com.enterprise.scheduler.monitoring.metrics.CustomJobMetricsCollector;
import com.enterprise.scheduler.monitoring.metrics.JobExecutionMetricsCollector;
import com.enterprise.scheduler.monitoring.metrics.SystemMetricsCollector;
import com.enterprise.scheduler.monitoring.model.PerformanceSnapshot;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

/**
 * Service for aggregating and analyzing performance metrics
 * Feature #27: Performance Analytics
 * Feature #28: Real-time Metrics Aggregation
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerformanceAnalyticsService {

    private final JobExecutionMetricsCollector jobExecutionMetrics;
    private final SystemMetricsCollector systemMetrics;
    private final CustomJobMetricsCollector customMetrics;
    private final MeterRegistry meterRegistry;

    // Store recent snapshots (last 100)
    private final Deque<PerformanceSnapshot> recentSnapshots = new ConcurrentLinkedDeque<>();
    private static final int MAX_SNAPSHOTS = 100;

    private final MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
    private final OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();

    /**
     * Collect performance snapshot periodically
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void collectPerformanceSnapshot() {
        try {
            PerformanceSnapshot snapshot = createSnapshot();
            addSnapshot(snapshot);
            log.debug("Performance snapshot collected: heap={}%, threads={}, executions={}",
                    snapshot.getSystemMetrics().getHeapUsagePercent(),
                    snapshot.getSystemMetrics().getThreadCount(),
                    snapshot.getExecutionMetrics().getTotalExecutions());
        } catch (Exception e) {
            log.error("Error collecting performance snapshot", e);
        }
    }

    /**
     * Create a new performance snapshot
     */
    public PerformanceSnapshot createSnapshot() {
        PerformanceSnapshot snapshot = new PerformanceSnapshot();
        snapshot.setTimestamp(Instant.now());

        // Collect system metrics
        snapshot.setSystemMetrics(collectSystemMetrics());

        // Collect execution metrics
        snapshot.setExecutionMetrics(collectExecutionMetrics());

        // Collect cluster metrics (if available)
        snapshot.setClusterMetrics(collectClusterMetrics());

        // Collect custom metrics
        snapshot.setCustomMetrics(collectCustomMetrics());

        return snapshot;
    }

    /**
     * Collect system metrics
     */
    private PerformanceSnapshot.SystemMetrics collectSystemMetrics() {
        PerformanceSnapshot.SystemMetrics metrics = new PerformanceSnapshot.SystemMetrics();

        // Memory metrics
        metrics.setHeapUsagePercent(systemMetrics.getHeapUsagePercentage());
        metrics.setHeapUsedBytes(memoryMXBean.getHeapMemoryUsage().getUsed());
        metrics.setHeapMaxBytes(memoryMXBean.getHeapMemoryUsage().getMax());
        metrics.setNonHeapUsedBytes(memoryMXBean.getNonHeapMemoryUsage().getUsed());

        // Thread metrics
        metrics.setThreadCount(threadMXBean.getThreadCount());
        metrics.setDaemonThreadCount(threadMXBean.getDaemonThreadCount());
        metrics.setPeakThreadCount(threadMXBean.getPeakThreadCount());

        // CPU metrics
        metrics.setSystemLoadAverage(osMXBean.getSystemLoadAverage());
        metrics.setAvailableProcessors(osMXBean.getAvailableProcessors());

        // Thread pool metrics (if available)
        metrics.setThreadPoolSize(0);
        metrics.setThreadPoolActive(0);
        metrics.setThreadPoolQueueSize(0);

        return metrics;
    }

    /**
     * Collect execution metrics
     */
    private PerformanceSnapshot.ExecutionMetrics collectExecutionMetrics() {
        PerformanceSnapshot.ExecutionMetrics metrics = new PerformanceSnapshot.ExecutionMetrics();

        metrics.setTotalExecutions((long) jobExecutionMetrics.jobExecutionsTotal.count());
        metrics.setSuccessfulExecutions((long) jobExecutionMetrics.jobExecutionsSuccessful.count());
        metrics.setFailedExecutions((long) jobExecutionMetrics.jobExecutionsFailed.count());
        metrics.setTimeoutExecutions((long) jobExecutionMetrics.jobExecutionsTimeout.count());
        metrics.setRetriedExecutions((long) jobExecutionMetrics.jobExecutionsRetried.count());

        // Calculate rates
        long total = metrics.getTotalExecutions();
        if (total > 0) {
            metrics.setSuccessRate((double) metrics.getSuccessfulExecutions() / total * 100);
            metrics.setFailureRate((double) metrics.getFailedExecutions() / total * 100);
        }

        // Custom metrics
        metrics.setMisfiredJobs((long) customMetrics.getMisfiredJobsCount());
        metrics.setDeadLetterJobs((long) customMetrics.getDeadLetterJobsCount());

        return metrics;
    }

    /**
     * Collect cluster metrics
     */
    private PerformanceSnapshot.ClusterMetrics collectClusterMetrics() {
        PerformanceSnapshot.ClusterMetrics metrics = new PerformanceSnapshot.ClusterMetrics();
        // These would be populated if cluster manager is available
        metrics.setClusterName("standalone");
        metrics.setMemberCount(1);
        metrics.setLocalMemberId("local");
        metrics.setHealthy(true);
        metrics.setMinRequiredMembers(1);
        return metrics;
    }

    /**
     * Collect custom metrics
     */
    private Map<String, Double> collectCustomMetrics() {
        Map<String, Double> metrics = new HashMap<>();
        // Collect any custom metrics from meter registry
        return metrics;
    }

    /**
     * Add snapshot to history
     */
    private void addSnapshot(PerformanceSnapshot snapshot) {
        recentSnapshots.addLast(snapshot);
        if (recentSnapshots.size() > MAX_SNAPSHOTS) {
            recentSnapshots.removeFirst();
        }
    }

    /**
     * Get current performance snapshot
     */
    public PerformanceSnapshot getCurrentSnapshot() {
        return createSnapshot();
    }

    /**
     * Get recent snapshots
     */
    public List<PerformanceSnapshot> getRecentSnapshots(int count) {
        return recentSnapshots.stream()
                .skip(Math.max(0, recentSnapshots.size() - count))
                .collect(Collectors.toList());
    }

    /**
     * Get all recent snapshots
     */
    public List<PerformanceSnapshot> getAllRecentSnapshots() {
        return new ArrayList<>(recentSnapshots);
    }

    /**
     * Get performance trend for a specific metric
     */
    public List<MetricDataPoint> getMetricTrend(String metricName, int count) {
        return recentSnapshots.stream()
                .skip(Math.max(0, recentSnapshots.size() - count))
                .map(snapshot -> {
                    double value = extractMetricValue(snapshot, metricName);
                    return new MetricDataPoint(snapshot.getTimestamp(), value);
                })
                .collect(Collectors.toList());
    }

    /**
     * Extract metric value from snapshot
     */
    private double extractMetricValue(PerformanceSnapshot snapshot, String metricName) {
        switch (metricName) {
            case "heapUsagePercent":
                return snapshot.getSystemMetrics().getHeapUsagePercent();
            case "threadCount":
                return snapshot.getSystemMetrics().getThreadCount();
            case "systemLoadAverage":
                return snapshot.getSystemMetrics().getSystemLoadAverage();
            case "totalExecutions":
                return snapshot.getExecutionMetrics().getTotalExecutions();
            case "successRate":
                return snapshot.getExecutionMetrics().getSuccessRate();
            case "failureRate":
                return snapshot.getExecutionMetrics().getFailureRate();
            default:
                return snapshot.getCustomMetrics().getOrDefault(metricName, 0.0);
        }
    }

    /**
     * Calculate average for a metric over recent snapshots
     */
    public double getMetricAverage(String metricName, int count) {
        return getMetricTrend(metricName, count).stream()
                .mapToDouble(MetricDataPoint::getValue)
                .average()
                .orElse(0.0);
    }

    /**
     * Get performance summary
     */
    public PerformanceSummary getPerformanceSummary() {
        PerformanceSnapshot current = getCurrentSnapshot();

        PerformanceSummary summary = new PerformanceSummary();
        summary.setCurrentSnapshot(current);
        summary.setAvgHeapUsage(getMetricAverage("heapUsagePercent", 10));
        summary.setAvgThreadCount(getMetricAverage("threadCount", 10));
        summary.setAvgSuccessRate(getMetricAverage("successRate", 10));
        summary.setAvgFailureRate(getMetricAverage("failureRate", 10));
        summary.setTotalSnapshots(recentSnapshots.size());

        return summary;
    }

    /**
     * Clear snapshot history
     */
    public void clearHistory() {
        recentSnapshots.clear();
        log.info("Performance snapshot history cleared");
    }

    /**
     * Metric data point
     */
    public static class MetricDataPoint {
        private final Instant timestamp;
        private final double value;

        public MetricDataPoint(Instant timestamp, double value) {
            this.timestamp = timestamp;
            this.value = value;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public double getValue() {
            return value;
        }
    }

    /**
     * Performance summary
     */
    public static class PerformanceSummary {
        private PerformanceSnapshot currentSnapshot;
        private double avgHeapUsage;
        private double avgThreadCount;
        private double avgSuccessRate;
        private double avgFailureRate;
        private int totalSnapshots;

        public PerformanceSnapshot getCurrentSnapshot() {
            return currentSnapshot;
        }

        public void setCurrentSnapshot(PerformanceSnapshot currentSnapshot) {
            this.currentSnapshot = currentSnapshot;
        }

        public double getAvgHeapUsage() {
            return avgHeapUsage;
        }

        public void setAvgHeapUsage(double avgHeapUsage) {
            this.avgHeapUsage = avgHeapUsage;
        }

        public double getAvgThreadCount() {
            return avgThreadCount;
        }

        public void setAvgThreadCount(double avgThreadCount) {
            this.avgThreadCount = avgThreadCount;
        }

        public double getAvgSuccessRate() {
            return avgSuccessRate;
        }

        public void setAvgSuccessRate(double avgSuccessRate) {
            this.avgSuccessRate = avgSuccessRate;
        }

        public double getAvgFailureRate() {
            return avgFailureRate;
        }

        public void setAvgFailureRate(double avgFailureRate) {
            this.avgFailureRate = avgFailureRate;
        }

        public int getTotalSnapshots() {
            return totalSnapshots;
        }

        public void setTotalSnapshots(int totalSnapshots) {
            this.totalSnapshots = totalSnapshots;
        }
    }
}
