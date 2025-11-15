package com.enterprise.scheduler.core.spi;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.JobExecution;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for job execution persistence
 * Feature #22: Job Execution History
 */
public interface JobExecutionRepository {

    /**
     * Save or update a job execution
     */
    JobExecution save(JobExecution execution);

    /**
     * Find execution by ID
     */
    Optional<JobExecution> findById(String id);

    /**
     * Find executions by job ID
     */
    List<JobExecution> findByJobId(String jobId);

    /**
     * Find executions by job ID with pagination
     */
    List<JobExecution> findByJobId(String jobId, int page, int size);

    /**
     * Find executions by status
     */
    List<JobExecution> findByStatus(JobStatus status);

    /**
     * Find running executions
     */
    List<JobExecution> findRunning();

    /**
     * Find executions in time range
     */
    List<JobExecution> findByTimeRange(Instant start, Instant end);

    /**
     * Find latest execution for a job
     */
    Optional<JobExecution> findLatestByJobId(String jobId);

    /**
     * Delete old executions
     */
    int deleteOlderThan(Instant timestamp);

    /**
     * Count executions by job ID
     */
    long countByJobId(String jobId);
}
