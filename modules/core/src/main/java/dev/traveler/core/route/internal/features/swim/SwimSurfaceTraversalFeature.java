package dev.traveler.core.route.internal.features.swim;

import dev.traveler.core.route.internal.SurfaceTraversalFeature;
import dev.traveler.core.route.start.SubmergedSwimRouteStartProvider;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.DefaultSurfaceRouteStepProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.world.navigation.SurfaceConnectionProviders;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SwimSurfaceTransitionProvider;
import java.util.List;

public final class SwimSurfaceTraversalFeature implements SurfaceTraversalFeature {
    @Override
    public List<SurfaceRouteStartProvider> routeStartProviders() {
        return List.of(new SubmergedSwimRouteStartProvider());
    }

    @Override
    public List<SurfaceConnectionProvider> connectionProviders() {
        return List.of(SurfaceConnectionProviders.swim());
    }

    @Override
    public List<SurfaceTransitionProvider> transitionProviders() {
        return List.of(new SwimSurfaceTransitionProvider());
    }

    @Override
    public List<SurfaceRouteStepProvider> routeStepProviders() {
        return List.of(new DefaultSurfaceRouteStepProvider());
    }
}
