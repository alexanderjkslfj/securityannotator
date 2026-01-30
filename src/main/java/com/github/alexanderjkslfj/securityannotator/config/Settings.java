package com.github.alexanderjkslfj.securityannotator.config;

import com.github.alexanderjkslfj.securityannotator.services.KeyService;
import com.github.alexanderjkslfj.securityannotator.toolWindow.LLMProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.CompletableFuture;

public class Settings implements Configurable {
    @Nullable JPanel panel = null;
    @Nullable Dictionary<LLMProvider, @NotNull JBPasswordField> apiKeyFields = null;

    @Override
    public String getDisplayName() {
        return "Security Annotator";
    }

    @Override
    public @NotNull JComponent createComponent() {
        LLMProvider[] providers = LLMProvider.values();

        apiKeyFields = new Hashtable<>(providers.length);
        FormBuilder panelBuilder = FormBuilder.createFormBuilder();

        for (LLMProvider provider: providers) {
            JBPasswordField field = new JBPasswordField();
            apiKeyFields.put(provider, field);
            panelBuilder.addLabeledComponent(provider + " API key:", field, true);
        }

        reloadApiKey();

        panel = panelBuilder
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        return panel;
    }

    @Override
    public boolean isModified() {
        if(apiKeyFields == null) return false;
        KeyService keyService = ApplicationManager.getApplication().getService(KeyService.class);

        LLMProvider[] providers = LLMProvider.values();
        for(LLMProvider provider: providers) {
            JBPasswordField field = apiKeyFields.get(provider);

            String apiKey = keyService.getApiKey(provider).join();
            if (apiKey == null) {
                apiKey = "";
            }
            String text = new String(field.getPassword());

            if(!apiKey.equals(text)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void apply() {
        if(apiKeyFields == null) return;
        KeyService keyService = ApplicationManager.getApplication().getService(KeyService.class);

        LLMProvider[] providers = LLMProvider.values();
        for(LLMProvider provider: providers) {
            String apiKey = new String(apiKeyFields.get(provider).getPassword());
            keyService.setApiKey(provider,apiKey);
        }
    }

    @Override
    public void reset() {
        reloadApiKey().join();
    }

    private @NotNull CompletableFuture<Void> reloadApiKey() {
        if(apiKeyFields == null) return CompletableFuture.completedFuture(null);
        KeyService keyService = ApplicationManager.getApplication().getService(KeyService.class);

        CompletableFuture<?>[] futures = Arrays.stream(LLMProvider.values())
                .map(provider -> {
                    JBPasswordField field = apiKeyFields.get(provider);
                    field.setEnabled(false);
                    return keyService.getApiKey(provider).thenAccept(key -> {
                        field.setText(key != null ? key : "");
                        field.setEnabled(true);
                    });
                })
                .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        apiKeyFields = null;
    }
}
