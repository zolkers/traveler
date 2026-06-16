package dev.traveler.core.pathfinder.kernel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelResult;
import dev.traveler.core.pathfinder.kernel.noop.EmptyPathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.route.RouteSearchDiagnostics;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PathfinderModuleContractTest {
    @Test
    void descriptorRejectsBlankId() {
        assertThrows(IllegalArgumentException.class, () ->
                new PathfinderModuleDescriptor(" ", PathfinderModuleType.TRAVERSAL, 0, true));
    }

    @Test
    void emptyModuleIsDisabledAndStable() {
        EmptyPathfinderModule module = new EmptyPathfinderModule(PathfinderModuleType.EXECUTION, "execution.none");

        assertEquals("execution.none", module.descriptor().id());
        assertEquals(PathfinderModuleType.EXECUTION, module.descriptor().type());
        assertFalse(module.descriptor().enabled());
    }

    @Test
    void kernelResultRejectsNullValues() {
        RouteSearchDiagnostics diagnostics = RouteSearchDiagnostics.none(0, 0);
        List<PathfinderModuleDescriptor> modules = List.of();

        assertThrows(NullPointerException.class, () -> new PathfinderKernelResult(null, diagnostics, modules));
        assertThrows(NullPointerException.class, () -> new PathfinderKernelResult(Optional.empty(), null, modules));
        assertThrows(NullPointerException.class, () -> new PathfinderKernelResult(Optional.empty(), diagnostics, null));
    }

    @Test
    void kernelResultDefensivelyCopiesActiveModules() {
        PathfinderModuleDescriptor traversalModule =
                new PathfinderModuleDescriptor("traversal.default", PathfinderModuleType.TRAVERSAL, 0, true);
        List<PathfinderModuleDescriptor> modules = new ArrayList<>();
        modules.add(traversalModule);

        PathfinderKernelResult result =
                new PathfinderKernelResult(Optional.empty(), RouteSearchDiagnostics.none(0, 0), modules);
        modules.add(new PathfinderModuleDescriptor("debug.default", PathfinderModuleType.DEBUG, 1, true));

        assertEquals(List.of(traversalModule), result.activeModules());
    }

    @Test
    void kernelResultExposesUnmodifiableActiveModules() {
        PathfinderKernelResult result = new PathfinderKernelResult(
                Optional.empty(),
                RouteSearchDiagnostics.none(0, 0),
                List.of(new PathfinderModuleDescriptor("debug.default", PathfinderModuleType.DEBUG, 0, true)));

        assertThrows(UnsupportedOperationException.class, () -> result.activeModules()
                .add(new PathfinderModuleDescriptor("debug.extra", PathfinderModuleType.DEBUG, 1, true)));
    }
}
