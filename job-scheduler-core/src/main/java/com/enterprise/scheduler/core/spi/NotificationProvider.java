package com.enterprise.scheduler.core.spi;

import com.enterprise.scheduler.core.domain.JobExecution;

/**
 * Notification provider interface for alerting
 * Feature #29: Alerting System
 */
public interface NotificationProvider {

    /**
     * Get provider name
     */
    String getName();

    /**
     * Send notification for job completion
     */
    void notifySuccess(JobExecution execution);

    /**
     * Send notification for job failure
     */
    void notifyFailure(JobExecution execution);

    /**
     * Send notification for job timeout
     */
    void notifyTimeout(JobExecution execution);

    /**
     * Send custom notification
     */
    void notify(String title, String message, NotificationLevel level);

    enum NotificationLevel {
        INFO,
        WARNING,
        ERROR,
        CRITICAL
    }
}
