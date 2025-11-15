package com.enterprise.scheduler.plugins.executor;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.ExecutionResult;
import com.enterprise.scheduler.core.spi.JobExecutor;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Script executor plugin
 * Executes scripts in various languages (Python, JavaScript, Ruby, etc.)
 * Features #41: Plugin Architecture, #42: Custom Job Type Support, #50: Custom Executor Support
 */
@Slf4j
public class ScriptJobExecutor implements JobExecutor {

    private static final String TYPE = "script";
    private final Map<String, Process> runningProcesses = new ConcurrentHashMap<>();

    // Supported script types and their interpreters
    private static final Map<String, String> INTERPRETERS = new HashMap<>();

    static {
        INTERPRETERS.put("python", "python3");
        INTERPRETERS.put("python2", "python2");
        INTERPRETERS.put("python3", "python3");
        INTERPRETERS.put("node", "node");
        INTERPRETERS.put("javascript", "node");
        INTERPRETERS.put("js", "node");
        INTERPRETERS.put("ruby", "ruby");
        INTERPRETERS.put("perl", "perl");
        INTERPRETERS.put("php", "php");
        INTERPRETERS.put("groovy", "groovy");
        INTERPRETERS.put("bash", "bash");
        INTERPRETERS.put("sh", "sh");
    }

    @Override
    public String getSupportedType() {
        return TYPE;
    }

    @Override
    public ExecutionResult execute(Job job, JobExecution execution) {
        log.info("Executing script job: {} (execution: {})", job.getName(), execution.getId());

        Path tempScriptFile = null;
        try {
            // Get script configuration
            String scriptType = (String) job.getConfiguration().get("scriptType");
            String script = (String) job.getConfiguration().get("script");
            String scriptFile = (String) job.getConfiguration().get("scriptFile");

            if (scriptType == null || scriptType.trim().isEmpty()) {
                return ExecutionResult.failure("Script type is required",
                    new IllegalArgumentException("Missing 'scriptType' in job configuration"));
            }

            if ((script == null || script.trim().isEmpty()) &&
                (scriptFile == null || scriptFile.trim().isEmpty())) {
                return ExecutionResult.failure("Either 'script' or 'scriptFile' is required",
                    new IllegalArgumentException("Missing script content or file path"));
            }

            // Get interpreter
            String interpreter = INTERPRETERS.get(scriptType.toLowerCase());
            if (interpreter == null) {
                interpreter = (String) job.getConfiguration().getOrDefault("interpreter", scriptType);
            }

            // Prepare script file
            Path scriptPath;
            if (scriptFile != null && !scriptFile.trim().isEmpty()) {
                scriptPath = new File(scriptFile).toPath();
                if (!Files.exists(scriptPath)) {
                    return ExecutionResult.failure("Script file not found: " + scriptFile,
                        new IllegalArgumentException("Script file does not exist"));
                }
            } else {
                // Create temporary script file
                String extension = getScriptExtension(scriptType);
                tempScriptFile = Files.createTempFile("job-script-", extension);
                Files.write(tempScriptFile, script.getBytes(StandardCharsets.UTF_8));
                scriptPath = tempScriptFile;
            }

            // Get script arguments
            List<String> scriptArgs = getScriptArguments(job);

            // Get working directory
            String workingDir = (String) job.getConfiguration().get("workingDirectory");

            // Get environment variables
            Map<String, String> envVars = getEnvironmentVariables(job);

            // Build command
            List<String> command = new ArrayList<>();
            command.add(interpreter);
            command.add(scriptPath.toAbsolutePath().toString());
            command.addAll(scriptArgs);

            // Build process
            ProcessBuilder processBuilder = new ProcessBuilder(command);

            // Set working directory if specified
            if (workingDir != null && !workingDir.isEmpty()) {
                processBuilder.directory(new File(workingDir));
            }

            // Set environment variables
            if (envVars != null && !envVars.isEmpty()) {
                processBuilder.environment().putAll(envVars);
            }

            // Add job parameters as environment variables
            execution.getParameters().forEach((key, value) -> {
                String envKey = "JOB_PARAM_" + key.toUpperCase().replace(".", "_");
                processBuilder.environment().put(envKey, String.valueOf(value));
            });

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
                    log.debug("Script output: {}", line);
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
            metrics.put("scriptType", scriptType);
            metrics.put("interpreter", interpreter);
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
                        .errorMessage("Script failed with exit code: " + exitCode)
                        .logs(logs)
                        .metrics(metrics)
                        .build();
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            runningProcesses.remove(execution.getId());
            return ExecutionResult.failure("Script execution interrupted", e);
        } catch (IOException e) {
            runningProcesses.remove(execution.getId());
            return ExecutionResult.failure("Failed to execute script", e);
        } catch (Exception e) {
            runningProcesses.remove(execution.getId());
            return ExecutionResult.failure("Unexpected error during script execution", e);
        } finally {
            // Clean up temporary script file
            if (tempScriptFile != null) {
                try {
                    Files.deleteIfExists(tempScriptFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary script file: {}", tempScriptFile, e);
                }
            }
        }
    }

    @Override
    public void cancel(JobExecution execution) {
        log.info("Cancelling script execution: {}", execution.getId());
        Process process = runningProcesses.get(execution.getId());
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
            runningProcesses.remove(execution.getId());
            log.info("Script process cancelled: {}", execution.getId());
        }
    }

    @Override
    public int getPriority() {
        return 70; // Medium priority for script executor
    }

    @SuppressWarnings("unchecked")
    private List<String> getScriptArguments(Job job) {
        Object argsObj = job.getConfiguration().get("arguments");
        List<String> args = new ArrayList<>();

        if (argsObj instanceof List) {
            ((List<?>) argsObj).forEach(arg -> args.add(String.valueOf(arg)));
        } else if (argsObj instanceof String) {
            args.add((String) argsObj);
        }

        return args;
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

    private String getScriptExtension(String scriptType) {
        switch (scriptType.toLowerCase()) {
            case "python":
            case "python2":
            case "python3":
                return ".py";
            case "node":
            case "javascript":
            case "js":
                return ".js";
            case "ruby":
                return ".rb";
            case "perl":
                return ".pl";
            case "php":
                return ".php";
            case "groovy":
                return ".groovy";
            case "bash":
            case "sh":
                return ".sh";
            default:
                return ".txt";
        }
    }
}
