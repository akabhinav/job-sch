package com.enterprise.scheduler.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Alert instance
 * Feature #29: Alerting System
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alert {

    /**
     * Alert ID
     */
    private String id;

    /**
     * Rule that triggered this alert
     */
    private String ruleId;

    /**
     * Rule name
     */
    private String ruleName;

    /**
     * Alert severity
     */
    private AlertRule.AlertSeverity severity;

    /**
     * Alert message
     */
    private String message;

    /**
     * Metric name
     */
    private String metricName;

    /**
     * Metric value that triggered the alert
     */
    private double triggerValue;

    /**
     * Threshold value
     */
    private double threshold;

    /**
     * Job name (if applicable)
     */
    private String jobName;

    /**
     * Alert status
     */
    @Builder.Default
    private AlertStatus status = AlertStatus.ACTIVE;

    /**
     * Timestamp when alert was triggered
     */
    private Instant triggeredAt;

    /**
     * Timestamp when alert was acknowledged
     */
    private Instant acknowledgedAt;

    /**
     * Acknowledged by user
     */
    private String acknowledgedBy;

    /**
     * Timestamp when alert was resolved
     */
    private Instant resolvedAt;

    /**
     * Additional context
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /**
     * Alert status
     */
    public enum AlertStatus {
        ACTIVE,
        ACKNOWLEDGED,
        RESOLVED,
        SUPPRESSED
    }

    /**
     * Check if alert is active
     */
    public boolean isActive() {
        return status == AlertStatus.ACTIVE;
    }

    /**
     * Acknowledge alert
     */
    public void acknowledge(String user) {
        this.status = AlertStatus.ACKNOWLEDGED;
        this.acknowledgedAt = Instant.now();
        this.acknowledgedBy = user;
    }

    /**
     * Resolve alert
     */
    public void resolve() {
        this.status = AlertStatus.RESOLVED;
        this.resolvedAt = Instant.now();
    }

    /**
     * Suppress alert
     */
    public void suppress() {
        this.status = AlertStatus.SUPPRESSED;
    }
}
