package com.enterprise.scheduler.cluster;

import com.hazelcast.cluster.Member;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Service for discovering and tracking nodes in the cluster.
 * Maintains node capabilities, status, and availability information.
 */
@Service
public class NodeDiscoveryService {

    private static final Logger logger = LoggerFactory.getLogger(NodeDiscoveryService.class);
    private static final String NODE_INFO_MAP = "node-discovery-info";
    private static final long DISCOVERY_INTERVAL_SECONDS = 30;
    private static final long NODE_TIMEOUT_SECONDS = 120;

    private final HazelcastInstance hazelcastInstance;
    private final ClusterManager clusterManager;
    private final IMap<String, NodeDiscoveryInfo> nodeInfoMap;
    private final ScheduledExecutorService discoveryExecutor;
    private final Map<String, NodeCapabilities> localCapabilitiesCache;

    @Autowired
    public NodeDiscoveryService(HazelcastInstance hazelcastInstance,
                               ClusterManager clusterManager) {
        this.hazelcastInstance = hazelcastInstance;
        this.clusterManager = clusterManager;
        this.nodeInfoMap = hazelcastInstance.getMap(NODE_INFO_MAP);
        this.discoveryExecutor = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "node-discovery-thread")
        );
        this.localCapabilitiesCache = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the node discovery service.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing NodeDiscoveryService");

        // Register this node
        registerLocalNode();

        // Schedule periodic discovery updates
        discoveryExecutor.scheduleAtFixedRate(
            this::updateNodeDiscovery,
            DISCOVERY_INTERVAL_SECONDS,
            DISCOVERY_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        // Schedule stale node cleanup
        discoveryExecutor.scheduleAtFixedRate(
            this::cleanupStaleNodes,
            60,
            60,
            TimeUnit.SECONDS
        );

        logger.info("NodeDiscoveryService initialized");
    }

    /**
     * Registers the local node with its capabilities.
     */
    public void registerLocalNode() {
        String nodeId = clusterManager.getLocalMemberId();
        Member localMember = hazelcastInstance.getCluster().getLocalMember();

        NodeCapabilities capabilities = detectNodeCapabilities();
        localCapabilitiesCache.put(nodeId, capabilities);

        NodeDiscoveryInfo info = new NodeDiscoveryInfo(
            nodeId,
            localMember.getAddress().getHost(),
            localMember.getAddress().getPort(),
            capabilities,
            NodeStatus.ACTIVE,
            Instant.now()
        );

        nodeInfoMap.put(nodeId, info);
        logger.info("Registered local node: {} with capabilities: {}", nodeId, capabilities);
    }

    /**
     * Detects capabilities of the local node.
     */
    private NodeCapabilities detectNodeCapabilities() {
        Runtime runtime = Runtime.getRuntime();

        return new NodeCapabilities(
            runtime.availableProcessors(),
            runtime.maxMemory(),
            getSupportedJobTypes(),
            getNodeTags(),
            getPriorityLevel()
        );
    }

    /**
     * Gets supported job types for this node.
     * Can be configured via system properties or environment variables.
     */
    private Set<String> getSupportedJobTypes() {
        String jobTypes = System.getProperty("node.job.types",
                                            System.getenv().getOrDefault("NODE_JOB_TYPES", "ALL"));

        if ("ALL".equalsIgnoreCase(jobTypes)) {
            return Set.of("BATCH", "REALTIME", "SCHEDULED", "ADHOC");
        }

        return Arrays.stream(jobTypes.split(","))
                    .map(String::trim)
                    .map(String::toUpperCase)
                    .collect(Collectors.toSet());
    }

    /**
     * Gets node tags for specialized routing.
     */
    private Set<String> getNodeTags() {
        String tags = System.getProperty("node.tags",
                                        System.getenv().getOrDefault("NODE_TAGS", ""));

        if (tags.isEmpty()) {
            return new HashSet<>();
        }

        return Arrays.stream(tags.split(","))
                    .map(String::trim)
                    .collect(Collectors.toSet());
    }

    /**
     * Gets node priority level (higher priority nodes get more jobs).
     */
    private int getPriorityLevel() {
        return Integer.parseInt(
            System.getProperty("node.priority",
                             System.getenv().getOrDefault("NODE_PRIORITY", "5"))
        );
    }

    /**
     * Updates node discovery information periodically.
     */
    private void updateNodeDiscovery() {
        try {
            registerLocalNode();
            logger.debug("Updated node discovery information");
        } catch (Exception e) {
            logger.error("Failed to update node discovery", e);
        }
    }

    /**
     * Cleans up stale node entries.
     */
    private void cleanupStaleNodes() {
        try {
            Instant threshold = Instant.now().minusSeconds(NODE_TIMEOUT_SECONDS);
            Set<String> clusterMemberIds = clusterManager.getClusterMembers().stream()
                .map(m -> m.getUuid().toString())
                .collect(Collectors.toSet());

            nodeInfoMap.entrySet().removeIf(entry -> {
                String nodeId = entry.getKey();
                NodeDiscoveryInfo info = entry.getValue();

                // Remove if not in cluster or last update is too old
                boolean shouldRemove = !clusterMemberIds.contains(nodeId) ||
                                      info.getLastUpdate().isBefore(threshold);

                if (shouldRemove) {
                    logger.info("Removing stale node from discovery: {}", nodeId);
                }

                return shouldRemove;
            });

        } catch (Exception e) {
            logger.error("Failed to cleanup stale nodes", e);
        }
    }

    /**
     * Discovers all active nodes in the cluster.
     */
    public List<NodeDiscoveryInfo> discoverNodes() {
        return new ArrayList<>(nodeInfoMap.values());
    }

    /**
     * Discovers nodes that can handle a specific job type.
     */
    public List<NodeDiscoveryInfo> discoverNodesForJobType(String jobType) {
        return nodeInfoMap.values().stream()
            .filter(info -> info.getStatus() == NodeStatus.ACTIVE)
            .filter(info -> info.getCapabilities().getSupportedJobTypes().contains(jobType.toUpperCase()))
            .collect(Collectors.toList());
    }

    /**
     * Discovers nodes with specific tags.
     */
    public List<NodeDiscoveryInfo> discoverNodesByTags(Set<String> requiredTags) {
        return nodeInfoMap.values().stream()
            .filter(info -> info.getStatus() == NodeStatus.ACTIVE)
            .filter(info -> info.getCapabilities().getTags().containsAll(requiredTags))
            .collect(Collectors.toList());
    }

    /**
     * Gets discovery information for a specific node.
     */
    public Optional<NodeDiscoveryInfo> getNodeInfo(String nodeId) {
        return Optional.ofNullable(nodeInfoMap.get(nodeId));
    }

    /**
     * Gets the number of active nodes.
     */
    public int getActiveNodeCount() {
        return (int) nodeInfoMap.values().stream()
            .filter(info -> info.getStatus() == NodeStatus.ACTIVE)
            .count();
    }

    /**
     * Shutdown the discovery service.
     */
    public void shutdown() {
        logger.info("Shutting down NodeDiscoveryService");
        discoveryExecutor.shutdown();
        try {
            if (!discoveryExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                discoveryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            discoveryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Node discovery information.
     */
    public static class NodeDiscoveryInfo implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String nodeId;
        private final String host;
        private final int port;
        private final NodeCapabilities capabilities;
        private final NodeStatus status;
        private final Instant lastUpdate;

        public NodeDiscoveryInfo(String nodeId, String host, int port,
                                NodeCapabilities capabilities, NodeStatus status,
                                Instant lastUpdate) {
            this.nodeId = nodeId;
            this.host = host;
            this.port = port;
            this.capabilities = capabilities;
            this.status = status;
            this.lastUpdate = lastUpdate;
        }

        public String getNodeId() {
            return nodeId;
        }

        public String getHost() {
            return host;
        }

        public int getPort() {
            return port;
        }

        public NodeCapabilities getCapabilities() {
            return capabilities;
        }

        public NodeStatus getStatus() {
            return status;
        }

        public Instant getLastUpdate() {
            return lastUpdate;
        }

        @Override
        public String toString() {
            return "NodeDiscoveryInfo{" +
                   "nodeId='" + nodeId + '\'' +
                   ", host='" + host + '\'' +
                   ", port=" + port +
                   ", capabilities=" + capabilities +
                   ", status=" + status +
                   ", lastUpdate=" + lastUpdate +
                   '}';
        }
    }

    /**
     * Node capabilities.
     */
    public static class NodeCapabilities implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final int cpuCores;
        private final long maxMemory;
        private final Set<String> supportedJobTypes;
        private final Set<String> tags;
        private final int priorityLevel;

        public NodeCapabilities(int cpuCores, long maxMemory,
                              Set<String> supportedJobTypes,
                              Set<String> tags, int priorityLevel) {
            this.cpuCores = cpuCores;
            this.maxMemory = maxMemory;
            this.supportedJobTypes = new HashSet<>(supportedJobTypes);
            this.tags = new HashSet<>(tags);
            this.priorityLevel = priorityLevel;
        }

        public int getCpuCores() {
            return cpuCores;
        }

        public long getMaxMemory() {
            return maxMemory;
        }

        public Set<String> getSupportedJobTypes() {
            return supportedJobTypes;
        }

        public Set<String> getTags() {
            return tags;
        }

        public int getPriorityLevel() {
            return priorityLevel;
        }

        @Override
        public String toString() {
            return "NodeCapabilities{" +
                   "cpuCores=" + cpuCores +
                   ", maxMemory=" + maxMemory +
                   ", supportedJobTypes=" + supportedJobTypes +
                   ", tags=" + tags +
                   ", priorityLevel=" + priorityLevel +
                   '}';
        }
    }

    /**
     * Node status enumeration.
     */
    public enum NodeStatus {
        ACTIVE,
        BUSY,
        DRAINING,
        OFFLINE
    }
}
