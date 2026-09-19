package com.archguard.analysis;

import com.archguard.cycle.DependencyCycle;
import com.archguard.graph.PackageDependency;
import com.archguard.graph.PackageDependencyGraph;
import com.archguard.rules.ArchitectureRules;
import com.archguard.rules.ForbiddenEdgeChecker;
import com.archguard.rules.ForbiddenEdgeRule;
import com.archguard.rules.LayerMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Turns graph facts plus YAML rules into a list of violations with blast radius.
 */
public final class ArchitectureChecker {

    private final BlastRadiusCalculator blastRadiusCalculator = new BlastRadiusCalculator();

    public List<ArchitectureViolation> check(
            PackageDependencyGraph graph,
            List<DependencyCycle> cycles,
            ArchitectureRules rules
    ) {
        List<ArchitectureViolation> violations = new ArrayList<>();
        if (rules.isNoCycles()) {
            for (DependencyCycle cycle : cycles) {
                List<String> packages = cycle.getPackages();
                List<String> blastRadius = blastRadiusCalculator.packagesDependingOn(graph, packages);
                String fromPackage = packages.get(0);
                String toPackage = packages.size() > 1 ? packages.get(1) : fromPackage;
                violations.add(new ArchitectureViolation(
                        ArchitectureViolation.RULE_NO_CYCLES,
                        fromPackage,
                        toPackage,
                        "HIGH",
                        blastRadius
                ));
            }
        }
        ForbiddenEdgeChecker forbiddenEdgeChecker = new ForbiddenEdgeChecker(rules);
        LayerMatcher layerMatcher = new LayerMatcher(rules);
        for (PackageDependency edge : forbiddenEdgeChecker.findForbidden(graph)) {
            String fromLayer = layerMatcher.layerOf(edge.getFromPackage());
            String toLayer = layerMatcher.layerOf(edge.getToPackage());
            String severity = severityFor(rules, fromLayer, toLayer);
            List<String> blastRadius = blastRadiusCalculator.packagesDependingOn(
                    graph,
                    Set.of(edge.getFromPackage())
            );
            violations.add(new ArchitectureViolation(
                    ArchitectureViolation.forbiddenRuleId(fromLayer, toLayer),
                    edge.getFromPackage(),
                    edge.getToPackage(),
                    severity,
                    blastRadius
            ));
        }
        return List.copyOf(violations);
    }

    private static String severityFor(ArchitectureRules rules, String fromLayer, String toLayer) {
        for (ForbiddenEdgeRule rule : rules.getForbidden()) {
            if (rule.getFrom().equals(fromLayer) && rule.getTo().equals(toLayer)) {
                return rule.getSeverity();
            }
        }
        return "HIGH";
    }
}
