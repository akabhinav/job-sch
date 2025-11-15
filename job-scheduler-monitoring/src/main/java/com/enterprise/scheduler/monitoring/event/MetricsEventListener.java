package com.enterprise.scheduler.monitoring.event;

import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.monitoring.metrics.MetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Event listener for integrating metrics collection with job lifecycle
 * Feature #23: Job Metrics Collection
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MetricsEventListener {

    private final MetricsService metricsService;

    /**
     * Listen for job execution start events
     */
    @EventListener
    public void onJobExecutionStart(JobExecutionStartEvent event) {
        try {
            metricsService.recordJobStart(event.getExecution());
        } catch (Exception e) {
            log.error("Error recording job start metrics", e);
        }
    }

    /**
     * Listen for job execution complete events
     */
    @EventListener
    public void onJobExecutionComplete(JobExecutionCompleteEvent event) {
        try {
            metricsService.recordJobComplete(event.getExecution());
        } catch (Exception e) {
            log.error("Error recording job completion metrics", e);
        }
    }

    /**
     * Listen for job failure events
     */
    @EventListener
    public void onJobExecutionFailure(JobExecutionFailureEvent event) {
        try {
            metricsService.recordJobFailure(
                    event.getExecution().getJobName(),
                    event.getErrorType()
            );
        } catch (Exception e) {
            log.error("Error recording job failure metrics", e);
        }
    }

    /**
     * Job execution start event
     */
    public static class JobExecutionStartEvent {
        private final JobExecution execution;

        public JobExecutionStartEvent(JobExecution execution) {
            this.execution = execution;
        }

        public JobExecution getExecution() {
            return execution;
        }
    }

    /**
     * Job execution complete event
     */
    public static class JobExecutionCompleteEvent {
        private final JobExecution execution;

        public JobExecutionCompleteEvent(JobExecution execution) {
            this.execution = execution;
        }

        public JobExecution getExecution() {
            return execution;
        }
    }

    /**
     * Job execution failure event
     */
    public static class JobExecutionFailureEvent {
        private final JobExecution execution;
        private final String errorType;

        public JobExecutionFailureEvent(JobExecution execution, String errorType) {
            this.execution = execution;
            this.errorType = errorType;
        }

        public JobExecution getExecution() {
            return execution;
        }

        public String getErrorType() {
            return errorType;
        }
    }
}
