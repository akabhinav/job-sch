package com.enterprise.scheduler.security.audit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Audit Event entity for tracking security and operational events.
 *
 * Feature #55: Audit Logging
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditEvent {
    /**
     * Unique identifier
     */
    private String id;

    /**
     * Event type
     */
    private AuditEventType eventType;

    /**
     * User who triggered the event
     */
    private String userId;

    /**
     * Username for display
     */
    private String username;

    /**
     * Tenant context
     */
    private String tenantId;

    /**
     * Resource type (e.g., "job", "user", "apikey")
     */
    private String resourceType;

    /**
     * Resource ID
     */
    private String resourceId;

    /**
     * Event description
     */
    private String description;

    /**
     * Event timestamp
     */
    private Instant timestamp;

    /**
     * IP address of the client
     */
    private String ipAddress;

    /**
     * User agent string
     */
    private String userAgent;

    /**
     * Event severity
     */
    private AuditSeverity severity;

    /**
     * Whether the action was successful
     */
    private boolean success;

    /**
     * Error message if action failed
     */
    private String errorMessage;

    /**
     * Additional metadata
     */
    private Map<String, Object> metadata;

    /**
     * Request ID for correlation
     */
    private String requestId;

    /**
     * Session ID
     */
    private String sessionId;
}
