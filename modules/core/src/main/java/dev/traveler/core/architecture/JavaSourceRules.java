package dev.traveler.core.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class JavaSourceRules {
    private JavaSourceRules() {}

    public static List<String> forbiddenImports(Path sourceRoot, List<String> forbiddenImports)
            throws IOException {
        return javaFiles(sourceRoot)
                .flatMap(path -> violationsIn(path, forbiddenImports).stream())
                .toList();
    }

    public static List<String> forbiddenPackages(Path sourceRoot, List<String> forbiddenPackages)
            throws IOException {
        return javaFiles(sourceRoot)
                .filter(path -> containsAny(path.toString(), forbiddenPackages))
                .map(Path::toString)
                .toList();
    }

    public static List<String> directJavaSources(Path sourceRoot) throws IOException {
        try (var files = Files.list(sourceRoot)) {
            return files.filter(Files::isRegularFile)
                    .filter(hasJavaSuffix())
                    .map(Path::toString)
                    .toList();
        }
    }

    private static Stream<Path> javaFiles(Path sourceRoot) throws IOException {
        return Files.walk(sourceRoot).filter(Files::isRegularFile).filter(hasJavaSuffix());
    }

    private static Predicate<Path> hasJavaSuffix() {
        return path -> path.toString().endsWith(".java");
    }

    private static List<String> violationsIn(Path path, List<String> forbiddenImports) {
        String source = read(path);
        return forbiddenImports.stream()
                .filter(source::contains)
                .map(forbidden -> path + " imports " + forbidden)
                .toList();
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
