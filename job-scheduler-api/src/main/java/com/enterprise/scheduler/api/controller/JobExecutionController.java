package com.enterprise.scheduler.api.controller;

import com.enterprise.scheduler.api.dto.*;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.service.SchedulerService;
import com.enterprise.scheduler.core.spi.JobExecutionRepository;
import com.enterprise.scheduler.security.rbac.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST controller for job execution management
 * Features #22, #66: Execution Management and History
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/executions")
@RequiredArgsConstructor
@Validated
@Tag(name = "Job Executions", description = "Job execution management and history")
public class JobExecutionController {

    private final SchedulerService schedulerService;
    private final JobExecutionRepository executionRepository;
    private final DtoMapper dtoMapper;

    @PostMapping("/jobs/{jobId}/execute")
    @Operation(summary = "Execute a job manually", description = "Triggers manual execution of a job")
    @RequirePermission("execution:trigger")
    public ResponseEntity<ApiResponse<JobExecutionResponse>> executeJob(
            @Parameter(description = "Job ID") @PathVariable String jobId,
            @Valid @RequestBody(required = false) JobExecutionRequest request) {
        log.info("Manually executing job: {}", jobId);

        Map<String, Object> parameters = request != null ? request.getParameters() : null;
        JobExecution execution = schedulerService.executeManually(jobId, parameters);
        JobExecutionResponse response = dtoMapper.toJobExecutionResponse(execution);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, "Job execution started"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get execution by ID", description = "Retrieves a job execution by its ID")
    @RequirePermission("execution:read")
    public ResponseEntity<ApiResponse<JobExecutionResponse>> getExecution(
            @Parameter(description = "Execution ID") @PathVariable String id) {
        log.debug("Getting execution: {}", id);

        JobExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + id));
        JobExecutionResponse response = dtoMapper.toJobExecutionResponse(execution);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all executions", description = "Retrieves all job executions")
    @RequirePermission("execution:read")
    public ResponseEntity<ApiResponse<List<JobExecutionResponse>>> getAllExecutions(
            @Parameter(description = "Filter by job ID") @RequestParam(required = false) String jobId,
            @Parameter(description = "Limit results") @RequestParam(defaultValue = "100") int limit) {
        log.debug("Getting executions for job: {}", jobId);

        List<JobExecution> executions = jobId != null
                ? executionRepository.findByJobId(jobId, limit)
                : executionRepository.findAll(limit);

        List<JobExecutionResponse> responses = executions.stream()
                .map(dtoMapper::toJobExecutionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @GetMapping("/jobs/{jobId}/history")
    @Operation(summary = "Get job execution history", description = "Retrieves execution history for a specific job")
    @RequirePermission("execution:read")
    public ResponseEntity<ApiResponse<List<JobExecutionResponse>>> getJobHistory(
            @Parameter(description = "Job ID") @PathVariable String jobId,
            @Parameter(description = "Limit results") @RequestParam(defaultValue = "50") int limit) {
        log.debug("Getting execution history for job: {}", jobId);

        List<JobExecution> executions = executionRepository.findByJobId(jobId, limit);
        List<JobExecutionResponse> responses = executions.stream()
                .map(dtoMapper::toJobExecutionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel execution", description = "Cancels a running job execution")
    @RequirePermission("execution:cancel")
    public ResponseEntity<ApiResponse<JobExecutionResponse>> cancelExecution(
            @Parameter(description = "Execution ID") @PathVariable String id) {
        log.info("Cancelling execution: {}", id);

        // Implementation would call JobExecutionEngine.cancelExecution(id)
        JobExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + id));

        JobExecutionResponse response = dtoMapper.toJobExecutionResponse(execution);

        return ResponseEntity.ok(ApiResponse.success(response, "Execution cancellation requested"));
    }

    @GetMapping("/{id}/logs")
    @Operation(summary = "Get execution logs", description = "Retrieves logs for a job execution")
    @RequirePermission("execution:read")
    public ResponseEntity<ApiResponse<String>> getExecutionLogs(
            @Parameter(description = "Execution ID") @PathVariable String id) {
        log.debug("Getting logs for execution: {}", id);

        JobExecution execution = executionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Execution not found: " + id));

        String logs = execution.getLogs() != null
                ? execution.getLogs()
                : "No logs available";

        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/running")
    @Operation(summary = "Get running executions", description = "Retrieves all currently running executions")
    @RequirePermission("execution:read")
    public ResponseEntity<ApiResponse<List<JobExecutionResponse>>> getRunningExecutions() {
        log.debug("Getting running executions");

        List<JobExecution> executions = executionRepository.findRunning();
        List<JobExecutionResponse> responses = executions.stream()
                .map(dtoMapper::toJobExecutionResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }
}
