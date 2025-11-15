package com.enterprise.scheduler.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Registry for tracking all cluster nodes and their current state.
 * Maintains comprehensive information about each node in the cluster.
 */
@Component
public class NodeRegistry {

    private static final Logger logger = LoggerFactory.getLogger(NodeRegistry.class);
    private static final String NODE_REGISTRY_MAP = "node-registry";
    private static final String NODE_METRICS_MAP = "node-metrics";

    private final HazelcastInstance hazelcastInstance;
    private final ClusterManager clusterManager;
    private final IMap<String, NodeRegistration> nodeRegistryMap;
    private final IMap<String, NodeMetrics> nodeMetricsMap;
    private final Map<String, NodeEventListener> eventListeners;

    @Autowired
    public NodeRegistry(HazelcastInstance hazelcastInstance,
                       ClusterManager clusterManager) {
        this.hazelcastInstance = hazelcastInstance;
        this.clusterManager = clusterManager;
        this.nodeRegistryMap = hazelcastInstance.getMap(NODE_REGISTRY_MAP);
        this.nodeMetricsMap = hazelcastInstance.getMap(NODE_METRICS_MAP);
        this.eventListeners = new ConcurrentHashMap<>();
    }

    /**
     * Initializes the node registry.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing NodeRegistry");

        // Register the local node
        registerLocalNode();

        // Listen to cluster events
        clusterManager.registerEventListener("node-registry", this::handleClusterEvent);

        logger.info("NodeRegistry initialized");
    }

    /**
     * Registers the local node in the registry.
     */
    public void registerLocalNode() {
        String nodeId = clusterManager.getLocalMemberId();

        NodeRegistration registration = new NodeRegistration(
            nodeId,
            getHostName(),
            getNodeRole(),
            NodeState.ACTIVE,
            Instant.now(),
            getNodeMetadata()
        );

        nodeRegistryMap.put(nodeId, registration);
        logger.info("Registered local node in registry: {}", nodeId);

        // Notify listeners
        notifyEventListeners(new NodeRegistryEvent(
            NodeEventType.NODE_REGISTERED,
            nodeId,
            registration
        ));
    }

    /**
     * Updates node state.
     */
    public void updateNodeState(String nodeId, NodeState newState) {
        NodeRegistration current = nodeRegistryMap.get(nodeId);
        if (current != null) {
            NodeRegistration updated = current.withState(newState);
            nodeRegistryMap.put(nodeId, updated);

            logger.info("Updated node {} state to {}", nodeId, newState);

            // Notify listeners
            notifyEventListeners(new NodeRegistryEvent(
                NodeEventType.NODE_STATE_CHANGED,
                nodeId,
                updated
            ));
        }
    }

    /**
     * Updates node metrics.
     */
    public void updateNodeMetrics(String nodeId, NodeMetrics metrics) {
        nodeMetricsMap.put(nodeId, metrics);
        logger.debug("Updated metrics for node: {}", nodeId);
    }

    /**
     * Updates local node metrics.
     */
    public void updateLocalNodeMetrics() {
        String nodeId = clusterManager.getLocalMemberId();
        NodeMetrics metrics = collectLocalMetrics();
        updateNodeMetrics(nodeId, metrics);
    }

    /**
     * Gets node registration information.
     */
    public Optional<NodeRegistration> getNodeRegistration(String nodeId) {
        return Optional.ofNullable(nodeRegistryMap.get(nodeId));
    }

    /**
     * Gets node metrics.
     */
    public Optional<NodeMetrics> getNodeMetrics(String nodeId) {
        return Optional.ofNullable(nodeMetricsMap.get(nodeId));
    }

    /**
     * Gets all registered nodes.
     */
    public List<NodeRegistration> getAllNodes() {
        return new ArrayList<>(nodeRegistryMap.values());
    }

    /**
     * Gets all active nodes.
     */
    public List<NodeRegistration> getActiveNodes() {
        return nodeRegistryMap.values().stream()
            .filter(node -> node.getState() == NodeState.ACTIVE)
            .collect(Collectors.toList());
    }

    /**
     * Gets nodes by role.
     */
    public List<NodeRegistration> getNodesByRole(NodeRole role) {
        return nodeRegistryMap.values().stream()
            .filter(node -> node.getRole() == role)
            .collect(Collectors.toList());
    }

    /**
     * Gets nodes by state.
     */
    public List<NodeRegistration> getNodesByState(NodeState state) {
        return nodeRegistryMap.values().stream()
            .filter(node -> node.getState() == state)
            .collect(Collectors.toList());
    }

    /**
     * Unregisters a node from the registry.
     */
    public void unregisterNode(String nodeId) {
        NodeRegistration registration = nodeRegistryMap.remove(nodeId);
        nodeMetricsMap.remove(nodeId);

        if (registration != null) {
            logger.info("Unregistered node from registry: {}", nodeId);

            // Notify listeners
            notifyEventListeners(new NodeRegistryEvent(
                NodeEventType.NODE_UNREGISTERED,
                nodeId,
                registration
            ));
        }
    }

    /**
     * Gets comprehensive node information.
     */
    public Optional<NodeInfo> getNodeInfo(String nodeId) {
        Optional<NodeRegistration> registration = getNodeRegistration(nodeId);
        Optional<NodeMetrics> metrics = getNodeMetrics(nodeId);

        if (registration.isPresent()) {
            return Optional.of(new NodeInfo(
                registration.get(),
                metrics.orElse(null)
            ));
        }

        return Optional.empty();
    }

    /**
     * Gets all node information.
     */
    public List<NodeInfo> getAllNodeInfo() {
        return nodeRegistryMap.keySet().stream()
            .map(this::getNodeInfo)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());
    }

    /**
     * Registers a node event listener.
     */
    public void registerEventListener(String listenerId, NodeEventListener listener) {
        eventListeners.put(listenerId, listener);
        logger.debug("Registered node event listener: {}", listenerId);
    }

    /**
     * Unregisters a node event listener.
     */
    public void unregisterEventListener(String listenerId) {
        eventListeners.remove(listenerId);
        logger.debug("Unregistered node event listener: {}", listenerId);
    }

    /**
     * Handles cluster events.
     */
    private void handleClusterEvent(ClusterManager.ClusterEvent event) {
        switch (event.getType()) {
            case NODE_LEFT:
                unregisterNode(event.getMemberId());
                break;
            case NODE_JOINED:
                // Node will register itself
                logger.debug("Node joined: {}", event.getMemberId());
                break;
            default:
                // Handle other events as needed
                break;
        }
    }

    /**
     * Notifies all event listeners.
     */
    private void notifyEventListeners(NodeRegistryEvent event) {
        eventListeners.values().forEach(listener -> {
            try {
                listener.onNodeEvent(event);
            } catch (Exception e) {
                logger.error("Error notifying node event listener", e);
            }
        });
    }

    /**
     * Collects metrics from the local node.
     */
    private NodeMetrics collectLocalMetrics() {
        Runtime runtime = Runtime.getRuntime();

        return new NodeMetrics(
            runtime.availableProcessors(),
            runtime.totalMemory(),
            runtime.freeMemory(),
            runtime.maxMemory(),
            getSystemLoad(),
            Thread.activeCount(),
            Instant.now()
        );
    }

    /**
     * Gets hostname of the local node.
     */
    private String getHostName() {
        try {
            return java.net.InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Gets the role of this node.
     */
    private NodeRole getNodeRole() {
        String role = System.getProperty("node.role",
                                        System.getenv().getOrDefault("NODE_ROLE", "WORKER"));
        try {
            return NodeRole.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NodeRole.WORKER;
        }
    }

    /**
     * Gets system load average.
     */
    private double getSystemLoad() {
        try {
            return java.lang.management.ManagementFactory
                .getOperatingSystemMXBean()
                .getSystemLoadAverage();
        } catch (Exception e) {
            return -1.0;
        }
    }

    /**
     * Gets node metadata.
     */
    private Map<String, String> getNodeMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("java.version", System.getProperty("java.version"));
        metadata.put("os.name", System.getProperty("os.name"));
        metadata.put("os.version", System.getProperty("os.version"));
        metadata.put("os.arch", System.getProperty("os.arch"));
        return metadata;
    }

    /**
     * Node registration information.
     */
    public static class NodeRegistration implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String nodeId;
        private final String hostname;
        private final NodeRole role;
        private final NodeState state;
        private final Instant registeredAt;
        private final Map<String, String> metadata;

        public NodeRegistration(String nodeId, String hostname, NodeRole role,
                              NodeState state, Instant registeredAt,
                              Map<String, String> metadata) {
            this.nodeId = nodeId;
            this.hostname = hostname;
            this.role = role;
            this.state = state;
            this.registeredAt = registeredAt;
            this.metadata = metadata;
        }

        public NodeRegistration withState(NodeState newState) {
            return new NodeRegistration(nodeId, hostname, role, newState,
                                       registeredAt, metadata);
        }

        public String getNodeId() { return nodeId; }
        public String getHostname() { return hostname; }
        public NodeRole getRole() { return role; }
        public NodeState getState() { return state; }
        public Instant getRegisteredAt() { return registeredAt; }
        public Map<String, String> getMetadata() { return metadata; }
    }

    /**
     * Node metrics.
     */
    public static class NodeMetrics implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final int cpuCores;
        private final long totalMemory;
        private final long freeMemory;
        private final long maxMemory;
        private final double systemLoad;
        private final int activeThreads;
        private final Instant timestamp;

        public NodeMetrics(int cpuCores, long totalMemory, long freeMemory,
                          long maxMemory, double systemLoad, int activeThreads,
                          Instant timestamp) {
            this.cpuCores = cpuCores;
            this.totalMemory = totalMemory;
            this.freeMemory = freeMemory;
            this.maxMemory = maxMemory;
            this.systemLoad = systemLoad;
            this.activeThreads = activeThreads;
            this.timestamp = timestamp;
        }

        public int getCpuCores() { return cpuCores; }
        public long getTotalMemory() { return totalMemory; }
        public long getFreeMemory() { return freeMemory; }
        public long getMaxMemory() { return maxMemory; }
        public double getSystemLoad() { return systemLoad; }
        public int getActiveThreads() { return activeThreads; }
        public Instant getTimestamp() { return timestamp; }

        public long getUsedMemory() {
            return totalMemory - freeMemory;
        }

        public double getMemoryUtilization() {
            return (double) getUsedMemory() / totalMemory * 100;
        }
    }

    /**
     * Combined node information.
     */
    public static class NodeInfo {
        private final NodeRegistration registration;
        private final NodeMetrics metrics;

        public NodeInfo(NodeRegistration registration, NodeMetrics metrics) {
            this.registration = registration;
            this.metrics = metrics;
        }

        public NodeRegistration getRegistration() { return registration; }
        public NodeMetrics getMetrics() { return metrics; }
    }

    /**
     * Node roles.
     */
    public enum NodeRole {
        MASTER,
        WORKER,
        COORDINATOR,
        HYBRID
    }

    /**
     * Node states.
     */
    public enum NodeState {
        ACTIVE,
        BUSY,
        DRAINING,
        MAINTENANCE,
        OFFLINE
    }

    /**
     * Node event types.
     */
    public enum NodeEventType {
        NODE_REGISTERED,
        NODE_UNREGISTERED,
        NODE_STATE_CHANGED,
        NODE_METRICS_UPDATED
    }

    /**
     * Node registry event.
     */
    public static class NodeRegistryEvent {
        private final NodeEventType type;
        private final String nodeId;
        private final NodeRegistration registration;
        private final Instant timestamp;

        public NodeRegistryEvent(NodeEventType type, String nodeId,
                                NodeRegistration registration) {
            this.type = type;
            this.nodeId = nodeId;
            this.registration = registration;
            this.timestamp = Instant.now();
        }

        public NodeEventType getType() { return type; }
        public String getNodeId() { return nodeId; }
        public NodeRegistration getRegistration() { return registration; }
        public Instant getTimestamp() { return timestamp; }
    }

    /**
     * Node event listener interface.
     */
    public interface NodeEventListener {
        void onNodeEvent(NodeRegistryEvent event);
    }
}
