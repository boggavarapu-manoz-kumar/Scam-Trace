package com.kampusvc.digitalrisk.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

/**
 * MVC configuration for the Digital Risk Protection Platform.
 *
 * <p>CORS is configured narrowly: only origins supplied via {@code app.cors.allowed-origins}
 * are permitted. In production this must be set to the exact frontend origin.
 * During local development the Vite dev-server proxy forwards requests as
 * same-origin, so CORS is only exercised when the frontend is served from a
 * different port or host than the backend.</p>
 *
 * <p>No wildcard origins are permitted. Credentials are not exposed here;
 * they will be addressed in the dedicated security sprint.</p>
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebConfig.class);

    /** Comma-separated list of allowed frontend origins, e.g. {@code http://localhost:5173}. */
    private final String[] allowedOrigins;

    public WebConfig(
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String rawOrigins) {
        this.allowedOrigins = Arrays.stream(rawOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);
        log.info("CORS allowed origins: {}", Arrays.toString(this.allowedOrigins));
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type", "Accept", "Authorization")
                .exposedHeaders("X-Request-Id")
                .allowCredentials(false)
                .maxAge(3600);
    }
}
