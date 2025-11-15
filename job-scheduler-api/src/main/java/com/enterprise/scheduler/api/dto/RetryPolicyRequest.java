package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Retry policy request
 * Feature #19: Job Retry Mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Retry policy configuration")
public class RetryPolicyRequest {

    @Schema(description = "Maximum retry attempts", example = "3")
    private Integer maxAttempts;

    @Schema(description = "Delay between retries in milliseconds", example = "5000")
    private Long retryDelayMs;

    @Schema(description = "Retry backoff multiplier", example = "2.0")
    private Double backoffMultiplier;

    @Schema(description = "Maximum retry delay in milliseconds", example = "60000")
    private Long maxRetryDelayMs;

    @Schema(description = "Retry on specific exception types")
    private String[] retryableExceptions;
}
