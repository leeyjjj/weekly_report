package com.weekly.service;

import com.weekly.config.AppConfig;
import com.weekly.model.openai.ChatRequest;
import com.weekly.model.openai.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicLong;

public class OpenAiService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiService.class);
    private final AtomicLong callCount = new AtomicLong(0);

    private final RestClient restClient;
    private final AppConfig.OpenaiConfig config;

    public OpenAiService(RestClient restClient, AppConfig.OpenaiConfig config) {
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
        log.info("[LLM Call #{}] Calling OpenAI API model: {}", currentCall, config.model());

        ChatResponse response = restClient.post()
                .uri("/v1/chat/completions")
                .header("Authorization", "Bearer " + config.apiKey())
                .body(request)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    String body = new String(res.getBody().readAllBytes());
                    log.error("OpenAI API error - status: {}, body: {}", res.getStatusCode(), body);
                    throw new RuntimeException("OpenAI API 오류 (" + res.getStatusCode() + "): " + body);
                })
                .body(ChatResponse.class);

        if (response == null) {
            throw new RuntimeException("OpenAI API returned null response");
        }

        String text = response.extractText();
        log.info("[LLM Call #{}] OpenAI API response: {} chars", currentCall, text.length());
        return text;
    }

    @Override
    public String getProviderName() {
        return "OpenAI (" + config.model() + ")";
    }
}
