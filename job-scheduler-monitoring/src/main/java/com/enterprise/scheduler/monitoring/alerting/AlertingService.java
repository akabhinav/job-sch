package com.enterprise.scheduler.monitoring.alerting;

import com.enterprise.scheduler.common.util.IdGenerator;
import com.enterprise.scheduler.core.spi.NotificationProvider;
import com.enterprise.scheduler.monitoring.analytics.PerformanceAnalyticsService;
import com.enterprise.scheduler.monitoring.model.Alert;
import com.enterprise.scheduler.monitoring.model.AlertRule;
import com.enterprise.scheduler.monitoring.model.PerformanceSnapshot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for managing alerts and notifications
 * Feature #29: Alerting System
 * Feature #30: Alert Rules Configuration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AlertingService {

    private final PerformanceAnalyticsService performanceAnalytics;
    private final List<NotificationProvider> notificationProviders;

    // Alert rules registry
    private final Map<String, AlertRule> alertRules = new ConcurrentHashMap<>();

    // Active alerts
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();

    // Alert history (last 1000)
    private final Deque<Alert> alertHistory = new LinkedList<>();
    private static final int MAX_HISTORY = 1000;

    // Cooldown tracking
    private final Map<String, Instant> ruleCooldowns = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Initializing alerting service with {} notification providers",
                notificationProviders.size());

        // Register default alert rules
        registerDefaultAlertRules();
    }

    /**
     * Register default alert rules
     */
    private void registerDefaultAlertRules() {
        // High heap usage
        registerAlertRule(AlertRule.builder()
                .id("high-heap-usage")
                .name("High Heap Usage")
                .description("Alert when heap usage exceeds threshold")
                .metricName("heapUsagePercent")
                .conditionType(AlertRule.ConditionType.THRESHOLD)
                .operator(AlertRule.ComparisonOperator.GREATER_THAN)
                .threshold(85.0)
                .severity(AlertRule.AlertSeverity.WARNING)
                .cooldownSeconds(300)
                .build());

        // Critical heap usage
        registerAlertRule(AlertRule.builder()
                .id("critical-heap-usage")
                .name("Critical Heap Usage")
                .description("Alert when heap usage is critically high")
                .metricName("heapUsagePercent")
                .conditionType(AlertRule.ConditionType.THRESHOLD)
                .operator(AlertRule.ComparisonOperator.GREATER_THAN)
                .threshold(95.0)
                .severity(AlertRule.AlertSeverity.CRITICAL)
                .cooldownSeconds(180)
                .build());

        // High failure rate
        registerAlertRule(AlertRule.builder()
                .id("high-failure-rate")
                .name("High Job Failure Rate")
                .description("Alert when job failure rate exceeds threshold")
                .metricName("failureRate")
                .conditionType(AlertRule.ConditionType.THRESHOLD)
                .operator(AlertRule.ComparisonOperator.GREATER_THAN)
                .threshold(10.0)
                .severity(AlertRule.AlertSeverity.ERROR)
                .cooldownSeconds(600)
                .build());

        // System load
        registerAlertRule(AlertRule.builder()
                .id("high-system-load")
                .name("High System Load")
                .description("Alert when system load is high")
                .metricName("systemLoadAverage")
                .conditionType(AlertRule.ConditionType.THRESHOLD)
                .operator(AlertRule.ComparisonOperator.GREATER_THAN)
                .threshold(5.0)
                .severity(AlertRule.AlertSeverity.WARNING)
                .cooldownSeconds(300)
                .build());

        log.info("Registered {} default alert rules", alertRules.size());
    }

    /**
     * Register alert rule
     */
    public void registerAlertRule(AlertRule rule) {
        alertRules.put(rule.getId(), rule);
        log.info("Registered alert rule: {} - {}", rule.getId(), rule.getName());
    }

    /**
     * Update alert rule
     */
    public void updateAlertRule(String ruleId, AlertRule rule) {
        if (!alertRules.containsKey(ruleId)) {
            throw new IllegalArgumentException("Alert rule not found: " + ruleId);
        }
        rule.setId(ruleId);
        alertRules.put(ruleId, rule);
        log.info("Updated alert rule: {}", ruleId);
    }

    /**
     * Remove alert rule
     */
    public void removeAlertRule(String ruleId) {
        alertRules.remove(ruleId);
        ruleCooldowns.remove(ruleId);
        log.info("Removed alert rule: {}", ruleId);
    }

    /**
     * Get alert rule
     */
    public AlertRule getAlertRule(String ruleId) {
        return alertRules.get(ruleId);
    }

    /**
     * Get all alert rules
     */
    public List<AlertRule> getAllAlertRules() {
        return new ArrayList<>(alertRules.values());
    }

    /**
     * Enable alert rule
     */
    public void enableAlertRule(String ruleId) {
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(true);
            log.info("Enabled alert rule: {}", ruleId);
        }
    }

    /**
     * Disable alert rule
     */
    public void disableAlertRule(String ruleId) {
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            rule.setEnabled(false);
            log.info("Disabled alert rule: {}", ruleId);
        }
    }

    /**
     * Evaluate alert rules (scheduled)
     */
    @Scheduled(fixedRate = 30000) // Every 30 seconds
    public void evaluateAlertRules() {
        log.debug("Evaluating {} alert rules", alertRules.size());

        PerformanceSnapshot snapshot = performanceAnalytics.getCurrentSnapshot();

        for (AlertRule rule : alertRules.values()) {
            if (!rule.isEnabled()) {
                continue;
            }

            // Check cooldown
            if (isInCooldown(rule.getId())) {
                log.trace("Rule {} is in cooldown, skipping", rule.getId());
                continue;
            }

            try {
                evaluateRule(rule, snapshot);
            } catch (Exception e) {
                log.error("Error evaluating rule: {}", rule.getId(), e);
            }
        }
    }

    /**
     * Evaluate a single rule
     */
    private void evaluateRule(AlertRule rule, PerformanceSnapshot snapshot) {
        double metricValue = extractMetricValue(snapshot, rule.getMetricName());

        if (rule.isTriggered(metricValue)) {
            log.info("Alert rule triggered: {} - metric={}, value={}, threshold={}",
                    rule.getName(), rule.getMetricName(), metricValue, rule.getThreshold());

            Alert alert = createAlert(rule, metricValue);
            triggerAlert(alert);
        }
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
     * Create alert from rule
     */
    private Alert createAlert(AlertRule rule, double value) {
        return Alert.builder()
                .id(IdGenerator.generateId())
                .ruleId(rule.getId())
                .ruleName(rule.getName())
                .severity(rule.getSeverity())
                .metricName(rule.getMetricName())
                .triggerValue(value)
                .threshold(rule.getThreshold())
                .jobName(rule.getJobNameFilter())
                .message(formatAlertMessage(rule, value))
                .triggeredAt(Instant.now())
                .status(Alert.AlertStatus.ACTIVE)
                .build();
    }

    /**
     * Format alert message
     */
    private String formatAlertMessage(AlertRule rule, double value) {
        return String.format("%s: %s is %.2f (threshold: %.2f)",
                rule.getName(),
                rule.getMetricName(),
                value,
                rule.getThreshold());
    }

    /**
     * Trigger alert
     */
    public void triggerAlert(Alert alert) {
        // Add to active alerts
        activeAlerts.put(alert.getId(), alert);

        // Add to history
        addToHistory(alert);

        // Set cooldown
        setCooldown(alert.getRuleId());

        // Send notifications
        sendNotifications(alert);

        log.warn("Alert triggered: {} - {}", alert.getRuleName(), alert.getMessage());
    }

    /**
     * Send notifications for alert
     */
    private void sendNotifications(Alert alert) {
        NotificationProvider.NotificationLevel level = mapSeverityToLevel(alert.getSeverity());

        for (NotificationProvider provider : notificationProviders) {
            try {
                provider.notify(
                        alert.getRuleName(),
                        alert.getMessage(),
                        level
                );
                log.debug("Sent notification via provider: {}", provider.getName());
            } catch (Exception e) {
                log.error("Error sending notification via provider: {}", provider.getName(), e);
            }
        }
    }

    /**
     * Map alert severity to notification level
     */
    private NotificationProvider.NotificationLevel mapSeverityToLevel(AlertRule.AlertSeverity severity) {
        switch (severity) {
            case INFO:
                return NotificationProvider.NotificationLevel.INFO;
            case WARNING:
                return NotificationProvider.NotificationLevel.WARNING;
            case ERROR:
                return NotificationProvider.NotificationLevel.ERROR;
            case CRITICAL:
                return NotificationProvider.NotificationLevel.CRITICAL;
            default:
                return NotificationProvider.NotificationLevel.INFO;
        }
    }

    /**
     * Acknowledge alert
     */
    public void acknowledgeAlert(String alertId, String user) {
        Alert alert = activeAlerts.get(alertId);
        if (alert != null) {
            alert.acknowledge(user);
            log.info("Alert acknowledged: {} by {}", alertId, user);
        }
    }

    /**
     * Resolve alert
     */
    public void resolveAlert(String alertId) {
        Alert alert = activeAlerts.remove(alertId);
        if (alert != null) {
            alert.resolve();
            log.info("Alert resolved: {}", alertId);
        }
    }

    /**
     * Get active alerts
     */
    public List<Alert> getActiveAlerts() {
        return new ArrayList<>(activeAlerts.values());
    }

    /**
     * Get alert history
     */
    public List<Alert> getAlertHistory(int count) {
        return alertHistory.stream()
                .limit(count)
                .collect(Collectors.toList());
    }

    /**
     * Add alert to history
     */
    private void addToHistory(Alert alert) {
        alertHistory.addFirst(alert);
        if (alertHistory.size() > MAX_HISTORY) {
            alertHistory.removeLast();
        }
    }

    /**
     * Check if rule is in cooldown
     */
    private boolean isInCooldown(String ruleId) {
        Instant cooldownUntil = ruleCooldowns.get(ruleId);
        if (cooldownUntil == null) {
            return false;
        }
        return Instant.now().isBefore(cooldownUntil);
    }

    /**
     * Set cooldown for rule
     */
    private void setCooldown(String ruleId) {
        AlertRule rule = alertRules.get(ruleId);
        if (rule != null) {
            Instant cooldownUntil = Instant.now().plusSeconds(rule.getCooldownSeconds());
            ruleCooldowns.put(ruleId, cooldownUntil);
        }
    }

    /**
     * Clear all alerts
     */
    public void clearAllAlerts() {
        activeAlerts.clear();
        log.info("Cleared all active alerts");
    }

    /**
     * Get alert statistics
     */
    public AlertStatistics getAlertStatistics() {
        AlertStatistics stats = new AlertStatistics();
        stats.setActiveAlertCount(activeAlerts.size());
        stats.setTotalHistoryCount(alertHistory.size());
        stats.setAlertsByseverity(getAlertCountBySeverity());
        stats.setAlertsByRule(getAlertCountByRule());
        return stats;
    }

    /**
     * Get alert count by severity
     */
    private Map<AlertRule.AlertSeverity, Long> getAlertCountBySeverity() {
        return activeAlerts.values().stream()
                .collect(Collectors.groupingBy(
                        Alert::getSeverity,
                        Collectors.counting()
                ));
    }

    /**
     * Get alert count by rule
     */
    private Map<String, Long> getAlertCountByRule() {
        return activeAlerts.values().stream()
                .collect(Collectors.groupingBy(
                        Alert::getRuleId,
                        Collectors.counting()
                ));
    }

    /**
     * Alert statistics
     */
    public static class AlertStatistics {
        private int activeAlertCount;
        private int totalHistoryCount;
        private Map<AlertRule.AlertSeverity, Long> alertsByLevel;
        private Map<String, Long> alertsByRule;

        public int getActiveAlertCount() {
            return activeAlertCount;
        }

        public void setActiveAlertCount(int activeAlertCount) {
            this.activeAlertCount = activeAlertCount;
        }

        public int getTotalHistoryCount() {
            return totalHistoryCount;
        }

        public void setTotalHistoryCount(int totalHistoryCount) {
            this.totalHistoryCount = totalHistoryCount;
        }

        public Map<AlertRule.AlertSeverity, Long> getAlertsBySeverity() {
            return alertsByLevel;
        }

        public void setAlertsByLevel(Map<AlertRule.AlertSeverity, Long> alertsByLevel) {
            this.alertsByLevel = alertsByLevel;
        }

        public Map<String, Long> getAlertsByRule() {
            return alertsByRule;
        }

        public void setAlertsByRule(Map<String, Long> alertsByRule) {
            this.alertsByRule = alertsByRule;
        }

        public void setAlertsBySeverity(Map<AlertRule.AlertSeverity, Long> alertsBySeverity) {
            this.alertsByLevel = alertsBySeverity;
        }
    }
}
