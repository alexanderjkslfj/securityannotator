package com.github.alexanderjkslfj.securityannotator.toolWindow;

import org.jetbrains.annotations.NotNull;

public enum LLMProvider {
    RUB_GPT("RUB-GPT", "gpt-4.1-2025-04-14", "https://gpt.ruhr-uni-bochum.de/external/v1/chat/completions"),
    CHAT_GPT("ChatGPT", "gpt-4.1-mini", "https://api.openai.com/v1/chat/completions");

    private final String displayName;
    private final String model;
    private final String url;

    LLMProvider(@NotNull String displayName, @NotNull String model, @NotNull String url) {
        this.displayName = displayName;
        this.model = model;
        this.url = url;
    }

    public @NotNull String getModel() {
        return model;
    }

    public @NotNull String getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
