# Job Scheduler API Module

This module provides the REST API and WebSocket interface for the Enterprise Job Scheduler platform.

## Features Implemented

### REST API (Feature #43)
- **JobController** - CRUD operations for jobs (Features #1, #61)
  - Create, read, update, delete jobs
  - Enable/disable jobs
  - Check job executability

- **JobExecutionController** - Execution management and history (Features #22, #66)
  - Manual job execution
  - Execution history and logs
  - Running executions monitoring
  - Cancel executions

- **ScheduleController** - Schedule management (Feature #67)
  - Add/update/remove schedules
  - Activate/deactivate schedules

- **WorkflowController** - Workflow orchestration (Feature #15)
  - Sequential workflow execution
  - Parallel workflow execution
  - DAG-based workflows

- **TemplateController** - Job template management (Feature #10)
  - Create, read, update, delete templates
  - Instantiate jobs from templates

- **AdminController** - Admin operations (Feature #65)
  - Backup and restore
  - System statistics
  - Cache management
  - Configuration viewing

- **HealthController** - Health checks (Feature #26)
  - Overall health status
  - Kubernetes liveness/readiness probes
  - Component-specific health checks

### WebSocket Support (Feature #44)
- **JobEventWebSocketHandler** - Real-time job event updates
  - Subscribe to job events
  - Subscribe to event types
  - Broadcast job events to connected clients
  - Session-based filtering

### DTOs
- Request/Response models for all entities
- Validation annotations
- OpenAPI/Swagger documentation

### Configuration
- **OpenApiConfiguration** - Swagger/OpenAPI documentation
- **WebMvcConfiguration** - CORS and Web MVC settings
- **GlobalExceptionHandler** - Centralized exception handling

## API Endpoints

### Jobs
- `POST /api/v1/jobs` - Create job
- `GET /api/v1/jobs/{id}` - Get job by ID
- `GET /api/v1/jobs` - Get all jobs
- `PUT /api/v1/jobs/{id}` - Update job
- `DELETE /api/v1/jobs/{id}` - Delete job
- `POST /api/v1/jobs/{id}/enable` - Enable job
- `POST /api/v1/jobs/{id}/disable` - Disable job
- `GET /api/v1/jobs/{id}/can-execute` - Check if job can execute

### Job Executions
- `POST /api/v1/executions/jobs/{jobId}/execute` - Execute job manually
- `GET /api/v1/executions/{id}` - Get execution by ID
- `GET /api/v1/executions` - Get all executions
- `GET /api/v1/executions/jobs/{jobId}/history` - Get job execution history
- `POST /api/v1/executions/{id}/cancel` - Cancel execution
- `GET /api/v1/executions/{id}/logs` - Get execution logs
- `GET /api/v1/executions/running` - Get running executions

### Schedules
- `POST /api/v1/schedules/jobs/{jobId}` - Add schedule to job
- `GET /api/v1/schedules/jobs/{jobId}` - Get job schedule
- `PUT /api/v1/schedules/jobs/{jobId}` - Update job schedule
- `DELETE /api/v1/schedules/jobs/{jobId}` - Remove job schedule
- `POST /api/v1/schedules/jobs/{jobId}/activate` - Activate schedule
- `POST /api/v1/schedules/jobs/{jobId}/deactivate` - Deactivate schedule

### Workflows
- `POST /api/v1/workflows/execute` - Execute workflow
- `POST /api/v1/workflows/execute/sequential` - Execute sequential workflow
- `POST /api/v1/workflows/execute/parallel` - Execute parallel workflow

### Templates
- `POST /api/v1/templates` - Create template
- `GET /api/v1/templates/{id}` - Get template by ID
- `GET /api/v1/templates` - Get all templates
- `PUT /api/v1/templates/{id}` - Update template
- `DELETE /api/v1/templates/{id}` - Delete template
- `POST /api/v1/templates/{id}/instantiate` - Create job from template

### Admin
- `POST /api/v1/admin/backup` - Create backup
- `POST /api/v1/admin/restore` - Restore from backup
- `GET /api/v1/admin/stats` - Get system statistics
- `POST /api/v1/admin/cache/clear` - Clear cache
- `POST /api/v1/admin/shutdown` - Graceful shutdown
- `GET /api/v1/admin/config` - Get configuration

### Health
- `GET /api/v1/health` - Overall health check
- `GET /api/v1/health/live` - Liveness probe
- `GET /api/v1/health/ready` - Readiness probe
- `GET /api/v1/health/scheduler` - Scheduler health
- `GET /api/v1/health/database` - Database health
- `GET /api/v1/health/cluster` - Cluster health

### WebSocket
- `ws://localhost:8080/ws/job-events` - Job events WebSocket endpoint

## WebSocket Message Format

### Client to Server

#### Subscribe to Job
```json
{
  "action": "subscribe",
  "jobId": "job-123"
}
```

#### Subscribe to Event Type
```json
{
  "action": "subscribe",
  "eventType": "EXECUTION_COMPLETED"
}
```

#### Unsubscribe
```json
{
  "action": "unsubscribe",
  "jobId": "job-123"
}
```

#### Ping
```json
{
  "action": "ping"
}
```

### Server to Client

#### Connection Established
```json
{
  "type": "CONNECTED",
  "sessionId": "session-123",
  "timestamp": 1234567890
}
```

#### Job Event
```json
{
  "type": "JOB_EVENT",
  "eventType": "EXECUTION_STARTED",
  "jobId": "job-123",
  "executionId": "exec-456",
  "tenantId": "tenant-1",
  "timestamp": "2024-01-01T00:00:00Z",
  "payload": {}
}
```

## API Documentation

When the application is running, API documentation is available at:
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`

## Configuration

Main configuration is in `application.yml`:

```yaml
server:
  port: 8080

scheduler:
  execution:
    thread-pool-size: 20
    max-concurrent-jobs: 100

security:
  jwt:
    secret: your-secret-key
    expiration-ms: 3600000
```

## Running the Application

```bash
# Development mode
mvn spring-boot:run

# Production mode
java -jar job-scheduler-api.jar --spring.profiles.active=production

# With custom configuration
java -jar job-scheduler-api.jar --server.port=9090
```

## Environment Variables

- `SERVER_PORT` - Server port (default: 8080)
- `DB_URL` - Database URL
- `DB_USERNAME` - Database username
- `DB_PASSWORD` - Database password
- `JWT_SECRET` - JWT secret key
- `LOG_LEVEL` - Logging level
- `CLUSTER_ENABLED` - Enable clustering (true/false)

## Security

All endpoints are protected by RBAC (Role-Based Access Control). Authentication can be done via:
- JWT tokens (Bearer authentication)
- API keys (X-API-Key header)

## Testing

```bash
# Run all tests
mvn test

# Run integration tests
mvn verify
```

## Features Covered

- Feature #1: Job CRUD
- Feature #10: Job Templates
- Feature #15: Workflow Orchestration
- Feature #22: Job Execution Management
- Feature #26: Health Checks
- Feature #43: REST API Design
- Feature #44: WebSocket Support
- Feature #61: Job Operations
- Feature #65: Admin Operations
- Feature #66: Execution History
- Feature #67: Schedule Management
- Feature #68-70: Multi-tenancy and Security Operations
