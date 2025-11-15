package com.enterprise.scheduler.core.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Base class for all job events
 * Feature #45: Event-driven Architecture
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEvent {

    /**
     * Event ID
     */
    private String id;

    /**
     * Event type
     */
    private EventType type;

    /**
     * Job ID
     */
    private String jobId;

    /**
     * Execution ID (if applicable)
     */
    private String executionId;

    /**
     * Tenant ID
     */
    private String tenantId;

    /**
     * Event timestamp
     */
    @Builder.Default
    private Instant timestamp = Instant.now();

    /**
     * Event source
     */
    private String source;

    /**
     * Event payload
     */
    @Builder.Default
    private Map<String, Object> payload = new HashMap<>();

    public enum EventType {
        JOB_CREATED,
        JOB_UPDATED,
        JOB_DELETED,
        JOB_ENABLED,
        JOB_DISABLED,
        JOB_SCHEDULED,
        EXECUTION_STARTED,
        EXECUTION_PROGRESS,
        EXECUTION_COMPLETED,
        EXECUTION_FAILED,
        EXECUTION_CANCELLED,
        EXECUTION_TIMEOUT,
        EXECUTION_RETRYING,
        DEPENDENCY_RESOLVED,
        DEPENDENCY_FAILED
    }
}
