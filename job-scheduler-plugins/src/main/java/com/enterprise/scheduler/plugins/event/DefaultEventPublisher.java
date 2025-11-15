package com.enterprise.scheduler.plugins.event;

import com.enterprise.scheduler.core.event.JobEvent;
import com.enterprise.scheduler.core.spi.EventPublisher;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Default event publisher implementation
 * Publishes events to registered listeners using in-memory event bus
 * Feature #45: Event-driven Architecture, #41: Plugin Architecture
 */
@Slf4j
public class DefaultEventPublisher implements EventPublisher {

    private final CopyOnWriteArrayList<EventListener> listeners;
    private final ExecutorService asyncExecutor;
    private final boolean enableAsync;

    public DefaultEventPublisher() {
        this.listeners = new CopyOnWriteArrayList<>();
        this.enableAsync = Boolean.parseBoolean(System.getProperty("event.publisher.async.enabled", "true"));
        this.asyncExecutor = enableAsync ? Executors.newFixedThreadPool(
            Integer.parseInt(System.getProperty("event.publisher.async.threads", "4"))
        ) : null;

        log.info("Default event publisher initialized (async: {})", enableAsync);
    }

    @Override
    public void publish(JobEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null event");
            return;
        }

        log.debug("Publishing event: {} for job: {}", event.getType(), event.getJobId());

        // Publish to all listeners
        for (EventListener listener : listeners) {
            try {
                listener.onEvent(event);
            } catch (Exception e) {
                log.error("Error in event listener: {}", listener.getClass().getSimpleName(), e);
            }
        }
    }

    @Override
    public void publish(String channel, JobEvent event) {
        if (event == null) {
            log.warn("Attempted to publish null event to channel: {}", channel);
            return;
        }

        log.debug("Publishing event: {} to channel: {} for job: {}",
            event.getType(), channel, event.getJobId());

        // Add channel to event payload
        event.getPayload().put("channel", channel);

        // Publish to channel-specific listeners
        for (EventListener listener : listeners) {
            if (listener.getChannel() == null || listener.getChannel().equals(channel)) {
                try {
                    listener.onEvent(event);
                } catch (Exception e) {
                    log.error("Error in event listener: {} for channel: {}",
                        listener.getClass().getSimpleName(), channel, e);
                }
            }
        }
    }

    @Override
    public void publishAsync(JobEvent event) {
        if (!enableAsync) {
            log.debug("Async publishing disabled, publishing synchronously");
            publish(event);
            return;
        }

        if (event == null) {
            log.warn("Attempted to publish null event asynchronously");
            return;
        }

        log.debug("Publishing event asynchronously: {} for job: {}", event.getType(), event.getJobId());

        asyncExecutor.submit(() -> {
            try {
                publish(event);
            } catch (Exception e) {
                log.error("Error publishing event asynchronously", e);
            }
        });
    }

    /**
     * Register an event listener
     */
    public void registerListener(EventListener listener) {
        if (listener != null) {
            listeners.add(listener);
            log.info("Registered event listener: {} for channel: {}",
                listener.getClass().getSimpleName(),
                listener.getChannel() != null ? listener.getChannel() : "all");
        }
    }

    /**
     * Unregister an event listener
     */
    public void unregisterListener(EventListener listener) {
        if (listener != null) {
            listeners.remove(listener);
            log.info("Unregistered event listener: {}", listener.getClass().getSimpleName());
        }
    }

    /**
     * Register a simple consumer as listener
     */
    public void registerListener(Consumer<JobEvent> consumer) {
        registerListener(new EventListener() {
            @Override
            public void onEvent(JobEvent event) {
                consumer.accept(event);
            }

            @Override
            public String getChannel() {
                return null;
            }
        });
    }

    /**
     * Register a consumer for specific channel
     */
    public void registerListener(String channel, Consumer<JobEvent> consumer) {
        registerListener(new EventListener() {
            @Override
            public void onEvent(JobEvent event) {
                consumer.accept(event);
            }

            @Override
            public String getChannel() {
                return channel;
            }
        });
    }

    /**
     * Get number of registered listeners
     */
    public int getListenerCount() {
        return listeners.size();
    }

    /**
     * Clear all listeners
     */
    public void clearListeners() {
        listeners.clear();
        log.info("Cleared all event listeners");
    }

    /**
     * Shutdown the event publisher
     */
    public void shutdown() {
        if (asyncExecutor != null) {
            log.info("Shutting down async event publisher");
            asyncExecutor.shutdown();
            try {
                if (!asyncExecutor.awaitTermination(60, TimeUnit.SECONDS)) {
                    asyncExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                asyncExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        clearListeners();
    }

    /**
     * Event listener interface
     */
    public interface EventListener {
        /**
         * Handle event
         */
        void onEvent(JobEvent event);

        /**
         * Get channel filter (null = all channels)
         */
        String getChannel();
    }
}
