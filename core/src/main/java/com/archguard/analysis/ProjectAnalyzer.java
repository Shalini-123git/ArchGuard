package com.archguard.analysis;

import com.archguard.cycle.CycleDetector;
import com.archguard.cycle.DependencyCycle;
import com.archguard.graph.DependencyGraphBuilder;
import com.archguard.graph.PackageDependencyGraph;
import com.archguard.parse.JavaSourceParser;
import com.archguard.parse.LanguageParser;
import com.archguard.parse.ParseResult;
import com.archguard.parse.ParsedJavaFile;
import com.archguard.rules.ArchitectureRules;
import com.archguard.scan.ProjectScanner;
import com.archguard.scan.ScanOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;

/**
 * Walks a project, parses Java sources, builds the package graph, finds cycles, and applies YAML rules when given.
 */
public final class ProjectAnalyzer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProjectAnalyzer.class);

    private final ProjectScanner projectScanner;
    private final LanguageParser languageParser;
    private final DependencyGraphBuilder graphBuilder;
    private final CycleDetector cycleDetector;
    private final ArchitectureChecker architectureChecker;

    public ProjectAnalyzer() {
        this(
                new ProjectScanner(),
                new JavaSourceParser(),
                new DependencyGraphBuilder(),
                new CycleDetector(),
                new ArchitectureChecker()
        );
    }

    public ProjectAnalyzer(
            ProjectScanner projectScanner,
            LanguageParser languageParser,
            DependencyGraphBuilder graphBuilder,
            CycleDetector cycleDetector
    ) {
        this(projectScanner, languageParser, graphBuilder, cycleDetector, new ArchitectureChecker());
    }

    public ProjectAnalyzer(
            ProjectScanner projectScanner,
            LanguageParser languageParser,
            DependencyGraphBuilder graphBuilder,
            CycleDetector cycleDetector,
            ArchitectureChecker architectureChecker
    ) {
        this.projectScanner = projectScanner;
        this.languageParser = languageParser;
        this.graphBuilder = graphBuilder;
        this.cycleDetector = cycleDetector;
        this.architectureChecker = architectureChecker;
    }

    public ScanReport analyze(Path root, ScanOptions options) {
        return analyze(root, options, null);
    }

    public ScanReport analyze(Path root, ScanOptions options, ArchitectureRules rules) {
        return analyze(root, options, rules, () -> false);
    }

    public ScanReport analyze(Path root, ScanOptions options, ArchitectureRules rules, BooleanSupplier cancelled) {
        checkCancelled(cancelled);
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Scan path is not a directory: " + root);
        }
        List<Path> javaFiles = projectScanner.findJavaFiles(root, options);
        LOGGER.info("scanId={} stage=parsing started javaFileCount={}", scanId(), javaFiles.size());
        Set<String> projectPackages = new LinkedHashSet<>();
        List<ParsedJavaFile> parsedFiles = new ArrayList<>();
        List<ParseFailure> parseFailures = new ArrayList<>();

        for (Path javaFile : javaFiles) {
            checkCancelled(cancelled);
            ParseResult parseResult = languageParser.parse(javaFile);
            if (parseResult.isSuccessful()) {
                ParsedJavaFile parsedFile = parseResult.getParsedFile();
                parsedFiles.add(parsedFile);
                projectPackages.add(parsedFile.getPackageName());
            } else {
                ParseFailure failure = new ParseFailure(javaFile, parseResult.getErrorMessage());
                parseFailures.add(failure);
                System.err.println("Failed to parse " + javaFile + ": " + failure.getMessage());
            }
        }

        LOGGER.info("scanId={} stage=parsing completed parsedFileCount={} parseFailureCount={}", scanId(), parsedFiles.size(), parseFailures.size());
        checkCancelled(cancelled);
        LOGGER.info("scanId={} stage=graph_build started packageCount={}", scanId(), projectPackages.size());
        PackageDependencyGraph graph = graphBuilder.build(projectPackages, parsedFiles);
        LOGGER.info("scanId={} stage=graph_build completed dependencyCount={}", scanId(), graph.dependencies().size());
        checkCancelled(cancelled);
        LOGGER.info("scanId={} stage=cycle_detection started", scanId());
        List<DependencyCycle> cycles = cycleDetector.detect(graph);
        LOGGER.info("scanId={} stage=cycle_detection completed cycleCount={}", scanId(), cycles.size());
        checkCancelled(cancelled);
        LOGGER.info("scanId={} stage=rule_check started configured={}", scanId(), rules != null);
        LOGGER.info("scanId={} stage=blast_radius started", scanId());
        List<ArchitectureViolation> violations = rules == null
                ? List.of()
                : architectureChecker.check(graph, cycles, rules);
        checkCancelled(cancelled);
        int affectedPackageCount = violations.stream().mapToInt(ArchitectureViolation::getBlastRadiusCount).sum();
        LOGGER.info("scanId={} stage=blast_radius completed affectedPackageCount={}", scanId(), affectedPackageCount);
        LOGGER.info("scanId={} stage=rule_check completed violationCount={}", scanId(), violations.size());
        return new ScanReport(graph, cycles, parseFailures, violations);
    }

    private void checkCancelled(BooleanSupplier cancelled) {
        if (cancelled.getAsBoolean()) throw new ScanCancelledException();
    }

    private String scanId() {
        String scanId = MDC.get("scanId");
        return scanId == null ? "unknown" : scanId;
    }
}
