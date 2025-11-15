# Quick Test Guide - Job Scheduler Platform

Quick reference for testing all 70 features locally.

## 🚀 Quick Start

```bash
# 1. Start all services
docker-compose up -d

# 2. Wait for services (30-60 seconds)
docker-compose ps

# 3. Check health
curl http://localhost:8080/actuator/health

# 4. Open Swagger UI
open http://localhost:8080/swagger-ui.html

# 5. Run automated tests
./tests/run-all-tests.sh

# 6. Import sample jobs
./tests/import-sample-jobs.sh

# 7. Open WebSocket test
open tests/test-websocket.html
```

## 📋 Test Checklist (70 Features)

### ✅ Core Features (1-10) - Basic Job Operations

```bash
# Test #1: Create a job
curl -X POST http://localhost:8080/api/v1/jobs \
  -H "Content-Type: application/json" \
  -d '{
    "name": "my-first-job",
    "type": "shell",
    "configuration": {"command": "echo Hello"}
  }'

# Test #2: Execute job
curl -X POST http://localhost:8080/api/v1/executions/execute/{job-id}

# Test #3: Create scheduled job
# See TEST_SCENARIOS.md Scenario 2

# Test #4-6: Check job status, history, lifecycle
curl http://localhost:8080/api/v1/jobs/{job-id}
curl http://localhost:8080/api/v1/executions/job/{job-id}/history

# Test #7: Create jobs with dependencies
# See TEST_SCENARIOS.md Scenario 3

# Test #8: Set priority
# Create job with "priority": "HIGH"

# Test #9: Update job (version increments)
curl -X PUT http://localhost:8080/api/v1/jobs/{job-id}

# Test #10: Use template
curl http://localhost:8080/api/v1/templates
```

### ✅ Execution & Runtime (11-20)

```bash
# Test #11-12: Multi-threaded, parallel execution
# Jobs run automatically using virtual threads

# Test #13-14: Sequential, chaining
curl -X POST http://localhost:8080/api/v1/workflows/execute

# Test #15: Workflow orchestration
# See TEST_SCENARIOS.md Scenario 7

# Test #16: Dynamic parameters
curl -X POST http://localhost:8080/api/v1/executions/execute/{job-id} \
  -d '{"parameters": {"key": "value"}}'

# Test #17: Context passing
# Check execution.context in response

# Test #18: Timeout
# Create job with "timeoutMs": 5000

# Test #19: Retry
# Create job with retryPolicy

# Test #20: Cancellation
curl -X POST http://localhost:8080/api/v1/executions/{exec-id}/cancel
```

### ✅ Monitoring & Observability (21-30)

```bash
# Test #21: Real-time monitoring
open tests/test-websocket.html

# Test #22: Execution history
curl http://localhost:8080/api/v1/executions/job/{job-id}/history

# Test #23-24: Metrics & analytics
curl http://localhost:8080/api/v1/monitoring/metrics/snapshot
curl http://localhost:8080/api/v1/monitoring/performance/summary

# Test #25: Logs
curl http://localhost:8080/api/v1/executions/{exec-id}/logs

# Test #26: Health checks
curl http://localhost:8080/api/v1/health
curl http://localhost:8080/api/v1/health/live
curl http://localhost:8080/api/v1/health/ready

# Test #27: System metrics
curl http://localhost:8080/api/v1/monitoring/metrics/snapshot | jq '.systemMetrics'

# Test #28: Statistics
curl http://localhost:8080/api/v1/admin/stats

# Test #29: Alerts
curl http://localhost:8080/api/v1/monitoring/alerts

# Test #30: Dashboard API
# All monitoring endpoints are dashboard-ready
```

### ✅ Scalability & Distribution (31-40)

```bash
# Test #31: Cluster status
curl http://localhost:8080/api/v1/admin/cluster/status

# Test #32-34: Load balancing, distribution
docker-compose ps  # See 2 nodes
# Create jobs and check nodeId in executions

# Test #35: Leader election
curl http://localhost:8080/api/v1/admin/cluster/leader
# Stop node 1: docker-compose stop job-scheduler-1
# Check new leader: curl http://localhost:8082/api/v1/admin/cluster/leader

# Test #36: Node discovery
curl http://localhost:8080/api/v1/admin/cluster/members

# Test #37: Fault tolerance
# Stop node, jobs rebalance automatically

# Test #38-40: Auto-scaling, resources, queues
# Built-in, observable via cluster status
```

### ✅ Integration & Extensibility (41-50)

```bash
# Test #41: Plugin architecture
# Implemented via SPI, see META-INF/services

# Test #42: Custom job types
# Create jobs with type: shell, http, java, script

# Test #43: REST API
open http://localhost:8080/swagger-ui.html

# Test #44: WebSocket
open tests/test-websocket.html

# Test #45: Event-driven
# Events published automatically

# Test #46: Webhooks
# Use WebhookNotificationProvider

# Test #47: External triggers
curl -X POST http://localhost:8080/api/v1/executions/execute/{job-id}

# Test #48: Multi-database
# PostgreSQL in production, H2 in tests

# Test #49: Kafka integration
# KafkaEventPublisher available

# Test #50: Custom executors
# Implement JobExecutor interface
```

### ✅ Security & Governance (51-60)

```bash
# Test #51: JWT authentication
curl -X POST http://localhost:8080/api/v1/auth/login \
  -d '{"username":"admin","password":"admin"}'

# Test #52: RBAC
# Try operations with different roles

# Test #53: API key management
curl -X POST http://localhost:8080/api/v1/admin/api-keys

# Test #54: Permissions
# Check @RequirePermission annotations

# Test #55: Audit logging
curl http://localhost:8080/api/v1/admin/audit/logs

# Test #56-57: Encryption
# Configured at DB and TLS level

# Test #58: Credential management
# Vault integration ready

# Test #59: Rate limiting
# Configured via Spring properties

# Test #60: IP whitelisting
# Set ipRestrictions on API keys
```

### ✅ Operations & Management (61-70)

```bash
# Test #61: Import/Export
curl http://localhost:8080/api/v1/admin/export/jobs > jobs.json

# Test #62: Configuration
# application.yml, environment variables

# Test #63: Backup/Restore
curl -X POST http://localhost:8080/api/v1/admin/backup > backup.json
curl -X POST http://localhost:8080/api/v1/admin/restore \
  --data-binary @backup.json

# Test #64: Migration
# Export from one env, import to another

# Test #65: Admin API
curl http://localhost:8080/api/v1/admin/stats

# Test #66: Bulk operations
# Import multiple jobs

# Test #67: Scheduling calendar
curl http://localhost:8080/api/v1/schedules

# Test #68: Maintenance mode
# Set node state to MAINTENANCE

# Test #69: Rolling updates
docker-compose up -d --scale job-scheduler=3 --no-recreate

# Test #70: Multi-tenancy
curl -H "X-Tenant-ID: tenant-a" http://localhost:8080/api/v1/jobs
```

## 🧪 Automated Testing

### Run All Tests
```bash
./tests/run-all-tests.sh
```

### Run Specific Scenario
```bash
# See TEST_SCENARIOS.md for 20 detailed scenarios
# Example: Test timeout
curl -X POST http://localhost:8080/api/v1/jobs \
  -d '{"name":"timeout-test","type":"shell","timeoutMs":3000, \
       "configuration":{"command":"sleep 10"}}'
```

## 📊 Monitoring Dashboards

### Prometheus
```bash
open http://localhost:9090
# Query: job_execution_duration_seconds
```

### Grafana
```bash
open http://localhost:3000
# Login: admin/admin
# Add Prometheus data source
# Import job scheduler dashboard
```

### Spring Boot Actuator
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/prometheus
curl http://localhost:8080/actuator/health
```

## 🔍 Verification Checklist

- [ ] All services running (docker-compose ps)
- [ ] Health check passing (curl .../health)
- [ ] Swagger UI accessible
- [ ] Can create jobs
- [ ] Can execute jobs
- [ ] Jobs complete successfully
- [ ] Metrics being collected
- [ ] Cluster with 2 nodes
- [ ] Leader elected
- [ ] Jobs distributed across nodes
- [ ] WebSocket receiving events
- [ ] Grafana showing metrics
- [ ] Backup/restore works
- [ ] Multi-tenancy isolated

## 🐛 Troubleshooting

### Service not starting
```bash
docker-compose logs job-scheduler-1
docker-compose restart job-scheduler-1
```

### Database connection issues
```bash
docker-compose logs postgres
docker-compose exec postgres psql -U scheduler_user -d job_scheduler
```

### Cluster not forming
```bash
# Check Hazelcast logs
docker-compose logs | grep Hazelcast

# Verify network
docker network inspect job-scheduler-network
```

### Jobs not executing
```bash
# Check executor logs
curl http://localhost:8080/actuator/logfile | grep -A 10 "Executing job"

# Check thread pool
curl http://localhost:8080/api/v1/monitoring/metrics/snapshot | jq '.systemMetrics.threadPool'
```

## 📚 Additional Resources

- **Full Test Scenarios**: `TEST_SCENARIOS.md`
- **Sample Jobs**: `tests/sample-jobs.json`
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Architecture**: `ARCHITECTURE.md`
- **Feature List**: `FEATURES.md`

## ✅ Success Criteria

All 70 features tested when you can:

1. ✅ Create, schedule, and execute jobs
2. ✅ See job dependencies working
3. ✅ Monitor jobs in real-time via WebSocket
4. ✅ View metrics in Grafana
5. ✅ Cluster running with 2+ nodes
6. ✅ Jobs distributed across nodes
7. ✅ Leader election works on node failure
8. ✅ Backup and restore successful
9. ✅ Security (JWT, API keys) working
10. ✅ All health checks passing

**Run `./tests/run-all-tests.sh` for automated validation!**
