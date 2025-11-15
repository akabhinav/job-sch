package com.enterprise.scheduler.cluster;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.cp.CPSubsystem;
import com.hazelcast.cp.lock.FencedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Distributed lock service for job execution.
 * Ensures that only one node executes a specific job at a time using Hazelcast FencedLock.
 */
@Service
public class DistributedJobLock {

    private static final Logger logger = LoggerFactory.getLogger(DistributedJobLock.class);
    private static final String LOCK_PREFIX = "job-lock:";

    private final HazelcastInstance hazelcastInstance;
    private final CPSubsystem cpSubsystem;
    private final Map<String, LockInfo> activeLocks;

    @Autowired
    public DistributedJobLock(HazelcastInstance hazelcastInstance) {
        this.hazelcastInstance = hazelcastInstance;
        this.cpSubsystem = hazelcastInstance.getCPSubsystem();
        this.activeLocks = new ConcurrentHashMap<>();
    }

    /**
     * Acquires a distributed lock for a job.
     *
     * @param jobId the job ID to lock
     * @return LockHandle if lock acquired, empty otherwise
     */
    public Optional<LockHandle> acquireLock(String jobId) {
        return acquireLock(jobId, Duration.ZERO);
    }

    /**
     * Acquires a distributed lock for a job with timeout.
     *
     * @param jobId the job ID to lock
     * @param timeout maximum time to wait for the lock
     * @return LockHandle if lock acquired, empty otherwise
     */
    public Optional<LockHandle> acquireLock(String jobId, Duration timeout) {
        String lockName = LOCK_PREFIX + jobId;

        try {
            logger.debug("Attempting to acquire lock for job: {}", jobId);

            FencedLock lock = cpSubsystem.getLock(lockName);

            boolean acquired;
            if (timeout.isZero()) {
                acquired = lock.tryLock();
            } else {
                acquired = lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS);
            }

            if (acquired) {
                LockInfo lockInfo = new LockInfo(jobId, lockName, lock, Instant.now());
                activeLocks.put(jobId, lockInfo);

                logger.info("Acquired lock for job: {} (fence: {})", jobId, lock.getFence());
                return Optional.of(new LockHandle(jobId, this));
            } else {
                logger.debug("Failed to acquire lock for job: {} (already locked)", jobId);
                return Optional.empty();
            }

        } catch (InterruptedException e) {
            logger.warn("Interrupted while acquiring lock for job: {}", jobId);
            Thread.currentThread().interrupt();
            return Optional.empty();
        } catch (Exception e) {
            logger.error("Error acquiring lock for job: {}", jobId, e);
            return Optional.empty();
        }
    }

    /**
     * Releases a distributed lock for a job.
     *
     * @param jobId the job ID to unlock
     */
    public void releaseLock(String jobId) {
        LockInfo lockInfo = activeLocks.remove(jobId);

        if (lockInfo == null) {
            logger.warn("No active lock found for job: {}", jobId);
            return;
        }

        try {
            FencedLock lock = lockInfo.getLock();

            if (lock.isLockedByCurrentThread()) {
                lock.unlock();
                logger.info("Released lock for job: {}", jobId);
            } else {
                logger.warn("Lock for job {} is not held by current thread", jobId);
            }

        } catch (Exception e) {
            logger.error("Error releasing lock for job: {}", jobId, e);
        }
    }

    /**
     * Checks if a job is currently locked.
     *
     * @param jobId the job ID to check
     * @return true if locked, false otherwise
     */
    public boolean isLocked(String jobId) {
        String lockName = LOCK_PREFIX + jobId;

        try {
            FencedLock lock = cpSubsystem.getLock(lockName);
            return lock.isLocked();
        } catch (Exception e) {
            logger.error("Error checking lock status for job: {}", jobId, e);
            return false;
        }
    }

    /**
     * Checks if the current thread holds the lock for a job.
     *
     * @param jobId the job ID to check
     * @return true if current thread holds the lock, false otherwise
     */
    public boolean isLockedByCurrentThread(String jobId) {
        LockInfo lockInfo = activeLocks.get(jobId);
        if (lockInfo == null) {
            return false;
        }

        try {
            return lockInfo.getLock().isLockedByCurrentThread();
        } catch (Exception e) {
            logger.error("Error checking lock ownership for job: {}", jobId, e);
            return false;
        }
    }

    /**
     * Executes a task with a distributed lock.
     *
     * @param jobId the job ID to lock
     * @param task the task to execute
     * @param <T> the return type
     * @return result of the task, or empty if lock could not be acquired
     */
    public <T> Optional<T> executeWithLock(String jobId, Supplier<T> task) {
        return executeWithLock(jobId, task, Duration.ZERO);
    }

    /**
     * Executes a task with a distributed lock and timeout.
     *
     * @param jobId the job ID to lock
     * @param task the task to execute
     * @param timeout maximum time to wait for the lock
     * @param <T> the return type
     * @return result of the task, or empty if lock could not be acquired
     */
    public <T> Optional<T> executeWithLock(String jobId, Supplier<T> task, Duration timeout) {
        Optional<LockHandle> lockHandle = acquireLock(jobId, timeout);

        if (!lockHandle.isPresent()) {
            logger.warn("Could not acquire lock for job: {}", jobId);
            return Optional.empty();
        }

        try {
            T result = task.get();
            return Optional.of(result);
        } catch (Exception e) {
            logger.error("Error executing task with lock for job: {}", jobId, e);
            throw e;
        } finally {
            lockHandle.get().release();
        }
    }

    /**
     * Executes a runnable task with a distributed lock.
     *
     * @param jobId the job ID to lock
     * @param task the task to execute
     * @return true if task was executed, false if lock could not be acquired
     */
    public boolean executeWithLock(String jobId, Runnable task) {
        return executeWithLock(jobId, task, Duration.ZERO);
    }

    /**
     * Executes a runnable task with a distributed lock and timeout.
     *
     * @param jobId the job ID to lock
     * @param task the task to execute
     * @param timeout maximum time to wait for the lock
     * @return true if task was executed, false if lock could not be acquired
     */
    public boolean executeWithLock(String jobId, Runnable task, Duration timeout) {
        Optional<LockHandle> lockHandle = acquireLock(jobId, timeout);

        if (!lockHandle.isPresent()) {
            logger.warn("Could not acquire lock for job: {}", jobId);
            return false;
        }

        try {
            task.run();
            return true;
        } catch (Exception e) {
            logger.error("Error executing task with lock for job: {}", jobId, e);
            throw e;
        } finally {
            lockHandle.get().release();
        }
    }

    /**
     * Forces release of a lock (use with caution).
     *
     * @param jobId the job ID to force unlock
     */
    public void forceRelease(String jobId) {
        String lockName = LOCK_PREFIX + jobId;

        try {
            FencedLock lock = cpSubsystem.getLock(lockName);

            if (lock.isLocked()) {
                lock.unlock();
                activeLocks.remove(jobId);
                logger.warn("Force released lock for job: {}", jobId);
            }

        } catch (Exception e) {
            logger.error("Error force releasing lock for job: {}", jobId, e);
        }
    }

    /**
     * Gets information about an active lock.
     *
     * @param jobId the job ID
     * @return lock information if lock is active
     */
    public Optional<LockInfo> getLockInfo(String jobId) {
        return Optional.ofNullable(activeLocks.get(jobId));
    }

    /**
     * Gets all active locks.
     *
     * @return map of job ID to lock info
     */
    public Map<String, LockInfo> getActiveLocks() {
        return new ConcurrentHashMap<>(activeLocks);
    }

    /**
     * Lock handle returned when a lock is acquired.
     * Provides auto-closeable functionality for use with try-with-resources.
     */
    public static class LockHandle implements AutoCloseable {
        private final String jobId;
        private final DistributedJobLock lockService;
        private volatile boolean released = false;

        private LockHandle(String jobId, DistributedJobLock lockService) {
            this.jobId = jobId;
            this.lockService = lockService;
        }

        /**
         * Releases the lock.
         */
        public void release() {
            if (!released) {
                lockService.releaseLock(jobId);
                released = true;
            }
        }

        /**
         * Gets the job ID for this lock.
         */
        public String getJobId() {
            return jobId;
        }

        /**
         * Checks if the lock has been released.
         */
        public boolean isReleased() {
            return released;
        }

        @Override
        public void close() {
            release();
        }
    }

    /**
     * Information about an active lock.
     */
    public static class LockInfo {
        private final String jobId;
        private final String lockName;
        private final FencedLock lock;
        private final Instant acquiredAt;

        private LockInfo(String jobId, String lockName, FencedLock lock, Instant acquiredAt) {
            this.jobId = jobId;
            this.lockName = lockName;
            this.lock = lock;
            this.acquiredAt = acquiredAt;
        }

        public String getJobId() {
            return jobId;
        }

        public String getLockName() {
            return lockName;
        }

        public FencedLock getLock() {
            return lock;
        }

        public Instant getAcquiredAt() {
            return acquiredAt;
        }

        public long getLockFence() {
            return lock.getFence();
        }

        public Duration getLockDuration() {
            return Duration.between(acquiredAt, Instant.now());
        }

        @Override
        public String toString() {
            return "LockInfo{" +
                   "jobId='" + jobId + '\'' +
                   ", lockName='" + lockName + '\'' +
                   ", acquiredAt=" + acquiredAt +
                   ", fence=" + lock.getFence() +
                   ", duration=" + getLockDuration() +
                   '}';
        }
    }

    /**
     * Lock statistics for monitoring.
     */
    public static class LockStatistics {
        private final int activeLocks;
        private final Map<String, LockInfo> locks;
        private final Instant timestamp;

        public LockStatistics(int activeLocks, Map<String, LockInfo> locks) {
            this.activeLocks = activeLocks;
            this.locks = locks;
            this.timestamp = Instant.now();
        }

        public int getActiveLocks() {
            return activeLocks;
        }

        public Map<String, LockInfo> getLocks() {
            return locks;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }

    /**
     * Gets lock statistics.
     */
    public LockStatistics getStatistics() {
        return new LockStatistics(activeLocks.size(), getActiveLocks());
    }
}
