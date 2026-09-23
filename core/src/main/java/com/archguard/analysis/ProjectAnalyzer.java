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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Walks a project, parses Java sources, builds the package graph, finds cycles, and applies YAML rules when given.
 */
public final class ProjectAnalyzer {

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
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Scan path is not a directory: " + root);
        }
        List<Path> javaFiles = projectScanner.findJavaFiles(root, options);
        Set<String> projectPackages = new LinkedHashSet<>();
        List<ParsedJavaFile> parsedFiles = new ArrayList<>();
        List<ParseFailure> parseFailures = new ArrayList<>();

        for (Path javaFile : javaFiles) {
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

        PackageDependencyGraph graph = graphBuilder.build(projectPackages, parsedFiles);
        List<DependencyCycle> cycles = cycleDetector.detect(graph);
        List<ArchitectureViolation> violations = rules == null
                ? List.of()
                : architectureChecker.check(graph, cycles, rules);
        return new ScanReport(graph, cycles, parseFailures, violations);
    }
}
