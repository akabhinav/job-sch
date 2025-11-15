package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;
import java.util.List;
import java.util.Map;

/**
 * Workflow execution request
 * Feature #15: Workflow Orchestration
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Workflow execution request")
public class WorkflowRequest {

    @Schema(description = "Job IDs in workflow", required = true)
    @NotEmpty(message = "Job IDs are required")
    private List<String> jobIds;

    @Schema(description = "Workflow execution mode", example = "SEQUENTIAL")
    @Builder.Default
    private WorkflowExecutionMode mode = WorkflowExecutionMode.SEQUENTIAL;

    @Schema(description = "Shared workflow parameters")
    private Map<String, Object> parameters;

    @Schema(description = "Workflow context")
    private Map<String, Object> context;

    public enum WorkflowExecutionMode {
        SEQUENTIAL,
        PARALLEL,
        DAG
    }
}
