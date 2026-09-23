package com.archguard.cli;

import com.archguard.analysis.ArchitectureViolation;
import com.archguard.analysis.ParseFailure;
import com.archguard.analysis.ProjectAnalyzer;
import com.archguard.analysis.ScanReport;
import com.archguard.cycle.DependencyCycle;
import com.archguard.rules.ArchitectureRules;
import com.archguard.rules.ArchitectureRulesLoader;
import com.archguard.scan.ScanOptions;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Thin command line around {@link ProjectAnalyzer}. Argument parsing is manual on purpose (no Picocli).
 */
public final class ArchGuardCli {

    private static final String USAGE = "Usage: scan <path> [--rules <file>] [--include-tests]";

    private final ProjectAnalyzer projectAnalyzer;
    private final ArchitectureRulesLoader rulesLoader;
    private final PrintStream out;
    private final PrintStream err;

    public ArchGuardCli() {
        this(new ProjectAnalyzer(), new ArchitectureRulesLoader(), System.out, System.err);
    }

    ArchGuardCli(ProjectAnalyzer projectAnalyzer, PrintStream out, PrintStream err) {
        this(projectAnalyzer, new ArchitectureRulesLoader(), out, err);
    }

    ArchGuardCli(
            ProjectAnalyzer projectAnalyzer,
            ArchitectureRulesLoader rulesLoader,
            PrintStream out,
            PrintStream err
    ) {
        this.projectAnalyzer = projectAnalyzer;
        this.rulesLoader = rulesLoader;
        this.out = out;
        this.err = err;
    }

    public static void main(String[] args) {
        int exitCode = new ArchGuardCli().run(args);
        System.exit(exitCode);
    }

    int run(String[] args) {
        if (args.length < 2 || !"scan".equals(args[0])) {
            err.println(USAGE);
            return 1;
        }
        String pathArgument = null;
        String rulesArgument = null;
        boolean includeTestSources = false;
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if ("--include-tests".equals(argument)) {
                includeTestSources = true;
            } else if ("--rules".equals(argument)) {
                if (index + 1 >= args.length) {
                    err.println(USAGE);
                    return 1;
                }
                index++;
                rulesArgument = args[index];
            } else if (pathArgument == null) {
                pathArgument = argument;
            } else {
                err.println(USAGE);
                return 1;
            }
        }
        if (pathArgument == null) {
            err.println(USAGE);
            return 1;
        }
        Path root = Path.of(pathArgument);
        if (!Files.isDirectory(root)) {
            err.println("Scan path does not exist or is not a directory: " + pathArgument);
            return 1;
        }
        ArchitectureRules rules = null;
        if (rulesArgument != null) {
            Path rulesPath = Path.of(rulesArgument);
            if (!Files.isRegularFile(rulesPath)) {
                err.println("Rules file does not exist: " + rulesArgument);
                return 1;
            }
            try {
                rules = rulesLoader.load(rulesPath);
            } catch (IllegalArgumentException exception) {
                err.println(exception.getMessage());
                return 1;
            }
        }
        ScanOptions options = includeTestSources ? ScanOptions.includingTestSources() : ScanOptions.defaults();
        ScanReport report = projectAnalyzer.analyze(root, options, rules);
        printReport(report);
        return report.getViolations().isEmpty() ? 0 : 2;
    }

    private void printReport(ScanReport report) {
        out.println("Packages: " + report.packageCount());
        out.println("Edges: " + report.edgeCount());
        out.println("Parse failures: " + report.parseFailureCount());
        for (ParseFailure failure : report.getParseFailures()) {
            out.println("  " + failure.getFile() + " (" + failure.getMessage() + ")");
        }
        out.println("Cycles: " + report.getCycles().size());
        for (DependencyCycle cycle : report.getCycles()) {
            out.println("  " + cycle);
        }
        out.println("Violations: " + report.getViolations().size());
        for (ArchitectureViolation violation : report.getViolations()) {
            out.println(
                    "  " + violation.getRuleId()
                            + " " + violation.getFromPackage()
                            + " -> " + violation.getToPackage()
                            + " [" + violation.getSeverity() + "]"
                            + " blastRadius=" + violation.getBlastRadiusCount()
            );
        }
    }
}
