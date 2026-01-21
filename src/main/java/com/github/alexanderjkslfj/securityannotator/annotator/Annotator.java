package com.github.alexanderjkslfj.securityannotator.annotator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.alexanderjkslfj.securityannotator.dataPackage.MethodIDGenerator;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

public class Annotator {
    public static void insertFeatureComment(@NotNull Project project, String LLMResponse) throws JsonProcessingException {
        {
            List<PsiMethod> methods = MethodGatherer.collectMethods(project);

            Map<String, PsiMethod> methodIndex =
                    methods.stream().collect(toMap(
                            MethodIDGenerator::methodId,
                            Function.identity()
                    ));

            ObjectMapper mapper = new ObjectMapper();
            List<LLMResult> results =
                    mapper.readValue(LLMResponse, new TypeReference<>() {});

            for (LLMResult result : results) {
                PsiMethod method = methodIndex.get(result.methodId());
                if (method != null) {
                    PsiMethodAnnotator.annotateMethod(project,method, result.featureName());
                }
            }

            // VERY IMPORTANT: sort bottom → top to avoid offset shifting
            /*methods.sort((a, b) ->
                    Integer.compare(
                            b.getTextRange().getStartOffset(),
                            a.getTextRange().getStartOffset()
                    )
            );

            for (PsiMethod method : methods) {
                PsiMethodAnnotator.annotateMethod(
                        project,
                        method,
                        "DUMMY SECURITY FEATURE"
                );
            }*/
        }
    }
}
