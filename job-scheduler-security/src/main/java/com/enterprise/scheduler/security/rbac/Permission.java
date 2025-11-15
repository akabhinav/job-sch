package com.enterprise.scheduler.security.rbac;

import lombok.Getter;

/**
 * Permission enumeration for fine-grained access control.
 * Permissions define specific actions that can be performed on resources.
 *
 * Feature #53: Permission-Based Access Control
 */
@Getter
public enum Permission {
    // Job permissions
    JOB_CREATE("Create new jobs", "job:create"),
    JOB_READ("View job details", "job:read"),
    JOB_UPDATE("Update existing jobs", "job:update"),
    JOB_DELETE("Delete jobs", "job:delete"),
    JOB_EXECUTE("Execute jobs", "job:execute"),
    JOB_PAUSE("Pause running jobs", "job:pause"),
    JOB_RESUME("Resume paused jobs", "job:resume"),

    // Schedule permissions
    SCHEDULE_CREATE("Create job schedules", "schedule:create"),
    SCHEDULE_READ("View schedules", "schedule:read"),
    SCHEDULE_UPDATE("Update schedules", "schedule:update"),
    SCHEDULE_DELETE("Delete schedules", "schedule:delete"),

    // User management permissions
    USER_CREATE("Create users", "user:create"),
    USER_READ("View user information", "user:read"),
    USER_UPDATE("Update user information", "user:update"),
    USER_DELETE("Delete users", "user:delete"),
    ROLE_ASSIGN("Assign roles to users", "role:assign"),

    // Tenant management permissions
    TENANT_CREATE("Create tenants", "tenant:create"),
    TENANT_READ("View tenant information", "tenant:read"),
    TENANT_UPDATE("Update tenant information", "tenant:update"),
    TENANT_DELETE("Delete tenants", "tenant:delete"),

    // API key permissions
    APIKEY_CREATE("Create API keys", "apikey:create"),
    APIKEY_READ("View API keys", "apikey:read"),
    APIKEY_REVOKE("Revoke API keys", "apikey:revoke"),

    // Audit permissions
    AUDIT_READ("View audit logs", "audit:read"),
    AUDIT_EXPORT("Export audit logs", "audit:export"),

    // Monitoring permissions
    MONITORING_READ("View monitoring data", "monitoring:read"),
    MONITORING_CONFIGURE("Configure monitoring settings", "monitoring:configure"),

    // Cluster management permissions
    CLUSTER_MANAGE("Manage cluster nodes", "cluster:manage"),

    // Plugin permissions
    PLUGIN_INSTALL("Install plugins", "plugin:install"),
    PLUGIN_UNINSTALL("Uninstall plugins", "plugin:uninstall"),

    // System permissions
    SYSTEM_CONFIGURE("Configure system settings", "system:configure");

    private final String description;
    private final String code;

    Permission(String description, String code) {
        this.description = description;
        this.code = code;
    }

    /**
     * Get permission by code
     */
    public static Permission fromCode(String code) {
        for (Permission permission : values()) {
            if (permission.code.equals(code)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Unknown permission code: " + code);
    }

    /**
     * Check if this is a read-only permission
     */
    public boolean isReadOnly() {
        return code.endsWith(":read");
    }

    /**
     * Check if this is a write permission
     */
    public boolean isWrite() {
        return code.endsWith(":create") || code.endsWith(":update") || code.endsWith(":delete");
    }

    /**
     * Get the resource type this permission applies to
     */
    public String getResourceType() {
        return code.split(":")[0];
    }

    /**
     * Get the action this permission allows
     */
    public String getAction() {
        return code.split(":")[1];
    }
}
