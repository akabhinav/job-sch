package com.enterprise.scheduler.core.spi;

import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;

/**
 * Service Provider Interface for job executors
 * Features #41: Plugin Architecture, #42: Custom Job Type Support, #50: Custom Executor Support
 */
public interface JobExecutor {

    /**
     * Get the job type this executor supports
     */
    String getSupportedType();

    /**
     * Execute a job
     *
     * @param job       the job to execute
     * @param execution the execution context
     * @return execution result
     */
    ExecutionResult execute(Job job, JobExecution execution);

    /**
     * Cancel an executing job
     *
     * @param execution the execution to cancel
     */
    void cancel(JobExecution execution);

    /**
     * Check if this executor can handle the given job
     */
    default boolean canHandle(Job job) {
        return getSupportedType().equals(job.getType());
    }

    /**
     * Get executor priority (higher values = higher priority)
     */
    default int getPriority() {
        return 0;
    }
}
