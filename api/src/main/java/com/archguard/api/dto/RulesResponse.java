package com.archguard.api.dto;

import java.util.List;

public record RulesResponse(boolean custom, String rulesYaml, RulesView rules) {
    public record RulesView(List<Layer> layers, List<Forbidden> forbidden, boolean noCycles) { }
    public record Layer(String name, List<String> packagePatterns) { }
    public record Forbidden(String from, String to, String severity) { }
}
