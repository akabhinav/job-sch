package com.enterprise.scheduler.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Metric summary DTO
 * Feature #25: Custom Metrics Dashboard
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MetricSummary {

    /**
     * Metric name
     */
    private String name;

    /**
     * Metric description
     */
    private String description;

    /**
     * Current value
     */
    private double currentValue;

    /**
     * Average value
     */
    private double averageValue;

    /**
     * Minimum value
     */
    private double minValue;

    /**
     * Maximum value
     */
    private double maxValue;

    /**
     * Last updated timestamp
     */
    private Instant lastUpdated;

    /**
     * Metric unit
     */
    private String unit;

    /**
     * Metric type (gauge, counter, timer, etc.)
     */
    private String type;
}
