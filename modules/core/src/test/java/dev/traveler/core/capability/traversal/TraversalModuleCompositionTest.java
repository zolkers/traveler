package dev.traveler.core.capability.traversal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.capability.traversal.api.TraversalModule;
import dev.traveler.core.capability.traversal.impl.ClimbTraversalModule;
import dev.traveler.core.capability.traversal.impl.DropTraversalModule;
import dev.traveler.core.capability.traversal.impl.JumpTraversalModule;
import dev.traveler.core.capability.traversal.impl.StandardTraversalModules;
import dev.traveler.core.capability.traversal.impl.SwimTraversalModule;
import dev.traveler.core.capability.traversal.impl.WalkTraversalModule;
import dev.traveler.core.capability.traversal.spi.TraversalConnectionContributor;
import dev.traveler.core.capability.traversal.spi.TraversalRouteContributor;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModule;
import dev.traveler.core.pathfinder.kernel.spi.PathfinderModuleType;
import dev.traveler.core.pathfinder.module.api.PathfinderModuleSelection;
import dev.traveler.core.route.start.ClimbSurfaceRouteStartProvider;
import dev.traveler.core.route.start.StandingSurfaceRouteStartProvider;
import dev.traveler.core.route.start.SubmergedSwimRouteStartProvider;
import dev.traveler.core.route.step.ClimbSurfaceRouteStepProvider;
import dev.traveler.core.route.step.DefaultSurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.ClimbSurfaceTransitionProvider;
import dev.traveler.core.world.navigation.DefaultSurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SwimSurfaceTransitionProvider;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraversalModuleCompositionTest {
    @Test
    void standardModulesUseStableOrderAndIds() {
        List<TraversalModule> modules = StandardTraversalModules.modules();

        assertEquals(
                List.of(
                        ClimbTraversalModule.class,
                        SwimTraversalModule.class,
                        WalkTraversalModule.class,
                        DropTraversalModule.class,
                        JumpTraversalModule.class),
                modules.stream().map(Object::getClass).toList());
        assertEquals(
                List.of(
                        "traversal.climb",
                        "traversal.swim",
                        "traversal.walk",
                        "traversal.drop",
                        "traversal.jump"),
                moduleIds(modules));
        assertThrows(UnsupportedOperationException.class, () -> modules.add(new WalkTraversalModule()));
    }

    @Test
    void standardModuleSelectionEnablesTraversalModulesOnly() {
        PathfinderModuleSelection selection = StandardTraversalModules.selection();

        assertEquals(List.of(), selection.disabled());
        assertEquals(
                List.of(
                        "traversal.climb",
                        "traversal.swim",
                        "traversal.walk",
                        "traversal.drop",
                        "traversal.jump"),
                moduleIds(selection.enabled()));
    }

    @Test
    void standardModulesAreEnabledTraversalModulesWithDeterministicPriorities() {
        List<TraversalModule> modules = StandardTraversalModules.modules();

        for (int priority = 0; priority < modules.size(); priority++) {
            TraversalModule module = modules.get(priority);

            assertEquals(PathfinderModuleType.TRAVERSAL, module.descriptor().type());
            assertTrue(module.descriptor().enabled());
            assertEquals(priority, module.descriptor().priority());
        }
    }

    @Test
    void walkModuleExposesRouteAndConnectionContributors() {
        TraversalModule module = new WalkTraversalModule();
        TraversalRouteContributor routeContributor = onlyRouteContributor(module);

        assertConnectionProvider(module, "AdjacentSurfaceConnectionProvider");
        assertTrue(routeContributor.routeStartProviders().stream()
                .anyMatch(StandingSurfaceRouteStartProvider.class::isInstance));
        assertTrue(routeContributor.transitionProviders().stream()
                .anyMatch(DefaultSurfaceTransitionProvider.class::isInstance));
        assertTrue(routeContributor.routeStepProviders().stream()
                .anyMatch(DefaultSurfaceRouteStepProvider.class::isInstance));
    }

    @Test
    void climbModuleExposesRouteAndConnectionContributors() {
        TraversalModule module = new ClimbTraversalModule();
        TraversalRouteContributor routeContributor = onlyRouteContributor(module);

        assertConnectionProvider(module, "ClimbSurfaceConnectionProvider");
        assertTrue(routeContributor.routeStartProviders().stream()
                .anyMatch(ClimbSurfaceRouteStartProvider.class::isInstance));
        assertTrue(routeContributor.transitionProviders().stream()
                .anyMatch(ClimbSurfaceTransitionProvider.class::isInstance));
        assertTrue(routeContributor.routeStepProviders().stream()
                .anyMatch(ClimbSurfaceRouteStepProvider.class::isInstance));
    }

    @Test
    void swimModuleExposesRouteConnectionAndRecoveryContributors() {
        TraversalModule module = new SwimTraversalModule();
        TraversalRouteContributor routeContributor = onlyRouteContributor(module);

        assertConnectionProvider(module, "SwimSurfaceConnectionProvider");
        assertTrue(routeContributor.routeStartProviders().stream()
                .anyMatch(SubmergedSwimRouteStartProvider.class::isInstance));
        assertTrue(routeContributor.transitionProviders().stream()
                .anyMatch(SwimSurfaceTransitionProvider.class::isInstance));
        assertTrue(routeContributor.routeStepProviders().stream()
                .anyMatch(DefaultSurfaceRouteStepProvider.class::isInstance));
        assertEquals(1, module.recoveryContributors().size());
    }

    @Test
    void dropAndJumpModulesExposeConnectionContributorsOnly() {
        TraversalModule drop = new DropTraversalModule();
        TraversalModule jump = new JumpTraversalModule();

        assertConnectionProvider(drop, "DropSurfaceConnectionProvider");
        assertTrue(drop.routeContributors().isEmpty());
        assertConnectionProvider(jump, "JumpSurfaceConnectionProvider");
        assertTrue(jump.routeContributors().isEmpty());
    }

    @Test
    void standardModuleContributorListsAreUnmodifiable() {
        for (TraversalModule module : StandardTraversalModules.modules()) {
            assertThrows(UnsupportedOperationException.class, () -> module.connectionContributors().add(null));
            assertThrows(UnsupportedOperationException.class, () -> module.routeContributors().add(null));
            assertThrows(UnsupportedOperationException.class, () -> module.executionContributors().add(null));
            assertThrows(UnsupportedOperationException.class, () -> module.recoveryContributors().add(null));
            assertThrows(UnsupportedOperationException.class, () -> module.debugContributors().add(null));
        }
    }

    @Test
    void standardModuleNestedProviderListsAreUnmodifiable() {
        for (TraversalModule module : StandardTraversalModules.modules()) {
            for (TraversalConnectionContributor contributor : module.connectionContributors()) {
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> contributor.surfaceConnectionProviders().add(null));
            }
            for (TraversalRouteContributor contributor : module.routeContributors()) {
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> contributor.routeStartProviders().add(null));
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> contributor.transitionProviders().add(null));
                assertThrows(
                        UnsupportedOperationException.class,
                        () -> contributor.routeStepProviders().add(null));
            }
        }
    }

    private static TraversalRouteContributor onlyRouteContributor(TraversalModule module) {
        assertEquals(1, module.routeContributors().size());
        return module.routeContributors().get(0);
    }

    private static void assertConnectionProvider(TraversalModule module, String expectedProviderClassName) {
        assertEquals(1, module.connectionContributors().size());
        assertTrue(module.connectionContributors().get(0).surfaceConnectionProviders().stream()
                .anyMatch(provider -> provider.getClass().getSimpleName().equals(expectedProviderClassName)));
    }

    private static List<String> moduleIds(List<? extends PathfinderModule> modules) {
        return modules.stream()
                .map(PathfinderModule::descriptor)
                .map(descriptor -> descriptor.id())
                .toList();
    }
}
