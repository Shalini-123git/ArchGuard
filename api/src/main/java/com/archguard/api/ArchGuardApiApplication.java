package com.archguard.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import com.archguard.api.config.ArchGuardProperties;

/**
 * Spring Boot entry point. REST endpoints are added in a later phase.
 */
@SpringBootApplication
@EnableConfigurationProperties(ArchGuardProperties.class)
public class ArchGuardApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchGuardApiApplication.class, args);
    }
}
