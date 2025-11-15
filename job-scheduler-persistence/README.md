# Job Scheduler - Persistence Layer

The persistence layer provides data access and repository implementations for the Job Scheduler platform.

## Features

This module supports the following features:

- **Feature #5: Job Persistence** - Persistent storage for job definitions
- **Feature #48: Multi-Database Support** - PostgreSQL and H2 support
- **Feature #22: Job Execution History** - Complete execution tracking
- **Feature #63: Backup and Restore** - Data backup and recovery capabilities

## Architecture

### Entities

JPA entities for database persistence:

- `JobEntity` - Job definitions with configuration, parameters, and metadata
- `JobExecutionEntity` - Job execution records with results and metrics
- `ScheduleEntity` - Scheduling configurations (CRON, interval, one-time)
- `RetryPolicyEntity` - Embedded retry policy configuration

### Repositories

#### Spring Data JPA Repositories
- `JobJpaRepository` - Spring Data repository for jobs
- `JobExecutionJpaRepository` - Spring Data repository for executions
- `ScheduleJpaRepository` - Spring Data repository for schedules

#### SPI Implementations
- `JobRepositoryImpl` - Implements core SPI `JobRepository`
- `JobExecutionRepositoryImpl` - Implements core SPI `JobExecutionRepository`

### Mappers

Bidirectional mappers for converting between domain models and entities:

- `JobMapper` - Job domain ↔ JobEntity
- `JobExecutionMapper` - JobExecution domain ↔ JobExecutionEntity
- `ScheduleMapper` - Schedule domain ↔ ScheduleEntity
- `RetryPolicyMapper` - RetryPolicy domain ↔ RetryPolicyEntity

## Database Schema

### Tables

#### jobs
Core job definitions with:
- Job metadata (name, description, type, group)
- Status and priority
- Configuration and parameters (JSONB)
- Schedule reference
- Retry policy (embedded)
- Dependencies and tags
- Multi-tenancy support
- Audit fields (created/updated timestamps and users)

#### job_executions
Execution history with:
- Execution status and timing
- Trigger information
- Parameters and context (JSONB)
- Results and error details
- Metrics (JSONB)
- Log references
- Node information for cluster support

#### schedules
Schedule configurations with:
- Schedule type (CRON, INTERVAL, etc.)
- CRON expressions and intervals
- Start/end times
- Execution counters
- Misfire strategies

#### job_dependencies
Many-to-many relationship for job dependencies

#### job_tags
Many-to-many relationship for job tags

#### job_audit_log
Audit trail for job changes

#### backup_metadata
Backup operation metadata

### Indexes

Optimized indexes for common queries:
- Job lookups by status, tenant, group, type
- Execution lookups by job, status, time range, node
- Tag-based searches
- GIN indexes for JSONB fields (PostgreSQL)

## Database Support

### PostgreSQL (Primary)

Production-ready database with full feature support:

```yaml
spring:
  profiles:
    active: postgresql
  datasource:
    url: jdbc:postgresql://localhost:5432/job_scheduler
    username: scheduler_user
    password: scheduler_pass
```

Features:
- JSONB support for flexible configuration storage
- GIN indexes for efficient JSON queries
- Advanced indexing strategies
- Connection pooling with HikariCP
- Prepared statement caching

### H2 (Testing/Development)

In-memory database for testing:

```yaml
spring:
  profiles:
    active: h2
  datasource:
    url: jdbc:h2:mem:job_scheduler;MODE=PostgreSQL
```

## Configuration

### Application Properties

```properties
# Active profiles
spring.profiles.active=postgresql,dev

# JPA
spring.jpa.open-in-view=false
spring.jpa.hibernate.ddl-auto=validate

# Flyway
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true
```

### HikariCP Pool Settings

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

## Flyway Migrations

Database schema is managed by Flyway migrations:

- `V1__create_schedules_table.sql` - Schedule table
- `V2__create_jobs_table.sql` - Jobs table with dependencies and tags
- `V3__create_job_executions_table.sql` - Execution history
- `V4__create_backup_audit_tables.sql` - Audit and backup tables
- `V5__create_partitioning.sql` - Optional partitioning for large datasets

Migrations run automatically on application startup.

## Backup and Restore

### BackupService

The `BackupService` provides backup capabilities:

```java
@Autowired
private BackupService backupService;

// Full backup
BackupResult result = backupService.createFullBackup("/path/to/backup.json");

// Jobs only
BackupResult result = backupService.createJobsBackup("/path/to/jobs.json");

// Tenant-specific
BackupResult result = backupService.createTenantBackup("tenant-1", "/path/to/tenant.json");
```

## Usage Examples

### Saving a Job

```java
@Autowired
private JobRepository jobRepository;

Job job = Job.builder()
    .id(IdGenerator.generate())
    .name("data-sync-job")
    .type("shell")
    .status(JobStatus.SCHEDULED)
    .priority(JobPriority.HIGH)
    .tenantId("tenant-1")
    .build();

Job saved = jobRepository.save(job);
```

### Finding Jobs

```java
// By ID
Optional<Job> job = jobRepository.findById("job-123");

// By tenant
List<Job> jobs = jobRepository.findByTenant("tenant-1");

// By status
List<Job> runningJobs = jobRepository.findByStatus(JobStatus.RUNNING);

// By group
List<Job> groupJobs = jobRepository.findByGroup("data-processing");
```

### Saving Executions

```java
@Autowired
private JobExecutionRepository executionRepository;

JobExecution execution = JobExecution.builder()
    .id(IdGenerator.generate())
    .jobId("job-123")
    .status(JobStatus.RUNNING)
    .startTime(Instant.now())
    .triggerType(TriggerType.SCHEDULED)
    .build();

JobExecution saved = executionRepository.save(execution);
```

### Querying Executions

```java
// By job ID
List<JobExecution> executions = executionRepository.findByJobId("job-123");

// With pagination
List<JobExecution> page = executionRepository.findByJobId("job-123", 0, 20);

// Latest execution
Optional<JobExecution> latest = executionRepository.findLatestByJobId("job-123");

// Time range
List<JobExecution> range = executionRepository.findByTimeRange(
    Instant.now().minus(1, ChronoUnit.DAYS),
    Instant.now()
);

// Cleanup old executions
int deleted = executionRepository.deleteOlderThan(
    Instant.now().minus(30, ChronoUnit.DAYS)
);
```

## Performance Considerations

### Indexing Strategy

The schema includes comprehensive indexes for:
- Frequent lookup patterns (by ID, tenant, status)
- Time-based queries (execution history)
- JSON field searches (GIN indexes on PostgreSQL)

### Connection Pooling

HikariCP is configured for optimal performance:
- Prepared statement caching
- Batch operations
- Rewrite of batched inserts
- Connection leak detection

### JSONB Fields

Configuration, parameters, context, and metrics are stored as JSONB:
- Flexible schema for dynamic data
- Indexed for efficient queries
- Support for complex nested structures

### Partitioning (Optional)

For high-volume deployments, `V5__create_partitioning.sql` provides table partitioning by time range for the `job_executions` table.

## Multi-Tenancy

All entities support multi-tenancy through the `tenantId` field:

```java
// Tenant-scoped queries
List<Job> tenantJobs = jobRepository.findByTenant("tenant-1");
long count = jobRepository.countByTenant("tenant-1");

// Unique constraints respect tenancy
// Job names are unique per tenant
```

## Transaction Management

All repository operations are transactional:

- `@Transactional(readOnly = true)` for read operations
- `@Transactional` for write operations
- Optimistic locking via `@Version` on JobEntity

## Testing

Use H2 profile for testing:

```java
@SpringBootTest
@ActiveProfiles("h2")
class JobRepositoryTest {
    @Autowired
    private JobRepository jobRepository;

    @Test
    void testSaveAndFind() {
        Job job = Job.builder()
            .id("test-1")
            .name("test-job")
            .build();

        jobRepository.save(job);
        Optional<Job> found = jobRepository.findById("test-1");
        assertTrue(found.isPresent());
    }
}
```

## Dependencies

```xml
<dependencies>
    <!-- Spring Data JPA -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>

    <!-- PostgreSQL -->
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>

    <!-- H2 (testing) -->
    <dependency>
        <groupId>com.h2database</groupId>
        <artifactId>h2</artifactId>
    </dependency>

    <!-- Flyway -->
    <dependency>
        <groupId>org.flywaydb</groupId>
        <artifactId>flyway-core</artifactId>
    </dependency>
</dependencies>
```

## Migration from Other Databases

To migrate from another database:

1. Export data using `BackupService`
2. Configure new database connection
3. Run Flyway migrations
4. Import data from backup

## Troubleshooting

### Migration Failures

```bash
# Check Flyway status
./mvnw flyway:info

# Repair failed migrations
./mvnw flyway:repair

# Baseline existing database
./mvnw flyway:baseline
```

### Connection Pool Issues

Adjust pool settings based on load:

```yaml
spring.datasource.hikari:
  maximum-pool-size: 50  # Increase for high load
  leak-detection-threshold: 60000  # Detect connection leaks
```

### Performance Issues

1. Enable SQL logging to identify slow queries
2. Check index usage with EXPLAIN ANALYZE
3. Consider partitioning for large tables
4. Adjust connection pool settings
5. Enable query cache for read-heavy workloads

## License

Copyright (c) 2025 Enterprise Scheduler Team
