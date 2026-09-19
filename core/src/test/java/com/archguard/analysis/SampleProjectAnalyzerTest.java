package com.archguard.analysis;

import com.archguard.SampleProjectLocator;
import com.archguard.cycle.DependencyCycle;
import com.archguard.graph.PackageDependency;
import com.archguard.scan.ScanOptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleProjectAnalyzerTest {

    private final ProjectAnalyzer analyzer = new ProjectAnalyzer();

    @Test
    void findsTheKnownCycleAndSkipsTheUnparsableFile() {
        Path sampleProject = SampleProjectLocator.locate();
        ScanReport report = analyzer.analyze(sampleProject, ScanOptions.defaults());

        assertEquals(6, report.packageCount());
        assertEquals(5, report.edgeCount());
        assertEquals(1, report.parseFailureCount());
        assertTrue(report.getParseFailures().get(0).getFile().toString().endsWith("Broken.java"));

        assertTrue(report.getGraph().dependencies().contains(
                new PackageDependency("com.example.app", "com.example.tools")
        ));
        assertTrue(report.getGraph().dependencies().contains(
                new PackageDependency("com.example.app", "com.example.util")
        ));

        assertEquals(1, report.getCycles().size());
        DependencyCycle cycle = report.getCycles().get(0);
        assertEquals(
                List.of("com.example.cycle.a", "com.example.cycle.b", "com.example.cycle.c"),
                cycle.getPackages()
        );
    }

    @Test
    void includeTestsAddsASecondCycleThroughTheTestSource() {
        Path sampleProject = SampleProjectLocator.locate();
        ScanReport report = analyzer.analyze(sampleProject, ScanOptions.includingTestSources());
        assertEquals(2, report.getCycles().size());
    }
}
