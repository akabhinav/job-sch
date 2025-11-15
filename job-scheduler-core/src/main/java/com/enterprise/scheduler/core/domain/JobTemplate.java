package com.enterprise.scheduler.core.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Job template for creating reusable job blueprints
 * Feature #10: Job Templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTemplate {

    /**
     * Unique template identifier
     */
    private String id;

    /**
     * Template name
     */
    private String name;

    /**
     * Template description
     */
    private String description;

    /**
     * Job type
     */
    private String type;

    /**
     * Default configuration
     */
    @Builder.Default
    private Map<String, Object> defaultConfiguration = new HashMap<>();

    /**
     * Default parameters
     */
    @Builder.Default
    private Map<String, Object> defaultParameters = new HashMap<>();

    /**
     * Template variables that can be replaced
     */
    private Set<String> variables;

    /**
     * Template category
     */
    private String category;

    /**
     * Template tags
     */
    private Set<String> tags;

    /**
     * Template metadata
     */
    @Builder.Default
    private Map<String, String> metadata = new HashMap<>();

    /**
     * Created timestamp
     */
    private Instant createdAt;

    /**
     * Created by user
     */
    private String createdBy;

    /**
     * Version
     */
    @Builder.Default
    private Integer version = 1;

    /**
     * Tenant ID
     */
    private String tenantId;

    /**
     * Create a job from this template
     */
    public Job createJob(String name, Map<String, Object> parameters) {
        Map<String, Object> mergedParams = new HashMap<>(defaultParameters);
        if (parameters != null) {
            mergedParams.putAll(parameters);
        }

        return Job.builder()
                .name(name)
                .type(type)
                .description(description)
                .configuration(new HashMap<>(defaultConfiguration))
                .parameters(mergedParams)
                .tenantId(tenantId)
                .build();
    }
}
