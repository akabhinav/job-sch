package com.enterprise.scheduler.api.dto;

import com.enterprise.scheduler.common.model.ScheduleType;
import com.enterprise.scheduler.core.domain.MisfireStrategy;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Schedule creation/update request
 * Feature #67: Schedule Management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Schedule creation/update request")
public class ScheduleRequest {

    @Schema(description = "Schedule type", example = "CRON", required = true)
    @NotNull(message = "Schedule type is required")
    private ScheduleType type;

    @Schema(description = "Cron expression", example = "0 0 * * *")
    private String cronExpression;

    @Schema(description = "Interval in milliseconds", example = "3600000")
    private Long intervalMs;

    @Schema(description = "Start time")
    private Instant startTime;

    @Schema(description = "End time")
    private Instant endTime;

    @Schema(description = "Timezone", example = "UTC")
    private ZoneId timezone;

    @Schema(description = "Maximum executions")
    private Integer maxExecutions;

    @Schema(description = "Misfire strategy", example = "FIRE_ONCE")
    private MisfireStrategy misfireStrategy;

    @Schema(description = "Is schedule active", example = "true")
    private Boolean active;
}
