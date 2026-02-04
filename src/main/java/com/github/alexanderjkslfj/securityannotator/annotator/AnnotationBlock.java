package com.github.alexanderjkslfj.securityannotator.annotator;
import com.intellij.openapi.util.TextRange;

public record AnnotationBlock(
        TextRange range,
        String featureName) {
}
