package com.github.alexanderjkslfj.securityannotator.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexanderjkslfj.securityannotator.dataPackage.InvalidApiKeyException;
import com.github.alexanderjkslfj.securityannotator.toolWindow.LLMProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service(Service.Level.APP)
public final class PromptService {
    private static final @NotNull HttpClient CLIENT = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
    private static final @NotNull ObjectMapper RESPONSE_MAPPER = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final @NotNull ObjectMapper REQUEST_MAPPER = new ObjectMapper();

    private static final @NotNull String SYSTEM_MESSAGE = "You are a helpful assistant. You are an expert security researcher. You always follow instructions precisely. You never say more than what was asked.";

    private PromptService() {}

    public @NotNull CompletableFuture<@Nullable String> prompt(@NotNull LLMProvider provider, @NotNull String message) throws RuntimeException {
        String body = buildCompletionBody(message, provider.getModel());

        KeyService keyService = ApplicationManager
                .getApplication()
                .getService(KeyService.class);

        HttpRequest request = HttpRequest.newBuilder(URI.create(provider.getUrl()))
                .header("Authorization", String.format("Bearer %s", keyService.getApiKey(provider)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        return CLIENT
                .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    //checks if the api key is valid
                    int status = response.statusCode();
                    if (status == 401 || status == 403) {
                        throw new InvalidApiKeyException(
                                "Invalid API key for provider " + provider + " with model " + provider.getModel()
                        );
                    }
                    return response.body();
                })
                .thenApply(text -> {
                    try {
                        return RESPONSE_MAPPER.readValue(text, CompletionResponse.class);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON", e);
                    }
                })
                .thenApply(response -> {
                    List<Completion> completions = response.completions();
                    if (completions == null || completions.isEmpty()) {
                        throw new RuntimeException("Empty prompt response");
                    }
                    return completions.getFirst().message().content();
                });
    }

    // write the JSON body for a completions request
    private @NotNull String buildCompletionBody(@NotNull String message, @NotNull String model) throws RuntimeException {
        ChatRequest body = new ChatRequest(
                model,
                0,
                0.01,
                List.of(
                        new ChatMessage("system", SYSTEM_MESSAGE),
                        new ChatMessage("user", message)
                )
        );

        try {
            return REQUEST_MAPPER.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize JSON", e);
        }
    }
}

record CompletionResponse (
        @JsonProperty("choices") List<Completion> completions
) {}

record Completion(
        Message message
) {}

record Message (
        String content
) {}

record ChatMessage(String role, String content) {}

record ChatRequest(
        String model,
        double temperature,
        double top_p,
        List<ChatMessage> messages
) {}