package dev.traveler.core.pathfinder.kernel.impl;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.StandardTraversalModules;
import dev.traveler.core.pathfinder.kernel.api.PathfinderKernel;
import java.util.List;

public final class PathfinderKernels {
    private PathfinderKernels() {}

    public static PathfinderKernel routeOnly() {
        return routeOnly(StandardTraversalModules.modules());
    }

    public static PathfinderKernel routeOnly(List<? extends TraversalModule> traversalModules) {
        return new RouteOnlyPathfinderKernel(traversalModules);
    }
}
