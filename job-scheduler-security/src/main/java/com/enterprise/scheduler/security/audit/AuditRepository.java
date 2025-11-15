package com.enterprise.scheduler.security.audit;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Audit Event persistence.
 * Implementation should be provided in the persistence module.
 *
 * Feature #55: Audit Repository
 */
public interface AuditRepository {
    /**
     * Save an audit event
     */
    AuditEvent save(AuditEvent auditEvent);

    /**
     * Find audit event by ID
     */
    Optional<AuditEvent> findById(String id);

    /**
     * Find audit events by user
     */
    List<AuditEvent> findByUserId(String userId, int limit);

    /**
     * Find audit events by tenant
     */
    List<AuditEvent> findByTenantId(String tenantId, int limit);

    /**
     * Find audit events by event type
     */
    List<AuditEvent> findByEventType(AuditEventType eventType, int limit);

    /**
     * Find audit events by resource
     */
    List<AuditEvent> findByResource(String resourceType, String resourceId, int limit);

    /**
     * Find audit events by time range
     */
    List<AuditEvent> findByTimeRange(Instant startTime, Instant endTime, int limit);

    /**
     * Find audit events by user and time range
     */
    List<AuditEvent> findByUserIdAndTimeRange(String userId, Instant startTime, Instant endTime, int limit);

    /**
     * Find audit events by tenant and time range
     */
    List<AuditEvent> findByTenantIdAndTimeRange(String tenantId, Instant startTime, Instant endTime, int limit);

    /**
     * Find audit events by severity
     */
    List<AuditEvent> findBySeverity(AuditSeverity severity, int limit);

    /**
     * Find failed events
     */
    List<AuditEvent> findFailedEvents(int limit);

    /**
     * Find security events
     */
    List<AuditEvent> findSecurityEvents(int limit);

    /**
     * Count audit events by user
     */
    long countByUserId(String userId);

    /**
     * Count audit events by tenant
     */
    long countByTenantId(String tenantId);

    /**
     * Delete audit events older than specified time
     */
    int deleteOlderThan(Instant timestamp);
}
