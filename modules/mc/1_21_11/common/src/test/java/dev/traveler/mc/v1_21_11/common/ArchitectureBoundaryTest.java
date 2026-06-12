package dev.traveler.mc.v1_21_11.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {
    private static final List<String> FORBIDDEN_IMPORTS = List.of("net.fabricmc", "net.neoforged");
    private static final List<String> FORBIDDEN_PACKAGES = List.of(
            "/common/event/",
            "\\common\\event\\",
            "/common/render/",
            "\\common\\render\\",
            "/common/debug/",
            "\\common\\debug\\");

    @Test
    void commonDoesNotImportLoaderSpecificApis() throws IOException {
        List<String> violations = findForbiddenImports(Path.of("src/main/java"), FORBIDDEN_IMPORTS);

        assertTrue(violations.isEmpty(), () -> "Loader imports in common: " + violations);
    }

    @Test
    void commonDoesNotDefineCoreFrameworkPackages() throws IOException {
        List<String> violations = findForbiddenPackages(Path.of("src/main/java"), FORBIDDEN_PACKAGES);

        assertTrue(violations.isEmpty(), () -> "Reusable framework packages in common: " + violations);
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

    private static List<String> findForbiddenPackages(Path sourceRoot, List<String> forbiddenPackages)
            throws IOException {
        try (var files = Files.walk(sourceRoot)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> containsAny(path.toString(), forbiddenPackages))
                    .map(Path::toString)
                    .toList();
        }
    }

    private static boolean containsAny(String value, List<String> forbiddenPackages) {
        return forbiddenPackages.stream().anyMatch(value::contains);
    }

    private static String read(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to read " + path, exception);
        }
    }
}
