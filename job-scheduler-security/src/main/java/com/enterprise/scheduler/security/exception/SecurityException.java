package com.enterprise.scheduler.security.exception;

/**
 * Base exception for security-related errors.
 *
 * Feature #51-60: Security Exceptions
 */
public class SecurityException extends RuntimeException {

    public SecurityException(String message) {
        super(message);
    }

    public SecurityException(String message, Throwable cause) {
        super(message, cause);
    }
}
