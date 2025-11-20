package com.github.alexanderjkslfj.securityannotator.dataPackage;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OpenedClass {

    public @Nullable static String getCurrentPage(@NotNull Project project) {

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

        //if not editor is selected
        if (editor == null) {
            return null;
        }

        Document document = editor.getDocument();
        return document.getText();
    }
}
