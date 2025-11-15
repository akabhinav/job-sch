package com.enterprise.scheduler.monitoring.config;

import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Monitoring configuration
 * Feature #21: Metrics Collection Infrastructure
 */
@Slf4j
@Configuration
@EnableScheduling
public class MonitoringConfiguration {

    /**
     * Customize meter registry with common tags
     */
    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags() {
        return registry -> {
            registry.config()
                    .commonTags(
                            "application", "job-scheduler",
                            "environment", getEnvironment()
                    );
            log.info("Configured meter registry with common tags");
        };
    }

    /**
     * Enable @Timed annotation support
     */
    @Bean
    public TimedAspect timedAspect(MeterRegistry registry) {
        return new TimedAspect(registry);
    }

    /**
     * Get environment from system properties
     */
    private String getEnvironment() {
        return System.getProperty("spring.profiles.active", "default");
    }
}
