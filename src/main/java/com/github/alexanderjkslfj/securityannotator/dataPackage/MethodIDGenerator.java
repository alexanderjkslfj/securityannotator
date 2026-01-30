package com.github.alexanderjkslfj.securityannotator.dataPackage;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.stream.Collectors;

public class MethodIDGenerator {

    public static @NotNull String methodId(@NotNull PsiMethod method) {
        PsiClass cls = method.getContainingClass();
        String className = cls != null ? cls.getQualifiedName() : "<anonymous>";

        String params = Arrays.stream(method.getParameterList().getParameters())
                .map(p -> p.getType().getCanonicalText())
                .collect(Collectors.joining(","));

        return className + "#" + method.getName() + "(" + params + ")";
    }
}
