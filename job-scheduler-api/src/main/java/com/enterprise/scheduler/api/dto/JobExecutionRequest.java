package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Job execution request (for manual triggers)
 * Features #22, #66: Execution Management
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job execution request for manual triggers")
public class JobExecutionRequest {

    @Schema(description = "Execution parameters (override job defaults)")
    private Map<String, Object> parameters;

    @Schema(description = "Execution context")
    private Map<String, Object> context;

    @Schema(description = "Triggered by user")
    private String triggeredBy;
}
