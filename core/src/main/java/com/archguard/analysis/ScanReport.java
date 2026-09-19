package com.archguard.analysis;

import com.archguard.cycle.DependencyCycle;
import com.archguard.graph.PackageDependencyGraph;

import java.util.List;
import java.util.Objects;

/**
 * Deterministic facts from one folder scan. No LLM content lives here.
 */
public final class ScanReport {

    private final PackageDependencyGraph graph;
    private final List<DependencyCycle> cycles;
    private final List<ParseFailure> parseFailures;

    public ScanReport(
            PackageDependencyGraph graph,
            List<DependencyCycle> cycles,
            List<ParseFailure> parseFailures
    ) {
        this.graph = Objects.requireNonNull(graph, "graph");
        this.cycles = List.copyOf(cycles);
        this.parseFailures = List.copyOf(parseFailures);
    }

    public PackageDependencyGraph getGraph() {
        return graph;
    }

    public List<DependencyCycle> getCycles() {
        return cycles;
    }

    public List<ParseFailure> getParseFailures() {
        return parseFailures;
    }

    public int packageCount() {
        return graph.packageCount();
    }

    public int edgeCount() {
        return graph.edgeCount();
    }

    public int parseFailureCount() {
        return parseFailures.size();
    }
}
