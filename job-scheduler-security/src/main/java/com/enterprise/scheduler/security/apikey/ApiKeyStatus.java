package com.enterprise.scheduler.security.apikey;

/**
 * API Key status enumeration.
 *
 * Feature #54: API Key Management
 */
public enum ApiKeyStatus {
    /**
     * Key is active and can be used
     */
    ACTIVE,

    /**
     * Key has been revoked and cannot be used
     */
    REVOKED,

    /**
     * Key has expired
     */
    EXPIRED,

    /**
     * Key is suspended temporarily
     */
    SUSPENDED
}
