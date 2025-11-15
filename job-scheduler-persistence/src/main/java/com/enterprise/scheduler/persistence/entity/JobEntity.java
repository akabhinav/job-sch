package com.enterprise.scheduler.persistence.entity;

import com.enterprise.scheduler.common.model.JobPriority;
import com.enterprise.scheduler.common.model.JobStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JPA entity for Job
 * Features #1, #4, #5, #6, #7, #8, #9
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "jobs",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_job_name_tenant", columnNames = {"name", "tenant_id"})
    },
    indexes = {
        @Index(name = "idx_job_status", columnList = "status"),
        @Index(name = "idx_job_tenant", columnList = "tenant_id"),
        @Index(name = "idx_job_group", columnList = "job_group"),
        @Index(name = "idx_job_type", columnList = "type"),
        @Index(name = "idx_job_enabled", columnList = "enabled"),
        @Index(name = "idx_job_created_at", columnList = "created_at"),
        @Index(name = "idx_job_priority", columnList = "priority")
    }
)
public class JobEntity {

    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "type", nullable = false, length = 100)
    private String type;

    @Column(name = "job_group", length = 255)
    private String group;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private JobStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 50)
    @Builder.Default
    private JobPriority priority = JobPriority.NORMAL;

    @Column(name = "version", nullable = false)
    @Version
    private Integer version;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "configuration", columnDefinition = "jsonb")
    private Map<String, Object> configuration;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters", columnDefinition = "jsonb")
    private Map<String, Object> parameters;

    @ElementCollection
    @CollectionTable(
        name = "job_dependencies",
        joinColumns = @JoinColumn(name = "job_id"),
        indexes = @Index(name = "idx_job_dependency", columnList = "job_id, dependency_id")
    )
    @Column(name = "dependency_id", length = 64)
    @Builder.Default
    private Set<String> dependencies = new HashSet<>();

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "schedule_id")
    private ScheduleEntity schedule;

    @Embedded
    private RetryPolicyEntity retryPolicy;

    @Column(name = "timeout_ms")
    private Long timeoutMs;

    @Column(name = "max_concurrent_executions")
    @Builder.Default
    private Integer maxConcurrentExecutions = 1;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @ElementCollection
    @CollectionTable(
        name = "job_tags",
        joinColumns = @JoinColumn(name = "job_id"),
        indexes = @Index(name = "idx_job_tag", columnList = "tag")
    )
    @Column(name = "tag", length = 100)
    @Builder.Default
    private Set<String> tags = new HashSet<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, String> metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (version == null) {
            version = 1;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
