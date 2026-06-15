package dev.traveler.command.buildmycommand;

import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.architecture.JavaSourceRules;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class BuildMyCommandArchitectureBoundaryTest {
    private static final List<String> FORBIDDEN_IMPORTS = List.of(
            "net.minecraft",
            "net.fabricmc",
            "net.neoforged",
            "com.mojang.brigadier",
            "org.spongepowered.asm.mixin");

    @Test
    void buildMyCommandAdapterDoesNotImportMinecraftLoaderOrBrigadierTypes() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(Path.of("src/main/java"), FORBIDDEN_IMPORTS);

        assertTrue(violations.isEmpty(), () -> "Forbidden command adapter imports: " + violations);
    }
}
