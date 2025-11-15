package com.enterprise.scheduler.common.exception;

/**
 * Base exception for all scheduler-related exceptions
 */
public class SchedulerException extends RuntimeException {

    private final String errorCode;

    public SchedulerException(String message) {
        super(message);
        this.errorCode = "SCHEDULER_ERROR";
    }

    public SchedulerException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "SCHEDULER_ERROR";
    }

    public SchedulerException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public SchedulerException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
