package com.enterprise.scheduler.persistence.repository;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.JobExecution;
import com.enterprise.scheduler.core.spi.JobExecutionRepository;
import com.enterprise.scheduler.persistence.entity.JobExecutionEntity;
import com.enterprise.scheduler.persistence.mapper.JobExecutionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of JobExecutionRepository SPI
 * Feature #22: Job Execution History
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JobExecutionRepositoryImpl implements JobExecutionRepository {

    private final JobExecutionJpaRepository jpaRepository;
    private final JobExecutionMapper mapper;

    @Override
    @Transactional
    public JobExecution save(JobExecution execution) {
        log.debug("Saving job execution: {}", execution.getId());

        JobExecutionEntity entity;
        if (execution.getId() != null && jpaRepository.existsById(execution.getId())) {
            // Update existing execution
            entity = jpaRepository.findById(execution.getId())
                .orElseThrow(() -> new IllegalStateException("Execution not found: " + execution.getId()));
            mapper.updateEntity(execution, entity);
        } else {
            // Create new execution
            entity = mapper.toEntity(execution);
        }

        JobExecutionEntity saved = jpaRepository.save(entity);
        log.debug("Job execution saved successfully: {}", saved.getId());

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobExecution> findById(String id) {
        log.debug("Finding execution by id: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecution> findByJobId(String jobId) {
        log.debug("Finding executions by job id: {}", jobId);
        return jpaRepository.findByJobId(jobId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecution> findByJobId(String jobId, int page, int size) {
        log.debug("Finding executions by job id: {} (page: {}, size: {})", jobId, page, size);
        PageRequest pageRequest = PageRequest.of(page, size);
        return jpaRepository.findByJobIdOrderByStartTimeDesc(jobId, pageRequest).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecution> findByStatus(JobStatus status) {
        log.debug("Finding executions by status: {}", status);
        return jpaRepository.findByStatus(status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecution> findRunning() {
        log.debug("Finding running executions");
        return jpaRepository.findRunning().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobExecution> findByTimeRange(Instant start, Instant end) {
        log.debug("Finding executions in time range: {} to {}", start, end);
        return jpaRepository.findByTimeRange(start, end).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobExecution> findLatestByJobId(String jobId) {
        log.debug("Finding latest execution for job: {}", jobId);
        return jpaRepository.findLatestByJobId(jobId)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional
    public int deleteOlderThan(Instant timestamp) {
        log.debug("Deleting executions older than: {}", timestamp);
        int deleted = jpaRepository.deleteByStartTimeBefore(timestamp);
        log.debug("Deleted {} executions", deleted);
        return deleted;
    }

    @Override
    @Transactional(readOnly = true)
    public long countByJobId(String jobId) {
        return jpaRepository.countByJobId(jobId);
    }
}
