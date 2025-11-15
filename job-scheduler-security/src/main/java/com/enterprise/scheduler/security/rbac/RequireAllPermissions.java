package com.enterprise.scheduler.security.rbac;

import java.lang.annotation.*;

/**
 * Method-level security annotation requiring ALL of the specified permissions.
 *
 * Feature #53: Method-Level Security
 *
 * Example usage:
 * <pre>
 * {@code
 * @RequireAllPermissions({Permission.JOB_CREATE, Permission.SCHEDULE_CREATE})
 * public void createScheduledJob(Job job, Schedule schedule) { ... }
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireAllPermissions {
    /**
     * The permissions (user must have all of them)
     */
    Permission[] value();
}
