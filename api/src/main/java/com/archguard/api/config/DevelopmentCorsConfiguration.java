package com.archguard.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Allows the Vite dev server only when the explicit development profile is active. */
@Configuration
@Profile("dev")
public class DevelopmentCorsConfiguration implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**").allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST").allowedHeaders("Content-Type");
    }
}
