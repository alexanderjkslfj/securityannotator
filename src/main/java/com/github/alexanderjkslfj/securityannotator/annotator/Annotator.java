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
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;

import com.github.alexanderjkslfj.securityannotator.util.Annotation;
import org.jetbrains.annotations.Nullable;

record Change (
        boolean shouldExist,
        Annotation annotation
){}

record AnnotationPart (
        boolean isStart,
        String category,
        int line
){}

record ChangePart (
        boolean shouldExist,
        AnnotationPart part
){}

public class Annotator {
    public static void insertFeatureCommentByText(@NotNull Project project, @NotNull String LLMResponse) throws JsonProcessingException {
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

    public static void insertFeatureCommentByAnnotation(@NotNull Project project, @NotNull List<Annotation> annotations) throws RuntimeException{
        {
            Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();

            if (editor == null) {
                return; // in case editor is open
            }

            Document document = editor.getDocument();

            List<Annotation> existing = getExistingAnnotations(document);
            if(existing == null) throw new RuntimeException("Existing annotations are invalid.");
            List<Annotation> combined = new ArrayList<>();
            combined.addAll(existing);
            combined.addAll(annotations);
            List<Annotation> changed = deduplicateAnnotations(combined);
            List<Change> changes = getChanges(existing, changed);
            // Sort annotations descending by line so edits don't shift earlier ones
            List<ChangePart> parts = getParts(changes);
            parts.sort(Comparator.comparingInt(a -> -a.part().line()));

            PsiDocumentManager psiMgr = PsiDocumentManager.getInstance(project);
            WriteCommandAction.runWriteCommandAction(project, () -> {
                psiMgr.commitDocument(document);

                for (ChangePart ann : parts) {
                    if(ann.shouldExist()) {
                        insertAnnotationPart(document, ann.part());
                    } else {
                        removeAnnotationPart(document, ann.part());
                    }
                }

                psiMgr.commitDocument(document);
            });
        }
    }

    private static @NotNull List<ChangePart> getParts(@NotNull List<Change> changes) {
        List<ChangePart> parts = new ArrayList<>();
        for (Change change : changes) {
            parts.add(new ChangePart(change.shouldExist(), new AnnotationPart( true, change.annotation().category(), change.annotation().start_line())));
            parts.add(new ChangePart(change.shouldExist(), new AnnotationPart(false, change.annotation().category(), change.annotation().end_line())));
        }
        return parts;
    }

    private static @NotNull List<Change> getChanges(@NotNull List<Annotation> oldAnns, @NotNull List<Annotation> newAnns) {
        List<Change> changes =  new ArrayList<>();

        olds: for (Annotation oldAnn : oldAnns) {
            for (Annotation newAnn : newAnns) {
                if(oldAnn.start_line() == newAnn.start_line() && oldAnn.end_line() == newAnn.end_line() && oldAnn.category().equals(newAnn.category())) {
                    continue olds;
                }
            }
            changes.add(new Change(false, oldAnn));
        }

        news: for (Annotation newAnn : newAnns) {
            for (Annotation oldAnn : oldAnns) {
                if(oldAnn.start_line() == newAnn.start_line() && oldAnn.end_line() == newAnn.end_line() && oldAnn.category().equals(newAnn.category())) {
                    continue news;
                }
            }
            changes.add(new Change(true, newAnn));
        }

        return changes;
    }

    public static @NotNull List<Annotation> deduplicateAnnotations(@NotNull List<Annotation> annotations) {
        annotations.sort(Comparator.comparingInt(Annotation::start_line));

        List<Annotation> result = new ArrayList<>();
        annotationloop: for (Annotation fresh : annotations) {
            for (int i = result.size() - 1; i >= 0; i--) {
                Annotation open = result.get(i);
                if(fresh.start_line() > open.end_line() + 1) {
                    continue;
                }
                if(fresh.category().equals(open.category())) {
                    if(fresh.end_line() > open.end_line()) {
                        result.set(i, new Annotation(open.start_line(), fresh.end_line(), fresh.category()));
                    }
                    continue annotationloop;
                }
            }
            result.add(fresh);
        }

        return result;
    }

    private static final Pattern BEGIN_ANNOTATION = Pattern.compile("//\\s*&(begin|end)\\s*\\[\\s*([^\\]]*?)\\s*\\]");

    public static @NotNull String removeAnnotations(@NotNull String code) {
        return BEGIN_ANNOTATION.matcher(code).replaceAll("");
    }

    private static @Nullable List<Annotation> getExistingAnnotations(@NotNull Document document) {
        List<Annotation> existingAnnotations = new ArrayList<>();

        List<Annotation> unfinishedAnnotations = new ArrayList<>();

        List<String> lines = document.getText().lines().toList();
        for(int lineIdx = 0; lineIdx < lines.size(); lineIdx++) {
            var m = BEGIN_ANNOTATION.matcher(lines.get(lineIdx));
            if(!m.find()) {
                continue;
            }
            if(m.group(1).equals("begin")) {
                Annotation ann = new Annotation(lineIdx, -1, m.group(2));
                unfinishedAnnotations.add(ann);
            } else if (!unfinishedAnnotations.isEmpty()) {
                Annotation last = unfinishedAnnotations.removeLast();
                if(!m.group(2).equals(last.category())) {
                    return null;
                }
                Annotation full = new Annotation(last.start_line(), lineIdx, last.category());
                existingAnnotations.add(full);
            } else {
                return null;
            }
        }
        if(!unfinishedAnnotations.isEmpty()) {
            return null;
        }

        return existingAnnotations;
    }

    private static void insertAnnotationPart(@NotNull Document document, @NotNull AnnotationPart annPart) {
        if (annPart.isStart()) {
            int offset = document.getLineStartOffset(annPart.line()-1);
            String comment = "//&begin [" + annPart.category() + "]\n";
            document.insertString(offset, comment);
        } else {
            int offset = document.getLineEndOffset(annPart.line()-1);
            String comment = "\n//&end [" + annPart.category() + "]";
            document.insertString(offset, comment);
        }
    }

    private static void removeAnnotationPart(@NotNull Document document, @NotNull AnnotationPart annPart) {
        int startOffset = document.getLineStartOffset(annPart.line()-1);
        int endOffset = document.getLineEndOffset(annPart.line()-1);
        String lineText = document.getText(new TextRange(startOffset, endOffset));
        var m = BEGIN_ANNOTATION.matcher(lineText);
        if(!m.find()) return;
        int index = m.start();
        document.deleteString(index, endOffset + 1);
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
