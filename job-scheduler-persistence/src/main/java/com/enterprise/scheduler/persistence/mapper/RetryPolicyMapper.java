package com.enterprise.scheduler.persistence.mapper;

import com.enterprise.scheduler.core.domain.RetryPolicy;
import com.enterprise.scheduler.persistence.entity.RetryPolicyEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between RetryPolicy domain model and RetryPolicyEntity
 * Feature #19: Job Retry Mechanism
 */
@Component
public class RetryPolicyMapper {

    /**
     * Convert domain model to entity
     */
    public RetryPolicyEntity toEntity(RetryPolicy domain) {
        if (domain == null) {
            return null;
        }

        return RetryPolicyEntity.builder()
            .maxAttempts(domain.getMaxAttempts())
            .initialDelayMs(domain.getInitialDelayMs())
            .maxDelayMs(domain.getMaxDelayMs())
            .backoffMultiplier(domain.getBackoffMultiplier())
            .strategy(domain.getStrategy())
            .retryOnTimeout(domain.isRetryOnTimeout())
            .build();
    }

    /**
     * Convert entity to domain model
     */
    public RetryPolicy toDomain(RetryPolicyEntity entity) {
        if (entity == null) {
            return null;
        }

        return RetryPolicy.builder()
            .maxAttempts(entity.getMaxAttempts() != null ? entity.getMaxAttempts() : 3)
            .initialDelayMs(entity.getInitialDelayMs() != null ? entity.getInitialDelayMs() : 1000L)
            .maxDelayMs(entity.getMaxDelayMs() != null ? entity.getMaxDelayMs() : 60000L)
            .backoffMultiplier(entity.getBackoffMultiplier() != null ? entity.getBackoffMultiplier() : 2.0)
            .strategy(entity.getStrategy() != null ? entity.getStrategy() : RetryPolicy.RetryStrategy.EXPONENTIAL_BACKOFF)
            .retryOnTimeout(entity.getRetryOnTimeout() != null ? entity.getRetryOnTimeout() : true)
            .build();
    }
}
