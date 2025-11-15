package com.enterprise.scheduler.security.rbac;

import java.lang.annotation.*;

/**
 * Method-level security annotation requiring ANY of the specified permissions.
 *
 * Feature #53: Method-Level Security
 *
 * Example usage:
 * <pre>
 * {@code
 * @RequireAnyPermission({Permission.JOB_UPDATE, Permission.JOB_DELETE})
 * public void modifyJob(String jobId) { ... }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAnyPermission {
    /**
     * The permissions (user must have at least one)
     */
    Permission[] value();
}
