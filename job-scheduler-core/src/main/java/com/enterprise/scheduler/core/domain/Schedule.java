package com.enterprise.scheduler.core.domain;

import com.enterprise.scheduler.common.model.ScheduleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Schedule configuration for jobs
 * Feature #3: Advanced Scheduling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Schedule {

    /**
     * Unique schedule identifier
     */
    private String id;

    /**
     * Schedule type
     */
    private ScheduleType type;

    /**
     * Cron expression (for CRON type)
     */
    private String cronExpression;

    /**
     * Interval in milliseconds (for INTERVAL type)
     */
    private Long intervalMs;

    /**
     * Start time for the schedule
     */
    private Instant startTime;

    /**
     * End time for the schedule (null for indefinite)
     */
    private Instant endTime;

    /**
     * Timezone for cron expressions
     */
    @Builder.Default
    private ZoneId timezone = ZoneId.systemDefault();

    /**
     * Maximum number of executions (null for unlimited)
     */
    private Integer maxExecutions;

    /**
     * Current execution count
     */
    @Builder.Default
    private Integer executionCount = 0;

    /**
     * Whether schedule is active
     */
    @Builder.Default
    private boolean active = true;

    /**
     * Last execution time
     */
    private Instant lastExecutionTime;

    /**
     * Next execution time
     */
    private Instant nextExecutionTime;

    /**
     * Misfire handling strategy
     */
    @Builder.Default
    private MisfireStrategy misfireStrategy = MisfireStrategy.FIRE_ONCE;

    /**
     * Check if schedule is still valid
     */
    public boolean isValid() {
        Instant now = Instant.now();
        return active &&
                (startTime == null || now.isAfter(startTime)) &&
                (endTime == null || now.isBefore(endTime)) &&
                (maxExecutions == null || executionCount < maxExecutions);
    }

    /**
     * Increment execution count
     */
    public void incrementExecutionCount() {
        this.executionCount++;
    }
}
