package com.github.alexanderjkslfj.securityannotator.annotator;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MethodGatherer {
    public static @NotNull List<PsiMethod> collectMethods(@NotNull Project project) {

        Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
        if (editor == null) {
            return List.of();
        }

        PsiFile psiFile = PsiDocumentManager
                .getInstance(project)
                .getPsiFile(editor.getDocument());

        if (psiFile == null) {
            return List.of();
        }

        Collection<PsiMethod> methods =
                PsiTreeUtil.findChildrenOfType(psiFile, PsiMethod.class);

        return new ArrayList<>(methods);
    }
}
