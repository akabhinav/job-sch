package com.enterprise.scheduler.monitoring.api;

import com.enterprise.scheduler.monitoring.alerting.AlertingService;
import com.enterprise.scheduler.monitoring.analytics.PerformanceAnalyticsService;
import com.enterprise.scheduler.monitoring.metrics.MetricsService;
import com.enterprise.scheduler.monitoring.model.Alert;
import com.enterprise.scheduler.monitoring.model.AlertRule;
import com.enterprise.scheduler.monitoring.model.PerformanceSnapshot;
import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for monitoring endpoints
 * Feature #25: Custom Metrics Dashboard
 */
@Slf4j
@RestController
@RequestMapping("/api/monitoring")
@RequiredArgsConstructor
public class MonitoringController {

    private final MetricsService metricsService;
    private final PerformanceAnalyticsService performanceAnalytics;
    private final AlertingService alertingService;

    /**
     * Get current metrics snapshot
     */
    @GetMapping("/metrics/snapshot")
    @Timed(value = "monitoring.metrics.snapshot", description = "Get metrics snapshot")
    public ResponseEntity<Map<String, Object>> getMetricsSnapshot() {
        log.debug("Getting metrics snapshot");
        MetricsService.MetricsSnapshot snapshot = metricsService.getMetricsSnapshot();
        return ResponseEntity.ok(snapshot.toMap());
    }

    /**
     * Get job-specific metrics
     */
    @GetMapping("/metrics/job/{jobName}")
    @Timed(value = "monitoring.metrics.job", description = "Get job metrics")
    public ResponseEntity<Map<String, Object>> getJobMetrics(@PathVariable String jobName) {
        log.debug("Getting metrics for job: {}", jobName);
        MetricsService.JobMetrics metrics = metricsService.getJobMetrics(jobName);
        return ResponseEntity.ok(metrics.toMap());
    }

    /**
     * Get current performance snapshot
     */
    @GetMapping("/performance/current")
    @Timed(value = "monitoring.performance.current", description = "Get current performance")
    public ResponseEntity<PerformanceSnapshot> getCurrentPerformance() {
        log.debug("Getting current performance snapshot");
        PerformanceSnapshot snapshot = performanceAnalytics.getCurrentSnapshot();
        return ResponseEntity.ok(snapshot);
    }

    /**
     * Get recent performance snapshots
     */
    @GetMapping("/performance/history")
    @Timed(value = "monitoring.performance.history", description = "Get performance history")
    public ResponseEntity<List<PerformanceSnapshot>> getPerformanceHistory(
            @RequestParam(defaultValue = "20") int count) {
        log.debug("Getting performance history: {} snapshots", count);
        List<PerformanceSnapshot> snapshots = performanceAnalytics.getRecentSnapshots(count);
        return ResponseEntity.ok(snapshots);
    }

    /**
     * Get performance summary
     */
    @GetMapping("/performance/summary")
    @Timed(value = "monitoring.performance.summary", description = "Get performance summary")
    public ResponseEntity<PerformanceAnalyticsService.PerformanceSummary> getPerformanceSummary() {
        log.debug("Getting performance summary");
        PerformanceAnalyticsService.PerformanceSummary summary = performanceAnalytics.getPerformanceSummary();
        return ResponseEntity.ok(summary);
    }

    /**
     * Get metric trend
     */
    @GetMapping("/performance/trend/{metricName}")
    @Timed(value = "monitoring.performance.trend", description = "Get metric trend")
    public ResponseEntity<List<PerformanceAnalyticsService.MetricDataPoint>> getMetricTrend(
            @PathVariable String metricName,
            @RequestParam(defaultValue = "20") int count) {
        log.debug("Getting trend for metric: {}", metricName);
        List<PerformanceAnalyticsService.MetricDataPoint> trend =
                performanceAnalytics.getMetricTrend(metricName, count);
        return ResponseEntity.ok(trend);
    }

    /**
     * Get all alert rules
     */
    @GetMapping("/alerts/rules")
    @Timed(value = "monitoring.alerts.rules.list", description = "List alert rules")
    public ResponseEntity<List<AlertRule>> getAlertRules() {
        log.debug("Getting all alert rules");
        List<AlertRule> rules = alertingService.getAllAlertRules();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get alert rule by ID
     */
    @GetMapping("/alerts/rules/{ruleId}")
    @Timed(value = "monitoring.alerts.rules.get", description = "Get alert rule")
    public ResponseEntity<AlertRule> getAlertRule(@PathVariable String ruleId) {
        log.debug("Getting alert rule: {}", ruleId);
        AlertRule rule = alertingService.getAlertRule(ruleId);
        if (rule == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(rule);
    }

    /**
     * Create alert rule
     */
    @PostMapping("/alerts/rules")
    @Timed(value = "monitoring.alerts.rules.create", description = "Create alert rule")
    public ResponseEntity<AlertRule> createAlertRule(@RequestBody AlertRule rule) {
        log.info("Creating alert rule: {}", rule.getName());
        alertingService.registerAlertRule(rule);
        return ResponseEntity.ok(rule);
    }

    /**
     * Update alert rule
     */
    @PutMapping("/alerts/rules/{ruleId}")
    @Timed(value = "monitoring.alerts.rules.update", description = "Update alert rule")
    public ResponseEntity<AlertRule> updateAlertRule(
            @PathVariable String ruleId,
            @RequestBody AlertRule rule) {
        log.info("Updating alert rule: {}", ruleId);
        alertingService.updateAlertRule(ruleId, rule);
        return ResponseEntity.ok(rule);
    }

    /**
     * Delete alert rule
     */
    @DeleteMapping("/alerts/rules/{ruleId}")
    @Timed(value = "monitoring.alerts.rules.delete", description = "Delete alert rule")
    public ResponseEntity<Void> deleteAlertRule(@PathVariable String ruleId) {
        log.info("Deleting alert rule: {}", ruleId);
        alertingService.removeAlertRule(ruleId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Enable alert rule
     */
    @PostMapping("/alerts/rules/{ruleId}/enable")
    @Timed(value = "monitoring.alerts.rules.enable", description = "Enable alert rule")
    public ResponseEntity<Void> enableAlertRule(@PathVariable String ruleId) {
        log.info("Enabling alert rule: {}", ruleId);
        alertingService.enableAlertRule(ruleId);
        return ResponseEntity.ok().build();
    }

    /**
     * Disable alert rule
     */
    @PostMapping("/alerts/rules/{ruleId}/disable")
    @Timed(value = "monitoring.alerts.rules.disable", description = "Disable alert rule")
    public ResponseEntity<Void> disableAlertRule(@PathVariable String ruleId) {
        log.info("Disabling alert rule: {}", ruleId);
        alertingService.disableAlertRule(ruleId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get active alerts
     */
    @GetMapping("/alerts/active")
    @Timed(value = "monitoring.alerts.active", description = "Get active alerts")
    public ResponseEntity<List<Alert>> getActiveAlerts() {
        log.debug("Getting active alerts");
        List<Alert> alerts = alertingService.getActiveAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Get alert history
     */
    @GetMapping("/alerts/history")
    @Timed(value = "monitoring.alerts.history", description = "Get alert history")
    public ResponseEntity<List<Alert>> getAlertHistory(
            @RequestParam(defaultValue = "50") int count) {
        log.debug("Getting alert history: {} alerts", count);
        List<Alert> alerts = alertingService.getAlertHistory(count);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Acknowledge alert
     */
    @PostMapping("/alerts/{alertId}/acknowledge")
    @Timed(value = "monitoring.alerts.acknowledge", description = "Acknowledge alert")
    public ResponseEntity<Void> acknowledgeAlert(
            @PathVariable String alertId,
            @RequestParam String user) {
        log.info("Acknowledging alert: {} by user: {}", alertId, user);
        alertingService.acknowledgeAlert(alertId, user);
        return ResponseEntity.ok().build();
    }

    /**
     * Resolve alert
     */
    @PostMapping("/alerts/{alertId}/resolve")
    @Timed(value = "monitoring.alerts.resolve", description = "Resolve alert")
    public ResponseEntity<Void> resolveAlert(@PathVariable String alertId) {
        log.info("Resolving alert: {}", alertId);
        alertingService.resolveAlert(alertId);
        return ResponseEntity.ok().build();
    }

    /**
     * Get alert statistics
     */
    @GetMapping("/alerts/statistics")
    @Timed(value = "monitoring.alerts.statistics", description = "Get alert statistics")
    public ResponseEntity<AlertingService.AlertStatistics> getAlertStatistics() {
        log.debug("Getting alert statistics");
        AlertingService.AlertStatistics stats = alertingService.getAlertStatistics();
        return ResponseEntity.ok(stats);
    }

    /**
     * Clear all alerts
     */
    @DeleteMapping("/alerts/active")
    @Timed(value = "monitoring.alerts.clear", description = "Clear all alerts")
    public ResponseEntity<Void> clearAllAlerts() {
        log.warn("Clearing all active alerts");
        alertingService.clearAllAlerts();
        return ResponseEntity.noContent().build();
    }
}
