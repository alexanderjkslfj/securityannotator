package com.github.alexanderjkslfj.securityannotator.annotator;


import java.util.ArrayList;
import java.util.List;

import com.github.alexanderjkslfj.securityannotator.util.Annotation;

public class CreateSampleAnnotation {
    public static List<Annotation> createSampleAnnotations () {
        List<Annotation> annotations = new ArrayList<Annotation>();
        annotations.add(new Annotation (40,50,"security feature 1"));
        annotations.add(new Annotation (52,54,"security feature 2"));
        return annotations;
    }
}
