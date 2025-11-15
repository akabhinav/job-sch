package com.enterprise.scheduler.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Collects custom job scheduler metrics
 * Feature #25: Custom Metrics Dashboard
 */
@Slf4j
@Component
public class CustomJobMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Map<String, AtomicInteger> jobCounts;
    private final Map<String, AtomicLong> jobDurations;

    // Counters
    private final Counter scheduledJobsCounter;
    private final Counter manualJobsCounter;
    private final Counter dependencyTriggeredCounter;
    private final Counter eventTriggeredCounter;
    private final Counter misfiredJobsCounter;
    private final Counter deadletterJobsCounter;

    // Distribution summaries
    private final DistributionSummary jobExecutionSizeDistribution;

    public CustomJobMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.jobCounts = new ConcurrentHashMap<>();
        this.jobDurations = new ConcurrentHashMap<>();

        // Initialize counters
        this.scheduledJobsCounter = Counter.builder("job.trigger.scheduled")
                .description("Number of scheduled job triggers")
                .register(meterRegistry);

        this.manualJobsCounter = Counter.builder("job.trigger.manual")
                .description("Number of manual job triggers")
                .register(meterRegistry);

        this.dependencyTriggeredCounter = Counter.builder("job.trigger.dependency")
                .description("Number of dependency-triggered jobs")
                .register(meterRegistry);

        this.eventTriggeredCounter = Counter.builder("job.trigger.event")
                .description("Number of event-triggered jobs")
                .register(meterRegistry);

        this.misfiredJobsCounter = Counter.builder("job.misfired")
                .description("Number of misfired jobs")
                .register(meterRegistry);

        this.deadletterJobsCounter = Counter.builder("job.deadletter")
                .description("Number of jobs sent to dead letter queue")
                .register(meterRegistry);

        // Initialize distribution summary
        this.jobExecutionSizeDistribution = DistributionSummary.builder("job.execution.size")
                .description("Distribution of job execution payload sizes")
                .baseUnit("bytes")
                .register(meterRegistry);

        log.info("Custom job metrics collector initialized");
    }

    /**
     * Record scheduled job trigger
     */
    public void recordScheduledTrigger() {
        scheduledJobsCounter.increment();
    }

    /**
     * Record manual job trigger
     */
    public void recordManualTrigger() {
        manualJobsCounter.increment();
    }

    /**
     * Record dependency-triggered job
     */
    public void recordDependencyTrigger() {
        dependencyTriggeredCounter.increment();
    }

    /**
     * Record event-triggered job
     */
    public void recordEventTrigger() {
        eventTriggeredCounter.increment();
    }

    /**
     * Record misfired job
     */
    public void recordMisfiredJob(String jobName) {
        misfiredJobsCounter.increment();
        Counter.builder("job.misfired.by_job")
                .tag("job_name", jobName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record dead letter job
     */
    public void recordDeadLetterJob(String jobName) {
        deadletterJobsCounter.increment();
        Counter.builder("job.deadletter.by_job")
                .tag("job_name", jobName)
                .register(meterRegistry)
                .increment();
    }

    /**
     * Record job execution size
     */
    public void recordExecutionSize(long sizeInBytes) {
        jobExecutionSizeDistribution.record(sizeInBytes);
    }

    /**
     * Register job count gauge
     */
    public void registerJobCount(String status) {
        AtomicInteger count = jobCounts.computeIfAbsent(status, k -> new AtomicInteger(0));
        Gauge.builder("job.count", count, AtomicInteger::get)
                .description("Number of jobs by status")
                .tag("status", status)
                .register(meterRegistry);
    }

    /**
     * Update job count
     */
    public void updateJobCount(String status, int delta) {
        AtomicInteger count = jobCounts.computeIfAbsent(status, k -> {
            AtomicInteger newCount = new AtomicInteger(0);
            Gauge.builder("job.count", newCount, AtomicInteger::get)
                    .description("Number of jobs by status")
                    .tag("status", status)
                    .register(meterRegistry);
            return newCount;
        });
        count.addAndGet(delta);
    }

    /**
     * Set job count
     */
    public void setJobCount(String status, int value) {
        AtomicInteger count = jobCounts.computeIfAbsent(status, k -> {
            AtomicInteger newCount = new AtomicInteger(0);
            Gauge.builder("job.count", newCount, AtomicInteger::get)
                    .description("Number of jobs by status")
                    .tag("status", status)
                    .register(meterRegistry);
            return newCount;
        });
        count.set(value);
    }

    /**
     * Record job dependency chain depth
     */
    public void recordDependencyChainDepth(int depth) {
        DistributionSummary.builder("job.dependency.chain.depth")
                .description("Depth of job dependency chains")
                .register(meterRegistry)
                .record(depth);
    }

    /**
     * Record workflow execution
     */
    public void recordWorkflowExecution(String workflowName, int jobCount) {
        Counter.builder("workflow.executions")
                .description("Number of workflow executions")
                .tag("workflow", workflowName)
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("workflow.job.count")
                .description("Number of jobs in workflow executions")
                .tag("workflow", workflowName)
                .register(meterRegistry)
                .record(jobCount);
    }

    /**
     * Record job parameter count
     */
    public void recordParameterCount(String jobName, int parameterCount) {
        DistributionSummary.builder("job.parameters.count")
                .description("Number of parameters per job execution")
                .tag("job_name", jobName)
                .register(meterRegistry)
                .record(parameterCount);
    }

    /**
     * Record concurrent execution count
     */
    public void recordConcurrentExecutions(String jobName, int count) {
        Gauge.builder("job.concurrent.executions", count, Integer::intValue)
                .description("Number of concurrent executions for job")
                .tag("job_name", jobName)
                .register(meterRegistry);
    }

    /**
     * Record queue size
     */
    public void recordQueueSize(String queueName, int size) {
        Gauge.builder("job.queue.size", size, Integer::intValue)
                .description("Job queue size")
                .tag("queue", queueName)
                .register(meterRegistry);
    }

    /**
     * Record cluster node metric
     */
    public void recordClusterNodeMetric(String nodeId, String metricName, double value) {
        Gauge.builder("cluster.node." + metricName, value, Double::doubleValue)
                .description("Cluster node metric: " + metricName)
                .tag("node_id", nodeId)
                .register(meterRegistry);
    }

    /**
     * Record custom metric
     */
    public void recordCustomMetric(String metricName, String jobName, double value) {
        Gauge.builder("job.custom." + metricName, value, Double::doubleValue)
                .description("Custom job metric: " + metricName)
                .tag("job_name", jobName)
                .register(meterRegistry);
    }

    /**
     * Get total misfired jobs count
     */
    public double getMisfiredJobsCount() {
        return misfiredJobsCounter.count();
    }

    /**
     * Get total dead letter jobs count
     */
    public double getDeadLetterJobsCount() {
        return deadletterJobsCounter.count();
    }
}
