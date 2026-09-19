package com.archguard.scan;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Finds Java sources under a folder without following build output or VCS metadata.
 */
public final class ProjectScanner {

    private static final Set<String> SKIPPED_DIRECTORY_NAMES = Set.of(
            ".git", "target", "build", "node_modules"
    );

    public List<Path> findJavaFiles(Path root, ScanOptions options) {
        List<Path> javaFiles = new ArrayList<>();
        try {
            Files.walkFileTree(root, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) {
                    if (SKIPPED_DIRECTORY_NAMES.contains(directory.getFileName().toString())) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) {
                    if (isJavaSource(file) && (options.includeTestSources() || !isTestSource(root, file))) {
                        javaFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read project folder: " + root, exception);
        }
        return javaFiles;
    }

    private static boolean isJavaSource(Path file) {
        String fileName = file.getFileName().toString().toLowerCase(Locale.ROOT);
        if ("module-info.java".equals(fileName)) {
            return false;
        }
        return fileName.endsWith(".java");
    }

    /**
     * Maven/Gradle test trees live under a src/test segment. We skip them unless the caller asks.
     */
    static boolean isTestSource(Path root, Path file) {
        Path relative = root.toAbsolutePath().normalize().relativize(file.toAbsolutePath().normalize());
        int nameCount = relative.getNameCount();
        for (int index = 0; index < nameCount - 1; index++) {
            boolean src = "src".equals(relative.getName(index).toString());
            boolean test = "test".equals(relative.getName(index + 1).toString());
            if (src && test) {
                return true;
            }
        }
        return false;
    }
}
