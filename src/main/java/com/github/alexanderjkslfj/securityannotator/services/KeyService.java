package com.github.alexanderjkslfj.securityannotator.services;

import com.github.alexanderjkslfj.securityannotator.toolWindow.LLMProvider;
import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

@Service(Service.Level.APP)
public final class KeyService {
    private static class KeyStore {
        private final @NotNull CompletableFuture<Void> loadedApiKeyFromStorage = new CompletableFuture<>();
        private @Nullable String apiKey = null;
        private final @NotNull String apiStore;

        public KeyStore(@NotNull LLMProvider provider) {
            apiStore = "SecurityAnnotator.ApiKey." + provider;
            CompletableFuture.supplyAsync(() -> {
                CredentialAttributes attrs = new CredentialAttributes(apiStore);
                String storedKey = PasswordSafe.getInstance().getPassword(attrs);
                if (!loadedApiKeyFromStorage.isDone()) {
                    apiKey = storedKey;
                    loadedApiKeyFromStorage.complete(null);
                }
                return null;
            });
        }

        public void setApiKey(@NotNull String key) {
            apiKey = key;
            loadedApiKeyFromStorage.complete(null);
            CredentialAttributes attrs = new CredentialAttributes(apiStore);
            PasswordSafe.getInstance().setPassword(attrs, key);
        }

        public @NotNull CompletableFuture<@Nullable String> getApiKey() {
            return loadedApiKeyFromStorage.thenApply(x -> apiKey);
        }
    }

    private final @NotNull Dictionary<LLMProvider, KeyStore> keyStores;
    private KeyService() {
        LLMProvider[] providers = LLMProvider.values();
        keyStores = new Hashtable<>(providers.length);
        for(LLMProvider provider : providers) {
            keyStores.put(provider, new KeyStore(provider));
        }
    }

    public void setApiKey(@NotNull LLMProvider provider, @NotNull String key) {
        keyStores.get(provider).setApiKey(key);
    }

    public @NotNull CompletableFuture<@Nullable String> getApiKey(@NotNull LLMProvider provider) {
        return keyStores.get(provider).getApiKey();
    }
}
