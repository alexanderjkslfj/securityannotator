package com.github.alexanderjkslfj.securityannotator.annotator;

import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;

public class Annotator {
    public static void insertFeatureComment(@NotNull Project project, List<Annotation>annotations){
        {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

            if (editor == null) {
                return; // in case editor is open
            }

            Document document = editor.getDocument();
            PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(project);


            // Sort annotations descending by endLine so edits don't shift earlier ones
            annotations.sort(Comparator.comparingInt(a -> -a.getEndingRow()));

            WriteCommandAction.runWriteCommandAction(project, () -> {

                psiMgr.commitDocument(document);

                for (Annotation ann : annotations) {
                    insertAnnotation(document, ann);
                }

                psiMgr.commitDocument(document);
            });
        }
    }

    private static void insertAnnotation(Document document, Annotation ann) {

        int startOffset = document.getLineStartOffset(ann.getStartingRow()-1);
        int endOffset   = document.getLineEndOffset(ann.getEndingRow()-1);

        String startComment = "// " + ann.getAnnotationText() + "\n";
        String endComment   = "\n// end of " + ann.getAnnotationText() + "\n";

        document.insertString(endOffset, endComment);
        document.insertString(startOffset, startComment);
    }
}
