package com.enterprise.scheduler.common.exception;

/**
 * Exception thrown during job execution
 */
public class JobExecutionException extends SchedulerException {

    public JobExecutionException(String message) {
        super("JOB_EXECUTION_ERROR", message);
    }

    public JobExecutionException(String message, Throwable cause) {
        super("JOB_EXECUTION_ERROR", message, cause);
    }
}
