package com.enterprise.scheduler.persistence.entity;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.JobExecution.TriggerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * JPA entity for JobExecution
 * Features #2, #4, #17, #22, #23
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "job_executions",
    indexes = {
        @Index(name = "idx_execution_job_id", columnList = "job_id"),
        @Index(name = "idx_execution_status", columnList = "status"),
        @Index(name = "idx_execution_start_time", columnList = "start_time"),
        @Index(name = "idx_execution_end_time", columnList = "end_time"),
        @Index(name = "idx_execution_tenant", columnList = "tenant_id"),
        @Index(name = "idx_execution_node", columnList = "node_id"),
        @Index(name = "idx_execution_job_start", columnList = "job_id, start_time")
    }
)
public class JobExecutionEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "job_id", nullable = false, length = 64)
    private String jobId;

    @Column(name = "job_name", length = 255)
    private String jobName;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_type", length = 50)
    private TriggerType triggerType;

    @Column(name = "triggered_by", length = 100)
    private String triggeredBy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result", columnDefinition = "jsonb")
    private Object result;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(name = "attempt_number", nullable = false)
    @Builder.Default
    private Integer attemptNumber = 1;

    @Column(name = "node_id", length = 100)
    private String nodeId;

    @Column(name = "process_id", length = 100)
    private String processId;

    @Column(name = "thread_name", length = 255)
    private String threadName;

    @Column(name = "logs", columnDefinition = "text")
    private String logs;

    @Column(name = "log_reference", length = 500)
    private String logReference;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metrics", columnDefinition = "jsonb")
    private Map<String, Object> metrics;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @PrePersist
    protected void onCreate() {
        if (startTime == null) {
            startTime = Instant.now();
        }
    }
}
