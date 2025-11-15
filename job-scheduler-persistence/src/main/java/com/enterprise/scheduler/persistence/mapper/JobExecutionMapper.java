package com.enterprise.scheduler.persistence.mapper;

import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.persistence.entity.JobExecutionEntity;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting between JobExecution domain model and JobExecutionEntity
 * Feature #22: Job Execution History
 */
@Component
public class JobExecutionMapper {

    /**
     * Convert domain model to entity
     */
    public JobExecutionEntity toEntity(JobExecution domain) {
        if (domain == null) {
            return null;
        }

        return JobExecutionEntity.builder()
            .id(domain.getId())
            .jobId(domain.getJobId())
            .jobName(domain.getJobName())
            .status(domain.getStatus())
            .startTime(domain.getStartTime())
            .endTime(domain.getEndTime())
            .durationMs(domain.getDuration() != null ? domain.getDuration().toMillis() : null)
            .triggerType(domain.getTriggerType())
            .triggeredBy(domain.getTriggeredBy())
            .parameters(domain.getParameters())
            .context(domain.getContext())
            .result(domain.getResult())
            .errorMessage(domain.getErrorMessage())
            .stackTrace(domain.getStackTrace())
            .attemptNumber(domain.getAttemptNumber())
            .nodeId(domain.getNodeId())
            .processId(domain.getProcessId())
            .threadName(domain.getThreadName())
            .logs(domain.getLogs())
            .logReference(domain.getLogReference())
            .metrics(domain.getMetrics())
            .tenantId(domain.getTenantId())
            .build();
    }

    /**
     * Convert entity to domain model
     */
    public JobExecution toDomain(JobExecutionEntity entity) {
        if (entity == null) {
            return null;
        }

        return JobExecution.builder()
            .id(entity.getId())
            .jobId(entity.getJobId())
            .jobName(entity.getJobName())
            .status(entity.getStatus())
            .startTime(entity.getStartTime())
            .endTime(entity.getEndTime())
            .duration(entity.getDurationMs() != null ?
                java.time.Duration.ofMillis(entity.getDurationMs()) : null)
            .triggerType(entity.getTriggerType())
            .triggeredBy(entity.getTriggeredBy())
            .parameters(entity.getParameters())
            .context(entity.getContext())
            .result(entity.getResult())
            .errorMessage(entity.getErrorMessage())
            .stackTrace(entity.getStackTrace())
            .attemptNumber(entity.getAttemptNumber() != null ? entity.getAttemptNumber() : 1)
            .nodeId(entity.getNodeId())
            .processId(entity.getProcessId())
            .threadName(entity.getThreadName())
            .logs(entity.getLogs())
            .logReference(entity.getLogReference())
            .metrics(entity.getMetrics())
            .tenantId(entity.getTenantId())
            .build();
    }

    /**
     * Update entity from domain model (for updates)
     */
    public void updateEntity(JobExecution domain, JobExecutionEntity entity) {
        if (domain == null || entity == null) {
            return;
        }

        entity.setStatus(domain.getStatus());
        entity.setEndTime(domain.getEndTime());
        entity.setDurationMs(domain.getDuration() != null ? domain.getDuration().toMillis() : null);
        entity.setContext(domain.getContext());
        entity.setResult(domain.getResult());
        entity.setErrorMessage(domain.getErrorMessage());
        entity.setStackTrace(domain.getStackTrace());
        entity.setLogs(domain.getLogs());
        entity.setLogReference(domain.getLogReference());
        entity.setMetrics(domain.getMetrics());
    }
}
