package com.archguard.cli;

import com.archguard.analysis.ParseFailure;
import com.archguard.analysis.ProjectAnalyzer;
import com.archguard.analysis.ScanReport;
import com.archguard.cycle.DependencyCycle;
import com.archguard.scan.ScanOptions;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Thin command line around {@link ProjectAnalyzer}. Argument parsing is manual on purpose (no Picocli).
 */
public final class ArchGuardCli {

    private static final String USAGE = "Usage: scan <path> [--include-tests]";

    private final ProjectAnalyzer projectAnalyzer;
    private final PrintStream out;
    private final PrintStream err;

    public ArchGuardCli() {
        this(new ProjectAnalyzer(), System.out, System.err);
    }

    ArchGuardCli(ProjectAnalyzer projectAnalyzer, PrintStream out, PrintStream err) {
        this.projectAnalyzer = projectAnalyzer;
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
        boolean includeTestSources = false;
        for (int index = 1; index < args.length; index++) {
            String argument = args[index];
            if ("--include-tests".equals(argument)) {
                includeTestSources = true;
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
        ScanOptions options = includeTestSources ? ScanOptions.includingTestSources() : ScanOptions.defaults();
        ScanReport report = projectAnalyzer.analyze(root, options);
        printReport(report);
        return 0;
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
    }
}
