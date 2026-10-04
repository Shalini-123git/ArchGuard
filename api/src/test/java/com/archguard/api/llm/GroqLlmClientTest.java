package com.archguard.api.llm;

import com.archguard.api.config.ArchGuardProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpHeaders.RETRY_AFTER;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpStatus.TOO_MANY_REQUESTS;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

class GroqLlmClientTest {
    private final PromptBuilder promptBuilder = new PromptBuilder();
    private final ViolationContext context = new ViolationContext("rule", "from", "to", java.util.List.of(), java.util.List.of());

    @Test
    void retries429AndReturnsRealExplanation() {
        ArchGuardProperties properties = properties();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withStatus(TOO_MANY_REQUESTS).header(RETRY_AFTER, "0").body("busy"));
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"real explanation\"}}]}", APPLICATION_JSON));

        GroqLlmClient client = new GroqLlmClient(properties, promptBuilder, new ObjectMapper(), builder.build());

        assertEquals("real explanation", client.explain(context));
        server.verify();
    }

    @Test
    void lengthFinishReasonProducesDiagnosticFailure() {
        ArchGuardProperties properties = properties();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withSuccess("{\"choices\":[{\"finish_reason\":\"length\",\"message\":{\"content\":\"\"}}]}", APPLICATION_JSON));

        GroqLlmClient client = new GroqLlmClient(properties, promptBuilder, new ObjectMapper(), builder.build());

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> client.explain(context));
        assertEquals("Groq hit max_tokens before producing an explanation", failure.getMessage());
        server.verify();
    }

    @Test
    void unauthorizedResponseIsNotRetried() {
        ArchGuardProperties properties = properties();
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.groq.com/openai/v1");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://api.groq.com/openai/v1/chat/completions"))
                .andRespond(withStatus(UNAUTHORIZED).body("not authorized"));

        GroqLlmClient client = new GroqLlmClient(properties, promptBuilder, new ObjectMapper(), builder.build());

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> client.explain(context));
        assertEquals("Groq returned 401: not authorized", failure.getMessage());
        server.verify();
    }

    private ArchGuardProperties properties() {
        ArchGuardProperties properties = new ArchGuardProperties();
        properties.getLlm().setApiKey("test-key");
        properties.getLlm().setRetryBackoff(java.time.Duration.ZERO);
        properties.getLlm().setMaxTokens(1500);
        return properties;
    }
}
