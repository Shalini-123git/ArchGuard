package com.archguard.cli;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchGuardCliTest {

    @Test
    void scanFindsExactlyTheKnownCycleInSampleProject() {
        Path sampleProject = locateSampleProject();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ArchGuardCli cli = new ArchGuardCli(
                new com.archguard.analysis.ProjectAnalyzer(),
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)
        );

        int exitCode = cli.run(new String[] {"scan", sampleProject.toString()});
        String text = output.toString(StandardCharsets.UTF_8);

        assertEquals(0, exitCode);
        assertTrue(text.contains("Packages: 8"));
        assertTrue(text.contains("Edges: 7"));
        assertTrue(text.contains("Parse failures: 1"));
        assertTrue(text.contains("Cycles: 1"));
        assertTrue(text.contains("com.example.cycle.a"));
        assertTrue(text.contains("com.example.cycle.b"));
        assertTrue(text.contains("com.example.cycle.c"));
        assertTrue(text.contains("Broken.java"));
    }

    @Test
    void rejectsInvalidArguments() {
        ByteArrayOutputStream error = new ByteArrayOutputStream();
        ArchGuardCli cli = new ArchGuardCli(
                new com.archguard.analysis.ProjectAnalyzer(),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8),
                new PrintStream(error, true, StandardCharsets.UTF_8)
        );
        assertEquals(1, cli.run(new String[] {}));
        assertEquals(1, cli.run(new String[] {"scan"}));
        assertEquals(1, cli.run(new String[] {"scan", "does-not-exist"}));
        assertEquals(1, cli.run(new String[] {"scan", sampleRoot().toString(), "--rules"}));
        assertTrue(error.toString(StandardCharsets.UTF_8).contains("Usage:"));
    }

    @Test
    void scanWithRulesPrintsForbiddenViolationAndBlastRadius() {
        Path sampleProject = locateSampleProject();
        Path rules = sampleProject.getParent().resolve("sample-rules").resolve("archguard-rules.yml");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ArchGuardCli cli = new ArchGuardCli(
                new com.archguard.analysis.ProjectAnalyzer(),
                new PrintStream(output, true, StandardCharsets.UTF_8),
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8)
        );
        int exitCode = cli.run(new String[] {"scan", sampleProject.toString(), "--rules", rules.toString()});
        String text = output.toString(StandardCharsets.UTF_8);
        assertEquals(2, exitCode);
        assertTrue(text.contains("Cycles: 1"));
        assertTrue(text.contains("Violations: 2"));
        assertTrue(text.contains("forbidden:controller->repository"));
        assertTrue(text.contains("blastRadius=1"));
        assertTrue(text.contains("no-cycles"));
    }

    private static Path sampleRoot() {
        return locateSampleProject();
    }

    private static Path locateSampleProject() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 6 && directory != null; depth++) {
            Path candidate = directory.resolve("sample-project");
            if (Files.isDirectory(candidate.resolve("src"))) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException("sample-project not found");
    }
}
