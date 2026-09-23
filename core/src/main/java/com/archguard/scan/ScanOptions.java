package com.archguard.scan;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Options that change which files are walked. Defaults exclude test sources.
 */
public final class ScanOptions {

    private final boolean includeTestSources;
    private final Set<Path> excludedPaths;

    private ScanOptions(boolean includeTestSources, Set<Path> excludedPaths) {
        this.includeTestSources = includeTestSources;
        this.excludedPaths = Set.copyOf(excludedPaths);
    }

    public static ScanOptions defaults() {
        return new ScanOptions(false, Set.of());
    }

    public static ScanOptions includingTestSources() {
        return new ScanOptions(true, Set.of());
    }

    public boolean includeTestSources() {
        return includeTestSources;
    }

    public ScanOptions excluding(Path path) {
        Set<Path> paths = new LinkedHashSet<>(excludedPaths);
        paths.add(path.toAbsolutePath().normalize());
        return new ScanOptions(includeTestSources, paths);
    }

    Set<Path> excludedPaths() {
        return excludedPaths;
    }
}
