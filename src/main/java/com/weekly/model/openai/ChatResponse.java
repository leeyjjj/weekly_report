package com.weekly.model.openai;

import java.util.List;

public record ChatResponse(List<Choice> choices) {

    public record Choice(Message message) {}

    public record Message(String role, String content) {}

    public String extractText() {
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        var message = choices.getFirst().message();
        return message != null && message.content() != null ? message.content() : "";
    }
}
