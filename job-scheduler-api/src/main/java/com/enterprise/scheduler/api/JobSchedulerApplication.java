package com.enterprise.scheduler.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main application class for Job Scheduler
 * Features #43, #44: REST API and WebSocket Support
 */
@Slf4j
@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableConfigurationProperties
@ComponentScan(basePackages = "com.enterprise.scheduler")
public class JobSchedulerApplication {

    public static void main(String[] args) {
        log.info("Starting Job Scheduler Application...");
        SpringApplication.run(JobSchedulerApplication.class, args);
        log.info("Job Scheduler Application started successfully");
    }
}
