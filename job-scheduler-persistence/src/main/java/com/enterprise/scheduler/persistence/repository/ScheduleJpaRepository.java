package com.enterprise.scheduler.persistence.repository;

import com.enterprise.scheduler.persistence.entity.ScheduleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Spring Data JPA repository for ScheduleEntity
 * Feature #3: Advanced Scheduling
 */
@Repository
public interface ScheduleJpaRepository extends JpaRepository<ScheduleEntity, String> {

    /**
     * Find active schedules
     */
    List<ScheduleEntity> findByActive(Boolean active);

    /**
     * Find schedules ready for execution
     */
    @Query("SELECT s FROM ScheduleEntity s WHERE s.active = true AND s.nextExecutionTime <= :now")
    List<ScheduleEntity> findSchedulesReadyForExecution(@Param("now") Instant now);

    /**
     * Find schedules that have reached max executions
     */
    @Query("SELECT s FROM ScheduleEntity s WHERE s.maxExecutions IS NOT NULL AND s.executionCount >= s.maxExecutions")
    List<ScheduleEntity> findCompletedSchedules();
}
