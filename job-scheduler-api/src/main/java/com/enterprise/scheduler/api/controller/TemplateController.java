package com.enterprise.scheduler.api.controller;

import com.enterprise.scheduler.api.dto.*;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.domain.JobTemplate;
import com.enterprise.scheduler.core.service.JobService;
import com.enterprise.scheduler.security.rbac.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * REST controller for job template management
 * Feature #10: Job Templates
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@Validated
@Tag(name = "Templates", description = "Job template management operations")
public class TemplateController {

    private final JobService jobService;
    private final DtoMapper dtoMapper;

    // In-memory storage for templates (would be replaced with repository in production)
    private final Map<String, JobTemplate> templates = new ConcurrentHashMap<>();

    @PostMapping
    @Operation(summary = "Create template", description = "Creates a new job template")
    @RequirePermission("template:create")
    public ResponseEntity<ApiResponse<TemplateResponse>> createTemplate(
            @Valid @RequestBody TemplateRequest request) {
        log.info("Creating template: {}", request.getName());

        JobTemplate template = dtoMapper.toTemplate(request);
        template.setId(java.util.UUID.randomUUID().toString());
        template.setCreatedAt(java.time.Instant.now());

        templates.put(template.getId(), template);
        TemplateResponse response = dtoMapper.toTemplateResponse(template);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Template created successfully"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get template by ID", description = "Retrieves a template by its ID")
    @RequirePermission("template:read")
    public ResponseEntity<ApiResponse<TemplateResponse>> getTemplate(
            @Parameter(description = "Template ID") @PathVariable String id) {
        log.debug("Getting template: {}", id);

        JobTemplate template = templates.get(id);
        if (template == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Template not found"));
        }

        TemplateResponse response = dtoMapper.toTemplateResponse(template);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping
    @Operation(summary = "Get all templates", description = "Retrieves all job templates")
    @RequirePermission("template:read")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> getAllTemplates(
            @Parameter(description = "Filter by category") @RequestParam(required = false) String category) {
        log.debug("Getting all templates for category: {}", category);

        List<JobTemplate> templateList = new ArrayList<>(templates.values());
        if (category != null) {
            templateList = templateList.stream()
                    .filter(t -> category.equals(t.getCategory()))
                    .collect(Collectors.toList());
        }

        List<TemplateResponse> responses = templateList.stream()
                .map(dtoMapper::toTemplateResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update template", description = "Updates an existing template")
    @RequirePermission("template:update")
    public ResponseEntity<ApiResponse<TemplateResponse>> updateTemplate(
            @Parameter(description = "Template ID") @PathVariable String id,
            @Valid @RequestBody TemplateRequest request) {
        log.info("Updating template: {}", id);

        JobTemplate existing = templates.get(id);
        if (existing == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Template not found"));
        }

        JobTemplate template = dtoMapper.toTemplate(request);
        template.setId(id);
        template.setCreatedAt(existing.getCreatedAt());
        template.setCreatedBy(existing.getCreatedBy());
        template.setVersion(existing.getVersion() + 1);

        templates.put(id, template);
        TemplateResponse response = dtoMapper.toTemplateResponse(template);

        return ResponseEntity.ok(ApiResponse.success(response, "Template updated successfully"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete template", description = "Deletes a template")
    @RequirePermission("template:delete")
    public ResponseEntity<ApiResponse<Void>> deleteTemplate(
            @Parameter(description = "Template ID") @PathVariable String id) {
        log.info("Deleting template: {}", id);

        JobTemplate removed = templates.remove(id);
        if (removed == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Template not found"));
        }

        return ResponseEntity.ok(ApiResponse.success(null, "Template deleted successfully"));
    }

    @PostMapping("/{id}/instantiate")
    @Operation(summary = "Create job from template", description = "Creates a new job from a template")
    @RequirePermission("job:create")
    public ResponseEntity<ApiResponse<JobResponse>> instantiateTemplate(
            @Parameter(description = "Template ID") @PathVariable String id,
            @RequestParam String jobName,
            @RequestBody(required = false) Map<String, Object> parameters) {
        log.info("Creating job from template: {} with name: {}", id, jobName);

        JobTemplate template = templates.get(id);
        if (template == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Template not found"));
        }

        Job job = template.createJob(jobName, parameters);
        Job created = jobService.createJob(job);
        JobResponse response = dtoMapper.toJobResponse(created);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "Job created from template"));
    }
}
