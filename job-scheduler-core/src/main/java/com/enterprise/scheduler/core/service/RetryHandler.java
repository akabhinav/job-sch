package com.enterprise.scheduler.core.service;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.domain.RetryPolicy;
import com.enterprise.scheduler.core.engine.JobExecutionEngine;
import com.enterprise.scheduler.core.spi.JobExecutionRepository;
import com.enterprise.scheduler.core.spi.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * Retry handler for failed jobs
 * Feature #19: Job Retry Mechanism
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RetryHandler {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;
    private final JobExecutionEngine executionEngine;

    @Scheduled(fixedDelay = 30000) // Check every 30 seconds
    public void processRetries() {
        List<JobExecution> failedExecutions = executionRepository.findByStatus(JobStatus.FAILED);

        for (JobExecution execution : failedExecutions) {
            try {
                processRetry(execution);
            } catch (Exception e) {
                log.error("Error processing retry for execution: {}", execution.getId(), e);
            }
        }
    }

    public void processRetry(JobExecution execution) {
        Job job = jobRepository.findById(execution.getJobId()).orElse(null);
        if (job == null || job.getRetryPolicy() == null) {
            return;
        }

        RetryPolicy policy = job.getRetryPolicy();

        if (!shouldRetry(execution, policy)) {
            log.info("Execution {} not eligible for retry", execution.getId());
            return;
        }

        long delay = policy.calculateDelay(execution.getAttemptNumber());
        Instant retryTime = execution.getEndTime().plusMillis(delay);

        if (Instant.now().isAfter(retryTime)) {
            log.info("Retrying execution {} (attempt {})", execution.getId(), execution.getAttemptNumber() + 1);

            execution.setStatus(JobStatus.RETRYING);
            executionRepository.save(execution);

            JobExecution newExecution = executionEngine.executeJob(
                    job,
                    execution.getParameters(),
                    JobExecution.TriggerType.RETRY
            );
            newExecution.setAttemptNumber(execution.getAttemptNumber() + 1);
            executionRepository.save(newExecution);
        }
    }

    private boolean shouldRetry(JobExecution execution, RetryPolicy policy) {
        if (execution.getAttemptNumber() >= policy.getMaxAttempts()) {
            return false;
        }

        if (execution.getStatus() == JobStatus.TIMEOUT && !policy.isRetryOnTimeout()) {
            return false;
        }

        return execution.getStatus() == JobStatus.FAILED ||
               (execution.getStatus() == JobStatus.TIMEOUT && policy.isRetryOnTimeout());
    }
}
