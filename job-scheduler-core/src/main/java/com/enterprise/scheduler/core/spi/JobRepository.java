package com.enterprise/scheduler.core.spi;

import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for job persistence
 * Feature #5: Job Persistence, #48: Multi-Database Support
 */
public interface JobRepository {

    /**
     * Save or update a job
     */
    Job save(Job job);

    /**
     * Find job by ID
     */
    Optional<Job> findById(String id);

    /**
     * Find job by name and tenant
     */
    Optional<Job> findByNameAndTenant(String name, String tenantId);

    /**
     * Find all jobs
     */
    List<Job> findAll();

    /**
     * Find jobs by tenant
     */
    List<Job> findByTenant(String tenantId);

    /**
     * Find jobs by status
     */
    List<Job> findByStatus(com.enterprise.scheduler.common.model.JobStatus status);

    /**
     * Find jobs by group
     */
    List<Job> findByGroup(String group);

    /**
     * Delete job by ID
     */
    void deleteById(String id);

    /**
     * Check if job exists
     */
    boolean existsById(String id);

    /**
     * Count jobs by tenant
     */
    long countByTenant(String tenantId);
}
