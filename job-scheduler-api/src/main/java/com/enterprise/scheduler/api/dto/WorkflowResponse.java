package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/**
 * Workflow execution response
 * Feature #15: Workflow Orchestration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workflow execution response")
public class WorkflowResponse {

    @Schema(description = "Workflow ID")
    private String workflowId;

    @Schema(description = "Job executions in workflow")
    private List<JobExecutionResponse> executions;

    @Schema(description = "Workflow status")
    private String status;

    @Schema(description = "Start time")
    private Instant startTime;

    @Schema(description = "End time")
    private Instant endTime;

    @Schema(description = "Total jobs")
    private int totalJobs;

    @Schema(description = "Completed jobs")
    private int completedJobs;

    @Schema(description = "Failed jobs")
    private int failedJobs;
}
