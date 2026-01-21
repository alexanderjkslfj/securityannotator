package com.github.alexanderjkslfj.securityannotator.toolWindow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.alexanderjkslfj.securityannotator.config.Settings;
import com.github.alexanderjkslfj.securityannotator.services.PromptService;
import com.github.alexanderjkslfj.securityannotator.dataPackage.PromptBuilder;
import com.github.alexanderjkslfj.securityannotator.annotator.CreateSampleAnnotation;
import com.github.alexanderjkslfj.securityannotator.annotator.Annotator;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;

public class LLMWindow implements ToolWindowFactory{

    private volatile @Nullable String lastLlmResponse = null;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("LLM Analyzer", SwingConstants.CENTER);

        JButton analyzeButton = new JButton("Run Analysis");
        JButton apiKeySetterButton = new JButton("Enter API Key");
        JButton annotationGenerator = new JButton("generate annotations");
        annotationGenerator.setEnabled(false);

        ComboBox<LLMProvider> llmSelector =
                new ComboBox<>(LLMProvider.values());

        llmSelector.setSelectedItem(LLMProvider.RUB_GPT);

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JBScrollPane scrollPane = new JBScrollPane(outputArea);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(analyzeButton);
        buttonPanel.add(llmSelector);
        buttonPanel.add(apiKeySetterButton);
        buttonPanel.add(annotationGenerator);

        panel.add(label, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        apiKeySetterButton.addActionListener(e -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, Settings.class);
        });

        analyzeButton.addActionListener(e -> {
            String promptInput = null;
            try {
                promptInput = PromptBuilder.buildPrompt(project);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
            outputArea.setText(promptInput);
            //outputArea.setText(" Sending to LLM...");

            PromptService service = ApplicationManager
                    .getApplication()
                    .getService(PromptService.class);

            LLMProvider selected = (LLMProvider) llmSelector.getSelectedItem();

            CompletableFuture<String> request;

            if (selected == LLMProvider.CHAT_GPT) {
                request = service.promptChatGPT(promptInput);
            } else {
                request = service.promptRUBGPT(promptInput);
            }

            // Send the content to the LLM
            request.thenAccept(response -> ApplicationManager.getApplication().invokeLater(() -> {
                        lastLlmResponse = response;
                        annotationGenerator.setEnabled(true);
                        outputArea.setText("LLM Response:" + response);
                    }))
                    .exceptionally(ex -> {
                        lastLlmResponse = null;
                        annotationGenerator.setEnabled(false);
                        ApplicationManager.getApplication().invokeLater(() -> {
                            outputArea.setText("Error: " + ex.getMessage());
                        });
                        return null;
                    });
        });

        annotationGenerator.addActionListener(e -> {
            if (lastLlmResponse == null || lastLlmResponse.isBlank()) {
                Messages.showWarningDialog(
                        project,
                        "No LLM response available. Run analysis first.",
                        "Cannot Generate Annotations"
                );
                return;
            }
            try {
                Annotator.insertFeatureComment(project, lastLlmResponse);
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        });

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
