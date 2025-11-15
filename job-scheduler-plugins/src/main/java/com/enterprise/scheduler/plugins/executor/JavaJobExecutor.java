package com.enterprise.scheduler.plugins.executor;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.ExecutionResult;
import com.enterprise.scheduler.core.spi.JobExecutor;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Java code executor plugin
 * Executes Java classes/methods dynamically
 * Features #41: Plugin Architecture, #42: Custom Job Type Support, #50: Custom Executor Support
 */
@Slf4j
public class JavaJobExecutor implements JobExecutor {

    private static final String TYPE = "java";
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, Future<?>> runningTasks = new ConcurrentHashMap<>();

    @Override
    public String getSupportedType() {
        return TYPE;
    }

    @Override
    public ExecutionResult execute(Job job, JobExecution execution) {
        log.info("Executing Java job: {} (execution: {})", job.getName(), execution.getId());

        try {
            // Get Java class and method configuration
            String className = (String) job.getConfiguration().get("class");
            String methodName = (String) job.getConfiguration().getOrDefault("method", "execute");

            if (className == null || className.trim().isEmpty()) {
                return ExecutionResult.failure("Java class name is required",
                    new IllegalArgumentException("Missing 'class' in job configuration"));
            }

            // Get method arguments
            Object[] args = getMethodArguments(job, execution);

            // Load class
            Class<?> clazz = Class.forName(className);

            // Create instance or use static method
            Object instance = null;
            Method method = null;

            // Try to find method with JobExecution parameter
            try {
                method = clazz.getMethod(methodName, JobExecution.class);
                if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                    instance = clazz.getDeclaredConstructor().newInstance();
                }
                args = new Object[]{execution};
            } catch (NoSuchMethodException e) {
                // Try to find method with Map parameter (for parameters)
                try {
                    method = clazz.getMethod(methodName, Map.class);
                    if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                        instance = clazz.getDeclaredConstructor().newInstance();
                    }
                    args = new Object[]{execution.getParameters()};
                } catch (NoSuchMethodException ex) {
                    // Try to find no-arg method
                    try {
                        method = clazz.getMethod(methodName);
                        if (!java.lang.reflect.Modifier.isStatic(method.getModifiers())) {
                            instance = clazz.getDeclaredConstructor().newInstance();
                        }
                        args = new Object[0];
                    } catch (NoSuchMethodException exc) {
                        throw new NoSuchMethodException("Could not find suitable method: " + methodName);
                    }
                }
            }

            final Object targetInstance = instance;
            final Method targetMethod = method;
            final Object[] methodArgs = args;

            // Execute in executor service with timeout handling
            Callable<Object> task = () -> targetMethod.invoke(targetInstance, methodArgs);

            Future<Object> future = executorService.submit(task);
            runningTasks.put(execution.getId(), future);

            Object result;
            Long timeoutMs = job.getTimeoutMs();

            if (timeoutMs != null && timeoutMs > 0) {
                result = future.get(timeoutMs, TimeUnit.MILLISECONDS);
            } else {
                result = future.get();
            }

            runningTasks.remove(execution.getId());

            // Build metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("className", className);
            metrics.put("methodName", methodName);
            metrics.put("resultType", result != null ? result.getClass().getSimpleName() : "void");

            return ExecutionResult.builder()
                    .status(JobStatus.SUCCESS)
                    .result(result)
                    .logs("Successfully executed " + className + "." + methodName)
                    .metrics(metrics)
                    .build();

        } catch (java.util.concurrent.TimeoutException e) {
            runningTasks.remove(execution.getId());
            return ExecutionResult.timeout();
        } catch (java.util.concurrent.ExecutionException e) {
            runningTasks.remove(execution.getId());
            Throwable cause = e.getCause();
            return ExecutionResult.failure(
                "Java method execution failed: " + cause.getMessage(),
                cause
            );
        } catch (ClassNotFoundException e) {
            runningTasks.remove(execution.getId());
            return ExecutionResult.failure("Java class not found: " + e.getMessage(), e);
        } catch (NoSuchMethodException e) {
            runningTasks.remove(execution.getId());
            return ExecutionResult.failure("Java method not found: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            runningTasks.remove(execution.getId());
            return ExecutionResult.failure("Java execution interrupted", e);
        } catch (Exception e) {
            runningTasks.remove(execution.getId());
            return ExecutionResult.failure("Unexpected error during Java execution", e);
        }
    }

    @Override
    public void cancel(JobExecution execution) {
        log.info("Cancelling Java execution: {}", execution.getId());
        Future<?> future = runningTasks.get(execution.getId());
        if (future != null && !future.isDone()) {
            future.cancel(true);
            runningTasks.remove(execution.getId());
            log.info("Java task cancelled: {}", execution.getId());
        }
    }

    @Override
    public int getPriority() {
        return 80; // Medium-high priority for Java executor
    }

    @SuppressWarnings("unchecked")
    private Object[] getMethodArguments(Job job, JobExecution execution) {
        Object argsObj = job.getConfiguration().get("arguments");
        if (argsObj instanceof Object[]) {
            return (Object[]) argsObj;
        } else if (argsObj instanceof java.util.List) {
            return ((java.util.List<?>) argsObj).toArray();
        }
        return new Object[0];
    }

    /**
     * Shutdown executor service
     */
    public void shutdown() {
        log.info("Shutting down Java executor service");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
