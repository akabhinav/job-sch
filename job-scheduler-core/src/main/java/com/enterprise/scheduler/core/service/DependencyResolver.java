package com.enterprise.scheduler.core.service;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.JobExecutionRepository;
import com.enterprise.scheduler.core.spi.JobRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Dependency resolution service
 * Feature #7: Job Dependency Management
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DependencyResolver {

    private final JobRepository jobRepository;
    private final JobExecutionRepository executionRepository;

    public boolean areDependenciesResolved(Job job) {
        if (job.getDependencies() == null || job.getDependencies().isEmpty()) {
            return true;
        }

        for (String dependencyId : job.getDependencies()) {
            Optional<JobExecution> latestExecution = executionRepository.findLatestByJobId(dependencyId);

            if (latestExecution.isEmpty() || latestExecution.get().getStatus() != JobStatus.SUCCESS) {
                log.debug("Dependency not resolved: {} for job: {}", dependencyId, job.getId());
                return false;
            }
        }

        return true;
    }

    public List<Job> getExecutableJobs(List<Job> jobs) {
        return jobs.stream()
                .filter(Job::canExecute)
                .filter(this::areDependenciesResolved)
                .collect(Collectors.toList());
    }

    public boolean hasCyclicDependency(Job job) {
        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        return hasCycle(job.getId(), visited, recursionStack);
    }

    private boolean hasCycle(String jobId, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(jobId)) {
            return true;
        }

        if (visited.contains(jobId)) {
            return false;
        }

        visited.add(jobId);
        recursionStack.add(jobId);

        Optional<Job> jobOpt = jobRepository.findById(jobId);
        if (jobOpt.isPresent() && jobOpt.get().getDependencies() != null) {
            for (String dependency : jobOpt.get().getDependencies()) {
                if (hasCycle(dependency, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(jobId);
        return false;
    }

    public List<String> topologicalSort(List<Job> jobs) {
        Map<String, Set<String>> graph = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();

        for (Job job : jobs) {
            graph.putIfAbsent(job.getId(), new HashSet<>());
            inDegree.putIfAbsent(job.getId(), 0);

            if (job.getDependencies() != null) {
                for (String dep : job.getDependencies()) {
                    graph.putIfAbsent(dep, new HashSet<>());
                    graph.get(dep).add(job.getId());
                    inDegree.put(job.getId(), inDegree.getOrDefault(job.getId(), 0) + 1);
                }
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                queue.offer(entry.getKey());
            }
        }

        List<String> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            String current = queue.poll();
            result.add(current);

            for (String neighbor : graph.getOrDefault(current, new HashSet<>())) {
                inDegree.put(neighbor, inDegree.get(neighbor) - 1);
                if (inDegree.get(neighbor) == 0) {
                    queue.offer(neighbor);
                }
            }
        }

        return result;
    }
}
