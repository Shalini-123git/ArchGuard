package com.archguard;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SampleProjectLocator {

    private SampleProjectLocator() {
    }

    public static Path locate() {
        Path directory = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        for (int depth = 0; depth < 6 && directory != null; depth++) {
            Path candidate = directory.resolve("sample-project");
            if (Files.isDirectory(candidate.resolve("src"))) {
                return candidate;
            }
            directory = directory.getParent();
        }
        throw new IllegalStateException(
                "sample-project not found starting from " + System.getProperty("user.dir")
        );
    }
}
