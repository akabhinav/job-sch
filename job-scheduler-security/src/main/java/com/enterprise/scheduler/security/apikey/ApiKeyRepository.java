package com.enterprise.scheduler.security.apikey;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for API Key persistence.
 * Implementation should be provided in the persistence module.
 *
 * Feature #54: API Key Management
 */
public interface ApiKeyRepository {
    /**
     * Save an API key
     */
    ApiKey save(ApiKey apiKey);

    /**
     * Find API key by ID
     */
    Optional<ApiKey> findById(String id);

    /**
     * Find API key by key hash
     */
    Optional<ApiKey> findByKeyHash(String keyHash);

    /**
     * Find API key by prefix (for lookup before hashing)
     */
    Optional<ApiKey> findByKeyPrefix(String keyPrefix);

    /**
     * Find all API keys for a user
     */
    List<ApiKey> findByUserId(String userId);

    /**
     * Find all API keys for a tenant
     */
    List<ApiKey> findByTenantId(String tenantId);

    /**
     * Find all active API keys for a user
     */
    List<ApiKey> findActiveByUserId(String userId);

    /**
     * Find all active API keys for a tenant
     */
    List<ApiKey> findActiveByTenantId(String tenantId);

    /**
     * Delete an API key
     */
    void delete(String id);

    /**
     * Count API keys by user
     */
    long countByUserId(String userId);

    /**
     * Count API keys by tenant
     */
    long countByTenantId(String tenantId);
}
