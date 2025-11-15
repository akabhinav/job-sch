package com.enterprise.scheduler.persistence.mapper;

import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.persistence.entity.JobEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;

/**
 * Mapper for converting between Job domain model and JobEntity
 * Feature #5: Job Persistence
 */
@Component
@RequiredArgsConstructor
public class JobMapper {

    private final ScheduleMapper scheduleMapper;
    private final RetryPolicyMapper retryPolicyMapper;

    /**
     * Convert domain model to entity
     */
    public JobEntity toEntity(Job domain) {
        if (domain == null) {
            return null;
        }

        return JobEntity.builder()
            .id(domain.getId())
            .name(domain.getName())
            .description(domain.getDescription())
            .type(domain.getType())
            .group(domain.getGroup())
            .status(domain.getStatus())
            .priority(domain.getPriority())
            .version(domain.getVersion())
            .configuration(domain.getConfiguration())
            .parameters(domain.getParameters())
            .dependencies(domain.getDependencies() != null ? new HashSet<>(domain.getDependencies()) : new HashSet<>())
            .schedule(scheduleMapper.toEntity(domain.getSchedule()))
            .retryPolicy(retryPolicyMapper.toEntity(domain.getRetryPolicy()))
            .timeoutMs(domain.getTimeoutMs())
            .maxConcurrentExecutions(domain.getMaxConcurrentExecutions())
            .tenantId(domain.getTenantId())
            .tags(domain.getTags() != null ? new HashSet<>(domain.getTags()) : new HashSet<>())
            .metadata(domain.getMetadata())
            .createdAt(domain.getCreatedAt())
            .updatedAt(domain.getUpdatedAt())
            .createdBy(domain.getCreatedBy())
            .updatedBy(domain.getUpdatedBy())
            .enabled(domain.isEnabled())
            .build();
    }

    /**
     * Convert entity to domain model
     */
    public Job toDomain(JobEntity entity) {
        if (entity == null) {
            return null;
        }

        return Job.builder()
            .id(entity.getId())
            .name(entity.getName())
            .description(entity.getDescription())
            .type(entity.getType())
            .group(entity.getGroup())
            .status(entity.getStatus())
            .priority(entity.getPriority())
            .version(entity.getVersion())
            .configuration(entity.getConfiguration())
            .parameters(entity.getParameters())
            .dependencies(entity.getDependencies() != null ? new HashSet<>(entity.getDependencies()) : new HashSet<>())
            .schedule(scheduleMapper.toDomain(entity.getSchedule()))
            .retryPolicy(retryPolicyMapper.toDomain(entity.getRetryPolicy()))
            .timeoutMs(entity.getTimeoutMs())
            .maxConcurrentExecutions(entity.getMaxConcurrentExecutions())
            .tenantId(entity.getTenantId())
            .tags(entity.getTags() != null ? new HashSet<>(entity.getTags()) : new HashSet<>())
            .metadata(entity.getMetadata())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedBy(entity.getUpdatedBy())
            .enabled(entity.getEnabled() != null ? entity.getEnabled() : true)
            .build();
    }

    /**
     * Update entity from domain model (for updates)
     */
    public void updateEntity(Job domain, JobEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setType(domain.getType());
        entity.setGroup(domain.getGroup());
        entity.setStatus(domain.getStatus());
        entity.setPriority(domain.getPriority());
        entity.setConfiguration(domain.getConfiguration());
        entity.setParameters(domain.getParameters());
        entity.setDependencies(domain.getDependencies() != null ? new HashSet<>(domain.getDependencies()) : new HashSet<>());

        // Update schedule
        if (domain.getSchedule() != null) {
            if (entity.getSchedule() != null) {
                scheduleMapper.updateEntity(domain.getSchedule(), entity.getSchedule());
            } else {
                entity.setSchedule(scheduleMapper.toEntity(domain.getSchedule()));
            }
        } else {
            entity.setSchedule(null);
        }

        entity.setRetryPolicy(retryPolicyMapper.toEntity(domain.getRetryPolicy()));
        entity.setTimeoutMs(domain.getTimeoutMs());
        entity.setMaxConcurrentExecutions(domain.getMaxConcurrentExecutions());
        entity.setTenantId(domain.getTenantId());
        entity.setTags(domain.getTags() != null ? new HashSet<>(domain.getTags()) : new HashSet<>());
        entity.setMetadata(domain.getMetadata());
        entity.setUpdatedBy(domain.getUpdatedBy());
        entity.setEnabled(domain.isEnabled());
    }
}
