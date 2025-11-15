package com.github.alexanderjkslfj.securityannotator.config;

import com.github.alexanderjkslfj.securityannotator.services.PromptService;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.Configurable;
import com.intellij.ui.components.JBPasswordField;
import com.intellij.util.ui.FormBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.concurrent.CompletableFuture;

public class Settings implements Configurable {
    @Nullable JPanel panel = null;
    @Nullable JBPasswordField apiKeyField = null;

    @Override
    public String getDisplayName() {
        return "Security Annotator";
    }

    @Override
    public @NotNull JComponent createComponent() {
        apiKeyField = new JBPasswordField();

        reloadApiKey();

        panel = FormBuilder.createFormBuilder()
                .addLabeledComponent("API key:", apiKeyField, true)
                .addComponentFillVertically(new JPanel(), 0)
                .getPanel();

        return panel;
    }

    @Override
    public boolean isModified() {
        if(apiKeyField == null) return false;
        PromptService service = ApplicationManager.getApplication().getService(PromptService.class);
        String apiKey = service.getApiKey().join();
        if(apiKey == null) {
            apiKey = "";
        }
        String text = new String(apiKeyField.getPassword());
        return !apiKey.equals(text);
    }

    @Override
    public void apply() {
        if(apiKeyField == null) return;
        PromptService service = ApplicationManager.getApplication().getService(PromptService.class);
        String apiKey = new String(apiKeyField.getPassword());
        service.setApiKey(apiKey);
    }

    @Override
    public void reset() {
        reloadApiKey().join();
    }

    private @NotNull CompletableFuture<Void> reloadApiKey() {
        if(apiKeyField == null) return CompletableFuture.completedFuture(null);
        PromptService service = ApplicationManager.getApplication().getService(PromptService.class);
        apiKeyField.setEnabled(false);
        return service.getApiKey().thenAccept(key -> {
            apiKeyField.setText(key != null ? key : "");
            apiKeyField.setEnabled(true);
        });
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        apiKeyField = null;
    }
}
