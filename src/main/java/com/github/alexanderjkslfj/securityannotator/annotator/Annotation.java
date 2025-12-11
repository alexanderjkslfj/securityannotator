package com.github.alexanderjkslfj.securityannotator.annotator;

public class Annotation {
    private int startingRow;
    private int endingRow;
    private String annotationText;

    public Annotation(int startingRow, int endingRow, String annotationText) {
        this.startingRow = startingRow;
        this.endingRow = endingRow;
        this.annotationText = annotationText;
    }

    public int getStartingRow() {
        return startingRow;
    }
    public int getEndingRow() {
        return endingRow;
    }
    public String getAnnotationText() {
        return annotationText;
    }
}
