package com.enterprise.scheduler.core.spi;

import com.enterprise.scheduler.common.model.JobStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * Result of job execution
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionResult {

    /**
     * Execution status
     */
    private JobStatus status;

    /**
     * Result data
     */
    private Object result;

    /**
     * Error message if failed
     */
    private String errorMessage;

    /**
     * Error details
     */
    private Throwable throwable;

    /**
     * Output logs
     */
    private String logs;

    /**
     * Execution metrics
     */
    @Builder.Default
    private Map<String, Object> metrics = new HashMap<>();

    /**
     * Context data to pass to dependent jobs
     */
    @Builder.Default
    private Map<String, Object> context = new HashMap<>();

    public static ExecutionResult success(Object result) {
        return ExecutionResult.builder()
                .status(JobStatus.SUCCESS)
                .result(result)
                .build();
    }

    public static ExecutionResult failure(String errorMessage, Throwable throwable) {
        return ExecutionResult.builder()
                .status(JobStatus.FAILED)
                .errorMessage(errorMessage)
                .throwable(throwable)
                .build();
    }

    public static ExecutionResult timeout() {
        return ExecutionResult.builder()
                .status(JobStatus.TIMEOUT)
                .errorMessage("Job execution timed out")
                .build();
    }

    public static ExecutionResult cancelled() {
        return ExecutionResult.builder()
                .status(JobStatus.CANCELLED)
                .errorMessage("Job execution was cancelled")
                .build();
    }
}
