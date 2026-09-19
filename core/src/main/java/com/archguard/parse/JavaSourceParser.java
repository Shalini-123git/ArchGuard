package com.archguard.parse;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseResult;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.PackageDeclaration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Extracts package and import information with JavaParser. Never compiles or runs the file.
 */
public final class JavaSourceParser implements LanguageParser {

    private static final String DEFAULT_PACKAGE = "";

    private final JavaParser javaParser;

    public JavaSourceParser() {
        ParserConfiguration configuration = new ParserConfiguration();
        configuration.setCharacterEncoding(StandardCharsets.UTF_8);
        this.javaParser = new JavaParser(configuration);
    }

    @Override
    public com.archguard.parse.ParseResult parse(Path file) {
        try {
            ParseResult<CompilationUnit> parseResult = javaParser.parse(file);
            if (!parseResult.isSuccessful() || parseResult.getResult().isEmpty()) {
                return com.archguard.parse.ParseResult.failure(describeProblems(parseResult));
            }
            CompilationUnit unit = parseResult.getResult().get();
            String packageName = unit.getPackageDeclaration()
                    .map(PackageDeclaration::getNameAsString)
                    .orElse(DEFAULT_PACKAGE);
            List<String> importedPackages = new ArrayList<>();
            for (ImportDeclaration importDeclaration : unit.getImports()) {
                importedPackages.add(importedPackage(importDeclaration));
            }
            ParsedJavaFile parsedFile = new ParsedJavaFile(file, packageName, importedPackages);
            return com.archguard.parse.ParseResult.success(parsedFile);
        } catch (IOException exception) {
            return com.archguard.parse.ParseResult.failure(exception.getMessage());
        }
    }

    /**
     * Maps an import to a package: wildcards keep the given name, type imports drop the type,
     * static imports drop the member (and the type).
     */
    static String importedPackage(ImportDeclaration importDeclaration) {
        String name = importDeclaration.getNameAsString();
        if (importDeclaration.isAsterisk() && !importDeclaration.isStatic()) {
            return name;
        }
        if (importDeclaration.isStatic() && importDeclaration.isAsterisk()) {
            return dropLastSegment(name);
        }
        if (importDeclaration.isStatic()) {
            return dropLastSegment(dropLastSegment(name));
        }
        return dropLastSegment(name);
    }

    private static String dropLastSegment(String qualifiedName) {
        int lastDot = qualifiedName.lastIndexOf('.');
        if (lastDot < 0) {
            return "";
        }
        return qualifiedName.substring(0, lastDot);
    }

    private static String describeProblems(ParseResult<CompilationUnit> parseResult) {
        if (parseResult.getProblems().isEmpty()) {
            return "Unable to parse Java source";
        }
        return parseResult.getProblems().get(0).getVerboseMessage();
    }
}
