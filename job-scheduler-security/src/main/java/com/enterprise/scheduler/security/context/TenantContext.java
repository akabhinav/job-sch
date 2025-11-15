package com.enterprise.scheduler.security.context;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Tenant context object holding tenant-specific information.
 *
 * Feature #56: Multi-Tenancy Context
 */
@Data
public class TenantContext {
    /**
     * The tenant ID
     */
    private String tenantId;

    /**
     * Additional context data
     */
    private Map<String, Object> contextData;

    /**
     * Default constructor
     */
    public TenantContext() {
        this.contextData = new HashMap<>();
    }

    /**
     * Constructor with tenant ID
     */
    public TenantContext(String tenantId) {
        this.tenantId = tenantId;
        this.contextData = new HashMap<>();
    }

    /**
     * Set context data
     */
    public void setContextData(String key, Object value) {
        if (this.contextData == null) {
            this.contextData = new HashMap<>();
        }
        this.contextData.put(key, value);
    }

    /**
     * Get context data
     */
    public Object getContextData(String key) {
        return this.contextData != null ? this.contextData.get(key) : null;
    }

    /**
     * Check if context has data for a key
     */
    public boolean hasContextData(String key) {
        return this.contextData != null && this.contextData.containsKey(key);
    }

    /**
     * Remove context data
     */
    public void removeContextData(String key) {
        if (this.contextData != null) {
            this.contextData.remove(key);
        }
    }

    /**
     * Clear all context data
     */
    public void clearContextData() {
        if (this.contextData != null) {
            this.contextData.clear();
        }
    }
}
