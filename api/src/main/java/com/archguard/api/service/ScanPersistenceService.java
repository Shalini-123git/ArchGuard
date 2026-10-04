package com.archguard.api.service;

import com.archguard.analysis.ArchitectureViolation;
import com.archguard.analysis.ScanReport;
import com.archguard.api.exception.NotFoundException;
import com.archguard.api.persistence.DependencyEntity;
import com.archguard.api.persistence.ModuleEntity;
import com.archguard.api.persistence.ScanEntity;
import com.archguard.api.persistence.ViolationEntity;
import com.archguard.api.repository.DependencyJpaRepository;
import com.archguard.api.repository.ModuleJpaRepository;
import com.archguard.api.repository.ScanJpaRepository;
import com.archguard.api.repository.ViolationJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Persists lifecycle changes and deterministic core-analysis facts atomically. */
@Service
public class ScanPersistenceService {
    private final ScanJpaRepository scans;
    private final ModuleJpaRepository modules;
    private final DependencyJpaRepository dependencies;
    private final ViolationJpaRepository violations;

    public ScanPersistenceService(ScanJpaRepository scans, ModuleJpaRepository modules, DependencyJpaRepository dependencies, ViolationJpaRepository violations) {
        this.scans = scans; this.modules = modules; this.dependencies = dependencies; this.violations = violations;
    }

    @Transactional
    public boolean markRunning(UUID scanId) {
        ScanEntity scan = findForUpdate(scanId);
        if (!active(scan)) return false;
        scan.markRunning();
        return true;
    }

    @Transactional
    public boolean persistCompleted(UUID scanId, String commitSha, ScanReport report) {
        ScanEntity scan = findForUpdate(scanId);
        if (!active(scan)) return false;
        Map<String, ModuleEntity> moduleByName = new LinkedHashMap<>();
        for (String packageName : report.getGraph().packages()) moduleByName.put(packageName, new ModuleEntity(scan, packageName));
        modules.saveAll(moduleByName.values());
        dependencies.saveAll(report.getGraph().dependencies().stream()
                .map(edge -> new DependencyEntity(scan, moduleByName.get(edge.getFromPackage()), moduleByName.get(edge.getToPackage())))
                .toList());
        for (ArchitectureViolation violation : report.getViolations()) {
            var affected = violation.getBlastRadiusPackages().stream().map(moduleByName::get).filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
            violations.save(new ViolationEntity(scan, violation.getRuleId(), violation.getSeverity(),
                    moduleByName.get(violation.getFromPackage()), moduleByName.get(violation.getToPackage()), affected));
        }
        return true;
    }

    @Transactional
    public boolean markCompleted(UUID scanId, String commitSha) {
        ScanEntity scan = findForUpdate(scanId);
        if (!active(scan)) return false;
        scan.markCompleted(commitSha);
        return true;
    }

    @Transactional
    public boolean markFailed(UUID scanId, String message) {
        ScanEntity scan = findForUpdate(scanId);
        if (!active(scan)) return false;
        scan.markFailed(message);
        return true;
    }

    @Transactional
    public void markCancelled(UUID scanId) {
        ScanEntity scan = findForUpdate(scanId);
        if (active(scan)) scan.markCancelled();
    }

    @Transactional
    public void discardResults(UUID scanId) {
        violations.deleteByScanId(scanId);
        dependencies.deleteByScanId(scanId);
        modules.deleteByScanId(scanId);
    }

    private ScanEntity find(UUID scanId) { return scans.findById(scanId).orElseThrow(() -> new NotFoundException("Scan not found: " + scanId)); }
    private ScanEntity findForUpdate(UUID scanId) { return scans.findByIdForUpdate(scanId).orElseThrow(() -> new NotFoundException("Scan not found: " + scanId)); }
    private boolean active(ScanEntity scan) { return scan.getStatus() == com.archguard.api.persistence.ScanStatus.QUEUED || scan.getStatus() == com.archguard.api.persistence.ScanStatus.RUNNING; }
}
