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
        mockMvc.perform(get("/api/scans/{id}/graph", scanId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.modules.length()").value(org.hamcrest.Matchers.greaterThan(0)))
                .andExpect(jsonPath("$.dependencies.length()").value(org.hamcrest.Matchers.greaterThan(0)));
        mockMvc.perform(get("/api/scans/{id}/violations", scanId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }

    @Test
    void rejectsNonHttpsRemoteUrl() throws Exception {
        mockMvc.perform(post("/api/scans").contentType(MediaType.APPLICATION_JSON).content("{\"repoUrl\":\"file:///tmp/repository\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("repoUrl must be an HTTPS repository URL"));
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
