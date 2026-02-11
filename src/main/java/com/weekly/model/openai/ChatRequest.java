package com.weekly.model.openai;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("max_tokens") int maxTokens,
        double temperature
) {
    public record Message(String role, String content) {}

    public static ChatRequest of(String model, String systemPrompt, String userMessage,
                                 int maxTokens, double temperature) {
        return new ChatRequest(
                model,
                List.of(
                        new Message("system", systemPrompt),
                        new Message("user", userMessage)
                ),
                maxTokens,
                temperature
        );
    }
}
