package com.github.alexanderjkslfj.securityannotator.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexanderjkslfj.securityannotator.dataPackage.OpenedClass;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class Parser {
    private static final @NotNull ObjectMapper JSON_PARSER = new ObjectMapper();

    /// Parse the LLM response. Returns `null` if LLM response does not contain valid JSON.
    public static @Nullable List<Annotation> parse(@NotNull Project project, @NotNull String llmResponse) {
        int start = llmResponse.indexOf('[');
        if (start == -1) {
            return null;
        }
        int end = llmResponse.lastIndexOf(']');
        if (end == -1) {
            return null;
        }
        if (start >= end) {
            return null;
        }
        String json = llmResponse.substring(start, end + 1);
        List<RawAnnotation> list = parseJSON(json);
        if (list == null) {
            return null;
        }
        return findCode(project, list);
    }

    /// Parse the (potentially erroneous) JSON.
    private static @Nullable List<RawAnnotation> parseJSON(@NotNull String json) {
        try {
            return JSON_PARSER.readValue(json, new TypeReference<>() {});
        } catch(JsonProcessingException e) {
            return null;
        }
    }

    /// Find the lines of code the raw annotations refer to.
    private static @NotNull List<Annotation> findCode(@NotNull Project project, @NotNull List<RawAnnotation> rawAnnotations) throws RuntimeException {
        String page_code = OpenedClass.getCurrentPage(project);
        if(page_code == null) {
            throw new RuntimeException("current page is null");
        }

        List<Annotation> result = new ArrayList<>(rawAnnotations.size());

        for (RawAnnotation a: rawAnnotations) {
            String annotation_code = a.code();
            if(annotation_code == null || annotation_code.isEmpty()) {
                continue;
            }
            String category = a.category();
            if(category == null) {
                continue;
            }
            int start = page_code.indexOf(annotation_code);
            if(start == -1) {
                continue;
            }
            int end = start + annotation_code.length();
            int start_line = countNewlines(page_code.substring(0, start));
            int end_line = countNewlines(page_code.substring(0, end + 1));
            Annotation annotation = new Annotation(start_line, end_line, category);
            result.add(annotation);
        }

        return result;
    }

    /// Count the number of newlines in the string.
    private static int countNewlines(@NotNull String str) {
        return (int) Pattern.compile("\\R").matcher(str).results().count();
    }

    /// Retrieve an example of the JSON structure this parser can parse.
    /// This should be passed to the LLM.
    public static @NotNull String getStructureExample() {
        return "[{\"code\": \"some code\", \"category\": \"access control\"}, {\"code\": \"other code\", \"category\": \"cryptography.encryption\"}, ...]";
    }
}

record RawAnnotation (String code, String category){}