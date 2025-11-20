package com.github.alexanderjkslfj.securityannotator.toolWindow;

import com.github.alexanderjkslfj.securityannotator.config.Settings;
import com.github.alexanderjkslfj.securityannotator.services.PromptService;
import com.github.alexanderjkslfj.securityannotator.dataPackage.PromptBuilder;

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

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("LLM Analyzer", SwingConstants.CENTER);

        JButton analyzeButton = new JButton("Run Analysis");
        JButton apiKeySetterButton = new JButton("Enter API Key");

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JBScrollPane scrollPane = new JBScrollPane(outputArea);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(analyzeButton);
        buttonPanel.add(apiKeySetterButton);

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
            //currently gets stuck here due to issues with VPN tunnel

            PromptService service = ApplicationManager
                    .getApplication()
                    .getService(PromptService.class);

            // Send the content to the LLM
            service.prompt(promptInput)
                    .thenAccept(response -> ApplicationManager.getApplication().invokeLater(() -> {
                        outputArea.setText("LLM Response:" + response);
                    }))
                    .exceptionally(ex -> {
                        ApplicationManager.getApplication().invokeLater(() -> {
                            outputArea.setText("Error: " + ex.getMessage());
                        });
                        return null;
                    });
        });

        ContentFactory contentFactory = ContentFactory.getInstance();
        Content content = contentFactory.createContent(panel, "", false);
        toolWindow.getContentManager().addContent(content);
    }
}
