package com.github.alexanderjkslfj.securityannotator.annotator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class PsiMethodAnnotator {

    public static void annotateMethod(
            @NotNull Project project,
            @NotNull PsiMethod method,
            @NotNull String annotationName
    ) {
        PsiFile psiFile = method.getContainingFile();
        if (psiFile == null) return;

        Document document = PsiDocumentManager
                .getInstance(project)
                .getDocument(psiFile);

        if (document == null) return;

        WriteCommandAction.runWriteCommandAction(project, () -> {

            PsiDocumentManager.getInstance(project).commitDocument(document);

            int startOffset = method.getTextRange().getStartOffset();
            int endOffset   = method.getTextRange().getEndOffset();

            String startComment = "//&begin [" + annotationName + "]\n";
            String endComment   = "\n//&end [" + annotationName + "]\n";

            // Insert END first
            document.insertString(endOffset, endComment);

            // Insert START second
            document.insertString(startOffset, startComment);

            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
    }
}
