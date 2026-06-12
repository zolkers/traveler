package dev.traveler.mc.v1_21_11.fabric;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.architecture.JavaSourceRules;
import java.io.IOException;
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
        List<String> violations = JavaSourceRules.forbiddenImports(Path.of("src/main/java"), FORBIDDEN_IMPORTS);

        assertTrue(violations.isEmpty(), () -> "Core imports in fabric bootstrap: " + violations);
    }

    @Test
    void fabricRootPackageOnlyContainsEntrypoint() throws IOException {
        List<String> unexpectedSources = JavaSourceRules.directJavaSources(
                        Path.of("src/main/java/dev/traveler/mc/v1_21_11/fabric"))
                .stream()
                .filter(path -> !path.endsWith("TravelerFabricClientMod.java"))
                .toList();

        assertTrue(unexpectedSources.isEmpty(), () -> "Flat Fabric root sources: " + unexpectedSources);
    }
}
