package com.archguard.api.service;

import com.archguard.api.config.ArchGuardProperties;
import com.archguard.api.dto.CreateScanRequest;
import com.archguard.api.dto.GraphResponse;
import com.archguard.api.dto.QueueStatusResponse;
import com.archguard.api.dto.ScanResponse;
import com.archguard.api.dto.ViolationResponse;
import com.archguard.api.dto.RulesResponse;
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
import com.archguard.rules.ArchitectureRules;
import com.archguard.rules.ArchitectureRulesLoader;
import com.archguard.rules.LayerMatcher;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashMap;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Orchestrates validation, durable queueing, background execution, and API read models. */
@Service
public class ScanService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ScanService.class);
    private static final int MAX_PAGE_SIZE = 100;
    private final RepositoryJpaRepository repositories;
    private final ScanJpaRepository scans;
    private final ModuleJpaRepository modules;
    private final DependencyJpaRepository dependencies;
    private final ViolationJpaRepository violations;
    private final ScanWorker worker;
    private final IngestionService ingestion;
    private final ThreadPoolTaskExecutor scanExecutor;
    private final ArchGuardProperties properties;

    public ScanService(RepositoryJpaRepository repositories, ScanJpaRepository scans, ModuleJpaRepository modules,
                       DependencyJpaRepository dependencies, ViolationJpaRepository violations, ScanWorker worker, IngestionService ingestion,
                       @Qualifier("scanExecutor") ThreadPoolTaskExecutor scanExecutor, ArchGuardProperties properties) {
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
                LOGGER.info("scanId={} event=queued", scanId);
                try { scanExecutor.execute(() -> worker.execute(scanId)); }
                catch (RuntimeException ignored) { worker.reject(scanId); }
            }
        });
        return response(scan);
    }

    @Transactional(readOnly = true)
    public ScanResponse get(UUID scanId) { return response(findScan(scanId)); }

    @Transactional(readOnly = true)
    public List<ScanResponse> list(ScanStatus status, int page, int size) {
        if (page < 0) throw new BadRequestException("page must be zero or greater");
        if (size < 1 || size > MAX_PAGE_SIZE) throw new BadRequestException("size must be between 1 and " + MAX_PAGE_SIZE);
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var pageScans = status == null ? scans.findAllByOrderByCreatedAtDesc(pageable)
                : scans.findByStatusOrderByCreatedAtDesc(status, pageable);
        return pageScans.stream().map(this::response).toList();
    }

    @Transactional(readOnly = true)
    public QueueStatusResponse queueStatus() {
        var executor = scanExecutor.getThreadPoolExecutor();
        var queue = executor.getQueue();
        return new QueueStatusResponse(scanExecutor.getActiveCount(), scanExecutor.getPoolSize(), queue.size(),
                queue.size() + queue.remainingCapacity(), scans.countByStatus(ScanStatus.QUEUED),
                scans.countByStatus(ScanStatus.RUNNING), scanExecutor.getMaxPoolSize());
    }

    @Transactional(readOnly = true)
    public GraphResponse graph(UUID scanId) {
        ScanEntity scan = requireCompleted(scanId);
        var nodes = modules.findByScanIdOrderByName(scanId);
        var edges = dependencies.findByScanId(scanId);
        var scanViolations = violations.findWithAffectedModulesByScanId(scanId);
        List<Set<String>> cycles = cycles(nodes.stream().map(module -> module.getName()).toList(), edges);
        Set<String> cycleMembers = cycles.stream().flatMap(Set::stream).collect(java.util.stream.Collectors.toSet());
        Map<String, List<String>> edgeViolations = edgeViolations(scanViolations, edges, cycles);
        LayerMatcher layerMatcher = new LayerMatcher(rules(scan.getRulesYaml()));
        return new GraphResponse(nodes.stream().map(module -> new GraphResponse.GraphNodeResponse(module.getName(),
                layerMatcher.layerOf(module.getName()), cycleMembers.contains(module.getName()))).toList(), edges.stream()
                .map(edge -> new GraphResponse.GraphEdgeResponse(edge.getId().toString(), edge.getFromModule().getName(), edge.getToModule().getName(),
                        edgeViolations.getOrDefault(edgeKey(edge.getFromModule().getName(), edge.getToModule().getName()), List.of()))).toList());
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
    public RulesResponse rulesView(UUID scanId) {
        ScanEntity scan = findScan(scanId);
        String yaml = scan.getRulesYaml();
        if (yaml == null || yaml.isBlank()) return new RulesResponse(false, null, rulesView(new ArchitectureRules()));
        try {
            return new RulesResponse(true, yaml, rulesView(new ArchitectureRulesLoader().load(yaml)));
        } catch (RuntimeException exception) {
            return new RulesResponse(true, yaml, null);
        }
    }

    private RulesResponse.RulesView rulesView(ArchitectureRules rules) {
        return new RulesResponse.RulesView(
                rules.getLayers().stream().map(layer -> new RulesResponse.Layer(layer.getName(), List.copyOf(layer.getPackagePatterns()))).toList(),
                rules.getForbidden().stream().map(rule -> new RulesResponse.Forbidden(rule.getFrom(), rule.getTo(), rule.getSeverity())).toList(),
                rules.isNoCycles());
    }

    @Transactional(readOnly = true)
    public List<ScanResponse> history(UUID repositoryId) {
        if (!repositories.existsById(repositoryId)) throw new NotFoundException("Repository not found: " + repositoryId);
        return scans.findByRepositoryIdOrderByCreatedAtDesc(repositoryId).stream().map(this::response).toList();
    }

    private ScanResponse response(ScanEntity scan) {
        Integer score = scan.getStatus() == ScanStatus.COMPLETED
                ? HealthScoreCalculator.calculate(violations.countByScanId(scan.getId()))
                : null;
        return ScanResponse.from(scan, score);
    }

    private ScanEntity findScan(UUID scanId) { return scans.findById(scanId).orElseThrow(() -> new NotFoundException("Scan not found: " + scanId)); }
    private ScanEntity requireCompleted(UUID scanId) {
        ScanEntity scan = findScan(scanId);
        if (scan.getStatus() != ScanStatus.COMPLETED) throw new ConflictException("Scan is not completed");
        return scan;
    }

    private ArchitectureRules rules(String yaml) {
        return yaml == null || yaml.isBlank() ? new ArchitectureRules() : new ArchitectureRulesLoader().load(yaml);
    }

    private List<Set<String>> cycles(List<String> nodeNames, List<com.archguard.api.persistence.DependencyEntity> edges) {
        var graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
        nodeNames.forEach(graph::addVertex);
        edges.forEach(edge -> graph.addEdge(edge.getFromModule().getName(), edge.getToModule().getName()));
        return new KosarajuStrongConnectivityInspector<>(graph).stronglyConnectedSets().stream()
                .filter(component -> component.size() > 1).toList();
    }

    private Map<String, List<String>> edgeViolations(List<com.archguard.api.persistence.ViolationEntity> scanViolations,
                                                       List<com.archguard.api.persistence.DependencyEntity> edges, List<Set<String>> cycles) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (var violation : scanViolations) {
            if ("no-cycles".equals(violation.getRuleId())) {
                Set<String> cycle = cycles.stream().filter(component ->
                        component.contains(violation.getSourceModule().getName()) && component.contains(violation.getTargetModule().getName()))
                        .findFirst().orElse(Set.of());
                for (var edge : edges) if (cycle.contains(edge.getFromModule().getName()) && cycle.contains(edge.getToModule().getName())) {
                    result.computeIfAbsent(edgeKey(edge.getFromModule().getName(), edge.getToModule().getName()), ignored -> new java.util.ArrayList<>()).add(violation.getId().toString());
                }
            } else if (violation.getSourceModule() != null && violation.getTargetModule() != null) {
                result.computeIfAbsent(edgeKey(violation.getSourceModule().getName(), violation.getTargetModule().getName()), ignored -> new java.util.ArrayList<>()).add(violation.getId().toString());
            }
        }
        return result;
    }

    private String edgeKey(String from, String to) { return from + "\u0000" + to; }
}
