# Multi-stage Dockerfile for Job Scheduler Platform
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copy Maven wrapper and pom files
COPY .mvn .mvn
COPY mvnw .
COPY pom.xml .
COPY job-scheduler-common/pom.xml job-scheduler-common/
COPY job-scheduler-core/pom.xml job-scheduler-core/
COPY job-scheduler-persistence/pom.xml job-scheduler-persistence/
COPY job-scheduler-security/pom.xml job-scheduler-security/
COPY job-scheduler-monitoring/pom.xml job-scheduler-monitoring/
COPY job-scheduler-cluster/pom.xml job-scheduler-cluster/
COPY job-scheduler-plugins/pom.xml job-scheduler-plugins/
COPY job-scheduler-api/pom.xml job-scheduler-api/
COPY job-scheduler-tests/pom.xml job-scheduler-tests/

# Download dependencies
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY job-scheduler-common/src job-scheduler-common/src
COPY job-scheduler-core/src job-scheduler-core/src
COPY job-scheduler-persistence/src job-scheduler-persistence/src
COPY job-scheduler-security/src job-scheduler-security/src
COPY job-scheduler-monitoring/src job-scheduler-monitoring/src
COPY job-scheduler-cluster/src job-scheduler-cluster/src
COPY job-scheduler-plugins/src job-scheduler-plugins/src
COPY job-scheduler-api/src job-scheduler-api/src

# Build application
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Add application user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy JAR from builder
COPY --from=builder /app/job-scheduler-api/target/job-scheduler-api-*.jar app.jar

# Change ownership
RUN chown -R appuser:appgroup /app

USER appuser

# Expose ports
EXPOSE 8080 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Run application
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
