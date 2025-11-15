# Job Scheduler - Clustering and Distribution Module

This module provides clustering and distribution capabilities for the enterprise job scheduler, enabling horizontal scalability and high availability through distributed coordination.

## Features (Coverage: Features #31-40 - Scalability & Distribution)

### Core Components

1. **Cluster Management** (`ClusterConfiguration`, `ClusterManager`)
   - Hazelcast-based clustering
   - Automatic node discovery (TCP/IP, Multicast)
   - Cluster membership tracking
   - Event-driven cluster notifications
   - Distributed data structures (maps, queues, topics)

2. **Leader Election** (`LeaderElectionService`)
   - Strong consistency using Hazelcast CP Subsystem
   - Automatic leader election and failover
   - Leader heartbeat monitoring
   - Event listeners for leadership changes
   - Safe leader resignation

3. **Node Discovery** (`NodeDiscoveryService`)
   - Automatic node registration
   - Node capability detection (CPU, memory, job types)
   - Tag-based node filtering
   - Priority-based node selection
   - Stale node cleanup

4. **Job Distribution** (`DistributedJobCoordinator`, `JobDistributionStrategy`)
   - Multiple distribution strategies:
     - **Round Robin**: Even distribution across nodes
     - **Random**: Random node selection
     - **Least Loaded**: Send to node with fewest jobs
     - **Priority Based**: Distribute by node priority
     - **Capability Aware**: Match jobs to node capabilities
     - **Consistent Hashing**: Cache locality optimization
     - **Weighted Round Robin**: Priority-weighted distribution
   - Job assignment tracking
   - Automatic job rebalancing
   - Orphaned job detection and recovery

5. **Node Registry** (`NodeRegistry`)
   - Comprehensive node tracking
   - Node role management (MASTER, WORKER, COORDINATOR, HYBRID)
   - Node state tracking (ACTIVE, BUSY, DRAINING, MAINTENANCE, OFFLINE)
   - Node metrics collection
   - Event-driven node updates

6. **Health Monitoring** (`NodeHealthMonitor`)
   - Periodic health checks
   - CPU, memory, disk, and thread monitoring
   - Configurable health thresholds
   - Health issue detection and alerting
   - Cluster-wide health visibility
   - Stale health data detection

7. **Distributed Locks** (`DistributedJobLock`)
   - FencedLock-based distributed locking
   - Job execution synchronization
   - Lock timeout support
   - Auto-closeable lock handles
   - Lock statistics and monitoring
   - Force release capability

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Job Scheduler Cluster                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐      │
│  │   Node 1     │  │   Node 2     │  │   Node 3     │      │
│  │  (Leader)    │  │  (Follower)  │  │  (Follower)  │      │
│  └──────────────┘  └──────────────┘  └──────────────┘      │
│         │                 │                 │                │
│         └─────────────────┴─────────────────┘                │
│                           │                                  │
│              ┌────────────▼────────────┐                    │
│              │   Hazelcast Cluster     │                    │
│              │  - CP Subsystem         │                    │
│              │  - Distributed Maps     │                    │
│              │  - Distributed Locks    │                    │
│              │  - Reliable Topics      │                    │
│              └─────────────────────────┘                    │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

## Configuration

### Cluster Configuration

Configure cluster settings in `application-cluster.properties`:

```properties
# Basic cluster settings
cluster.name=job-scheduler-cluster
cluster.port=5701
cluster.members=node1:5701,node2:5701,node3:5701

# CP Subsystem (for leader election)
cluster.cp-member-count=3

# Discovery
cluster.multicast.enabled=false
cluster.tcp-ip.enabled=true
```

### Node Configuration

```properties
# Node settings
node.role=WORKER
node.job.types=ALL
node.tags=gpu,high-memory
node.priority=5
```

### Distribution Strategy

```properties
# Choose distribution strategy
cluster.job.distribution.strategy=LEAST_LOADED
```

Available strategies:
- `ROUND_ROBIN` - Simple round-robin distribution
- `RANDOM` - Random node selection
- `LEAST_LOADED` - Route to least loaded node
- `PRIORITY_BASED` - Use node priority levels
- `CAPABILITY_AWARE` - Match job to node capabilities
- `CONSISTENT_HASHING` - Consistent hashing for cache locality
- `WEIGHTED_ROUND_ROBIN` - Weighted by node priority

### Health Monitoring

```properties
# Health check configuration
cluster.health.check.interval=30
cluster.health.threshold.cpu=90.0
cluster.health.threshold.memory=90.0
cluster.health.threshold.disk=90.0
```

## Usage Examples

### 1. Job Distribution

```java
@Autowired
private DistributedJobCoordinator coordinator;

// Distribute a job
JobDistributionStrategy.JobDistributionContext context =
    new JobDistributionStrategy.JobDistributionContext(
        "job-123",
        "BATCH",
        5,
        Set.of("gpu"),
        metadata
    );

JobAssignmentResult result = coordinator.distributeJob(context);
if (result.isSuccess()) {
    System.out.println("Job assigned to: " + result.getAssignedNodeId());
}
```

### 2. Leader Election

```java
@Autowired
private LeaderElectionService leaderElection;

// Check if current node is leader
if (leaderElection.isLeader()) {
    // Execute leader-only tasks
    performLeaderTasks();
}

// Execute task only if leader
leaderElection.executeIfLeader(() -> {
    // This only runs on the leader node
    coordinateClusterWideOperation();
});

// Listen for leadership changes
leaderElection.registerLeadershipListener("my-listener", event -> {
    System.out.println("Leadership event: " + event.getType());
});
```

### 3. Distributed Locks

```java
@Autowired
private DistributedJobLock jobLock;

// Using try-with-resources (recommended)
Optional<LockHandle> handle = jobLock.acquireLock("job-123");
if (handle.isPresent()) {
    try (LockHandle lock = handle.get()) {
        // Execute job with exclusive lock
        executeJob();
    } // Lock automatically released
}

// Or use executeWithLock helper
jobLock.executeWithLock("job-123", () -> {
    executeJob();
});
```

### 4. Node Discovery

```java
@Autowired
private NodeDiscoveryService discovery;

// Find nodes that can handle specific job type
List<NodeDiscoveryInfo> nodes = discovery.discoverNodesForJobType("BATCH");

// Find nodes with specific tags
List<NodeDiscoveryInfo> gpuNodes =
    discovery.discoverNodesByTags(Set.of("gpu"));

// Get all active nodes
List<NodeDiscoveryInfo> activeNodes = discovery.discoverNodes();
```

### 5. Health Monitoring

```java
@Autowired
private NodeHealthMonitor healthMonitor;

// Get health status for a node
Optional<NodeHealthStatus> health = healthMonitor.getNodeHealth("node-123");
health.ifPresent(status -> {
    System.out.println("Health: " + status.getOverallHealth());
    System.out.println("CPU: " + status.getCpuUsage() + "%");
    System.out.println("Memory: " + status.getMemoryUsage() + "%");
});

// Get all healthy nodes
List<String> healthyNodes = healthMonitor.getHealthyNodes();

// Listen for health issues
healthMonitor.registerHealthListener("my-listener", event -> {
    System.out.println("Health issues on " + event.getNodeId());
    event.getIssues().forEach(issue ->
        System.out.println("  - " + issue.getDescription())
    );
});
```

### 6. Node Registry

```java
@Autowired
private NodeRegistry registry;

// Get all active nodes
List<NodeRegistration> activeNodes = registry.getActiveNodes();

// Get nodes by role
List<NodeRegistration> workers =
    registry.getNodesByRole(NodeRole.WORKER);

// Get node information with metrics
Optional<NodeInfo> nodeInfo = registry.getNodeInfo("node-123");
nodeInfo.ifPresent(info -> {
    System.out.println("Node: " + info.getRegistration().getHostname());
    if (info.getMetrics() != null) {
        System.out.println("Load: " + info.getMetrics().getSystemLoad());
    }
});
```

## Production Deployment

### Minimum Requirements

- **CP Member Count**: At least 3 nodes for leader election (odd number recommended)
- **Memory**: 2GB+ per node
- **Network**: Low-latency, reliable network between cluster nodes
- **JVM**: Java 11 or higher

### Best Practices

1. **Cluster Sizing**
   - Use odd number of CP members (3, 5, 7)
   - Start with 3-5 nodes, scale as needed
   - Monitor cluster size vs. job throughput

2. **Network Configuration**
   - Use TCP/IP discovery in production
   - Disable multicast for production
   - Configure proper network timeouts
   - Use dedicated network for cluster communication

3. **Health Thresholds**
   - Adjust thresholds based on workload
   - Monitor health alerts and tune accordingly
   - Set up external monitoring (Prometheus, etc.)

4. **Distribution Strategy**
   - Use LEAST_LOADED for balanced workload
   - Use CONSISTENT_HASHING for cache-heavy jobs
   - Use CAPABILITY_AWARE for heterogeneous cluster

5. **Monitoring**
   - Enable Hazelcast Management Center
   - Monitor cluster health metrics
   - Track job distribution patterns
   - Alert on leadership changes

## Testing

The module includes comprehensive test coverage for:

- Cluster formation and membership
- Leader election and failover
- Job distribution strategies
- Distributed lock acquisition and release
- Health monitoring and alerting
- Node discovery and registration

Run tests with:
```bash
mvn test
```

## Troubleshooting

### Split Brain

If cluster splits into multiple groups:
1. Check network connectivity
2. Verify CP member count configuration
3. Review Hazelcast logs for split-brain events
4. Use Hazelcast Management Center to inspect cluster state

### Leader Election Issues

If leader election fails:
1. Ensure CP member count >= 3
2. Check that nodes can communicate on cluster port
3. Verify CP subsystem configuration
4. Check for network partitions

### Job Distribution Problems

If jobs aren't distributing properly:
1. Verify nodes are registered in discovery
2. Check node capabilities match job requirements
3. Review distribution strategy configuration
4. Monitor node health status

## Dependencies

- **Hazelcast 5.3.1**: Distributed computing platform
- **Spring Boot 2.7+**: Application framework
- **SLF4J**: Logging facade

## License

Enterprise License - Internal Use Only
