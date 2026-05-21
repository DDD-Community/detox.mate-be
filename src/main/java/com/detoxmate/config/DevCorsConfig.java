package com.detoxmate.config;

import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@Profile("dev")
public class DevCorsConfig implements WebMvcConfigurer {

    static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:3000",
            "http://localhost:3001",
            "http://localhost:3002",
            "http://localhost:3003",
            "http://localhost:3004",
            "http://localhost:3005",
            "https://api-dev.detoxmate.co.kr",
            "https://api.detoxmate.co.kr"
    );

    static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    static final List<String> ALLOWED_HEADERS = List.of("Authorization", "X-Admin-Token", "Content-Type", "Accept");
    static final boolean ALLOW_CREDENTIALS = false;
    static final long MAX_AGE = 3600L;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(ALLOWED_ORIGINS.toArray(String[]::new))
                .allowedMethods(ALLOWED_METHODS.toArray(String[]::new))
                .allowedHeaders(ALLOWED_HEADERS.toArray(String[]::new))
                .allowCredentials(ALLOW_CREDENTIALS)
                .maxAge(MAX_AGE);
    }

    CorsConfiguration corsConfiguration() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(ALLOWED_ORIGINS);
        configuration.setAllowedMethods(ALLOWED_METHODS);
        configuration.setAllowedHeaders(ALLOWED_HEADERS);
        configuration.setAllowCredentials(ALLOW_CREDENTIALS);
        configuration.setMaxAge(MAX_AGE);
        return configuration;
    }
}
