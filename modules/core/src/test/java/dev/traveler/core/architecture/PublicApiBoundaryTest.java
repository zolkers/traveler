package dev.traveler.core.architecture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.common.api.DiagnosticPayload;
import dev.traveler.core.common.api.SettingsSection;
import dev.traveler.core.common.api.TravelerPort;
import dev.traveler.core.common.api.TravelerRegistry;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
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
    void commonApiStaysLean() {
        assertTrue(TravelerPort.class.isInterface());
        assertEquals(0, TravelerPort.class.getDeclaredMethods().length);

        assertTrue(TravelerRegistry.class.isInterface());
        assertEquals(2, TravelerRegistry.class.getTypeParameters().length);
        assertEquals(1, TravelerRegistry.class.getDeclaredMethods().length);

        Method resolve = Arrays.stream(TravelerRegistry.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("resolve"))
                .findFirst()
                .orElse(null);
        assertNotNull(resolve);
        assertEquals(1, resolve.getParameterCount());

        assertTrue(SettingsSection.class.isInterface());
        assertFalse(SettingsSection.class.isRecord());
        assertEquals(0, SettingsSection.class.getDeclaredMethods().length);

        assertTrue(DiagnosticPayload.class.isInterface());
        assertFalse(DiagnosticPayload.class.isRecord());
        assertEquals(0, DiagnosticPayload.class.getDeclaredMethods().length);
    }

    @Test
    void coreDoesNotReferenceMinecraftAdapters() throws IOException {
        List<String> violations = JavaSourceRules.forbiddenImports(SOURCE_ROOT, List.of("dev.traveler.mc."));

        assertTrue(violations.isEmpty(), () -> "Forbidden core references: " + violations);
    }
}
