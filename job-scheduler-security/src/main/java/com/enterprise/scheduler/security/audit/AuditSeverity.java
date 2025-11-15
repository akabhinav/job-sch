package com.enterprise.scheduler.security.audit;

/**
 * Audit event severity levels.
 *
 * Feature #55: Audit Severity
 */
public enum AuditSeverity {
    /**
     * Informational events (normal operations)
     */
    INFO,

    /**
     * Warning events (potential issues)
     */
    WARNING,

    /**
     * Error events (failures)
     */
    ERROR,

    /**
     * Critical events (security issues, system failures)
     */
    CRITICAL
}
