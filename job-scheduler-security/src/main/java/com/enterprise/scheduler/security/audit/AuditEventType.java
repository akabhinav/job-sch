package com.enterprise.scheduler.security.audit;

import lombok.Getter;

/**
 * Audit event types for tracking different types of security and operational events.
 *
 * Feature #55: Audit Event Types
 */
@Getter
public enum AuditEventType {
    // Authentication events
    LOGIN_SUCCESS("User login successful", "authentication"),
    LOGIN_FAILURE("User login failed", "authentication"),
    LOGOUT("User logout", "authentication"),
    TOKEN_REFRESH("JWT token refreshed", "authentication"),
    PASSWORD_CHANGE("Password changed", "authentication"),
    PASSWORD_RESET("Password reset", "authentication"),

    // Authorization events
    ACCESS_GRANTED("Access granted", "authorization"),
    ACCESS_DENIED("Access denied", "authorization"),
    PERMISSION_CHECK("Permission check performed", "authorization"),

    // User management events
    USER_CREATED("User created", "user"),
    USER_UPDATED("User updated", "user"),
    USER_DELETED("User deleted", "user"),
    USER_ENABLED("User enabled", "user"),
    USER_DISABLED("User disabled", "user"),
    ROLE_ASSIGNED("Role assigned to user", "user"),
    ROLE_REVOKED("Role revoked from user", "user"),

    // Job events
    JOB_CREATED("Job created", "job"),
    JOB_UPDATED("Job updated", "job"),
    JOB_DELETED("Job deleted", "job"),
    JOB_EXECUTED("Job executed", "job"),
    JOB_PAUSED("Job paused", "job"),
    JOB_RESUMED("Job resumed", "job"),
    JOB_CANCELLED("Job cancelled", "job"),

    // Schedule events
    SCHEDULE_CREATED("Schedule created", "schedule"),
    SCHEDULE_UPDATED("Schedule updated", "schedule"),
    SCHEDULE_DELETED("Schedule deleted", "schedule"),

    // API key events
    API_KEY_CREATED("API key created", "apikey"),
    API_KEY_USED("API key used", "apikey"),
    API_KEY_REVOKED("API key revoked", "apikey"),
    API_KEY_ROTATED("API key rotated", "apikey"),

    // Tenant events
    TENANT_CREATED("Tenant created", "tenant"),
    TENANT_UPDATED("Tenant updated", "tenant"),
    TENANT_DELETED("Tenant deleted", "tenant"),

    // Configuration events
    CONFIG_CHANGED("Configuration changed", "config"),
    PLUGIN_INSTALLED("Plugin installed", "plugin"),
    PLUGIN_UNINSTALLED("Plugin uninstalled", "plugin"),

    // Security events
    SECURITY_ALERT("Security alert", "security"),
    SUSPICIOUS_ACTIVITY("Suspicious activity detected", "security"),
    RATE_LIMIT_EXCEEDED("Rate limit exceeded", "security"),
    IP_BLOCKED("IP address blocked", "security"),

    // System events
    SYSTEM_STARTED("System started", "system"),
    SYSTEM_SHUTDOWN("System shutdown", "system"),
    BACKUP_CREATED("Backup created", "system"),
    BACKUP_RESTORED("Backup restored", "system"),

    // Audit events
    AUDIT_LOG_ACCESSED("Audit log accessed", "audit"),
    AUDIT_LOG_EXPORTED("Audit log exported", "audit");

    private final String description;
    private final String category;

    AuditEventType(String description, String category) {
        this.description = description;
        this.category = category;
    }

    /**
     * Check if this is a security-related event
     */
    public boolean isSecurityEvent() {
        return category.equals("security") ||
               category.equals("authentication") ||
               category.equals("authorization");
    }

    /**
     * Check if this is a critical event
     */
    public boolean isCritical() {
        return this == LOGIN_FAILURE ||
               this == ACCESS_DENIED ||
               this == SECURITY_ALERT ||
               this == SUSPICIOUS_ACTIVITY ||
               this == USER_DELETED ||
               this == TENANT_DELETED;
    }
}
