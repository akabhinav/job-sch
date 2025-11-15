package com.enterprise.scheduler.persistence.repository;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.persistence.entity.JobExecutionEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for JobExecutionEntity
 * Feature #22: Job Execution History
 */
@Repository
public interface JobExecutionJpaRepository extends JpaRepository<JobExecutionEntity, String> {

    /**
     * Find executions by job ID
     */
    List<JobExecutionEntity> findByJobId(String jobId);

    /**
     * Find executions by job ID with pagination
     */
    List<JobExecutionEntity> findByJobIdOrderByStartTimeDesc(String jobId, Pageable pageable);

    /**
     * Find executions by status
     */
    List<JobExecutionEntity> findByStatus(JobStatus status);

    /**
     * Find running executions
     */
    @Query("SELECT e FROM JobExecutionEntity e WHERE e.status = 'RUNNING'")
    List<JobExecutionEntity> findRunning();

    /**
     * Find executions in time range
     */
    @Query("SELECT e FROM JobExecutionEntity e WHERE e.startTime >= :start AND e.startTime <= :end ORDER BY e.startTime DESC")
    List<JobExecutionEntity> findByTimeRange(@Param("start") Instant start, @Param("end") Instant end);

    /**
     * Find latest execution for a job
     */
    @Query("SELECT e FROM JobExecutionEntity e WHERE e.jobId = :jobId ORDER BY e.startTime DESC LIMIT 1")
    Optional<JobExecutionEntity> findLatestByJobId(@Param("jobId") String jobId);

    /**
     * Delete executions older than timestamp
     */
    @Modifying
    @Query("DELETE FROM JobExecutionEntity e WHERE e.startTime < :timestamp")
    int deleteByStartTimeBefore(@Param("timestamp") Instant timestamp);

    /**
     * Count executions by job ID
     */
    long countByJobId(String jobId);

    /**
     * Find executions by tenant
     */
    List<JobExecutionEntity> findByTenantId(String tenantId);

    /**
     * Find executions by job ID and status
     */
    List<JobExecutionEntity> findByJobIdAndStatus(String jobId, JobStatus status);

    /**
     * Find executions by node ID
     */
    List<JobExecutionEntity> findByNodeId(String nodeId);

    /**
     * Count running executions for a job
     */
    @Query("SELECT COUNT(e) FROM JobExecutionEntity e WHERE e.jobId = :jobId AND e.status = 'RUNNING'")
    long countRunningByJobId(@Param("jobId") String jobId);

    /**
     * Find failed executions that need retry
     */
    @Query("SELECT e FROM JobExecutionEntity e WHERE e.status = 'FAILED' AND e.jobId IN " +
           "(SELECT j.id FROM JobEntity j WHERE j.retryPolicy.maxAttempts > e.attemptNumber)")
    List<JobExecutionEntity> findFailedExecutionsForRetry();
}
