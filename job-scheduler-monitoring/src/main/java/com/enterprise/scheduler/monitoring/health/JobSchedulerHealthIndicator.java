package com.enterprise.scheduler.monitoring.health;

import com.enterprise.scheduler.monitoring.metrics.SystemMetricsCollector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for job scheduler system
 * Feature #26: Health Monitoring
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobSchedulerHealthIndicator implements HealthIndicator {

    private final SystemMetricsCollector systemMetrics;
    private static final double HEAP_USAGE_WARNING_THRESHOLD = 80.0;
    private static final double HEAP_USAGE_CRITICAL_THRESHOLD = 90.0;
    private static final double THREAD_POOL_WARNING_THRESHOLD = 80.0;
    private static final double THREAD_POOL_CRITICAL_THRESHOLD = 95.0;

    @Override
    public Health health() {
        try {
            Map<String, Object> details = new HashMap<>();

            // Check heap memory
            double heapUsage = systemMetrics.getHeapUsagePercentage();
            details.put("heapUsagePercent", heapUsage);
            details.put("heapUsageStatus", getUsageStatus(heapUsage, HEAP_USAGE_WARNING_THRESHOLD, HEAP_USAGE_CRITICAL_THRESHOLD));

            // Check thread pool
            double threadPoolUtilization = systemMetrics.getThreadPoolUtilization();
            details.put("threadPoolUtilization", threadPoolUtilization);
            details.put("threadPoolStatus", getUsageStatus(threadPoolUtilization, THREAD_POOL_WARNING_THRESHOLD, THREAD_POOL_CRITICAL_THRESHOLD));

            // Check system load
            boolean isHighLoad = systemMetrics.isHighLoad();
            details.put("isHighLoad", isHighLoad);
            details.put("loadStatus", isHighLoad ? "WARNING" : "HEALTHY");

            // Thread info
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            details.put("threadCount", threadMXBean.getThreadCount());
            details.put("peakThreadCount", threadMXBean.getPeakThreadCount());
            details.put("daemonThreadCount", threadMXBean.getDaemonThreadCount());

            // Determine overall health
            Health.Builder healthBuilder = Health.up();

            if (heapUsage >= HEAP_USAGE_CRITICAL_THRESHOLD ||
                threadPoolUtilization >= THREAD_POOL_CRITICAL_THRESHOLD) {
                healthBuilder = Health.down();
                details.put("reason", "Critical resource usage detected");
            } else if (heapUsage >= HEAP_USAGE_WARNING_THRESHOLD ||
                       threadPoolUtilization >= THREAD_POOL_WARNING_THRESHOLD ||
                       isHighLoad) {
                healthBuilder = Health.status("WARNING");
                details.put("reason", "Resource usage approaching limits");
            } else {
                details.put("status", "All systems operational");
            }

            return healthBuilder.withDetails(details).build();

        } catch (Exception e) {
            log.error("Error checking job scheduler health", e);
            return Health.down()
                    .withException(e)
                    .build();
        }
    }

    /**
     * Get usage status string
     */
    private String getUsageStatus(double usage, double warningThreshold, double criticalThreshold) {
        if (usage >= criticalThreshold) {
            return "CRITICAL";
        } else if (usage >= warningThreshold) {
            return "WARNING";
        } else {
            return "HEALTHY";
        }
    }
}
