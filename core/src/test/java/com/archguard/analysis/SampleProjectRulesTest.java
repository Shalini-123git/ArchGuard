package com.archguard.analysis;

import com.archguard.SampleProjectLocator;
import com.archguard.rules.ArchitectureRules;
import com.archguard.rules.ArchitectureRulesLoader;
import com.archguard.scan.ScanOptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SampleProjectRulesTest {

    @Test
    void sampleProjectHasOneForbiddenEdgeAndOneCycleWithKnownBlastRadius() {
        Path sampleProject = SampleProjectLocator.locate();
        Path rulesFile = sampleProject.getParent().resolve("sample-rules").resolve("archguard-rules.yml");
        ArchitectureRules rules = new ArchitectureRulesLoader().load(rulesFile);
        ScanReport report = new ProjectAnalyzer().analyze(sampleProject, ScanOptions.defaults(), rules);

        assertEquals(2, report.getViolations().size());

        ArchitectureViolation cycle = report.getViolations().stream()
                .filter(violation -> ArchitectureViolation.RULE_NO_CYCLES.equals(violation.getRuleId()))
                .findFirst()
                .orElseThrow();
        assertEquals("com.example.cycle.a", cycle.getFromPackage());
        assertEquals("com.example.cycle.b", cycle.getToPackage());
        assertEquals(0, cycle.getBlastRadiusCount());

        ArchitectureViolation forbidden = report.getViolations().stream()
                .filter(violation -> violation.getRuleId().startsWith("forbidden:"))
                .findFirst()
                .orElseThrow();
        assertEquals("forbidden:controller->repository", forbidden.getRuleId());
        assertEquals("com.example.web", forbidden.getFromPackage());
        assertEquals("com.example.data", forbidden.getToPackage());
        assertEquals(List.of("com.example.app"), forbidden.getBlastRadiusPackages());
        assertTrue(report.getGraph().packages().contains("com.example.web"));
        assertTrue(report.getGraph().packages().contains("com.example.data"));
    }
}
