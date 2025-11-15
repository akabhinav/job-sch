# Job Scheduler API Examples

This document provides example API calls using curl.

## Authentication

All examples assume JWT authentication. First, obtain a token:

```bash
# Login (if authentication is enabled)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'

# Use the returned token in subsequent requests
export TOKEN="your-jwt-token-here"
```

## Job Management

### Create a Job

```bash
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "daily-backup",
    "description": "Daily database backup job",
    "type": "shell",
    "group": "backup-jobs",
    "priority": "HIGH",
    "configuration": {
      "command": "/scripts/backup.sh",
      "workingDirectory": "/opt/backups"
    },
    "parameters": {
      "retentionDays": 30,
      "compressionEnabled": true
    },
    "schedule": {
      "type": "CRON",
      "cronExpression": "0 0 2 * * ?",
      "timezone": "UTC",
      "misfireStrategy": "FIRE_ONCE"
    },
    "retryPolicy": {
      "maxAttempts": 3,
      "retryDelayMs": 5000,
      "backoffMultiplier": 2.0
    },
    "timeoutMs": 300000,
    "tags": ["backup", "database", "critical"],
    "enabled": true
  }'
```

### Get Job by ID

```bash
curl -X GET http://localhost:8080/api/v1/jobs/job-123 \
  -H "Authorization: Bearer $TOKEN"
```

### List All Jobs

```bash
curl -X GET http://localhost:8080/api/v1/jobs \
  -H "Authorization: Bearer $TOKEN"
```

### Update a Job

```bash
curl -X PUT http://localhost:8080/api/v1/jobs/job-123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "daily-backup",
    "description": "Updated daily database backup job",
    "type": "shell",
    "priority": "CRITICAL",
    "enabled": true
  }'
```

### Enable/Disable Job

```bash
# Enable
curl -X POST http://localhost:8080/api/v1/jobs/job-123/enable \
  -H "Authorization: Bearer $TOKEN"

# Disable
curl -X POST http://localhost:8080/api/v1/jobs/job-123/disable \
  -H "Authorization: Bearer $TOKEN"
```

### Delete a Job

```bash
curl -X DELETE http://localhost:8080/api/v1/jobs/job-123 \
  -H "Authorization: Bearer $TOKEN"
```

## Job Execution

### Execute Job Manually

```bash
curl -X POST http://localhost:8080/api/v1/executions/jobs/job-123/execute \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "parameters": {
      "customParam": "value",
      "override": true
    },
    "triggeredBy": "admin"
  }'
```

### Get Execution by ID

```bash
curl -X GET http://localhost:8080/api/v1/executions/exec-456 \
  -H "Authorization: Bearer $TOKEN"
```

### Get Job Execution History

```bash
curl -X GET http://localhost:8080/api/v1/executions/jobs/job-123/history?limit=50 \
  -H "Authorization: Bearer $TOKEN"
```

### Get Execution Logs

```bash
curl -X GET http://localhost:8080/api/v1/executions/exec-456/logs \
  -H "Authorization: Bearer $TOKEN"
```

### Cancel Execution

```bash
curl -X POST http://localhost:8080/api/v1/executions/exec-456/cancel \
  -H "Authorization: Bearer $TOKEN"
```

### Get Running Executions

```bash
curl -X GET http://localhost:8080/api/v1/executions/running \
  -H "Authorization: Bearer $TOKEN"
```

## Schedule Management

### Add Schedule to Job

```bash
curl -X POST http://localhost:8080/api/v1/schedules/jobs/job-123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "type": "CRON",
    "cronExpression": "0 0 * * * ?",
    "timezone": "America/New_York",
    "startTime": "2024-01-01T00:00:00Z",
    "misfireStrategy": "FIRE_ONCE",
    "active": true
  }'
```

### Get Job Schedule

```bash
curl -X GET http://localhost:8080/api/v1/schedules/jobs/job-123 \
  -H "Authorization: Bearer $TOKEN"
```

### Update Schedule

```bash
curl -X PUT http://localhost:8080/api/v1/schedules/jobs/job-123 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "type": "INTERVAL",
    "intervalMs": 3600000,
    "active": true
  }'
```

### Activate/Deactivate Schedule

```bash
# Activate
curl -X POST http://localhost:8080/api/v1/schedules/jobs/job-123/activate \
  -H "Authorization: Bearer $TOKEN"

# Deactivate
curl -X POST http://localhost:8080/api/v1/schedules/jobs/job-123/deactivate \
  -H "Authorization: Bearer $TOKEN"
```

## Workflow Orchestration

### Execute Sequential Workflow

```bash
curl -X POST http://localhost:8080/api/v1/workflows/execute \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "jobIds": ["job-1", "job-2", "job-3"],
    "mode": "SEQUENTIAL",
    "parameters": {
      "workflowName": "data-pipeline",
      "environment": "production"
    }
  }'
```

### Execute Parallel Workflow

```bash
curl -X POST http://localhost:8080/api/v1/workflows/execute/parallel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '["job-1", "job-2", "job-3"]'
```

## Template Management

### Create Template

```bash
curl -X POST http://localhost:8080/api/v1/templates \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "name": "backup-template",
    "description": "Template for backup jobs",
    "type": "shell",
    "category": "backup",
    "defaultConfiguration": {
      "command": "/scripts/backup.sh",
      "workingDirectory": "/opt/backups"
    },
    "defaultParameters": {
      "retentionDays": 30,
      "compressionEnabled": true
    },
    "variables": ["targetDatabase", "backupPath"],
    "tags": ["backup", "template"]
  }'
```

### Get Template

```bash
curl -X GET http://localhost:8080/api/v1/templates/template-123 \
  -H "Authorization: Bearer $TOKEN"
```

### List All Templates

```bash
curl -X GET http://localhost:8080/api/v1/templates \
  -H "Authorization: Bearer $TOKEN"
```

### Create Job from Template

```bash
curl -X POST "http://localhost:8080/api/v1/templates/template-123/instantiate?jobName=my-backup-job" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "targetDatabase": "production-db",
    "backupPath": "/backups/prod"
  }'
```

## Admin Operations

### Create Backup

```bash
curl -X POST http://localhost:8080/api/v1/admin/backup \
  -H "Authorization: Bearer $TOKEN"
```

### Restore Backup

```bash
curl -X POST "http://localhost:8080/api/v1/admin/restore?backupId=backup-123" \
  -H "Authorization: Bearer $TOKEN"
```

### Get System Statistics

```bash
curl -X GET http://localhost:8080/api/v1/admin/stats \
  -H "Authorization: Bearer $TOKEN"
```

### Clear Cache

```bash
curl -X POST http://localhost:8080/api/v1/admin/cache/clear \
  -H "Authorization: Bearer $TOKEN"
```

### Get Configuration

```bash
curl -X GET http://localhost:8080/api/v1/admin/config \
  -H "Authorization: Bearer $TOKEN"
```

## Health Checks

### Overall Health

```bash
curl -X GET http://localhost:8080/api/v1/health
```

### Liveness Probe

```bash
curl -X GET http://localhost:8080/api/v1/health/live
```

### Readiness Probe

```bash
curl -X GET http://localhost:8080/api/v1/health/ready
```

### Component Health

```bash
# Scheduler health
curl -X GET http://localhost:8080/api/v1/health/scheduler

# Database health
curl -X GET http://localhost:8080/api/v1/health/database

# Cluster health
curl -X GET http://localhost:8080/api/v1/health/cluster
```

## WebSocket Connection

### Connect to Job Events WebSocket

```javascript
// Using JavaScript WebSocket API
const ws = new WebSocket('ws://localhost:8080/ws/job-events');

ws.onopen = () => {
  console.log('Connected to job events');

  // Subscribe to specific job
  ws.send(JSON.stringify({
    action: 'subscribe',
    jobId: 'job-123'
  }));

  // Subscribe to event type
  ws.send(JSON.stringify({
    action: 'subscribe',
    eventType: 'EXECUTION_COMPLETED'
  }));
};

ws.onmessage = (event) => {
  const message = JSON.parse(event.data);
  console.log('Received:', message);
};

ws.onclose = () => {
  console.log('Disconnected');
};

ws.onerror = (error) => {
  console.error('WebSocket error:', error);
};
```

### Using wscat (CLI WebSocket Client)

```bash
# Install wscat
npm install -g wscat

# Connect
wscat -c ws://localhost:8080/ws/job-events

# Subscribe to job
> {"action": "subscribe", "jobId": "job-123"}

# Subscribe to event type
> {"action": "subscribe", "eventType": "EXECUTION_STARTED"}

# Ping
> {"action": "ping"}
```

## Actuator Endpoints

```bash
# Health check
curl http://localhost:8080/actuator/health

# Metrics
curl http://localhost:8080/actuator/metrics

# Prometheus metrics
curl http://localhost:8080/actuator/prometheus

# Environment info
curl http://localhost:8080/actuator/env \
  -H "Authorization: Bearer $TOKEN"

# Configuration properties
curl http://localhost:8080/actuator/configprops \
  -H "Authorization: Bearer $TOKEN"
```

## OpenAPI/Swagger UI

Access the interactive API documentation:

```bash
# Swagger UI
open http://localhost:8080/swagger-ui.html

# OpenAPI JSON
curl http://localhost:8080/api-docs
```

## Bulk Operations

### Create Multiple Jobs

```bash
for i in {1..5}; do
  curl -X POST http://localhost:8080/api/v1/jobs \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $TOKEN" \
    -d "{
      \"name\": \"job-$i\",
      \"description\": \"Test job $i\",
      \"type\": \"shell\",
      \"configuration\": {\"command\": \"echo Hello $i\"},
      \"enabled\": true
    }"
done
```

### Execute Multiple Jobs

```bash
JOB_IDS=("job-1" "job-2" "job-3")
for job_id in "${JOB_IDS[@]}"; do
  curl -X POST "http://localhost:8080/api/v1/executions/jobs/$job_id/execute" \
    -H "Authorization: Bearer $TOKEN"
done
```

## Error Handling

All API responses include proper error messages:

```bash
# Example error response
{
  "status": "error",
  "message": "Job not found: job-999",
  "timestamp": "2024-01-01T00:00:00Z",
  "requestId": "req-123456"
}
```

## Rate Limiting

The API includes rate limiting (configurable):

```bash
# If rate limit is exceeded
HTTP/1.1 429 Too Many Requests
{
  "status": "error",
  "message": "Rate limit exceeded. Try again later.",
  "timestamp": "2024-01-01T00:00:00Z"
}
```
