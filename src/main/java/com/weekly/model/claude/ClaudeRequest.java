package com.weekly.model.claude;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaudeRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<Message> messages,
        double temperature
) {
    public record Message(String role, String content) {}

    public static ClaudeRequest of(String model, String systemPrompt, String userMessage,
                                   int maxTokens, double temperature) {
        return new ClaudeRequest(
                model,
                maxTokens,
                systemPrompt,
                List.of(new Message("user", userMessage)),
                temperature
        );
    }
}
