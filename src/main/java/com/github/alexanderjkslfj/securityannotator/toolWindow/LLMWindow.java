package com.github.alexanderjkslfj.securityannotator.toolWindow;

import com.github.alexanderjkslfj.securityannotator.config.Settings;
import com.github.alexanderjkslfj.securityannotator.services.PromptService;
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

public class LLMWindow implements ToolWindowFactory{

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {

        JPanel panel = new JPanel(new BorderLayout());

        JLabel label = new JLabel("LLM Analyzer", SwingConstants.CENTER);

        JButton analyzeButton = new JButton("Run Analysis");
        JButton settingsButton = new JButton("Enter API Key");

        JTextArea outputArea = new JTextArea();
        outputArea.setEditable(false);
        JBScrollPane scrollPane = new JBScrollPane(outputArea);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(analyzeButton);
        buttonPanel.add(settingsButton);

        panel.add(label, BorderLayout.NORTH);
        panel.add(buttonPanel, BorderLayout.CENTER);
        panel.add(scrollPane, BorderLayout.SOUTH);

        settingsButton.addActionListener(e -> {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, Settings.class);
        });

        analyzeButton.addActionListener(e -> {
            outputArea.setText(" Sending to LLM...");

            PromptService service = ApplicationManager
                    .getApplication()
                    .getService(PromptService.class);

            // Send the content to the LLM
            service.prompt("do some security stuff")
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
