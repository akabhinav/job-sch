package com.enterprise.scheduler.security.exception;

import com.enterprise.scheduler.security.rbac.Permission;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Exception thrown when a user lacks required permissions.
 *
 * Feature #53: Permission Exceptions
 */
public class InsufficientPermissionsException extends SecurityException {

    private final Set<Permission> requiredPermissions;

    public InsufficientPermissionsException(Permission... requiredPermissions) {
        super("Insufficient permissions. Required: " +
                Arrays.stream(requiredPermissions)
                        .map(Permission::name)
                        .collect(Collectors.joining(", ")));
        this.requiredPermissions = Arrays.stream(requiredPermissions).collect(Collectors.toSet());
    }

    public InsufficientPermissionsException(String message, Permission... requiredPermissions) {
        super(message);
        this.requiredPermissions = Arrays.stream(requiredPermissions).collect(Collectors.toSet());
    }

    public Set<Permission> getRequiredPermissions() {
        return requiredPermissions;
    }
}
