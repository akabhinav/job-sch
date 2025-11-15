package com.enterprise.scheduler.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Monitors the health of cluster nodes.
 * Tracks metrics, detects failures, and triggers alerts.
 */
@Service
public class NodeHealthMonitor {

    private static final Logger logger = LoggerFactory.getLogger(NodeHealthMonitor.class);
    private static final String NODE_HEALTH_MAP = "node-health";

    private final HazelcastInstance hazelcastInstance;
    private final ClusterManager clusterManager;
    private final NodeRegistry nodeRegistry;
    private final IMap<String, NodeHealthStatus> nodeHealthMap;

    @Value("${cluster.health.check.interval:30}")
    private int healthCheckIntervalSeconds;

    @Value("${cluster.health.threshold.cpu:90.0}")
    private double cpuThreshold;

    @Value("${cluster.health.threshold.memory:90.0}")
    private double memoryThreshold;

    @Value("${cluster.health.threshold.disk:90.0}")
    private double diskThreshold;

    private final ScheduledExecutorService healthCheckExecutor;
    private final Map<String, HealthCheckListener> healthListeners;
    private volatile boolean running = false;

    @Autowired
    public NodeHealthMonitor(HazelcastInstance hazelcastInstance,
                            ClusterManager clusterManager,
                            NodeRegistry nodeRegistry) {
        this.hazelcastInstance = hazelcastInstance;
        this.clusterManager = clusterManager;
        this.nodeRegistry = nodeRegistry;
        this.nodeHealthMap = hazelcastInstance.getMap(NODE_HEALTH_MAP);
        this.healthCheckExecutor = Executors.newScheduledThreadPool(2,
            r -> new Thread(r, "health-monitor-thread"));
        this.healthListeners = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the health monitor.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing NodeHealthMonitor");

        running = true;

        // Schedule local health checks
        healthCheckExecutor.scheduleAtFixedRate(
            this::performLocalHealthCheck,
            10,
            healthCheckIntervalSeconds,
            TimeUnit.SECONDS
        );

        // Schedule cluster health monitoring
        healthCheckExecutor.scheduleAtFixedRate(
            this::monitorClusterHealth,
            30,
            healthCheckIntervalSeconds * 2,
            TimeUnit.SECONDS
        );

        logger.info("NodeHealthMonitor initialized with {} second interval",
                   healthCheckIntervalSeconds);
    }

    /**
     * Performs health check on the local node.
     */
    private void performLocalHealthCheck() {
        if (!running) {
            return;
        }

        try {
            String nodeId = clusterManager.getLocalMemberId();
            NodeHealthStatus healthStatus = collectLocalHealthStatus();

            // Store in distributed map
            nodeHealthMap.put(nodeId, healthStatus);

            // Update node registry metrics
            nodeRegistry.updateLocalNodeMetrics();

            // Check for health issues
            List<HealthIssue> issues = detectHealthIssues(healthStatus);
            if (!issues.isEmpty()) {
                logger.warn("Detected {} health issue(s) on local node", issues.size());
                notifyHealthIssues(nodeId, issues);
            }

            logger.debug("Local health check completed: {}", healthStatus.getOverallHealth());

        } catch (Exception e) {
            logger.error("Error performing local health check", e);
        }
    }

    /**
     * Monitors health of all cluster nodes.
     */
    private void monitorClusterHealth() {
        if (!running) {
            return;
        }

        try {
            Set<String> clusterNodeIds = clusterManager.getClusterMembers().stream()
                .map(m -> m.getUuid().toString())
                .collect(Collectors.toSet());

            Instant threshold = Instant.now().minusSeconds(healthCheckIntervalSeconds * 3);

            // Check each node's health status
            for (String nodeId : clusterNodeIds) {
                NodeHealthStatus status = nodeHealthMap.get(nodeId);

                if (status == null) {
                    logger.warn("No health status found for node: {}", nodeId);
                    continue;
                }

                // Check if health data is stale
                if (status.getTimestamp().isBefore(threshold)) {
                    logger.warn("Stale health data for node: {} (last update: {})",
                               nodeId, status.getTimestamp());
                    notifyHealthIssues(nodeId, Collections.singletonList(
                        new HealthIssue(HealthIssueType.STALE_HEALTH_DATA,
                                      "Health data is stale",
                                      IssueSeverity.WARNING)
                    ));
                }

                // Check overall health
                if (status.getOverallHealth() == HealthLevel.UNHEALTHY) {
                    logger.warn("Node {} is UNHEALTHY", nodeId);
                } else if (status.getOverallHealth() == HealthLevel.DEGRADED) {
                    logger.info("Node {} is DEGRADED", nodeId);
                }
            }

            // Clean up health data for nodes that left the cluster
            nodeHealthMap.keySet().removeIf(nodeId -> !clusterNodeIds.contains(nodeId));

            logger.debug("Cluster health monitoring completed");

        } catch (Exception e) {
            logger.error("Error monitoring cluster health", e);
        }
    }

    /**
     * Collects health status of the local node.
     */
    private NodeHealthStatus collectLocalHealthStatus() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        // CPU metrics
        double cpuLoad = osBean.getSystemLoadAverage() >= 0 ?
            osBean.getSystemLoadAverage() / osBean.getAvailableProcessors() * 100 : 0;

        // Memory metrics
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        double memoryUsage = (double) heapUsed / heapMax * 100;

        // Thread metrics
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();

        // Disk metrics (simplified - would need more detailed implementation)
        double diskUsage = getDiskUsage();

        // Network metrics (simplified)
        NetworkMetrics networkMetrics = collectNetworkMetrics();

        // Determine overall health
        HealthLevel overallHealth = determineOverallHealth(
            cpuLoad, memoryUsage, diskUsage
        );

        return new NodeHealthStatus(
            clusterManager.getLocalMemberId(),
            overallHealth,
            cpuLoad,
            memoryUsage,
            diskUsage,
            threadCount,
            heapUsed,
            heapMax,
            networkMetrics,
            Instant.now()
        );
    }

    /**
     * Detects health issues from status.
     */
    private List<HealthIssue> detectHealthIssues(NodeHealthStatus status) {
        List<HealthIssue> issues = new ArrayList<>();

        // Check CPU usage
        if (status.getCpuUsage() > cpuThreshold) {
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_CPU_USAGE,
                String.format("CPU usage is %.2f%% (threshold: %.2f%%)",
                            status.getCpuUsage(), cpuThreshold),
                status.getCpuUsage() > 95 ? IssueSeverity.CRITICAL : IssueSeverity.WARNING
            ));
        }

        // Check memory usage
        if (status.getMemoryUsage() > memoryThreshold) {
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_MEMORY_USAGE,
                String.format("Memory usage is %.2f%% (threshold: %.2f%%)",
                            status.getMemoryUsage(), memoryThreshold),
                status.getMemoryUsage() > 95 ? IssueSeverity.CRITICAL : IssueSeverity.WARNING
            ));
        }

        // Check disk usage
        if (status.getDiskUsage() > diskThreshold) {
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_DISK_USAGE,
                String.format("Disk usage is %.2f%% (threshold: %.2f%%)",
                            status.getDiskUsage(), diskThreshold),
                status.getDiskUsage() > 95 ? IssueSeverity.CRITICAL : IssueSeverity.WARNING
            ));
        }

        // Check thread count (simplified threshold)
        if (status.getThreadCount() > 1000) {
            issues.add(new HealthIssue(
                HealthIssueType.HIGH_THREAD_COUNT,
                String.format("Thread count is %d", status.getThreadCount()),
                IssueSeverity.WARNING
            ));
        }

        return issues;
    }

    /**
     * Determines overall health level based on metrics.
     */
    private HealthLevel determineOverallHealth(double cpu, double memory, double disk) {
        // Critical thresholds
        if (cpu > 95 || memory > 95 || disk > 95) {
            return HealthLevel.UNHEALTHY;
        }

        // Warning thresholds
        if (cpu > cpuThreshold || memory > memoryThreshold || disk > diskThreshold) {
            return HealthLevel.DEGRADED;
        }

        return HealthLevel.HEALTHY;
    }

    /**
     * Gets disk usage percentage (simplified).
     */
    private double getDiskUsage() {
        try {
            java.io.File root = new java.io.File("/");
            long total = root.getTotalSpace();
            long free = root.getFreeSpace();
            return (double) (total - free) / total * 100;
        } catch (Exception e) {
            logger.debug("Could not get disk usage", e);
            return 0.0;
        }
    }

    /**
     * Collects network metrics (simplified).
     */
    private NetworkMetrics collectNetworkMetrics() {
        // In a real implementation, would collect actual network stats
        return new NetworkMetrics(0, 0, 0, 0);
    }

    /**
     * Gets health status for a specific node.
     */
    public Optional<NodeHealthStatus> getNodeHealth(String nodeId) {
        return Optional.ofNullable(nodeHealthMap.get(nodeId));
    }

    /**
     * Gets health status for all nodes.
     */
    public Map<String, NodeHealthStatus> getAllNodeHealth() {
        return new HashMap<>(nodeHealthMap);
    }

    /**
     * Gets healthy nodes.
     */
    public List<String> getHealthyNodes() {
        return nodeHealthMap.entrySet().stream()
            .filter(entry -> entry.getValue().getOverallHealth() == HealthLevel.HEALTHY)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Gets unhealthy nodes.
     */
    public List<String> getUnhealthyNodes() {
        return nodeHealthMap.entrySet().stream()
            .filter(entry -> entry.getValue().getOverallHealth() == HealthLevel.UNHEALTHY)
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
    }

    /**
     * Registers a health check listener.
     */
    public void registerHealthListener(String listenerId, HealthCheckListener listener) {
        healthListeners.put(listenerId, listener);
        logger.debug("Registered health check listener: {}", listenerId);
    }

    /**
     * Unregisters a health check listener.
     */
    public void unregisterHealthListener(String listenerId) {
        healthListeners.remove(listenerId);
        logger.debug("Unregistered health check listener: {}", listenerId);
    }

    /**
     * Notifies listeners of health issues.
     */
    private void notifyHealthIssues(String nodeId, List<HealthIssue> issues) {
        HealthCheckEvent event = new HealthCheckEvent(nodeId, issues);

        healthListeners.values().forEach(listener -> {
            try {
                listener.onHealthIssue(event);
            } catch (Exception e) {
                logger.error("Error notifying health check listener", e);
            }
        });
    }

    /**
     * Shutdown the health monitor.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down NodeHealthMonitor");

        running = false;

        healthCheckExecutor.shutdown();
        try {
            if (!healthCheckExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                healthCheckExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            healthCheckExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        healthListeners.clear();

        logger.info("NodeHealthMonitor shutdown complete");
    }

    /**
     * Node health status.
     */
    public static class NodeHealthStatus implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String nodeId;
        private final HealthLevel overallHealth;
        private final double cpuUsage;
        private final double memoryUsage;
        private final double diskUsage;
        private final int threadCount;
        private final long heapUsed;
        private final long heapMax;
        private final NetworkMetrics networkMetrics;
        private final Instant timestamp;

        public NodeHealthStatus(String nodeId, HealthLevel overallHealth,
                               double cpuUsage, double memoryUsage, double diskUsage,
                               int threadCount, long heapUsed, long heapMax,
                               NetworkMetrics networkMetrics, Instant timestamp) {
            this.nodeId = nodeId;
            this.overallHealth = overallHealth;
            this.cpuUsage = cpuUsage;
            this.memoryUsage = memoryUsage;
            this.diskUsage = diskUsage;
            this.threadCount = threadCount;
            this.heapUsed = heapUsed;
            this.heapMax = heapMax;
            this.networkMetrics = networkMetrics;
            this.timestamp = timestamp;
        }

        public String getNodeId() { return nodeId; }
        public HealthLevel getOverallHealth() { return overallHealth; }
        public double getCpuUsage() { return cpuUsage; }
        public double getMemoryUsage() { return memoryUsage; }
        public double getDiskUsage() { return diskUsage; }
        public int getThreadCount() { return threadCount; }
        public long getHeapUsed() { return heapUsed; }
        public long getHeapMax() { return heapMax; }
        public NetworkMetrics getNetworkMetrics() { return networkMetrics; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Network metrics.
     */
    public static class NetworkMetrics implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final long bytesReceived;
        private final long bytesSent;
        private final long packetsReceived;
        private final long packetsSent;

        public NetworkMetrics(long bytesReceived, long bytesSent,
                            long packetsReceived, long packetsSent) {
            this.bytesReceived = bytesReceived;
            this.bytesSent = bytesSent;
            this.packetsReceived = packetsReceived;
            this.packetsSent = packetsSent;
        }

        public long getBytesReceived() { return bytesReceived; }
        public long getBytesSent() { return bytesSent; }
        public long getPacketsReceived() { return packetsReceived; }
        public long getPacketsSent() { return packetsSent; }
    }

    /**
     * Health levels.
     */
    public enum HealthLevel {
        HEALTHY,
        DEGRADED,
        UNHEALTHY,
        UNKNOWN
    }

    /**
     * Health issue types.
     */
    public enum HealthIssueType {
        HIGH_CPU_USAGE,
        HIGH_MEMORY_USAGE,
        HIGH_DISK_USAGE,
        HIGH_THREAD_COUNT,
        NETWORK_ERROR,
        STALE_HEALTH_DATA,
        OTHER
    }

    /**
     * Issue severity levels.
     */
    public enum IssueSeverity {
        INFO,
        WARNING,
        CRITICAL
    }

    /**
     * Health issue.
     */
    public static class HealthIssue {
        private final HealthIssueType type;
        private final String description;
        private final IssueSeverity severity;
        private final Instant timestamp;

        public HealthIssue(HealthIssueType type, String description, IssueSeverity severity) {
            this.type = type;
            this.description = description;
            this.severity = severity;
            this.timestamp = Instant.now();
        }

        public HealthIssueType getType() { return type; }
        public String getDescription() { return description; }
        public IssueSeverity getSeverity() { return severity; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Health check event.
     */
    public static class HealthCheckEvent {
        private final String nodeId;
        private final List<HealthIssue> issues;
        private final Instant timestamp;

        public HealthCheckEvent(String nodeId, List<HealthIssue> issues) {
            this.nodeId = nodeId;
            this.issues = issues;
            this.timestamp = Instant.now();
        }

        public String getNodeId() { return nodeId; }
        public List<HealthIssue> getIssues() { return issues; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Health check listener interface.
     */
    public interface HealthCheckListener {
        void onHealthIssue(HealthCheckEvent event);
    }
}
