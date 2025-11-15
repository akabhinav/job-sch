package com.enterprise.scheduler.common.exception;

/**
 * Exception thrown when a job is not found
 */
public class JobNotFoundException extends SchedulerException {

    public JobNotFoundException(String jobId) {
        super("JOB_NOT_FOUND", "Job not found: " + jobId);
    }

    public JobNotFoundException(String jobId, Throwable cause) {
        super("JOB_NOT_FOUND", "Job not found: " + jobId, cause);
    }
}
