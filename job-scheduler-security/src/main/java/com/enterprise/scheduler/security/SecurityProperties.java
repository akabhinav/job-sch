package com.enterprise.scheduler.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Configuration properties for security settings.
 *
 * Feature #51-60: Security Configuration Properties
 */
@Data
@Component
@ConfigurationProperties(prefix = "scheduler.security")
public class SecurityProperties {

    /**
     * JWT configuration
     */
    private JwtProperties jwt = new JwtProperties();

    /**
     * API key configuration
     */
    private ApiKeyProperties apiKey = new ApiKeyProperties();

    /**
     * Audit configuration
     */
    private AuditProperties audit = new AuditProperties();

    /**
     * CORS configuration
     */
    private CorsProperties cors = new CorsProperties();

    /**
     * Multi-tenancy configuration
     */
    private MultiTenancyProperties multiTenancy = new MultiTenancyProperties();

    @Data
    public static class JwtProperties {
        /**
         * JWT secret key (should be overridden in production)
         */
        private String secret = "defaultSecretKeyThatShouldBeChangedInProduction1234567890";

        /**
         * JWT access token expiration (default: 24 hours)
         */
        private Duration expiration = Duration.ofHours(24);

        /**
         * JWT refresh token expiration (default: 7 days)
         */
        private Duration refreshExpiration = Duration.ofDays(7);

        /**
         * JWT issuer
         */
        private String issuer = "job-scheduler-platform";
    }

    @Data
    public static class ApiKeyProperties {
        /**
         * Maximum number of API keys per user
         */
        private int maxKeysPerUser = 10;

        /**
         * Maximum number of API keys per tenant
         */
        private int maxKeysPerTenant = 100;

        /**
         * Default API key expiration (null means no expiration)
         */
        private Duration defaultExpiration = null;

        /**
         * Enable IP address restrictions
         */
        private boolean enableIpRestrictions = true;
    }

    @Data
    public static class AuditProperties {
        /**
         * Enable audit logging
         */
        private boolean enabled = true;

        /**
         * Audit log retention period (default: 90 days)
         */
        private Duration retentionPeriod = Duration.ofDays(90);

        /**
         * Enable async audit logging
         */
        private boolean asyncLogging = true;

        /**
         * Log all requests (can be verbose)
         */
        private boolean logAllRequests = false;
    }

    @Data
    public static class CorsProperties {
        /**
         * Allowed origins (default: all)
         */
        private String[] allowedOrigins = {"*"};

        /**
         * Allowed methods
         */
        private String[] allowedMethods = {"GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"};

        /**
         * Allowed headers
         */
        private String[] allowedHeaders = {"Authorization", "Content-Type", "X-Tenant-ID", "X-API-Key"};

        /**
         * Allow credentials
         */
        private boolean allowCredentials = false;

        /**
         * Max age for preflight requests
         */
        private Duration maxAge = Duration.ofHours(1);
    }

    @Data
    public static class MultiTenancyProperties {
        /**
         * Enable multi-tenancy
         */
        private boolean enabled = true;

        /**
         * Tenant ID header name
         */
        private String tenantHeader = "X-Tenant-ID";

        /**
         * Default tenant ID (for development)
         */
        private String defaultTenantId = null;

        /**
         * Enforce tenant isolation
         */
        private boolean enforceTenantIsolation = true;
    }
}
