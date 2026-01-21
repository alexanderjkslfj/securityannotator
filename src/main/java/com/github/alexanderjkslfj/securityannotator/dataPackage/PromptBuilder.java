package com.github.alexanderjkslfj.securityannotator.dataPackage;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.alexanderjkslfj.securityannotator.annotator.MethodGatherer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

//import static jdk.internal.org.jline.reader.impl.LineReaderImpl.CompletionType.List;

public class PromptBuilder {

    public static String buildPrompt(@NotNull Project project) throws IOException {

        List<PsiMethod> methods = MethodGatherer.collectMethods(project);

        List<MethodContents> payloads = methods.stream()
                .map(PsiMethodSerializer::serialize)
                .toList();

        ObjectNode root = JsonNodeFactory.instance.objectNode();
        root.put("instructions",
                "For each method that corresponds to a security feature from the taxonomy, return an object with: " +
                        "methodId, featureName (or null). " +
                        "Only use the provided methodId values. " +
                        "Return ONLY valid JSON."
        );
        root.set("methods", new ObjectMapper().valueToTree(payloads));

        String promptText1 = "I want you to search methods of my code and determine if they map to security features as categorized in the following taxonomy:\n";
        String taxonomy = new TaxonomyReader().readTaxonomy();
        String promptText2 = "\nThe methods that I want to be checked are:\n";
        return promptText1 + taxonomy + promptText2 + root.toPrettyString();
    }
}
