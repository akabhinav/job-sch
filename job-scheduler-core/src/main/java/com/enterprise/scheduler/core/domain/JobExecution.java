package com.enterprise.scheduler.core.domain;

import com.enterprise.scheduler.common.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Job execution instance
 * Features #2, #4, #17, #22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecution {

    /**
     * Unique execution identifier
     */
    private String id;

    /**
     * Associated job ID
     */
    private String jobId;

    /**
     * Job name (denormalized for performance)
     */
    private String jobName;

    /**
     * Execution status
     */
    private JobStatus status;

    /**
     * Start time
     */
    private Instant startTime;

    /**
     * End time
     */
    private Instant endTime;

    /**
     * Execution duration
     */
    private Duration duration;

    /**
     * Trigger type (SCHEDULED, MANUAL, DEPENDENCY, EVENT)
     */
    private TriggerType triggerType;

    /**
     * Triggered by (user ID or system)
     */
    private String triggeredBy;

    /**
     * Job parameters used for this execution
     * Feature #16: Dynamic Job Parameters
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * Execution context for data passing between jobs
     * Feature #17: Job Context & Data Passing
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    /**
     * Execution result/output
     */
    private Object result;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * Stack trace if failed
     */
    private String stackTrace;

    /**
     * Retry attempt number
     */
    @Builder.Default
    private int attemptNumber = 1;

    /**
     * Node ID where job was executed
     * Feature #34: Job Distribution
     */
    private String nodeId;

    /**
     * Process ID
     */
    private String processId;

    /**
     * Thread name
     */
    private String threadName;

    /**
     * Execution logs (inline, for small logs)
     */
    private String logs;

    /**
     * External log reference (for large logs)
     */
    private String logReference;

    /**
     * Execution metrics
     * Feature #23: Job Metrics Collection
     */
    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    /**
     * Tenant ID
     */
    private String tenantId;

    /**
     * Check if execution is terminal
     */
    public boolean isTerminal() {
        return status != null && status.isTerminal();
    }

    /**
     * Check if execution is running
     */
    public boolean isRunning() {
        return status == JobStatus.RUNNING;
    }

    /**
     * Calculate duration if not set
     */
    public Duration getDuration() {
        if (duration != null) {
            return duration;
        }
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime);
        }
        if (startTime != null && status == JobStatus.RUNNING) {
            return Duration.between(startTime, Instant.now());
        }
        return Duration.ZERO;
    }

    public enum TriggerType {
        SCHEDULED,
        MANUAL,
        DEPENDENCY,
        EVENT,
        RETRY,
        WEBHOOK,
        API
    }
}
