package com.enterprise.scheduler.api.dto;

import com.enterprise.scheduler.common.model.ScheduleType;
import com.enterprise.scheduler.core.domain.MisfireStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.ZoneId;

/**
 * Schedule response
 * Feature #67: Schedule Management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Schedule response")
public class ScheduleResponse {

    @Schema(description = "Schedule ID")
    private String id;

    @Schema(description = "Schedule type")
    private ScheduleType type;

    @Schema(description = "Cron expression")
    private String cronExpression;

    @Schema(description = "Interval in milliseconds")
    private Long intervalMs;

    @Schema(description = "Start time")
    private Instant startTime;

    @Schema(description = "End time")
    private Instant endTime;

    @Schema(description = "Timezone")
    private ZoneId timezone;

    @Schema(description = "Maximum executions")
    private Integer maxExecutions;

    @Schema(description = "Current execution count")
    private Integer executionCount;

    @Schema(description = "Is schedule active")
    private boolean active;

    @Schema(description = "Last execution time")
    private Instant lastExecutionTime;

    @Schema(description = "Next execution time")
    private Instant nextExecutionTime;

    @Schema(description = "Misfire strategy")
    private MisfireStrategy misfireStrategy;
}
