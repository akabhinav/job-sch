package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Retry policy response
 * Feature #19: Job Retry Mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Retry policy configuration")
public class RetryPolicyResponse {

    @Schema(description = "Maximum retry attempts")
    private Integer maxAttempts;

    @Schema(description = "Delay between retries in milliseconds")
    private Long retryDelayMs;

    @Schema(description = "Retry backoff multiplier")
    private Double backoffMultiplier;

    @Schema(description = "Maximum retry delay in milliseconds")
    private Long maxRetryDelayMs;

    @Schema(description = "Retry on specific exception types")
    private String[] retryableExceptions;
}
