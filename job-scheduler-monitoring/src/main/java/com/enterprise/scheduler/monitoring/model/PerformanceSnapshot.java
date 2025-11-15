package com.enterprise.scheduler.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Performance snapshot model
 * Feature #27: Performance Analytics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceSnapshot {

    /**
     * Snapshot timestamp
     */
    private Instant timestamp;

    /**
     * System metrics
     */
    @Builder.Default
    private SystemMetrics systemMetrics = new SystemMetrics();

    /**
     * Job execution metrics
     */
    @Builder.Default
    private ExecutionMetrics executionMetrics = new ExecutionMetrics();

    /**
     * Cluster metrics
     */
    @Builder.Default
    private ClusterMetrics clusterMetrics = new ClusterMetrics();

    /**
     * Custom metrics
     */
    @Builder.Default
    private Map<String, Double> customMetrics = new HashMap<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemMetrics {
        private double heapUsagePercent;
        private long heapUsedBytes;
        private long heapMaxBytes;
        private double nonHeapUsedBytes;
        private int threadCount;
        private int daemonThreadCount;
        private int peakThreadCount;
        private double systemLoadAverage;
        private int availableProcessors;
        private int threadPoolSize;
        private int threadPoolActive;
        private int threadPoolQueueSize;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionMetrics {
        private long totalExecutions;
        private long successfulExecutions;
        private long failedExecutions;
        private long timeoutExecutions;
        private long retriedExecutions;
        private long activeExecutions;
        private double averageExecutionDurationMs;
        private double successRate;
        private double failureRate;
        private long misfiredJobs;
        private long deadLetterJobs;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClusterMetrics {
        private String clusterName;
        private int memberCount;
        private String localMemberId;
        private boolean isHealthy;
        private int minRequiredMembers;
    }

    /**
     * Convert to map for serialization
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("timestamp", timestamp);
        map.put("systemMetrics", systemMetricsToMap());
        map.put("executionMetrics", executionMetricsToMap());
        map.put("clusterMetrics", clusterMetricsToMap());
        map.put("customMetrics", customMetrics);
        return map;
    }

    private Map<String, Object> systemMetricsToMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("heapUsagePercent", systemMetrics.heapUsagePercent);
        map.put("heapUsedBytes", systemMetrics.heapUsedBytes);
        map.put("heapMaxBytes", systemMetrics.heapMaxBytes);
        map.put("nonHeapUsedBytes", systemMetrics.nonHeapUsedBytes);
        map.put("threadCount", systemMetrics.threadCount);
        map.put("daemonThreadCount", systemMetrics.daemonThreadCount);
        map.put("peakThreadCount", systemMetrics.peakThreadCount);
        map.put("systemLoadAverage", systemMetrics.systemLoadAverage);
        map.put("availableProcessors", systemMetrics.availableProcessors);
        map.put("threadPoolSize", systemMetrics.threadPoolSize);
        map.put("threadPoolActive", systemMetrics.threadPoolActive);
        map.put("threadPoolQueueSize", systemMetrics.threadPoolQueueSize);
        return map;
    }

    private Map<String, Object> executionMetricsToMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("totalExecutions", executionMetrics.totalExecutions);
        map.put("successfulExecutions", executionMetrics.successfulExecutions);
        map.put("failedExecutions", executionMetrics.failedExecutions);
        map.put("timeoutExecutions", executionMetrics.timeoutExecutions);
        map.put("retriedExecutions", executionMetrics.retriedExecutions);
        map.put("activeExecutions", executionMetrics.activeExecutions);
        map.put("averageExecutionDurationMs", executionMetrics.averageExecutionDurationMs);
        map.put("successRate", executionMetrics.successRate);
        map.put("failureRate", executionMetrics.failureRate);
        map.put("misfiredJobs", executionMetrics.misfiredJobs);
        map.put("deadLetterJobs", executionMetrics.deadLetterJobs);
        return map;
    }

    private Map<String, Object> clusterMetricsToMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("clusterName", clusterMetrics.clusterName);
        map.put("memberCount", clusterMetrics.memberCount);
        map.put("localMemberId", clusterMetrics.localMemberId);
        map.put("isHealthy", clusterMetrics.isHealthy);
        map.put("minRequiredMembers", clusterMetrics.minRequiredMembers);
        return map;
    }
}
