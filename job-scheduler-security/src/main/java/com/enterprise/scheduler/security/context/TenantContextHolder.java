package com.enterprise.scheduler.security.context;

import lombok.extern.slf4j.Slf4j;

/**
 * Thread-local holder for tenant context in multi-tenant applications.
 * Provides a secure way to access the current tenant ID within the request scope.
 *
 * Feature #56: Multi-Tenancy Context
 */
@Slf4j
public class TenantContextHolder {

    private static final ThreadLocal<TenantContext> CONTEXT = new InheritableThreadLocal<>();

    /**
     * Set the tenant ID for the current thread
     */
    public static void setTenantId(String tenantId) {
        if (tenantId == null) {
            log.warn("Attempting to set null tenant ID");
            clear();
            return;
        }

        TenantContext context = CONTEXT.get();
        if (context == null) {
            context = new TenantContext();
            CONTEXT.set(context);
        }

        context.setTenantId(tenantId);
        log.debug("Set tenant context: {}", tenantId);
    }

    /**
     * Get the tenant ID for the current thread
     */
    public static String getTenantId() {
        TenantContext context = CONTEXT.get();
        return context != null ? context.getTenantId() : null;
    }

    /**
     * Set additional context data
     */
    public static void setContextData(String key, Object value) {
        TenantContext context = CONTEXT.get();
        if (context == null) {
            context = new TenantContext();
            CONTEXT.set(context);
        }
        context.setContextData(key, value);
    }

    /**
     * Get additional context data
     */
    public static Object getContextData(String key) {
        TenantContext context = CONTEXT.get();
        return context != null ? context.getContextData(key) : null;
    }

    /**
     * Get the entire tenant context
     */
    public static TenantContext getContext() {
        return CONTEXT.get();
    }

    /**
     * Set the entire tenant context
     */
    public static void setContext(TenantContext context) {
        CONTEXT.set(context);
    }

    /**
     * Clear the tenant context for the current thread
     */
    public static void clear() {
        CONTEXT.remove();
        log.debug("Cleared tenant context");
    }

    /**
     * Check if tenant context is set
     */
    public static boolean isSet() {
        TenantContext context = CONTEXT.get();
        return context != null && context.getTenantId() != null;
    }

    /**
     * Execute a runnable with a specific tenant context
     */
    public static void executeWithTenant(String tenantId, Runnable runnable) {
        String previousTenantId = getTenantId();
        try {
            setTenantId(tenantId);
            runnable.run();
        } finally {
            if (previousTenantId != null) {
                setTenantId(previousTenantId);
            } else {
                clear();
            }
        }
    }

    /**
     * Execute a callable with a specific tenant context
     */
    public static <T> T executeWithTenant(String tenantId, java.util.concurrent.Callable<T> callable) throws Exception {
        String previousTenantId = getTenantId();
        try {
            setTenantId(tenantId);
            return callable.call();
        } finally {
            if (previousTenantId != null) {
                setTenantId(previousTenantId);
            } else {
                clear();
            }
        }
    }
}
