package com.archguard.rules;

import java.util.ArrayList;
import java.util.List;

/**
 * A named layer and the package prefixes that belong to it.
 */
public final class LayerRule {

    private String name;
    private List<String> packagePatterns = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getPackagePatterns() {
        return packagePatterns;
    }

    public void setPackagePatterns(List<String> packagePatterns) {
        this.packagePatterns = packagePatterns == null ? new ArrayList<>() : packagePatterns;
    }
}
