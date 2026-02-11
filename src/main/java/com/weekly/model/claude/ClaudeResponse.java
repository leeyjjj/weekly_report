package com.weekly.model.claude;

import java.util.List;

public record ClaudeResponse(List<ContentBlock> content) {

    public record ContentBlock(String type, String text) {}

    public String extractText() {
        if (content == null || content.isEmpty()) {
            return "";
        }
        return content.stream()
                .filter(block -> "text".equals(block.type()))
                .map(ContentBlock::text)
                .reduce("", (a, b) -> a + b);
    }
}
