package com.archguard.cycle;

import com.archguard.graph.PackageDependencyGraph;
import org.jgrapht.alg.connectivity.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Finds circular package dependencies using strongly connected components.
 */
public final class CycleDetector {

    public List<DependencyCycle> detect(PackageDependencyGraph graph) {
        KosarajuStrongConnectivityInspector<String, DefaultEdge> inspector =
                new KosarajuStrongConnectivityInspector<>(graph.jgrapht());
        List<DependencyCycle> cycles = new ArrayList<>();
        for (Set<String> component : inspector.stronglyConnectedSets()) {
            if (component.size() <= 1) {
                continue;
            }
            List<String> packages = new ArrayList<>(component);
            packages.sort(Comparator.naturalOrder());
            cycles.add(new DependencyCycle(packages));
        }
        cycles.sort(Comparator.comparing(cycle -> String.join(",", cycle.getPackages())));
        return List.copyOf(cycles);
    }
}
