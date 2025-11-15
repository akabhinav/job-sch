package com.enterprise.scheduler.monitoring.metrics;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Collects system-level metrics
 * Feature #24: System Performance Metrics
 */
@Slf4j
@Component
public class SystemMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final MemoryMXBean memoryMXBean;
    private final ThreadMXBean threadMXBean;
    private final OperatingSystemMXBean osMXBean;

    private ThreadPoolExecutor jobExecutorPool;

    public SystemMetricsCollector(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.memoryMXBean = ManagementFactory.getMemoryMXBean();
        this.threadMXBean = ManagementFactory.getThreadMXBean();
        this.osMXBean = ManagementFactory.getOperatingSystemMXBean();
    }

    @PostConstruct
    public void init() {
        // Register standard JVM metrics
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);

        // Register custom system metrics
        registerMemoryMetrics();
        registerThreadMetrics();
        registerCpuMetrics();

        log.info("System metrics collector initialized");
    }

    /**
     * Register memory metrics
     */
    private void registerMemoryMetrics() {
        // Heap memory
        Gauge.builder("system.memory.heap.used", memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getUsed())
                .description("Used heap memory in bytes")
                .baseUnit("bytes")
                .register(meterRegistry);

        Gauge.builder("system.memory.heap.max", memoryMXBean,
                bean -> bean.getHeapMemoryUsage().getMax())
                .description("Max heap memory in bytes")
                .baseUnit("bytes")
                .register(meterRegistry);

        Gauge.builder("system.memory.heap.usage", memoryMXBean,
                bean -> {
                    double used = bean.getHeapMemoryUsage().getUsed();
                    double max = bean.getHeapMemoryUsage().getMax();
                    return max > 0 ? (used / max) * 100 : 0;
                })
                .description("Heap memory usage percentage")
                .baseUnit("percent")
                .register(meterRegistry);

        // Non-heap memory
        Gauge.builder("system.memory.nonheap.used", memoryMXBean,
                bean -> bean.getNonHeapMemoryUsage().getUsed())
                .description("Used non-heap memory in bytes")
                .baseUnit("bytes")
                .register(meterRegistry);
    }

    /**
     * Register thread metrics
     */
    private void registerThreadMetrics() {
        Gauge.builder("system.threads.count", threadMXBean,
                ThreadMXBean::getThreadCount)
                .description("Current thread count")
                .register(meterRegistry);

        Gauge.builder("system.threads.daemon.count", threadMXBean,
                ThreadMXBean::getDaemonThreadCount)
                .description("Current daemon thread count")
                .register(meterRegistry);

        Gauge.builder("system.threads.peak.count", threadMXBean,
                ThreadMXBean::getPeakThreadCount)
                .description("Peak thread count")
                .register(meterRegistry);

        Gauge.builder("system.threads.total.started", threadMXBean,
                ThreadMXBean::getTotalStartedThreadCount)
                .description("Total started thread count")
                .register(meterRegistry);
    }

    /**
     * Register CPU metrics
     */
    private void registerCpuMetrics() {
        Gauge.builder("system.cpu.count", osMXBean,
                OperatingSystemMXBean::getAvailableProcessors)
                .description("Number of available processors")
                .register(meterRegistry);

        Gauge.builder("system.load.average", osMXBean,
                OperatingSystemMXBean::getSystemLoadAverage)
                .description("System load average")
                .register(meterRegistry);
    }

    /**
     * Register thread pool metrics for job executor
     */
    public void registerThreadPoolMetrics(ThreadPoolExecutor executor, String poolName) {
        this.jobExecutorPool = executor;

        Gauge.builder("threadpool.size", executor,
                ThreadPoolExecutor::getPoolSize)
                .description("Current thread pool size")
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool.active", executor,
                ThreadPoolExecutor::getActiveCount)
                .description("Active thread count in pool")
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool.queue.size", executor,
                exec -> exec.getQueue().size())
                .description("Thread pool queue size")
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool.completed", executor,
                ThreadPoolExecutor::getCompletedTaskCount)
                .description("Completed task count")
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool.core.size", executor,
                ThreadPoolExecutor::getCorePoolSize)
                .description("Core thread pool size")
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool.max.size", executor,
                ThreadPoolExecutor::getMaximumPoolSize)
                .description("Maximum thread pool size")
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool.largest.size", executor,
                ThreadPoolExecutor::getLargestPoolSize)
                .description("Largest thread pool size reached")
                .tag("pool", poolName)
                .register(meterRegistry);

        log.info("Thread pool metrics registered for: {}", poolName);
    }

    /**
     * Collect current system metrics snapshot
     */
    @Scheduled(fixedRate = 60000) // Every minute
    public void collectSystemSnapshot() {
        log.debug("Collecting system metrics snapshot");
        log.debug("Heap usage: {}%", getHeapUsagePercentage());
        log.debug("Thread count: {}", threadMXBean.getThreadCount());
        log.debug("System load: {}", osMXBean.getSystemLoadAverage());

        if (jobExecutorPool != null) {
            log.debug("Thread pool active: {}/{}",
                    jobExecutorPool.getActiveCount(),
                    jobExecutorPool.getPoolSize());
        }
    }

    /**
     * Get heap usage percentage
     */
    public double getHeapUsagePercentage() {
        double used = memoryMXBean.getHeapMemoryUsage().getUsed();
        double max = memoryMXBean.getHeapMemoryUsage().getMax();
        return max > 0 ? (used / max) * 100 : 0;
    }

    /**
     * Get thread pool utilization percentage
     */
    public double getThreadPoolUtilization() {
        if (jobExecutorPool == null) {
            return 0;
        }
        int active = jobExecutorPool.getActiveCount();
        int max = jobExecutorPool.getMaximumPoolSize();
        return max > 0 ? ((double) active / max) * 100 : 0;
    }

    /**
     * Check if system is under high load
     */
    public boolean isHighLoad() {
        double loadAverage = osMXBean.getSystemLoadAverage();
        int processors = osMXBean.getAvailableProcessors();
        return loadAverage > processors * 0.8;
    }
}
