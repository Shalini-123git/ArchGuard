package com.archguard.api.service;

import com.archguard.analysis.ProjectAnalyzer;
import com.archguard.analysis.ScanReport;
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

    public ScanWorker(ScanJpaRepository scans, ScanPersistenceService persistence, IngestionService ingestion,
                      ViolationExplanationService explanations) {
        this.scans = scans; this.persistence = persistence; this.ingestion = ingestion; this.explanations = explanations;
    }

    public void execute(UUID scanId) {
        long startedAt = System.nanoTime();
        String previousScanId = MDC.get("scanId");
        MDC.put("scanId", scanId.toString());
        try {
            LOGGER.info("scanId={} event=started", scanId);
            persistence.markRunning(scanId);
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
            String message = safeMessage(exception);
            persistence.markFailed(scanId, message);
            LOGGER.info("scanId={} event=failed exceptionType={} message={} durationMs={}", scanId,
                    exception.getClass().getSimpleName(), message, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
        } finally {
            if (previousScanId == null) MDC.remove("scanId");
            else MDC.put("scanId", previousScanId);
        }
    }

    public void reject(UUID scanId) {
        persistence.markFailed(scanId, "Scan queue is full; try again later");
        LOGGER.info("scanId={} event=failed reason=executor_queue_full", scanId);
    }

    private void complete(UUID scanId, ProjectSource source, ArchitectureRules rules, long startedAt) {
        ScanReport report = analyzer.analyze(source.root(), ScanOptions.defaults(), rules);
        persistence.persistCompleted(scanId, source.commitSha(), report);
        explanations.explainScan(scanId, source.root());
        LOGGER.info("scanId={} event=completed violationCount={} durationMs={}", scanId,
                report.getViolations().size(), TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));
    }

    private ArchitectureRules rules(String rulesYaml) {
        return rulesYaml == null || rulesYaml.isBlank() ? null : rulesLoader.load(rulesYaml);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Scan failed" : message.substring(0, Math.min(message.length(), 2000));
    }
}
