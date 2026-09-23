package com.archguard.rules;

/**
 * Maps a Java package to a layer using first-match prefix patterns.
 */
public final class LayerMatcher {

    public static final String UNKNOWN = "unknown";

    private final ArchitectureRules rules;

    public LayerMatcher(ArchitectureRules rules) {
        this.rules = rules;
    }

    public String layerOf(String packageName) {
        for (LayerRule layer : rules.getLayers()) {
            for (String pattern : layer.getPackagePatterns()) {
                if (matches(packageName, pattern)) {
                    return layer.getName();
                }
            }
        }
        return UNKNOWN;
    }

    /**
     * A pattern matches the package itself or any nested package (com.example.web → com.example.web.api).
     */
    static boolean matches(String packageName, String pattern) {
        if (pattern == null || pattern.isBlank()) {
            return false;
        }
        return packageName.equals(pattern) || packageName.startsWith(pattern + ".");
    }
}
