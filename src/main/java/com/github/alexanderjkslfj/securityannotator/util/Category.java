package com.github.alexanderjkslfj.securityannotator.util;

import org.jetbrains.annotations.NotNull;

/// A record containing an annotation name, providing several utility methods.
public class Category {
    String[] category;

    public Category(String[] category) {
        this.category = category;
    }

    public Category(String category) {
        this.category = splitCategory(category);
    }

    public @NotNull String[] toArray() {
        return category.clone();
    }

    @Override
    public @NotNull String toString() {
        return joinCategory(category);
    }

    public void setCategory(@NotNull String[] category) {
        this.category = category;
    }

    public void setCategory(@NotNull String category) {
        this.category = splitCategory(category);
    }

    public int length() {
        return category.length;
    }

    /// Check whether two Categories overlap even if they're non-equal, e.g. "a.b.c" and "a.b"
    public boolean overlaps(@NotNull Category other) {
        for(int i = 0; i < Math.min(this.category.length, other.category.length); i++) {
            if(!this.category[i].equals(other.category[i])) return false;
        }
        return true;
    }

    /// Convert a Category from array form to string form
    public static @NotNull String joinCategory(@NotNull String[] category) {
        return String.join(".", category);
    }

    /// Convert a Category from string from to array form
    public static @NotNull String[] splitCategory(@NotNull String category) {
        return category.split("\\.");
    }
}
