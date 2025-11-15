package com.enterprise.scheduler.monitoring.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Health status DTO
 * Feature #26: Health Monitoring
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus {

    /**
     * Overall health status
     */
    @Builder.Default
    private Status status = Status.UNKNOWN;

    /**
     * Timestamp
     */
    private Instant timestamp;

    /**
     * Component health details
     */
    @Builder.Default
    private Map<String, ComponentHealth> components = new HashMap<>();

    /**
     * Additional details
     */
    @Builder.Default
    private Map<String, Object> details = new HashMap<>();

    /**
     * Health status enumeration
     */
    public enum Status {
        UP,
        DOWN,
        WARNING,
        UNKNOWN
    }

    /**
     * Component health
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth {
        private Status status;
        private String message;

        @Builder.Default
        private Map<String, Object> details = new HashMap<>();
    }

    /**
     * Check if system is healthy
     */
    public boolean isHealthy() {
        return status == Status.UP || status == Status.WARNING;
    }
}
