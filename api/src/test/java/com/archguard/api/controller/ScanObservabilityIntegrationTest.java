package com.archguard.api.controller;

import com.archguard.api.persistence.RepositoryEntity;
import com.archguard.api.persistence.ScanEntity;
import com.archguard.api.persistence.ScanSourceType;
import com.archguard.api.persistence.ScanStatus;
import com.archguard.api.repository.RepositoryJpaRepository;
import com.archguard.api.repository.ScanJpaRepository;
import com.archguard.api.service.StartupScanRecoveryRunner;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScanObservabilityIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ScanJpaRepository scans;
    @Autowired private RepositoryJpaRepository repositories;
    @Autowired private StartupScanRecoveryRunner recoveryRunner;

    @Test
    void listsScansWithOptionalStatusAndPagination() throws Exception {
        RepositoryEntity repository = repositories.save(new RepositoryEntity("https://example.test/list-" + java.util.UUID.randomUUID()));
        ScanEntity failed = new ScanEntity(repository, ScanSourceType.REMOTE, null);
        failed.markFailed("test failure");
        scans.save(failed);

        mockMvc.perform(get("/api/scans").param("status", "FAILED").param("page", "0").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].status").value("FAILED"));
        mockMvc.perform(get("/api/scans").param("size", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/scans").param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesLiveExecutorAndDatabaseQueueMetrics() throws Exception {
        mockMvc.perform(get("/api/scans/queue-status").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeThreadCount").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.poolSize").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.queueSize").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.queueCapacity").value(greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.queuedScans").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.runningScans").value(greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.maxConcurrentScans").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void startupRecoveryFailsQueuedAndRunningScansButLeavesTerminalScansAlone() {
        RepositoryEntity repository = repositories.save(new RepositoryEntity("https://example.test/" + java.util.UUID.randomUUID()));
        ScanEntity queued = scans.save(new ScanEntity(repository, ScanSourceType.REMOTE, null));
        ScanEntity running = new ScanEntity(repository, ScanSourceType.REMOTE, null);
        running.markRunning();
        running = scans.save(running);
        ScanEntity completed = new ScanEntity(repository, ScanSourceType.REMOTE, null);
        completed.markCompleted("abc123");
        completed = scans.save(completed);

        recoveryRunner.run(null);

        org.junit.jupiter.api.Assertions.assertEquals(ScanStatus.FAILED, scans.findById(queued.getId()).orElseThrow().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("Scan interrupted by server restart", scans.findById(queued.getId()).orElseThrow().getErrorMessage());
        org.junit.jupiter.api.Assertions.assertEquals(ScanStatus.FAILED, scans.findById(running.getId()).orElseThrow().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals(ScanStatus.COMPLETED, scans.findById(completed.getId()).orElseThrow().getStatus());
    }
}