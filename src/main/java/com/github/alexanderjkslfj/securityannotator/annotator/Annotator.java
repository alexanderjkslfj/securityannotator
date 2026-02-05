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
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

            PsiFile psiFile = methods.getFirst().getContainingFile();
            List<AnnotationBlock> existingBlocks =
                    collectAnnotationBlocks(psiFile);

            for (LLMResult result : results) {
                PsiMethod method = methodIndex.get(result.methodId());

                if (method == null)  continue;
                if (isMethodAlreadyAnnotated(method, existingBlocks)) continue;
                PsiMethodAnnotator.annotateMethod(project,method, result.featureName());
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

        String startComment = "//&begin [" + ann.category() + "]\n";
        String endComment   = "\n//&end [" + ann.category() + "]";

        document.insertString(endOffset, endComment);
        document.insertString(startOffset, startComment);
    }

    private static boolean isMethodAlreadyAnnotated(
            @NotNull PsiMethod method,
            @NotNull List<AnnotationBlock> blocks
    ) {
        TextRange methodRange = method.getTextRange();

        return blocks.stream()
                .anyMatch(b -> b.range().contains(methodRange));
    }

    private static List<AnnotationBlock> collectAnnotationBlocks(@NotNull PsiFile file) {
        List<PsiComment> comments =
                PsiTreeUtil.findChildrenOfType(file, PsiComment.class)
                        .stream()
                        .filter(c -> c.getText().startsWith("//&"))
                        .sorted(Comparator.comparingInt(c -> c.getTextRange().getStartOffset()))
                        .toList();

        List<AnnotationBlock> blocks = new ArrayList<>();

        PsiComment currentBegin = null;
        String feature = null;

        for (PsiComment comment : comments) {
            String text = comment.getText();

            if (text.startsWith("//&begin")) {
                currentBegin = comment;
                feature = text.substring("//&begin".length()).trim();
            } else if (text.startsWith("//&end") && currentBegin != null) {
                TextRange range = new TextRange(
                        currentBegin.getTextRange().getStartOffset(),
                        comment.getTextRange().getEndOffset()
                );
                blocks.add(new AnnotationBlock(range, feature));
                currentBegin = null;
                feature = null;
            }
        }

        return blocks;
    }
}
