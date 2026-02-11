package com.weekly.service;

public interface LlmService {

    String generate(String systemPrompt, String userInput);

    String getProviderName();
}
