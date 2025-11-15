package com.enterprise.scheduler.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Alert rule configuration
 * Feature #29: Alerting System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlertRule {

    /**
     * Rule ID
     */
    private String id;

    /**
     * Rule name
     */
    private String name;

    /**
     * Rule description
     */
    private String description;

    /**
     * Metric name to monitor
     */
    private String metricName;

    /**
     * Alert condition type
     */
    private ConditionType conditionType;

    /**
     * Threshold value
     */
    private double threshold;

    /**
     * Comparison operator
     */
    private ComparisonOperator operator;

    /**
     * Alert severity
     */
    private AlertSeverity severity;

    /**
     * Whether rule is enabled
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Cooldown period in seconds
     */
    @Builder.Default
    private int cooldownSeconds = 300; // 5 minutes

    /**
     * Notification channels
     */
    @Builder.Default
    private Map<String, Object> notificationConfig = new HashMap<>();

    /**
     * Job name filter (null = all jobs)
     */
    private String jobNameFilter;

    /**
     * Condition types
     */
    public enum ConditionType {
        THRESHOLD,
        RATE_OF_CHANGE,
        CONSECUTIVE_FAILURES,
        DURATION
    }

    /**
     * Comparison operators
     */
    public enum ComparisonOperator {
        GREATER_THAN,
        GREATER_THAN_OR_EQUAL,
        LESS_THAN,
        LESS_THAN_OR_EQUAL,
        EQUAL,
        NOT_EQUAL
    }

    /**
     * Alert severity levels
     */
    public enum AlertSeverity {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }

    /**
     * Check if value triggers the alert
     */
    public boolean isTriggered(double value) {
        switch (operator) {
            case GREATER_THAN:
                return value > threshold;
            case GREATER_THAN_OR_EQUAL:
                return value >= threshold;
            case LESS_THAN:
                return value < threshold;
            case LESS_THAN_OR_EQUAL:
                return value <= threshold;
            case EQUAL:
                return value == threshold;
            case NOT_EQUAL:
                return value != threshold;
            default:
                return false;
        }
    }
}
