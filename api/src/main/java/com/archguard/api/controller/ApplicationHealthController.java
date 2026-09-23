package com.archguard.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lightweight liveness endpoint used by the local container health check. */
@RestController
@RequestMapping("/api/health")
public class ApplicationHealthController {
    @GetMapping
    public HealthResponse health() {
        return new HealthResponse("UP");
    }

    public record HealthResponse(String status) { }
}
