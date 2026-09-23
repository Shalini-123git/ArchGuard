package com.archguard.analysis;

import com.archguard.graph.DependencyGraphBuilder;
import com.archguard.graph.PackageDependencyGraph;
import com.archguard.parse.ParsedJavaFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlastRadiusCalculatorTest {

    private final DependencyGraphBuilder graphBuilder = new DependencyGraphBuilder();
    private final BlastRadiusCalculator calculator = new BlastRadiusCalculator();

    @Test
    void countsTransitiveDependentsAndExcludesTheSeed() {
        PackageDependencyGraph graph = graphBuilder.build(
                Set.of("app", "web", "data"),
                List.of(
                        parsed("app", "web"),
                        parsed("web", "data")
                )
        );
        assertEquals(List.of("app", "web"), calculator.packagesDependingOn(graph, List.of("data")));
        assertEquals(List.of("app"), calculator.packagesDependingOn(graph, List.of("web")));
        assertEquals(List.of(), calculator.packagesDependingOn(graph, List.of("app")));
    }

    private static ParsedJavaFile parsed(String fromPackage, String toPackage) {
        return new ParsedJavaFile(Path.of(fromPackage + ".java"), fromPackage, List.of(toPackage));
    }
}
