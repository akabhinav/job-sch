package com.enterprise.scheduler.persistence.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Persistence layer configuration
 * Features #5: Job Persistence, #48: Multi-Database Support
 */
@Configuration
@EnableJpaRepositories(basePackages = "com.enterprise.scheduler.persistence.repository")
@EntityScan(basePackages = "com.enterprise.scheduler.persistence.entity")
@EnableTransactionManagement
public class PersistenceConfiguration {
    // Configuration is handled by Spring Boot auto-configuration
    // This class is mainly for explicit component scanning and documentation
}
