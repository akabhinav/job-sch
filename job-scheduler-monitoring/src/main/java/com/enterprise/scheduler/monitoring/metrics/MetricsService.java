package com.enterprise.scheduler.monitoring.metrics;

import com.enterprise.scheduler.core.domain.JobExecution;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Central service for metrics collection and management
 * Feature #21: Metrics Collection Infrastructure
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final JobExecutionMetricsCollector jobExecutionMetrics;
    private final SystemMetricsCollector systemMetrics;
    private final CustomJobMetricsCollector customMetrics;
    private final MeterRegistry meterRegistry;

    /**
     * Record job execution start
     */
    public void recordJobStart(JobExecution execution) {
        log.debug("Recording job start: {}", execution.getJobName());
        jobExecutionMetrics.recordExecutionStart(execution);

        // Record trigger type
        switch (execution.getTriggerType()) {
            case SCHEDULED:
                customMetrics.recordScheduledTrigger();
                break;
            case MANUAL:
                customMetrics.recordManualTrigger();
                break;
            case DEPENDENCY:
                customMetrics.recordDependencyTrigger();
                break;
            case EVENT:
                customMetrics.recordEventTrigger();
                break;
        }

        // Record parameter count
        if (execution.getParameters() != null) {
            customMetrics.recordParameterCount(
                    execution.getJobName(),
                    execution.getParameters().size()
            );
        }
    }

    /**
     * Record job execution completion
     */
    public void recordJobComplete(JobExecution execution) {
        log.debug("Recording job completion: {} - {}",
                execution.getJobName(), execution.getStatus());
        jobExecutionMetrics.recordExecutionComplete(execution);

        // Record execution size if available
        if (execution.getResult() != null) {
            String result = execution.getResult().toString();
            customMetrics.recordExecutionSize(result.length());
        }
    }

    /**
     * Record job failure
     */
    public void recordJobFailure(String jobName, String errorType) {
        log.debug("Recording job failure: {} - {}", jobName, errorType);
        jobExecutionMetrics.recordExecutionError(jobName, errorType);
    }

    /**
     * Record misfired job
     */
    public void recordMisfiredJob(String jobName) {
        log.debug("Recording misfired job: {}", jobName);
        customMetrics.recordMisfiredJob(jobName);
    }

    /**
     * Record dead letter job
     */
    public void recordDeadLetterJob(String jobName) {
        log.warn("Recording dead letter job: {}", jobName);
        customMetrics.recordDeadLetterJob(jobName);
    }

    /**
     * Record workflow execution
     */
    public void recordWorkflowExecution(String workflowName, int jobCount) {
        log.debug("Recording workflow execution: {} with {} jobs",
                workflowName, jobCount);
        customMetrics.recordWorkflowExecution(workflowName, jobCount);
    }

    /**
     * Update job counts by status
     */
    public void updateJobCounts(Map<String, Integer> counts) {
        counts.forEach((status, count) -> {
            log.trace("Updating job count for {}: {}", status, count);
            customMetrics.setJobCount(status, count);
        });
    }

    /**
     * Get current metrics snapshot
     */
    public MetricsSnapshot getMetricsSnapshot() {
        MetricsSnapshot snapshot = new MetricsSnapshot();

        // System metrics
        snapshot.heapUsagePercent = systemMetrics.getHeapUsagePercentage();
        snapshot.threadPoolUtilization = systemMetrics.getThreadPoolUtilization();
        snapshot.isHighLoad = systemMetrics.isHighLoad();

        // Job execution metrics
        snapshot.totalExecutions = jobExecutionMetrics.jobExecutionsTotal.count();
        snapshot.successfulExecutions = jobExecutionMetrics.jobExecutionsSuccessful.count();
        snapshot.failedExecutions = jobExecutionMetrics.jobExecutionsFailed.count();
        snapshot.timeoutExecutions = jobExecutionMetrics.jobExecutionsTimeout.count();
        snapshot.retriedExecutions = jobExecutionMetrics.jobExecutionsRetried.count();

        // Custom metrics
        snapshot.misfiredJobs = customMetrics.getMisfiredJobsCount();
        snapshot.deadLetterJobs = customMetrics.getDeadLetterJobsCount();

        return snapshot;
    }

    /**
     * Get metrics for specific job
     */
    public JobMetrics getJobMetrics(String jobName) {
        JobMetrics metrics = new JobMetrics();
        metrics.jobName = jobName;
        metrics.successRate = jobExecutionMetrics.getSuccessRate(jobName);
        metrics.failureRate = jobExecutionMetrics.getFailureRate(jobName);
        return metrics;
    }

    /**
     * Record custom metric
     */
    public void recordCustomMetric(String metricName, String jobName, double value) {
        log.debug("Recording custom metric: {} for job: {} = {}",
                metricName, jobName, value);
        customMetrics.recordCustomMetric(metricName, jobName, value);
    }

    /**
     * Metrics snapshot
     */
    public static class MetricsSnapshot {
        public double heapUsagePercent;
        public double threadPoolUtilization;
        public boolean isHighLoad;
        public double totalExecutions;
        public double successfulExecutions;
        public double failedExecutions;
        public double timeoutExecutions;
        public double retriedExecutions;
        public double misfiredJobs;
        public double deadLetterJobs;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("heapUsagePercent", heapUsagePercent);
            map.put("threadPoolUtilization", threadPoolUtilization);
            map.put("isHighLoad", isHighLoad);
            map.put("totalExecutions", totalExecutions);
            map.put("successfulExecutions", successfulExecutions);
            map.put("failedExecutions", failedExecutions);
            map.put("timeoutExecutions", timeoutExecutions);
            map.put("retriedExecutions", retriedExecutions);
            map.put("misfiredJobs", misfiredJobs);
            map.put("deadLetterJobs", deadLetterJobs);
            return map;
        }
    }

    /**
     * Job-specific metrics
     */
    public static class JobMetrics {
        public String jobName;
        public double successRate;
        public double failureRate;

        public Map<String, Object> toMap() {
            Map<String, Object> map = new HashMap<>();
            map.put("jobName", jobName);
            map.put("successRate", successRate);
            map.put("failureRate", failureRate);
            return map;
        }
    }
}
