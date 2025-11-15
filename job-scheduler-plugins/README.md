# Job Scheduler Plugins

This module contains plugin implementations for the Job Scheduler platform, providing extensibility through the Service Provider Interface (SPI) pattern.

## Overview

The plugin system enables custom job executors, notification providers, and event publishers to be seamlessly integrated into the job scheduler platform. All plugins are automatically discovered and loaded via Java's ServiceLoader mechanism.

**Features Covered**: #41 (Plugin Architecture), #42 (Custom Job Type Support), #45 (Event-driven Architecture), #46 (Webhook Support), #50 (Custom Executor Support)

## Job Executor Plugins

Job executors implement the `JobExecutor` SPI to handle different types of job execution.

### ShellJobExecutor

Executes shell commands on the host system.

**Type**: `shell`

**Configuration**:
```java
{
  "command": "echo 'Hello World'",           // Required: Shell command to execute
  "workingDirectory": "/path/to/dir",        // Optional: Working directory
  "environment": {                            // Optional: Environment variables
    "ENV_VAR": "value"
  }
}
```

**Example Job**:
```json
{
  "name": "backup-job",
  "type": "shell",
  "configuration": {
    "command": "tar -czf backup.tar.gz /data",
    "workingDirectory": "/backups"
  }
}
```

**Features**:
- Cross-platform support (Windows cmd.exe, Unix sh)
- Environment variable injection
- Output capture
- Timeout support
- Cancellation support

---

### HttpJobExecutor

Makes HTTP/HTTPS requests.

**Type**: `http`

**Configuration**:
```java
{
  "url": "https://api.example.com/endpoint", // Required: Target URL
  "method": "POST",                           // Optional: HTTP method (default: GET)
  "headers": {                                // Optional: HTTP headers
    "Authorization": "Bearer token",
    "Content-Type": "application/json"
  },
  "body": "{\"key\":\"value\"}",             // Optional: Request body
  "connectTimeout": 5000,                     // Optional: Connect timeout in ms (default: 5000)
  "readTimeout": 30000,                       // Optional: Read timeout in ms (default: 30000)
  "followRedirects": true                     // Optional: Follow redirects (default: true)
}
```

**Example Job**:
```json
{
  "name": "api-trigger",
  "type": "http",
  "configuration": {
    "url": "https://api.example.com/process",
    "method": "POST",
    "headers": {
      "Authorization": "Bearer ${token}"
    },
    "body": "{\"action\":\"process\"}"
  }
}
```

**Features**:
- All HTTP methods (GET, POST, PUT, DELETE, PATCH, etc.)
- Custom headers support
- Request body support
- Response capture
- Timeout configuration
- Redirect handling

---

### JavaJobExecutor

Executes Java classes/methods dynamically.

**Type**: `java`

**Configuration**:
```java
{
  "class": "com.example.MyJobClass",         // Required: Fully qualified class name
  "method": "execute",                        // Optional: Method name (default: "execute")
  "arguments": []                             // Optional: Method arguments
}
```

**Method Signatures Supported**:
```java
// Option 1: JobExecution parameter
public Object execute(JobExecution execution) { }

// Option 2: Map parameter (receives job parameters)
public Object execute(Map<String, Object> params) { }

// Option 3: No arguments
public Object execute() { }
```

**Example Job**:
```json
{
  "name": "data-processor",
  "type": "java",
  "configuration": {
    "class": "com.example.DataProcessor",
    "method": "processData"
  }
}
```

**Features**:
- Dynamic class loading
- Multiple method signature support
- Return value capture
- Timeout support
- Cancellation support (via thread interruption)

---

### ScriptJobExecutor

Executes scripts in various languages (Python, JavaScript, Ruby, etc.).

**Type**: `script`

**Configuration**:
```java
{
  "scriptType": "python",                    // Required: Script language
  "script": "print('Hello')",                // Required if scriptFile not provided
  "scriptFile": "/path/to/script.py",       // Required if script not provided
  "interpreter": "python3",                  // Optional: Custom interpreter path
  "arguments": ["arg1", "arg2"],            // Optional: Script arguments
  "workingDirectory": "/path/to/dir",       // Optional: Working directory
  "environment": {                           // Optional: Environment variables
    "VAR": "value"
  }
}
```

**Supported Script Types**:
- `python`, `python2`, `python3` - Python scripts (.py)
- `node`, `javascript`, `js` - JavaScript/Node.js scripts (.js)
- `ruby` - Ruby scripts (.rb)
- `perl` - Perl scripts (.pl)
- `php` - PHP scripts (.php)
- `groovy` - Groovy scripts (.groovy)
- `bash`, `sh` - Bash/Shell scripts (.sh)

**Example Job**:
```json
{
  "name": "python-analysis",
  "type": "script",
  "configuration": {
    "scriptType": "python",
    "script": "import sys\nprint('Processing data...')\nprint('Complete!')",
    "environment": {
      "DATA_PATH": "/data"
    }
  }
}
```

**Features**:
- Multiple language support
- Inline scripts or file-based scripts
- Temporary file management for inline scripts
- Environment variable injection
- Job parameters as environment variables (JOB_PARAM_*)
- Output capture
- Timeout support

---

## Notification Provider Plugins

Notification providers implement the `NotificationProvider` SPI to send alerts about job execution status.

### EmailNotificationProvider

Sends email notifications via SMTP.

**Name**: `email`

**Configuration** (via system properties):
```properties
notification.email.enabled=true
notification.email.smtp.host=smtp.example.com
notification.email.smtp.port=587
notification.email.smtp.auth=true
notification.email.smtp.starttls=true
notification.email.smtp.ssl=false
notification.email.smtp.username=user@example.com
notification.email.smtp.password=password
notification.email.from=job-scheduler@example.com
notification.email.recipients=admin@example.com,ops@example.com
```

**Features**:
- SMTP support with authentication
- TLS/SSL support
- Multiple recipients
- Priority headers for critical alerts
- Success, failure, and timeout notifications

---

### SlackNotificationProvider

Sends notifications to Slack via webhook.

**Name**: `slack`

**Configuration** (via system properties):
```properties
notification.slack.enabled=true
notification.slack.webhook.url=https://hooks.slack.com/services/YOUR/WEBHOOK/URL
notification.slack.channel=#job-alerts
notification.slack.username=Job Scheduler
```

**Features**:
- Webhook-based integration
- Rich message formatting with emojis
- Color-coded messages (success=green, warning=yellow, error=red)
- Channel targeting

---

### WebhookNotificationProvider

Sends generic webhook notifications to configured endpoints.

**Name**: `webhook`

**Configuration** (via system properties):
```properties
notification.webhook.enabled=true
notification.webhook.url=https://api.example.com/notifications
notification.webhook.auth.token=your-bearer-token
notification.webhook.timeout=5000
```

**Payload Format**:
```json
{
  "eventType": "SUCCESS|FAILURE|TIMEOUT|CUSTOM_NOTIFICATION",
  "timestamp": "2025-11-15T10:30:00Z",
  "message": "Job completed successfully",
  "execution": {
    "id": "exec-123",
    "jobId": "job-456",
    "jobName": "my-job",
    "status": "SUCCESS",
    "startTime": "2025-11-15T10:29:00Z",
    "endTime": "2025-11-15T10:30:00Z",
    "duration": "PT1M",
    "triggeredBy": "user@example.com",
    "nodeId": "node-1"
  }
}
```

**Features**:
- Generic HTTP POST webhook
- Bearer token authentication
- Configurable timeout
- JSON payload format

---

### LogNotificationProvider

Logs notifications using SLF4J.

**Name**: `log`

**Features**:
- INFO level for success
- WARN level for timeout
- ERROR level for failure and critical alerts
- Detailed execution information in debug mode
- Always enabled (no configuration required)

---

## Event Publisher Plugins

Event publishers implement the `EventPublisher` SPI to publish job lifecycle events.

### DefaultEventPublisher

In-memory event bus with listener registration.

**Configuration** (via system properties):
```properties
event.publisher.async.enabled=true
event.publisher.async.threads=4
```

**Usage**:
```java
DefaultEventPublisher publisher = new DefaultEventPublisher();

// Register listener
publisher.registerListener(event -> {
    System.out.println("Event: " + event.getType());
});

// Register channel-specific listener
publisher.registerListener("job-events", event -> {
    System.out.println("Job event: " + event.getJobId());
});

// Publish event
publisher.publish(jobEvent);
publisher.publish("job-events", jobEvent);
publisher.publishAsync(jobEvent);
```

**Features**:
- In-memory event distribution
- Listener registration
- Channel-based routing
- Synchronous and asynchronous publishing
- Thread-safe implementation

---

### KafkaEventPublisher

Publishes events to Apache Kafka.

**Configuration** (via system properties):
```properties
event.publisher.kafka.enabled=true
event.publisher.kafka.bootstrap.servers=localhost:9092
event.publisher.kafka.topic=job-scheduler-events
event.publisher.kafka.acks=1
event.publisher.kafka.retries=3
event.publisher.kafka.compression=snappy
```

**Note**: This is a stub implementation. To use Kafka:
1. Add kafka-clients dependency to pom.xml
2. Uncomment Kafka-specific code in KafkaEventPublisher.java
3. Configure Kafka bootstrap servers

**Event Format**:
```json
{
  "id": "event-123",
  "type": "EXECUTION_COMPLETED",
  "jobId": "job-456",
  "executionId": "exec-789",
  "tenantId": "tenant-1",
  "timestamp": "2025-11-15T10:30:00Z",
  "source": "node-1",
  "payload": {
    "status": "SUCCESS",
    "duration": "PT1M"
  }
}
```

**Features**:
- Kafka topic publishing
- Channel-to-topic mapping
- Configurable compression
- Retry configuration
- Asynchronous delivery

---

## SPI Registration

All plugins are registered via Java's ServiceLoader mechanism using META-INF/services files:

- `META-INF/services/com.enterprise.scheduler.core.spi.JobExecutor`
- `META-INF/services/com.enterprise.scheduler.core.spi.NotificationProvider`
- `META-INF/services/com.enterprise.scheduler.core.spi.EventPublisher`

Plugins are automatically discovered and loaded at runtime.

## Creating Custom Plugins

### Custom Job Executor

1. Implement the `JobExecutor` interface:
```java
public class MyCustomExecutor implements JobExecutor {
    @Override
    public String getSupportedType() {
        return "custom";
    }

    @Override
    public ExecutionResult execute(Job job, JobExecution execution) {
        // Your execution logic
        return ExecutionResult.success(result);
    }

    @Override
    public void cancel(JobExecution execution) {
        // Cancellation logic
    }
}
```

2. Register in `META-INF/services/com.enterprise.scheduler.core.spi.JobExecutor`:
```
com.example.MyCustomExecutor
```

### Custom Notification Provider

1. Implement the `NotificationProvider` interface:
```java
public class MyNotificationProvider implements NotificationProvider {
    @Override
    public String getName() {
        return "mynotifier";
    }

    @Override
    public void notifySuccess(JobExecution execution) {
        // Send success notification
    }

    @Override
    public void notifyFailure(JobExecution execution) {
        // Send failure notification
    }

    @Override
    public void notifyTimeout(JobExecution execution) {
        // Send timeout notification
    }

    @Override
    public void notify(String title, String message, NotificationLevel level) {
        // Send custom notification
    }
}
```

2. Register in `META-INF/services/com.enterprise.scheduler.core.spi.NotificationProvider`:
```
com.example.MyNotificationProvider
```

### Custom Event Publisher

1. Implement the `EventPublisher` interface:
```java
public class MyEventPublisher implements EventPublisher {
    @Override
    public void publish(JobEvent event) {
        // Publish event
    }

    @Override
    public void publish(String channel, JobEvent event) {
        // Publish to specific channel
    }

    @Override
    public void publishAsync(JobEvent event) {
        // Async publish
    }
}
```

2. Register in `META-INF/services/com.enterprise.scheduler.core.spi.EventPublisher`:
```
com.example.MyEventPublisher
```

## Dependencies

This module depends on:
- `job-scheduler-common` - Common utilities and models
- `job-scheduler-core` - Core domain and SPI interfaces
- `lombok` - Code generation
- `javax.mail` (for EmailNotificationProvider)

## Testing

See the `job-scheduler-tests` module for comprehensive plugin testing examples.

## License

Enterprise Job Scheduler Platform - Internal Use Only
