package dev.traveler.core.capability.traversal.impl;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalDebugContributor;
import dev.traveler.core.capability.traversal.spi.TraversalExecutionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRecoveryContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleDescriptor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import java.util.List;
import java.util.Objects;

abstract class AbstractTraversalModule implements TraversalModule {
    private final PathfinderModuleDescriptor descriptor;
    private final List<TraversalConnectionContributor> connectionContributors;
    private final List<TraversalRouteContributor> routeContributors;
    private final List<TraversalRecoveryContributor> recoveryContributors;

    AbstractTraversalModule(
            String id,
            int priority,
            List<SurfaceConnectionProvider> connectionProviders,
            List<SurfaceRouteStartProvider> routeStartProviders,
            List<SurfaceTransitionProvider> transitionProviders,
            List<SurfaceRouteStepProvider> routeStepProviders,
            List<TraversalRecoveryContributor> recoveryContributors) {
        descriptor = new PathfinderModuleDescriptor(
                Objects.requireNonNull(id, "id"),
                PathfinderModuleType.TRAVERSAL,
                priority,
                true);
        connectionContributors = connectionContributors(connectionProviders);
        routeContributors = routeContributors(routeStartProviders, transitionProviders, routeStepProviders);
        this.recoveryContributors =
                List.copyOf(Objects.requireNonNull(recoveryContributors, "recoveryContributors"));
    }

    @Override
    public final PathfinderModuleDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public final List<TraversalConnectionContributor> connectionContributors() {
        return connectionContributors;
    }

    @Override
    public final List<TraversalRouteContributor> routeContributors() {
        return routeContributors;
    }

    @Override
    public final List<TraversalExecutionContributor> executionContributors() {
        return List.of();
    }

    @Override
    public final List<TraversalRecoveryContributor> recoveryContributors() {
        return recoveryContributors;
    }

    @Override
    public final List<TraversalDebugContributor> debugContributors() {
        return List.of();
    }

    private static List<TraversalConnectionContributor> connectionContributors(
            List<SurfaceConnectionProvider> providers) {
        List<SurfaceConnectionProvider> safeProviders =
                List.copyOf(Objects.requireNonNull(providers, "connectionProviders"));
        if (safeProviders.isEmpty()) {
            return List.of();
        }
        return List.of(new SurfaceConnectionContribution(safeProviders));
    }

    private static List<TraversalRouteContributor> routeContributors(
            List<SurfaceRouteStartProvider> routeStartProviders,
            List<SurfaceTransitionProvider> transitionProviders,
            List<SurfaceRouteStepProvider> routeStepProviders) {
        SurfaceRouteContribution contribution = new SurfaceRouteContribution(
                routeStartProviders,
                transitionProviders,
                routeStepProviders);
        if (contribution.routeStartProviders().isEmpty()
                && contribution.transitionProviders().isEmpty()
                && contribution.routeStepProviders().isEmpty()) {
            return List.of();
        }
        return List.of(contribution);
    }

    private record SurfaceConnectionContribution(List<SurfaceConnectionProvider> surfaceConnectionProviders)
            implements TraversalConnectionContributor {
        private SurfaceConnectionContribution {
            surfaceConnectionProviders =
                    List.copyOf(Objects.requireNonNull(surfaceConnectionProviders, "surfaceConnectionProviders"));
        }
    }

    private record SurfaceRouteContribution(
            List<SurfaceRouteStartProvider> routeStartProviders,
            List<SurfaceTransitionProvider> transitionProviders,
            List<SurfaceRouteStepProvider> routeStepProviders)
            implements TraversalRouteContributor {
        private SurfaceRouteContribution {
            routeStartProviders =
                    List.copyOf(Objects.requireNonNull(routeStartProviders, "routeStartProviders"));
            transitionProviders =
                    List.copyOf(Objects.requireNonNull(transitionProviders, "transitionProviders"));
            routeStepProviders =
                    List.copyOf(Objects.requireNonNull(routeStepProviders, "routeStepProviders"));
        }
    }
}
