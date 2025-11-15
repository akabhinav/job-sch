package com.enterprise.scheduler.plugins.executor;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.ExecutionResult;
import com.enterprise.scheduler.core.spi.JobExecutor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Shell command executor plugin
 * Executes shell commands and scripts
 * Features #41: Plugin Architecture, #42: Custom Job Type Support, #50: Custom Executor Support
 */
@Slf4j
public class ShellJobExecutor implements JobExecutor {

    private static final String TYPE = "shell";
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    @Override
    public String getSupportedType() {
        return TYPE;
    }

    @Override
    public ExecutionResult execute(Job job, JobExecution execution) {
        log.info("Executing shell job: {} (execution: {})", job.getName(), execution.getId());

        try {
            // Get shell command from configuration
            String command = getCommand(job);
            if (command == null || command.trim().isEmpty()) {
                return ExecutionResult.failure("Shell command is required",
                    new IllegalArgumentException("Missing 'command' in job configuration"));
            }

            // Get working directory
            String workingDir = (String) job.getConfiguration().get("workingDirectory");

            // Get environment variables
            Map<String, String> envVars = getEnvironmentVariables(job);

            // Build process
            ProcessBuilder processBuilder = new ProcessBuilder();

            // Determine shell based on OS
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                processBuilder.command("cmd.exe", "/c", command);
            } else {
                processBuilder.command("sh", "-c", command);
            }

            // Set working directory if specified
            if (workingDir != null && !workingDir.isEmpty()) {
                processBuilder.directory(new java.io.File(workingDir));
            }

            // Set environment variables
            if (envVars != null && !envVars.isEmpty()) {
                processBuilder.environment().putAll(envVars);
            }

            // Redirect error stream
            processBuilder.redirectErrorStream(true);

            // Start process
            Process process = processBuilder.start();
            runningProcesses.put(execution.getId(), process);

            // Capture output
            StringBuilder output = new StringBuilder();
            List<String> outputLines = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                    outputLines.add(line);
                    log.debug("Shell output: {}", line);
                }
            }

            // Wait for process to complete with timeout
            boolean completed;
            Long timeoutMs = job.getTimeoutMs();
            if (timeoutMs != null && timeoutMs > 0) {
                completed = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
            } else {
                process.waitFor();
                completed = true;
            }

            runningProcesses.remove(execution.getId());

            if (!completed) {
                process.destroyForcibly();
                return ExecutionResult.timeout();
            }

            int exitCode = process.exitValue();
            String logs = output.toString();

            // Build metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("exitCode", exitCode);
            metrics.put("outputLines", outputLines.size());

            if (exitCode == 0) {
                return ExecutionResult.builder()
                        .status(JobStatus.SUCCESS)
                        .result(exitCode)
                        .logs(logs)
                        .metrics(metrics)
                        .build();
            } else {
                return ExecutionResult.builder()
                        .status(JobStatus.FAILED)
                        .errorMessage("Shell command failed with exit code: " + exitCode)
                        .logs(logs)
                        .metrics(metrics)
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            runningProcesses.remove(execution.getId());
            return ExecutionResult.failure("Shell execution interrupted", e);
        } catch (IOException e) {
            runningProcesses.remove(execution.getId());
            return ExecutionResult.failure("Failed to execute shell command", e);
        } catch (Exception e) {
            runningProcesses.remove(execution.getId());
            return ExecutionResult.failure("Unexpected error during shell execution", e);
        }
    }

    @Override
    public void cancel(JobExecution execution) {
        log.info("Cancelling shell execution: {}", execution.getId());
        Process process = runningProcesses.get(execution.getId());
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningProcesses.remove(execution.getId());
            log.info("Shell process cancelled: {}", execution.getId());
        }
    }

    @Override
    public int getPriority() {
        return 100; // High priority for shell executor
    }

    private String getCommand(Job job) {
        Object cmdObj = job.getConfiguration().get("command");
        return cmdObj != null ? cmdObj.toString() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getEnvironmentVariables(Job job) {
        Object envObj = job.getConfiguration().get("environment");
        if (envObj instanceof Map) {
            Map<String, String> result = new HashMap<>();
            ((Map<?, ?>) envObj).forEach((k, v) ->
                result.put(k.toString(), v != null ? v.toString() : ""));
            return result;
        }
        return new HashMap<>();
    }
}
