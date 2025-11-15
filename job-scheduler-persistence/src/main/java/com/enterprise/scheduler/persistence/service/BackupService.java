package com.enterprise.scheduler.persistence.service;

import com.enterprise.scheduler.persistence.entity.JobEntity;
import com.enterprise.scheduler.persistence.entity.JobExecutionEntity;
import com.enterprise.scheduler.persistence.repository.JobExecutionJpaRepository;
import com.enterprise.scheduler.persistence.repository.JobJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for backup and restore operations
 * Feature #63: Backup and Restore
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BackupService {

    private final JobJpaRepository jobRepository;
    private final JobExecutionJpaRepository executionRepository;
    private final ObjectMapper objectMapper;

    /**
     * Create a full backup of jobs and executions
     */
    @Transactional(readOnly = true)
    public BackupResult createFullBackup(String backupPath) {
        log.info("Creating full backup to: {}", backupPath);

        try {
            List<JobEntity> jobs = jobRepository.findAll();
            List<JobExecutionEntity> executions = executionRepository.findAll();

            Map<String, Object> backup = new HashMap<>();
            backup.put("timestamp", Instant.now().toString());
            backup.put("version", "1.0");
            backup.put("jobs", jobs);
            backup.put("executions", executions);
            backup.put("jobCount", jobs.size());
            backup.put("executionCount", executions.size());

            File backupFile = new File(backupPath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(backupFile, backup);

            log.info("Backup created successfully: {} jobs, {} executions", jobs.size(), executions.size());

            return BackupResult.builder()
                .success(true)
                .backupPath(backupPath)
                .jobCount(jobs.size())
                .executionCount(executions.size())
                .backupSize(backupFile.length())
                .timestamp(Instant.now())
                .build();

        } catch (IOException e) {
            log.error("Failed to create backup", e);
            return BackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .timestamp(Instant.now())
                .build();
        }
    }

    /**
     * Create a backup of jobs only
     */
    @Transactional(readOnly = true)
    public BackupResult createJobsBackup(String backupPath) {
        log.info("Creating jobs backup to: {}", backupPath);

        try {
            List<JobEntity> jobs = jobRepository.findAll();

            Map<String, Object> backup = new HashMap<>();
            backup.put("timestamp", Instant.now().toString());
            backup.put("version", "1.0");
            backup.put("type", "JOBS_ONLY");
            backup.put("jobs", jobs);
            backup.put("jobCount", jobs.size());

            File backupFile = new File(backupPath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(backupFile, backup);

            log.info("Jobs backup created successfully: {} jobs", jobs.size());

            return BackupResult.builder()
                .success(true)
                .backupPath(backupPath)
                .jobCount(jobs.size())
                .backupSize(backupFile.length())
                .timestamp(Instant.now())
                .build();

        } catch (IOException e) {
            log.error("Failed to create jobs backup", e);
            return BackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .timestamp(Instant.now())
                .build();
        }
    }

    /**
     * Create a backup for a specific tenant
     */
    @Transactional(readOnly = true)
    public BackupResult createTenantBackup(String tenantId, String backupPath) {
        log.info("Creating tenant backup for: {} to: {}", tenantId, backupPath);

        try {
            List<JobEntity> jobs = jobRepository.findByTenantId(tenantId);
            List<JobExecutionEntity> executions = executionRepository.findByTenantId(tenantId);

            Map<String, Object> backup = new HashMap<>();
            backup.put("timestamp", Instant.now().toString());
            backup.put("version", "1.0");
            backup.put("type", "TENANT_BACKUP");
            backup.put("tenantId", tenantId);
            backup.put("jobs", jobs);
            backup.put("executions", executions);
            backup.put("jobCount", jobs.size());
            backup.put("executionCount", executions.size());

            File backupFile = new File(backupPath);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(backupFile, backup);

            log.info("Tenant backup created successfully for {}: {} jobs, {} executions",
                tenantId, jobs.size(), executions.size());

            return BackupResult.builder()
                .success(true)
                .backupPath(backupPath)
                .jobCount(jobs.size())
                .executionCount(executions.size())
                .backupSize(backupFile.length())
                .timestamp(Instant.now())
                .build();

        } catch (IOException e) {
            log.error("Failed to create tenant backup", e);
            return BackupResult.builder()
                .success(false)
                .errorMessage(e.getMessage())
                .timestamp(Instant.now())
                .build();
        }
    }

    /**
     * Generate a backup filename with timestamp
     */
    public String generateBackupFilename(String prefix, String suffix) {
        String timestamp = DateTimeFormatter.ISO_INSTANT
            .format(Instant.now())
            .replaceAll(":", "-");
        return String.format("%s_%s.%s", prefix, timestamp, suffix);
    }

    /**
     * Backup result data class
     */
    @lombok.Data
    @lombok.Builder
    public static class BackupResult {
        private boolean success;
        private String backupPath;
        private int jobCount;
        private int executionCount;
        private long backupSize;
        private Instant timestamp;
        private String errorMessage;
    }
}
