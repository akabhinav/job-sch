package com.enterprise.scheduler.security.apikey;

import com.enterprise.scheduler.security.audit.AuditService;
import com.enterprise.scheduler.security.audit.AuditEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Service for managing API keys.
 * Provides secure generation, validation, and lifecycle management of API keys.
 *
 * Feature #54: API Key Management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final String KEY_PREFIX_LIVE = "sk_live_";
    private static final String KEY_PREFIX_TEST = "sk_test_";
    private static final int KEY_LENGTH = 32; // bytes
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Create a new API key
     */
    public ApiKeyCreationResult createApiKey(String name, String userId, String tenantId,
                                            Set<String> scopes, boolean isTest) {
        return createApiKey(name, userId, tenantId, scopes, isTest, null, null);
    }

    /**
     * Create a new API key with expiration and IP restrictions
     */
    public ApiKeyCreationResult createApiKey(String name, String userId, String tenantId,
                                            Set<String> scopes, boolean isTest,
                                            Instant expiresAt, Set<String> allowedIpAddresses) {
        // Generate random key
        byte[] keyBytes = new byte[KEY_LENGTH];
        SECURE_RANDOM.nextBytes(keyBytes);
        String randomPart = Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);

        // Create full key with prefix
        String prefix = isTest ? KEY_PREFIX_TEST : KEY_PREFIX_LIVE;
        String fullKey = prefix + randomPart;

        // Hash the key for storage
        String keyHash = passwordEncoder.encode(fullKey);

        // Create API key entity
        ApiKey apiKey = ApiKey.builder()
                .id(UUID.randomUUID().toString())
                .name(name)
                .keyHash(keyHash)
                .keyPrefix(prefix)
                .userId(userId)
                .tenantId(tenantId)
                .scopes(scopes)
                .status(ApiKeyStatus.ACTIVE)
                .createdAt(Instant.now())
                .expiresAt(expiresAt)
                .usageCount(0)
                .allowedIpAddresses(allowedIpAddresses)
                .build();

        // Save to repository
        apiKey = apiKeyRepository.save(apiKey);

        // Audit the creation
        auditService.logEvent(
                AuditEventType.API_KEY_CREATED,
                userId,
                tenantId,
                "API key created: " + name,
                null
        );

        log.info("Created API key: id={}, name={}, userId={}, tenantId={}",
                apiKey.getId(), name, userId, tenantId);

        // Return the full key only once (it won't be stored)
        return new ApiKeyCreationResult(apiKey, fullKey);
    }

    /**
     * Validate an API key
     */
    public Optional<ApiKey> validateApiKey(String key) {
        return validateApiKey(key, null);
    }

    /**
     * Validate an API key with IP address check
     */
    public Optional<ApiKey> validateApiKey(String key, String ipAddress) {
        if (key == null || key.isEmpty()) {
            return Optional.empty();
        }

        // Extract prefix to narrow down search
        String prefix = extractPrefix(key);
        if (prefix == null) {
            log.warn("Invalid API key format");
            return Optional.empty();
        }

        // Find potential keys by prefix
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findByKeyPrefix(prefix);
        if (apiKeyOpt.isEmpty()) {
            log.warn("API key not found by prefix: {}", prefix);
            return Optional.empty();
        }

        ApiKey apiKey = apiKeyOpt.get();

        // Verify the full key hash
        if (!passwordEncoder.matches(key, apiKey.getKeyHash())) {
            log.warn("API key hash mismatch");
            return Optional.empty();
        }

        // Check if key is valid
        if (!apiKey.isValid()) {
            log.warn("API key is not valid: id={}, status={}", apiKey.getId(), apiKey.getStatus());
            return Optional.empty();
        }

        // Check IP address restriction
        if (ipAddress != null && !apiKey.isIpAddressAllowed(ipAddress)) {
            log.warn("IP address not allowed: key={}, ip={}", apiKey.getId(), ipAddress);
            return Optional.empty();
        }

        // Update last used timestamp and usage count
        apiKey.setLastUsedAt(Instant.now());
        apiKey.setUsageCount(apiKey.getUsageCount() + 1);
        apiKeyRepository.save(apiKey);

        return Optional.of(apiKey);
    }

    /**
     * Revoke an API key
     */
    public void revokeApiKey(String keyId, String revokedBy) {
        Optional<ApiKey> apiKeyOpt = apiKeyRepository.findById(keyId);
        if (apiKeyOpt.isEmpty()) {
            throw new IllegalArgumentException("API key not found: " + keyId);
        }

        ApiKey apiKey = apiKeyOpt.get();
        apiKey.setStatus(ApiKeyStatus.REVOKED);
        apiKeyRepository.save(apiKey);

        auditService.logEvent(
                AuditEventType.API_KEY_REVOKED,
                revokedBy,
                apiKey.getTenantId(),
                "API key revoked: " + apiKey.getName(),
                null
        );

        log.info("Revoked API key: id={}, name={}, revokedBy={}",
                keyId, apiKey.getName(), revokedBy);
    }

    /**
     * Get API key by ID
     */
    public Optional<ApiKey> getApiKey(String keyId) {
        return apiKeyRepository.findById(keyId);
    }

    /**
     * List all API keys for a user
     */
    public List<ApiKey> listApiKeysForUser(String userId) {
        return apiKeyRepository.findByUserId(userId);
    }

    /**
     * List all API keys for a tenant
     */
    public List<ApiKey> listApiKeysForTenant(String tenantId) {
        return apiKeyRepository.findByTenantId(tenantId);
    }

    /**
     * List active API keys for a user
     */
    public List<ApiKey> listActiveApiKeysForUser(String userId) {
        return apiKeyRepository.findActiveByUserId(userId);
    }

    /**
     * List active API keys for a tenant
     */
    public List<ApiKey> listActiveApiKeysForTenant(String tenantId) {
        return apiKeyRepository.findActiveByTenantId(tenantId);
    }

    /**
     * Rotate an API key (revoke old, create new)
     */
    public ApiKeyCreationResult rotateApiKey(String oldKeyId, String userId) {
        Optional<ApiKey> oldKeyOpt = apiKeyRepository.findById(oldKeyId);
        if (oldKeyOpt.isEmpty()) {
            throw new IllegalArgumentException("API key not found: " + oldKeyId);
        }

        ApiKey oldKey = oldKeyOpt.get();

        // Create new key with same properties
        ApiKeyCreationResult newKey = createApiKey(
                oldKey.getName() + " (Rotated)",
                oldKey.getUserId(),
                oldKey.getTenantId(),
                oldKey.getScopes(),
                oldKey.getKeyPrefix().equals(KEY_PREFIX_TEST),
                oldKey.getExpiresAt(),
                oldKey.getAllowedIpAddresses()
        );

        // Revoke old key
        revokeApiKey(oldKeyId, userId);

        log.info("Rotated API key: oldId={}, newId={}", oldKeyId, newKey.getApiKey().getId());

        return newKey;
    }

    /**
     * Clean up expired API keys
     */
    public int cleanupExpiredKeys() {
        // This would be called by a scheduled task
        // Implementation depends on repository capabilities
        log.info("Cleaning up expired API keys");
        return 0; // Placeholder
    }

    /**
     * Extract prefix from key
     */
    private String extractPrefix(String key) {
        if (key.startsWith(KEY_PREFIX_LIVE)) {
            return KEY_PREFIX_LIVE;
        } else if (key.startsWith(KEY_PREFIX_TEST)) {
            return KEY_PREFIX_TEST;
        }
        return null;
    }

    /**
     * Result object containing the API key entity and the plain text key
     */
    public record ApiKeyCreationResult(ApiKey apiKey, String plainTextKey) {
        /**
         * Get a masked version of the key for display
         */
        public String getMaskedKey() {
            if (plainTextKey == null || plainTextKey.length() < 12) {
                return "***";
            }
            return plainTextKey.substring(0, 12) + "..." + plainTextKey.substring(plainTextKey.length() - 4);
        }
    }
}
