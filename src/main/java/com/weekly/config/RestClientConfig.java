package com.weekly.config;

import com.weekly.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    private static final Logger log = LoggerFactory.getLogger(RestClientConfig.class);

    @Bean
    public RestClient teamsRestClient() {
        return RestClient.builder()
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Bean
    public LlmService llmService(AppConfig appConfig) {
        String provider = appConfig.llmProvider() != null ? appConfig.llmProvider() : "gemini";

        return switch (provider.toLowerCase()) {
            case "exaone" -> {
                log.info("Using EXAONE LLM provider (model: {})", appConfig.exaone().model());
                RestClient restClient = RestClient.builder()
                        .baseUrl(appConfig.exaone().baseUrl())
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                yield new ExaoneService(restClient, appConfig.exaone());
            }
            case "openai" -> {
                log.info("Using OpenAI LLM provider (model: {})", appConfig.openai().model());
                RestClient restClient = RestClient.builder()
                        .baseUrl(appConfig.openai().baseUrl())
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                yield new OpenAiService(restClient, appConfig.openai());
            }
            case "claude" -> {
                log.info("Using Claude LLM provider (model: {})", appConfig.claude().model());
                RestClient restClient = RestClient.builder()
                        .baseUrl(appConfig.claude().baseUrl())
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                yield new ClaudeService(restClient, appConfig.claude());
            }
            default -> {
                log.info("Using Gemini LLM provider (model: {})", appConfig.gemini().model());
                RestClient restClient = RestClient.builder()
                        .baseUrl(appConfig.gemini().baseUrl())
                        .defaultHeader("Content-Type", "application/json")
                        .build();
                yield new GeminiService(restClient, appConfig.gemini());
            }
        };
    }
}
