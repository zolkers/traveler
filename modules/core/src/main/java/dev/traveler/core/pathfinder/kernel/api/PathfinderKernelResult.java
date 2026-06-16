package dev.traveler.core.pathfinder.kernel.api;

import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.route.RouteSearchDiagnostics;
import dev.traveler.core.route.api.RoutePlan;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record PathfinderKernelResult(
        Optional<RoutePlan> routePlan,
        RouteSearchDiagnostics diagnostics,
        List<PathfinderModuleDescriptor> activeModules) {
    public PathfinderKernelResult {
        Objects.requireNonNull(routePlan, "routePlan");
        Objects.requireNonNull(diagnostics, "diagnostics");
        activeModules = List.copyOf(Objects.requireNonNull(activeModules, "activeModules"));
    }
}
