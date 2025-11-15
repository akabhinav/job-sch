package com.enterprise.scheduler.persistence.entity;

import com.enterprise.scheduler.common.model.ScheduleType;
import com.enterprise.scheduler.core.domain.MisfireStrategy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * JPA entity for Schedule
 * Feature #3: Advanced Scheduling
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "schedules", indexes = {
    @Index(name = "idx_schedule_active", columnList = "active"),
    @Index(name = "idx_schedule_next_execution", columnList = "next_execution_time")
})
public class ScheduleEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private ScheduleType type;

    @Column(name = "cron_expression", length = 255)
    private String cronExpression;

    @Column(name = "interval_ms")
    private Long intervalMs;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "timezone", length = 100)
    private String timezone;

    @Column(name = "max_executions")
    private Integer maxExecutions;

    @Column(name = "execution_count", nullable = false)
    @Builder.Default
    private Integer executionCount = 0;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_execution_time")
    private Instant lastExecutionTime;

    @Column(name = "next_execution_time")
    private Instant nextExecutionTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "misfire_strategy", length = 50)
    @Builder.Default
    private MisfireStrategy misfireStrategy = MisfireStrategy.FIRE_ONCE;
}
