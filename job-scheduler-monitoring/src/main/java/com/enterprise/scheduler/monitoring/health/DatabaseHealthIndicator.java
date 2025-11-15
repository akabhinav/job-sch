package com.enterprise.scheduler.monitoring.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

/**
 * Health indicator for database connectivity and performance
 * Feature #26: Health Monitoring
 */
@Slf4j
@Component
public class DatabaseHealthIndicator implements HealthIndicator {

    private final DataSource dataSource;
    private static final int CONNECTION_TIMEOUT_MS = 5000;
    private static final int QUERY_TIMEOUT_SECONDS = 3;

    @Autowired(required = false)
    public DatabaseHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        if (dataSource == null) {
            return Health.unknown()
                    .withDetail("reason", "No DataSource configured")
                    .build();
        }

        Map<String, Object> details = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try (Connection connection = dataSource.getConnection()) {
            long connectionTime = System.currentTimeMillis() - startTime;
            details.put("connectionTimeMs", connectionTime);
            details.put("database", connection.getMetaData().getDatabaseProductName());
            details.put("databaseVersion", connection.getMetaData().getDatabaseProductVersion());
            details.put("driverName", connection.getMetaData().getDriverName());
            details.put("driverVersion", connection.getMetaData().getDriverVersion());
            details.put("url", sanitizeUrl(connection.getMetaData().getURL()));

            // Test query execution
            long queryStartTime = System.currentTimeMillis();
            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);

                // Execute a simple query to verify database is responsive
                boolean result = statement.execute("SELECT 1");
                if (result) {
                    try (ResultSet rs = statement.getResultSet()) {
                        rs.next(); // Ensure we can fetch the result
                    }
                }

                long queryTime = System.currentTimeMillis() - queryStartTime;
                details.put("queryTimeMs", queryTime);
                details.put("queryStatus", "SUCCESS");
            }

            // Check connection pool info (if available)
            addConnectionPoolInfo(details);

            // Determine health status based on performance
            Health.Builder healthBuilder = Health.up();

            if (connectionTime > CONNECTION_TIMEOUT_MS) {
                healthBuilder = Health.status("WARNING");
                details.put("warning", "Database connection time is high");
            }

            details.put("status", "Database is accessible and responsive");
            return healthBuilder.withDetails(details).build();

        } catch (Exception e) {
            log.error("Database health check failed", e);
            return Health.down()
                    .withException(e)
                    .withDetail("error", e.getMessage())
                    .build();
        }
    }

    /**
     * Sanitize database URL to remove sensitive information
     */
    private String sanitizeUrl(String url) {
        if (url == null) {
            return "unknown";
        }
        // Remove password and other sensitive query parameters
        return url.replaceAll("password=[^&;]*", "password=***")
                  .replaceAll("pwd=[^&;]*", "pwd=***")
                  .replaceAll("user=[^&;]*", "user=***");
    }

    /**
     * Add connection pool information if available
     */
    private void addConnectionPoolInfo(Map<String, Object> details) {
        try {
            // Try to get HikariCP info
            if (dataSource.getClass().getName().contains("HikariDataSource")) {
                // Use reflection to get HikariCP metrics
                java.lang.reflect.Method getHikariPoolMXBean = dataSource.getClass().getMethod("getHikariPoolMXBean");
                Object poolMXBean = getHikariPoolMXBean.invoke(dataSource);

                if (poolMXBean != null) {
                    int activeConnections = (int) poolMXBean.getClass().getMethod("getActiveConnections").invoke(poolMXBean);
                    int idleConnections = (int) poolMXBean.getClass().getMethod("getIdleConnections").invoke(poolMXBean);
                    int totalConnections = (int) poolMXBean.getClass().getMethod("getTotalConnections").invoke(poolMXBean);
                    int threadsAwaitingConnection = (int) poolMXBean.getClass().getMethod("getThreadsAwaitingConnection").invoke(poolMXBean);

                    details.put("pool.active", activeConnections);
                    details.put("pool.idle", idleConnections);
                    details.put("pool.total", totalConnections);
                    details.put("pool.waiting", threadsAwaitingConnection);
                    details.put("pool.type", "HikariCP");
                }
            }
        } catch (Exception e) {
            log.debug("Could not retrieve connection pool info: {}", e.getMessage());
            // Not critical, just skip
        }
    }
}
