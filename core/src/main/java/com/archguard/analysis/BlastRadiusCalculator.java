package com.archguard.analysis;

import com.archguard.graph.PackageDependencyGraph;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.EdgeReversedGraph;
import org.jgrapht.traverse.BreadthFirstIterator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Packages that depend on a violation, directly or transitively (reverse reachability).
 */
public final class BlastRadiusCalculator {

    public List<String> packagesDependingOn(PackageDependencyGraph graph, Collection<String> seeds) {
        Set<String> seedSet = new LinkedHashSet<>(seeds);
        Graph<String, DefaultEdge> reversed = new EdgeReversedGraph<>(graph.jgrapht());
        Set<String> reached = new LinkedHashSet<>();
        for (String seed : seedSet) {
            if (!reversed.containsVertex(seed)) {
                continue;
            }
            BreadthFirstIterator<String, DefaultEdge> walk = new BreadthFirstIterator<>(reversed, seed);
            while (walk.hasNext()) {
                String packageName = walk.next();
                if (!seedSet.contains(packageName)) {
                    reached.add(packageName);
                }
            }
        }
        List<String> ordered = new ArrayList<>(reached);
        ordered.sort(Comparator.naturalOrder());
        return List.copyOf(ordered);
    }
}
