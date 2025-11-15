package com.enterprise.scheduler.common.model;

/**
 * Job execution status enumeration
 * Feature #4: Job State Management
 */
public enum JobStatus {
    /**
     * Job is scheduled but not yet running
     */
    SCHEDULED,

    /**
     * Job is currently executing
     */
    RUNNING,

    /**
     * Job completed successfully
     */
    SUCCESS,

    /**
     * Job failed during execution
     */
    FAILED,

    /**
     * Job was cancelled
     */
    CANCELLED,

    /**
     * Job execution timed out
     */
    TIMEOUT,

    /**
     * Job is paused
     */
    PAUSED,

    /**
     * Job is waiting for dependencies
     */
    WAITING,

    /**
     * Job is retrying after failure
     */
    RETRYING,

    /**
     * Job is in an unknown state
     */
    UNKNOWN;

    public boolean isTerminal() {
        return this == SUCCESS || this == FAILED || this == CANCELLED || this == TIMEOUT;
    }

    public boolean isActive() {
        return this == RUNNING || this == SCHEDULED || this == WAITING || this == RETRYING;
    }
}
