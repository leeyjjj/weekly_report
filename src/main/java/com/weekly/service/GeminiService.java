package com.weekly.service;

import com.weekly.config.AppConfig;
import com.weekly.model.gemini.GeminiRequest;
import com.weekly.model.gemini.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import java.util.concurrent.atomic.AtomicLong;

public class GeminiService implements LlmService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);
    private static final int MAX_RETRIES = 3;
    private static final long RETRY_DELAY_MS = 45_000;
    private final AtomicLong callCount = new AtomicLong(0);

    private final RestClient restClient;
    private final AppConfig.GeminiConfig config;

    public GeminiService(RestClient restClient, AppConfig.GeminiConfig config) {
        this.restClient = restClient;
        this.config = config;
    }

    @Override
    public String generate(String systemPrompt, String userInput) {
        String uri = "/models/{model}:generateContent?key={apiKey}";

        GeminiRequest request = GeminiRequest.of(
                systemPrompt, userInput,
                config.maxOutputTokens(), config.temperature()
        );

        long currentCall = callCount.incrementAndGet();

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.info("[LLM Call #{}] Calling Gemini API (attempt {}/{}) model: {}", currentCall, attempt, MAX_RETRIES, config.model());

                GeminiResponse response = restClient.post()
                        .uri(uri, config.model(), config.apiKey())
                        .body(request)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, (req, res) -> {
                            String body = new String(res.getBody().readAllBytes());
                            int status = res.getStatusCode().value();
                            log.error("Gemini API error - status: {}, body: {}", status, body);
                            if (status == 429) {
                                throw new RateLimitException(body);
                            }
                            throw new RuntimeException("Gemini API 오류 (" + status + "): " + body);
                        })
                        .body(GeminiResponse.class);

                if (response == null) {
                    throw new RuntimeException("Gemini API returned null response");
                }

                String text = response.extractText();
                log.info("[LLM Call #{}] Gemini API response: {} chars", currentCall, text.length());
                return text;

            } catch (RateLimitException e) {
                if (attempt < MAX_RETRIES) {
                    log.warn("Rate limit hit, waiting {}s before retry ({}/{})", RETRY_DELAY_MS / 1000, attempt + 1, MAX_RETRIES);
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("API 요청이 중단되었습니다.", ie);
                    }
                } else {
                    throw new RuntimeException("Gemini API 할당량 초과: 잠시 후 다시 시도해주세요.");
                }
            }
        }

        throw new RuntimeException("Gemini API 호출 실패");
    }

    @Override
    public String getProviderName() {
        return "Gemini (" + config.model() + ")";
    }

    private static class RateLimitException extends RuntimeException {
        RateLimitException(String message) {
            super(message);
        }
    }
}
