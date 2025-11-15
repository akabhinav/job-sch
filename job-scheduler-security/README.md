# Job Scheduler Security Module

This module provides comprehensive security features for the Job Scheduler Platform, including authentication, authorization, audit logging, and multi-tenancy support.

## Features (Coverage: #51-60)

### 1. JWT Authentication (#51-52)
- Token-based authentication using JSON Web Tokens
- Access and refresh token support
- Secure token generation and validation
- Multi-tenancy support in tokens

### 2. Role-Based Access Control (RBAC) (#53)
- Predefined roles: ADMIN, JOB_MANAGER, JOB_VIEWER, OPERATOR, TENANT_ADMIN, etc.
- Fine-grained permissions for all operations
- Method-level security annotations
- Custom permission evaluator

### 3. API Key Management (#54)
- Secure API key generation and validation
- Support for live and test keys
- IP address restrictions
- Key rotation and revocation
- Usage tracking

### 4. Audit Logging (#55)
- Comprehensive audit trail for all security events
- Multiple event types and severity levels
- Automatic context capture (user, tenant, IP, etc.)
- Configurable retention policies
- Export capabilities for compliance

### 5. Multi-Tenancy (#56)
- Thread-local tenant context
- Tenant isolation enforcement
- Tenant-aware security checks
- Cross-tenant access control

## Architecture

```
job-scheduler-security/
├── src/main/java/com/enterprise/scheduler/security/
│   ├── jwt/                    # JWT authentication
│   │   ├── JwtTokenProvider.java
│   │   └── JwtAuthenticationFilter.java
│   ├── rbac/                   # Role-based access control
│   │   ├── Role.java
│   │   ├── Permission.java
│   │   ├── CustomPermissionEvaluator.java
│   │   ├── RequirePermission.java
│   │   ├── RequireAnyPermission.java
│   │   ├── RequireAllPermissions.java
│   │   └── PermissionAspect.java
│   ├── apikey/                 # API key management
│   │   ├── ApiKey.java
│   │   ├── ApiKeyStatus.java
│   │   ├── ApiKeyService.java
│   │   ├── ApiKeyRepository.java
│   │   └── ApiKeyAuthenticationFilter.java
│   ├── audit/                  # Audit logging
│   │   ├── AuditEvent.java
│   │   ├── AuditEventType.java
│   │   ├── AuditSeverity.java
│   │   ├── AuditService.java
│   │   └── AuditRepository.java
│   ├── context/                # Security context
│   │   ├── TenantContext.java
│   │   ├── TenantContextHolder.java
│   │   ├── TenantAware.java
│   │   └── SecurityContext.java
│   ├── exception/              # Security exceptions
│   │   ├── SecurityException.java
│   │   ├── InvalidApiKeyException.java
│   │   ├── TenantAccessDeniedException.java
│   │   └── InsufficientPermissionsException.java
│   ├── util/                   # Utilities
│   │   └── SecurityUtils.java
│   ├── SecurityConfiguration.java
│   └── SecurityProperties.java
└── src/main/resources/
    └── application-security.yml
```

## Usage

### 1. JWT Authentication

#### Generate Token
```java
@Autowired
private JwtTokenProvider tokenProvider;

// Generate access token
String token = tokenProvider.generateTokenForUser(
    "username",
    "tenant-id",
    Role.JOB_MANAGER
);

// Generate refresh token
Authentication auth = ...; // from Spring Security
String refreshToken = tokenProvider.generateRefreshToken(auth);
```

#### Validate Token
```java
// Validate token
boolean isValid = tokenProvider.validateToken(token);

// Extract information
String username = tokenProvider.getUsernameFromToken(token);
String tenantId = tokenProvider.getTenantIdFromToken(token);
String roles = tokenProvider.getRolesFromToken(token);
```

### 2. Role-Based Access Control

#### Using Annotations
```java
@Service
public class JobService {

    // Spring Security annotations
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteAllJobs() { ... }

    @PreAuthorize("hasAnyRole('ADMIN', 'JOB_MANAGER')")
    public void createJob(Job job) { ... }

    // Custom permission annotations
    @RequirePermission(Permission.JOB_CREATE)
    public void createScheduledJob(Job job) { ... }

    @RequireAnyPermission({Permission.JOB_UPDATE, Permission.JOB_DELETE})
    public void modifyJob(String jobId) { ... }

    @RequireAllPermissions({Permission.JOB_CREATE, Permission.SCHEDULE_CREATE})
    public void createWithSchedule(Job job, Schedule schedule) { ... }
}
```

#### Programmatic Checks
```java
@Autowired
private CustomPermissionEvaluator permissionEvaluator;

public void someMethod() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // Check single permission
    if (permissionEvaluator.hasPermission(auth, null, Permission.JOB_CREATE)) {
        // User has permission
    }

    // Check multiple permissions
    if (permissionEvaluator.hasAnyPermission(auth,
            Permission.JOB_UPDATE, Permission.JOB_DELETE)) {
        // User has at least one permission
    }
}
```

### 3. API Key Management

#### Create API Key
```java
@Autowired
private ApiKeyService apiKeyService;

// Create API key
ApiKeyService.ApiKeyCreationResult result = apiKeyService.createApiKey(
    "My API Key",           // name
    "user-123",             // userId
    "tenant-456",           // tenantId
    Set.of("jobs:read", "jobs:write"), // scopes
    false                   // isTest
);

// Get the plain text key (only available once!)
String plainTextKey = result.plainTextKey();
System.out.println("Your API key: " + plainTextKey);
```

#### Validate API Key
```java
// Validate API key
Optional<ApiKey> apiKey = apiKeyService.validateApiKey(apiKeyString);

if (apiKey.isPresent() && apiKey.get().hasScope("jobs:read")) {
    // API key is valid and has required scope
}
```

#### Revoke API Key
```java
apiKeyService.revokeApiKey(keyId, "admin-user");
```

### 4. Audit Logging

#### Log Events
```java
@Autowired
private AuditService auditService;

// Log a simple event
auditService.logEvent(
    AuditEventType.JOB_CREATED,
    "user-123",
    "tenant-456",
    "Created job: daily-backup",
    Map.of("jobId", "job-789")
);

// Log a security event
auditService.logSecurityEvent(
    AuditEventType.SUSPICIOUS_ACTIVITY,
    "user-123",
    "tenant-456",
    "Multiple failed login attempts",
    AuditSeverity.WARNING,
    Map.of("attempts", 5)
);

// Log authentication
auditService.logAuthenticationEvent(
    AuditEventType.LOGIN_SUCCESS,
    "user-123",
    true,
    null
);
```

#### Query Audit Events
```java
// Get events for a user
List<AuditEvent> userEvents = auditService.getAuditEventsForUser("user-123", 100);

// Get events for a tenant
List<AuditEvent> tenantEvents = auditService.getAuditEventsForTenant("tenant-456", 100);

// Get security events
List<AuditEvent> securityEvents = auditService.getSecurityEvents(50);

// Get events by time range
List<AuditEvent> events = auditService.getAuditEventsByTimeRange(
    startTime, endTime, 100
);
```

### 5. Multi-Tenancy

#### Using Tenant Context
```java
// Set tenant context
TenantContextHolder.setTenantId("tenant-123");

try {
    // All operations within this block are tenant-scoped
    jobService.listJobs();
} finally {
    // Always clear context
    TenantContextHolder.clear();
}

// Execute with tenant context
TenantContextHolder.executeWithTenant("tenant-123", () -> {
    // Tenant-scoped operations
    jobService.createJob(job);
});
```

#### Tenant-Aware Entities
```java
@Entity
public class Job implements TenantAware {
    private String tenantId;

    @Override
    public String getTenantId() {
        return tenantId;
    }

    @Override
    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }
}
```

#### Using Security Context
```java
// Get current user and tenant
Optional<String> username = SecurityContext.getCurrentUsername();
Optional<String> tenantId = SecurityContext.getCurrentTenantId();

// Check authentication
if (SecurityContext.isAuthenticated()) {
    // User is authenticated
}

// Check roles
if (SecurityContext.hasRole("ADMIN")) {
    // User is admin
}

if (SecurityContext.hasAnyRole("ADMIN", "JOB_MANAGER")) {
    // User has at least one role
}
```

## Configuration

Add to your `application.yml`:

```yaml
scheduler:
  security:
    jwt:
      secret: ${JWT_SECRET:your-secret-key-at-least-32-characters}
      expiration: PT24H
      refresh-expiration: P7D

    api-key:
      max-keys-per-user: 10
      enable-ip-restrictions: true

    audit:
      enabled: true
      retention-period: P90D
      async-logging: true

    multi-tenancy:
      enabled: true
      tenant-header: X-Tenant-ID
      enforce-tenant-isolation: true
```

## Security Best Practices

1. **JWT Secrets**: Always use strong, randomly generated secrets in production
2. **API Keys**: Treat API keys like passwords - never log or expose them
3. **Audit Logs**: Regularly review audit logs for suspicious activity
4. **Tenant Isolation**: Always enforce tenant isolation in multi-tenant deployments
5. **Password Policies**: Use strong password requirements
6. **HTTPS**: Always use HTTPS in production
7. **CORS**: Configure specific allowed origins, not wildcards
8. **Rate Limiting**: Implement rate limiting for authentication endpoints

## API Endpoints

### Authentication
- `POST /api/auth/login` - User login
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/logout` - User logout

### API Keys
- `POST /api/apikeys` - Create API key
- `GET /api/apikeys` - List API keys
- `DELETE /api/apikeys/{id}` - Revoke API key
- `POST /api/apikeys/{id}/rotate` - Rotate API key

### Audit
- `GET /api/audit/events` - List audit events
- `GET /api/audit/events/{id}` - Get audit event
- `POST /api/audit/export` - Export audit logs

## Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn verify
```

## Dependencies

- Spring Boot 3.x
- Spring Security 6.x
- JJWT (Java JWT library)
- Lombok
- JUnit 5

## Contributing

Follow the platform's contribution guidelines and ensure all security tests pass before submitting PRs.

## License

Copyright (c) 2024 Enterprise Job Scheduler Platform
