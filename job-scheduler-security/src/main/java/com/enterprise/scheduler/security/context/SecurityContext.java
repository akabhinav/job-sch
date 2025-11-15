package com.enterprise.scheduler.security.context;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Enhanced Security Context that combines Spring Security with tenant context.
 * Provides convenient access to both authentication and tenant information.
 *
 * Feature #56: Enhanced Security Context
 */
@Slf4j
public class SecurityContext {

    /**
     * Get the current authentication
     */
    public static Optional<Authentication> getAuthentication() {
        org.springframework.security.core.context.SecurityContext context =
                SecurityContextHolder.getContext();
        return Optional.ofNullable(context.getAuthentication());
    }

    /**
     * Get the current username
     */
    public static Optional<String> getCurrentUsername() {
        return getAuthentication()
                .filter(Authentication::isAuthenticated)
                .map(Authentication::getName);
    }

    /**
     * Get the current user ID (same as username in this implementation)
     */
    public static Optional<String> getCurrentUserId() {
        return getCurrentUsername();
    }

    /**
     * Get the current tenant ID
     */
    public static Optional<String> getCurrentTenantId() {
        return Optional.ofNullable(TenantContextHolder.getTenantId());
    }

    /**
     * Check if the current user is authenticated
     */
    public static boolean isAuthenticated() {
        return getAuthentication()
                .map(Authentication::isAuthenticated)
                .orElse(false);
    }

    /**
     * Check if the current user has a specific role
     */
    public static boolean hasRole(String role) {
        return getAuthentication()
                .map(auth -> auth.getAuthorities().stream()
                        .anyMatch(grantedAuthority ->
                                grantedAuthority.getAuthority().equals("ROLE_" + role)))
                .orElse(false);
    }

    /**
     * Check if the current user has any of the specified roles
     */
    public static boolean hasAnyRole(String... roles) {
        if (roles == null || roles.length == 0) {
            return false;
        }

        return getAuthentication()
                .map(auth -> {
                    for (String role : roles) {
                        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        if (auth.getAuthorities().stream()
                                .anyMatch(grantedAuthority ->
                                        grantedAuthority.getAuthority().equals(authority))) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Check if the current user has all of the specified roles
     */
    public static boolean hasAllRoles(String... roles) {
        if (roles == null || roles.length == 0) {
            return true;
        }

        return getAuthentication()
                .map(auth -> {
                    for (String role : roles) {
                        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;
                        if (auth.getAuthorities().stream()
                                .noneMatch(grantedAuthority ->
                                        grantedAuthority.getAuthority().equals(authority))) {
                            return false;
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * Get tenant-aware user identifier (userId@tenantId)
     */
    public static Optional<String> getTenantAwareUserId() {
        Optional<String> userId = getCurrentUserId();
        Optional<String> tenantId = getCurrentTenantId();

        if (userId.isPresent() && tenantId.isPresent()) {
            return Optional.of(userId.get() + "@" + tenantId.get());
        } else if (userId.isPresent()) {
            return userId;
        }

        return Optional.empty();
    }

    /**
     * Execute a runnable with temporary authentication
     */
    public static void executeWithAuthentication(Authentication authentication, Runnable runnable) {
        org.springframework.security.core.context.SecurityContext previousContext =
                SecurityContextHolder.getContext();
        try {
            org.springframework.security.core.context.SecurityContext newContext =
                    SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(newContext);
            runnable.run();
        } finally {
            SecurityContextHolder.setContext(previousContext);
        }
    }

    /**
     * Execute a runnable with both authentication and tenant context
     */
    public static void executeWithContext(Authentication authentication, String tenantId, Runnable runnable) {
        org.springframework.security.core.context.SecurityContext previousSecurityContext =
                SecurityContextHolder.getContext();
        String previousTenantId = TenantContextHolder.getTenantId();

        try {
            // Set security context
            org.springframework.security.core.context.SecurityContext newContext =
                    SecurityContextHolder.createEmptyContext();
            newContext.setAuthentication(authentication);
            SecurityContextHolder.setContext(newContext);

            // Set tenant context
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            }

            runnable.run();
        } finally {
            SecurityContextHolder.setContext(previousSecurityContext);
            if (previousTenantId != null) {
                TenantContextHolder.setTenantId(previousTenantId);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    /**
     * Clear all context (security and tenant)
     */
    public static void clearAll() {
        SecurityContextHolder.clearContext();
        TenantContextHolder.clear();
    }

    /**
     * Get context information as a string (for logging)
     */
    public static String getContextInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("Security Context: ");

        getCurrentUsername().ifPresentOrElse(
                username -> sb.append("user=").append(username),
                () -> sb.append("anonymous")
        );

        getCurrentTenantId().ifPresent(
                tenantId -> sb.append(", tenant=").append(tenantId)
        );

        return sb.toString();
    }
}
