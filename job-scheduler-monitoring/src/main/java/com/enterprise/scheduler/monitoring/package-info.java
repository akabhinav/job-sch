/**
 * Job Scheduler Monitoring and Observability Module
 *
 * <p>This module provides comprehensive monitoring, metrics collection, and observability
 * features for the job scheduler platform.</p>
 *
 * <h2>Features Implemented (Features #21-30)</h2>
 * <ul>
 *   <li>#21: Metrics Collection Infrastructure - Core metrics collection using Micrometer</li>
 *   <li>#22: Execution Monitoring - Track job execution lifecycle and status</li>
 *   <li>#23: Job Metrics Collection - Detailed job-level metrics (duration, success rate, etc.)</li>
 *   <li>#24: System Performance Metrics - JVM, thread pool, memory, CPU metrics</li>
 *   <li>#25: Custom Metrics Dashboard - Custom job scheduler metrics</li>
 *   <li>#26: Health Monitoring - Health indicators for scheduler, database, cluster</li>
 *   <li>#27: Performance Analytics - Aggregation and analysis of metrics</li>
 *   <li>#28: Real-time Metrics Aggregation - Live metrics snapshots and trends</li>
 *   <li>#29: Alerting System - Rule-based alerting with NotificationProvider integration</li>
 *   <li>#30: Alert Rules Configuration - Configurable alert rules and thresholds</li>
 * </ul>
 *
 * <h2>Main Components</h2>
 *
 * <h3>Metrics Collectors</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.monitoring.metrics.JobExecutionMetricsCollector} -
 *       Collects job execution metrics (duration, success/failure rates)</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.metrics.SystemMetricsCollector} -
 *       Collects system metrics (memory, CPU, thread pool)</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.metrics.CustomJobMetricsCollector} -
 *       Collects custom job scheduler metrics</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.metrics.MetricsService} -
 *       Central service for metrics collection</li>
 * </ul>
 *
 * <h3>Health Indicators</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.monitoring.health.JobSchedulerHealthIndicator} -
 *       Health check for job scheduler system</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.health.DatabaseHealthIndicator} -
 *       Health check for database connectivity</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.health.ClusterHealthIndicator} -
 *       Health check for cluster status</li>
 * </ul>
 *
 * <h3>Performance Analytics</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.monitoring.analytics.PerformanceAnalyticsService} -
 *       Aggregates and analyzes performance metrics</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.model.PerformanceSnapshot} -
 *       Snapshot of system performance at a point in time</li>
 * </ul>
 *
 * <h3>Alerting</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.monitoring.alerting.AlertingService} -
 *       Manages alerts and integrates with NotificationProvider SPI</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.model.AlertRule} -
 *       Alert rule configuration</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.model.Alert} -
 *       Alert instance</li>
 * </ul>
 *
 * <h3>Configuration</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.monitoring.config.MonitoringConfiguration} -
 *       Monitoring module configuration</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.config.ActuatorConfiguration} -
 *       Spring Boot Actuator configuration</li>
 * </ul>
 *
 * <h3>API</h3>
 * <ul>
 *   <li>{@link com.enterprise.scheduler.monitoring.api.MonitoringController} -
 *       REST API for monitoring operations</li>
 *   <li>{@link com.enterprise.scheduler.monitoring.actuator.JobSchedulerEndpoint} -
 *       Custom Actuator endpoint</li>
 * </ul>
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Inject MetricsService
 * @Autowired
 * private MetricsService metricsService;
 *
 * // Record job start
 * metricsService.recordJobStart(execution);
 *
 * // Record job completion
 * metricsService.recordJobComplete(execution);
 *
 * // Get metrics snapshot
 * MetricsSnapshot snapshot = metricsService.getMetricsSnapshot();
 * }</pre>
 *
 * <h2>Actuator Endpoints</h2>
 * <ul>
 *   <li>/actuator/health - Health status (with custom indicators)</li>
 *   <li>/actuator/metrics - Micrometer metrics</li>
 *   <li>/actuator/prometheus - Prometheus metrics endpoint</li>
 *   <li>/actuator/jobscheduler - Custom job scheduler status</li>
 * </ul>
 *
 * <h2>REST API Endpoints</h2>
 * <ul>
 *   <li>GET /api/monitoring/metrics/snapshot - Current metrics snapshot</li>
 *   <li>GET /api/monitoring/performance/current - Current performance snapshot</li>
 *   <li>GET /api/monitoring/performance/history - Performance history</li>
 *   <li>GET /api/monitoring/alerts/active - Active alerts</li>
 *   <li>GET /api/monitoring/alerts/rules - Alert rules</li>
 *   <li>POST /api/monitoring/alerts/rules - Create alert rule</li>
 * </ul>
 *
 * <h2>Configuration Properties</h2>
 * <p>See application.yml for configuration options:</p>
 * <ul>
 *   <li>scheduler.monitoring.enabled - Enable/disable monitoring</li>
 *   <li>scheduler.monitoring.metrics.collection-interval - Metrics collection interval</li>
 *   <li>scheduler.monitoring.alerting.enabled - Enable/disable alerting</li>
 *   <li>scheduler.monitoring.alerting.evaluation-interval - Alert evaluation interval</li>
 *   <li>management.endpoints.web.exposure.include - Actuator endpoints to expose</li>
 * </ul>
 *
 * @since 1.0.0
 * @author Enterprise Scheduler Team
 */
package com.enterprise.scheduler.monitoring;
