package dev.traveler.core.world.navigation;

import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.start.SurfaceRouteStartResolver;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.route.step.SurfaceRouteStepResolver;
import dev.traveler.core.world.navigation.features.climb.ClimbSurfaceTraversalFeature;
import dev.traveler.core.world.navigation.features.drop.DropSurfaceTraversalFeature;
import dev.traveler.core.world.navigation.features.jump.JumpSurfaceTraversalFeature;
import dev.traveler.core.world.navigation.features.walk.WalkSurfaceTraversalFeature;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class SurfaceTraversalFeatures {
    private SurfaceTraversalFeatures() {}

    public static List<SurfaceTraversalFeature> standard() {
        return List.of(
                new ClimbSurfaceTraversalFeature(),
                new WalkSurfaceTraversalFeature(),
                new DropSurfaceTraversalFeature(),
                new JumpSurfaceTraversalFeature());
    }

    public static SurfaceRouteStartResolver startResolver(List<SurfaceTraversalFeature> features) {
        return new SurfaceRouteStartResolver(routeStartProviders(features));
    }

    public static SurfaceRouteStepResolver routeStepResolver(List<SurfaceTraversalFeature> features) {
        return new SurfaceRouteStepResolver(routeStepProviders(features));
    }

    public static SurfaceTransitionResolver transitionResolver(List<SurfaceTraversalFeature> features) {
        return new SurfaceTransitionResolver(transitionProviders(features));
    }

    public static List<SurfaceRouteStartProvider> routeStartProviders(List<SurfaceTraversalFeature> features) {
        List<SurfaceRouteStartProvider> providers = new ArrayList<>();
        for (SurfaceTraversalFeature feature : safeFeatures(features)) {
            providers.addAll(feature.routeStartProviders());
        }
        return List.copyOf(providers);
    }

    public static List<SurfaceConnectionProvider> connectionProviders(List<SurfaceTraversalFeature> features) {
        List<SurfaceConnectionProvider> providers = new ArrayList<>();
        for (SurfaceTraversalFeature feature : safeFeatures(features)) {
            providers.addAll(feature.connectionProviders());
        }
        return List.copyOf(providers);
    }

    public static List<SurfaceTransitionProvider> transitionProviders(List<SurfaceTraversalFeature> features) {
        List<SurfaceTransitionProvider> providers = new ArrayList<>();
        for (SurfaceTraversalFeature feature : safeFeatures(features)) {
            providers.addAll(feature.transitionProviders());
        }
        return List.copyOf(providers);
    }

    public static List<SurfaceRouteStepProvider> routeStepProviders(List<SurfaceTraversalFeature> features) {
        List<SurfaceRouteStepProvider> providers = new ArrayList<>();
        for (SurfaceTraversalFeature feature : safeFeatures(features)) {
            providers.addAll(feature.routeStepProviders());
        }
        return List.copyOf(providers);
    }

    private static List<SurfaceTraversalFeature> safeFeatures(List<SurfaceTraversalFeature> features) {
        List<SurfaceTraversalFeature> safeFeatures = List.copyOf(Objects.requireNonNull(features, "features"));
        if (safeFeatures.isEmpty()) {
            throw new IllegalArgumentException("features must not be empty");
        }
        return safeFeatures;
    }
}
