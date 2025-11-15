package com.enterprise.scheduler.plugins.notification;

import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.NotificationProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Log-based notification provider plugin
 * Logs notifications using SLF4J
 * Feature #29: Alerting System, #41: Plugin Architecture
 */
@Slf4j
public class LogNotificationProvider implements NotificationProvider {

    private static final String NAME = "log";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void notifySuccess(JobExecution execution) {
        log.info("JOB SUCCESS | Job: {} | Execution: {} | Duration: {} | Triggered by: {} | Node: {}",
            execution.getJobName(),
            execution.getId(),
            execution.getDuration(),
            execution.getTriggeredBy(),
            execution.getNodeId());

        if (log.isDebugEnabled()) {
            log.debug("Job execution details: {}", formatExecutionDetails(execution));
        }
    }

    @Override
    public void notifyFailure(JobExecution execution) {
        log.error("JOB FAILURE | Job: {} | Execution: {} | Duration: {} | Triggered by: {} | Node: {} | Error: {}",
            execution.getJobName(),
            execution.getId(),
            execution.getDuration(),
            execution.getTriggeredBy(),
            execution.getNodeId(),
            execution.getErrorMessage() != null ? execution.getErrorMessage() : "Unknown error");

        if (log.isDebugEnabled()) {
            log.debug("Job execution details: {}", formatExecutionDetails(execution));
        }

        if (execution.getStackTrace() != null && !execution.getStackTrace().isEmpty()) {
            log.debug("Stack trace:\n{}", execution.getStackTrace());
        }
    }

    @Override
    public void notifyTimeout(JobExecution execution) {
        log.warn("JOB TIMEOUT | Job: {} | Execution: {} | Duration: {} | Triggered by: {} | Node: {}",
            execution.getJobName(),
            execution.getId(),
            execution.getDuration(),
            execution.getTriggeredBy(),
            execution.getNodeId());

        if (log.isDebugEnabled()) {
            log.debug("Job execution details: {}", formatExecutionDetails(execution));
        }
    }

    @Override
    public void notify(String title, String message, NotificationLevel level) {
        switch (level) {
            case INFO:
                log.info("NOTIFICATION | {} | {}", title, message);
                break;
            case WARNING:
                log.warn("NOTIFICATION | {} | {}", title, message);
                break;
            case ERROR:
                log.error("NOTIFICATION | {} | {}", title, message);
                break;
            case CRITICAL:
                log.error("CRITICAL NOTIFICATION | {} | {}", title, message);
                break;
            default:
                log.info("NOTIFICATION | {} | {}", title, message);
        }
    }

    private String formatExecutionDetails(JobExecution execution) {
        StringBuilder details = new StringBuilder();
        details.append("\n--- Job Execution Details ---\n");
        details.append("Job Name: ").append(execution.getJobName()).append("\n");
        details.append("Job ID: ").append(execution.getJobId()).append("\n");
        details.append("Execution ID: ").append(execution.getId()).append("\n");
        details.append("Status: ").append(execution.getStatus()).append("\n");

        if (execution.getStartTime() != null) {
            details.append("Start Time: ").append(execution.getStartTime()).append("\n");
        }
        if (execution.getEndTime() != null) {
            details.append("End Time: ").append(execution.getEndTime()).append("\n");
        }

        details.append("Duration: ").append(execution.getDuration()).append("\n");
        details.append("Trigger Type: ").append(execution.getTriggerType()).append("\n");

        if (execution.getTriggeredBy() != null) {
            details.append("Triggered By: ").append(execution.getTriggeredBy()).append("\n");
        }
        if (execution.getNodeId() != null) {
            details.append("Node ID: ").append(execution.getNodeId()).append("\n");
        }

        details.append("Attempt Number: ").append(execution.getAttemptNumber()).append("\n");

        if (execution.getParameters() != null && !execution.getParameters().isEmpty()) {
            details.append("Parameters: ").append(execution.getParameters()).append("\n");
        }

        if (execution.getResult() != null) {
            details.append("Result: ").append(execution.getResult()).append("\n");
        }

        if (execution.getErrorMessage() != null) {
            details.append("Error Message: ").append(execution.getErrorMessage()).append("\n");
        }

        if (execution.getMetrics() != null && !execution.getMetrics().isEmpty()) {
            details.append("Metrics: ").append(execution.getMetrics()).append("\n");
        }

        details.append("--- End Details ---");
        return details.toString();
    }
}
