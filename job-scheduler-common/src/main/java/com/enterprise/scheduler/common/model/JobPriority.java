package com.enterprise.scheduler.common.model;

/**
 * Job priority levels
 * Feature #8: Job Priority Management
 */
public enum JobPriority {
    LOWEST(0),
    LOW(1),
    NORMAL(2),
    HIGH(3),
    HIGHEST(4),
    CRITICAL(5);

    private final int value;

    JobPriority(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static JobPriority fromValue(int value) {
        for (JobPriority priority : values()) {
            if (priority.value == value) {
                return priority;
            }
        }
        return NORMAL;
    }
}
