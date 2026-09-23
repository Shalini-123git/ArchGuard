package com.archguard.parse;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.expr.Name;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    void extractsPackageAndSimpleImport(@TempDir Path tempDir) throws Exception {
        Path file = write(
                tempDir,
                "Simple.java",
                """
                package com.example.app;
                import com.example.tools.ToolBox;
                class Simple {}
                """
        );
        ParseResult result = parser.parse(file);
        assertTrue(result.isSuccessful());
        assertEquals("com.example.app", result.getParsedFile().getPackageName());
        assertEquals(List.of("com.example.tools"), result.getParsedFile().getImportedPackages());
    }

    @Test
    void parsesJava17Syntax(@TempDir Path tempDir) throws Exception {
        Path file = write(
                tempDir,
                "Modern.java",
                "record Modern(String value) {}\n"
                        + "class Usage { boolean matches(Object value) { return value instanceof Modern modern; } }"
        );
        ParseResult result = parser.parse(file);
        assertTrue(result.isSuccessful());
    }

    @Test
    void mapsWildcardImportToPackage(@TempDir Path tempDir) throws Exception {
        Path file = write(
                tempDir,
                "Wild.java",
                """
                package com.example.app;
                import com.example.tools.*;
                class Wild {}
                """
        );
        ParseResult result = parser.parse(file);
        assertEquals(List.of("com.example.tools"), result.getParsedFile().getImportedPackages());
    }

    @Test
    void mapsStaticImportToTypePackage(@TempDir Path tempDir) throws Exception {
        Path file = write(
                tempDir,
                "StaticUse.java",
                """
                package com.example.app;
                import static com.example.util.Numbers.ZERO;
                class StaticUse {}
                """
        );
        ParseResult result = parser.parse(file);
        assertEquals(List.of("com.example.util"), result.getParsedFile().getImportedPackages());
    }

    @Test
    void mapsStaticWildcardImportToTypePackage() {
        ImportDeclaration importDeclaration = new ImportDeclaration(new Name("com.example.util.Numbers"), true, true);
        assertEquals("com.example.util", JavaSourceParser.importedPackage(importDeclaration));
    }

    @Test
    void skipsUnparsableFileAndReportsFailure(@TempDir Path tempDir) throws Exception {
        Path file = write(tempDir, "Broken.java", "this is not valid Java {{{");
        ParseResult result = parser.parse(file);
        assertFalse(result.isSuccessful());
        assertFalse(result.getErrorMessage().isBlank());
    }

    @Test
    void usesEmptyPackageWhenDeclarationIsMissing(@TempDir Path tempDir) throws Exception {
        Path file = write(tempDir, "DefaultPkg.java", "class DefaultPkg {}");
        ParseResult result = parser.parse(file);
        assertTrue(result.isSuccessful());
        assertEquals("", result.getParsedFile().getPackageName());
    }

    private static Path write(Path directory, String fileName, String contents) throws Exception {
        Path file = directory.resolve(fileName);
        Files.writeString(file, contents);
        return file;
    }
}
