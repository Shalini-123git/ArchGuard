package com.archguard.scan;

/**
 * Options that change which files are walked. Defaults exclude test sources.
 */
public final class ScanOptions {

    private final boolean includeTestSources;

    private ScanOptions(boolean includeTestSources) {
        this.includeTestSources = includeTestSources;
    }

    public static ScanOptions defaults() {
        return new ScanOptions(false);
    }

    public static ScanOptions includingTestSources() {
        return new ScanOptions(true);
    }

    public boolean includeTestSources() {
        return includeTestSources;
    }
}
