package com.weekly.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppConfig(
        String llmProvider,
        GeminiConfig gemini,
        ExaoneConfig exaone,
        OpenaiConfig openai,
        ClaudeConfig claude,
        TeamsConfig teams,
        OutputConfig output
) {

    public record GeminiConfig(
            String apiKey,
            String model,
            String baseUrl,
            int maxOutputTokens,
            double temperature
    ) {}

    public record ExaoneConfig(
            String apiKey,
            String model,
            String baseUrl,
            int maxOutputTokens,
            double temperature
    ) {}

    public record OpenaiConfig(
            String apiKey,
            String model,
            String baseUrl,
            int maxOutputTokens,
            double temperature
    ) {}

    public record ClaudeConfig(
            String apiKey,
            String model,
            String baseUrl,
            int maxOutputTokens,
            double temperature
    ) {}

    public record TeamsConfig(String webhookUrl) {
        public boolean isEnabled() {
            return webhookUrl != null && !webhookUrl.isBlank();
        }
    }

    public record OutputConfig(String directory) {}
}
