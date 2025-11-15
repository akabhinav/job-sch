package com.enterprise.scheduler.persistence.mapper;

import com.enterprise.scheduler.core.domain.Schedule;
import com.enterprise.scheduler.persistence.entity.ScheduleEntity;
import org.springframework.stereotype.Component;

import java.time.ZoneId;

/**
 * Mapper for converting between Schedule domain model and ScheduleEntity
 * Feature #3: Advanced Scheduling
 */
@Component
public class ScheduleMapper {

    /**
     * Convert domain model to entity
     */
    public ScheduleEntity toEntity(Schedule domain) {
        if (domain == null) {
            return null;
        }

        return ScheduleEntity.builder()
            .id(domain.getId())
            .type(domain.getType())
            .cronExpression(domain.getCronExpression())
            .intervalMs(domain.getIntervalMs())
            .startTime(domain.getStartTime())
            .endTime(domain.getEndTime())
            .timezone(domain.getTimezone() != null ? domain.getTimezone().getId() : null)
            .maxExecutions(domain.getMaxExecutions())
            .executionCount(domain.getExecutionCount())
            .active(domain.isActive())
            .lastExecutionTime(domain.getLastExecutionTime())
            .nextExecutionTime(domain.getNextExecutionTime())
            .misfireStrategy(domain.getMisfireStrategy())
            .build();
    }

    /**
     * Convert entity to domain model
     */
    public Schedule toDomain(ScheduleEntity entity) {
        if (entity == null) {
            return null;
        }

        return Schedule.builder()
            .id(entity.getId())
            .type(entity.getType())
            .cronExpression(entity.getCronExpression())
            .intervalMs(entity.getIntervalMs())
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .timezone(entity.getTimezone() != null ? ZoneId.of(entity.getTimezone()) : ZoneId.systemDefault())
            .maxExecutions(entity.getMaxExecutions())
            .executionCount(entity.getExecutionCount() != null ? entity.getExecutionCount() : 0)
            .active(entity.getActive() != null ? entity.getActive() : true)
            .lastExecutionTime(entity.getLastExecutionTime())
            .nextExecutionTime(entity.getNextExecutionTime())
            .misfireStrategy(entity.getMisfireStrategy())
            .build();
    }

    /**
     * Update entity from domain model (for updates)
     */
    public void updateEntity(Schedule domain, ScheduleEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setType(domain.getType());
        entity.setCronExpression(domain.getCronExpression());
        entity.setIntervalMs(domain.getIntervalMs());
        entity.setStartTime(domain.getStartTime());
        entity.setEndTime(domain.getEndTime());
        entity.setTimezone(domain.getTimezone() != null ? domain.getTimezone().getId() : null);
        entity.setMaxExecutions(domain.getMaxExecutions());
        entity.setExecutionCount(domain.getExecutionCount());
        entity.setActive(domain.isActive());
        entity.setLastExecutionTime(domain.getLastExecutionTime());
        entity.setNextExecutionTime(domain.getNextExecutionTime());
        entity.setMisfireStrategy(domain.getMisfireStrategy());
    }
}
