package com.archguard.graph;

import com.archguard.parse.ParsedJavaFile;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import java.util.Collection;
import java.util.Set;

/**
 * Builds a package graph from parsed files. Only imports that match packages in this project are kept.
 */
public final class DependencyGraphBuilder {

    public PackageDependencyGraph build(Set<String> projectPackages, Collection<ParsedJavaFile> parsedFiles) {
        Graph<String, DefaultEdge> graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        for (String projectPackage : projectPackages) {
            graph.addVertex(projectPackage);
        }
        for (ParsedJavaFile parsedFile : parsedFiles) {
            String fromPackage = parsedFile.getPackageName();
            for (String importedPackage : parsedFile.getImportedPackages()) {
                if (!isInternalDependency(fromPackage, importedPackage, projectPackages)) {
                    continue;
                }
                graph.addEdge(fromPackage, importedPackage);
            }
        }
        return new PackageDependencyGraph(graph);
    }

    private static boolean isInternalDependency(
            String fromPackage,
            String importedPackage,
            Set<String> projectPackages
    ) {
        if (fromPackage.equals(importedPackage)) {
            return false;
        }
        if (isJdkPackage(importedPackage)) {
            return false;
        }
        return projectPackages.contains(importedPackage);
    }

    private static boolean isJdkPackage(String importedPackage) {
        return importedPackage.equals("java")
                || importedPackage.equals("javax")
                || importedPackage.startsWith("java.")
                || importedPackage.startsWith("javax.");
    }
}
