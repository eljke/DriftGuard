package ru.eljke.driftguard.spring.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringStarterArchitectureTest {
    private static final List<String> FORBIDDEN_REFERENCES = List.of(
            "ru.eljke.driftguard.demo",
            "ru.eljke.driftguard.testkit"
    );

    @Test
    void springStarterDoesNotDependOnDemoOrTestkit() throws IOException {
        List<String> violations = findForbiddenReferencesInSourceRoot(Path.of("src/main/java"), FORBIDDEN_REFERENCES);

        assertTrue(
                violations.isEmpty(),
                () -> "Spring Boot starter must not depend on demo or testkit modules:\n"
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
