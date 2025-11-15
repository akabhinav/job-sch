package com.enterprise.scheduler.common.model;

/**
 * Schedule type enumeration
 * Feature #3: Advanced Scheduling
 */
public enum ScheduleType {
    /**
     * Cron-based scheduling (e.g., "0 0 * * * ?")
     */
    CRON,

    /**
     * Fixed interval scheduling (e.g., every 5 minutes)
     */
    INTERVAL,

    /**
     * Fixed rate scheduling
     */
    FIXED_RATE,

    /**
     * One-time execution at a specific time
     */
    ONE_TIME,

    /**
     * Manual trigger only
     */
    MANUAL,

    /**
     * Event-driven scheduling
     */
    EVENT_DRIVEN,

    /**
     * Dependency-based scheduling
     */
    DEPENDENCY_BASED
}
