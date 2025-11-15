package com.enterprise.scheduler.core.service;

import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Workflow orchestration service
 * Feature #15: Job Workflow Orchestration
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowOrchestrator {

    private final JobRepository jobRepository;
    private final SchedulerService schedulerService;
    private final DependencyResolver dependencyResolver;

    public CompletableFuture<List<JobExecution>> executeWorkflow(List<String> jobIds) {
        List<Job> jobs = jobIds.stream()
                .map(id -> jobRepository.findById(id).orElseThrow())
                .collect(Collectors.toList());

        List<String> executionOrder = dependencyResolver.topologicalSort(jobs);

        return CompletableFuture.supplyAsync(() -> {
            List<JobExecution> executions = new ArrayList<>();
            Map<String, Object> sharedContext = new HashMap<>();

            for (String jobId : executionOrder) {
                Job job = jobRepository.findById(jobId).orElseThrow();

                // Merge shared context with job parameters
                Map<String, Object> params = new HashMap<>(job.getParameters());
                params.putAll(sharedContext);

                JobExecution execution = schedulerService.executeManually(jobId, params);
                executions.add(execution);

                // Update shared context with execution results
                if (execution.getContext() != null) {
                    sharedContext.putAll(execution.getContext());
                }
            }

            return executions;
        });
    }

    public CompletableFuture<List<JobExecution>> executeParallelWorkflow(List<String> jobIds) {
        List<Job> jobs = jobIds.stream()
                .map(id -> jobRepository.findById(id).orElseThrow())
                .collect(Collectors.toList());

        List<CompletableFuture<JobExecution>> futures = jobs.stream()
                .map(job -> CompletableFuture.supplyAsync(() ->
                    schedulerService.scheduleJob(job)))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toList()));
    }
}
