package com.github.alexanderjkslfj.securityannotator.toolWindow;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.alexanderjkslfj.securityannotator.config.Settings;
import com.github.alexanderjkslfj.securityannotator.services.PromptService;
import com.github.alexanderjkslfj.securityannotator.dataPackage.PromptBuilder;
import com.github.alexanderjkslfj.securityannotator.annotator.Annotator;

import com.github.alexanderjkslfj.securityannotator.util.Annotation;
import com.github.alexanderjkslfj.securityannotator.util.Parser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
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
import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import static com.github.alexanderjkslfj.securityannotator.services.ResponseLogger.logLlmResponse;

public class LLMWindow implements ToolWindowFactory{
    private @Nullable java.util.List<Annotation> toBeApplied = null;
    private @Nullable String lastLlmResponse = null;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("LLM Analyzer", SwingConstants.CENTER);

        JButton analyzeButton = new JButton("Run Analysis");
        JButton applyButton = new JButton("Apply Result");
        applyButton.setEnabled(false);
        JButton apiKeySetterButton = new JButton("Enter API Key");

        ComboBox<LLMProvider> llmSelector =
                new ComboBox<>(LLMProvider.values());
        llmSelector.setSelectedItem(LLMProvider.RUB_GPT);

        ComboBox<AnalyzationMethod> methodSelector =
                new ComboBox<>(AnalyzationMethod.values());
        methodSelector.setSelectedItem(AnalyzationMethod.RAW_TEXT);

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JBScrollPane scrollPane = new JBScrollPane(outputArea);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(methodSelector);
        buttonPanel.add(analyzeButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(llmSelector);
        buttonPanel.add(apiKeySetterButton);

        panel.add(label, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        apiKeySetterButton.addActionListener(e -> ShowSettingsUtil.getInstance().showSettingsDialog(project, Settings.class));

        applyButton.addActionListener(e -> {
            if(toBeApplied != null) {
                Annotator.insertFeatureCommentByAnnotation(project, toBeApplied);
                toBeApplied = null;
                applyButton.setEnabled(false);
                outputArea.setText("Annotations applied.");
            } else if (lastLlmResponse != null) {
                boolean success = true;
                try {
                    Annotator.insertFeatureCommentByText(project, lastLlmResponse);
                } catch (JsonProcessingException ignored) {
                    success = false;
                }
                toBeApplied = null;
                applyButton.setEnabled(false);
                if(success) {
                    outputArea.setText("Annotations applied.");
                } else {
                    outputArea.setText("Annotations could not be applied.");
                }
            }
        });

        analyzeButton.addActionListener(e -> {
            applyButton.setEnabled(false);
            analyzeButton.setEnabled(false);

            lastLlmResponse = null;
            toBeApplied = null;

            AnalyzationMethod method = (AnalyzationMethod) methodSelector.getSelectedItem();
            String promptInput;
            try {
                if (method == AnalyzationMethod.RAW_TEXT) {
                    promptInput = PromptBuilder.buildTextPrompt(project);
                } else {
                    promptInput = PromptBuilder.buildPsiPrompt(project);
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            outputArea.setText("Analyzing code...");

            PromptService service = ApplicationManager
                    .getApplication()
                    .getService(PromptService.class);

            LLMProvider provider = (LLMProvider) llmSelector.getSelectedItem();
            if(provider == null) {
                analyzeButton.setEnabled(true);
                outputArea.setText("An error occurred.");
                return;
            }
            CompletableFuture<String> request = service.prompt(provider, promptInput);

            if (method == AnalyzationMethod.RAW_TEXT) {
                request
                        .thenAccept(response -> ApplicationManager.getApplication().invokeLater(() -> {
                            analyzeButton.setEnabled(true);
                            if (response != null) {
                                java.util.List<Annotation> list = Parser.parse(project, response);
                                if (list != null && !list.isEmpty()) {
                                    toBeApplied = Annotator.deduplicateAnnotations(list);

                                    toBeApplied.sort(Comparator.comparingInt(x -> x.start_line));
                                    outputArea.setText(annotationsToText(toBeApplied));
                                    applyButton.setEnabled(true);

                                    logLlmResponse(annotationsToText(toBeApplied), provider.toString());
                                } else {
                                    outputArea.setText("No annotations found.");
                                }
                            } else {
                                outputArea.setText("No annotations found.");
                            }
                        }))
                        .exceptionally(ex -> {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                analyzeButton.setEnabled(true);
                                outputArea.setText("Error: " + ex.getMessage());
                            });
                            return null;
                        });
            } else {
                request.thenAccept(response -> {
                    analyzeButton.setEnabled(true);
                    if(response != null) {
                        lastLlmResponse = response;
                        outputArea.setText(response);
                        applyButton.setEnabled(true);
                        logLlmResponse(provider.toString(), response);
                    }
                }).exceptionally(ex -> {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        analyzeButton.setEnabled(true);
                        outputArea.setText("Error: " + ex.getMessage());
                    });
                    return null;
                });
            }
        });

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }

    private @NotNull String annotationsToText(@NotNull java.util.List<Annotation> annotations) {
        StringBuilder text = new StringBuilder("Annotations found:\n");
        for (Annotation a : annotations) {
            if (a.start_line == a.end_line) {
                text.append("Line ").append(a.start_line).append(": ").append(a.category).append("\n");
            } else {
                text.append("Lines ").append(a.start_line).append(" to ").append(a.end_line).append(": ").append(a.category).append("\n");
            }
        }
        return text.toString();
    }
}
