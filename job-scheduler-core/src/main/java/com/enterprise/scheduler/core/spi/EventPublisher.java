package com.enterprise.scheduler.core.spi;

import com.enterprise.scheduler.core.event.JobEvent;

/**
 * Event publisher interface for event-driven architecture
 * Feature #45: Event-driven Architecture
 */
public interface EventPublisher {

    /**
     * Publish a job event
     */
    void publish(JobEvent event);

    /**
     * Publish event to specific channel
     */
    void publish(String channel, JobEvent event);

    /**
     * Publish event asynchronously
     */
    void publishAsync(JobEvent event);
}
