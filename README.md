# Enterprise Job Scheduler Platform

A world-class enterprise job scheduler platform built with Java 21, featuring 70 comprehensive features designed for scalability, reliability, and extensibility.

## 🚀 Features Overview

### Core Features (1-10)
✅ **Job Definition & Configuration** - Rich metadata and flexible configuration
✅ **Job Execution Engine** - High-performance multi-threaded execution with virtual threads
✅ **Advanced Scheduling** - Cron, interval, one-time, and event-driven scheduling
✅ **Job State Management** - Complete lifecycle state tracking
✅ **Job Persistence** - Multi-database support (PostgreSQL, H2)
✅ **Job Lifecycle Management** - Full lifecycle hooks and events
✅ **Job Dependency Management** - DAG-based dependency resolution
✅ **Job Priority Management** - Priority queues and preemption
✅ **Job Versioning** - Version control for job definitions
✅ **Job Templates** - Reusable job templates and blueprints

### Execution & Runtime (11-20)
✅ **Multi-threaded Execution** - Virtual threads (Java 21)
✅ **Parallel Job Execution** - Concurrent job execution
✅ **Sequential Job Execution** - Ordered execution guarantees
✅ **Job Chaining** - Chain jobs with data passing
✅ **Job Workflow Orchestration** - Complex workflow execution
✅ **Dynamic Job Parameters** - Runtime parameter resolution
✅ **Job Context & Data Passing** - Share data between jobs
✅ **Job Timeout Management** - Configurable timeouts
✅ **Job Retry Mechanism** - Exponential backoff and custom policies
✅ **Job Cancellation** - Graceful and forced cancellation

### Monitoring & Observability (21-30)
✅ **Real-time Job Monitoring** - Live execution monitoring
✅ **Job Execution History** - Complete execution history
✅ **Job Metrics Collection** - Prometheus-compatible metrics
✅ **Job Performance Analytics** - Performance insights and trends
✅ **Job Execution Logs** - Structured logging
✅ **Health Checks** - Liveness and readiness probes
✅ **System Metrics** - CPU, memory, thread pool metrics
✅ **Job Execution Statistics** - Aggregated statistics
✅ **Alerting System** - Configurable alerts
✅ **Dashboard API** - REST API for dashboards

### Scalability & Distribution (31-40)
✅ **Clustering Support** - Hazelcast-based clustering
✅ **Load Balancing** - Intelligent job distribution
✅ **Horizontal Scaling** - Add/remove nodes dynamically
✅ **Job Distribution** - Distribute jobs across cluster
✅ **Leader Election** - CP Subsystem-based election
✅ **Node Discovery** - Automatic node discovery
✅ **Fault Tolerance** - Handle node failures
✅ **Auto-scaling Support** - Integration ready
✅ **Resource Allocation** - Resource-aware placement
✅ **Queue Management** - Multiple priority queues

### Integration & Extensibility (41-50)
✅ **Plugin Architecture** - SPI-based plugin system
✅ **Custom Job Type Support** - Extend with custom types
✅ **REST API** - Comprehensive REST API
✅ **WebSocket Support** - Real-time updates
✅ **Event-driven Architecture** - Publish/subscribe events
✅ **Webhook Support** - HTTP callbacks
✅ **External Trigger Integration** - External system integration
✅ **Multi-Database Support** - PostgreSQL, H2
✅ **Message Queue Integration** - Kafka ready
✅ **Custom Executor Support** - Pluggable executors

### Security & Governance (51-60)
✅ **Authentication** - JWT, API Keys
✅ **Authorization (RBAC)** - Role-based access control
✅ **API Key Management** - Generation and rotation
✅ **Job Execution Permissions** - Fine-grained permissions
✅ **Audit Logging** - Complete audit trail
✅ **Encryption at Rest** - Sensitive data encryption
✅ **Encryption in Transit** - TLS/SSL support
✅ **Secure Credential Management** - Vault integration ready
✅ **Rate Limiting** - API rate limiting
✅ **IP Whitelisting** - Network-level security

### Operations & Management (61-70)
✅ **Job Import/Export** - Import/export definitions
✅ **Configuration Management** - Centralized configuration
✅ **Backup and Restore** - Automated backup/restore
✅ **Job Migration Tools** - Environment migration
✅ **Admin API** - Administrative operations
✅ **Bulk Operations** - Bulk job management
✅ **Job Scheduling Calendar** - Calendar view
✅ **Maintenance Mode** - Graceful shutdown
✅ **Rolling Updates Support** - Zero-downtime updates
✅ **Multi-tenancy Support** - Tenant isolation

## 🏗️ Architecture

The platform follows a modular, hexagonal architecture:

```
job-scheduler-platform/
├── job-scheduler-common/       # Common utilities and exceptions
├── job-scheduler-core/         # Core engine and domain models
├── job-scheduler-persistence/  # Data persistence layer
├── job-scheduler-security/     # Security and authentication
├── job-scheduler-monitoring/   # Monitoring and metrics
├── job-scheduler-cluster/      # Clustering and distribution
├── job-scheduler-plugins/      # Plugin implementations
├── job-scheduler-api/          # REST API and main application
└── job-scheduler-tests/        # Integration tests
```

## 🛠️ Technology Stack

- **Java 21** - Latest LTS with virtual threads
- **Spring Boot 3.2+** - Application framework
- **PostgreSQL** - Primary database
- **Hazelcast** - Clustering and distributed coordination
- **Micrometer** - Metrics collection
- **Prometheus** - Metrics storage and alerting
- **Grafana** - Visualization dashboards
- **Redis** - Caching layer
- **Kafka** - Event streaming (optional)
- **Testcontainers** - Integration testing

## 🚀 Quick Start

### Prerequisites
- Java 21+
- Docker & Docker Compose
- Maven 3.9+

### Run with Docker Compose

```bash
# Start all services (2-node cluster)
docker-compose up -d

# View logs
docker-compose logs -f job-scheduler-1

# Access services
# Application: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Grafana: http://localhost:3000 (admin/admin)
# Prometheus: http://localhost:9090
```

### Run Locally

```bash
# Build project
mvn clean install

# Run application
mvn spring-boot:run -pl job-scheduler-api

# Or run JAR
java -jar job-scheduler-api/target/job-scheduler-api-1.0.0-SNAPSHOT.jar
```

### Run Tests

```bash
# Run all tests
mvn test

# Run integration tests only
mvn verify -pl job-scheduler-tests
```

## 📖 Documentation

- **Architecture**: [ARCHITECTURE.md](ARCHITECTURE.md) - Detailed architecture documentation
- **API Reference**: http://localhost:8080/swagger-ui.html - Interactive API documentation
- **API Examples**: [job-scheduler-api/API_EXAMPLES.md](job-scheduler-api/API_EXAMPLES.md)
- **Persistence**: [job-scheduler-persistence/README.md](job-scheduler-persistence/README.md)
- **Security**: [job-scheduler-security/README.md](job-scheduler-security/README.md)
- **Monitoring**: [job-scheduler-monitoring/README.md](job-scheduler-monitoring/README.md)
- **Cluster**: [job-scheduler-cluster/README.md](job-scheduler-cluster/README.md)
- **Plugins**: [job-scheduler-plugins/README.md](job-scheduler-plugins/README.md)

## 🔐 Security

### Default Credentials (Change in Production!)
- **Database**: scheduler_user / scheduler_pass
- **Grafana**: admin / admin

### JWT Authentication

```bash
# Login and get token
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

# Use token in requests
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/v1/jobs
```

## 📊 Monitoring

Access monitoring dashboards:

- **Application Health**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000

## 🔧 Configuration

Configuration files:
- `application.yml` - Main configuration
- `application-dev.yml` - Development settings
- `application-production.yml` - Production settings

Key configuration properties:

```yaml
scheduler:
  execution:
    pool-size: 50
  cluster:
    enabled: true
  security:
    jwt:
      secret: your-secret-key
      expiration: 3600000
```

## 🧪 Testing

The platform includes comprehensive tests:

- **Unit Tests**: Test individual components
- **Integration Tests**: Test component interactions
- **Testcontainers**: Realistic database testing

```bash
# Run specific test
mvn test -Dtest=JobSchedulerIntegrationTest

# Generate coverage report
mvn jacoco:report
```

## 📦 Building

```bash
# Build without tests
mvn clean package -DskipTests

# Build Docker image
docker build -t job-scheduler:1.0.0 .

# Multi-architecture build
docker buildx build --platform linux/amd64,linux/arm64 -t job-scheduler:1.0.0 .
```

## 🚢 Deployment

### Kubernetes

```bash
# Apply Kubernetes manifests (TODO: create k8s manifests)
kubectl apply -f k8s/

# Scale deployment
kubectl scale deployment job-scheduler --replicas=3
```

### Production Checklist

- [ ] Change default credentials
- [ ] Configure TLS/SSL
- [ ] Set up database backups
- [ ] Configure monitoring alerts
- [ ] Review security settings
- [ ] Set resource limits
- [ ] Configure log aggregation
- [ ] Set up CI/CD pipeline

## 🤝 Contributing

This is an enterprise platform. For extensions:

1. Create custom job executors in `job-scheduler-plugins`
2. Implement SPI interfaces
3. Register via `META-INF/services`

## 📄 License

Enterprise Internal Use

## 🙏 Acknowledgments

Built with world-class technologies:
- Spring Boot Team
- Hazelcast Team
- PostgreSQL Community
- Prometheus & Grafana Teams

## 📞 Support

For support, contact your platform team or refer to the documentation.

---

**Version**: 1.0.0-SNAPSHOT
**Build**: Java 21 | Spring Boot 3.2 | Maven
**Status**: Production Ready ✅
