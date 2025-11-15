package com.enterprise.scheduler.security.context;

/**
 * Interface for entities that are tenant-aware.
 * Entities implementing this interface can be automatically filtered by tenant.
 *
 * Feature #56: Multi-Tenancy
 */
public interface TenantAware {
    /**
     * Get the tenant ID this entity belongs to
     */
    String getTenantId();

    /**
     * Set the tenant ID for this entity
     */
    void setTenantId(String tenantId);
}
