package ru.eljke.driftguard.core.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CoreArchitectureTest {
    private static final List<String> FORBIDDEN_REFERENCES = List.of(
            "org.springframework",
            "org.apache.kafka",
            "ru.eljke.driftguard.algorithms",
            "ru.eljke.driftguard.kafka",
            "ru.eljke.driftguard.spring",
            "ru.eljke.driftguard.demo",
            "ru.eljke.driftguard.testkit"
    );

    @Test
    void coreDoesNotDependOnAdaptersAlgorithmsOrDemo() throws IOException {
        List<String> violations = findForbiddenReferencesInSourceRoot(Path.of("src/main/java"), FORBIDDEN_REFERENCES);

        assertTrue(
                violations.isEmpty(),
                () -> "Core must stay framework-agnostic and must not depend on adapters, algorithms, demo or testkit:\n"
                        + String.join("\n", violations)
        );
    }

    private static List<String> findForbiddenReferencesInSourceRoot(Path sourceRoot, List<String> forbiddenReferences) throws IOException {
        try (Stream<Path> files = Files.walk(sourceRoot)) {
            return files
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> findForbiddenReferencesInSourceFile(path, forbiddenReferences).stream())
                    .toList();
        }
    }

    private static List<String> findForbiddenReferencesInSourceFile(Path sourceFile, List<String> forbiddenReferences) {
        try {
            String content = Files.readString(sourceFile);
            return forbiddenReferences.stream()
                    .filter(content::contains)
                    .map(reference -> sourceFile + " -> " + reference)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read " + sourceFile, exception);
        }
    }
}
