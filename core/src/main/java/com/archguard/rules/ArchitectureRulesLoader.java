package com.archguard.rules;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Reads YAML rules and rejects files that would silently ignore misconfiguration.
 */
public final class ArchitectureRulesLoader {

    private final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());

    public ArchitectureRules load(Path file) {
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Rules file does not exist: " + file);
        }
        try {
            ArchitectureRules rules = objectMapper.readValue(file.toFile(), ArchitectureRules.class);
            if (rules == null) {
                throw new IllegalArgumentException("Rules file is empty: " + file);
            }
            validate(rules, file);
            return rules;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read rules file " + file + ": " + exception.getMessage(), exception);
        }
    }

    /** Loads rule text supplied by an API caller without requiring a temporary rules file. */
    public ArchitectureRules load(String yamlText) {
        if (yamlText == null || yamlText.isBlank()) {
            throw new IllegalArgumentException("Rules YAML is empty");
        }
        Path source = Path.of("request-rules.yml");
        try {
            ArchitectureRules rules = objectMapper.readValue(yamlText, ArchitectureRules.class);
            if (rules == null) {
                throw new IllegalArgumentException("Rules YAML is empty");
            }
            validate(rules, source);
            return rules;
        } catch (IOException exception) {
            throw new IllegalArgumentException("Could not read rules YAML: " + exception.getMessage(), exception);
        }
    }

    private static void validate(ArchitectureRules rules, Path file) {
        Set<String> layerNames = new HashSet<>();
        for (LayerRule layer : rules.getLayers()) {
            if (layer.getName() == null || layer.getName().isBlank()) {
                throw new IllegalArgumentException("Every layer in " + file + " must have a name");
            }
            if (!layerNames.add(layer.getName())) {
                throw new IllegalArgumentException("Duplicate layer name '" + layer.getName() + "' in " + file);
            }
        }
        for (ForbiddenEdgeRule forbidden : rules.getForbidden()) {
            if (forbidden.getFrom() == null || forbidden.getTo() == null) {
                throw new IllegalArgumentException("Each forbidden edge in " + file + " needs from and to layer names");
            }
            if (!layerNames.contains(forbidden.getFrom()) || !layerNames.contains(forbidden.getTo())) {
                throw new IllegalArgumentException(
                        "Forbidden edge " + forbidden.getFrom() + " -> " + forbidden.getTo()
                                + " refers to a layer that is not defined in " + file
                );
            }
        }
    }
}
