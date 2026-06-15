package dev.traveler.core.world.navigation.features.climb;

import dev.traveler.core.route.start.ClimbSurfaceRouteStartProvider;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.ClimbSurfaceRouteStepProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.ClimbSurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceTraversalFeature;
import java.util.List;

public final class ClimbSurfaceTraversalFeature implements SurfaceTraversalFeature {
    @Override
    public List<SurfaceRouteStartProvider> routeStartProviders() {
        return List.of(new ClimbSurfaceRouteStartProvider());
    }

    @Override
    public List<SurfaceConnectionProvider> connectionProviders() {
        return List.of(SurfaceConnectionProviders.climb());
    }

    @Override
    public List<SurfaceTransitionProvider> transitionProviders() {
        return List.of(new ClimbSurfaceTransitionProvider());
    }

    @Override
    public List<SurfaceRouteStepProvider> routeStepProviders() {
        return List.of(new ClimbSurfaceRouteStepProvider());
    }
}
