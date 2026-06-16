package dev.traveler.core.architecture;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class PublicApiBoundaryTest {
    private static final Path SOURCE_ROOT = Path.of("src/main/java");
    private static final Path COMMON_API_PACKAGE =
            SOURCE_ROOT.resolve("dev/traveler/core/common/api");

    @Test
    void commonApiPackageExists() {
        assertTrue(Files.isDirectory(COMMON_API_PACKAGE), () -> "Missing API package: " + COMMON_API_PACKAGE);
    }

    @Test
    void coreDoesNotImportMinecraftAdapters() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(SOURCE_ROOT, List.of("import dev.traveler.mc."));

        assertTrue(violations.isEmpty(), () -> "Forbidden core imports: " + violations);
    }
}
