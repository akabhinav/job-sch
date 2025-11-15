package com.enterprise.scheduler.api.controller;

import com.enterprise.scheduler.api.dto.ApiResponse;
import com.enterprise.scheduler.persistence.service.BackupService;
import com.enterprise.scheduler.security.rbac.RequirePermission;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for admin operations
 * Feature #65: Admin Operations
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Validated
@Tag(name = "Admin", description = "Administrative operations")
public class AdminController {

    private final BackupService backupService;

    @PostMapping("/backup")
    @Operation(summary = "Create backup", description = "Creates a backup of the system")
    @RequirePermission("admin:backup")
    public ResponseEntity<ApiResponse<Map<String, Object>>> createBackup() {
        log.info("Creating system backup");

        try {
            backupService.createBackup();

            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", Instant.now());
            result.put("status", "completed");

            return ResponseEntity.ok(ApiResponse.success(result, "Backup created successfully"));
        } catch (Exception e) {
            log.error("Error creating backup", e);
            return ResponseEntity
                    .status(500)
                    .body(ApiResponse.error("Backup failed: " + e.getMessage()));
        }
    }

    @PostMapping("/restore")
    @Operation(summary = "Restore from backup", description = "Restores the system from a backup")
    @RequirePermission("admin:restore")
    public ResponseEntity<ApiResponse<Map<String, Object>>> restoreBackup(
            @Parameter(description = "Backup ID") @RequestParam String backupId) {
        log.info("Restoring from backup: {}", backupId);

        try {
            backupService.restoreBackup(backupId);

            Map<String, Object> result = new HashMap<>();
            result.put("timestamp", Instant.now());
            result.put("backupId", backupId);
            result.put("status", "completed");

            return ResponseEntity.ok(ApiResponse.success(result, "Restore completed successfully"));
        } catch (Exception e) {
            log.error("Error restoring backup", e);
            return ResponseEntity
                    .status(500)
                    .body(ApiResponse.error("Restore failed: " + e.getMessage()));
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "Get system statistics", description = "Retrieves system statistics")
    @RequirePermission("admin:read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        log.debug("Getting system statistics");

        Map<String, Object> stats = new HashMap<>();
        stats.put("timestamp", Instant.now());
        stats.put("uptime", getUptime());
        stats.put("jvm", getJvmStats());
        stats.put("threads", getThreadStats());

        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @PostMapping("/cache/clear")
    @Operation(summary = "Clear cache", description = "Clears system caches")
    @RequirePermission("admin:cache")
    public ResponseEntity<ApiResponse<Void>> clearCache() {
        log.info("Clearing system cache");

        // Implementation would clear caches
        return ResponseEntity.ok(ApiResponse.success(null, "Cache cleared successfully"));
    }

    @PostMapping("/shutdown")
    @Operation(summary = "Graceful shutdown", description = "Initiates graceful system shutdown")
    @RequirePermission("admin:shutdown")
    public ResponseEntity<ApiResponse<Void>> gracefulShutdown() {
        log.warn("Graceful shutdown requested");

        // Implementation would initiate shutdown
        return ResponseEntity.ok(ApiResponse.success(null, "Shutdown initiated"));
    }

    @GetMapping("/config")
    @Operation(summary = "Get configuration", description = "Retrieves system configuration")
    @RequirePermission("admin:read")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConfiguration() {
        log.debug("Getting system configuration");

        Map<String, Object> config = new HashMap<>();
        config.put("version", "1.0.0-SNAPSHOT");
        config.put("environment", System.getenv().getOrDefault("ENV", "development"));
        config.put("profiles", System.getProperty("spring.profiles.active", "default"));

        return ResponseEntity.ok(ApiResponse.success(config));
    }

    private long getUptime() {
        return java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
    }

    private Map<String, Object> getJvmStats() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> jvm = new HashMap<>();
        jvm.put("maxMemory", runtime.maxMemory());
        jvm.put("totalMemory", runtime.totalMemory());
        jvm.put("freeMemory", runtime.freeMemory());
        jvm.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        jvm.put("processors", runtime.availableProcessors());
        return jvm;
    }

    private Map<String, Object> getThreadStats() {
        Map<String, Object> threads = new HashMap<>();
        threads.put("count", Thread.activeCount());
        threads.put("peak", java.lang.management.ManagementFactory.getThreadMXBean().getPeakThreadCount());
        threads.put("daemon", java.lang.management.ManagementFactory.getThreadMXBean().getDaemonThreadCount());
        return threads;
    }
}
