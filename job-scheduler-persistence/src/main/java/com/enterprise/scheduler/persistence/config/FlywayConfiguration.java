package com.enterprise.scheduler.persistence.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Flyway configuration for database migrations
 * Features #5: Job Persistence, #63: Backup and Restore
 */
@Slf4j
@Configuration
public class FlywayConfiguration {

    /**
     * Custom Flyway migration strategy for production
     */
    @Bean
    @Profile("!test")
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Starting Flyway migration...");
            flyway.migrate();
            log.info("Flyway migration completed successfully");
        };
    }

    /**
     * Flyway migration strategy for testing (clean and migrate)
     */
    @Bean
    @Profile("test")
    public FlywayMigrationStrategy flywayTestMigrationStrategy() {
        return flyway -> {
            log.info("Starting Flyway test migration (clean and migrate)...");
            flyway.clean();
            flyway.migrate();
            log.info("Flyway test migration completed successfully");
        };
    }
}
