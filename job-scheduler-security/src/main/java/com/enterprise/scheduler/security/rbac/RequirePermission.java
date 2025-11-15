package com.enterprise.scheduler.security.rbac;

import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.annotation.*;

/**
 * Method-level security annotation for permission-based access control.
 * This is a convenience annotation that wraps @PreAuthorize with permission checks.
 *
 * Feature #53: Method-Level Security
 *
 * Example usage:
 * <pre>
 * {@code
 * @RequirePermission(Permission.JOB_CREATE)
 * public void createJob(Job job) { ... }
 *
 * @RequirePermission(value = Permission.JOB_DELETE, requireTenantAccess = true)
 * public void deleteJob(String jobId) { ... }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@PreAuthorize("hasPermission(null, #permission)")
public @interface RequirePermission {
    /**
     * The required permission
     */
    Permission value();

    /**
     * Whether to enforce tenant-based access control
     */
    boolean requireTenantAccess() default false;
}
