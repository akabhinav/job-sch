package com.enterprise.scheduler.core.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Executor service configuration
 * Feature #11: Multi-threaded Execution
 */
@Configuration
public class ExecutorConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService executorService() {
        // Use virtual threads from Java 21
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
