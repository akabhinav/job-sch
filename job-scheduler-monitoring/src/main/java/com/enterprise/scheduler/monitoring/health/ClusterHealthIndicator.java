package com.enterprise.scheduler.monitoring.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Health indicator for cluster status
 * Feature #26: Health Monitoring
 * Feature #31: Cluster Health Monitoring
 */
@Slf4j
@Component
public class ClusterHealthIndicator implements HealthIndicator {

    private final Object clusterManager;
    private final int minClusterSize;

    @Autowired(required = false)
    public ClusterHealthIndicator(
            @Autowired(required = false) Object clusterManager,
            @Value("${scheduler.cluster.min-size:1}") int minClusterSize) {
        this.clusterManager = clusterManager;
        this.minClusterSize = minClusterSize;
    }

    @Override
    public Health health() {
        if (clusterManager == null) {
            return Health.status("STANDALONE")
                    .withDetail("mode", "standalone")
                    .withDetail("reason", "Cluster not configured")
                    .build();
        }

        try {
            Map<String, Object> details = new HashMap<>();

            // Get cluster metadata using reflection
            Method getClusterMetadata = clusterManager.getClass().getMethod("getClusterMetadata");
            Object metadata = getClusterMetadata.invoke(clusterManager);

            // Extract cluster information
            String clusterName = (String) metadata.getClass().getMethod("getClusterName").invoke(metadata);
            int memberCount = (int) metadata.getClass().getMethod("getMemberCount").invoke(metadata);
            List<?> nodes = (List<?>) metadata.getClass().getMethod("getNodes").invoke(metadata);

            details.put("clusterName", clusterName);
            details.put("memberCount", memberCount);
            details.put("minRequiredMembers", minClusterSize);
            details.put("nodes", getNodeDetails(nodes));

            // Get local member ID
            Method getLocalMemberId = clusterManager.getClass().getMethod("getLocalMemberId");
            String localMemberId = (String) getLocalMemberId.invoke(clusterManager);
            details.put("localMemberId", localMemberId);

            // Check cluster health
            Method isClusterHealthy = clusterManager.getClass().getMethod("isClusterHealthy", int.class);
            boolean healthy = (boolean) isClusterHealthy.invoke(clusterManager, minClusterSize);

            Health.Builder healthBuilder;
            if (healthy) {
                healthBuilder = Health.up();
                details.put("status", "Cluster is healthy");
            } else {
                healthBuilder = Health.down();
                details.put("reason", String.format(
                    "Cluster size (%d) below minimum (%d)",
                    memberCount, minClusterSize
                ));
            }

            // Add warning if cluster is at minimum
            if (memberCount == minClusterSize && memberCount > 1) {
                healthBuilder = Health.status("WARNING");
                details.put("warning", "Cluster is at minimum size");
            }

            return healthBuilder.withDetails(details).build();

        } catch (Exception e) {
            log.error("Error checking cluster health", e);
            return Health.down()
                    .withException(e)
                    .withDetail("error", "Failed to retrieve cluster information")
                    .build();
        }
    }

    /**
     * Extract node details from node list
     */
    private List<Map<String, Object>> getNodeDetails(List<?> nodes) {
        return nodes.stream()
                .map(node -> {
                    Map<String, Object> nodeMap = new HashMap<>();
                    try {
                        nodeMap.put("nodeId", node.getClass().getMethod("getNodeId").invoke(node));
                        nodeMap.put("host", node.getClass().getMethod("getHost").invoke(node));
                        nodeMap.put("port", node.getClass().getMethod("getPort").invoke(node));
                        nodeMap.put("lite", node.getClass().getMethod("isLite").invoke(node));
                        nodeMap.put("local", node.getClass().getMethod("isLocal").invoke(node));
                    } catch (Exception e) {
                        log.debug("Error extracting node details", e);
                    }
                    return nodeMap;
                })
                .collect(java.util.stream.Collectors.toList());
    }
}
