package com.archguard.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScanControllerIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void localSampleProjectScanPersistsGraphAndViolations() throws Exception {
        String sampleProject = Path.of("..", "sample-project").toAbsolutePath().normalize().toString();
        String request = objectMapper.createObjectNode()
                .put("localPath", sampleProject)
                .put("rulesYaml", "layers:\n  - name: app\n    packagePatterns:\n      - com.example.app\n  - name: web\n    packagePatterns:\n      - com.example.web\n  - name: repository\n    packagePatterns:\n      - com.example.data\nforbidden:\n  - from: web\n    to: repository\nnoCycles: true\n")
                .toString();
        String response = mockMvc.perform(post("/api/scans").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("QUEUED"))
                .andReturn().getResponse().getContentAsString();
        String scanId = objectMapper.readTree(response).get("id").asText();

        JsonNode scan = waitForCompletion(scanId);
        assertTrue("COMPLETED".equals(scan.get("status").asText()), () -> scan.toString());
        assertTrue(scan.get("healthScore").isInt(), () -> scan.toString());
        mockMvc.perform(get("/api/scans/{id}/graph", scanId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.modules.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.modules[0].layer").exists())
                .andExpect(jsonPath("$.dependencies.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.dependencies[0].violationIds").exists());
        mockMvc.perform(get("/api/scans/{id}/violations", scanId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$[0].explanation").isNotEmpty())
                .andExpect(jsonPath("$[0].explanationFallback").value(true));
    }

    @Test
    void localSampleProjectScanWithoutRulesDetectsCycles() throws Exception {
            String sampleProject = Path.of("..", "sample-project").toAbsolutePath().normalize().toString();
            String request = objectMapper.createObjectNode().put("localPath", sampleProject).toString();
        String response = mockMvc.perform(post("/api/scans").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
            String scanId = objectMapper.readTree(response).get("id").asText();

        JsonNode scan = waitForCompletion(scanId);
        assertTrue("COMPLETED".equals(scan.get("status").asText()), () -> scan.toString());
        assertTrue(scan.get("healthScore").asInt() < 100, () -> scan.toString());
        mockMvc.perform(get("/api/scans/{id}/violations", scanId)).andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.ruleId == 'no-cycles')].severity").value(org.hamcrest.Matchers.hasItem("HIGH")));
    }

    @Test
    void rejectsNonHttpsRemoteUrl() throws Exception {
        mockMvc.perform(post("/api/scans").contentType(MediaType.APPLICATION_JSON).content("{\"repoUrl\":\"file:///tmp/repository\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("repoUrl must be an HTTPS repository URL"));
    }

    @Test
    void returnsDefaultRulesWhenScanHasNoRules() throws Exception {
        String scanId = queueLocalScan(null);
        waitForCompletion(scanId);
        mockMvc.perform(get("/api/scans/{id}/rules", scanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(false))
                .andExpect(jsonPath("$.rulesYaml").value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.rules.layers").isEmpty())
                .andExpect(jsonPath("$.rules.forbidden").isEmpty())
                .andExpect(jsonPath("$.rules.noCycles").value(true));
    }

    @Test
    void returnsParsedCustomRules() throws Exception {
        String yaml = "layers:\n  - name: web\n    packagePatterns: [com.example.web]\n  - name: repository\n    packagePatterns: [com.example.data]\nforbidden:\n  - from: web\n    to: repository\n    severity: MEDIUM\nnoCycles: false\n";
        String scanId = queueLocalScan(yaml);
        waitForCompletion(scanId);
        mockMvc.perform(get("/api/scans/{id}/rules", scanId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.custom").value(true))
                .andExpect(jsonPath("$.rulesYaml").value(yaml))
                .andExpect(jsonPath("$.rules.layers[0].name").value("web"))
                .andExpect(jsonPath("$.rules.layers[0].packagePatterns[0]").value("com.example.web"))
                .andExpect(jsonPath("$.rules.forbidden[0].severity").value("MEDIUM"))
                .andExpect(jsonPath("$.rules.noCycles").value(false));
    }

    @Test
    void unknownScanRulesReturnsNotFound() throws Exception {
        String missingId = "00000000-0000-0000-0000-000000000000";
        mockMvc.perform(get("/api/scans/{id}/rules", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Scan not found: " + missingId));
    }

    private String queueLocalScan(String rulesYaml) throws Exception {
        String sampleProject = Path.of("..", "sample-project").toAbsolutePath().normalize().toString();
        var requestNode = objectMapper.createObjectNode().put("localPath", sampleProject);
        if (rulesYaml != null) requestNode.put("rulesYaml", rulesYaml);
        String response = mockMvc.perform(post("/api/scans").contentType(MediaType.APPLICATION_JSON)
                        .content(requestNode.toString())).andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private JsonNode waitForCompletion(String scanId) throws Exception {
        JsonNode result = null;
        for (int attempt = 0; attempt < 50; attempt++) {
            String body = mockMvc.perform(get("/api/scans/{id}", scanId)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            result = objectMapper.readTree(body);
            if (!"QUEUED".equals(result.get("status").asText()) && !"RUNNING".equals(result.get("status").asText())) return result;
            Thread.sleep(100);
        }
        return result;
    }
}
