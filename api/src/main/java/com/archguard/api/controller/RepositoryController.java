package com.archguard.api.controller;

import com.archguard.api.dto.ScanResponse;
import com.archguard.api.service.ScanService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** HTTP endpoint for a repository's persisted scan history. */
@RestController
@RequestMapping("/api/repos")
public class RepositoryController {
    private final ScanService scanService;
    public RepositoryController(ScanService scanService) { this.scanService = scanService; }

    @GetMapping("/{repositoryId}/history")
    public List<ScanResponse> history(@PathVariable UUID repositoryId) { return scanService.history(repositoryId); }
}
