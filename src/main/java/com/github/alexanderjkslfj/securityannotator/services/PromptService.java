package com.github.alexanderjkslfj.securityannotator.services;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.Service;
import com.intellij.util.concurrency.AppExecutorUtil;
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

    private static final @NotNull CompletableFuture<Void> loadedApiKeyFromStorage = new CompletableFuture<>();

    private static @Nullable String API_KEY = null;
    private static @Nullable String MODEL = null;

    private static final @NotNull String SYSTEM_MESSAGE = "You are a helpful assistant. You are an expert security researcher. You always follow instruction precisely. You never say more than what was asked.";
    private static final @NotNull String API_STORE = "SecurityAnnotator.ApiKey";

    private static boolean retrievingBestModel = false;
    private static final @NotNull CompletableFuture<Void> retrievedBestModel = new CompletableFuture<>();

    private PromptService() {
        CompletableFuture.supplyAsync(() -> {
            CredentialAttributes attrs = new CredentialAttributes(API_STORE);
            String storedKey = PasswordSafe.getInstance().getPassword(attrs);
            if(!loadedApiKeyFromStorage.isDone()) {
                API_KEY = storedKey;
                loadedApiKeyFromStorage.complete(null);
                setup();
            }
            return null;
        }, AppExecutorUtil.getAppExecutorService());
    }

    public void setApiKey(@NotNull String key) {
        API_KEY = key;
        loadedApiKeyFromStorage.complete(null);
        CredentialAttributes attrs = new CredentialAttributes(API_STORE);
        PasswordSafe.getInstance().setPassword(attrs, key);
        setup();
    }

    public @NotNull CompletableFuture<@Nullable String> getApiKey() {
        return loadedApiKeyFromStorage.thenApply(x -> API_KEY);
    }

    public @NotNull CompletableFuture<@Nullable String> prompt(@NotNull String message) {
        return retrievedBestModel.thenCompose(x -> {
            String body = buildCompletionBody(message);

            HttpRequest request = HttpRequest.newBuilder(URI.create("https://gpt.ruhr-uni-bochum.de/external/v1/chat/completions"))
                    .header("Authorization", String.format("Bearer %s", API_KEY))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            return CLIENT
                    .sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
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
        });
    }

    // write the json body for a completions request
    private @NotNull String buildCompletionBody(@NotNull String message) {
        ChatRequest body = new ChatRequest(
                MODEL,
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

    // check the list of models available and pick the best one
    private void setup() {
        if(!retrievingBestModel && MODEL == null && API_KEY != null) {
            retrievingBestModel = true;
            this.getBestModel().handle((model, ex) -> {
                if(ex == null) {
                    MODEL = model;
                    retrievedBestModel.complete(null);
                }
                retrievingBestModel = false;
                return null;
            });
        }
    }

    // retrieve the list of models and return the one with the largest context window
    private @NotNull CompletableFuture<@NotNull String> getBestModel() {
        if(API_KEY == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("API key hasn't been set."));
        }

        HttpRequest request = HttpRequest.newBuilder(URI.create("https://gpt.ruhr-uni-bochum.de/external/v1/models"))
                .header("Authorization", String.format("Bearer %s", API_KEY))
                .header("Content-Type", "application/json")
                .GET()
                .build();

        return CLIENT.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(HttpResponse::body)
                .thenApply(text -> {
                    try {
                        return RESPONSE_MAPPER.readValue(text, new  TypeReference<List<ModelInfo>>() {});
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JSON", e);
                    }
                })
                .thenApply(infos -> {
                    if(infos.isEmpty()) {
                        throw new RuntimeException("Empty JSON response");
                    }
                    int biggestContext = 0;
                    String biggestModel = "";
                    for(ModelInfo info : infos) {
                        if(info.contextLength() > biggestContext) {
                            biggestContext = info.contextLength();
                            biggestModel = info.name();
                        }
                    }
                    return biggestModel;
                });
    }
}

record ModelInfo(
        @JsonProperty("provided_by") String providedBy,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("name") String name,
        @JsonProperty("in_token_cost") double inTokenCost,
        @JsonProperty("out_token_cost") double outTokenCost,
        @JsonProperty("context_length") int contextLength
) {}

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

record ChatRequest(String model, List<ChatMessage> messages) {}