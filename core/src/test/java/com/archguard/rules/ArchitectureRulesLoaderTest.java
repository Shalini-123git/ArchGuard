package com.archguard.rules;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArchitectureRulesLoaderTest {

    private final ArchitectureRulesLoader loader = new ArchitectureRulesLoader();

    @Test
    void loadsLayersAndForbiddenEdges(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("rules.yml");
        Files.writeString(
                file,
                """
                layers:
                  - name: controller
                    packagePatterns:
                      - com.example.web
                  - name: repository
                    packagePatterns:
                      - com.example.data
                forbidden:
                  - from: controller
                    to: repository
                    severity: HIGH
                noCycles: true
                """
        );
        ArchitectureRules rules = loader.load(file);
        assertEquals(2, rules.getLayers().size());
        assertEquals("controller", rules.getLayers().get(0).getName());
        assertEquals(1, rules.getForbidden().size());
        assertTrue(rules.isNoCycles());
    }

    @Test
    void rejectsForbiddenEdgeForUnknownLayer(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("bad.yml");
        Files.writeString(
                file,
                """
                layers:
                  - name: controller
                    packagePatterns:
                      - com.example.web
                forbidden:
                  - from: controller
                    to: repository
                """
        );
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> loader.load(file));
        assertTrue(exception.getMessage().contains("not defined"));
    }
}
