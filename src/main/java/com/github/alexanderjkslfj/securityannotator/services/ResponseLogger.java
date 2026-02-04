package com.github.alexanderjkslfj.securityannotator.services;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.diagnostic.Logger;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;

public class ResponseLogger {

    private static Path getLlmLogFile() {
        Path logDir = Path.of(PathManager.getLogPath(), "security-annotator");

        try {
            Files.createDirectories(logDir);
        } catch (IOException e) {
            throw new RuntimeException("Failed to create log directory", e);
        }

        return logDir.resolve("llm-responses.log");
    }

    public static void logLlmResponse(
            @NotNull String provider,
            @NotNull String response
    ) {
        Path logFile = getLlmLogFile();

        String entry = """
            ===== %s =====
            Provider: %s
            %s

            """.formatted(
                Instant.now(),
                provider,
                response
        );

        try {
            Files.writeString(
                    logFile,
                    entry,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            Logger.getInstance("SecurityAnnotator")
                    .warn("Failed to write LLM response log", e);
        }
    }
}
