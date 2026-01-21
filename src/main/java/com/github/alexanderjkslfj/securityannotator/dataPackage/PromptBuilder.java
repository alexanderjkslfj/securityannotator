package com.github.alexanderjkslfj.securityannotator.dataPackage;

import java.io.IOException;

import com.github.alexanderjkslfj.securityannotator.util.Parser;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class PromptBuilder {

    public static String buildPrompt(@NotNull Project project) throws IOException {
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
}
