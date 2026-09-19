package com.archguard.cycle;

import com.archguard.graph.DependencyGraphBuilder;
import com.archguard.graph.PackageDependencyGraph;
import com.archguard.parse.ParsedJavaFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CycleDetectorTest {

    private final DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder();
    private final CycleDetector cycleDetector = new CycleDetector();

    @Test
    void reportsEachSccLargerThanOnePackage() {
        PackageDependencyGraph graph = graphBuilder.build(
                Set.of("a", "b", "c", "d"),
                List.of(
                        parsed("a", "b"),
                        parsed("b", "c"),
                        parsed("c", "a"),
                        parsed("d", "a")
                )
        );

        List<DependencyCycle> cycles = cycleDetector.detect(graph);
        assertEquals(1, cycles.size());
        assertEquals(List.of("a", "b", "c"), cycles.get(0).getPackages());
    }

    @Test
    void reportsNoCycleWhenTheGraphIsADag() {
        PackageDependencyGraph graph = graphBuilder.build(
                Set.of("a", "b"),
                List.of(parsed("a", "b"))
        );
        assertTrue(cycleDetector.detect(graph).isEmpty());
    }

    private static ParsedJavaFile parsed(String fromPackage, String toPackage) {
        return new ParsedJavaFile(Path.of(fromPackage + ".java"), fromPackage, List.of(toPackage));
    }
}
