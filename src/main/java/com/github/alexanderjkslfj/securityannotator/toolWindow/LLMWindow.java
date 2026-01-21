package com.github.alexanderjkslfj.securityannotator.toolWindow;

import com.github.alexanderjkslfj.securityannotator.config.Settings;
import com.github.alexanderjkslfj.securityannotator.services.PromptService;
import com.github.alexanderjkslfj.securityannotator.dataPackage.PromptBuilder;
import com.github.alexanderjkslfj.securityannotator.annotator.CreateSampleAnnotation;
import com.github.alexanderjkslfj.securityannotator.annotator.Annotator;

import com.github.alexanderjkslfj.securityannotator.util.Annotation;
import com.github.alexanderjkslfj.securityannotator.util.Parser;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

public class LLMWindow implements ToolWindowFactory{
    private java.util.List<Annotation> toBeApplied = null;

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("LLM Analyzer", SwingConstants.CENTER);

        JButton analyzeButton = new JButton("Run Analysis");
        JButton applyButton = new JButton("Apply Result");
        applyButton.setEnabled(false);
        JButton apiKeySetterButton = new JButton("Enter API Key");
        JButton annotationGenerator = new JButton("generate dummy annotations");

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JBScrollPane scrollPane = new JBScrollPane(outputArea);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(analyzeButton);
        buttonPanel.add(applyButton);
        buttonPanel.add(apiKeySetterButton);
        buttonPanel.add(annotationGenerator);

        panel.add(label, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        apiKeySetterButton.addActionListener(e -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, Settings.class);
        });

        applyButton.addActionListener(e -> {
            if(toBeApplied != null) {
                Annotator.insertFeatureComment(project, toBeApplied);
                toBeApplied = null;
                applyButton.setEnabled(false);
                outputArea.setText("Annotations applied.");
            }
        });

        analyzeButton.addActionListener(e -> {
            applyButton.setEnabled(false);
            analyzeButton.setEnabled(false);
            String promptInput;
            try {
                promptInput = PromptBuilder.buildPrompt(project);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            outputArea.setText("Analyzing code...");

            PromptService service = ApplicationManager
                    .getApplication()
                    .getService(PromptService.class);

            // Send the content to the LLM
            service.prompt(promptInput)
                .thenAccept(response -> ApplicationManager.getApplication().invokeLater(() -> {
                    analyzeButton.setEnabled(true);
                    if(response != null) {
                        java.util.List<Annotation> list = Parser.parse(project, response);
                        if(list != null && !list.isEmpty()) {
                            toBeApplied = list;

                            String text = "Annotations found:\n";
                            for (Annotation a : list) {
                                if(a.start_line() == a.end_line()) {
                                    text += "Line " + a.start_line() + ": " + a.category() + "\n";
                                } else {
                                    text += "Lines " + a.start_line() + " to " + a.end_line() + ": " + a.category() + "\n";
                                }
                            }
                            outputArea.setText(text);

                            applyButton.setEnabled(true);
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
        });

        //currently generates hand made annotations, should later use LLM response data
        annotationGenerator.addActionListener(e -> {
            Annotator.insertFeatureComment(project, CreateSampleAnnotation.createSampleAnnotations());
        });

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
