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
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * HTTP request executor plugin
 * Makes HTTP/HTTPS requests (GET, POST, PUT, DELETE, etc.)
 * Features #41: Plugin Architecture, #42: Custom Job Type Support, #50: Custom Executor Support
 */
@Slf4j
public class HttpJobExecutor implements JobExecutor {

    private static final String TYPE = "http";
    private final Map<String, HttpURLConnection> activeConnections = new ConcurrentHashMap<>();

    @Override
    public String getSupportedType() {
        return TYPE;
    }

    @Override
    public ExecutionResult execute(Job job, JobExecution execution) {
        log.info("Executing HTTP job: {} (execution: {})", job.getName(), execution.getId());

        HttpURLConnection connection = null;
        try {
            // Get HTTP configuration
            String urlString = (String) job.getConfiguration().get("url");
            if (urlString == null || urlString.trim().isEmpty()) {
                return ExecutionResult.failure("URL is required",
                    new IllegalArgumentException("Missing 'url' in job configuration"));
            }

            String method = (String) job.getConfiguration().getOrDefault("method", "GET");
            Map<String, String> headers = getHeaders(job);
            String body = (String) job.getConfiguration().get("body");
            Integer connectTimeout = getIntValue(job.getConfiguration().get("connectTimeout"), 5000);
            Integer readTimeout = getIntValue(job.getConfiguration().get("readTimeout"), 30000);
            Boolean followRedirects = getBooleanValue(job.getConfiguration().get("followRedirects"), true);

            // Create connection
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            activeConnections.put(execution.getId(), connection);

            // Configure connection
            connection.setRequestMethod(method.toUpperCase());
            connection.setConnectTimeout(connectTimeout);
            connection.setReadTimeout(readTimeout);
            connection.setInstanceFollowRedirects(followRedirects);

            // Set headers
            if (headers != null && !headers.isEmpty()) {
                headers.forEach(connection::setRequestProperty);
            }

            // Set default headers if not provided
            if (!headers.containsKey("User-Agent")) {
                connection.setRequestProperty("User-Agent", "JobScheduler-HttpExecutor/1.0");
            }

            // Send body for POST/PUT/PATCH
            if (body != null && !body.isEmpty() &&
                    ("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method))) {
                connection.setDoOutput(true);
                if (!headers.containsKey("Content-Type")) {
                    connection.setRequestProperty("Content-Type", "application/json");
                }

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = body.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
            }

            // Get response
            int responseCode = connection.getResponseCode();
            String responseMessage = connection.getResponseMessage();

            // Read response body
            StringBuilder response = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            responseCode >= 400 ? connection.getErrorStream() : connection.getInputStream(),
                            StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
            } catch (Exception e) {
                log.debug("No response body: {}", e.getMessage());
            }

            // Collect response headers
            Map<String, String> responseHeaders = new HashMap<>();
            connection.getHeaderFields().forEach((key, values) -> {
                if (key != null && values != null && !values.isEmpty()) {
                    responseHeaders.put(key, String.join(", ", values));
                }
            });

            // Build metrics
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("responseCode", responseCode);
            metrics.put("responseMessage", responseMessage);
            metrics.put("responseSize", response.length());
            metrics.put("url", urlString);
            metrics.put("method", method);

            // Build result
            Map<String, Object> result = new HashMap<>();
            result.put("statusCode", responseCode);
            result.put("statusMessage", responseMessage);
            result.put("body", response.toString().trim());
            result.put("headers", responseHeaders);

            activeConnections.remove(execution.getId());

            // Check if successful (2xx status codes)
            if (responseCode >= 200 && responseCode < 300) {
                return ExecutionResult.builder()
                        .status(JobStatus.SUCCESS)
                        .result(result)
                        .logs("HTTP " + method + " " + urlString + " -> " + responseCode)
                        .metrics(metrics)
                        .build();
            } else {
                return ExecutionResult.builder()
                        .status(JobStatus.FAILED)
                        .errorMessage("HTTP request failed with status: " + responseCode + " " + responseMessage)
                        .result(result)
                        .logs("HTTP " + method + " " + urlString + " -> " + responseCode + "\n" + response)
                        .metrics(metrics)
                        .build();
            }

        } catch (IOException e) {
            activeConnections.remove(execution.getId());
            return ExecutionResult.failure("HTTP request failed: " + e.getMessage(), e);
        } catch (Exception e) {
            activeConnections.remove(execution.getId());
            return ExecutionResult.failure("Unexpected error during HTTP execution", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @Override
    public void cancel(JobExecution execution) {
        log.info("Cancelling HTTP execution: {}", execution.getId());
        HttpURLConnection connection = activeConnections.get(execution.getId());
        if (connection != null) {
            connection.disconnect();
            activeConnections.remove(execution.getId());
            log.info("HTTP connection cancelled: {}", execution.getId());
        }
    }

    @Override
    public int getPriority() {
        return 90; // High priority for HTTP executor
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> getHeaders(Job job) {
        Object headersObj = job.getConfiguration().get("headers");
        if (headersObj instanceof Map) {
            Map<String, String> result = new HashMap<>();
            ((Map<?, ?>) headersObj).forEach((k, v) ->
                result.put(k.toString(), v != null ? v.toString() : ""));
            return result;
        }
        return new HashMap<>();
    }

    private Integer getIntValue(Object value, int defaultValue) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private Boolean getBooleanValue(Object value, boolean defaultValue) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return defaultValue;
    }
}
