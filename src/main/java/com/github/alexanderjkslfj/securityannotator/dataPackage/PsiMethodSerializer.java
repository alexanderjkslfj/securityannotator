package com.github.alexanderjkslfj.securityannotator.dataPackage;

import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public class PsiMethodSerializer {
    public static @NotNull MethodContents serialize(@NotNull PsiMethod method) {
        String id = MethodIDGenerator.methodId(method);

        String signature = method.getText()
                .substring(0, method.getText().indexOf('{'))
                .trim();

        String body = method.getBody() != null
                ? method.getBody().getText()
                : "";

        return new MethodContents(
                id,
                method.getName(),
                signature,
                body
        );
    }
}
