package com.github.alexanderjkslfj.securityannotator.util;

import org.jetbrains.annotations.NotNull;

public class Annotation {
    public int start_line;
    public int end_line;
    public @NotNull Category category;

    public Annotation(int start_line, int end_line, @NotNull Category category) {
        this.start_line = start_line;
        this.end_line = end_line;
        this.category = category;
    }

    public Annotation(int start_line, int end_line, @NotNull String[] category) {
        this.start_line = start_line;
        this.end_line = end_line;
        this.category = new Category(category);
    }

    public Annotation(int start_line, int end_line, @NotNull String category) {
        this.start_line = start_line;
        this.end_line = end_line;
        this.category = new Category(category);
    }

    @Override
    public @NotNull String toString() {
        return "Annotation from line " +  start_line + " to line " + end_line + " (" + category + ")";
    }
}