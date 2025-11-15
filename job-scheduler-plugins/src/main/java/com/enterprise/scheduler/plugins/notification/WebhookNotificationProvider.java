package com.enterprise.scheduler.plugins.notification;

import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.NotificationProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Webhook notification provider plugin
 * Sends generic webhook notifications to configured endpoints
 * Feature #29: Alerting System, #41: Plugin Architecture, #46: Webhook Support
 */
@Slf4j
public class WebhookNotificationProvider implements NotificationProvider {

    private static final String NAME = "webhook";
    private final String webhookUrl;
    private final String authToken;
    private final int timeoutMs;
    private final boolean enabled;

    public WebhookNotificationProvider() {
        this.enabled = Boolean.parseBoolean(System.getProperty("notification.webhook.enabled", "false"));

        if (enabled) {
            this.webhookUrl = System.getProperty("notification.webhook.url");
            this.authToken = System.getProperty("notification.webhook.auth.token");
            this.timeoutMs = Integer.parseInt(System.getProperty("notification.webhook.timeout", "5000"));

            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("Webhook URL not configured, notifications will be skipped");
            } else {
                log.info("Webhook notification provider initialized for URL: {}", webhookUrl);
            }
        } else {
            this.webhookUrl = null;
            this.authToken = null;
            this.timeoutMs = 5000;
            log.info("Webhook notification provider disabled");
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void notifySuccess(JobExecution execution) {
        if (!isConfigured()) return;

        String payload = buildExecutionPayload(execution, "SUCCESS", "Job completed successfully");
        sendWebhook(payload);
    }

    @Override
    public void notifyFailure(JobExecution execution) {
        if (!isConfigured()) return;

        String payload = buildExecutionPayload(execution, "FAILURE",
            execution.getErrorMessage() != null ? execution.getErrorMessage() : "Job failed");
        sendWebhook(payload);
    }

    @Override
    public void notifyTimeout(JobExecution execution) {
        if (!isConfigured()) return;

        String payload = buildExecutionPayload(execution, "TIMEOUT", "Job execution timed out");
        sendWebhook(payload);
    }

    @Override
    public void notify(String title, String message, NotificationLevel level) {
        if (!isConfigured()) return;

        String payload = buildCustomPayload(title, message, level);
        sendWebhook(payload);
    }

    private void sendWebhook(String payload) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(webhookUrl);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("User-Agent", "JobScheduler-WebhookNotifier/1.0");
            connection.setConnectTimeout(timeoutMs);
            connection.setReadTimeout(timeoutMs);
            connection.setDoOutput(true);

            // Add authorization header if token is configured
            if (authToken != null && !authToken.isEmpty()) {
                connection.setRequestProperty("Authorization", "Bearer " + authToken);
            }

            // Send payload
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode >= 200 && responseCode < 300) {
                log.debug("Webhook notification sent successfully, response code: {}", responseCode);
            } else {
                log.error("Failed to send webhook notification, response code: {}", responseCode);
            }

        } catch (Exception e) {
            log.error("Error sending webhook notification to: {}", webhookUrl, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private String buildExecutionPayload(JobExecution execution, String eventType, String message) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"eventType\":\"").append(eventType).append("\",");
        json.append("\"timestamp\":\"").append(Instant.now().toString()).append("\",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\",");
        json.append("\"execution\":{");
        json.append("\"id\":\"").append(execution.getId()).append("\",");
        json.append("\"jobId\":\"").append(execution.getJobId()).append("\",");
        json.append("\"jobName\":\"").append(escapeJson(execution.getJobName())).append("\",");
        json.append("\"status\":\"").append(execution.getStatus()).append("\",");

        if (execution.getStartTime() != null) {
            json.append("\"startTime\":\"").append(execution.getStartTime().toString()).append("\",");
        }
        if (execution.getEndTime() != null) {
            json.append("\"endTime\":\"").append(execution.getEndTime().toString()).append("\",");
        }

        json.append("\"duration\":\"").append(execution.getDuration()).append("\",");

        if (execution.getTriggeredBy() != null) {
            json.append("\"triggeredBy\":\"").append(escapeJson(execution.getTriggeredBy())).append("\",");
        }
        if (execution.getNodeId() != null) {
            json.append("\"nodeId\":\"").append(escapeJson(execution.getNodeId())).append("\",");
        }
        if (execution.getErrorMessage() != null) {
            json.append("\"errorMessage\":\"").append(escapeJson(execution.getErrorMessage())).append("\",");
        }

        json.append("\"attemptNumber\":").append(execution.getAttemptNumber());
        json.append("}");
        json.append("}");

        return json.toString();
    }

    private String buildCustomPayload(String title, String message, NotificationLevel level) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"eventType\":\"CUSTOM_NOTIFICATION\",");
        json.append("\"timestamp\":\"").append(Instant.now().toString()).append("\",");
        json.append("\"title\":\"").append(escapeJson(title)).append("\",");
        json.append("\"message\":\"").append(escapeJson(message)).append("\",");
        json.append("\"level\":\"").append(level.name()).append("\"");
        json.append("}");

        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
            .replace("\b", "\\b")
            .replace("\f", "\\f");
    }

    private boolean isConfigured() {
        if (!enabled) {
            log.debug("Webhook notifications disabled");
            return false;
        }
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Webhook URL not configured");
            return false;
        }
        return true;
    }
}
