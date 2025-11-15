# Job Scheduler Monitoring Module

This module provides comprehensive monitoring, metrics collection, and observability features for the Enterprise Job Scheduler Platform.

## Features (Features #21-30)

### Metrics Collection
- **#21: Metrics Collection Infrastructure** - Core infrastructure using Micrometer
- **#22: Execution Monitoring** - Track job execution lifecycle
- **#23: Job Metrics Collection** - Job-specific metrics (duration, success/failure rates)
- **#24: System Performance Metrics** - JVM, thread pool, memory, CPU metrics
- **#25: Custom Metrics Dashboard** - Custom job scheduler metrics

### Health Monitoring
- **#26: Health Monitoring** - Health indicators for scheduler, database, and cluster
- **#27: Performance Analytics** - Metrics aggregation and analysis
- **#28: Real-time Metrics Aggregation** - Live metrics snapshots and trends

### Alerting
- **#29: Alerting System** - Rule-based alerting with NotificationProvider SPI integration
- **#30: Alert Rules Configuration** - Configurable alert rules and thresholds

## Architecture

```
monitoring/
├── metrics/           # Metrics collectors
├── health/            # Health indicators
├── analytics/         # Performance analytics
├── alerting/          # Alerting system
├── model/             # Domain models and DTOs
├── config/            # Configuration classes
├── api/               # REST API controllers
├── actuator/          # Custom Actuator endpoints
└── event/             # Event listeners
```

## Components

### Metrics Collectors

1. **JobExecutionMetricsCollector** - Collects job execution metrics
   - Execution duration
   - Success/failure rates
   - Retry counts
   - Timeout events

2. **SystemMetricsCollector** - Collects system-level metrics
   - Heap memory usage
   - Thread pool statistics
   - CPU load
   - Thread counts

3. **CustomJobMetricsCollector** - Collects custom job scheduler metrics
   - Trigger type distributions
   - Misfired jobs
   - Dead letter jobs
   - Workflow metrics

4. **MetricsService** - Central metrics collection service
   - Unified interface for all metrics
   - Metrics snapshot retrieval
   - Job-specific metrics

### Health Indicators

1. **JobSchedulerHealthIndicator** - Job scheduler system health
   - Heap usage status
   - Thread pool utilization
   - System load monitoring

2. **DatabaseHealthIndicator** - Database connectivity health
   - Connection status
   - Query performance
   - Connection pool metrics

3. **ClusterHealthIndicator** - Cluster health status
   - Member count
   - Cluster connectivity
   - Node information

### Performance Analytics

1. **PerformanceAnalyticsService** - Performance analysis and aggregation
   - Periodic snapshot collection
   - Metric trending
   - Performance summaries
   - Historical data retention

2. **PerformanceSnapshot** - Performance data model
   - System metrics
   - Execution metrics
   - Cluster metrics
   - Custom metrics

### Alerting System

1. **AlertingService** - Alert management and notification
   - Rule-based alerting
   - NotificationProvider integration
   - Alert lifecycle management
   - Cooldown periods

2. **AlertRule** - Alert rule configuration
   - Threshold-based rules
   - Multiple comparison operators
   - Severity levels
   - Cooldown configuration

3. **Alert** - Alert instance
   - Alert status tracking
   - Acknowledgment support
   - Resolution tracking

## Configuration

### Application Properties (application.yml)

```yaml
# Management endpoints
management:
  endpoints:
    web:
      exposure:
        include: '*'
      base-path: /actuator

  # Metrics configuration
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        job.execution.duration: true

# Scheduler monitoring
scheduler:
  monitoring:
    enabled: true
    metrics:
      collection-interval: 30s
    analytics:
      enabled: true
      snapshot-interval: 30s
    alerting:
      enabled: true
      evaluation-interval: 30s
```

## REST API Endpoints

### Metrics Endpoints
- `GET /api/monitoring/metrics/snapshot` - Current metrics snapshot
- `GET /api/monitoring/metrics/job/{jobName}` - Job-specific metrics

### Performance Endpoints
- `GET /api/monitoring/performance/current` - Current performance snapshot
- `GET /api/monitoring/performance/history?count={count}` - Performance history
- `GET /api/monitoring/performance/summary` - Performance summary
- `GET /api/monitoring/performance/trend/{metricName}?count={count}` - Metric trend

### Alert Endpoints
- `GET /api/monitoring/alerts/rules` - List all alert rules
- `GET /api/monitoring/alerts/rules/{ruleId}` - Get alert rule
- `POST /api/monitoring/alerts/rules` - Create alert rule
- `PUT /api/monitoring/alerts/rules/{ruleId}` - Update alert rule
- `DELETE /api/monitoring/alerts/rules/{ruleId}` - Delete alert rule
- `POST /api/monitoring/alerts/rules/{ruleId}/enable` - Enable alert rule
- `POST /api/monitoring/alerts/rules/{ruleId}/disable` - Disable alert rule
- `GET /api/monitoring/alerts/active` - List active alerts
- `GET /api/monitoring/alerts/history?count={count}` - Alert history
- `POST /api/monitoring/alerts/{alertId}/acknowledge?user={user}` - Acknowledge alert
- `POST /api/monitoring/alerts/{alertId}/resolve` - Resolve alert
- `GET /api/monitoring/alerts/statistics` - Alert statistics
- `DELETE /api/monitoring/alerts/active` - Clear all alerts

## Actuator Endpoints

### Standard Endpoints
- `/actuator/health` - Health status with custom indicators
- `/actuator/metrics` - Micrometer metrics
- `/actuator/prometheus` - Prometheus metrics endpoint
- `/actuator/info` - Application information
- `/actuator/env` - Environment properties
- `/actuator/loggers` - Logger configuration
- `/actuator/threaddump` - Thread dump
- `/actuator/heapdump` - Heap dump

### Custom Endpoints
- `/actuator/jobscheduler` - Job scheduler comprehensive status

## Usage Examples

### Recording Metrics

```java
@Autowired
private MetricsService metricsService;

// Record job start
metricsService.recordJobStart(execution);

// Record job completion
metricsService.recordJobComplete(execution);

// Record job failure
metricsService.recordJobFailure("myJob", "TimeoutException");

// Get metrics snapshot
MetricsSnapshot snapshot = metricsService.getMetricsSnapshot();
```

### Creating Alert Rules

```java
@Autowired
private AlertingService alertingService;

// Create alert rule
AlertRule rule = AlertRule.builder()
    .id("high-failure-rate")
    .name("High Job Failure Rate")
    .metricName("failureRate")
    .operator(ComparisonOperator.GREATER_THAN)
    .threshold(10.0)
    .severity(AlertSeverity.ERROR)
    .cooldownSeconds(300)
    .build();

alertingService.registerAlertRule(rule);
```

### Accessing Performance Analytics

```java
@Autowired
private PerformanceAnalyticsService performanceAnalytics;

// Get current snapshot
PerformanceSnapshot current = performanceAnalytics.getCurrentSnapshot();

// Get recent snapshots
List<PerformanceSnapshot> history = performanceAnalytics.getRecentSnapshots(20);

// Get metric trend
List<MetricDataPoint> trend = performanceAnalytics.getMetricTrend("heapUsagePercent", 50);

// Get performance summary
PerformanceSummary summary = performanceAnalytics.getPerformanceSummary();
```

## Integration with Core Module

The monitoring module integrates with the core job scheduler through:

1. **Event Listeners** - `MetricsEventListener` listens for job execution events
2. **Notification Provider SPI** - Alerting service uses NotificationProvider for alerts
3. **Job Execution Hooks** - Metrics collected at job lifecycle points

## Metrics Collected

### Job Execution Metrics
- `job.executions.total` - Total number of executions
- `job.executions.successful` - Successful executions
- `job.executions.failed` - Failed executions
- `job.executions.timeout` - Timeout executions
- `job.executions.retried` - Retried executions
- `job.executions.active` - Currently active executions
- `job.execution.duration` - Execution duration (timer)
- `job.executions.by_job.*` - Per-job metrics

### System Metrics
- `system.memory.heap.used` - Heap memory used
- `system.memory.heap.max` - Max heap memory
- `system.memory.heap.usage` - Heap usage percentage
- `system.threads.count` - Thread count
- `system.cpu.count` - CPU count
- `system.load.average` - System load average
- `threadpool.*` - Thread pool metrics

### Custom Metrics
- `job.trigger.*` - Trigger type counters
- `job.misfired` - Misfired jobs
- `job.deadletter` - Dead letter jobs
- `job.count` - Job counts by status
- `workflow.executions` - Workflow executions

## Default Alert Rules

The following alert rules are registered by default:

1. **High Heap Usage** (WARNING)
   - Threshold: 85%
   - Cooldown: 5 minutes

2. **Critical Heap Usage** (CRITICAL)
   - Threshold: 95%
   - Cooldown: 3 minutes

3. **High Job Failure Rate** (ERROR)
   - Threshold: 10%
   - Cooldown: 10 minutes

4. **High System Load** (WARNING)
   - Threshold: 5.0
   - Cooldown: 5 minutes

## Dependencies

- Spring Boot Actuator
- Micrometer Core
- Micrometer Prometheus Registry
- Job Scheduler Core Module
- Job Scheduler Common Module

## Testing

Run tests with:
```bash
mvn test -pl job-scheduler-monitoring
```

## License

Enterprise License - Internal Use Only
