package com.enterprise.scheduler.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Strategy interface for distributing jobs across cluster nodes.
 * Supports various load balancing algorithms.
 */
public interface JobDistributionStrategy {

    /**
     * Selects a node to execute the given job.
     *
     * @param job the job to distribute
     * @param availableNodes list of available nodes
     * @return selected node, or empty if no suitable node found
     */
    Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
        JobDistributionContext job,
        List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes
    );

    /**
     * Gets the strategy name.
     */
    String getStrategyName();

    /**
     * Round-robin distribution strategy.
     */
    class RoundRobinStrategy implements JobDistributionStrategy {
        private static final Logger logger = LoggerFactory.getLogger(RoundRobinStrategy.class);
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
                JobDistributionContext job,
                List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes) {

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes for job distribution");
                return Optional.empty();
            }

            int index = Math.abs(counter.getAndIncrement() % availableNodes.size());
            NodeDiscoveryService.NodeDiscoveryInfo selected = availableNodes.get(index);

            logger.debug("RoundRobin selected node: {} for job: {}",
                        selected.getNodeId(), job.getJobId());

            return Optional.of(selected);
        }

        @Override
        public String getStrategyName() {
            return "ROUND_ROBIN";
        }
    }

    /**
     * Random distribution strategy.
     */
    class RandomStrategy implements JobDistributionStrategy {
        private static final Logger logger = LoggerFactory.getLogger(RandomStrategy.class);

        @Override
        public Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
                JobDistributionContext job,
                List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes) {

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes for job distribution");
                return Optional.empty();
            }

            int index = ThreadLocalRandom.current().nextInt(availableNodes.size());
            NodeDiscoveryService.NodeDiscoveryInfo selected = availableNodes.get(index);

            logger.debug("Random selected node: {} for job: {}",
                        selected.getNodeId(), job.getJobId());

            return Optional.of(selected);
        }

        @Override
        public String getStrategyName() {
            return "RANDOM";
        }
    }

    /**
     * Least-loaded distribution strategy.
     * Selects the node with the fewest currently executing jobs.
     */
    class LeastLoadedStrategy implements JobDistributionStrategy {
        private static final Logger logger = LoggerFactory.getLogger(LeastLoadedStrategy.class);
        private final Map<String, Integer> nodeLoadMap;

        public LeastLoadedStrategy(Map<String, Integer> nodeLoadMap) {
            this.nodeLoadMap = nodeLoadMap;
        }

        @Override
        public Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
                JobDistributionContext job,
                List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes) {

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes for job distribution");
                return Optional.empty();
            }

            // Find node with minimum load
            NodeDiscoveryService.NodeDiscoveryInfo selected = availableNodes.stream()
                .min(Comparator.comparingInt(node ->
                    nodeLoadMap.getOrDefault(node.getNodeId(), 0)))
                .orElse(availableNodes.get(0));

            logger.debug("LeastLoaded selected node: {} (load: {}) for job: {}",
                        selected.getNodeId(),
                        nodeLoadMap.getOrDefault(selected.getNodeId(), 0),
                        job.getJobId());

            return Optional.of(selected);
        }

        @Override
        public String getStrategyName() {
            return "LEAST_LOADED";
        }
    }

    /**
     * Priority-based distribution strategy.
     * Selects nodes based on their priority level.
     */
    class PriorityBasedStrategy implements JobDistributionStrategy {
        private static final Logger logger = LoggerFactory.getLogger(PriorityBasedStrategy.class);

        @Override
        public Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
                JobDistributionContext job,
                List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes) {

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes for job distribution");
                return Optional.empty();
            }

            // Group nodes by priority
            Map<Integer, List<NodeDiscoveryService.NodeDiscoveryInfo>> nodesByPriority =
                availableNodes.stream()
                    .collect(Collectors.groupingBy(
                        node -> node.getCapabilities().getPriorityLevel()));

            // Get highest priority
            int maxPriority = nodesByPriority.keySet().stream()
                .max(Integer::compareTo)
                .orElse(0);

            // Select randomly from highest priority nodes
            List<NodeDiscoveryService.NodeDiscoveryInfo> highPriorityNodes =
                nodesByPriority.get(maxPriority);

            int index = ThreadLocalRandom.current().nextInt(highPriorityNodes.size());
            NodeDiscoveryService.NodeDiscoveryInfo selected = highPriorityNodes.get(index);

            logger.debug("PriorityBased selected node: {} (priority: {}) for job: {}",
                        selected.getNodeId(), maxPriority, job.getJobId());

            return Optional.of(selected);
        }

        @Override
        public String getStrategyName() {
            return "PRIORITY_BASED";
        }
    }

    /**
     * Capability-aware distribution strategy.
     * Selects nodes based on CPU and memory capabilities.
     */
    class CapabilityAwareStrategy implements JobDistributionStrategy {
        private static final Logger logger = LoggerFactory.getLogger(CapabilityAwareStrategy.class);

        @Override
        public Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
                JobDistributionContext job,
                List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes) {

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes for job distribution");
                return Optional.empty();
            }

            // Calculate capability score for each node
            NodeDiscoveryService.NodeDiscoveryInfo selected = availableNodes.stream()
                .max(Comparator.comparingDouble(this::calculateCapabilityScore))
                .orElse(availableNodes.get(0));

            logger.debug("CapabilityAware selected node: {} for job: {}",
                        selected.getNodeId(), job.getJobId());

            return Optional.of(selected);
        }

        private double calculateCapabilityScore(NodeDiscoveryService.NodeDiscoveryInfo node) {
            NodeDiscoveryService.NodeCapabilities caps = node.getCapabilities();
            // Weighted score based on CPU cores and memory
            return (caps.getCpuCores() * 1.0) + (caps.getMaxMemory() / 1_000_000_000.0);
        }

        @Override
        public String getStrategyName() {
            return "CAPABILITY_AWARE";
        }
    }

    /**
     * Consistent hashing distribution strategy.
     * Ensures same job types tend to go to same nodes for better cache locality.
     */
    class ConsistentHashingStrategy implements JobDistributionStrategy {
        private static final Logger logger = LoggerFactory.getLogger(ConsistentHashingStrategy.class);
        private static final int VIRTUAL_NODES = 150;

        @Override
        public Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
                JobDistributionContext job,
                List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes) {

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes for job distribution");
                return Optional.empty();
            }

            // Build consistent hash ring
            TreeMap<Integer, NodeDiscoveryService.NodeDiscoveryInfo> ring = new TreeMap<>();

            for (NodeDiscoveryService.NodeDiscoveryInfo node : availableNodes) {
                for (int i = 0; i < VIRTUAL_NODES; i++) {
                    String virtualNodeKey = node.getNodeId() + "#" + i;
                    int hash = virtualNodeKey.hashCode();
                    ring.put(hash, node);
                }
            }

            // Hash the job key
            String jobKey = job.getJobType() + ":" + job.getJobId();
            int jobHash = jobKey.hashCode();

            // Find the first node clockwise from the job hash
            Map.Entry<Integer, NodeDiscoveryService.NodeDiscoveryInfo> entry =
                ring.ceilingEntry(jobHash);

            if (entry == null) {
                entry = ring.firstEntry();
            }

            NodeDiscoveryService.NodeDiscoveryInfo selected = entry.getValue();

            logger.debug("ConsistentHashing selected node: {} for job: {} (hash: {})",
                        selected.getNodeId(), job.getJobId(), jobHash);

            return Optional.of(selected);
        }

        @Override
        public String getStrategyName() {
            return "CONSISTENT_HASHING";
        }
    }

    /**
     * Weighted round-robin distribution strategy.
     * Distributes based on node priorities with weighted distribution.
     */
    class WeightedRoundRobinStrategy implements JobDistributionStrategy {
        private static final Logger logger = LoggerFactory.getLogger(WeightedRoundRobinStrategy.class);
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Optional<NodeDiscoveryService.NodeDiscoveryInfo> selectNode(
                JobDistributionContext job,
                List<NodeDiscoveryService.NodeDiscoveryInfo> availableNodes) {

            if (availableNodes.isEmpty()) {
                logger.warn("No available nodes for job distribution");
                return Optional.empty();
            }

            // Create weighted list (repeat nodes based on priority)
            List<NodeDiscoveryService.NodeDiscoveryInfo> weightedNodes = new ArrayList<>();
            for (NodeDiscoveryService.NodeDiscoveryInfo node : availableNodes) {
                int weight = node.getCapabilities().getPriorityLevel();
                for (int i = 0; i < weight; i++) {
                    weightedNodes.add(node);
                }
            }

            if (weightedNodes.isEmpty()) {
                return Optional.of(availableNodes.get(0));
            }

            int index = Math.abs(counter.getAndIncrement() % weightedNodes.size());
            NodeDiscoveryService.NodeDiscoveryInfo selected = weightedNodes.get(index);

            logger.debug("WeightedRoundRobin selected node: {} for job: {}",
                        selected.getNodeId(), job.getJobId());

            return Optional.of(selected);
        }

        @Override
        public String getStrategyName() {
            return "WEIGHTED_ROUND_ROBIN";
        }
    }

    /**
     * Job distribution context containing job metadata for distribution decisions.
     */
    class JobDistributionContext {
        private final String jobId;
        private final String jobType;
        private final int priority;
        private final Set<String> requiredTags;
        private final Map<String, Object> metadata;

        public JobDistributionContext(String jobId, String jobType, int priority,
                                     Set<String> requiredTags, Map<String, Object> metadata) {
            this.jobId = jobId;
            this.jobType = jobType;
            this.priority = priority;
            this.requiredTags = requiredTags != null ? requiredTags : new HashSet<>();
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }

        public String getJobId() {
            return jobId;
        }

        public String getJobType() {
            return jobType;
        }

        public int getPriority() {
            return priority;
        }

        public Set<String> getRequiredTags() {
            return requiredTags;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }
    }
}
