package com.enterprise.scheduler.plugins.notification;

import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.NotificationProvider;
import lombok.extern.slf4j.Slf4j;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

/**
 * Email notification provider plugin
 * Sends email notifications via SMTP
 * Feature #29: Alerting System, #41: Plugin Architecture
 */
@Slf4j
public class EmailNotificationProvider implements NotificationProvider {

    private static final String NAME = "email";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final Properties mailProperties;
    private final String fromAddress;
    private final String[] defaultRecipients;
    private final boolean enabled;

    public EmailNotificationProvider() {
        this.mailProperties = new Properties();
        this.enabled = Boolean.parseBoolean(System.getProperty("notification.email.enabled", "false"));

        if (enabled) {
            // SMTP configuration from system properties
            String smtpHost = System.getProperty("notification.email.smtp.host", "localhost");
            String smtpPort = System.getProperty("notification.email.smtp.port", "25");
            String smtpAuth = System.getProperty("notification.email.smtp.auth", "false");
            String smtpStartTls = System.getProperty("notification.email.smtp.starttls", "false");
            String smtpSsl = System.getProperty("notification.email.smtp.ssl", "false");

            mailProperties.put("mail.smtp.host", smtpHost);
            mailProperties.put("mail.smtp.port", smtpPort);
            mailProperties.put("mail.smtp.auth", smtpAuth);
            mailProperties.put("mail.smtp.starttls.enable", smtpStartTls);
            mailProperties.put("mail.smtp.ssl.enable", smtpSsl);

            this.fromAddress = System.getProperty("notification.email.from", "job-scheduler@example.com");
            String recipients = System.getProperty("notification.email.recipients", "");
            this.defaultRecipients = recipients.isEmpty() ? new String[0] : recipients.split(",");

            log.info("Email notification provider initialized with SMTP host: {}", smtpHost);
        } else {
            this.fromAddress = null;
            this.defaultRecipients = new String[0];
            log.info("Email notification provider disabled");
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void notifySuccess(JobExecution execution) {
        if (!enabled) {
            log.debug("Email notifications disabled, skipping success notification for: {}", execution.getId());
            return;
        }

        String subject = String.format("[Job Scheduler] Job Succeeded: %s", execution.getJobName());
        String body = buildSuccessMessage(execution);
        sendEmail(subject, body, NotificationLevel.INFO);
    }

    @Override
    public void notifyFailure(JobExecution execution) {
        if (!enabled) {
            log.debug("Email notifications disabled, skipping failure notification for: {}", execution.getId());
            return;
        }

        String subject = String.format("[Job Scheduler] Job Failed: %s", execution.getJobName());
        String body = buildFailureMessage(execution);
        sendEmail(subject, body, NotificationLevel.ERROR);
    }

    @Override
    public void notifyTimeout(JobExecution execution) {
        if (!enabled) {
            log.debug("Email notifications disabled, skipping timeout notification for: {}", execution.getId());
            return;
        }

        String subject = String.format("[Job Scheduler] Job Timed Out: %s", execution.getJobName());
        String body = buildTimeoutMessage(execution);
        sendEmail(subject, body, NotificationLevel.WARNING);
    }

    @Override
    public void notify(String title, String message, NotificationLevel level) {
        if (!enabled) {
            log.debug("Email notifications disabled, skipping custom notification: {}", title);
            return;
        }

        String subject = String.format("[Job Scheduler] %s: %s", level.name(), title);
        sendEmail(subject, message, level);
    }

    private void sendEmail(String subject, String body, NotificationLevel level) {
        if (defaultRecipients.length == 0) {
            log.warn("No email recipients configured, skipping email notification");
            return;
        }

        try {
            // Create session
            Session session = Session.getInstance(mailProperties, getAuthenticator());

            // Create message
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));

            // Set recipients
            InternetAddress[] recipients = new InternetAddress[defaultRecipients.length];
            for (int i = 0; i < defaultRecipients.length; i++) {
                recipients[i] = new InternetAddress(defaultRecipients[i].trim());
            }
            message.setRecipients(Message.RecipientType.TO, recipients);

            // Set subject and body
            message.setSubject(subject);
            message.setText(body);

            // Set priority based on level
            if (level == NotificationLevel.CRITICAL || level == NotificationLevel.ERROR) {
                message.setHeader("X-Priority", "1");
                message.setHeader("Importance", "High");
            }

            // Send message
            Transport.send(message);

            log.info("Email notification sent: {} (level: {})", subject, level);

        } catch (MessagingException e) {
            log.error("Failed to send email notification: {}", subject, e);
        }
    }

    private Authenticator getAuthenticator() {
        String username = System.getProperty("notification.email.smtp.username");
        String password = System.getProperty("notification.email.smtp.password");

        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            return new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };
        }
        return null;
    }

    private String buildSuccessMessage(JobExecution execution) {
        return String.format(
            "Job Execution Completed Successfully\n\n" +
            "Job Name: %s\n" +
            "Job ID: %s\n" +
            "Execution ID: %s\n" +
            "Status: %s\n" +
            "Start Time: %s\n" +
            "End Time: %s\n" +
            "Duration: %s\n" +
            "Triggered By: %s\n" +
            "Node: %s\n",
            execution.getJobName(),
            execution.getJobId(),
            execution.getId(),
            execution.getStatus(),
            execution.getStartTime() != null ? execution.getStartTime().toString() : "N/A",
            execution.getEndTime() != null ? execution.getEndTime().toString() : "N/A",
            execution.getDuration(),
            execution.getTriggeredBy(),
            execution.getNodeId() != null ? execution.getNodeId() : "N/A"
        );
    }

    private String buildFailureMessage(JobExecution execution) {
        return String.format(
            "Job Execution Failed\n\n" +
            "Job Name: %s\n" +
            "Job ID: %s\n" +
            "Execution ID: %s\n" +
            "Status: %s\n" +
            "Start Time: %s\n" +
            "End Time: %s\n" +
            "Duration: %s\n" +
            "Triggered By: %s\n" +
            "Node: %s\n" +
            "Error: %s\n\n" +
            "Stack Trace:\n%s\n",
            execution.getJobName(),
            execution.getJobId(),
            execution.getId(),
            execution.getStatus(),
            execution.getStartTime() != null ? execution.getStartTime().toString() : "N/A",
            execution.getEndTime() != null ? execution.getEndTime().toString() : "N/A",
            execution.getDuration(),
            execution.getTriggeredBy(),
            execution.getNodeId() != null ? execution.getNodeId() : "N/A",
            execution.getErrorMessage() != null ? execution.getErrorMessage() : "Unknown error",
            execution.getStackTrace() != null ? execution.getStackTrace() : "N/A"
        );
    }

    private String buildTimeoutMessage(JobExecution execution) {
        return String.format(
            "Job Execution Timed Out\n\n" +
            "Job Name: %s\n" +
            "Job ID: %s\n" +
            "Execution ID: %s\n" +
            "Status: %s\n" +
            "Start Time: %s\n" +
            "Duration: %s\n" +
            "Triggered By: %s\n" +
            "Node: %s\n",
            execution.getJobName(),
            execution.getJobId(),
            execution.getId(),
            execution.getStatus(),
            execution.getStartTime() != null ? execution.getStartTime().toString() : "N/A",
            execution.getDuration(),
            execution.getTriggeredBy(),
            execution.getNodeId() != null ? execution.getNodeId() : "N/A"
        );
    }
}
