package com.enterprise.scheduler.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Leader election service using Hazelcast CP Subsystem.
 * Ensures only one node is the leader at any time for coordinating cluster-wide operations.
 */
@Service
public class LeaderElectionService {

    private static final Logger logger = LoggerFactory.getLogger(LeaderElectionService.class);
    private static final String LEADER_LOCK_NAME = "scheduler-leader-lock";

    private final HazelcastInstance hazelcastInstance;
    private final ClusterManager clusterManager;
    private final CPSubsystem cpSubsystem;
    private final ExecutorService leaderElectionExecutor;
    private final ScheduledExecutorService heartbeatExecutor;

    @Value("${cluster.leader.election.enabled:true}")
    private boolean leaderElectionEnabled;

    @Value("${cluster.leader.heartbeat.interval:10}")
    private int heartbeatIntervalSeconds;

    private volatile boolean isLeader = false;
    private volatile Instant becameLeaderAt;
    private FencedLock leaderLock;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<String, Consumer<LeadershipEvent>> leadershipListeners = new ConcurrentHashMap<>();
    private Future<?> leaderElectionFuture;

    @Autowired
    public LeaderElectionService(HazelcastInstance hazelcastInstance,
                                 ClusterManager clusterManager) {
        this.hazelcastInstance = hazelcastInstance;
        this.clusterManager = clusterManager;
        this.cpSubsystem = hazelcastInstance.getCPSubsystem();
        this.leaderElectionExecutor = Executors.newSingleThreadExecutor(
            r -> new Thread(r, "leader-election-thread")
        );
        this.heartbeatExecutor = Executors.newSingleThreadScheduledExecutor(
            r -> new Thread(r, "leader-heartbeat-thread")
        );
    }

    /**
     * Initializes the leader election service.
     */
    @PostConstruct
    public void initialize() {
        if (!leaderElectionEnabled) {
            logger.info("Leader election is disabled");
            return;
        }

        logger.info("Initializing LeaderElectionService");

        try {
            // Get the leader lock from CP Subsystem
            leaderLock = cpSubsystem.getLock(LEADER_LOCK_NAME);

            running.set(true);

            // Start leader election process
            leaderElectionFuture = leaderElectionExecutor.submit(this::runLeaderElection);

            // Start heartbeat monitoring
            heartbeatExecutor.scheduleAtFixedRate(
                this::sendLeaderHeartbeat,
                heartbeatIntervalSeconds,
                heartbeatIntervalSeconds,
                TimeUnit.SECONDS
            );

            logger.info("LeaderElectionService initialized");

        } catch (Exception e) {
            logger.error("Failed to initialize LeaderElectionService", e);
            throw new RuntimeException("LeaderElectionService initialization failed", e);
        }
    }

    /**
     * Main leader election loop.
     */
    private void runLeaderElection() {
        String nodeId = clusterManager.getLocalMemberId();
        logger.info("Starting leader election for node: {}", nodeId);

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                // Try to acquire leadership
                if (!isLeader) {
                    attemptLeadershipAcquisition();
                }

                // Sleep before next attempt if not leader
                if (!isLeader) {
                    Thread.sleep(5000);
                }

            } catch (InterruptedException e) {
                logger.info("Leader election interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.error("Error in leader election", e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        logger.info("Leader election loop terminated for node: {}", nodeId);
    }

    /**
     * Attempts to acquire leadership.
     */
    private void attemptLeadershipAcquisition() {
        String nodeId = clusterManager.getLocalMemberId();

        try {
            logger.debug("Node {} attempting to acquire leadership", nodeId);

            // Try to acquire the lock (blocking call)
            leaderLock.lock();

            // Successfully acquired lock - became leader
            onBecameLeader();

            // Hold the lock while leader (this blocks)
            // The lock will be released on shutdown or if this thread is interrupted

        } catch (Exception e) {
            logger.error("Error acquiring leadership", e);
            onLostLeadership();
        }
    }

    /**
     * Called when this node becomes the leader.
     */
    private void onBecameLeader() {
        String nodeId = clusterManager.getLocalMemberId();
        isLeader = true;
        becameLeaderAt = Instant.now();

        logger.info("Node {} became the LEADER at {}", nodeId, becameLeaderAt);

        // Notify cluster manager
        clusterManager.notifyClusterEvent(
            new ClusterManager.ClusterEvent(
                ClusterManager.ClusterEventType.LEADER_ELECTED,
                nodeId
            )
        );

        // Notify listeners
        notifyLeadershipListeners(new LeadershipEvent(
            LeadershipEventType.BECAME_LEADER,
            nodeId,
            becameLeaderAt
        ));
    }

    /**
     * Called when this node loses leadership.
     */
    private void onLostLeadership() {
        if (!isLeader) {
            return;
        }

        String nodeId = clusterManager.getLocalMemberId();
        isLeader = false;

        logger.warn("Node {} LOST leadership", nodeId);

        // Notify cluster manager
        clusterManager.notifyClusterEvent(
            new ClusterManager.ClusterEvent(
                ClusterManager.ClusterEventType.LEADER_LOST,
                nodeId
            )
        );

        // Notify listeners
        notifyLeadershipListeners(new LeadershipEvent(
            LeadershipEventType.LOST_LEADER,
            nodeId,
            Instant.now()
        ));

        // Release the lock if held
        try {
            if (leaderLock != null && leaderLock.isLockedByCurrentThread()) {
                leaderLock.unlock();
            }
        } catch (Exception e) {
            logger.error("Error releasing leader lock", e);
        }
    }

    /**
     * Sends periodic heartbeat if this node is the leader.
     */
    private void sendLeaderHeartbeat() {
        if (!isLeader) {
            return;
        }

        try {
            String nodeId = clusterManager.getLocalMemberId();

            // Verify we still hold the lock
            if (!leaderLock.isLockedByCurrentThread()) {
                logger.warn("Leader lock lost for node {}", nodeId);
                onLostLeadership();
                return;
            }

            logger.debug("Leader heartbeat from node: {}", nodeId);

            // Notify listeners of heartbeat
            notifyLeadershipListeners(new LeadershipEvent(
                LeadershipEventType.LEADER_HEARTBEAT,
                nodeId,
                Instant.now()
            ));

        } catch (Exception e) {
            logger.error("Error sending leader heartbeat", e);
        }
    }

    /**
     * Checks if this node is currently the leader.
     */
    public boolean isLeader() {
        return isLeader;
    }

    /**
     * Gets the time when this node became leader.
     */
    public Instant getBecameLeaderAt() {
        return becameLeaderAt;
    }

    /**
     * Executes a task only if this node is the leader.
     */
    public void executeIfLeader(Runnable task) {
        if (isLeader) {
            try {
                task.run();
            } catch (Exception e) {
                logger.error("Error executing leader task", e);
            }
        }
    }

    /**
     * Executes a task only if this node is the leader and returns a result.
     */
    public <T> T executeIfLeader(Callable<T> task, T defaultValue) {
        if (isLeader) {
            try {
                return task.call();
            } catch (Exception e) {
                logger.error("Error executing leader task", e);
                return defaultValue;
            }
        }
        return defaultValue;
    }

    /**
     * Registers a leadership event listener.
     */
    public void registerLeadershipListener(String listenerId, Consumer<LeadershipEvent> listener) {
        leadershipListeners.put(listenerId, listener);
        logger.debug("Registered leadership listener: {}", listenerId);
    }

    /**
     * Unregisters a leadership event listener.
     */
    public void unregisterLeadershipListener(String listenerId) {
        leadershipListeners.remove(listenerId);
        logger.debug("Unregistered leadership listener: {}", listenerId);
    }

    /**
     * Notifies all leadership listeners of an event.
     */
    private void notifyLeadershipListeners(LeadershipEvent event) {
        leadershipListeners.values().forEach(listener -> {
            try {
                listener.accept(event);
            } catch (Exception e) {
                logger.error("Error notifying leadership listener", e);
            }
        });
    }

    /**
     * Resigns from leadership (voluntarily).
     */
    public void resignLeadership() {
        if (!isLeader) {
            logger.debug("Not leader, cannot resign");
            return;
        }

        logger.info("Node {} resigning from leadership", clusterManager.getLocalMemberId());
        onLostLeadership();
    }

    /**
     * Shutdown the leader election service.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down LeaderElectionService");

        running.set(false);

        // Resign leadership if we are the leader
        if (isLeader) {
            resignLeadership();
        }

        // Cancel leader election future
        if (leaderElectionFuture != null) {
            leaderElectionFuture.cancel(true);
        }

        // Shutdown executors
        shutdownExecutor(leaderElectionExecutor, "leader-election");
        shutdownExecutor(heartbeatExecutor, "heartbeat");

        // Clear listeners
        leadershipListeners.clear();

        logger.info("LeaderElectionService shutdown complete");
    }

    /**
     * Helper method to shutdown an executor service.
     */
    private void shutdownExecutor(ExecutorService executor, String name) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("{} executor did not terminate gracefully, forcing shutdown", name);
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            logger.warn("{} executor shutdown interrupted", name);
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Leadership event types.
     */
    public enum LeadershipEventType {
        BECAME_LEADER,
        LOST_LEADER,
        LEADER_HEARTBEAT
    }

    /**
     * Leadership event data.
     */
    public static class LeadershipEvent {
        private final LeadershipEventType type;
        private final String nodeId;
        private final Instant timestamp;

        public LeadershipEvent(LeadershipEventType type, String nodeId, Instant timestamp) {
            this.type = type;
            this.nodeId = nodeId;
            this.timestamp = timestamp;
        }

        public LeadershipEventType getType() {
            return type;
        }

        public String getNodeId() {
            return nodeId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return "LeadershipEvent{" +
                   "type=" + type +
                   ", nodeId='" + nodeId + '\'' +
                   ", timestamp=" + timestamp +
                   '}';
        }
    }
}
