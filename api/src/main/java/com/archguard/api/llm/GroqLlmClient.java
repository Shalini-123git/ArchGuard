package com.archguard.api.llm;

import com.archguard.api.config.ArchGuardProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.SocketTimeoutException;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

/** Groq Chat Completions adapter with bounded transport timeouts and targeted retries. */
@Component
public class GroqLlmClient implements LlmClient {
    private static final Logger log = LoggerFactory.getLogger(GroqLlmClient.class);
    private static final int MAX_ATTEMPTS = 3;
    private static final int MAX_ERROR_BODY_LENGTH = 300;
    private final ArchGuardProperties.Llm properties;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final RestClient client;

    @Autowired
    public GroqLlmClient(ArchGuardProperties properties, PromptBuilder promptBuilder, ObjectMapper objectMapper) {
        this.properties = properties.getLlm();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getLlm().getConnectTimeout());
        factory.setReadTimeout(properties.getLlm().getReadTimeout());
        this.client = RestClient.builder().baseUrl("https://api.groq.com/openai/v1").requestFactory(factory).build();
        log.info("Groq LLM configured: apiKeyConfigured={}, model={}",
                properties.getLlm().getApiKey() != null && !properties.getLlm().getApiKey().isBlank(),
                this.properties.getModel());
    }

    GroqLlmClient(ArchGuardProperties properties, PromptBuilder promptBuilder, ObjectMapper objectMapper, RestClient client) {
        this.properties = properties.getLlm();
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.client = client;
    }

    @Override
    public String explain(ViolationContext context) {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new IllegalStateException("LLM_API_KEY is not configured");
        }
        RuntimeException lastFailure = null;
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            try {
                return request(promptBuilder.build(context));
            } catch (RestClientResponseException exception) {
                lastFailure = new IllegalStateException("Groq returned " + exception.getStatusCode().value()
                        + ": " + truncate(exception.getResponseBodyAsString()), exception);
                if (!isRetryable(exception.getStatusCode().value()) || attempt == MAX_ATTEMPTS - 1) throw lastFailure;
                waitBeforeRetry(attempt, exception);
            } catch (ResourceAccessException exception) {
                if (!isTimeout(exception) || attempt == MAX_ATTEMPTS - 1) throw exception;
                lastFailure = exception;
                waitBeforeRetry(attempt, null);
            } catch (RuntimeException exception) {
                throw exception;
            }
        }
        throw lastFailure;
    }

    private String request(String prompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.getModel());
        body.put("temperature", 0.2);
        body.put("max_tokens", properties.getMaxTokens());
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        if (properties.getModel().startsWith("openai/gpt-oss")) body.put("reasoning_effort", "low");
        String response = client.post().uri("/chat/completions")
                .header("Authorization", "Bearer " + properties.getApiKey())
                .body(body).retrieve().body(String.class);
        JsonNode root;
        try {
            root = objectMapper.readTree(response);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not read Groq response", exception);
        }
        JsonNode choice = root.path("choices").path(0);
        JsonNode content = choice.path("message").path("content");
        if (content.asText().isBlank() && "length".equals(choice.path("finish_reason").asText())) {
            throw new IllegalStateException("Groq hit max_tokens before producing an explanation");
        }
        if (content.isMissingNode() || content.asText().isBlank()) throw new IllegalStateException("Groq response did not include an explanation");
        return content.asText().trim();
    }

    private boolean isRetryable(int status) { return status == 429 || status >= 500; }

    private boolean isTimeout(ResourceAccessException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) return true;
            cause = cause.getCause();
        }
        return false;
    }

    private void waitBeforeRetry(int attempt, RestClientResponseException exception) {
        long delaySeconds = exception != null && exception.getStatusCode().value() == 429
                ? retryAfterSeconds(exception).orElse(attempt == 0 ? 2L : 4L)
                : Math.max(1L, properties.getRetryBackoff().toSeconds());
        sleep(Math.min(20L, delaySeconds) * 1000L);
    }

    private java.util.OptionalLong retryAfterSeconds(RestClientResponseException exception) {
        try {
            String value = exception.getResponseHeaders().getFirst("Retry-After");
            return value == null ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException exceptionValue) {
            return java.util.OptionalLong.empty();
        }
    }

    private String truncate(String value) {
        return value.length() <= MAX_ERROR_BODY_LENGTH ? value : value.substring(0, MAX_ERROR_BODY_LENGTH);
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LLM retry interrupted", exception);
        }
    }
}
