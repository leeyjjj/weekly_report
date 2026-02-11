package com.weekly.model.gemini;

import java.util.List;

public record GeminiRequest(
        List<Content> contents,
        GenerationConfig generationConfig,
        SystemInstruction systemInstruction
) {

    public record Content(String role, List<Part> parts) {}

    public record Part(String text) {}

    public record GenerationConfig(int maxOutputTokens, double temperature) {}

    public record SystemInstruction(List<Part> parts) {}

    public static GeminiRequest of(String systemPrompt, String userMessage,
                                   int maxTokens, double temperature) {
        return new GeminiRequest(
                List.of(new Content("user", List.of(new Part(userMessage)))),
                new GenerationConfig(maxTokens, temperature),
                new SystemInstruction(List.of(new Part(systemPrompt)))
        );
    }
}
