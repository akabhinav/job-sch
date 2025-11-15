package com.enterprise.scheduler.security.rbac;

import lombok.Getter;

import java.util.Set;

/**
 * Role enumeration for Role-Based Access Control (RBAC).
 * Each role has a set of permissions that define what actions can be performed.
 *
 * Feature #53: Role-Based Access Control
 */
@Getter
public enum Role {
    /**
     * System administrator with full access
     */
    ADMIN(Set.of(
        Permission.JOB_CREATE,
        Permission.JOB_READ,
        Permission.JOB_UPDATE,
        Permission.JOB_DELETE,
        Permission.JOB_EXECUTE,
        Permission.JOB_PAUSE,
        Permission.JOB_RESUME,
        Permission.SCHEDULE_CREATE,
        Permission.SCHEDULE_READ,
        Permission.SCHEDULE_UPDATE,
        Permission.SCHEDULE_DELETE,
        Permission.USER_CREATE,
        Permission.USER_READ,
        Permission.USER_UPDATE,
        Permission.USER_DELETE,
        Permission.ROLE_ASSIGN,
        Permission.TENANT_CREATE,
        Permission.TENANT_READ,
        Permission.TENANT_UPDATE,
        Permission.TENANT_DELETE,
        Permission.APIKEY_CREATE,
        Permission.APIKEY_READ,
        Permission.APIKEY_REVOKE,
        Permission.AUDIT_READ,
        Permission.AUDIT_EXPORT,
        Permission.MONITORING_READ,
        Permission.MONITORING_CONFIGURE,
        Permission.CLUSTER_MANAGE,
        Permission.PLUGIN_INSTALL,
        Permission.PLUGIN_UNINSTALL,
        Permission.SYSTEM_CONFIGURE
    )),

    /**
     * Job manager who can create and manage jobs
     */
    JOB_MANAGER(Set.of(
        Permission.JOB_CREATE,
        Permission.JOB_READ,
        Permission.JOB_UPDATE,
        Permission.JOB_DELETE,
        Permission.JOB_EXECUTE,
        Permission.JOB_PAUSE,
        Permission.JOB_RESUME,
        Permission.SCHEDULE_CREATE,
        Permission.SCHEDULE_READ,
        Permission.SCHEDULE_UPDATE,
        Permission.SCHEDULE_DELETE,
        Permission.MONITORING_READ
    )),

    /**
     * Job viewer who can only view jobs and their executions
     */
    JOB_VIEWER(Set.of(
        Permission.JOB_READ,
        Permission.SCHEDULE_READ,
        Permission.MONITORING_READ
    )),

    /**
     * Operator who can execute and manage job executions
     */
    OPERATOR(Set.of(
        Permission.JOB_READ,
        Permission.JOB_EXECUTE,
        Permission.JOB_PAUSE,
        Permission.JOB_RESUME,
        Permission.SCHEDULE_READ,
        Permission.MONITORING_READ,
        Permission.MONITORING_CONFIGURE
    )),

    /**
     * API key manager who can create and manage API keys
     */
    API_KEY_MANAGER(Set.of(
        Permission.APIKEY_CREATE,
        Permission.APIKEY_READ,
        Permission.APIKEY_REVOKE,
        Permission.JOB_READ,
        Permission.USER_READ
    )),

    /**
     * Auditor who can view audit logs
     */
    AUDITOR(Set.of(
        Permission.AUDIT_READ,
        Permission.AUDIT_EXPORT,
        Permission.JOB_READ,
        Permission.USER_READ,
        Permission.MONITORING_READ
    )),

    /**
     * Tenant administrator with full access within their tenant
     */
    TENANT_ADMIN(Set.of(
        Permission.JOB_CREATE,
        Permission.JOB_READ,
        Permission.JOB_UPDATE,
        Permission.JOB_DELETE,
        Permission.JOB_EXECUTE,
        Permission.JOB_PAUSE,
        Permission.JOB_RESUME,
        Permission.SCHEDULE_CREATE,
        Permission.SCHEDULE_READ,
        Permission.SCHEDULE_UPDATE,
        Permission.SCHEDULE_DELETE,
        Permission.USER_CREATE,
        Permission.USER_READ,
        Permission.USER_UPDATE,
        Permission.USER_DELETE,
        Permission.ROLE_ASSIGN,
        Permission.APIKEY_CREATE,
        Permission.APIKEY_READ,
        Permission.APIKEY_REVOKE,
        Permission.AUDIT_READ,
        Permission.MONITORING_READ,
        Permission.MONITORING_CONFIGURE
    )),

    /**
     * Regular user with basic access
     */
    USER(Set.of(
        Permission.JOB_READ,
        Permission.JOB_EXECUTE,
        Permission.SCHEDULE_READ
    )),

    /**
     * Service account for API integrations
     */
    SERVICE_ACCOUNT(Set.of(
        Permission.JOB_CREATE,
        Permission.JOB_READ,
        Permission.JOB_UPDATE,
        Permission.JOB_EXECUTE,
        Permission.SCHEDULE_CREATE,
        Permission.SCHEDULE_READ,
        Permission.SCHEDULE_UPDATE,
        Permission.MONITORING_READ
    ));

    private final Set<Permission> permissions;

    Role(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    /**
     * Check if this role has a specific permission
     */
    public boolean hasPermission(Permission permission) {
        return permissions.contains(permission);
    }

    /**
     * Get the Spring Security authority name
     */
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
