package dev.traveler.mc.v1_21_11.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "dev.traveler.core.path",
            "dev.traveler.core.graph",
            "dev.traveler.core.smooth",
            "dev.traveler.core.world");

    @Test
    void fabricDoesNotImplementPathfinderLogicDirectly() throws IOException {
        List<String> violations = findForbiddenImports(Path.of("src/main/java"), FORBIDDEN_IMPORTS);

        assertTrue(violations.isEmpty(), () -> "Core imports in fabric bootstrap: " + violations);
    }

    private static List<String> findForbiddenImports(Path sourceRoot, List<String> forbiddenImports)
            throws IOException {
        try (var files = Files.walk(sourceRoot)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(path -> violationsIn(path, forbiddenImports).stream())
                    .toList();
        }
    }

    private static List<String> violationsIn(Path path, List<String> forbiddenImports) {
        String source = read(path);
        return forbiddenImports.stream()
                .filter(source::contains)
                .map(forbidden -> path + " imports " + forbidden)
                .toList();
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
