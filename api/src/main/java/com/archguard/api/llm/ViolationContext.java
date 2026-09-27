package com.archguard.api.llm;

import java.util.List;

/** Bounded deterministic facts supplied to an LLM; no source code is interpreted as instructions. */
public record ViolationContext(String ruleDescription, String fromPackage, String toPackage,
                               List<String> blastRadiusPackages, List<String> offendingImportLines) {
    public ViolationContext {
        blastRadiusPackages = List.copyOf(blastRadiusPackages);
        offendingImportLines = List.copyOf(offendingImportLines);
    }
}
