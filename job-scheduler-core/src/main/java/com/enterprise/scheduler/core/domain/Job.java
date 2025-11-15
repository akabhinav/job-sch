package com.enterprise.scheduler.core.domain;

import com.enterprise.scheduler.common.model.JobPriority;
import com.enterprise.scheduler.common.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Core Job domain model
 * Features #1, #4, #6, #8, #9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Job {

    /**
     * Unique job identifier
     */
    private String id;

    /**
     * Job name (must be unique per tenant)
     */
    private String name;

    /**
     * Job description
     */
    private String description;

    /**
     * Job type (e.g., "shell", "http", "java", "custom")
     * Feature #42: Custom Job Type Support
     */
    private String type;

    /**
     * Job group for organization
     */
    private String group;

    /**
     * Current job status
     * Feature #4: Job State Management
     */
    private JobStatus status;

    /**
     * Job priority
     * Feature #8: Job Priority Management
     */
    @Builder.Default
    private JobPriority priority = JobPriority.NORMAL;

    /**
     * Job version for versioning support
     * Feature #9: Job Versioning
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * Job configuration as key-value pairs
     * Feature #1: Job Definition & Configuration
     */
    @Builder.Default
    private Map<String, Object> configuration = new HashMap<>();

    /**
     * Job parameters (dynamic, runtime-resolved)
     * Feature #16: Dynamic Job Parameters
     */
    @Builder.Default
    private Map<String, Object> parameters = new HashMap<>();

    /**
     * Job dependencies (other job IDs this job depends on)
     * Feature #7: Job Dependency Management
     */
    private Set<String> dependencies;

    /**
     * Associated schedule
     */
    private Schedule schedule;

    /**
     * Retry configuration
     * Feature #19: Job Retry Mechanism
     */
    private RetryPolicy retryPolicy;

    /**
     * Timeout configuration in milliseconds
     * Feature #18: Job Timeout Management
     */
    private Long timeoutMs;

    /**
     * Maximum concurrent executions allowed
     * Feature #12: Parallel Job Execution
     */
    @Builder.Default
    private Integer maxConcurrentExecutions = 1;

    /**
     * Tenant ID for multi-tenancy
     * Feature #70: Multi-tenancy Support
     */
    private String tenantId;

    /**
     * Tags for categorization and search
     */
    private Set<String> tags;

    /**
     * Job metadata
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Creation timestamp
     */
    private Instant createdAt;

    /**
     * Last update timestamp
     */
    private Instant updatedAt;

    /**
     * Created by user
     */
    private String createdBy;

    /**
     * Last updated by user
     */
    private String updatedBy;

    /**
     * Whether job is enabled
     */
    @Builder.Default
    private boolean enabled = true;

    /**
     * Check if job can be executed based on dependencies
     */
    public boolean canExecute() {
        return enabled && status != JobStatus.PAUSED;
    }

    /**
     * Check if job has dependencies
     */
    public boolean hasDependencies() {
        return dependencies != null && !dependencies.isEmpty();
    }
}
