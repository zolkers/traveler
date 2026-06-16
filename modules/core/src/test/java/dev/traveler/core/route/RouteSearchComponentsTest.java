package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.WalkTraversalModule;
import dev.traveler.core.capability.traversal.noop.NoTraversalModule;
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
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RouteSearchComponentsTest {
    @Test
    void noTraversalModuleBuildsValidEmptyProviderComponents() {
        RouteSearchComponents components =
                RouteSearchComponents.withTraversalModules(List.of(new NoTraversalModule("traversal.none")));

        assertTrue(surfaceConnectionProviders(components).isEmpty());
        assertTrue(components.surfaceStartResolver().providers().isEmpty());
        assertTrue(components.surfaceTransitionResolver().providers().isEmpty());
        assertTrue(components.surfaceRouteStepResolver().providers().isEmpty());
    }

    @Test
    void standardUsesTraversalModulesInStableProviderOrder() {
        RouteSearchComponents components = RouteSearchComponents.standard();

        assertEquals(
                List.of(
                        "ClimbSurfaceConnectionProvider",
                        "SwimSurfaceConnectionProvider",
                        "AdjacentSurfaceConnectionProvider",
                        "DropSurfaceConnectionProvider",
                        "JumpSurfaceConnectionProvider"),
                providerClassNames(surfaceConnectionProviders(components)));
        assertEquals(
                List.of(
                        "ClimbSurfaceRouteStartProvider",
                        "SubmergedSwimRouteStartProvider",
                        "StandingSurfaceRouteStartProvider"),
                providerClassNames(components.surfaceStartResolver().providers()));
        assertEquals(
                List.of(
                        "ClimbSurfaceTransitionProvider",
                        "SwimSurfaceTransitionProvider",
                        "DefaultSurfaceTransitionProvider"),
                providerClassNames(components.surfaceTransitionResolver().providers()));
        assertEquals(
                List.of(
                        "ClimbSurfaceRouteStepProvider",
                        "DefaultSurfaceRouteStepProvider",
                        "DefaultSurfaceRouteStepProvider"),
                providerClassNames(components.surfaceRouteStepResolver().providers()));
    }

    @Test
    void standardRouteCompositionKeepsClimbHandlersBeforeDefaultHandlers() {
        RouteSearchComponents components = RouteSearchComponents.standard();

        assertProviderPrecedes(
                components.surfaceTransitionResolver().providers(),
                "ClimbSurfaceTransitionProvider",
                "DefaultSurfaceTransitionProvider");
        assertProviderPrecedes(
                components.surfaceTransitionResolver().providers(),
                "SwimSurfaceTransitionProvider",
                "DefaultSurfaceTransitionProvider");
        assertProviderPrecedes(
                components.surfaceRouteStepResolver().providers(),
                "ClimbSurfaceRouteStepProvider",
                "DefaultSurfaceRouteStepProvider");
    }

    @Test
    void disabledTraversalModulesAreIgnored() {
        RouteSearchComponents components = RouteSearchComponents.withTraversalModules(
                List.of(new DisabledTraversalModule("traversal.disabled"), new WalkTraversalModule()));

        assertEquals(
                List.of("AdjacentSurfaceConnectionProvider"),
                providerClassNames(surfaceConnectionProviders(components)));
        assertEquals(1, components.surfaceStartResolver().providers().size());
        assertEquals(1, components.surfaceTransitionResolver().providers().size());
        assertEquals(1, components.surfaceRouteStepResolver().providers().size());
    }

    @Test
    void rejectsNullTraversalModules() {
        assertThrows(NullPointerException.class, () -> RouteSearchComponents.withTraversalModules(null));
        assertThrows(
                NullPointerException.class,
                () -> RouteSearchComponents.withTraversalModules(Arrays.asList(new WalkTraversalModule(), null)));
    }

    private static List<SurfaceConnectionProvider> surfaceConnectionProviders(RouteSearchComponents components) {
        assertTrue(components.surfaceGraphFactory() instanceof DefaultSurfaceRouteGraphFactory);
        DefaultSurfaceRouteGraphFactory factory =
                (DefaultSurfaceRouteGraphFactory) components.surfaceGraphFactory();
        return factory.connectionProviders();
    }

    private static List<String> providerClassNames(List<?> providers) {
        return providers.stream()
                .map(provider -> provider.getClass().getSimpleName())
                .toList();
    }

    private static void assertProviderPrecedes(List<?> providers, String earlier, String later) {
        List<String> names = providerClassNames(providers);
        assertTrue(names.indexOf(earlier) >= 0, () -> earlier + " missing from " + names);
        assertTrue(names.indexOf(later) >= 0, () -> later + " missing from " + names);
        assertTrue(names.indexOf(earlier) < names.indexOf(later), () -> names.toString());
    }

    private static final class DisabledTraversalModule implements TraversalModule {
        private final PathfinderModuleDescriptor descriptor;

        private DisabledTraversalModule(String id) {
            descriptor = new PathfinderModuleDescriptor(id, PathfinderModuleType.TRAVERSAL, Integer.MAX_VALUE, false);
        }

        @Override
        public PathfinderModuleDescriptor descriptor() {
            return descriptor;
        }

        @Override
        public List<TraversalConnectionContributor> connectionContributors() {
            SurfaceConnectionProvider provider = (context, node, connections) -> {};
            return List.of(() -> List.of(provider));
        }

        @Override
        public List<TraversalRouteContributor> routeContributors() {
            return List.of(new TraversalRouteContributor() {
                @Override
                public List<SurfaceRouteStartProvider> routeStartProviders() {
                    return List.of(context -> List.of());
                }

                @Override
                public List<SurfaceTransitionProvider> transitionProviders() {
                    return List.of(context -> Optional.empty());
                }

                @Override
                public List<SurfaceRouteStepProvider> routeStepProviders() {
                    return List.of(context -> Optional.empty());
                }
            });
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
}
