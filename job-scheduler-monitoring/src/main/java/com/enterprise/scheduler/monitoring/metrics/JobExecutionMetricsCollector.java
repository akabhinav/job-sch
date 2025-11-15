package com.enterprise.scheduler.monitoring.metrics;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.JobExecution;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Collects metrics for job executions
 * Feature #23: Job Metrics Collection
 * Feature #22: Execution Monitoring
 */
@Slf4j
@Component
public class JobExecutionMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final ConcurrentMap<String, Timer.Sample> activeSamples;

    // Metrics
    private final Counter jobExecutionsTotal;
    private final Counter jobExecutionsSuccessful;
    private final Counter jobExecutionsFailed;
    private final Counter jobExecutionsTimeout;
    private final Counter jobExecutionsRetried;

    public JobExecutionMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.activeSamples = new ConcurrentHashMap<>();

        // Initialize counters
        this.jobExecutionsTotal = Counter.builder("job.executions.total")
                .description("Total number of job executions")
                .register(meterRegistry);

        this.jobExecutionsSuccessful = Counter.builder("job.executions.successful")
                .description("Number of successful job executions")
                .register(meterRegistry);

        this.jobExecutionsFailed = Counter.builder("job.executions.failed")
                .description("Number of failed job executions")
                .register(meterRegistry);

        this.jobExecutionsTimeout = Counter.builder("job.executions.timeout")
                .description("Number of job executions that timed out")
                .register(meterRegistry);

        this.jobExecutionsRetried = Counter.builder("job.executions.retried")
                .description("Number of job executions that were retried")
                .register(meterRegistry);
    }

    /**
     * Record job execution start
     */
    public void recordExecutionStart(JobExecution execution) {
        log.debug("Recording execution start for job: {}, execution: {}",
                execution.getJobName(), execution.getId());

        jobExecutionsTotal.increment();

        // Start timing
        Timer.Sample sample = Timer.start(meterRegistry);
        activeSamples.put(execution.getId(), sample);

        // Record gauge for active executions
        meterRegistry.gauge("job.executions.active",
                activeSamples.size());
    }

    /**
     * Record job execution completion
     */
    public void recordExecutionComplete(JobExecution execution) {
        log.debug("Recording execution completion for job: {}, execution: {}, status: {}",
                execution.getJobName(), execution.getId(), execution.getStatus());

        // Stop timing
        Timer.Sample sample = activeSamples.remove(execution.getId());
        if (sample != null) {
            sample.stop(Timer.builder("job.execution.duration")
                    .description("Job execution duration")
                    .tag("job_name", execution.getJobName())
                    .tag("job_type", execution.getJobId())
                    .tag("status", execution.getStatus().name())
                    .tag("trigger_type", execution.getTriggerType().name())
                    .register(meterRegistry));
        }

        // Record status-specific metrics
        switch (execution.getStatus()) {
            case COMPLETED:
                jobExecutionsSuccessful.increment();
                Counter.builder("job.executions.by_job.successful")
                        .tag("job_name", execution.getJobName())
                        .register(meterRegistry)
                        .increment();
                break;

            case FAILED:
                jobExecutionsFailed.increment();
                Counter.builder("job.executions.by_job.failed")
                        .tag("job_name", execution.getJobName())
                        .register(meterRegistry)
                        .increment();
                break;

            case TIMEOUT:
                jobExecutionsTimeout.increment();
                Counter.builder("job.executions.by_job.timeout")
                        .tag("job_name", execution.getJobName())
                        .register(meterRegistry)
                        .increment();
                break;

            default:
                log.warn("Unexpected terminal status: {}", execution.getStatus());
        }

        // Record retry metrics
        if (execution.getAttemptNumber() > 1) {
            jobExecutionsRetried.increment();
        }

        // Update active executions gauge
        meterRegistry.gauge("job.executions.active", activeSamples.size());

        // Record custom metrics from execution
        if (execution.getMetrics() != null) {
            execution.getMetrics().forEach((key, value) -> {
                if (value instanceof Number) {
                    meterRegistry.gauge("job.custom.metric." + key,
                            execution,
                            exec -> ((Number) value).doubleValue());
                }
            });
        }
    }

    /**
     * Record job execution duration manually
     */
    public void recordExecutionDuration(String jobName, Duration duration, JobStatus status) {
        Timer.builder("job.execution.duration")
                .description("Job execution duration")
                .tag("job_name", jobName)
                .tag("status", status.name())
                .register(meterRegistry)
                .record(duration);
    }

    /**
     * Record job failure with error type
     */
    public void recordExecutionError(String jobName, String errorType) {
        Counter.builder("job.executions.errors")
                .description("Job execution errors by type")
                .tag("job_name", jobName)
                .tag("error_type", errorType)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Get success rate for a job
     */
    public double getSuccessRate(String jobName) {
        double successful = Counter.builder("job.executions.by_job.successful")
                .tag("job_name", jobName)
                .register(meterRegistry)
                .count();

        double failed = Counter.builder("job.executions.by_job.failed")
                .tag("job_name", jobName)
                .register(meterRegistry)
                .count();

        double total = successful + failed;
        return total > 0 ? (successful / total) * 100 : 0;
    }

    /**
     * Get failure rate for a job
     */
    public double getFailureRate(String jobName) {
        return 100 - getSuccessRate(jobName);
    }

    /**
     * Clean up stale samples (for executions that didn't complete properly)
     */
    public void cleanupStaleSamples() {
        log.debug("Cleaning up {} stale timing samples", activeSamples.size());
        activeSamples.clear();
    }
}
