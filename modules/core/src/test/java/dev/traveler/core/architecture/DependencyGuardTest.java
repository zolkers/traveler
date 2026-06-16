package dev.traveler.core.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DependencyGuardTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final String CAPABILITY_FEATURE_ROOT = "dev.traveler.core.capability.";
    private static final String ROUTE_TRAVERSAL_FEATURE_ROOT = "dev.traveler.core.route.internal.features.";
    private static final Pattern CONCRETE_API_DECLARATION =
            Pattern.compile("\\b(class|record|enum)\\s+(Default|Standard|NoOp|Identity)\\w*\\b");

    private record ImportReference(Path source, String importedType) {}

    @Test
    void sharedSpatialTypesMustNotLiveUnderNavigation() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(
                SOURCE_ROOT,
                List.of("dev.traveler.core.navigation.spatial."));

        assertTrue(violations.isEmpty(), () -> "Legacy navigation spatial imports: " + violations);
    }

    @Test
    void worldMustNotImportRouteOrNavigation() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(
                SOURCE_ROOT.resolve("dev/traveler/core/world"),
                List.of("dev.traveler.core.route.", "dev.traveler.core.navigation."));

        assertTrue(violations.isEmpty(), () -> "World dependency violations: " + violations);
    }

    @Test
    void routeMustNotImportNavigation() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(
                SOURCE_ROOT.resolve("dev/traveler/core/route"),
                List.of("dev.traveler.core.navigation."));

        assertTrue(violations.isEmpty(), () -> "Route dependency violations: " + violations);
    }

    @Test
    void coreProductionMustNotImportPlatformApis() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(
                SOURCE_ROOT,
                List.of("dev.traveler.mc.", "net.minecraft.", "net.fabricmc."));

        assertTrue(violations.isEmpty(), () -> "Core platform dependency violations: " + violations);
    }

    @Test
    void domainsMustNotImportOtherDomainsInternalPackages() throws IOException {
        List<String> violations = crossDomainInternalImports();

        assertTrue(violations.isEmpty(), () -> "Cross-domain internal imports: " + violations);
    }

    @Test
    void internalArchitectureClassesMustNotUseTravelerPrefix() throws IOException {
        List<String> violations = productionSources(SOURCE_ROOT).stream()
                .filter(path -> containsPathSegment(path, "pathfinder") || containsPathSegment(path, "capability"))
                .filter(path -> path.getFileName().toString().startsWith("Traveler"))
                .map(SOURCE_ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), () -> "Internal architecture classes must not use Traveler prefix: "
                + violations);
    }

    @Test
    void featureImplementationsMustNotImportOtherFeatureInternals() throws IOException {
        List<String> violations = importReferences(SOURCE_ROOT).stream()
                .filter(DependencyGuardTest::forbiddenFeatureInternalImport)
                .map(DependencyGuardTest::formatImportReference)
                .toList();

        assertTrue(violations.isEmpty(), () -> "Feature implementation imports another feature internal package: "
                + violations);
    }

    @Test
    void concreteFeatureImplementationsMustStayOutOfApiPackages() throws IOException {
        List<String> violations = productionSources(SOURCE_ROOT).stream()
                .filter(path -> containsPathSegment(path, "api"))
                .filter(DependencyGuardTest::concreteApiImplementationDeclaration)
                .map(SOURCE_ROOT::relativize)
                .map(Path::toString)
                .toList();

        assertTrue(violations.isEmpty(), () -> "Concrete implementations belong in impl/noop, not api: "
                + violations);
    }

    @Test
    void featureInternalImportDetectionCoversRouteTraversalFeatures() {
        Path routeWalkFeature = Path.of(
                "src/main/java/dev/traveler/core/route/internal/features/walk/WalkSurfaceTraversalFeature.java");
        Path capabilityPathFeature = Path.of(
                "src/main/java/dev/traveler/core/capability/path/PathCapability.java");

        assertFalse(forbiddenFeatureInternalImport(new ImportReference(
                routeWalkFeature,
                "dev.traveler.core.route.internal.features.walk.internal.WalkSupport")));
        assertTrue(forbiddenFeatureInternalImport(new ImportReference(
                routeWalkFeature,
                "dev.traveler.core.route.internal.features.climb.internal.ClimbSupport")));
        assertFalse(forbiddenFeatureInternalImport(new ImportReference(
                capabilityPathFeature,
                "dev.traveler.core.capability.path.internal.PathSupport")));
        assertTrue(forbiddenFeatureInternalImport(new ImportReference(
                capabilityPathFeature,
                "dev.traveler.core.capability.debug.internal.DebugSupport")));
    }

    @Test
    void importParsingHandlesStaticImportsAndInternalPackageSegments() {
        Path routeWalkFeature = Path.of(
                "src/main/java/dev/traveler/core/route/internal/features/walk/WalkSurfaceTraversalFeature.java");

        ImportReference staticImport = importReference(
                routeWalkFeature,
                "import static dev.traveler.core.route.internal.features.climb.internal.ClimbSupport.member;");
        ImportReference internalizedImport = importReference(
                routeWalkFeature,
                "import dev.traveler.core.route.internal.features.climb.internalized.ClimbSupport;");

        assertEquals(
                "dev.traveler.core.route.internal.features.climb.internal.ClimbSupport",
                staticImport.importedType());
        assertTrue(forbiddenFeatureInternalImport(staticImport));
        assertFalse(forbiddenFeatureInternalImport(internalizedImport));
    }

    @Test
    void crossDomainInternalParsingHandlesStaticImportsAndInternalPackageSegments() {
        ImportReference staticImport = importReference(
                Path.of("src/main/java/dev/traveler/core/navigation/control/ControlProjector.java"),
                "import static dev.traveler.core.route.internal.RouteInternals.member;");
        ImportReference internalizedImport = importReference(
                Path.of("src/main/java/dev/traveler/core/navigation/control/ControlProjector.java"),
                "import dev.traveler.core.route.internalized.RouteSupport;");

        assertEquals("route", internalImportDomain(staticImport));
        assertTrue(crossDomainInternalImport(staticImport));
        assertEquals("", internalImportDomain(internalizedImport));
        assertFalse(crossDomainInternalImport(internalizedImport));
    }

    @Test
    void concreteApiDeclarationDetectionRejectsClassesRecordsAndEnums(@TempDir Path temporaryDirectory)
            throws IOException {
        Path interfaceSource = javaSource(temporaryDirectory, "DefaultTraversalView.java", """
                package dev.traveler.core.navigation.api;

                public interface DefaultTraversalView {}
                """);
        Path classSource = javaSource(temporaryDirectory, "DefaultTraversalPlanner.java", """
                package dev.traveler.core.navigation.api;

                public final class DefaultTraversalPlanner {}
                """);
        Path recordSource = javaSource(temporaryDirectory, "StandardTraversalSettings.java", """
                package dev.traveler.core.navigation.api;

                public record StandardTraversalSettings() {}
                """);
        Path enumSource = javaSource(temporaryDirectory, "NoOpTraversalMode.java", """
                package dev.traveler.core.navigation.api;

                public enum NoOpTraversalMode { INSTANCE }
                """);

        assertFalse(concreteApiImplementationDeclaration(interfaceSource));
        assertTrue(concreteApiImplementationDeclaration(classSource));
        assertTrue(concreteApiImplementationDeclaration(recordSource));
        assertTrue(concreteApiImplementationDeclaration(enumSource));
    }

    private static List<String> crossDomainInternalImports() throws IOException {
        List<String> violations = new ArrayList<>();
        for (ImportReference reference : importReferences(SOURCE_ROOT)) {
            if (crossDomainInternalImport(reference)) {
                violations.add(formatImportReference(reference));
            }
        }
        return violations;
    }

    private static String domainFor(Path file) {
        String normalized = file.toString().replace('\\', '/');
        String marker = "dev/traveler/core/";
        int markerIndex = normalized.indexOf(marker);
        if (markerIndex < 0) {
            return "";
        }
        String rest = normalized.substring(markerIndex + marker.length());
        int separator = rest.indexOf('/');
        return separator < 0 ? "" : rest.substring(0, separator);
    }

    private static boolean crossDomainInternalImport(ImportReference reference) {
        String currentDomain = domainFor(reference.source());
        String importedDomain = internalImportDomain(reference);
        return !currentDomain.isBlank()
                && !importedDomain.isBlank()
                && !importedDomain.equals(currentDomain);
    }

    private static String internalImportDomain(ImportReference reference) {
        String importedType = reference.importedType();
        String prefix = "dev.traveler.core.";
        if (!importedType.startsWith(prefix) || !hasPackageSegment(importedType, "internal")) {
            return "";
        }
        String rest = importedType.substring(prefix.length());
        int separator = rest.indexOf('.');
        return separator < 0 ? "" : rest.substring(0, separator);
    }

    private static List<Path> productionSources(Path sourceRoot) throws IOException {
        try (var files = Files.walk(sourceRoot)) {
            return files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
        }
    }

    private static List<ImportReference> importReferences(Path sourceRoot) throws IOException {
        List<ImportReference> references = new ArrayList<>();
        for (Path file : productionSources(sourceRoot)) {
            for (String line : Files.readAllLines(file)) {
                ImportReference reference = importReference(file, line);
                if (reference != null) {
                    references.add(reference);
                }
            }
        }
        return references;
    }

    private static ImportReference importReference(Path source, String line) {
        String trimmed = line.trim();
        if (!trimmed.startsWith("import ")) {
            return null;
        }
        String importedType = trimmed.substring("import ".length()).trim();
        boolean staticImport = importedType.startsWith("static ");
        if (staticImport) {
            importedType = importedType.substring("static ".length()).trim();
        }
        int semicolon = importedType.indexOf(';');
        if (semicolon >= 0) {
            importedType = importedType.substring(0, semicolon).trim();
        }
        if (staticImport) {
            importedType = stripStaticMember(importedType);
        }
        return importedType.isBlank() ? null : new ImportReference(source, importedType);
    }

    private static String stripStaticMember(String importedType) {
        int memberSeparator = importedType.lastIndexOf('.');
        return memberSeparator < 0 ? importedType : importedType.substring(0, memberSeparator);
    }

    private static boolean containsPathSegment(Path path, String segment) {
        for (Path part : path) {
            if (part.toString().equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private static boolean concreteApiImplementationDeclaration(Path path) {
        try {
            int braceDepth = 0;
            for (String line : Files.readAllLines(path)) {
                String code = stripLineComment(line);
                if (braceDepth == 0 && CONCRETE_API_DECLARATION.matcher(code).find()) {
                    return true;
                }
                braceDepth += braceDelta(code);
            }
            return false;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static String stripLineComment(String line) {
        int commentStart = line.indexOf("//");
        return commentStart < 0 ? line : line.substring(0, commentStart);
    }

    private static int braceDelta(String line) {
        int delta = 0;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '{') {
                delta++;
            } else if (character == '}') {
                delta--;
            }
        }
        return delta;
    }

    private static boolean forbiddenFeatureInternalImport(ImportReference reference) {
        return importsOtherFeatureInternal(reference, CAPABILITY_FEATURE_ROOT)
                || importsOtherFeatureInternal(reference, ROUTE_TRAVERSAL_FEATURE_ROOT);
    }

    private static boolean importsOtherFeatureInternal(ImportReference reference, String featureRoot) {
        String sourceFeature = featureName(normalizedSource(reference.source()), featureRoot);
        String importedFeature = featureName(reference.importedType(), featureRoot);
        return !sourceFeature.isBlank()
                && !importedFeature.isBlank()
                && hasInternalPackageSegment(reference.importedType(), featureRoot, importedFeature)
                && !sourceFeature.equals(importedFeature);
    }

    private static boolean hasInternalPackageSegment(String importedType, String featureRoot, String featureName) {
        String featurePrefix = featureRoot + featureName + ".";
        int featureIndex = importedType.indexOf(featurePrefix);
        if (featureIndex < 0) {
            return false;
        }
        String suffix = importedType.substring(featureIndex + featurePrefix.length());
        for (String segment : suffix.split("\\.")) {
            if (segment.equals("internal")) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasPackageSegment(String importedType, String expectedSegment) {
        for (String segment : importedType.split("\\.")) {
            if (segment.equals(expectedSegment)) {
                return true;
            }
        }
        return false;
    }

    private static String featureName(String value, String featureRoot) {
        int rootIndex = value.indexOf(featureRoot);
        if (rootIndex < 0) {
            return "";
        }
        int start = rootIndex + featureRoot.length();
        int end = value.indexOf('.', start);
        return end < 0 ? "" : value.substring(start, end);
    }

    private static String normalizedSource(Path source) {
        return source.toString().replace('\\', '.').replace('/', '.');
    }

    private static String formatImportReference(ImportReference reference) {
        return reference.source() + " imports " + reference.importedType();
    }

    private static Path javaSource(Path directory, String fileName, String source) throws IOException {
        Path path = directory.resolve(fileName);
        Files.writeString(path, source);
        return path;
    }
}
