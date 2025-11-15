package com.enterprise.scheduler.plugins.notification;

import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.NotificationProvider;
import lombok.extern.slf4j.Slf4j;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Slack notification provider plugin
 * Sends notifications to Slack via webhook
 * Feature #29: Alerting System, #41: Plugin Architecture
 */
@Slf4j
public class SlackNotificationProvider implements NotificationProvider {

    private static final String NAME = "slack";
    private final String webhookUrl;
    private final String channel;
    private final String username;
    private final boolean enabled;

    public SlackNotificationProvider() {
        this.enabled = Boolean.parseBoolean(System.getProperty("notification.slack.enabled", "false"));

        if (enabled) {
            this.webhookUrl = System.getProperty("notification.slack.webhook.url");
            this.channel = System.getProperty("notification.slack.channel", "#job-alerts");
            this.username = System.getProperty("notification.slack.username", "Job Scheduler");

            if (webhookUrl == null || webhookUrl.isEmpty()) {
                log.warn("Slack webhook URL not configured, notifications will be skipped");
            } else {
                log.info("Slack notification provider initialized for channel: {}", channel);
            }
        } else {
            this.webhookUrl = null;
            this.channel = null;
            this.username = null;
            log.info("Slack notification provider disabled");
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void notifySuccess(JobExecution execution) {
        if (!isConfigured()) return;

        String message = String.format(
            ":white_check_mark: *Job Succeeded*\n" +
            "Job: `%s`\n" +
            "Execution ID: `%s`\n" +
            "Duration: %s\n" +
            "Triggered by: %s",
            execution.getJobName(),
            execution.getId(),
            execution.getDuration(),
            execution.getTriggeredBy()
        );

        sendSlackMessage(message, "good");
    }

    @Override
    public void notifyFailure(JobExecution execution) {
        if (!isConfigured()) return;

        String message = String.format(
            ":x: *Job Failed*\n" +
            "Job: `%s`\n" +
            "Execution ID: `%s`\n" +
            "Duration: %s\n" +
            "Triggered by: %s\n" +
            "Error: %s",
            execution.getJobName(),
            execution.getId(),
            execution.getDuration(),
            execution.getTriggeredBy(),
            execution.getErrorMessage() != null ? execution.getErrorMessage() : "Unknown error"
        );

        sendSlackMessage(message, "danger");
    }

    @Override
    public void notifyTimeout(JobExecution execution) {
        if (!isConfigured()) return;

        String message = String.format(
            ":warning: *Job Timed Out*\n" +
            "Job: `%s`\n" +
            "Execution ID: `%s`\n" +
            "Duration: %s\n" +
            "Triggered by: %s",
            execution.getJobName(),
            execution.getId(),
            execution.getDuration(),
            execution.getTriggeredBy()
        );

        sendSlackMessage(message, "warning");
    }

    @Override
    public void notify(String title, String message, NotificationLevel level) {
        if (!isConfigured()) return;

        String icon = getIconForLevel(level);
        String color = getColorForLevel(level);
        String formattedMessage = String.format("%s *%s*\n%s", icon, title, message);

        sendSlackMessage(formattedMessage, color);
    }

    private void sendSlackMessage(String text, String color) {
        try {
            // Build Slack message payload
            String payload = buildSlackPayload(text, color);

            // Send HTTP POST to webhook
            URL url = new URL(webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                log.debug("Slack notification sent successfully");
            } else {
                log.error("Failed to send Slack notification, response code: {}", responseCode);
            }

            connection.disconnect();

        } catch (Exception e) {
            log.error("Error sending Slack notification", e);
        }
    }

    private String buildSlackPayload(String text, String color) {
        // Escape JSON special characters
        text = escapeJson(text);

        return String.format(
            "{\"channel\":\"%s\",\"username\":\"%s\",\"attachments\":[{\"color\":\"%s\",\"text\":\"%s\",\"mrkdwn_in\":[\"text\"]}]}",
            channel,
            username,
            color,
            text
        );
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }

    private String getIconForLevel(NotificationLevel level) {
        switch (level) {
            case INFO:
                return ":information_source:";
            case WARNING:
                return ":warning:";
            case ERROR:
                return ":x:";
            case CRITICAL:
                return ":rotating_light:";
            default:
                return ":bell:";
        }
    }

    private String getColorForLevel(NotificationLevel level) {
        switch (level) {
            case INFO:
                return "good";
            case WARNING:
                return "warning";
            case ERROR:
            case CRITICAL:
                return "danger";
            default:
                return "#808080";
        }
    }

    private boolean isConfigured() {
        if (!enabled) {
            log.debug("Slack notifications disabled");
            return false;
        }
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Slack webhook URL not configured");
            return false;
        }
        return true;
    }
}
