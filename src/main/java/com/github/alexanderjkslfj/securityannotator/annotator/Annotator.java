package com.github.alexanderjkslfj.securityannotator.annotator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexanderjkslfj.securityannotator.dataPackage.MethodIDGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

import com.github.alexanderjkslfj.securityannotator.util.Annotation;

public class Annotator {
    public static void insertFeatureCommentByText(@NotNull Project project, String LLMResponse) throws JsonProcessingException {
        {
            List<PsiMethod> methods = MethodGatherer.collectMethods(project);

            Map<String, PsiMethod> methodIndex =
                    methods.stream().collect(toMap(
                            MethodIDGenerator::methodId,
                            Function.identity()
                    ));

            ObjectMapper mapper = new ObjectMapper();
            List<LLMResult> results =
                    mapper.readValue(LLMResponse, new TypeReference<>() {});

            for (LLMResult result : results) {
                PsiMethod method = methodIndex.get(result.methodId());
                if (method != null) {
                    PsiMethodAnnotator.annotateMethod(project,method, result.featureName());
                }
            }
        }
    }

    public static void insertFeatureCommentByAnnotation(@NotNull Project project, List<Annotation>annotations){
        {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

            if (editor == null) {
                return; // in case editor is open
            }

            Document document = editor.getDocument();
            PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(project);


            // Sort annotations descending by endLine so edits don't shift earlier ones
            annotations.sort(Comparator.comparingInt(a -> -a.end_line()));

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

        int startOffset = document.getLineStartOffset(ann.start_line()-1);
        int endOffset   = document.getLineEndOffset(ann.end_line()-1);

        String startComment = "// " + ann.category() + "\n";
        String endComment   = "\n// end of " + ann.category() + "\n";

        document.insertString(endOffset, endComment);
        document.insertString(startOffset, startComment);
    }
}
