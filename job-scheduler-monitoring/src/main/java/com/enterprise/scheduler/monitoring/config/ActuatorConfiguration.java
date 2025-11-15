package com.enterprise.scheduler.monitoring.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.CorsEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.endpoint.web.WebEndpointProperties;
import org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType;
import org.springframework.boot.actuate.endpoint.web.EndpointMediaTypes;
import org.springframework.boot.actuate.endpoint.web.WebEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ControllerEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.annotation.ServletEndpointsSupplier;
import org.springframework.boot.actuate.endpoint.web.servlet.WebMvcEndpointHandlerMapping;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Spring Boot Actuator configuration
 * Feature #30: Actuator Endpoints Configuration
 */
@Slf4j
@Configuration
public class ActuatorConfiguration {

    @PostConstruct
    public void init() {
        log.info("Spring Boot Actuator configured for job scheduler monitoring");
    }

    /**
     * Custom endpoint mapping for Actuator
     * This bean is needed when using a custom management port
     */
    @Bean
    public WebMvcEndpointHandlerMapping webEndpointServletHandlerMapping(
            WebEndpointsSupplier webEndpointsSupplier,
            ServletEndpointsSupplier servletEndpointsSupplier,
            ControllerEndpointsSupplier controllerEndpointsSupplier,
            EndpointMediaTypes endpointMediaTypes,
            CorsEndpointProperties corsProperties,
            WebEndpointProperties webEndpointProperties,
            Environment environment) {

        List<String> basePath = new ArrayList<>();
        basePath.add(webEndpointProperties.getBasePath());

        return new WebMvcEndpointHandlerMapping(
                new org.springframework.boot.actuate.endpoint.web.EndpointMapping(webEndpointProperties.getBasePath()),
                webEndpointsSupplier.getEndpoints(),
                endpointMediaTypes,
                corsProperties.toCorsConfiguration(),
                new org.springframework.boot.actuate.endpoint.web.EndpointLinksResolver(
                        webEndpointsSupplier.getEndpoints(),
                        webEndpointProperties.getBasePath()),
                shouldRegisterLinksMapping(webEndpointProperties, environment, basePath)
        );
    }

    private boolean shouldRegisterLinksMapping(
            WebEndpointProperties webEndpointProperties,
            Environment environment,
            Collection<String> basePath) {
        return webEndpointProperties.getDiscovery().isEnabled() &&
               (org.springframework.boot.actuate.autoconfigure.web.server.ManagementPortType.get(environment) == ManagementPortType.DIFFERENT);
    }
}
