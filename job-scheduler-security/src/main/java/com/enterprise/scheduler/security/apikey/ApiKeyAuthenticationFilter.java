package com.enterprise.scheduler.security.apikey;

import com.enterprise.scheduler.security.context.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authentication filter for API key-based authentication.
 * Checks for API keys in the X-API-Key header.
 *
 * Feature #54: API Key Authentication
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final ApiKeyService apiKeyService;

    private static final String API_KEY_HEADER = "X-API-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String apiKey = extractApiKeyFromRequest(request);

            if (StringUtils.hasText(apiKey)) {
                String ipAddress = getClientIpAddress(request);
                Optional<ApiKey> apiKeyOpt = apiKeyService.validateApiKey(apiKey, ipAddress);

                if (apiKeyOpt.isPresent()) {
                    ApiKey validatedKey = apiKeyOpt.get();

                    // Set tenant context
                    if (validatedKey.getTenantId() != null) {
                        TenantContextHolder.setTenantId(validatedKey.getTenantId());
                        log.debug("Set tenant context from API key: {}", validatedKey.getTenantId());
                    }

                    // Create authentication token with scopes as authorities
                    List<SimpleGrantedAuthority> authorities = validatedKey.getScopes() != null ?
                            validatedKey.getScopes().stream()
                                    .map(scope -> new SimpleGrantedAuthority("SCOPE_" + scope))
                                    .collect(Collectors.toList()) :
                            Collections.emptyList();

                    // Add SERVICE_ACCOUNT role
                    authorities.add(new SimpleGrantedAuthority("ROLE_SERVICE_ACCOUNT"));

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    validatedKey.getUserId(),
                                    null,
                                    authorities
                            );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated with API key: userId={}, tenantId={}",
                            validatedKey.getUserId(), validatedKey.getTenantId());
                }
            }
        } catch (Exception ex) {
            log.error("Could not set API key authentication in security context", ex);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Clear tenant context after request processing
            TenantContextHolder.clear();
        }
    }

    /**
     * Extract API key from request header
     */
    private String extractApiKeyFromRequest(HttpServletRequest request) {
        String apiKey = request.getHeader(API_KEY_HEADER);

        if (StringUtils.hasText(apiKey)) {
            return apiKey.trim();
        }

        return null;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (StringUtils.hasText(ip) && !"unknown".equalsIgnoreCase(ip)) {
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }

    /**
     * Skip filter for certain paths
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        // Skip API key filter for auth endpoints and health checks
        return path.startsWith("/api/auth/") ||
               path.startsWith("/actuator/health") ||
               path.startsWith("/swagger-ui") ||
               path.startsWith("/v3/api-docs");
    }
}
