package com.archguard.rules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LayerMatcherTest {

    @Test
    void firstMatchingPrefixWins() {
        ArchitectureRules rules = new ArchitectureRules();
        LayerRule controller = new LayerRule();
        controller.setName("controller");
        controller.setPackagePatterns(java.util.List.of("com.example.web"));
        LayerRule repository = new LayerRule();
        repository.setName("repository");
        repository.setPackagePatterns(java.util.List.of("com.example.data"));
        rules.setLayers(java.util.List.of(controller, repository));

        LayerMatcher matcher = new LayerMatcher(rules);
        assertEquals("controller", matcher.layerOf("com.example.web"));
        assertEquals("controller", matcher.layerOf("com.example.web.api"));
        assertEquals("repository", matcher.layerOf("com.example.data"));
        assertEquals(LayerMatcher.UNKNOWN, matcher.layerOf("com.example.app"));
    }

    @Test
    void prefixDoesNotMatchASiblingPackage() {
        assertTrue(LayerMatcher.matches("com.example.web", "com.example.web"));
        assertFalse(LayerMatcher.matches("com.example.webextra", "com.example.web"));
    }
}
