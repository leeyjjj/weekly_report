package com.weekly.service;

import com.weekly.config.AppConfig;
import com.weekly.model.openai.ChatRequest;
import com.weekly.model.openai.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicLong;

public class ExaoneService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(ExaoneService.class);
    private final AtomicLong callCount = new AtomicLong(0);

    private final RestClient restClient;
    private final AppConfig.ExaoneConfig config;

    public ExaoneService(RestClient restClient, AppConfig.ExaoneConfig config) {
        this.restClient = restClient;
        this.config = config;
    }

    @Override
    public String generate(String systemPrompt, String userInput) {
        ChatRequest request = ChatRequest.of(
                config.model(), systemPrompt, userInput,
                config.maxOutputTokens(), config.temperature()
        );

        long currentCall = callCount.incrementAndGet();
        log.info("[LLM Call #{}] Calling EXAONE API model: {}", currentCall, config.model());

        var requestSpec = restClient.post()
                .uri("/v1/chat/completions");

        if (config.apiKey() != null && !config.apiKey().isBlank()) {
            requestSpec = requestSpec.header("Authorization", "Bearer " + config.apiKey());
        }

        ChatResponse response = requestSpec
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());
                    log.error("EXAONE API error - status: {}, body: {}", res.getStatusCode(), body);
                    throw new RuntimeException("EXAONE API 오류 (" + res.getStatusCode() + "): " + body);
                })
                .body(ChatResponse.class);

        if (response == null) {
            throw new RuntimeException("EXAONE API returned null response");
        }

        String text = response.extractText();
        log.info("[LLM Call #{}] EXAONE API response: {} chars", currentCall, text.length());
        return text;
    }

    @Override
    public String getProviderName() {
        return "EXAONE (" + config.model() + ")";
    }
}
