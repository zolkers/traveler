package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.route.start.ClimbSurfaceRouteStartProvider;
import dev.traveler.core.route.start.StandingSurfaceRouteStartProvider;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.step.ClimbSurfaceRouteStepProvider;
import dev.traveler.core.route.step.DefaultSurfaceRouteStepProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class SurfaceTraversalFeaturesTest {
    @Test
    void standardFeaturesExposeRouteStartsAndConnections() {
        List<SurfaceTraversalFeature> features = SurfaceTraversalFeatures.standard();

        List<SurfaceRouteStartProvider> starts = SurfaceTraversalFeatures.routeStartProviders(features);
        List<SurfaceConnectionProvider> connections = SurfaceTraversalFeatures.connectionProviders(features);
        List<SurfaceTransitionProvider> transitions = SurfaceTraversalFeatures.transitionProviders(features);
        List<SurfaceRouteStepProvider> routeSteps = SurfaceTraversalFeatures.routeStepProviders(features);

        assertTrue(starts.stream().anyMatch(StandingSurfaceRouteStartProvider.class::isInstance));
        assertTrue(starts.stream().anyMatch(ClimbSurfaceRouteStartProvider.class::isInstance));
        assertTrue(connections.stream().anyMatch(AdjacentSurfaceConnectionProvider.class::isInstance));
        assertTrue(connections.stream().anyMatch(DropSurfaceConnectionProvider.class::isInstance));
        assertTrue(connections.stream().anyMatch(JumpSurfaceConnectionProvider.class::isInstance));
        assertTrue(connections.stream().anyMatch(ClimbSurfaceConnectionProvider.class::isInstance));
        assertTrue(transitions.stream().anyMatch(DefaultSurfaceTransitionProvider.class::isInstance));
        assertTrue(transitions.stream().anyMatch(ClimbSurfaceTransitionProvider.class::isInstance));
        assertTrue(routeSteps.stream().anyMatch(DefaultSurfaceRouteStepProvider.class::isInstance));
        assertTrue(routeSteps.stream().anyMatch(ClimbSurfaceRouteStepProvider.class::isInstance));
    }

    @Test
    void standardFeatureClassesLiveInFeaturePackages() {
        for (SurfaceTraversalFeature feature : SurfaceTraversalFeatures.standard()) {
            assertTrue(
                    feature.getClass().getPackageName().contains(".features."),
                    () -> feature.getClass().getName());
        }
    }

    @Test
    void rejectsEmptyFeatureLists() {
        assertThrows(
                IllegalArgumentException.class,
                () -> SurfaceTraversalFeatures.routeStartProviders(List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> SurfaceTraversalFeatures.connectionProviders(List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> SurfaceTraversalFeatures.transitionProviders(List.of()));
        assertThrows(
                IllegalArgumentException.class,
                () -> SurfaceTraversalFeatures.routeStepProviders(List.of()));
    }

    @Test
    void supportsFeaturesThatOnlyExposeStartsOrConnections() {
        SurfaceTraversalFeature startOnly = new SurfaceTraversalFeature() {
            @Override
            public List<SurfaceRouteStartProvider> routeStartProviders() {
                return List.of(context -> List.of());
            }
        };
        SurfaceTraversalFeature connectionOnly = new SurfaceTraversalFeature() {
            @Override
            public List<SurfaceConnectionProvider> connectionProviders() {
                return List.of((context, node, connections) -> {});
            }
        };
        SurfaceTraversalFeature transitionOnly = new SurfaceTraversalFeature() {
            @Override
            public List<SurfaceTransitionProvider> transitionProviders() {
                return List.of(context -> java.util.Optional.empty());
            }
        };
        SurfaceTraversalFeature routeStepOnly = new SurfaceTraversalFeature() {
            @Override
            public List<SurfaceRouteStepProvider> routeStepProviders() {
                return List.of(context -> java.util.Optional.empty());
            }
        };

        assertFalse(SurfaceTraversalFeatures.routeStartProviders(List.of(
                        startOnly,
                        connectionOnly,
                        transitionOnly,
                        routeStepOnly))
                .isEmpty());
        assertFalse(SurfaceTraversalFeatures.connectionProviders(List.of(
                        startOnly,
                        connectionOnly,
                        transitionOnly,
                        routeStepOnly))
                .isEmpty());
        assertFalse(SurfaceTraversalFeatures.transitionProviders(List.of(
                        startOnly,
                        connectionOnly,
                        transitionOnly,
                        routeStepOnly))
                .isEmpty());
        assertFalse(SurfaceTraversalFeatures.routeStepProviders(List.of(
                        startOnly,
                        connectionOnly,
                        transitionOnly,
                        routeStepOnly))
                .isEmpty());
    }
}
