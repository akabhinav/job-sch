package com.enterprise.scheduler.api.dto;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.JobExecution;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Job execution response
 * Features #22, #66: Execution Management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job execution response")
public class JobExecutionResponse {

    @Schema(description = "Execution ID")
    private String id;

    @Schema(description = "Job ID")
    private String jobId;

    @Schema(description = "Job name")
    private String jobName;

    @Schema(description = "Execution status")
    private JobStatus status;

    @Schema(description = "Start time")
    private Instant startTime;

    @Schema(description = "End time")
    private Instant endTime;

    @Schema(description = "Duration")
    private Duration duration;

    @Schema(description = "Trigger type")
    private JobExecution.TriggerType triggerType;

    @Schema(description = "Triggered by")
    private String triggeredBy;

    @Schema(description = "Execution parameters")
    private Map<String, Object> parameters;

    @Schema(description = "Execution context")
    private Map<String, Object> context;

    @Schema(description = "Execution result")
    private Object result;

    @Schema(description = "Error message")
    private String errorMessage;

    @Schema(description = "Stack trace")
    private String stackTrace;

    @Schema(description = "Attempt number")
    private int attemptNumber;

    @Schema(description = "Node ID")
    private String nodeId;

    @Schema(description = "Process ID")
    private String processId;

    @Schema(description = "Thread name")
    private String threadName;

    @Schema(description = "Execution logs")
    private String logs;

    @Schema(description = "Log reference")
    private String logReference;

    @Schema(description = "Execution metrics")
    private Map<String, Object> metrics;

    @Schema(description = "Tenant ID")
    private String tenantId;
}
