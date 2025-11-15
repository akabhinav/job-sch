package com.enterprise.scheduler.security.rbac;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * Aspect for handling custom permission annotations.
 * Provides AOP-based permission checking for methods annotated with custom security annotations.
 *
 * Feature #53: Method-Level Security Aspect
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final CustomPermissionEvaluator permissionEvaluator;

    /**
     * Handle @RequireAnyPermission annotation
     */
    @Around("@annotation(com.enterprise.scheduler.security.rbac.RequireAnyPermission)")
    public Object checkAnyPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequireAnyPermission annotation = method.getAnnotation(RequireAnyPermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        Permission[] requiredPermissions = annotation.value();
        boolean hasPermission = permissionEvaluator.hasAnyPermission(authentication, requiredPermissions);

        if (!hasPermission) {
            log.warn("Access denied: user={}, method={}, requiredPermissions={}",
                    authentication.getName(),
                    method.getName(),
                    java.util.Arrays.toString(requiredPermissions));
            throw new AccessDeniedException(
                    "Insufficient permissions. Required any of: " +
                    java.util.Arrays.toString(requiredPermissions));
        }

        log.debug("Permission check passed: user={}, method={}",
                authentication.getName(), method.getName());
        return joinPoint.proceed();
    }

    /**
     * Handle @RequireAllPermissions annotation
     */
    @Around("@annotation(com.enterprise.scheduler.security.rbac.RequireAllPermissions)")
    public Object checkAllPermissions(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequireAllPermissions annotation = method.getAnnotation(RequireAllPermissions.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }

        Permission[] requiredPermissions = annotation.value();
        boolean hasAllPermissions = permissionEvaluator.hasAllPermissions(authentication, requiredPermissions);

        if (!hasAllPermissions) {
            log.warn("Access denied: user={}, method={}, requiredPermissions={}",
                    authentication.getName(),
                    method.getName(),
                    java.util.Arrays.toString(requiredPermissions));
            throw new AccessDeniedException(
                    "Insufficient permissions. Required all of: " +
                    java.util.Arrays.toString(requiredPermissions));
        }

        log.debug("Permission check passed: user={}, method={}",
                authentication.getName(), method.getName());
        return joinPoint.proceed();
    }
}
