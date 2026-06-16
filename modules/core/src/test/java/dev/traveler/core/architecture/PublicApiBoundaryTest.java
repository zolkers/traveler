package dev.traveler.core.architecture;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.common.api.DiagnosticPayload;
import dev.traveler.core.common.api.SettingsSection;
import dev.traveler.core.common.api.TravelerPort;
import dev.traveler.core.common.api.TravelerRegistry;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;
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
    void commonApiStaysLean() {
        assertTrue(TravelerPort.class.isInterface());
        assertEquals(0, TravelerPort.class.getDeclaredMethods().length);

        assertTrue(TravelerRegistry.class.isInterface());
        assertEquals(2, TravelerRegistry.class.getTypeParameters().length);
        assertArrayEquals(new String[] {"K", "V"}, typeParameterNames(TravelerRegistry.class));

        Method resolve = TravelerRegistry.class.getDeclaredMethods()[0];
        assertEquals("resolve", resolve.getName());
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

    private static String[] typeParameterNames(Class<?> type) {
        return java.util.Arrays.stream(type.getTypeParameters()).map(TypeVariable::getName).toArray(String[]::new);
    }
}
