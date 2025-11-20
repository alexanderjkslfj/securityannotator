package com.github.alexanderjkslfj.securityannotator.dataPackage;

import java.io.IOException;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class PromptBuilder {

    public static String buildPrompt(@NotNull Project project) throws IOException {
        String prompt = "";
        String promptText = "You are a code security analyzer.\n given the following taxonomy:\n";
        String taxonomy = new TaxonomyReader().readTaxonomy();
        String promptText2 = "analyze the following code segment and find security features that match the ones in the taxonomy:\n ";
        String openedClass = OpenedClass.getCurrentPage(project);
        String promptText3 = "please return the features in a format matching the taxonomy. If no security features were found, please simply reply with 'no security features found'";
        prompt = promptText + taxonomy + promptText2 + openedClass + promptText3 ;
        return prompt;
    }
}
