package com.enterprise.scheduler.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import java.util.Map;
import java.util.Set;

/**
 * Job template request
 * Feature #10: Job Templates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Job template creation/update request")
public class TemplateRequest {

    @Schema(description = "Template name", required = true)
    @NotBlank(message = "Template name is required")
    private String name;

    @Schema(description = "Template description")
    private String description;

    @Schema(description = "Job type", required = true)
    @NotBlank(message = "Job type is required")
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
}
