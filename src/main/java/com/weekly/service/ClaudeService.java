package com.weekly.service;

import com.weekly.config.AppConfig;
import com.weekly.model.claude.ClaudeRequest;
import com.weekly.model.claude.ClaudeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicLong;

public class ClaudeService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(ClaudeService.class);
    private final AtomicLong callCount = new AtomicLong(0);

    private final RestClient restClient;
    private final AppConfig.ClaudeConfig config;

    public ClaudeService(RestClient restClient, AppConfig.ClaudeConfig config) {
        this.restClient = restClient;
        this.config = config;
    }

    @Override
    public String generate(String systemPrompt, String userInput) {
        ClaudeRequest request = ClaudeRequest.of(
                config.model(), systemPrompt, userInput,
                config.maxOutputTokens(), config.temperature()
        );

        long currentCall = callCount.incrementAndGet();
        log.info("[LLM Call #{}] Calling Claude API model: {}", currentCall, config.model());

        ClaudeResponse response = restClient.post()
                .uri("/v1/messages")
                .header("x-api-key", config.apiKey())
                .header("anthropic-version", "2023-06-01")
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());
                    log.error("Claude API error - status: {}, body: {}", res.getStatusCode(), body);
                    throw new RuntimeException("Claude API 오류 (" + res.getStatusCode() + "): " + body);
                })
                .body(ClaudeResponse.class);

        if (response == null) {
            throw new RuntimeException("Claude API returned null response");
        }

        String text = response.extractText();
        log.info("[LLM Call #{}] Claude API response: {} chars", currentCall, text.length());
        return text;
    }

    @Override
    public String getProviderName() {
        return "Claude (" + config.model() + ")";
    }
}
