package com.archguard.api.service;

import com.archguard.analysis.ProjectAnalyzer;
import com.archguard.analysis.ScanReport;
import com.archguard.analysis.ScanCancelledException;
import com.archguard.api.persistence.ScanEntity;
import com.archguard.api.persistence.ScanSourceType;
import com.archguard.api.repository.ScanJpaRepository;
import com.archguard.api.llm.ViolationExplanationService;
import com.archguard.rules.ArchitectureRules;
import com.archguard.rules.ArchitectureRulesLoader;
import com.archguard.scan.ScanOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

/** Executes one queued scan outside the request thread and records a terminal status. */
@Service
public class ScanWorker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanWorker.class);
    private final ScanJpaRepository scans;
    private final ScanPersistenceService persistence;
    private final IngestionService ingestion;
    private final ViolationExplanationService explanations;
    private final ProjectAnalyzer analyzer = new ProjectAnalyzer();
    private final ArchitectureRulesLoader rulesLoader = new ArchitectureRulesLoader();
    private final ScanCancellationRegistry cancellationRegistry;

    public ScanWorker(ScanJpaRepository scans, ScanPersistenceService persistence, IngestionService ingestion,
                      ViolationExplanationService explanations, ScanCancellationRegistry cancellationRegistry) {
        this.scans = scans; this.persistence = persistence; this.ingestion = ingestion; this.explanations = explanations;
        this.cancellationRegistry = cancellationRegistry;
    }

    public void execute(UUID scanId) {
        long startedAt = System.nanoTime();
        String previousScanId = MDC.get("scanId");
        MDC.put("scanId", scanId.toString());
        cancellationRegistry.register(scanId, Thread.currentThread());
        try {
            LOGGER.info("scanId={} event=started", scanId);
            ScanEntity initial = scans.findById(scanId).orElseThrow();
            if (initial.getStatus() == com.archguard.api.persistence.ScanStatus.CANCELLED || cancelled(scanId)) return;
            if (!persistence.markRunning(scanId)) return;
            ScanEntity scan = scans.findById(scanId).orElseThrow();
            ArchitectureRules rules = rules(scan.getRulesYaml());
            if (scan.getSourceType() == ScanSourceType.REMOTE) {
                ingestion.withRemoteRepository(scan.getRepository().getRepositoryUrl(), source -> {
                    complete(scanId, source, rules, startedAt);
                    return null;
                });
            } else {
                complete(scanId, ingestion.localSource(scan.getRepository().getRepositoryUrl().substring("local:".length())), rules, startedAt);
            }
        } catch (Exception exception) {
            if (cancelled(scanId) || exception instanceof ScanCancelledException) {
                persistence.discardResults(scanId);
                LOGGER.info("scanId={} event=cancelled", scanId);
                return;
            }
            String message = safeMessage(exception);
            persistence.markFailed(scanId, message);
            LOGGER.info("scanId={} event=failed exceptionType={} message={} durationMs={}", scanId,
                    exception.getClass().getSimpleName(), message, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        } finally {
            cancellationRegistry.unregister(scanId);
            if (previousScanId == null) MDC.remove("scanId");
            else MDC.put("scanId", previousScanId);
        }
    }

    public void reject(UUID scanId) {
        persistence.markFailed(scanId, "Scan queue is full; try again later");
        LOGGER.info("scanId={} event=failed reason=executor_queue_full", scanId);
    }

    private void complete(UUID scanId, ProjectSource source, ArchitectureRules rules, long startedAt) {
        ScanReport report = analyzer.analyze(source.root(), ScanOptions.defaults(), rules,
                () -> cancelled(scanId) || Thread.currentThread().isInterrupted());
        if (!persistence.persistCompleted(scanId, source.commitSha(), report)) return;
        explanations.explainScan(scanId, source.root(), () -> cancelled(scanId) || Thread.currentThread().isInterrupted());
        if (!persistence.markCompleted(scanId, source.commitSha())) {
            persistence.discardResults(scanId);
            return;
        }
        LOGGER.info("scanId={} event=completed violationCount={} durationMs={}", scanId,
                report.getViolations().size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    private boolean cancelled(UUID scanId) {
        return cancellationRegistry.isCancelled(scanId) || Thread.currentThread().isInterrupted();
    }

    private ArchitectureRules rules(String rulesYaml) {
        return rulesYaml == null || rulesYaml.isBlank() ? new ArchitectureRules() : rulesLoader.load(rulesYaml);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Scan failed" : message.substring(0, Math.min(message.length(), 2000));
    }
}
