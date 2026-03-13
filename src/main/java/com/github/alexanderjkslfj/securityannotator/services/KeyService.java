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

/// Responsible for API key management of all providers.
@Service(Service.Level.APP)
public final class KeyService {
    /// Responsible for API key management of a single provider.
    private static class KeyStore {
        private final @NotNull CompletableFuture<Void> loadedApiKeyFromStorage = new CompletableFuture<>();
        private @Nullable String apiKey = null;
        private final @NotNull String apiStore;

        public KeyStore(@NotNull LLMProvider provider) {
            // Name the storage after the provider.
            apiStore = "SecurityAnnotator.ApiKey." + provider;

            // Retrieve the API key from disk, if possible.
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

        /// Change the provider's API key.
        public void setApiKey(@NotNull String key) {
            apiKey = key;
            // Skip loading from storage, as it's no longer necessary.
            loadedApiKeyFromStorage.complete(null);
            CredentialAttributes attrs = new CredentialAttributes(apiStore);
            PasswordSafe.getInstance().setPassword(attrs, key);
        }

        /// Retrieve the provider's API key. Returns `null` if no API key is available.
        public @NotNull CompletableFuture<@Nullable String> getApiKey() {
            // Wait for loading from storage to complete before returning.
            return loadedApiKeyFromStorage.thenApply(x -> apiKey);
        }
    }

    private final @NotNull Dictionary<LLMProvider, KeyStore> keyStores;
    private KeyService() {
        // Each provider gets their own KeyStore.
        LLMProvider[] providers = LLMProvider.values();
        keyStores = new Hashtable<>(providers.length);
        for(LLMProvider provider : providers) {
            keyStores.put(provider, new KeyStore(provider));
        }
    }

    /// Change a provider's API key.
    public void setApiKey(@NotNull LLMProvider provider, @NotNull String key) {
        // Delegate work to the respective KeyStore.
        keyStores.get(provider).setApiKey(key);
    }

    /// Retrieve a provider's API key. Returns `null` if no API key is available.
    public @NotNull CompletableFuture<@Nullable String> getApiKey(@NotNull LLMProvider provider) {
        // Delegate work to the respective KeyStore.
        return keyStores.get(provider).getApiKey();
    }
}
