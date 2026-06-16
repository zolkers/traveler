package dev.traveler.core.capability.traversal.api;

import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalDebugContributor;
import dev.traveler.core.capability.traversal.spi.TraversalExecutionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import java.util.List;

public interface TraversalModule extends PathfinderModule {
    List<TraversalConnectionContributor> connectionContributors();

    List<TraversalRouteContributor> routeContributors();

    List<TraversalExecutionContributor> executionContributors();

    List<TraversalRecoveryContributor> recoveryContributors();

    List<TraversalDebugContributor> debugContributors();
}
