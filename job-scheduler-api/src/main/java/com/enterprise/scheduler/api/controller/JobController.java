package com.enterprise.scheduler.api.controller;

import com.enterprise.scheduler.api.dto.*;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.service.JobService;
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
import java.util.stream.Collectors;

/**
 * REST controller for job management
 * Features #1, #61: Job CRUD Operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Validated
@Tag(name = "Jobs", description = "Job management operations")
public class JobController {

    private final JobService jobService;
    private final DtoMapper dtoMapper;

    @PostMapping
    @Operation(summary = "Create a new job", description = "Creates a new job with the specified configuration")
    @RequirePermission("job:create")
    public ResponseEntity<ApiResponse<JobResponse>> createJob(
            @Valid @RequestBody JobRequest request) {
        log.info("Creating job: {}", request.getName());

        Job job = dtoMapper.toJob(request);
        Job created = jobService.createJob(job);
        JobResponse response = dtoMapper.toJobResponse(created);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Job created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID", description = "Retrieves a job by its unique identifier")
    @RequirePermission("job:read")
    public ResponseEntity<ApiResponse<JobResponse>> getJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        log.debug("Getting job: {}", id);

        Job job = jobService.getJobById(id);
        JobResponse response = dtoMapper.toJobResponse(job);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all jobs", description = "Retrieves all jobs (tenant-scoped)")
    @RequirePermission("job:read")
    public ResponseEntity<ApiResponse<List<JobResponse>>> getAllJobs(
            @Parameter(description = "Filter by tenant ID") @RequestParam(required = false) String tenantId) {
        log.debug("Getting all jobs for tenant: {}", tenantId);

        List<Job> jobs = tenantId != null
                ? jobService.getJobsByTenant(tenantId)
                : jobService.getAllJobs();

        List<JobResponse> responses = jobs.stream()
                .map(dtoMapper::toJobResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a job", description = "Updates an existing job")
    @RequirePermission("job:update")
    public ResponseEntity<ApiResponse<JobResponse>> updateJob(
            @Parameter(description = "Job ID") @PathVariable String id,
            @Valid @RequestBody JobRequest request) {
        log.info("Updating job: {}", id);

        Job job = dtoMapper.toJob(request);
        Job updated = jobService.updateJob(id, job);
        JobResponse response = dtoMapper.toJobResponse(updated);

        return ResponseEntity.ok(ApiResponse.success(response, "Job updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a job", description = "Deletes a job by its ID")
    @RequirePermission("job:delete")
    public ResponseEntity<ApiResponse<Void>> deleteJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        log.info("Deleting job: {}", id);

        jobService.deleteJob(id);

        return ResponseEntity.ok(ApiResponse.success(null, "Job deleted successfully"));
    }

    @PostMapping("/{id}/enable")
    @Operation(summary = "Enable a job", description = "Enables a job for execution")
    @RequirePermission("job:update")
    public ResponseEntity<ApiResponse<JobResponse>> enableJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        log.info("Enabling job: {}", id);

        Job job = jobService.enableJob(id);
        JobResponse response = dtoMapper.toJobResponse(job);

        return ResponseEntity.ok(ApiResponse.success(response, "Job enabled successfully"));
    }

    @PostMapping("/{id}/disable")
    @Operation(summary = "Disable a job", description = "Disables a job from execution")
    @RequirePermission("job:update")
    public ResponseEntity<ApiResponse<JobResponse>> disableJob(
            @Parameter(description = "Job ID") @PathVariable String id) {
        log.info("Disabling job: {}", id);

        Job job = jobService.disableJob(id);
        JobResponse response = dtoMapper.toJobResponse(job);

        return ResponseEntity.ok(ApiResponse.success(response, "Job disabled successfully"));
    }

    @GetMapping("/{id}/can-execute")
    @Operation(summary = "Check if job can execute", description = "Checks if a job can be executed")
    @RequirePermission("job:read")
    public ResponseEntity<ApiResponse<Boolean>> canExecute(
            @Parameter(description = "Job ID") @PathVariable String id) {
        log.debug("Checking if job can execute: {}", id);

        boolean canExecute = jobService.canExecute(id);

        return ResponseEntity.ok(ApiResponse.success(canExecute));
    }
}
