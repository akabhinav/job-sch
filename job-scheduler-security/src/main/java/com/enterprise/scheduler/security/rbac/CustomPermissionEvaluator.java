package com.enterprise.scheduler.security.rbac;

import com.enterprise.scheduler.security.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Custom Permission Evaluator for fine-grained access control.
 * Evaluates permissions based on roles and tenant context.
 *
 * Feature #53: Permission Evaluation
 */
@Slf4j
@Component
public class CustomPermissionEvaluator implements PermissionEvaluator {

    /**
     * Evaluate permission for a specific domain object
     */
    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        if (permission instanceof String) {
            return hasPermission(authentication, (String) permission);
        } else if (permission instanceof Permission) {
            return hasPermission(authentication, (Permission) permission);
        }

        log.warn("Unsupported permission type: {}", permission.getClass());
        return false;
    }

    /**
     * Evaluate permission for a specific object ID and type
     */
    @Override
    public boolean hasPermission(Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        // Check basic permission first
        boolean hasBasicPermission = hasPermission(authentication, permission);
        if (!hasBasicPermission) {
            return false;
        }

        // Additional tenant-based checks
        String tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null && targetDomainObject != null) {
            // Check if the target object belongs to the current tenant
            // This would require the domain object to implement a TenantAware interface
            return checkTenantAccess(authentication, targetId, targetType, tenantId);
        }

        return true;
    }

    /**
     * Check if authentication has a specific permission (by string)
     */
    private boolean hasPermission(Authentication authentication, String permissionCode) {
        try {
            Permission permission = Permission.fromCode(permissionCode);
            return hasPermission(authentication, permission);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid permission code: {}", permissionCode);
            return false;
        }
    }

    /**
     * Check if authentication has a specific permission (by enum)
     */
    private boolean hasPermission(Authentication authentication, Permission permission) {
        Set<Role> userRoles = extractRoles(authentication);

        // Check if any of the user's roles has the required permission
        boolean hasPermission = userRoles.stream()
                .anyMatch(role -> role.hasPermission(permission));

        if (log.isDebugEnabled()) {
            log.debug("Permission check: user={}, permission={}, result={}",
                    authentication.getName(), permission, hasPermission);
        }

        return hasPermission;
    }

    /**
     * Check if user has any of the specified permissions
     */
    public boolean hasAnyPermission(Authentication authentication, Permission... permissions) {
        return Arrays.stream(permissions)
                .anyMatch(permission -> hasPermission(authentication, permission));
    }

    /**
     * Check if user has all of the specified permissions
     */
    public boolean hasAllPermissions(Authentication authentication, Permission... permissions) {
        return Arrays.stream(permissions)
                .allMatch(permission -> hasPermission(authentication, permission));
    }

    /**
     * Extract roles from authentication
     */
    private Set<Role> extractRoles(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5)) // Remove "ROLE_" prefix
                .map(this::parseRole)
                .filter(role -> role != null)
                .collect(Collectors.toSet());
    }

    /**
     * Parse role from string
     */
    private Role parseRole(String roleName) {
        try {
            return Role.valueOf(roleName);
        } catch (IllegalArgumentException e) {
            log.warn("Unknown role: {}", roleName);
            return null;
        }
    }

    /**
     * Check tenant-based access control
     */
    private boolean checkTenantAccess(Authentication authentication, Serializable targetId,
                                     String targetType, String tenantId) {
        // Check if user is admin (admins can access all tenants)
        if (extractRoles(authentication).contains(Role.ADMIN)) {
            return true;
        }

        // For tenant-specific resources, verify the resource belongs to the current tenant
        // This is a placeholder - actual implementation would query the resource
        log.debug("Tenant access check: user={}, tenantId={}, targetType={}, targetId={}",
                authentication.getName(), tenantId, targetType, targetId);

        // In a real implementation, you would:
        // 1. Fetch the resource by targetId and targetType
        // 2. Check if resource.getTenantId() equals tenantId
        // 3. Return the result

        return true; // Placeholder - implement actual logic
    }

    /**
     * Check if user can access a specific tenant
     */
    public boolean canAccessTenant(Authentication authentication, String tenantId) {
        // Admins can access all tenants
        if (extractRoles(authentication).contains(Role.ADMIN)) {
            return true;
        }

        // Check if the user's current tenant matches
        String currentTenantId = TenantContextHolder.getTenantId();
        if (currentTenantId != null) {
            return currentTenantId.equals(tenantId);
        }

        // Additional checks could be implemented here, such as:
        // - Checking user's assigned tenants from database
        // - Checking tenant hierarchy/relationships
        // - Checking cross-tenant access permissions

        return false;
    }
}
