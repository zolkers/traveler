package dev.traveler.mc.v1_21_11.common;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.architecture.JavaSourceRules;
import java.io.IOException;
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
        List<String> violations = JavaSourceRules.forbiddenImports(Path.of("src/main/java"), FORBIDDEN_IMPORTS);

        assertTrue(violations.isEmpty(), () -> "Loader imports in common: " + violations);
    }

    @Test
    void commonDoesNotDefineCoreFrameworkPackages() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenPackages(Path.of("src/main/java"), FORBIDDEN_PACKAGES);

        assertTrue(violations.isEmpty(), () -> "Reusable framework packages in common: " + violations);
    }

    @Test
    void minecraftAdaptersLiveInFocusedSubpackages() throws IOException {
        List<String> directAdapterSources = JavaSourceRules.directJavaSources(
                Path.of("src/main/java/dev/traveler/mc/v1_21_11/common/adapter"));

        assertTrue(directAdapterSources.isEmpty(), () -> "Flat Minecraft adapter sources: " + directAdapterSources);
    }
}
