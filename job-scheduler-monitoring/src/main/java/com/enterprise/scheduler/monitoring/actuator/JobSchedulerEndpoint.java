package com.enterprise.scheduler.monitoring.actuator;

import com.enterprise.scheduler.monitoring.alerting.AlertingService;
import com.enterprise.scheduler.monitoring.analytics.PerformanceAnalyticsService;
import com.enterprise.scheduler.monitoring.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom Actuator endpoint for job scheduler monitoring
 * Feature #21: Metrics Collection Infrastructure
 */
@Component
@Endpoint(id = "jobscheduler")
@RequiredArgsConstructor
public class JobSchedulerEndpoint {

    private final MetricsService metricsService;
    private final PerformanceAnalyticsService performanceAnalytics;
    private final AlertingService alertingService;

    /**
     * Get comprehensive job scheduler status
     */
    @ReadOperation
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();

        // Basic info
        status.put("timestamp", Instant.now());
        status.put("status", "UP");

        // Metrics snapshot
        MetricsService.MetricsSnapshot metricsSnapshot = metricsService.getMetricsSnapshot();
        status.put("metrics", metricsSnapshot.toMap());

        // Performance summary
        PerformanceAnalyticsService.PerformanceSummary performanceSummary =
                performanceAnalytics.getPerformanceSummary();
        status.put("performance", buildPerformanceMap(performanceSummary));

        // Alert statistics
        AlertingService.AlertStatistics alertStats = alertingService.getAlertStatistics();
        status.put("alerts", buildAlertStatsMap(alertStats));

        return status;
    }

    /**
     * Build performance map
     */
    private Map<String, Object> buildPerformanceMap(
            PerformanceAnalyticsService.PerformanceSummary summary) {
        Map<String, Object> map = new HashMap<>();
        map.put("avgHeapUsage", summary.getAvgHeapUsage());
        map.put("avgThreadCount", summary.getAvgThreadCount());
        map.put("avgSuccessRate", summary.getAvgSuccessRate());
        map.put("avgFailureRate", summary.getAvgFailureRate());
        map.put("totalSnapshots", summary.getTotalSnapshots());
        return map;
    }

    /**
     * Build alert statistics map
     */
    private Map<String, Object> buildAlertStatsMap(AlertingService.AlertStatistics stats) {
        Map<String, Object> map = new HashMap<>();
        map.put("activeCount", stats.getActiveAlertCount());
        map.put("historyCount", stats.getTotalHistoryCount());
        map.put("bySeverity", stats.getAlertsBySeverity());
        map.put("byRule", stats.getAlertsByRule());
        return map;
    }
}
