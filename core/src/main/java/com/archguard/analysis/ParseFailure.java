package com.archguard.analysis;

import java.nio.file.Path;
import java.util.Objects;

/**
 * A Java file that JavaParser could not read. Counted in the scan report and skipped in the graph.
 */
public final class ParseFailure {

    private final Path file;
    private final String message;

    public ParseFailure(Path file, String message) {
        this.file = Objects.requireNonNull(file, "file");
        this.message = Objects.requireNonNull(message, "message");
    }

    public Path getFile() {
        return file;
    }

    public String getMessage() {
        return message;
    }
}
