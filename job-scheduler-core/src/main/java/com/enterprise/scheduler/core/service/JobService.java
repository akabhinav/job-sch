package com.enterprise.scheduler.core.service;

import com.enterprise.scheduler.common.exception.JobNotFoundException;
import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.common.util.IdGenerator;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.event.JobEvent;
import com.enterprise.scheduler.core.spi.EventPublisher;
import com.enterprise.scheduler.core.spi.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

/**
 * Job service for managing job lifecycle
 * Features #1, #4, #6, #9
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final EventPublisher eventPublisher;
    private final DependencyResolver dependencyResolver;

    public Job createJob(Job job) {
        if (job.getId() == null) {
            job.setId(IdGenerator.generateJobId());
        }

        job.setStatus(JobStatus.SCHEDULED);
        job.setCreatedAt(Instant.now());
        job.setUpdatedAt(Instant.now());

        Job savedJob = jobRepository.save(job);

        eventPublisher.publishAsync(JobEvent.builder()
                .type(JobEvent.EventType.JOB_CREATED)
                .jobId(savedJob.getId())
                .tenantId(savedJob.getTenantId())
                .build());

        log.info("Created job: {} with ID: {}", savedJob.getName(), savedJob.getId());
        return savedJob;
    }

    public Job updateJob(String id, Job job) {
        Job existing = getJobById(id);

        job.setId(existing.getId());
        job.setCreatedAt(existing.getCreatedAt());
        job.setCreatedBy(existing.getCreatedBy());
        job.setUpdatedAt(Instant.now());
        job.setVersion(existing.getVersion() + 1);

        Job updated = jobRepository.save(job);

        eventPublisher.publishAsync(JobEvent.builder()
                .type(JobEvent.EventType.JOB_UPDATED)
                .jobId(updated.getId())
                .tenantId(updated.getTenantId())
                .build());

        return updated;
    }

    public Job getJobById(String id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> new JobNotFoundException(id));
    }

    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }

    public List<Job> getJobsByTenant(String tenantId) {
        return jobRepository.findByTenant(tenantId);
    }

    public void deleteJob(String id) {
        Job job = getJobById(id);
        jobRepository.deleteById(id);

        eventPublisher.publishAsync(JobEvent.builder()
                .type(JobEvent.EventType.JOB_DELETED)
                .jobId(id)
                .tenantId(job.getTenantId())
                .build());

        log.info("Deleted job: {}", id);
    }

    public Job enableJob(String id) {
        Job job = getJobById(id);
        job.setEnabled(true);
        job.setUpdatedAt(Instant.now());

        Job updated = jobRepository.save(job);

        eventPublisher.publishAsync(JobEvent.builder()
                .type(JobEvent.EventType.JOB_ENABLED)
                .jobId(id)
                .tenantId(job.getTenantId())
                .build());

        return updated;
    }

    public Job disableJob(String id) {
        Job job = getJobById(id);
        job.setEnabled(false);
        job.setUpdatedAt(Instant.now());

        Job updated = jobRepository.save(job);

        eventPublisher.publishAsync(JobEvent.builder()
                .type(JobEvent.EventType.JOB_DISABLED)
                .jobId(id)
                .tenantId(job.getTenantId())
                .build());

        return updated;
    }

    public boolean canExecute(String jobId) {
        Job job = getJobById(jobId);

        if (!job.canExecute()) {
            return false;
        }

        if (job.hasDependencies()) {
            return dependencyResolver.areDependenciesResolved(job);
        }

        return true;
    }
}
