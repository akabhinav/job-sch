package com.enterprise.scheduler.api.dto;

import com.enterprise.scheduler.common.model.JobPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Map;
import java.util.Set;

/**
 * Job creation/update request
 * Features #1, #61: Job CRUD
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job creation/update request")
public class JobRequest {

    @Schema(description = "Job name", example = "daily-backup-job", required = true)
    @NotBlank(message = "Job name is required")
    private String name;

    @Schema(description = "Job description", example = "Daily database backup")
    private String description;

    @Schema(description = "Job type", example = "shell", required = true)
    @NotBlank(message = "Job type is required")
    private String type;

    @Schema(description = "Job group", example = "backup-jobs")
    private String group;

    @Schema(description = "Job priority", example = "HIGH")
    private JobPriority priority;

    @Schema(description = "Job configuration")
    private Map<String, Object> configuration;

    @Schema(description = "Job parameters")
    private Map<String, Object> parameters;

    @Schema(description = "Job dependencies (job IDs)")
    private Set<String> dependencies;

    @Schema(description = "Schedule configuration")
    private ScheduleRequest schedule;

    @Schema(description = "Retry policy")
    private RetryPolicyRequest retryPolicy;

    @Schema(description = "Timeout in milliseconds", example = "300000")
    private Long timeoutMs;

    @Schema(description = "Maximum concurrent executions", example = "1")
    private Integer maxConcurrentExecutions;

    @Schema(description = "Job tags")
    private Set<String> tags;

    @Schema(description = "Job metadata")
    private Map<String, String> metadata;

    @Schema(description = "Whether job is enabled", example = "true")
    private Boolean enabled;
}
