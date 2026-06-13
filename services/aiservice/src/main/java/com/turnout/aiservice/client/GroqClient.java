package com.turnout.aiservice.client;

import com.turnout.aiservice.config.GroqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;

@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final WebClient webClient;
    private final GroqProperties props;

    public GroqClient(WebClient webClient, GroqProperties props) {
        this.webClient = webClient;
        this.props = props;
    }

    /**
     * Sends a system prompt + user message to Groq and returns the raw text response.
     * Uses .block() safely because virtual threads are enabled — no OS thread is held hostage.
     */
    public String chat(String systemPrompt, String userMessage) {
        Map<String, Object> requestBody = buildRequestBody(systemPrompt, userMessage);

        try {
            Map<String, Object> response = webClient.post()
                    .uri(props.getApiUrl())
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            return extractContent(response);

        } catch (WebClientResponseException e) {
            log.error("Groq API error: status={} body={}", e.getStatusCode(), e.getResponseBodyAsString());
            return "AI service temporarily unavailable.";
        } catch (Exception e) {
            log.error("Groq call failed: {}", e.getMessage());
            return "AI service temporarily unavailable.";
        }
    }

    private Map<String, Object> buildRequestBody(String systemPrompt, String userMessage) {
        return Map.of(
                "model",       props.getModel(),
                "max_tokens",  props.getMaxTokens(),
                "temperature", props.getTemperature(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user",   "content", userMessage)
                )
        );
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> response) {
        if (response == null) {
            log.warn("Groq returned null response");
            return "AI service temporarily unavailable.";
        }
        try {
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            log.error("Failed to parse Groq response structure: {}", response);
            return "AI service temporarily unavailable.";
        }
    }
}
