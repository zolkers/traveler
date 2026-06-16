package dev.traveler.core.capability.traversal.spi;

import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import java.util.List;

public interface TraversalRouteContributor {
    List<SurfaceRouteStartProvider> routeStartProviders();

    List<SurfaceTransitionProvider> transitionProviders();

    List<SurfaceRouteStepProvider> routeStepProviders();
}
