package com.enterprise.scheduler.api.controller;

import com.enterprise.scheduler.api.dto.*;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.Schedule;
import com.enterprise.scheduler.core.service.JobService;
import com.enterprise.scheduler.security.rbac.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * REST controller for schedule management
 * Feature #67: Schedule Management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/schedules")
@RequiredArgsConstructor
@Validated
@Tag(name = "Schedules", description = "Schedule management operations")
public class ScheduleController {

    private final JobService jobService;
    private final DtoMapper dtoMapper;

    @PostMapping("/jobs/{jobId}")
    @Operation(summary = "Add schedule to job", description = "Adds or updates a schedule for a job")
    @RequirePermission("schedule:update")
    public ResponseEntity<ApiResponse<ScheduleResponse>> addSchedule(
            @Parameter(description = "Job ID") @PathVariable String jobId,
            @Valid @RequestBody ScheduleRequest request) {
        log.info("Adding schedule to job: {}", jobId);

        Job job = jobService.getJobById(jobId);
        Schedule schedule = dtoMapper.toSchedule(request);
        job.setSchedule(schedule);

        Job updated = jobService.updateJob(jobId, job);
        ScheduleResponse response = dtoMapper.toScheduleResponse(updated.getSchedule());

        return ResponseEntity.ok(ApiResponse.success(response, "Schedule added successfully"));
    }

    @GetMapping("/jobs/{jobId}")
    @Operation(summary = "Get job schedule", description = "Retrieves the schedule for a specific job")
    @RequirePermission("schedule:read")
    public ResponseEntity<ApiResponse<ScheduleResponse>> getSchedule(
            @Parameter(description = "Job ID") @PathVariable String jobId) {
        log.debug("Getting schedule for job: {}", jobId);

        Job job = jobService.getJobById(jobId);
        if (job.getSchedule() == null) {
            return ResponseEntity.ok(ApiResponse.error("Job has no schedule"));
        }

        ScheduleResponse response = dtoMapper.toScheduleResponse(job.getSchedule());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/jobs/{jobId}")
    @Operation(summary = "Update job schedule", description = "Updates the schedule for a job")
    @RequirePermission("schedule:update")
    public ResponseEntity<ApiResponse<ScheduleResponse>> updateSchedule(
            @Parameter(description = "Job ID") @PathVariable String jobId,
            @Valid @RequestBody ScheduleRequest request) {
        log.info("Updating schedule for job: {}", jobId);

        Job job = jobService.getJobById(jobId);
        Schedule schedule = dtoMapper.toSchedule(request);

        if (job.getSchedule() != null) {
            schedule.setId(job.getSchedule().getId());
            schedule.setExecutionCount(job.getSchedule().getExecutionCount());
            schedule.setLastExecutionTime(job.getSchedule().getLastExecutionTime());
        }

        job.setSchedule(schedule);
        Job updated = jobService.updateJob(jobId, job);
        ScheduleResponse response = dtoMapper.toScheduleResponse(updated.getSchedule());

        return ResponseEntity.ok(ApiResponse.success(response, "Schedule updated successfully"));
    }

    @DeleteMapping("/jobs/{jobId}")
    @Operation(summary = "Remove job schedule", description = "Removes the schedule from a job")
    @RequirePermission("schedule:delete")
    public ResponseEntity<ApiResponse<Void>> removeSchedule(
            @Parameter(description = "Job ID") @PathVariable String jobId) {
        log.info("Removing schedule from job: {}", jobId);

        Job job = jobService.getJobById(jobId);
        job.setSchedule(null);
        jobService.updateJob(jobId, job);

        return ResponseEntity.ok(ApiResponse.success(null, "Schedule removed successfully"));
    }

    @PostMapping("/jobs/{jobId}/activate")
    @Operation(summary = "Activate schedule", description = "Activates a job schedule")
    @RequirePermission("schedule:update")
    public ResponseEntity<ApiResponse<ScheduleResponse>> activateSchedule(
            @Parameter(description = "Job ID") @PathVariable String jobId) {
        log.info("Activating schedule for job: {}", jobId);

        Job job = jobService.getJobById(jobId);
        if (job.getSchedule() != null) {
            job.getSchedule().setActive(true);
            Job updated = jobService.updateJob(jobId, job);
            ScheduleResponse response = dtoMapper.toScheduleResponse(updated.getSchedule());
            return ResponseEntity.ok(ApiResponse.success(response, "Schedule activated"));
        }

        return ResponseEntity.ok(ApiResponse.error("Job has no schedule"));
    }

    @PostMapping("/jobs/{jobId}/deactivate")
    @Operation(summary = "Deactivate schedule", description = "Deactivates a job schedule")
    @RequirePermission("schedule:update")
    public ResponseEntity<ApiResponse<ScheduleResponse>> deactivateSchedule(
            @Parameter(description = "Job ID") @PathVariable String jobId) {
        log.info("Deactivating schedule for job: {}", jobId);

        Job job = jobService.getJobById(jobId);
        if (job.getSchedule() != null) {
            job.getSchedule().setActive(false);
            Job updated = jobService.updateJob(jobId, job);
            ScheduleResponse response = dtoMapper.toScheduleResponse(updated.getSchedule());
            return ResponseEntity.ok(ApiResponse.success(response, "Schedule deactivated"));
        }

        return ResponseEntity.ok(ApiResponse.error("Job has no schedule"));
    }
}
