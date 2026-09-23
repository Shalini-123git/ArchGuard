package com.archguard.api.service;

import com.archguard.api.config.ArchGuardProperties;
import com.archguard.api.dto.CreateScanRequest;
import com.archguard.api.dto.GraphResponse;
import com.archguard.api.dto.ScanResponse;
import com.archguard.api.dto.ViolationResponse;
import com.archguard.api.exception.BadRequestException;
import com.archguard.api.exception.ConflictException;
import com.archguard.api.exception.NotFoundException;
import com.archguard.api.persistence.RepositoryEntity;
import com.archguard.api.persistence.ScanEntity;
import com.archguard.api.persistence.ScanSourceType;
import com.archguard.api.persistence.ScanStatus;
import com.archguard.api.repository.DependencyJpaRepository;
import com.archguard.api.repository.ModuleJpaRepository;
import com.archguard.api.repository.RepositoryJpaRepository;
import com.archguard.api.repository.ScanJpaRepository;
import com.archguard.api.repository.ViolationJpaRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

/** Orchestrates validation, durable queueing, background execution, and API read models. */
@Service
public class ScanService {
    private final RepositoryJpaRepository repositories;
    private final ScanJpaRepository scans;
    private final ModuleJpaRepository modules;
    private final DependencyJpaRepository dependencies;
    private final ViolationJpaRepository violations;
    private final ScanWorker worker;
    private final IngestionService ingestion;
    private final TaskExecutor scanExecutor;
    private final ArchGuardProperties properties;

    public ScanService(RepositoryJpaRepository repositories, ScanJpaRepository scans, ModuleJpaRepository modules,
                       DependencyJpaRepository dependencies, ViolationJpaRepository violations, ScanWorker worker, IngestionService ingestion,
                       @Qualifier("scanExecutor") TaskExecutor scanExecutor, ArchGuardProperties properties) {
        this.repositories = repositories; this.scans = scans; this.modules = modules; this.dependencies = dependencies;
        this.violations = violations; this.worker = worker; this.ingestion = ingestion; this.scanExecutor = scanExecutor; this.properties = properties;
    }

    @Transactional
    public ScanResponse queue(CreateScanRequest request) {
        boolean local = request.localPath() != null && !request.localPath().isBlank();
        if (local && !properties.getScan().isLocalEnabled()) throw new BadRequestException("Local scans are disabled");
        if (!local) ingestion.validateRemoteUrl(request.repoUrl().trim());
        String source = local ? "local:" + Path.of(request.localPath()).toAbsolutePath().normalize() : request.repoUrl().trim();
        RepositoryEntity repository = repositories.findByRepositoryUrl(source).orElseGet(() -> repositories.save(new RepositoryEntity(source)));
        ScanEntity scan = scans.save(new ScanEntity(repository, local ? ScanSourceType.LOCAL : ScanSourceType.REMOTE, request.rulesYaml()));
        UUID scanId = scan.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try { scanExecutor.execute(() -> worker.execute(scanId)); }
                catch (RuntimeException ignored) { worker.reject(scanId); }
            }
        });
        return ScanResponse.from(scan);
    }

    @Transactional(readOnly = true)
    public ScanResponse get(UUID scanId) { return ScanResponse.from(findScan(scanId)); }

    @Transactional(readOnly = true)
    public GraphResponse graph(UUID scanId) {
        requireCompleted(scanId);
        var nodes = modules.findByScanIdOrderByName(scanId);
        return new GraphResponse(nodes.stream().map(module -> module.getName()).toList(), dependencies.findByScanId(scanId).stream()
                .map(edge -> new GraphResponse.GraphEdgeResponse(edge.getFromModule().getName(), edge.getToModule().getName())).toList());
    }

    @Transactional(readOnly = true)
    public List<ViolationResponse> violations(UUID scanId) {
        requireCompleted(scanId);
        return violations.findWithAffectedModulesByScanId(scanId).stream().map(violation -> new ViolationResponse(
                violation.getId(), violation.getRuleId(), violation.getSeverity(),
                violation.getSourceModule() == null ? null : violation.getSourceModule().getName(),
                violation.getTargetModule() == null ? null : violation.getTargetModule().getName(), violation.getBlastRadiusCount(),
                violation.getAffectedModules().stream().map(module -> module.getName()).sorted().toList(),
                violation.getExplanation(), violation.isExplanationFallback())).toList();
    }

    @Transactional(readOnly = true)
    public List<ScanResponse> history(UUID repositoryId) {
        if (!repositories.existsById(repositoryId)) throw new NotFoundException("Repository not found: " + repositoryId);
        return scans.findByRepositoryIdOrderByCreatedAtDesc(repositoryId).stream().map(ScanResponse::from).toList();
    }

    private ScanEntity findScan(UUID scanId) { return scans.findById(scanId).orElseThrow(() -> new NotFoundException("Scan not found: " + scanId)); }
    private void requireCompleted(UUID scanId) {
        if (findScan(scanId).getStatus() != ScanStatus.COMPLETED) throw new ConflictException("Scan is not completed");
    }
}
