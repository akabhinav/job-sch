package com.enterprise.scheduler.core.domain;

/**
 * Strategy for handling missed job executions
 * Feature #3: Advanced Scheduling
 */
public enum MisfireStrategy {
    /**
     * Fire the job once immediately
     */
    FIRE_ONCE,

    /**
     * Fire all missed executions
     */
    FIRE_ALL,

    /**
     * Ignore missed executions
     */
    IGNORE,

    /**
     * Fire the next scheduled execution
     */
    FIRE_NEXT
}
