package dev.traveler.core.pathfinder.kernel.impl;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelMode;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernelResult;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.route.RouteGoal;
import dev.traveler.core.route.RouteSearchComponents;
import dev.traveler.core.route.RouteSearchResult;
import dev.traveler.core.route.RouteSearchService;
import dev.traveler.core.route.RouteSearchSettings;
import dev.traveler.core.route.api.RoutePlan;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.movement.MovementProfile;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class RouteOnlyPathfinderKernel implements PathfinderKernel {
    private final RouteSearchComponents routeComponents;
    private final List<PathfinderModuleDescriptor> activeModules;

    RouteOnlyPathfinderKernel(List<? extends TraversalModule> traversalModules) {
        List<TraversalModule> enabledTraversalModules = enabledTraversalModules(traversalModules);
        routeComponents = RouteSearchComponents.withTraversalModules(enabledTraversalModules);
        activeModules = enabledTraversalModules.stream()
                .map(TraversalModule::descriptor)
                .toList();
    }

    @Override
    public PathfinderKernelResult findRoute(
            SurfaceWorldLayer worldLayer,
            BlockPosition start,
            RouteGoal goal,
            MovementProfile movementProfile) {
        RouteSearchSettings settings = RouteSearchSettings.standardClient()
                .withMovementProfile(Objects.requireNonNull(movementProfile, "movementProfile"));
        RouteSearchService routeSearchService = new RouteSearchService(settings, routeComponents);
        RouteSearchResult routeResult = routeSearchService.search(worldLayer, start, goal);
        Optional<RoutePlan> routePlan = routeResult.route()
                .map(route -> routeSearchService.plan(routeResult));
        return new PathfinderKernelResult(routePlan, routeResult.diagnostics(), activeModules);
    }

    @Override
    public PathfinderKernelMode mode() {
        return PathfinderKernelMode.ROUTE_ONLY;
    }

    private static List<TraversalModule> enabledTraversalModules(List<? extends TraversalModule> modules) {
        List<TraversalModule> enabledModules = new ArrayList<>();
        for (TraversalModule module : List.copyOf(Objects.requireNonNull(modules, "traversalModules"))) {
            if (enabled(module)) {
                enabledModules.add(module);
            }
        }
        enabledModules.sort(Comparator.comparingInt(module -> module.descriptor().priority()));
        return List.copyOf(enabledModules);
    }

    private static boolean enabled(TraversalModule module) {
        return Objects.requireNonNull(module.descriptor(), "descriptor").enabled();
    }
}
