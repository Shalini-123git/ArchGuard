package com.archguard.graph;

import com.archguard.parse.ParsedJavaFile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DependencyGraphBuilderTest {

    private final DependencyGraphBuilder builder = new DependencyGraphBuilder();

    @Test
    void keepsOnlyInternalPackageEdges() {
        ParsedJavaFile file = parsed(
                "com.example.app",
                List.of("com.example.tools", "java.util", "com.thirdparty.lib")
        );
        PackageDependencyGraph graph = builder.build(
                Set.of("com.example.app", "com.example.tools"),
                List.of(file)
        );

        assertEquals(2, graph.packageCount());
        assertEquals(1, graph.edgeCount());
        assertTrue(graph.dependencies().contains(new PackageDependency("com.example.app", "com.example.tools")));
        assertFalse(graph.packages().contains("java.util"));
        assertFalse(graph.packages().contains("com.thirdparty.lib"));
    }

    @Test
    void ignoresSamePackageImports() {
        ParsedJavaFile file = parsed("com.example.app", List.of("com.example.app"));
        PackageDependencyGraph graph = builder.build(Set.of("com.example.app"), List.of(file));
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void ignoresJdkImportsEvenIfNamedLikeAProjectPackage() {
        ParsedJavaFile file = parsed("com.example.app", List.of("java.util", "javax.sql"));
        PackageDependencyGraph graph = builder.build(
                Set.of("com.example.app", "java.util"),
                List.of(file)
        );
        assertEquals(0, graph.edgeCount());
    }

    private static ParsedJavaFile parsed(String packageName, List<String> importedPackages) {
        return new ParsedJavaFile(Path.of(packageName + ".java"), packageName, importedPackages);
    }
}
