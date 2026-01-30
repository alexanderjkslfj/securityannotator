package com.github.alexanderjkslfj.securityannotator.toolWindow;

public enum AnalyzationMethod {
    PSI_TREE("Java Methods"),
    RAW_TEXT("Individual Lines");

    private final String displayName;

    AnalyzationMethod(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
