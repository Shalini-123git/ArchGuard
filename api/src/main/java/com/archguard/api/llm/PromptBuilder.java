package com.archguard.api.llm;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Renders the fixed safety-focused prompt from a bounded violation context. */
@Component
public class PromptBuilder {
    private final String template;

    public PromptBuilder() {
        try (var stream = new ClassPathResource("prompts/violation-explanation.txt").getInputStream()) {
            template = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not load the violation explanation prompt", exception);
        }
    }

    public String build(ViolationContext context) {
        return template.replace("{{ruleDescription}}", context.ruleDescription())
                .replace("{{fromPackage}}", context.fromPackage())
                .replace("{{toPackage}}", context.toPackage())
                .replace("{{blastRadiusPackages}}", String.join(", ", context.blastRadiusPackages()))
                .replace("{{snippet}}", String.join(System.lineSeparator(), context.offendingImportLines()));
    }
}
