package com.enterprise.scheduler.security.exception;

/**
 * Exception thrown when access to a tenant is denied.
 *
 * Feature #56: Tenant Access Exceptions
 */
public class TenantAccessDeniedException extends SecurityException {

    private final String tenantId;

    public TenantAccessDeniedException(String tenantId) {
        super("Access denied to tenant: " + tenantId);
        this.tenantId = tenantId;
    }

    public TenantAccessDeniedException(String tenantId, String message) {
        super(message);
        this.tenantId = tenantId;
    }

    public String getTenantId() {
        return tenantId;
    }
}
