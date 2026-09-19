package com.archguard.parse;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Package name and imported packages taken from one Java file.
 */
public final class ParsedJavaFile {

    private final Path file;
    private final String packageName;
    private final List<String> importedPackages;

    public ParsedJavaFile(Path file, String packageName, List<String> importedPackages) {
        this.file = Objects.requireNonNull(file, "file");
        this.packageName = Objects.requireNonNull(packageName, "packageName");
        this.importedPackages = List.copyOf(importedPackages);
    }

    public Path getFile() {
        return file;
    }

    public String getPackageName() {
        return packageName;
    }

    public List<String> getImportedPackages() {
        return importedPackages;
    }
}
