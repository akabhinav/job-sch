package com.enterprise.scheduler.security.audit;

import com.enterprise.scheduler.security.context.TenantContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service for audit logging and compliance tracking.
 * Provides comprehensive audit trail for security and operational events.
 *
 * Feature #55: Audit Service
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    /**
     * Log an audit event (synchronous)
     */
    public void logEvent(AuditEventType eventType, String userId, String tenantId,
                        String description, Map<String, Object> metadata) {
        AuditEvent event = createAuditEvent(eventType, userId, tenantId, description, metadata, true, null);
        auditRepository.save(event);
    }

    /**
     * Log an audit event asynchronously
     */
    @Async
    public void logEventAsync(AuditEventType eventType, String userId, String tenantId,
                             String description, Map<String, Object> metadata) {
        AuditEvent event = createAuditEvent(eventType, userId, tenantId, description, metadata, true, null);
        auditRepository.save(event);
    }

    /**
     * Log a failed event
     */
    public void logFailedEvent(AuditEventType eventType, String userId, String tenantId,
                              String description, String errorMessage, Map<String, Object> metadata) {
        AuditEvent event = createAuditEvent(eventType, userId, tenantId, description, metadata, false, errorMessage);
        auditRepository.save(event);
    }

    /**
     * Log a security event
     */
    public void logSecurityEvent(AuditEventType eventType, String userId, String tenantId,
                                String description, AuditSeverity severity, Map<String, Object> metadata) {
        AuditEvent event = createAuditEvent(eventType, userId, tenantId, description, metadata, true, null);
        event.setSeverity(severity);
        auditRepository.save(event);

        if (severity == AuditSeverity.CRITICAL || severity == AuditSeverity.ERROR) {
            log.error("Security event: type={}, userId={}, tenantId={}, description={}",
                    eventType, userId, tenantId, description);
        }
    }

    /**
     * Log authentication event
     */
    public void logAuthenticationEvent(AuditEventType eventType, String username, boolean success,
                                      String errorMessage) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("username", username);

        AuditEvent event = createAuditEvent(
                eventType,
                username,
                null,
                eventType.getDescription(),
                metadata,
                success,
                errorMessage
        );

        event.setSeverity(success ? AuditSeverity.INFO : AuditSeverity.WARNING);
        auditRepository.save(event);
    }

    /**
     * Log authorization event
     */
    public void logAuthorizationEvent(String userId, String resource, String action,
                                     boolean granted, String tenantId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("resource", resource);
        metadata.put("action", action);
        metadata.put("granted", granted);

        AuditEventType eventType = granted ? AuditEventType.ACCESS_GRANTED : AuditEventType.ACCESS_DENIED;
        AuditSeverity severity = granted ? AuditSeverity.INFO : AuditSeverity.WARNING;

        AuditEvent event = createAuditEvent(
                eventType,
                userId,
                tenantId,
                String.format("%s access to %s", granted ? "Granted" : "Denied", resource),
                metadata,
                granted,
                null
        );

        event.setSeverity(severity);
        auditRepository.save(event);
    }

    /**
     * Log resource event (create, update, delete)
     */
    public void logResourceEvent(AuditEventType eventType, String userId, String tenantId,
                                 String resourceType, String resourceId,
                                 String description, Map<String, Object> metadata) {
        AuditEvent event = createAuditEvent(eventType, userId, tenantId, description, metadata, true, null);
        event.setResourceType(resourceType);
        event.setResourceId(resourceId);
        auditRepository.save(event);
    }

    /**
     * Get audit events for a user
     */
    public List<AuditEvent> getAuditEventsForUser(String userId, int limit) {
        return auditRepository.findByUserId(userId, limit);
    }

    /**
     * Get audit events for a tenant
     */
    public List<AuditEvent> getAuditEventsForTenant(String tenantId, int limit) {
        return auditRepository.findByTenantId(tenantId, limit);
    }

    /**
     * Get audit events for a resource
     */
    public List<AuditEvent> getAuditEventsForResource(String resourceType, String resourceId, int limit) {
        return auditRepository.findByResource(resourceType, resourceId, limit);
    }

    /**
     * Get audit events by time range
     */
    public List<AuditEvent> getAuditEventsByTimeRange(Instant startTime, Instant endTime, int limit) {
        return auditRepository.findByTimeRange(startTime, endTime, limit);
    }

    /**
     * Get security events
     */
    public List<AuditEvent> getSecurityEvents(int limit) {
        return auditRepository.findSecurityEvents(limit);
    }

    /**
     * Get failed events
     */
    public List<AuditEvent> getFailedEvents(int limit) {
        return auditRepository.findFailedEvents(limit);
    }

    /**
     * Export audit events (for compliance)
     */
    public void exportAuditEvents(String tenantId, Instant startTime, Instant endTime,
                                  String exportedBy) {
        List<AuditEvent> events = auditRepository.findByTenantIdAndTimeRange(
                tenantId, startTime, endTime, Integer.MAX_VALUE);

        // Log the export action
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("eventCount", events.size());
        metadata.put("startTime", startTime);
        metadata.put("endTime", endTime);

        logEvent(
                AuditEventType.AUDIT_LOG_EXPORTED,
                exportedBy,
                tenantId,
                String.format("Exported %d audit events", events.size()),
                metadata
        );

        log.info("Exported {} audit events for tenant {} by user {}",
                events.size(), tenantId, exportedBy);
    }

    /**
     * Clean up old audit events
     */
    public int cleanupOldAuditEvents(int retentionDays) {
        Instant cutoffTime = Instant.now().minus(retentionDays, ChronoUnit.DAYS);
        int deletedCount = auditRepository.deleteOlderThan(cutoffTime);

        log.info("Cleaned up {} audit events older than {} days", deletedCount, retentionDays);
        return deletedCount;
    }

    /**
     * Create audit event with context information
     */
    private AuditEvent createAuditEvent(AuditEventType eventType, String userId, String tenantId,
                                       String description, Map<String, Object> metadata,
                                       boolean success, String errorMessage) {
        // Get current HTTP request if available
        HttpServletRequest request = getCurrentRequest();

        // Get tenant from context if not provided
        if (tenantId == null) {
            tenantId = TenantContextHolder.getTenantId();
        }

        // Get user from security context if not provided
        if (userId == null) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                userId = auth.getName();
            }
        }

        // Determine severity based on event type
        AuditSeverity severity = determineSeverity(eventType, success);

        return AuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .eventType(eventType)
                .userId(userId)
                .username(userId)
                .tenantId(tenantId)
                .description(description)
                .timestamp(Instant.now())
                .ipAddress(request != null ? getClientIpAddress(request) : null)
                .userAgent(request != null ? request.getHeader("User-Agent") : null)
                .severity(severity)
                .success(success)
                .errorMessage(errorMessage)
                .metadata(metadata)
                .requestId(request != null ? request.getHeader("X-Request-ID") : null)
                .sessionId(request != null ? request.getSession(false) != null ?
                        request.getSession(false).getId() : null : null)
                .build();
    }

    /**
     * Determine severity based on event type and success
     */
    private AuditSeverity determineSeverity(AuditEventType eventType, boolean success) {
        if (!success) {
            return eventType.isCritical() ? AuditSeverity.CRITICAL : AuditSeverity.ERROR;
        }

        if (eventType.isCritical()) {
            return AuditSeverity.WARNING;
        }

        if (eventType.isSecurityEvent()) {
            return AuditSeverity.INFO;
        }

        return AuditSeverity.INFO;
    }

    /**
     * Get current HTTP request from request context
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Handle multiple IPs in X-Forwarded-For
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}
