package com.github.alexanderjkslfj.securityannotator.dataPackage;

import java.io.IOException;
import java.util.List;

import com.github.alexanderjkslfj.securityannotator.util.Parser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.alexanderjkslfj.securityannotator.annotator.MethodGatherer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;

public class PromptBuilder {
        public static String buildTextPrompt(@NotNull Project project) throws IOException {
                String promptText1 = "I want a section of code to be searched for security features. I'm looking for the following categories of security features (formatted as a JSON array):\n";
                String taxonomy = new TaxonomyReader().readTaxonomy();
                String promptText2 = "\nWhenever you find a piece of code that matches a category, give me the code piece together with its category.\n";
                String promptText22 = "When multiple consecutive lines share the same category, bundle them into one.\n";
                String promptText3 = "Answer in JSON format, like this: ";
                String responseStructure = Parser.getStructureExample();
                String promptText4 = "\nEverything that follows is the code that is to be searched:\n";
                String openedClass = OpenedClass.getCurrentPage(project);
                return promptText1 + taxonomy + promptText2 + promptText22 + promptText3 + responseStructure + promptText4 + openedClass;
        }

    public static String buildPsiPrompt(@NotNull Project project) throws IOException {
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
