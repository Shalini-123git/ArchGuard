package com.archguard.scan;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProjectScannerTest {

    private final ProjectScanner scanner = new ProjectScanner();

    @Test
    void skipsBuildDirectoriesAndTestSourcesByDefault(@TempDir Path root) throws Exception {
        Path main = root.resolve("src/main/java/Main.java");
        Path test = root.resolve("src/test/java/MainTest.java");
        Path inTarget = root.resolve("target/generated/Gen.java");
        Path inGit = root.resolve(".git/hooks/Hook.java");
        Files.createDirectories(main.getParent());
        Files.createDirectories(test.getParent());
        Files.createDirectories(inTarget.getParent());
        Files.createDirectories(inGit.getParent());
        Files.writeString(main, "class Main {}");
        Files.writeString(test, "class MainTest {}");
        Files.writeString(inTarget, "class Gen {}");
        Files.writeString(inGit, "class Hook {}");

        List<Path> files = scanner.findJavaFiles(root, ScanOptions.defaults());
        assertEquals(1, files.size());
        assertTrue(files.get(0).endsWith("Main.java"));
    }

    @Test
    void includesTestSourcesWhenAsked(@TempDir Path root) throws Exception {
        Path test = root.resolve("src/test/java/MainTest.java");
        Files.createDirectories(test.getParent());
        Files.writeString(test, "class MainTest {}");

        List<Path> files = scanner.findJavaFiles(root, ScanOptions.includingTestSources());
        assertEquals(1, files.size());
    }

    @Test
    void excludesConfiguredDirectory(@TempDir Path root) throws Exception {
        Path included = root.resolve("src/main/java/Main.java");
        Path excludedDirectory = root.resolve("sample-project/src/main/java");
        Path excluded = excludedDirectory.resolve("Broken.java");
        Files.createDirectories(included.getParent());
        Files.createDirectories(excludedDirectory);
        Files.writeString(included, "class Main {}");
        Files.writeString(excluded, "this is not valid Java");

        List<Path> files = scanner.findJavaFiles(root, ScanOptions.defaults().excluding(root.resolve("sample-project")));
        assertEquals(List.of(included), files);
    }
}
