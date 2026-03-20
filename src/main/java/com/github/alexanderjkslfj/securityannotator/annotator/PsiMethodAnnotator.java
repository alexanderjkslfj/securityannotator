package com.github.alexanderjkslfj.securityannotator.annotator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

public class PsiMethodAnnotator {
    /// adds the HanS annotations in front and after the method in the editor window
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

            int lastDot = annotationName.lastIndexOf('.');
            String annotationNameShort = annotationName.substring(lastDot + 1);

            String startComment = "//&begin [" + annotationNameShort + "]\n";
            String endComment   = "\n//&end [" + annotationNameShort + "]";

            /// Insert END first
            document.insertString(endOffset, endComment);

            /// Insert START second
            document.insertString(startOffset, startComment);

            PsiDocumentManager.getInstance(project).commitDocument(document);
        });
    }
}
