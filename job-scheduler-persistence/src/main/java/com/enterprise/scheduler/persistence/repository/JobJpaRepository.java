package com.enterprise.scheduler.persistence.repository;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.persistence.entity.JobEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for JobEntity
 * Feature #5: Job Persistence, #48: Multi-Database Support
 */
@Repository
public interface JobJpaRepository extends JpaRepository<JobEntity, String> {

    /**
     * Find job by name and tenant
     */
    Optional<JobEntity> findByNameAndTenantId(String name, String tenantId);

    /**
     * Find all jobs by tenant
     */
    List<JobEntity> findByTenantId(String tenantId);

    /**
     * Find jobs by status
     */
    List<JobEntity> findByStatus(JobStatus status);

    /**
     * Find jobs by group
     */
    @Query("SELECT j FROM JobEntity j WHERE j.group = :group")
    List<JobEntity> findByGroup(@Param("group") String group);

    /**
     * Count jobs by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Find enabled jobs by status
     */
    List<JobEntity> findByEnabledAndStatus(Boolean enabled, JobStatus status);

    /**
     * Find jobs by tenant and status
     */
    List<JobEntity> findByTenantIdAndStatus(String tenantId, JobStatus status);

    /**
     * Find jobs by tag
     */
    @Query("SELECT j FROM JobEntity j JOIN j.tags t WHERE t = :tag")
    List<JobEntity> findByTag(@Param("tag") String tag);

    /**
     * Find jobs that have dependencies
     */
    @Query("SELECT j FROM JobEntity j WHERE SIZE(j.dependencies) > 0")
    List<JobEntity> findJobsWithDependencies();
}
