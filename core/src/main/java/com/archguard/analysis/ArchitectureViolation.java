package com.archguard.analysis;

import java.util.List;
import java.util.Objects;

/**
 * One architecture problem with the facts needed later for an LLM prompt. No generated prose.
 */
public final class ArchitectureViolation {

    public static final String RULE_NO_CYCLES = "no-cycles";

    private final String ruleId;
    private final String fromPackage;
    private final String toPackage;
    private final String severity;
    private final List<String> blastRadiusPackages;

    public ArchitectureViolation(
            String ruleId,
            String fromPackage,
            String toPackage,
            String severity,
            List<String> blastRadiusPackages
    ) {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
        this.fromPackage = Objects.requireNonNull(fromPackage, "fromPackage");
        this.toPackage = Objects.requireNonNull(toPackage, "toPackage");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.blastRadiusPackages = List.copyOf(blastRadiusPackages);
    }

    public String getRuleId() {
        return ruleId;
    }

    public String getFromPackage() {
        return fromPackage;
    }

    public String getToPackage() {
        return toPackage;
    }

    public String getSeverity() {
        return severity;
    }

    public List<String> getBlastRadiusPackages() {
        return blastRadiusPackages;
    }

    public int getBlastRadiusCount() {
        return blastRadiusPackages.size();
    }

    public static String forbiddenRuleId(String fromLayer, String toLayer) {
        return "forbidden:" + fromLayer + "->" + toLayer;
    }
}
