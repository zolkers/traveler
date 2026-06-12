package dev.traveler.core;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.architecture.JavaSourceRules;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class ArchitectureBoundaryTest {
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "net.minecraft",
            "net.fabricmc",
            "net.neoforged",
            "com.mojang.brigadier",
            "org.spongepowered.asm.mixin",
            "dev.riege.buildmycommand");

    @Test
    void coreDoesNotImportMinecraftLoadersMixinBrigadierOrBuildMyCommand() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(Path.of("src/main/java"), FORBIDDEN_IMPORTS);

        assertTrue(violations.isEmpty(), () -> "Forbidden core imports: " + violations);
    }

    @Test
    void worldDomainTypesLiveInFocusedSubpackages() throws IOException {
        List<String> directWorldSources =
                JavaSourceRules.directJavaSources(Path.of("src/main/java/dev/traveler/core/world"));

        assertTrue(directWorldSources.isEmpty(), () -> "Flat core world package sources: " + directWorldSources);
    }
}
