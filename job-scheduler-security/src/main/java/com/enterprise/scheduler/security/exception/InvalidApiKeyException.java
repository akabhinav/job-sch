package com.enterprise.scheduler.security.exception;

/**
 * Exception thrown when an API key is invalid or revoked.
 *
 * Feature #54: API Key Exceptions
 */
public class InvalidApiKeyException extends SecurityException {

    public InvalidApiKeyException(String message) {
        super(message);
    }

    public InvalidApiKeyException(String message, Throwable cause) {
        super(message, cause);
    }
}
