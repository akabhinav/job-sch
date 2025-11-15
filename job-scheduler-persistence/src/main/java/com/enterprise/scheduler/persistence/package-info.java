/**
 * Job Scheduler Persistence Layer
 *
 * <p>This package provides the data persistence implementation for the Job Scheduler platform.
 * It implements the SPI (Service Provider Interface) contracts defined in the core module.
 *
 * <h2>Features Supported</h2>
 * <ul>
 *   <li>Feature #5: Job Persistence</li>
 *   <li>Feature #48: Multi-Database Support (PostgreSQL, H2)</li>
 *   <li>Feature #22: Job Execution History</li>
 *   <li>Feature #63: Backup and Restore</li>
 * </ul>
 *
 * <h2>Architecture</h2>
 * <p>The persistence layer follows a clean architecture with clear separation of concerns:
 *
 * <h3>Entities ({@link com.enterprise.scheduler.persistence.entity})</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.persistence.entity.JobEntity} - JPA entity for jobs</li>
 *   <li>{@link com.enterprise.scheduler.persistence.entity.JobExecutionEntity} - JPA entity for job executions</li>
 *   <li>{@link com.enterprise.scheduler.persistence.entity.ScheduleEntity} - JPA entity for schedules</li>
 *   <li>{@link com.enterprise.scheduler.persistence.entity.RetryPolicyEntity} - Embeddable retry policy</li>
 * </ul>
 *
 * <h3>Repositories ({@link com.enterprise.scheduler.persistence.repository})</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.persistence.repository.JobJpaRepository} - Spring Data JPA repository for jobs</li>
 *   <li>{@link com.enterprise.scheduler.persistence.repository.JobExecutionJpaRepository} - Spring Data JPA repository for executions</li>
 *   <li>{@link com.enterprise.scheduler.persistence.repository.JobRepositoryImpl} - SPI implementation</li>
 *   <li>{@link com.enterprise.scheduler.persistence.repository.JobExecutionRepositoryImpl} - SPI implementation</li>
 * </ul>
 *
 * <h3>Mappers ({@link com.enterprise.scheduler.persistence.mapper})</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.persistence.mapper.JobMapper} - Job entity/domain mapper</li>
 *   <li>{@link com.enterprise.scheduler.persistence.mapper.JobExecutionMapper} - Execution entity/domain mapper</li>
 *   <li>{@link com.enterprise.scheduler.persistence.mapper.ScheduleMapper} - Schedule entity/domain mapper</li>
 *   <li>{@link com.enterprise.scheduler.persistence.mapper.RetryPolicyMapper} - RetryPolicy entity/domain mapper</li>
 * </ul>
 *
 * <h3>Configuration ({@link com.enterprise.scheduler.persistence.config})</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.persistence.config.PersistenceConfiguration} - JPA configuration</li>
 *   <li>{@link com.enterprise.scheduler.persistence.config.DataSourceConfiguration} - DataSource configuration</li>
 *   <li>{@link com.enterprise.scheduler.persistence.config.FlywayConfiguration} - Database migration configuration</li>
 * </ul>
 *
 * <h2>Database Schema</h2>
 * <p>The database schema is managed by Flyway migrations in {@code src/main/resources/db/migration}:
 * <ul>
 *   <li>V1__create_schedules_table.sql - Schedule table</li>
 *   <li>V2__create_jobs_table.sql - Job table with dependencies and tags</li>
 *   <li>V3__create_job_executions_table.sql - Job execution history</li>
 *   <li>V4__create_backup_audit_tables.sql - Audit and backup metadata</li>
 *   <li>V5__create_partitioning.sql - Optional partitioning for large datasets</li>
 * </ul>
 *
 * <h2>Supported Databases</h2>
 * <ul>
 *   <li>PostgreSQL (primary, production-ready)</li>
 *   <li>H2 (for testing and development)</li>
 * </ul>
 *
 * <h2>Key Features</h2>
 * <ul>
 *   <li>JSONB support for flexible configuration storage</li>
 *   <li>Optimized indexes for query performance</li>
 *   <li>Audit trail for job changes</li>
 *   <li>Backup and restore capabilities</li>
 *   <li>Multi-tenancy support</li>
 *   <li>Optimistic locking for concurrency control</li>
 *   <li>Connection pooling with HikariCP</li>
 *   <li>Support for partitioning large execution history tables</li>
 * </ul>
 *
 * @author Job Scheduler Team
 * @version 1.0.0
 * @since 1.0.0
 */
package com.enterprise.scheduler.persistence;
