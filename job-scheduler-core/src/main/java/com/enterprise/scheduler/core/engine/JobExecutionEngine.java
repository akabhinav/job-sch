package com.enterprise.scheduler.core.engine;

import com.enterprise.scheduler.common.exception.JobExecutionException;
import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.common.util.IdGenerator;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.event.JobEvent;
import com.enterprise.scheduler.core.spi.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * Core job execution engine
 * Features #2, #11, #12, #18, #19, #20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobExecutionEngine {

    private final List<JobExecutor> jobExecutors;
    private final JobExecutionRepository executionRepository;
    private final EventPublisher eventPublisher;
    private final ExecutorService executorService;
    private final Map<String, Future<?>> runningExecutions = new ConcurrentHashMap<>();

    public JobExecution executeJob(Job job, Map<String, Object> parameters, JobExecution.TriggerType triggerType) {
        JobExecutor executor = findExecutor(job);

        if (executor == null) {
            throw new JobExecutionException("No executor found for job type: " + job.getType());
        }

        JobExecution execution = createExecution(job, parameters, triggerType);
        execution = executionRepository.save(execution);

        publishEvent(JobEvent.EventType.EXECUTION_STARTED, execution);

        final JobExecution finalExecution = execution;
        Future<?> future = executorService.submit(() -> executeWithTimeout(job, finalExecution, executor));
        runningExecutions.put(execution.getId(), future);

        return execution;
    }

    private void executeWithTimeout(Job job, JobExecution execution, JobExecutor executor) {
        try {
            execution.setStartTime(Instant.now());
            execution.setStatus(JobStatus.RUNNING);
            executionRepository.save(execution);

            ExecutionResult result;

            if (job.getTimeoutMs() != null) {
                result = executeWithTimeoutInternal(job, execution, executor);
            } else {
                result = executor.execute(job, execution);
            }

            completeExecution(execution, result);

        } catch (Exception e) {
            log.error("Error executing job: {}", job.getId(), e);
            handleExecutionError(execution, e);
        } finally {
            runningExecutions.remove(execution.getId());
        }
    }

    private ExecutionResult executeWithTimeoutInternal(Job job, JobExecution execution, JobExecutor executor) {
        ExecutorService timeoutExecutor = Executors.newSingleThreadExecutor();

        try {
            Future<ExecutionResult> future = timeoutExecutor.submit(() -> executor.execute(job, execution));
            return future.get(job.getTimeoutMs(), TimeUnit.MILLISECONDS);

        } catch (TimeoutException e) {
            log.warn("Job execution timed out: {}", job.getId());
            executor.cancel(execution);
            return ExecutionResult.timeout();

        } catch (InterruptedException | ExecutionException e) {
            throw new JobExecutionException("Execution failed", e);

        } finally {
            timeoutExecutor.shutdownNow();
        }
    }

    private void completeExecution(JobExecution execution, ExecutionResult result) {
        execution.setEndTime(Instant.now());
        execution.setDuration(Duration.between(execution.getStartTime(), execution.getEndTime()));
        execution.setStatus(result.getStatus());
        execution.setResult(result.getResult());
        execution.setLogs(result.getLogs());
        execution.setMetrics(result.getMetrics());

        if (result.getErrorMessage() != null) {
            execution.setErrorMessage(result.getErrorMessage());
        }

        if (result.getThrowable() != null) {
            execution.setStackTrace(getStackTrace(result.getThrowable()));
        }

        if (result.getContext() != null) {
            execution.getContext().putAll(result.getContext());
        }

        executionRepository.save(execution);

        JobEvent.EventType eventType = switch (result.getStatus()) {
            case SUCCESS -> JobEvent.EventType.EXECUTION_COMPLETED;
            case FAILED -> JobEvent.EventType.EXECUTION_FAILED;
            case CANCELLED -> JobEvent.EventType.EXECUTION_CANCELLED;
            case TIMEOUT -> JobEvent.EventType.EXECUTION_TIMEOUT;
            default -> JobEvent.EventType.EXECUTION_COMPLETED;
        };

        publishEvent(eventType, execution);
    }

    private void handleExecutionError(JobExecution execution, Exception e) {
        execution.setEndTime(Instant.now());
        execution.setStatus(JobStatus.FAILED);
        execution.setErrorMessage(e.getMessage());
        execution.setStackTrace(getStackTrace(e));
        executionRepository.save(execution);

        publishEvent(JobEvent.EventType.EXECUTION_FAILED, execution);
    }

    public void cancelExecution(String executionId) {
        JobExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new JobExecutionException("Execution not found: " + executionId));

        Future<?> future = runningExecutions.get(executionId);
        if (future != null) {
            future.cancel(true);
        }

        execution.setStatus(JobStatus.CANCELLED);
        execution.setEndTime(Instant.now());
        executionRepository.save(execution);

        publishEvent(JobEvent.EventType.EXECUTION_CANCELLED, execution);
    }

    private JobExecutor findExecutor(Job job) {
        return jobExecutors.stream()
                .filter(executor -> executor.canHandle(job))
                .max(Comparator.comparingInt(JobExecutor::getPriority))
                .orElse(null);
    }

    private JobExecution createExecution(Job job, Map<String, Object> parameters, JobExecution.TriggerType triggerType) {
        return JobExecution.builder()
                .id(IdGenerator.generateExecutionId())
                .jobId(job.getId())
                .jobName(job.getName())
                .status(JobStatus.SCHEDULED)
                .triggerType(triggerType)
                .parameters(parameters != null ? parameters : job.getParameters())
                .tenantId(job.getTenantId())
                .build();
    }

    private void publishEvent(JobEvent.EventType eventType, JobExecution execution) {
        eventPublisher.publishAsync(JobEvent.builder()
                .type(eventType)
                .jobId(execution.getJobId())
                .executionId(execution.getId())
                .tenantId(execution.getTenantId())
                .build());
    }

    private String getStackTrace(Throwable throwable) {
        StringBuilder sb = new StringBuilder();
        sb.append(throwable.toString()).append("\n");
        for (StackTraceElement element : throwable.getStackTrace()) {
            sb.append("\tat ").append(element.toString()).append("\n");
        }
        return sb.toString();
    }
}
