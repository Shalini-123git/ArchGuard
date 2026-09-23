package com.archguard.rules;

import com.archguard.graph.PackageDependency;
import com.archguard.graph.PackageDependencyGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * Finds graph edges whose endpoints sit in a forbidden layer pair.
 */
public final class ForbiddenEdgeChecker {

    private final LayerMatcher layerMatcher;
    private final ArchitectureRules rules;

    public ForbiddenEdgeChecker(ArchitectureRules rules) {
        this.rules = rules;
        this.layerMatcher = new LayerMatcher(rules);
    }

    public List<PackageDependency> findForbidden(PackageDependencyGraph graph) {
        List<PackageDependency> forbiddenEdges = new ArrayList<>();
        for (PackageDependency dependency : graph.dependencies()) {
            String fromLayer = layerMatcher.layerOf(dependency.getFromPackage());
            String toLayer = layerMatcher.layerOf(dependency.getToPackage());
            if (isForbidden(fromLayer, toLayer)) {
                forbiddenEdges.add(dependency);
            }
        }
        return List.copyOf(forbiddenEdges);
    }

    private boolean isForbidden(String fromLayer, String toLayer) {
        for (ForbiddenEdgeRule rule : rules.getForbidden()) {
            if (rule.getFrom().equals(fromLayer) && rule.getTo().equals(toLayer)) {
                return true;
            }
        }
        return false;
    }
}
