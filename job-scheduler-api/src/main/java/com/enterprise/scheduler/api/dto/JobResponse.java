package com.enterprise.scheduler.api.dto;

import com.enterprise.scheduler.common.model.JobPriority;
import com.enterprise.scheduler.common.model.JobStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Job response
 * Features #1, #61: Job CRUD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job response")
public class JobResponse {

    @Schema(description = "Job ID", example = "job-123456")
    private String id;

    @Schema(description = "Job name", example = "daily-backup-job")
    private String name;

    @Schema(description = "Job description")
    private String description;

    @Schema(description = "Job type", example = "shell")
    private String type;

    @Schema(description = "Job group")
    private String group;

    @Schema(description = "Job status")
    private JobStatus status;

    @Schema(description = "Job priority")
    private JobPriority priority;

    @Schema(description = "Job version")
    private Integer version;

    @Schema(description = "Job configuration")
    private Map<String, Object> configuration;

    @Schema(description = "Job parameters")
    private Map<String, Object> parameters;

    @Schema(description = "Job dependencies")
    private Set<String> dependencies;

    @Schema(description = "Schedule configuration")
    private ScheduleResponse schedule;

    @Schema(description = "Retry policy")
    private RetryPolicyResponse retryPolicy;

    @Schema(description = "Timeout in milliseconds")
    private Long timeoutMs;

    @Schema(description = "Maximum concurrent executions")
    private Integer maxConcurrentExecutions;

    @Schema(description = "Tenant ID")
    private String tenantId;

    @Schema(description = "Job tags")
    private Set<String> tags;

    @Schema(description = "Job metadata")
    private Map<String, String> metadata;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Update timestamp")
    private Instant updatedAt;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Updated by")
    private String updatedBy;

    @Schema(description = "Is job enabled")
    private boolean enabled;
}
