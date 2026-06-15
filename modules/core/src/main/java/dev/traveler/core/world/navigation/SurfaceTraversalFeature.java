package dev.traveler.core.world.navigation;

import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import java.util.List;

public interface SurfaceTraversalFeature {
    default List<SurfaceRouteStartProvider> routeStartProviders() {
        return List.of();
    }

    default List<SurfaceConnectionProvider> connectionProviders() {
        return List.of();
    }

    default List<SurfaceTransitionProvider> transitionProviders() {
        return List.of();
    }

    default List<SurfaceRouteStepProvider> routeStepProviders() {
        return List.of();
    }
}
