package com.archguard.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot entry point. REST endpoints are added in a later phase.
 */
@SpringBootApplication
public class ArchGuardApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArchGuardApiApplication.class, args);
    }
}
