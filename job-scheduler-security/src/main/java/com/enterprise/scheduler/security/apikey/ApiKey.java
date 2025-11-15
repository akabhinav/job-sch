package com.enterprise.scheduler.security.apikey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

/**
 * API Key entity for programmatic access to the platform.
 *
 * Feature #54: API Key Management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiKey {
    /**
     * Unique identifier
     */
    private String id;

    /**
     * API key name/description
     */
    private String name;

    /**
     * The actual API key (hashed in storage)
     */
    private String keyHash;

    /**
     * Prefix of the key for identification (stored in plain text)
     * Format: "sk_live_" or "sk_test_"
     */
    private String keyPrefix;

    /**
     * User/service account this key belongs to
     */
    private String userId;

    /**
     * Tenant this key is scoped to
     */
    private String tenantId;

    /**
     * Scopes/permissions this key has
     */
    private Set<String> scopes;

    /**
     * Key status
     */
    private ApiKeyStatus status;

    /**
     * Creation timestamp
     */
    private Instant createdAt;

    /**
     * Expiration timestamp (null means no expiration)
     */
    private Instant expiresAt;

    /**
     * Last used timestamp
     */
    private Instant lastUsedAt;

    /**
     * Number of times this key has been used
     */
    private long usageCount;

    /**
     * IP address restrictions (null or empty means no restrictions)
     */
    private Set<String> allowedIpAddresses;

    /**
     * Additional metadata
     */
    private String metadata;

    /**
     * Check if the API key is active and valid
     */
    public boolean isValid() {
        if (status != ApiKeyStatus.ACTIVE) {
            return false;
        }

        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            return false;
        }

        return true;
    }

    /**
     * Check if the API key has a specific scope
     */
    public boolean hasScope(String scope) {
        return scopes != null && scopes.contains(scope);
    }

    /**
     * Check if IP address is allowed
     */
    public boolean isIpAddressAllowed(String ipAddress) {
        if (allowedIpAddresses == null || allowedIpAddresses.isEmpty()) {
            return true; // No restrictions
        }

        return allowedIpAddresses.contains(ipAddress);
    }
}
