package com.enterprise.scheduler.api.controller;

import com.enterprise.scheduler.api.dto.ApiResponse;
import com.enterprise.scheduler.monitoring.health.ClusterHealthIndicator;
import com.enterprise.scheduler.monitoring.health.DatabaseHealthIndicator;
import com.enterprise.scheduler.monitoring.health.JobSchedulerHealthIndicator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for health checks
 * Feature #26: Health Checks
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/health")
@RequiredArgsConstructor
@Validated
@Tag(name = "Health", description = "System health check operations")
public class HealthController {

    private final JobSchedulerHealthIndicator jobSchedulerHealthIndicator;
    private final DatabaseHealthIndicator databaseHealthIndicator;
    private final ClusterHealthIndicator clusterHealthIndicator;

    @GetMapping
    @Operation(summary = "Overall health check", description = "Returns overall system health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        log.debug("Health check requested");

        Map<String, Object> healthStatus = new HashMap<>();
        healthStatus.put("timestamp", Instant.now());
        healthStatus.put("status", "UP");
        healthStatus.put("components", getAllComponentsHealth());

        return ResponseEntity.ok(ApiResponse.success(healthStatus));
    }

    @GetMapping("/live")
    @Operation(summary = "Liveness probe", description = "Returns liveness status (Kubernetes compatible)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> liveness() {
        Map<String, Object> liveness = new HashMap<>();
        liveness.put("status", "UP");
        liveness.put("timestamp", Instant.now());

        return ResponseEntity.ok(ApiResponse.success(liveness));
    }

    @GetMapping("/ready")
    @Operation(summary = "Readiness probe", description = "Returns readiness status (Kubernetes compatible)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> readiness() {
        Map<String, Object> readiness = new HashMap<>();

        try {
            Health dbHealth = databaseHealthIndicator.health();
            boolean isReady = "UP".equals(dbHealth.getStatus().getCode());

            readiness.put("status", isReady ? "UP" : "DOWN");
            readiness.put("timestamp", Instant.now());
            readiness.put("database", dbHealth.getStatus().getCode());

            return ResponseEntity.ok(ApiResponse.success(readiness));
        } catch (Exception e) {
            log.error("Readiness check failed", e);
            readiness.put("status", "DOWN");
            readiness.put("timestamp", Instant.now());
            readiness.put("error", e.getMessage());

            return ResponseEntity.status(503).body(ApiResponse.success(readiness));
        }
    }

    @GetMapping("/scheduler")
    @Operation(summary = "Scheduler health", description = "Returns job scheduler health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> schedulerHealth() {
        log.debug("Scheduler health check requested");

        Health health = jobSchedulerHealthIndicator.health();
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("details", health.getDetails());
        response.put("timestamp", Instant.now());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/database")
    @Operation(summary = "Database health", description = "Returns database health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> databaseHealth() {
        log.debug("Database health check requested");

        Health health = databaseHealthIndicator.health();
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("details", health.getDetails());
        response.put("timestamp", Instant.now());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/cluster")
    @Operation(summary = "Cluster health", description = "Returns cluster health status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> clusterHealth() {
        log.debug("Cluster health check requested");

        Health health = clusterHealthIndicator.health();
        Map<String, Object> response = new HashMap<>();
        response.put("status", health.getStatus().getCode());
        response.put("details", health.getDetails());
        response.put("timestamp", Instant.now());

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Map<String, Object> getAllComponentsHealth() {
        Map<String, Object> components = new HashMap<>();

        try {
            components.put("scheduler", jobSchedulerHealthIndicator.health().getStatus().getCode());
        } catch (Exception e) {
            components.put("scheduler", "DOWN");
        }

        try {
            components.put("database", databaseHealthIndicator.health().getStatus().getCode());
        } catch (Exception e) {
            components.put("database", "DOWN");
        }

        try {
            components.put("cluster", clusterHealthIndicator.health().getStatus().getCode());
        } catch (Exception e) {
            components.put("cluster", "DOWN");
        }

        return components;
    }
}
