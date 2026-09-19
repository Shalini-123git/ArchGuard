package com.archguard.parse;

import java.nio.file.Path;

/**
 * Reads a source file without compiling or executing it.
 * Implementations exist so the language can be swapped later.
 */
public interface LanguageParser {

    ParseResult parse(Path file);
}
