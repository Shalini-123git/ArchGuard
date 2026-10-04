package com.archguard.api.llm;

import com.archguard.api.config.ArchGuardProperties;
import com.archguard.api.persistence.ExplanationCacheEntity;
import com.archguard.api.persistence.ViolationEntity;
import com.archguard.api.repository.ExplanationCacheRepository;
import com.archguard.api.repository.ViolationJpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Explains a bounded number of persisted violations and caches provider results. */
@Service
public class ViolationExplanationService {
    private static final Logger log = LoggerFactory.getLogger(ViolationExplanationService.class);
    private final ViolationJpaRepository violations;
    private final ExplanationCacheRepository cache;
    private final LlmClient llmClient;
    private final ArchGuardProperties.Llm properties;

    public ViolationExplanationService(ViolationJpaRepository violations, ExplanationCacheRepository cache,
                                       LlmClient llmClient, ArchGuardProperties properties) {
        this.violations = violations; this.cache = cache; this.llmClient = llmClient; this.properties = properties.getLlm();
    }

    @Transactional
    public void explainScan(java.util.UUID scanId, Path projectRoot) {
        List<ViolationEntity> selected = violations.findWithAffectedModulesByScanId(scanId).stream()
                .sorted(Comparator.comparing(ViolationEntity::getRuleId).thenComparing(v -> moduleName(v.getSourceModule())))
                .limit(properties.getViolationsPerScanLimit()).toList();
        for (int index = 0; index < selected.size(); index++) {
            ViolationEntity violation = selected.get(index);
            ViolationContext context = context(violation, projectRoot);
            String hash = hash(context);
            ExplanationResult result = cache.findById(hash)
                    .filter(value -> !value.isFallback())
                    .map(value -> new ExplanationResult(value.getExplanation(), false))
                    .orElseGet(() -> createAndCache(hash, context, violation));
            violation.setExplanation(result.text(), result.fallback());
            violations.save(violation);
            if (index < selected.size() - 1) sleepBetweenCalls();
        }
    }

    ViolationContext context(ViolationEntity violation, Path root) {
        String from = moduleName(violation.getSourceModule());
        String to = moduleName(violation.getTargetModule());
        List<String> blastRadius = violation.getAffectedModules().stream().map(module -> module.getName()).sorted()
                .limit(properties.getBlastRadiusLimit()).toList();
        return new ViolationContext(ruleDescription(violation), from, to, blastRadius, importLines(root, to));
    }

    private ExplanationResult createAndCache(String hash, ViolationContext context, ViolationEntity violation) {
        ExplanationResult result;
        try {
            result = new ExplanationResult(llmClient.explain(context), false);
        } catch (RuntimeException exception) {
            log.warn("LLM explanation failed for violation id={} ruleId={}: {}",
                    violation.getId(), violation.getRuleId(), exception.getMessage());
            result = new ExplanationResult(fallback(context), true);
        }
        if (!result.fallback()) cache.save(new ExplanationCacheEntity(hash, result.text(), false));
        return result;
    }

    private void sleepBetweenCalls() {
        if (properties.getDelayBetweenCallsMs() <= 0) return;
        try {
            Thread.sleep(properties.getDelayBetweenCallsMs());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM explanation delay interrupted", exception);
        }
    }

    private List<String> importLines(Path root, String toPackage) {
        if (toPackage.isBlank() || !Files.isDirectory(root)) return List.of("No offending import line was available.");
        try (var paths = Files.walk(root)) {
            List<String> lines = paths.filter(path -> path.toString().endsWith(".java")).sorted().flatMap(path -> readLines(path).stream())
                    .filter(line -> line.trim().startsWith("import ") && line.contains(toPackage))
                    .limit(properties.getSnippetLineLimit()).toList();
            return lines.isEmpty() ? List.of("No offending import line was available.") : lines;
        } catch (IOException exception) {
            return List.of("No offending import line was available.");
        }
    }

    private List<String> readLines(Path file) { try { return Files.readAllLines(file, StandardCharsets.UTF_8); } catch (IOException exception) { return List.of(); } }
    private String ruleDescription(ViolationEntity violation) { return "Architecture rule " + violation.getRuleId() + " (severity " + violation.getSeverity() + ")"; }
    private String moduleName(com.archguard.api.persistence.ModuleEntity module) { return module == null ? "Not applicable" : module.getName(); }
    private String fallback(ViolationContext context) {
        return "Why it is a problem: " + context.ruleDescription() + " involves " + context.fromPackage() + " and " + context.toPackage()
                + ". What could break: packages depending on this area may be affected; exact impact is uncertain. Suggested fix: remove or invert the dependency to comply with the stated rule.";
    }
    String hash(ViolationContext context) {
        try {
            String facts = String.join("\n", context.ruleDescription(), context.fromPackage(), context.toPackage(),
                    String.join("\n", context.blastRadiusPackages()), String.join("\n", context.offendingImportLines()));
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(facts.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(); for (byte byteValue : digest) value.append(String.format(Locale.ROOT, "%02x", byteValue)); return value.toString();
        } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }
}
