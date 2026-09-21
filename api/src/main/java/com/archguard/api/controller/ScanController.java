package com.archguard.api.controller;

import com.archguard.api.dto.CreateScanRequest;
import com.archguard.api.dto.GraphResponse;
import com.archguard.api.dto.ScanResponse;
import com.archguard.api.dto.ViolationResponse;
import com.archguard.api.service.ScanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** HTTP endpoints for queueing scans and reading their deterministic results. */
@RestController
@RequestMapping("/api/scans")
public class ScanController {
    private final ScanService scanService;
    public ScanController(ScanService scanService) { this.scanService = scanService; }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ScanResponse create(@Valid @RequestBody CreateScanRequest request) { return scanService.queue(request); }

    @GetMapping("/{scanId}")
    public ScanResponse get(@PathVariable UUID scanId) { return scanService.get(scanId); }

    @GetMapping("/{scanId}/graph")
    public GraphResponse graph(@PathVariable UUID scanId) { return scanService.graph(scanId); }

    @GetMapping("/{scanId}/violations")
    public List<ViolationResponse> violations(@PathVariable UUID scanId) { return scanService.violations(scanId); }
}
