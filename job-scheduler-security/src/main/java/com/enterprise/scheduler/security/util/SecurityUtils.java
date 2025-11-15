package com.enterprise.scheduler.security.util;

import com.enterprise.scheduler.security.context.SecurityContext;
import com.enterprise.scheduler.security.rbac.Permission;
import com.enterprise.scheduler.security.rbac.Role;
import lombok.experimental.UtilityClass;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility class for security-related operations.
 *
 * Feature #51-60: Security Utilities
 */
@UtilityClass
public class SecurityUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generate a secure random token
     */
    public static String generateSecureToken(int length) {
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Generate a random API key
     */
    public static String generateApiKey() {
        return generateSecureToken(32);
    }

    /**
     * Generate a random password
     */
    public static String generateRandomPassword(int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*";
        StringBuilder password = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            password.append(chars.charAt(SECURE_RANDOM.nextInt(chars.length())));
        }
        return password.toString();
    }

    /**
     * Convert roles to Spring Security authorities
     */
    public static Collection<? extends GrantedAuthority> rolesToAuthorities(Set<Role> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority(role.getAuthority()))
                .collect(Collectors.toSet());
    }

    /**
     * Convert permissions to Spring Security authorities
     */
    public static Collection<? extends GrantedAuthority> permissionsToAuthorities(Set<Permission> permissions) {
        return permissions.stream()
                .map(permission -> new SimpleGrantedAuthority("PERMISSION_" + permission.name()))
                .collect(Collectors.toSet());
    }

    /**
     * Get all permissions for a set of roles
     */
    public static Set<Permission> getPermissionsForRoles(Set<Role> roles) {
        return roles.stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Check if the current user has a specific permission
     */
    public static boolean hasPermission(Permission permission) {
        return SecurityContext.getAuthentication()
                .map(auth -> {
                    Set<Role> roles = extractRoles(auth.getAuthorities());
                    return roles.stream().anyMatch(role -> role.hasPermission(permission));
                })
                .orElse(false);
    }

    /**
     * Check if the current user has any of the specified permissions
     */
    public static boolean hasAnyPermission(Permission... permissions) {
        return SecurityContext.getAuthentication()
                .map(auth -> {
                    Set<Role> roles = extractRoles(auth.getAuthorities());
                    Set<Permission> userPermissions = getPermissionsForRoles(roles);
                    for (Permission permission : permissions) {
                        if (userPermissions.contains(permission)) {
                            return true;
                        }
                    }
                    return false;
                })
                .orElse(false);
    }

    /**
     * Check if the current user has all of the specified permissions
     */
    public static boolean hasAllPermissions(Permission... permissions) {
        return SecurityContext.getAuthentication()
                .map(auth -> {
                    Set<Role> roles = extractRoles(auth.getAuthorities());
                    Set<Permission> userPermissions = getPermissionsForRoles(roles);
                    for (Permission permission : permissions) {
                        if (!userPermissions.contains(permission)) {
                            return false;
                        }
                    }
                    return true;
                })
                .orElse(false);
    }

    /**
     * Extract roles from authorities
     */
    private static Set<Role> extractRoles(Collection<? extends GrantedAuthority> authorities) {
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring(5))
                .map(roleName -> {
                    try {
                        return Role.valueOf(roleName);
                    } catch (IllegalArgumentException e) {
                        return null;
                    }
                })
                .filter(role -> role != null)
                .collect(Collectors.toSet());
    }

    /**
     * Mask sensitive data (e.g., API keys)
     */
    public static String maskSensitiveData(String data) {
        if (data == null || data.length() < 8) {
            return "***";
        }
        return data.substring(0, 4) + "..." + data.substring(data.length() - 4);
    }

    /**
     * Validate password strength
     */
    public static boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }

        boolean hasUpper = password.chars().anyMatch(Character::isUpperCase);
        boolean hasLower = password.chars().anyMatch(Character::isLowerCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(ch -> !Character.isLetterOrDigit(ch));

        return hasUpper && hasLower && hasDigit && hasSpecial;
    }

    /**
     * Generate a tenant-aware entity ID
     */
    public static String generateTenantAwareId(String tenantId, String entityType) {
        return String.format("%s_%s_%s", tenantId, entityType, generateSecureToken(16));
    }
}
