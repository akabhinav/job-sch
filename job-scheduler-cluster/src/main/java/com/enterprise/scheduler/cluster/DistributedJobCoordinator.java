package com.enterprise.scheduler.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.topic.ITopic;
import com.hazelcast.topic.Message;
import com.hazelcast.topic.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Coordinates job distribution and execution across the cluster.
 * Handles job assignment, monitoring, and rebalancing.
 */
@Service
public class DistributedJobCoordinator implements MessageListener<JobCoordinationMessage> {

    private static final Logger logger = LoggerFactory.getLogger(DistributedJobCoordinator.class);
    private static final String JOB_METADATA_MAP = "job-metadata";
    private static final String JOB_ASSIGNMENT_MAP = "job-assignments";
    private static final String COORDINATION_TOPIC = "job-coordination";

    private final HazelcastInstance hazelcastInstance;
    private final ClusterManager clusterManager;
    private final LeaderElectionService leaderElectionService;
    private final NodeDiscoveryService nodeDiscoveryService;

    @Value("${cluster.job.distribution.strategy:LEAST_LOADED}")
    private String distributionStrategyName;

    private JobDistributionStrategy distributionStrategy;
    private IMap<String, JobMetadata> jobMetadataMap;
    private IMap<String, String> jobAssignmentMap; // jobId -> nodeId
    private ITopic<JobCoordinationMessage> coordinationTopic;
    private final Map<String, Integer> nodeLoadMap = new ConcurrentHashMap<>();
    private final ScheduledExecutorService coordinatorExecutor;
    private volatile boolean initialized = false;

    @Autowired
    public DistributedJobCoordinator(HazelcastInstance hazelcastInstance,
                                    ClusterManager clusterManager,
                                    LeaderElectionService leaderElectionService,
                                    NodeDiscoveryService nodeDiscoveryService) {
        this.hazelcastInstance = hazelcastInstance;
        this.clusterManager = clusterManager;
        this.leaderElectionService = leaderElectionService;
        this.nodeDiscoveryService = nodeDiscoveryService;
        this.coordinatorExecutor = Executors.newScheduledThreadPool(2,
            r -> new Thread(r, "job-coordinator-thread"));
    }

    /**
     * Initializes the distributed job coordinator.
     */
    @PostConstruct
    public void initialize() {
        logger.info("Initializing DistributedJobCoordinator");

        try {
            // Get distributed data structures
            jobMetadataMap = hazelcastInstance.getMap(JOB_METADATA_MAP);
            jobAssignmentMap = hazelcastInstance.getMap(JOB_ASSIGNMENT_MAP);
            coordinationTopic = hazelcastInstance.getReliableTopic(COORDINATION_TOPIC);

            // Subscribe to coordination messages
            coordinationTopic.addMessageListener(this);

            // Initialize distribution strategy
            initializeDistributionStrategy();

            // Schedule periodic tasks (only leader executes these)
            coordinatorExecutor.scheduleAtFixedRate(
                this::rebalanceJobs,
                60,
                60,
                TimeUnit.SECONDS
            );

            coordinatorExecutor.scheduleAtFixedRate(
                this::monitorJobAssignments,
                30,
                30,
                TimeUnit.SECONDS
            );

            initialized = true;
            logger.info("DistributedJobCoordinator initialized with strategy: {}",
                       distributionStrategyName);

        } catch (Exception e) {
            logger.error("Failed to initialize DistributedJobCoordinator", e);
            throw new RuntimeException("DistributedJobCoordinator initialization failed", e);
        }
    }

    /**
     * Initializes the job distribution strategy.
     */
    private void initializeDistributionStrategy() {
        switch (distributionStrategyName.toUpperCase()) {
            case "ROUND_ROBIN":
                distributionStrategy = new JobDistributionStrategy.RoundRobinStrategy();
                break;
            case "RANDOM":
                distributionStrategy = new JobDistributionStrategy.RandomStrategy();
                break;
            case "LEAST_LOADED":
                distributionStrategy = new JobDistributionStrategy.LeastLoadedStrategy(nodeLoadMap);
                break;
            case "PRIORITY_BASED":
                distributionStrategy = new JobDistributionStrategy.PriorityBasedStrategy();
                break;
            case "CAPABILITY_AWARE":
                distributionStrategy = new JobDistributionStrategy.CapabilityAwareStrategy();
                break;
            case "CONSISTENT_HASHING":
                distributionStrategy = new JobDistributionStrategy.ConsistentHashingStrategy();
                break;
            case "WEIGHTED_ROUND_ROBIN":
                distributionStrategy = new JobDistributionStrategy.WeightedRoundRobinStrategy();
                break;
            default:
                logger.warn("Unknown distribution strategy: {}, using LEAST_LOADED",
                           distributionStrategyName);
                distributionStrategy = new JobDistributionStrategy.LeastLoadedStrategy(nodeLoadMap);
        }
    }

    /**
     * Distributes a job to an appropriate node in the cluster.
     */
    public JobAssignmentResult distributeJob(JobDistributionStrategy.JobDistributionContext job) {
        try {
            logger.debug("Distributing job: {} (type: {})", job.getJobId(), job.getJobType());

            // Get available nodes for this job type
            List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes =
                nodeDiscoveryService.discoverNodesForJobType(job.getJobType());

            // Filter by required tags if specified
            if (!job.getRequiredTags().isEmpty()) {
                availableNodes = availableNodes.stream()
                    .filter(node -> node.getCapabilities().getTags()
                                       .containsAll(job.getRequiredTags()))
                    .collect(Collectors.toList());
            }

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes found for job: {} (type: {})",
                           job.getJobId(), job.getJobType());
                return JobAssignmentResult.failure(job.getJobId(),
                    "No available nodes for job type: " + job.getJobType());
            }

            // Use distribution strategy to select a node
            Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectedNode =
                distributionStrategy.selectNode(job, availableNodes);

            if (!selectedNode.isPresent()) {
                return JobAssignmentResult.failure(job.getJobId(),
                    "Distribution strategy failed to select a node");
            }

            String nodeId = selectedNode.get().getNodeId();

            // Store job metadata
            JobMetadata metadata = new JobMetadata(
                job.getJobId(),
                job.getJobType(),
                nodeId,
                JobStatus.ASSIGNED,
                Instant.now(),
                job.getPriority(),
                job.getMetadata()
            );
            jobMetadataMap.put(job.getJobId(), metadata);

            // Store assignment
            jobAssignmentMap.put(job.getJobId(), nodeId);

            // Update node load
            nodeLoadMap.merge(nodeId, 1, Integer::sum);

            // Broadcast assignment
            broadcastJobAssignment(job.getJobId(), nodeId);

            logger.info("Job {} assigned to node {}", job.getJobId(), nodeId);

            return JobAssignmentResult.success(job.getJobId(), nodeId);

        } catch (Exception e) {
            logger.error("Failed to distribute job: {}", job.getJobId(), e);
            return JobAssignmentResult.failure(job.getJobId(), e.getMessage());
        }
    }

    /**
     * Updates job status.
     */
    public void updateJobStatus(String jobId, JobStatus status) {
        JobMetadata metadata = jobMetadataMap.get(jobId);
        if (metadata != null) {
            JobMetadata updated = metadata.withStatus(status);
            jobMetadataMap.put(jobId, updated);

            // Broadcast status update
            broadcastJobStatusUpdate(jobId, status);

            // Update node load if job completed or failed
            if (status == JobStatus.COMPLETED || status == JobStatus.FAILED) {
                String nodeId = jobAssignmentMap.get(jobId);
                if (nodeId != null) {
                    nodeLoadMap.merge(nodeId, -1, (old, decrement) -> Math.max(0, old + decrement));
                }
            }

            logger.debug("Updated job {} status to {}", jobId, status);
        }
    }

    /**
     * Gets job metadata.
     */
    public Optional<JobMetadata> getJobMetadata(String jobId) {
        return Optional.ofNullable(jobMetadataMap.get(jobId));
    }

    /**
     * Gets the node assigned to a job.
     */
    public Optional<String> getAssignedNode(String jobId) {
        return Optional.ofNullable(jobAssignmentMap.get(jobId));
    }

    /**
     * Gets all jobs assigned to a specific node.
     */
    public List<JobMetadata> getJobsForNode(String nodeId) {
        return jobMetadataMap.values().stream()
            .filter(metadata -> nodeId.equals(metadata.getAssignedNodeId()))
            .collect(Collectors.toList());
    }

    /**
     * Rebalances jobs across the cluster (leader only).
     */
    private void rebalanceJobs() {
        leaderElectionService.executeIfLeader(() -> {
            try {
                logger.debug("Checking for job rebalancing");

                // Get cluster nodes
                Set<String> activeNodeIds = clusterManager.getClusterMembers().stream()
                    .map(member -> member.getUuid().toString())
                    .collect(Collectors.toSet());

                // Find jobs assigned to nodes that are no longer in the cluster
                List<JobMetadata> orphanedJobs = jobMetadataMap.values().stream()
                    .filter(metadata -> !activeNodeIds.contains(metadata.getAssignedNodeId()))
                    .filter(metadata -> metadata.getStatus() == JobStatus.ASSIGNED ||
                                       metadata.getStatus() == JobStatus.RUNNING)
                    .collect(Collectors.toList());

                if (!orphanedJobs.isEmpty()) {
                    logger.info("Found {} orphaned jobs, reassigning", orphanedJobs.size());

                    for (JobMetadata orphaned : orphanedJobs) {
                        // Create distribution context
                        JobDistributionStrategy.JobDistributionContext context =
                            new JobDistributionStrategy.JobDistributionContext(
                                orphaned.getJobId(),
                                orphaned.getJobType(),
                                orphaned.getPriority(),
                                Collections.emptySet(),
                                orphaned.getMetadata()
                            );

                        // Redistribute
                        distributeJob(context);
                    }
                }

            } catch (Exception e) {
                logger.error("Error during job rebalancing", e);
            }
        });
    }

    /**
     * Monitors job assignments and handles timeouts (leader only).
     */
    private void monitorJobAssignments() {
        leaderElectionService.executeIfLeader(() -> {
            try {
                Instant threshold = Instant.now().minusSeconds(300); // 5 minutes

                long stuckJobs = jobMetadataMap.values().stream()
                    .filter(metadata -> metadata.getStatus() == JobStatus.ASSIGNED)
                    .filter(metadata -> metadata.getAssignedAt().isBefore(threshold))
                    .count();

                if (stuckJobs > 0) {
                    logger.warn("Found {} stuck jobs in ASSIGNED state", stuckJobs);
                }

            } catch (Exception e) {
                logger.error("Error monitoring job assignments", e);
            }
        });
    }

    /**
     * Broadcasts job assignment to the cluster.
     */
    private void broadcastJobAssignment(String jobId, String nodeId) {
        JobCoordinationMessage message = new JobCoordinationMessage(
            MessageType.JOB_ASSIGNED,
            jobId,
            nodeId,
            null
        );
        coordinationTopic.publish(message);
    }

    /**
     * Broadcasts job status update to the cluster.
     */
    private void broadcastJobStatusUpdate(String jobId, JobStatus status) {
        JobCoordinationMessage message = new JobCoordinationMessage(
            MessageType.JOB_STATUS_UPDATE,
            jobId,
            null,
            status
        );
        coordinationTopic.publish(message);
    }

    /**
     * Handles coordination messages.
     */
    @Override
    public void onMessage(Message<JobCoordinationMessage> message) {
        JobCoordinationMessage msg = message.getMessageObject();
        logger.debug("Received coordination message: {}", msg);

        // Handle messages as needed by individual nodes
        switch (msg.getType()) {
            case JOB_ASSIGNED:
                handleJobAssigned(msg);
                break;
            case JOB_STATUS_UPDATE:
                handleJobStatusUpdate(msg);
                break;
            default:
                logger.debug("Unhandled message type: {}", msg.getType());
        }
    }

    /**
     * Handles job assigned message.
     */
    private void handleJobAssigned(JobCoordinationMessage msg) {
        String localNodeId = clusterManager.getLocalMemberId();
        if (localNodeId.equals(msg.getNodeId())) {
            logger.info("Job {} assigned to this node", msg.getJobId());
            // Node can now execute the job
        }
    }

    /**
     * Handles job status update message.
     */
    private void handleJobStatusUpdate(JobCoordinationMessage msg) {
        logger.debug("Job {} status updated to {}", msg.getJobId(), msg.getStatus());
        // Handle status update as needed
    }

    /**
     * Shutdown the coordinator.
     */
    @PreDestroy
    public void shutdown() {
        logger.info("Shutting down DistributedJobCoordinator");

        coordinatorExecutor.shutdown();
        try {
            if (!coordinatorExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                coordinatorExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            coordinatorExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        initialized = false;
        logger.info("DistributedJobCoordinator shutdown complete");
    }

    /**
     * Job metadata stored in distributed map.
     */
    public static class JobMetadata implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final String jobId;
        private final String jobType;
        private final String assignedNodeId;
        private final JobStatus status;
        private final Instant assignedAt;
        private final int priority;
        private final Map<String, Object> metadata;

        public JobMetadata(String jobId, String jobType, String assignedNodeId,
                          JobStatus status, Instant assignedAt, int priority,
                          Map<String, Object> metadata) {
            this.jobId = jobId;
            this.jobType = jobType;
            this.assignedNodeId = assignedNodeId;
            this.status = status;
            this.assignedAt = assignedAt;
            this.priority = priority;
            this.metadata = metadata;
        }

        public JobMetadata withStatus(JobStatus newStatus) {
            return new JobMetadata(jobId, jobType, assignedNodeId, newStatus,
                                  assignedAt, priority, metadata);
        }

        public String getJobId() { return jobId; }
        public String getJobType() { return jobType; }
        public String getAssignedNodeId() { return assignedNodeId; }
        public JobStatus getStatus() { return status; }
        public Instant getAssignedAt() { return assignedAt; }
        public int getPriority() { return priority; }
        public Map<String, Object> getMetadata() { return metadata; }
    }

    /**
     * Job status enumeration.
     */
    public enum JobStatus {
        ASSIGNED,
        RUNNING,
        COMPLETED,
        FAILED,
        CANCELLED
    }

    /**
     * Job assignment result.
     */
    public static class JobAssignmentResult {
        private final String jobId;
        private final String assignedNodeId;
        private final boolean success;
        private final String errorMessage;

        private JobAssignmentResult(String jobId, String assignedNodeId,
                                   boolean success, String errorMessage) {
            this.jobId = jobId;
            this.assignedNodeId = assignedNodeId;
            this.success = success;
            this.errorMessage = errorMessage;
        }

        public static JobAssignmentResult success(String jobId, String nodeId) {
            return new JobAssignmentResult(jobId, nodeId, true, null);
        }

        public static JobAssignmentResult failure(String jobId, String errorMessage) {
            return new JobAssignmentResult(jobId, null, false, errorMessage);
        }

        public String getJobId() { return jobId; }
        public String getAssignedNodeId() { return assignedNodeId; }
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Message types for coordination.
     */
    public enum MessageType {
        JOB_ASSIGNED,
        JOB_STATUS_UPDATE
    }

    /**
     * Coordination message.
     */
    public static class JobCoordinationMessage implements java.io.Serializable {
        private static final long serialVersionUID = 1L;

        private final MessageType type;
        private final String jobId;
        private final String nodeId;
        private final JobStatus status;

        public JobCoordinationMessage(MessageType type, String jobId,
                                     String nodeId, JobStatus status) {
            this.type = type;
            this.jobId = jobId;
            this.nodeId = nodeId;
            this.status = status;
        }

        public MessageType getType() { return type; }
        public String getJobId() { return jobId; }
        public String getNodeId() { return nodeId; }
        public JobStatus getStatus() { return status; }
    }
}
