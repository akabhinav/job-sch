package com.enterprise.scheduler.api.dto;

import com.enterprise.scheduler.core.domain.*;
import org.springframework.stereotype.Component;

/**
 * DTO mapper for converting between domain models and DTOs
 * Feature #43: REST API Design
 */
@Component
public class DtoMapper {

    public JobResponse toJobResponse(Job job) {
        if (job == null) {
            return null;
        }

        return JobResponse.builder()
                .id(job.getId())
                .name(job.getName())
                .description(job.getDescription())
                .type(job.getType())
                .group(job.getGroup())
                .status(job.getStatus())
                .priority(job.getPriority())
                .version(job.getVersion())
                .configuration(job.getConfiguration())
                .parameters(job.getParameters())
                .dependencies(job.getDependencies())
                .schedule(toScheduleResponse(job.getSchedule()))
                .retryPolicy(toRetryPolicyResponse(job.getRetryPolicy()))
                .timeoutMs(job.getTimeoutMs())
                .maxConcurrentExecutions(job.getMaxConcurrentExecutions())
                .tenantId(job.getTenantId())
                .tags(job.getTags())
                .metadata(job.getMetadata())
                .createdAt(job.getCreatedAt())
                .updatedAt(job.getUpdatedAt())
                .createdBy(job.getCreatedBy())
                .updatedBy(job.getUpdatedBy())
                .enabled(job.isEnabled())
                .build();
    }

    public Job toJob(JobRequest request) {
        if (request == null) {
            return null;
        }

        return Job.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .group(request.getGroup())
                .priority(request.getPriority())
                .configuration(request.getConfiguration())
                .parameters(request.getParameters())
                .dependencies(request.getDependencies())
                .schedule(toSchedule(request.getSchedule()))
                .retryPolicy(toRetryPolicy(request.getRetryPolicy()))
                .timeoutMs(request.getTimeoutMs())
                .maxConcurrentExecutions(request.getMaxConcurrentExecutions())
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .enabled(request.getEnabled() != null ? request.getEnabled() : true)
                .build();
    }

    public ScheduleResponse toScheduleResponse(Schedule schedule) {
        if (schedule == null) {
            return null;
        }

        return ScheduleResponse.builder()
                .id(schedule.getId())
                .type(schedule.getType())
                .cronExpression(schedule.getCronExpression())
                .intervalMs(schedule.getIntervalMs())
                .startTime(schedule.getStartTime())
                .endTime(schedule.getEndTime())
                .timezone(schedule.getTimezone())
                .maxExecutions(schedule.getMaxExecutions())
                .executionCount(schedule.getExecutionCount())
                .active(schedule.isActive())
                .lastExecutionTime(schedule.getLastExecutionTime())
                .nextExecutionTime(schedule.getNextExecutionTime())
                .misfireStrategy(schedule.getMisfireStrategy())
                .build();
    }

    public Schedule toSchedule(ScheduleRequest request) {
        if (request == null) {
            return null;
        }

        return Schedule.builder()
                .type(request.getType())
                .cronExpression(request.getCronExpression())
                .intervalMs(request.getIntervalMs())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .timezone(request.getTimezone())
                .maxExecutions(request.getMaxExecutions())
                .misfireStrategy(request.getMisfireStrategy())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
    }

    public RetryPolicyResponse toRetryPolicyResponse(RetryPolicy retryPolicy) {
        if (retryPolicy == null) {
            return null;
        }

        return RetryPolicyResponse.builder()
                .maxAttempts(retryPolicy.getMaxAttempts())
                .retryDelayMs(retryPolicy.getRetryDelayMs())
                .backoffMultiplier(retryPolicy.getBackoffMultiplier())
                .maxRetryDelayMs(retryPolicy.getMaxRetryDelayMs())
                .retryableExceptions(retryPolicy.getRetryableExceptions())
                .build();
    }

    public RetryPolicy toRetryPolicy(RetryPolicyRequest request) {
        if (request == null) {
            return null;
        }

        return RetryPolicy.builder()
                .maxAttempts(request.getMaxAttempts())
                .retryDelayMs(request.getRetryDelayMs())
                .backoffMultiplier(request.getBackoffMultiplier())
                .maxRetryDelayMs(request.getMaxRetryDelayMs())
                .retryableExceptions(request.getRetryableExceptions())
                .build();
    }

    public JobExecutionResponse toJobExecutionResponse(JobExecution execution) {
        if (execution == null) {
            return null;
        }

        return JobExecutionResponse.builder()
                .id(execution.getId())
                .jobId(execution.getJobId())
                .jobName(execution.getJobName())
                .status(execution.getStatus())
                .startTime(execution.getStartTime())
                .endTime(execution.getEndTime())
                .duration(execution.getDuration())
                .triggerType(execution.getTriggerType())
                .triggeredBy(execution.getTriggeredBy())
                .parameters(execution.getParameters())
                .context(execution.getContext())
                .result(execution.getResult())
                .errorMessage(execution.getErrorMessage())
                .stackTrace(execution.getStackTrace())
                .attemptNumber(execution.getAttemptNumber())
                .nodeId(execution.getNodeId())
                .processId(execution.getProcessId())
                .threadName(execution.getThreadName())
                .logs(execution.getLogs())
                .logReference(execution.getLogReference())
                .metrics(execution.getMetrics())
                .tenantId(execution.getTenantId())
                .build();
    }

    public TemplateResponse toTemplateResponse(JobTemplate template) {
        if (template == null) {
            return null;
        }

        return TemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .description(template.getDescription())
                .type(template.getType())
                .defaultConfiguration(template.getDefaultConfiguration())
                .defaultParameters(template.getDefaultParameters())
                .variables(template.getVariables())
                .category(template.getCategory())
                .tags(template.getTags())
                .metadata(template.getMetadata())
                .createdAt(template.getCreatedAt())
                .createdBy(template.getCreatedBy())
                .version(template.getVersion())
                .tenantId(template.getTenantId())
                .build();
    }

    public JobTemplate toTemplate(TemplateRequest request) {
        if (request == null) {
            return null;
        }

        return JobTemplate.builder()
                .name(request.getName())
                .description(request.getDescription())
                .type(request.getType())
                .defaultConfiguration(request.getDefaultConfiguration())
                .defaultParameters(request.getDefaultParameters())
                .variables(request.getVariables())
                .category(request.getCategory())
                .tags(request.getTags())
                .metadata(request.getMetadata())
                .build();
    }
}
