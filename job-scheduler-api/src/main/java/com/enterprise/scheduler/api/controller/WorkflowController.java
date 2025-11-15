package com.enterprise.scheduler.api.controller;

import com.enterprise.scheduler.api.dto.*;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.service.WorkflowOrchestrator;
import com.enterprise.scheduler.security.rbac.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * REST controller for workflow orchestration
 * Feature #15: Workflow Orchestration
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/workflows")
@RequiredArgsConstructor
@Validated
@Tag(name = "Workflows", description = "Workflow orchestration operations")
public class WorkflowController {

    private final WorkflowOrchestrator workflowOrchestrator;
    private final DtoMapper dtoMapper;

    @PostMapping("/execute")
    @Operation(summary = "Execute workflow", description = "Executes a workflow of multiple jobs")
    @RequirePermission("workflow:execute")
    public ResponseEntity<ApiResponse<WorkflowResponse>> executeWorkflow(
            @Valid @RequestBody WorkflowRequest request) {
        log.info("Executing workflow with {} jobs in {} mode",
                request.getJobIds().size(), request.getMode());

        Instant startTime = Instant.now();
        CompletableFuture<List<JobExecution>> future;

        if (request.getMode() == WorkflowRequest.WorkflowExecutionMode.PARALLEL) {
            future = workflowOrchestrator.executeParallelWorkflow(request.getJobIds());
        } else {
            future = workflowOrchestrator.executeWorkflow(request.getJobIds());
        }

        try {
            List<JobExecution> executions = future.get();
            List<JobExecutionResponse> executionResponses = executions.stream()
                    .map(dtoMapper::toJobExecutionResponse)
                    .collect(Collectors.toList());

            long completedCount = executions.stream()
                    .filter(JobExecution::isTerminal)
                    .count();

            long failedCount = executions.stream()
                    .filter(e -> e.getStatus() == com.enterprise.scheduler.common.model.JobStatus.FAILED)
                    .count();

            WorkflowResponse response = WorkflowResponse.builder()
                    .workflowId(java.util.UUID.randomUUID().toString())
                    .executions(executionResponses)
                    .status(failedCount > 0 ? "PARTIALLY_COMPLETED" : "COMPLETED")
                    .startTime(startTime)
                    .endTime(Instant.now())
                    .totalJobs(executions.size())
                    .completedJobs((int) completedCount)
                    .failedJobs((int) failedCount)
                    .build();

            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(ApiResponse.success(response, "Workflow execution completed"));
        } catch (Exception e) {
            log.error("Error executing workflow", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Workflow execution failed: " + e.getMessage()));
        }
    }

    @PostMapping("/execute/sequential")
    @Operation(summary = "Execute sequential workflow", description = "Executes jobs sequentially based on dependencies")
    @RequirePermission("workflow:execute")
    public ResponseEntity<ApiResponse<WorkflowResponse>> executeSequentialWorkflow(
            @RequestBody List<String> jobIds) {
        log.info("Executing sequential workflow with {} jobs", jobIds.size());

        WorkflowRequest request = WorkflowRequest.builder()
                .jobIds(jobIds)
                .mode(WorkflowRequest.WorkflowExecutionMode.SEQUENTIAL)
                .build();

        return executeWorkflow(request);
    }

    @PostMapping("/execute/parallel")
    @Operation(summary = "Execute parallel workflow", description = "Executes jobs in parallel")
    @RequirePermission("workflow:execute")
    public ResponseEntity<ApiResponse<WorkflowResponse>> executeParallelWorkflow(
            @RequestBody List<String> jobIds) {
        log.info("Executing parallel workflow with {} jobs", jobIds.size());

        WorkflowRequest request = WorkflowRequest.builder()
                .jobIds(jobIds)
                .mode(WorkflowRequest.WorkflowExecutionMode.PARALLEL)
                .build();

        return executeWorkflow(request);
    }
}
