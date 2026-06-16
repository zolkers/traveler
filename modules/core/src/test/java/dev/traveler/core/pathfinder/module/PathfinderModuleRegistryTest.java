package dev.traveler.core.pathfinder.module;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.pathfinder.module.api.PathfinderModuleSelection;
import dev.traveler.core.pathfinder.module.impl.DefaultPathfinderModuleRegistry;
import dev.traveler.core.pathfinder.module.noop.EmptyPathfinderModuleRegistry;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PathfinderModuleRegistryTest {
    @Test
    void registryReturnsEnabledModulesByPriority() {
        TestModule low = new TestModule("low", 10, true);
        TestModule high = new TestModule("high", 1, true);
        TestModule smoothing = new TestModule("smoothing", PathfinderModuleType.SMOOTHING, 0, true);

        DefaultPathfinderModuleRegistry registry = new DefaultPathfinderModuleRegistry(List.of(low, high, smoothing));

        assertEquals(List.of(high, low), registry.enabled(PathfinderModuleType.TRAVERSAL));
    }

    @Test
    void registryExcludesDisabledModules() {
        TestModule enabled = new TestModule("enabled", 10, true);
        TestModule disabled = new TestModule("disabled", 1, false);

        DefaultPathfinderModuleRegistry registry = new DefaultPathfinderModuleRegistry(List.of(disabled, enabled));

        assertEquals(List.of(enabled), registry.enabled(PathfinderModuleType.TRAVERSAL));
    }

    @Test
    void emptyRegistryReturnsDisabledFallback() {
        EmptyPathfinderModuleRegistry registry = new EmptyPathfinderModuleRegistry();

        PathfinderModule fallback = registry.fallback(PathfinderModuleType.TRAVERSAL);

        assertTrue(registry.enabled(PathfinderModuleType.TRAVERSAL).isEmpty());
        assertEquals("fallback.traversal", fallback.descriptor().id());
        assertEquals(PathfinderModuleType.TRAVERSAL, fallback.descriptor().type());
        assertFalse(fallback.descriptor().enabled());
    }

    @Test
    void defaultRegistryReturnsDisabledFallback() {
        DefaultPathfinderModuleRegistry registry = new DefaultPathfinderModuleRegistry(List.of());

        PathfinderModule fallback = registry.fallback(PathfinderModuleType.EXECUTION);

        assertEquals("fallback.execution", fallback.descriptor().id());
        assertEquals(PathfinderModuleType.EXECUTION, fallback.descriptor().type());
        assertFalse(fallback.descriptor().enabled());
        assertEquals(Integer.MAX_VALUE, fallback.descriptor().priority());
    }

    @Test
    void registryDefensivelyCopiesModuleList() {
        TestModule first = new TestModule("first", 10, true);
        TestModule addedLater = new TestModule("added-later", 1, true);
        List<PathfinderModule> modules = new ArrayList<>(List.of(first));

        DefaultPathfinderModuleRegistry registry = new DefaultPathfinderModuleRegistry(modules);
        modules.add(addedLater);

        assertEquals(List.of(first), registry.enabled(PathfinderModuleType.TRAVERSAL));
    }

    @Test
    void registryFailsFastForNullInputs() {
        DefaultPathfinderModuleRegistry registry = new DefaultPathfinderModuleRegistry(List.of());

        assertThrows(NullPointerException.class, () -> new DefaultPathfinderModuleRegistry(null));
        assertThrows(NullPointerException.class, () -> new DefaultPathfinderModuleRegistry(listWithNullModule()));
        assertThrows(NullPointerException.class, () -> registry.enabled(null));
        assertThrows(NullPointerException.class, () -> registry.fallback(null));
    }

    @Test
    void emptyRegistryFailsFastForNullType() {
        EmptyPathfinderModuleRegistry registry = new EmptyPathfinderModuleRegistry();

        assertThrows(NullPointerException.class, () -> registry.enabled(null));
        assertThrows(NullPointerException.class, () -> registry.fallback(null));
    }

    @Test
    void moduleSelectionDefensivelyCopiesLists() {
        TestModule enabled = new TestModule("enabled", 1, true);
        TestModule disabled = new TestModule("disabled", 2, false);
        List<PathfinderModule> enabledModules = new ArrayList<>(List.of(enabled));
        List<PathfinderModule> disabledModules = new ArrayList<>(List.of(disabled));

        PathfinderModuleSelection selection = new PathfinderModuleSelection(enabledModules, disabledModules);
        enabledModules.clear();
        disabledModules.clear();

        assertEquals(List.of(enabled), selection.enabled());
        assertEquals(List.of(disabled), selection.disabled());
    }

    @Test
    void moduleSelectionFailsFastForNullInputs() {
        assertThrows(NullPointerException.class, () -> new PathfinderModuleSelection(null, List.of()));
        assertThrows(NullPointerException.class, () -> new PathfinderModuleSelection(List.of(), null));
        assertThrows(NullPointerException.class, () -> new PathfinderModuleSelection(listWithNullModule(), List.of()));
        assertThrows(NullPointerException.class, () -> new PathfinderModuleSelection(List.of(), listWithNullModule()));
    }

    private static List<PathfinderModule> listWithNullModule() {
        List<PathfinderModule> modules = new ArrayList<>();
        modules.add(null);
        return modules;
    }

    private record TestModule(String id, PathfinderModuleType type, int priority, boolean enabled)
            implements PathfinderModule {
        private TestModule(String id, int priority, boolean enabled) {
            this(id, PathfinderModuleType.TRAVERSAL, priority, enabled);
        }

        @Override
        public PathfinderModuleDescriptor descriptor() {
            return new PathfinderModuleDescriptor(id, type, priority, enabled);
        }
    }
}
