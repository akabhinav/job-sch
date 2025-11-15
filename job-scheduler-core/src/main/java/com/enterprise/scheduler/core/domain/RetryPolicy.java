package com.enterprise.scheduler.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Retry policy configuration for failed jobs
 * Feature #19: Job Retry Mechanism
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetryPolicy {

    /**
     * Maximum number of retry attempts
     */
    @Builder.Default
    private int maxAttempts = 3;

    /**
     * Initial retry delay in milliseconds
     */
    @Builder.Default
    private long initialDelayMs = 1000;

    /**
     * Maximum retry delay in milliseconds
     */
    @Builder.Default
    private long maxDelayMs = 60000;

    /**
     * Backoff multiplier for exponential backoff
     */
    @Builder.Default
    private double backoffMultiplier = 2.0;

    /**
     * Retry strategy
     */
    @Builder.Default
    private RetryStrategy strategy = RetryStrategy.EXPONENTIAL_BACKOFF;

    /**
     * Whether to retry on timeout
     */
    @Builder.Default
    private boolean retryOnTimeout = true;

    /**
     * Calculate delay for a given attempt number
     */
    public long calculateDelay(int attemptNumber) {
        if (strategy == RetryStrategy.FIXED) {
            return initialDelayMs;
        } else if (strategy == RetryStrategy.EXPONENTIAL_BACKOFF) {
            long delay = (long) (initialDelayMs * Math.pow(backoffMultiplier, attemptNumber - 1));
            return Math.min(delay, maxDelayMs);
        } else if (strategy == RetryStrategy.LINEAR) {
            long delay = initialDelayMs * attemptNumber;
            return Math.min(delay, maxDelayMs);
        }
        return initialDelayMs;
    }

    public enum RetryStrategy {
        FIXED,
        EXPONENTIAL_BACKOFF,
        LINEAR
    }
}
