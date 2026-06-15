package dev.traveler.core.world.navigation.features.walk;

import dev.traveler.core.route.start.StandingSurfaceRouteStartProvider;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.DefaultSurfaceRouteStepProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.DefaultSurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceTraversalFeature;
import java.util.List;

public final class WalkSurfaceTraversalFeature implements SurfaceTraversalFeature {
    @Override
    public List<SurfaceRouteStartProvider> routeStartProviders() {
        return List.of(new StandingSurfaceRouteStartProvider());
    }

    @Override
    public List<SurfaceConnectionProvider> connectionProviders() {
        return List.of(SurfaceConnectionProviders.adjacent());
    }

    @Override
    public List<SurfaceTransitionProvider> transitionProviders() {
        return List.of(new DefaultSurfaceTransitionProvider());
    }

    @Override
    public List<SurfaceRouteStepProvider> routeStepProviders() {
        return List.of(new DefaultSurfaceRouteStepProvider());
    }
}
