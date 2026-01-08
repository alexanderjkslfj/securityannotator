package com.github.alexanderjkslfj.securityannotator.util;

public record Annotation(
        int start_line,
        int end_line,
        String category
){}