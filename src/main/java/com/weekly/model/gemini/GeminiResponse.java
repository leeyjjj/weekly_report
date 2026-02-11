package com.weekly.model.gemini;

import java.util.List;

public record GeminiResponse(List<Candidate> candidates) {

    public record Candidate(Content content) {}

    public record Content(List<Part> parts, String role) {}

    public record Part(String text) {}

    public String extractText() {
        if (candidates == null || candidates.isEmpty()) {
            return "";
        }
        var parts = candidates.getFirst().content().parts();
        if (parts == null || parts.isEmpty()) {
            return "";
        }
        return parts.getFirst().text();
    }
}
