package com.enterprise.scheduler.cluster;

import com.hazelcast.cluster.Member;
import com.hazelcast.cluster.MembershipEvent;
import com.hazelcast.cluster.MembershipListener;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.IAtomicLong;
import com.hazelcast.map.IMap;
import com.hazelcast.topic.ITopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Manages cluster lifecycle, membership, and coordination.
 * Handles node joining, leaving, and cluster-wide events.
 */
@Component
public class ClusterManager implements MembershipListener {

    private static final Logger logger = LoggerFactory.getLogger(ClusterManager.class);

    private final HazelcastInstance hazelcastInstance;
    private final Map<String, ClusterEventListener> eventListeners = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;
    private String localMemberId;

    @Autowired
    public ClusterManager(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
    }

    /**
     * Initializes the cluster manager and registers membership listeners.
     */
    @PostConstruct
    public void initialize() {
        if (initialized) {
            logger.warn("ClusterManager already initialized");
            return;
        }

        try {
            // Register membership listener
            hazelcastInstance.getCluster().addMembershipListener(this);

            // Get local member ID
            Member localMember = hazelcastInstance.getCluster().getLocalMember();
            localMemberId = localMember.getUuid().toString();

            logger.info("ClusterManager initialized. Local member: {}, Address: {}",
                       localMemberId, localMember.getAddress());

            // Log cluster information
            logClusterInfo();

            initialized = true;

            // Notify listeners of initialization
            notifyClusterEvent(new ClusterEvent(ClusterEventType.NODE_JOINED, localMemberId));

        } catch (Exception e) {
            logger.error("Failed to initialize ClusterManager", e);
            throw new RuntimeException("ClusterManager initialization failed", e);
        }
    }

    /**
     * Cleanup when shutting down.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down ClusterManager for member: {}", localMemberId);

        try {
            // Notify listeners of shutdown
            notifyClusterEvent(new ClusterEvent(ClusterEventType.NODE_LEAVING, localMemberId));

            // Clear listeners
            eventListeners.clear();

            // Shutdown Hazelcast instance
            if (hazelcastInstance != null && hazelcastInstance.getLifecycleService().isRunning()) {
                hazelcastInstance.getLifecycleService().shutdown();
            }

            initialized = false;
            logger.info("ClusterManager shutdown complete");

        } catch (Exception e) {
            logger.error("Error during ClusterManager shutdown", e);
        }
    }

    /**
     * Gets the local member ID.
     */
    public String getLocalMemberId() {
        return localMemberId;
    }

    /**
     * Gets all cluster members.
     */
    public Set<Member> getClusterMembers() {
        return hazelcastInstance.getCluster().getMembers();
    }

    /**
     * Gets the number of active cluster members.
     */
    public int getClusterSize() {
        return hazelcastInstance.getCluster().getMembers().size();
    }

    /**
     * Checks if the cluster is healthy (has minimum required members).
     */
    public boolean isClusterHealthy(int minMembers) {
        return getClusterSize() >= minMembers;
    }

    /**
     * Gets cluster-wide distributed map.
     */
    public <K, V> IMap<K, V> getDistributedMap(String mapName) {
        return hazelcastInstance.getMap(mapName);
    }

    /**
     * Gets cluster-wide distributed topic.
     */
    public <T> ITopic<T> getDistributedTopic(String topicName) {
        return hazelcastInstance.getReliableTopic(topicName);
    }

    /**
     * Gets atomic long counter.
     */
    public IAtomicLong getAtomicLong(String name) {
        return hazelcastInstance.getCPSubsystem().getAtomicLong(name);
    }

    /**
     * Broadcasts a message to all cluster members.
     */
    public <T> void broadcastMessage(String topicName, T message) {
        try {
            ITopic<T> topic = hazelcastInstance.getReliableTopic(topicName);
            topic.publish(message);
            logger.debug("Broadcast message to topic: {}", topicName);
        } catch (Exception e) {
            logger.error("Failed to broadcast message to topic: {}", topicName, e);
        }
    }

    /**
     * Registers a cluster event listener.
     */
    public void registerEventListener(String listenerId, ClusterEventListener listener) {
        eventListeners.put(listenerId, listener);
        logger.debug("Registered cluster event listener: {}", listenerId);
    }

    /**
     * Unregisters a cluster event listener.
     */
    public void unregisterEventListener(String listenerId) {
        eventListeners.remove(listenerId);
        logger.debug("Unregistered cluster event listener: {}", listenerId);
    }

    /**
     * Gets cluster metadata including all member information.
     */
    public ClusterMetadata getClusterMetadata() {
        Set<Member> members = getClusterMembers();
        Member localMember = hazelcastInstance.getCluster().getLocalMember();

        List<NodeInfo> nodeInfoList = members.stream()
            .map(member -> new NodeInfo(
                member.getUuid().toString(),
                member.getAddress().getHost(),
                member.getAddress().getPort(),
                member.isLiteMember(),
                member.equals(localMember)
            ))
            .collect(Collectors.toList());

        return new ClusterMetadata(
            hazelcastInstance.getConfig().getClusterName(),
            members.size(),
            nodeInfoList,
            Instant.now()
        );
    }

    /**
     * Logs cluster information.
     */
    private void logClusterInfo() {
        Set<Member> members = getClusterMembers();
        logger.info("Cluster '{}' has {} member(s):",
                   hazelcastInstance.getConfig().getClusterName(),
                   members.size());

        for (Member member : members) {
            logger.info("  - Member: {}, Address: {}, Lite: {}",
                       member.getUuid(),
                       member.getAddress(),
                       member.isLiteMember());
        }
    }

    /**
     * Notifies all registered listeners of a cluster event.
     */
    private void notifyClusterEvent(ClusterEvent event) {
        eventListeners.values().forEach(listener -> {
            try {
                listener.onClusterEvent(event);
            } catch (Exception e) {
                logger.error("Error notifying cluster event listener", e);
            }
        });
    }

    // MembershipListener implementation

    @Override
    public void memberAdded(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        logger.info("Member added to cluster: {}, Address: {}",
                   member.getUuid(), member.getAddress());

        logClusterInfo();
        notifyClusterEvent(new ClusterEvent(
            ClusterEventType.NODE_JOINED,
            member.getUuid().toString()
        ));
    }

    @Override
    public void memberRemoved(MembershipEvent membershipEvent) {
        Member member = membershipEvent.getMember();
        logger.warn("Member removed from cluster: {}, Address: {}",
                   member.getUuid(), member.getAddress());

        logClusterInfo();
        notifyClusterEvent(new ClusterEvent(
            ClusterEventType.NODE_LEFT,
            member.getUuid().toString()
        ));
    }

    /**
     * Interface for cluster event listeners.
     */
    public interface ClusterEventListener {
        void onClusterEvent(ClusterEvent event);
    }

    /**
     * Cluster event types.
     */
    public enum ClusterEventType {
        NODE_JOINED,
        NODE_LEFT,
        NODE_LEAVING,
        LEADER_ELECTED,
        LEADER_LOST
    }

    /**
     * Cluster event data.
     */
    public static class ClusterEvent {
        private final ClusterEventType type;
        private final String memberId;
        private final Instant timestamp;
        private final Map<String, Object> metadata;

        public ClusterEvent(ClusterEventType type, String memberId) {
            this(type, memberId, new HashMap<>());
        }

        public ClusterEvent(ClusterEventType type, String memberId, Map<String, Object> metadata) {
            this.type = type;
            this.memberId = memberId;
            this.timestamp = Instant.now();
            this.metadata = metadata;
        }

        public ClusterEventType getType() {
            return type;
        }

        public String getMemberId() {
            return memberId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }

    /**
     * Cluster metadata.
     */
    public static class ClusterMetadata {
        private final String clusterName;
        private final int memberCount;
        private final List<NodeInfo> nodes;
        private final Instant timestamp;

        public ClusterMetadata(String clusterName, int memberCount,
                             List<NodeInfo> nodes, Instant timestamp) {
            this.clusterName = clusterName;
            this.memberCount = memberCount;
            this.nodes = nodes;
            this.timestamp = timestamp;
        }

        public String getClusterName() {
            return clusterName;
        }

        public int getMemberCount() {
            return memberCount;
        }

        public List<NodeInfo> getNodes() {
            return nodes;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Node information.
     */
    public static class NodeInfo {
        private final String nodeId;
        private final String host;
        private final int port;
        private final boolean lite;
        private final boolean local;

        public NodeInfo(String nodeId, String host, int port, boolean lite, boolean local) {
            this.nodeId = nodeId;
            this.host = host;
            this.port = port;
            this.lite = lite;
            this.local = local;
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

        public boolean isLite() {
            return lite;
        }

        public boolean isLocal() {
            return local;
        }
    }
}
