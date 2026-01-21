package com.github.alexanderjkslfj.securityannotator.toolWindow;

public enum LLMProvider {
    RUB_GPT("RUB-GPT"),
    CHAT_GPT("ChatGPT");

    private final String displayName;

    LLMProvider(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
