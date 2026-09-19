package com.archguard.graph;

import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Directed package graph. JGraphT stays inside this type so callers do not depend on it directly.
 */
public final class PackageDependencyGraph {

    private final Graph<String, DefaultEdge> graph;

    PackageDependencyGraph(Graph<String, DefaultEdge> graph) {
        this.graph = graph;
    }

    public Set<String> packages() {
        return Collections.unmodifiableSet(graph.vertexSet());
    }

    public int packageCount() {
        return graph.vertexSet().size();
    }

    public int edgeCount() {
        return graph.edgeSet().size();
    }

    public List<PackageDependency> dependencies() {
        List<PackageDependency> dependencies = new ArrayList<>();
        for (DefaultEdge edge : graph.edgeSet()) {
            String fromPackage = graph.getEdgeSource(edge);
            String toPackage = graph.getEdgeTarget(edge);
            dependencies.add(new PackageDependency(fromPackage, toPackage));
        }
        return List.copyOf(dependencies);
    }

    /** Exposed so cycle detection can run JGraphT algorithms on the same graph instance. */
    public Graph<String, DefaultEdge> jgrapht() {
        return graph;
    }
}
