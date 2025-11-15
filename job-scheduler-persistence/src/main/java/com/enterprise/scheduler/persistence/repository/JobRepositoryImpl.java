package com.enterprise.scheduler.persistence.repository;

import com.enterprise.scheduler.common.model.JobStatus;
import com.enterprise.scheduler.core.domain.Job;
import com.enterprise.scheduler.core.spi.JobRepository;
import com.enterprise.scheduler.persistence.entity.JobEntity;
import com.enterprise.scheduler.persistence.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JPA implementation of JobRepository SPI
 * Features #5: Job Persistence, #48: Multi-Database Support
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class JobRepositoryImpl implements JobRepository {

    private final JobJpaRepository jpaRepository;
    private final JobMapper mapper;

    @Override
    @Transactional
    public Job save(Job job) {
        log.debug("Saving job: {}", job.getId());

        JobEntity entity;
        if (job.getId() != null && jpaRepository.existsById(job.getId())) {
            // Update existing job
            entity = jpaRepository.findById(job.getId())
                .orElseThrow(() -> new IllegalStateException("Job not found: " + job.getId()));
            mapper.updateEntity(job, entity);
        } else {
            // Create new job
            entity = mapper.toEntity(job);
        }

        JobEntity saved = jpaRepository.save(entity);
        log.debug("Job saved successfully: {}", saved.getId());

        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findById(String id) {
        log.debug("Finding job by id: {}", id);
        return jpaRepository.findById(id)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Job> findByNameAndTenant(String name, String tenantId) {
        log.debug("Finding job by name: {} and tenant: {}", name, tenantId);
        return jpaRepository.findByNameAndTenantId(name, tenantId)
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findAll() {
        log.debug("Finding all jobs");
        return jpaRepository.findAll().stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findByTenant(String tenantId) {
        log.debug("Finding jobs by tenant: {}", tenantId);
        return jpaRepository.findByTenantId(tenantId).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findByStatus(JobStatus status) {
        log.debug("Finding jobs by status: {}", status);
        return jpaRepository.findByStatus(status).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Job> findByGroup(String group) {
        log.debug("Finding jobs by group: {}", group);
        return jpaRepository.findByGroup(group).stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteById(String id) {
        log.debug("Deleting job: {}", id);
        jpaRepository.deleteById(id);
        log.debug("Job deleted successfully: {}", id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return jpaRepository.existsById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public long countByTenant(String tenantId) {
        return jpaRepository.countByTenantId(tenantId);
    }
}
