package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Job template response
 * Feature #10: Job Templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job template response")
public class TemplateResponse {

    @Schema(description = "Template ID")
    private String id;

    @Schema(description = "Template name")
    private String name;

    @Schema(description = "Template description")
    private String description;

    @Schema(description = "Job type")
    private String type;

    @Schema(description = "Default configuration")
    private Map<String, Object> defaultConfiguration;

    @Schema(description = "Default parameters")
    private Map<String, Object> defaultParameters;

    @Schema(description = "Template variables")
    private Set<String> variables;

    @Schema(description = "Template category")
    private String category;

    @Schema(description = "Template tags")
    private Set<String> tags;

    @Schema(description = "Template metadata")
    private Map<String, String> metadata;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Template version")
    private Integer version;

    @Schema(description = "Tenant ID")
    private String tenantId;
}
