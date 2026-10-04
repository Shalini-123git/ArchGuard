package com.archguard.api.llm;

import com.archguard.api.config.ArchGuardProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/** Groq Chat Completions adapter with bounded transport timeouts and one retry. */
@Component
public class GroqLlmClient implements LlmClient {
    private final ArchGuardProperties.Llm properties;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final RestClient client;

    public GroqLlmClient(ArchGuardProperties properties, PromptBuilder promptBuilder, ObjectMapper objectMapper) {
        this.properties = properties.getLlm();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getLlm().getConnectTimeout());
        factory.setReadTimeout(properties.getLlm().getReadTimeout());
        this.client = RestClient.builder().baseUrl("https://api.groq.com/openai/v1").requestFactory(factory).build();
    }

    @Override
    public String explain(ViolationContext context) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("LLM_API_KEY is not configured");
        }
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < 2; attempt++) {
            try {
                return request(promptBuilder.build(context));
            } catch (RuntimeException exception) {
                System.err.println("GROQ REQUEST FAILED: " + exception.getMessage());
                exception.printStackTrace();
                lastFailure = exception;
                if (attempt == 0) waitBeforeRetry();
            }
        }
        throw lastFailure;
    }

    private String request(String prompt) {
        Map<String, Object> body = Map.of(
                "model", properties.getModel(),
                "temperature", 0.2,
                "max_tokens", 220,
                "messages", List.of(Map.of("role", "user", "content", prompt))
        );
        String response = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body).retrieve().body(String.class);
        try {
            JsonNode content = objectMapper.readTree(response).path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText().isBlank()) throw new IllegalStateException("Groq response did not include an explanation");
            return content.asText().trim();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read Groq response", exception);
        }
    }

    private void waitBeforeRetry() {
        try {
            Thread.sleep(properties.getRetryBackoff().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM retry interrupted", exception);
        }
    }
}
