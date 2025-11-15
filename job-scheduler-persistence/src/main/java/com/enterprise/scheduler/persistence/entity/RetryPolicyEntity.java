package com.enterprise.scheduler.persistence.entity;

import com.enterprise.scheduler.core.domain.RetryPolicy.RetryStrategy;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Embeddable retry policy configuration
 * Feature #19: Job Retry Mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class RetryPolicyEntity {

    @Column(name = "retry_max_attempts")
    private Integer maxAttempts;

    @Column(name = "retry_initial_delay_ms")
    private Long initialDelayMs;

    @Column(name = "retry_max_delay_ms")
    private Long maxDelayMs;

    @Column(name = "retry_backoff_multiplier")
    private Double backoffMultiplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "retry_strategy", length = 50)
    private RetryStrategy strategy;

    @Column(name = "retry_on_timeout")
    private Boolean retryOnTimeout;
}
