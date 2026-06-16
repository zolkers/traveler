package dev.traveler.core.capability.traversal.noop;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalDebugContributor;
import dev.traveler.core.capability.traversal.spi.TraversalExecutionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import java.util.List;

public final class NoTraversalModule implements TraversalModule {
    private final PathfinderModuleDescriptor descriptor;

    public NoTraversalModule(String id) {
        descriptor = new PathfinderModuleDescriptor(id, PathfinderModuleType.TRAVERSAL, Integer.MAX_VALUE, false);
    }

    @Override
    public PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public List<TraversalConnectionContributor> connectionContributors() {
        return List.of();
    }

    @Override
    public List<TraversalRouteContributor> routeContributors() {
        return List.of();
    }

    @Override
    public List<TraversalExecutionContributor> executionContributors() {
        return List.of();
    }

    @Override
    public List<TraversalRecoveryContributor> recoveryContributors() {
        return List.of();
    }

    @Override
    public List<TraversalDebugContributor> debugContributors() {
        return List.of();
    }
}
