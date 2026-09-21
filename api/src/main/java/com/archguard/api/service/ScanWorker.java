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
import org.springframework.stereotype.Service;

import java.util.UUID;

/** Executes one queued scan outside the request thread and records a terminal status. */
@Service
public class ScanWorker {
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
        try {
            persistence.markRunning(scanId);
            ScanEntity scan = scans.findById(scanId).orElseThrow();
            ArchitectureRules rules = rules(scan.getRulesYaml());
            if (scan.getSourceType() == ScanSourceType.REMOTE) {
                ingestion.withRemoteRepository(scan.getRepository().getRepositoryUrl(), source -> {
                    complete(scanId, source, rules);
                    return null;
                });
            } else {
                complete(scanId, ingestion.localSource(scan.getRepository().getRepositoryUrl().substring("local:".length())), rules);
            }
        } catch (Exception exception) {
            persistence.markFailed(scanId, safeMessage(exception));
        }
    }

    public void reject(UUID scanId) {
        persistence.markFailed(scanId, "Scan queue is full; try again later");
    }

    private void complete(UUID scanId, ProjectSource source, ArchitectureRules rules) {
        ScanReport report = analyzer.analyze(source.root(), ScanOptions.defaults(), rules);
        persistence.persistCompleted(scanId, source.commitSha(), report);
        explanations.explainScan(scanId, source.root());
    }

    private ArchitectureRules rules(String rulesYaml) {
        return rulesYaml == null || rulesYaml.isBlank() ? null : rulesLoader.load(rulesYaml);
    }

    private String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? "Scan failed" : message.substring(0, Math.min(message.length(), 2000));
    }
}
