package com.enterprise.scheduler.core.service;

import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.domain.Schedule;
import com.enterprise.scheduler.core.engine.JobExecutionEngine;
import com.enterprise.scheduler.core.spi.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Scheduler service for managing job schedules
 * Features #3, #13, #14, #15
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SchedulerService {

    private final JobRepository jobRepository;
    private final JobExecutionEngine executionEngine;
    private final DependencyResolver dependencyResolver;
    private final RetryHandler retryHandler;

    /**
     * Check and execute scheduled jobs every minute
     */
    @Scheduled(fixedDelay = 60000)
    public void checkScheduledJobs() {
        List<Job> scheduledJobs = jobRepository.findAll().stream()
                .filter(Job::isEnabled)
                .filter(job -> job.getSchedule() != null)
                .filter(job -> job.getSchedule().isValid())
                .filter(this::isReadyForExecution)
                .toList();

        List<Job> executableJobs = dependencyResolver.getExecutableJobs(scheduledJobs);

        for (Job job : executableJobs) {
            try {
                scheduleJob(job);
            } catch (Exception e) {
                log.error("Error scheduling job: {}", job.getId(), e);
            }
        }
    }

    public JobExecution scheduleJob(Job job) {
        log.info("Scheduling job: {} ({})", job.getName(), job.getId());

        JobExecution execution = executionEngine.executeJob(
                job,
                job.getParameters(),
                JobExecution.TriggerType.SCHEDULED
        );

        if (job.getSchedule() != null) {
            job.getSchedule().incrementExecutionCount();
            job.getSchedule().setLastExecutionTime(Instant.now());
            calculateNextExecutionTime(job.getSchedule());
            jobRepository.save(job);
        }

        return execution;
    }

    public JobExecution executeManually(String jobId, Map<String, Object> parameters) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new RuntimeException("Job not found: " + jobId));

        return executionEngine.executeJob(
                job,
                parameters,
                JobExecution.TriggerType.MANUAL
        );
    }

    public void executeChain(List<String> jobIds) {
        List<String> orderedJobs = orderJobsByDependencies(jobIds);

        for (String jobId : orderedJobs) {
            Job job = jobRepository.findById(jobId).orElseThrow();
            scheduleJob(job);
        }
    }

    private boolean isReadyForExecution(Job job) {
        Schedule schedule = job.getSchedule();
        if (schedule.getNextExecutionTime() == null) {
            calculateNextExecutionTime(schedule);
        }

        return schedule.getNextExecutionTime() != null &&
                schedule.getNextExecutionTime().isBefore(Instant.now());
    }

    private void calculateNextExecutionTime(Schedule schedule) {
        // Simplified - production would use Quartz CronExpression
        if (schedule.getType() == com.enterprise.scheduler.common.model.ScheduleType.INTERVAL) {
            Instant next = Instant.now().plusMillis(schedule.getIntervalMs());
            schedule.setNextExecutionTime(next);
        } else if (schedule.getType() == com.enterprise.scheduler.common.model.ScheduleType.CRON) {
            // Would use CronExpression parser here
            schedule.setNextExecutionTime(Instant.now().plusSeconds(3600));
        }
    }

    private List<String> orderJobsByDependencies(List<String> jobIds) {
        List<Job> jobs = jobIds.stream()
                .map(id -> jobRepository.findById(id).orElseThrow())
                .toList();

        return dependencyResolver.topologicalSort(jobs);
    }
}
