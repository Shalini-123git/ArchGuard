package com.archguard.parse;

/**
 * Outcome of parsing one source file. Failed parses are skipped by the scanner, not thrown.
 */
public final class ParseResult {

    private final ParsedJavaFile parsedFile;
    private final String errorMessage;

    private ParseResult(ParsedJavaFile parsedFile, String errorMessage) {
        this.parsedFile = parsedFile;
        this.errorMessage = errorMessage;
    }

    public static ParseResult success(ParsedJavaFile parsedFile) {
        return new ParseResult(parsedFile, null);
    }

    public static ParseResult failure(String errorMessage) {
        return new ParseResult(null, errorMessage);
    }

    public boolean isSuccessful() {
        return parsedFile != null;
    }

    public ParsedJavaFile getParsedFile() {
        return parsedFile;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
