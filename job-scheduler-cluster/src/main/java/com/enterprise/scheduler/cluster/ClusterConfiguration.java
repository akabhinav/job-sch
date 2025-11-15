package com.enterprise.scheduler.cluster;

import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration class for Hazelcast cluster setup.
 * Configures cluster networking, discovery, and CP subsystem for leader election.
 */
@Configuration
public class ClusterConfiguration {

    @Value("${cluster.name:job-scheduler-cluster}")
    private String clusterName;

    @Value("${cluster.port:5701}")
    private int clusterPort;

    @Value("${cluster.members:localhost}")
    private String clusterMembers;

    @Value("${cluster.cp-member-count:3}")
    private int cpMemberCount;

    @Value("${cluster.multicast.enabled:false}")
    private boolean multicastEnabled;

    @Value("${cluster.tcp-ip.enabled:true}")
    private boolean tcpIpEnabled;

    /**
     * Creates and configures the Hazelcast instance for the cluster.
     *
     * @return configured HazelcastInstance
     */
    @Bean
    public HazelcastInstance hazelcastInstance() {
        Config config = new Config();
        config.setClusterName(clusterName);

        // Configure instance name
        config.setInstanceName("job-scheduler-node-" + System.currentTimeMillis());

        // Configure network settings
        configureNetwork(config);

        // Configure CP Subsystem for leader election
        configureCPSubsystem(config);

        // Configure distributed data structures
        configureDataStructures(config);

        // Configure management center
        configureManagementCenter(config);

        return Hazelcast.newHazelcastInstance(config);
    }

    /**
     * Configures network settings including discovery mechanisms.
     */
    private void configureNetwork(Config config) {
        NetworkConfig networkConfig = config.getNetworkConfig();
        networkConfig.setPort(clusterPort);
        networkConfig.setPortAutoIncrement(true);

        JoinConfig joinConfig = networkConfig.getJoin();

        // Configure multicast discovery
        MulticastConfig multicastConfig = joinConfig.getMulticastConfig();
        multicastConfig.setEnabled(multicastEnabled);

        // Configure TCP/IP discovery
        TcpIpConfig tcpIpConfig = joinConfig.getTcpIpConfig();
        tcpIpConfig.setEnabled(tcpIpEnabled);

        // Parse and add cluster members
        if (tcpIpEnabled && clusterMembers != null && !clusterMembers.isEmpty()) {
            List<String> members = Arrays.asList(clusterMembers.split(","));
            members.forEach(tcpIpConfig::addMember);
        }

        // Disable AWS and Azure discovery by default
        joinConfig.getAwsConfig().setEnabled(false);
        joinConfig.getAzureConfig().setEnabled(false);
        joinConfig.getGcpConfig().setEnabled(false);
        joinConfig.getKubernetesConfig().setEnabled(false);
    }

    /**
     * Configures CP Subsystem for strong consistency and leader election.
     */
    private void configureCPSubsystem(Config config) {
        CPSubsystemConfig cpConfig = config.getCPSubsystemConfig();
        cpConfig.setCPMemberCount(cpMemberCount);
        cpConfig.setSessionTimeToLiveSeconds(300);
        cpConfig.setSessionHeartbeatIntervalSeconds(5);
        cpConfig.setMissingCPMemberAutoRemovalSeconds(14400); // 4 hours

        // Configure FencedLock for distributed locking
        FencedLockConfig lockConfig = new FencedLockConfig("job-execution-locks");
        lockConfig.setLockAcquireLimit(1);
        cpConfig.addLockConfig(lockConfig);

        // Configure Semaphore for resource limiting
        SemaphoreConfig semaphoreConfig = new SemaphoreConfig("job-semaphore");
        semaphoreConfig.setInitialPermits(100);
        cpConfig.addSemaphoreConfig(semaphoreConfig);
    }

    /**
     * Configures distributed data structures (maps, queues, etc.).
     */
    private void configureDataStructures(Config config) {
        // Configure distributed map for job metadata
        MapConfig jobMapConfig = new MapConfig("job-metadata");
        jobMapConfig.setBackupCount(2);
        jobMapConfig.setAsyncBackupCount(1);
        jobMapConfig.setTimeToLiveSeconds(0); // No expiration
        jobMapConfig.setMaxIdleSeconds(0);

        // Configure eviction policy
        EvictionConfig evictionConfig = new EvictionConfig();
        evictionConfig.setEvictionPolicy(EvictionPolicy.LRU);
        evictionConfig.setMaxSizePolicy(MaxSizePolicy.PER_NODE);
        evictionConfig.setSize(10000);
        jobMapConfig.setEvictionConfig(evictionConfig);

        config.addMapConfig(jobMapConfig);

        // Configure distributed map for node health
        MapConfig nodeHealthConfig = new MapConfig("node-health");
        nodeHealthConfig.setBackupCount(1);
        nodeHealthConfig.setTimeToLiveSeconds(60); // Health data expires after 1 minute
        config.addMapConfig(nodeHealthConfig);

        // Configure distributed queue for job distribution
        QueueConfig queueConfig = new QueueConfig("job-queue");
        queueConfig.setBackupCount(1);
        queueConfig.setMaxSize(10000);
        config.addQueueConfig(queueConfig);

        // Configure reliable topic for cluster events
        ReliableTopicConfig topicConfig = new ReliableTopicConfig("cluster-events");
        topicConfig.setReadBatchSize(10);
        config.addReliableTopicConfig(topicConfig);
    }

    /**
     * Configures Hazelcast Management Center if enabled.
     */
    private void configureManagementCenter(Config config) {
        ManagementCenterConfig mcConfig = config.getManagementCenterConfig();
        // Management center can be configured via environment variables
        String mcUrl = System.getenv("HAZELCAST_MC_URL");
        if (mcUrl != null && !mcUrl.isEmpty()) {
            mcConfig.setScriptingEnabled(true);
        }
    }

    /**
     * Creates a client configuration for remote access to the cluster.
     *
     * @return ClientConfig for Hazelcast client
     */
    @Bean
    public com.hazelcast.client.config.ClientConfig hazelcastClientConfig() {
        com.hazelcast.client.config.ClientConfig clientConfig =
            new com.hazelcast.client.config.ClientConfig();

        clientConfig.setClusterName(clusterName);

        com.hazelcast.client.config.ClientNetworkConfig networkConfig =
            clientConfig.getNetworkConfig();

        // Add cluster members
        if (clusterMembers != null && !clusterMembers.isEmpty()) {
            Arrays.asList(clusterMembers.split(","))
                  .forEach(member -> networkConfig.addAddress(member + ":" + clusterPort));
        }

        // Configure connection retry
        com.hazelcast.client.config.ConnectionRetryConfig retryConfig =
            clientConfig.getConnectionStrategyConfig().getConnectionRetryConfig();
        retryConfig.setClusterConnectTimeoutMillis(30000);
        retryConfig.setInitialBackoffMillis(1000);
        retryConfig.setMaxBackoffMillis(30000);

        return clientConfig;
    }
}
