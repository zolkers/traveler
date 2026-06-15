package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.graph.Connection;
import dev.traveler.core.graph.Graph;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.ClimbableBlockBehavior;
import dev.traveler.core.world.behavior.special.ClimbSurfaceGeometry;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.FluidBlockBehavior;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.behavior.special.SlabBlockBehavior;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.behavior.special.VineBlockBehavior;
import dev.traveler.core.navigation.spatial.NavigationPoint;
import dev.traveler.core.route.start.SurfaceRouteStartProvider;
import dev.traveler.core.route.start.SurfaceRouteStartResolver;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.navigation.SurfaceConnectionProvider;
import dev.traveler.core.route.step.DefaultSurfaceRouteStepProvider;
import dev.traveler.core.route.step.SurfaceRouteStepProvider;
import dev.traveler.core.world.navigation.SurfaceTransitionProvider;
import dev.traveler.core.world.navigation.SurfaceTraversalFeature;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RouteSearchServiceTest {
    @Test
    void findsSurfaceRouteWithSemanticActions() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(flatSurface(0, 2));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(2, 63, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertTrue(result.route().isPresent());
        assertEquals(RouteSearchFailureReason.NONE, result.diagnostics().reason());
        assertEquals(MovementAction.WALK, result.route().orElseThrow().actions().getFirst());
    }

    @Test
    void acceptsExplicitRouteGoalWhileKeepingBlockTargetCompatibility() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(flatSurface(0, 2));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());
        RouteGoal goal = RouteGoal.blockTarget(new BlockPosition(2, 63, 0));

        RouteSearchResult result = service.search(world, new BlockPosition(0, 64, 0), goal);

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(goal, result.goal());
        assertEquals(RouteSearchFailureReason.NONE, result.diagnostics().reason());
    }

    @Test
    void reportsMissingStartSurface() {
        Map<BlockPosition, SurfaceBlock> blocks = flatSurface(2, 2);
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(blocks);

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(2, 63, 0));

        assertEquals(PathfinderStatus.NOT_FOUND, result.status());
        assertEquals(RouteSearchFailureReason.NO_START_SURFACE, result.diagnostics().reason());
        assertFalse(result.route().isPresent());
    }

    @Test
    void reportsMissingGoalSurface() {
        Map<BlockPosition, SurfaceBlock> blocks = flatSurface(0, 0);
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(blocks);

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(2, 63, 0));

        assertEquals(PathfinderStatus.NOT_FOUND, result.status());
        assertEquals(RouteSearchFailureReason.NO_GOAL_SURFACE, result.diagnostics().reason());
        assertFalse(result.route().isPresent());
    }

    @Test
    void usesDirectFallbackWhenWorldIsUnavailable() {
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(null, new BlockPosition(0, 64, 0), new BlockPosition(2, 64, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(RouteSearchFailureReason.WORLD_UNAVAILABLE, result.diagnostics().reason());
        assertFalse(result.route().isPresent());
    }

    @Test
    void findsRouteOutOfOneBlockSpaceSurroundedBySlabsAndStair() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(slabRingWithStairExit());
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(2, 63, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertTrue(result.route().isPresent());
        assertTrue(hasVerticalAction(result.route().orElseThrow()));
    }

    @Test
    void compactsFlatSameSpecialBehaviorSurfaceRuns() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(bottomSlabSurface(0, 4, -1, 1));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(4, 63, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertEquals(2, route.nodes().size());
        assertEquals(List.of(MovementAction.WALK), route.actions());
    }

    @Test
    void acceptsCustomSurfaceGraphFactory() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(flatSurface(0, 2));
        RouteSearchComponents components = RouteSearchComponents.standard()
                .withSurfaceGraphFactory((layer, start, goal, settings) -> directSurfaceGraph(goal));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient(), components);

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(2, 63, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(2, result.route().orElseThrow().nodes().size());
    }

    @Test
    void acceptsCustomSurfaceSmoothingSelector() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(bottomSlabSurface(0, 4, -1, 1));
        RouteSearchComponents components = RouteSearchComponents.standard()
                .withSurfaceSmoothingSelector((path, anchor, limit, lineOfWalk) -> anchor + 1);
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient(), components);

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(4, 63, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertTrue(result.route().orElseThrow().nodes().size() > 2);
    }

    @Test
    void acceptsCustomSurfaceStartResolver() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(flatSurface(0, 2));
        SurfaceNode injectedStart = new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, 64.0);
        RouteSearchComponents components = RouteSearchComponents.standard()
                .withSurfaceStartResolver(new SurfaceRouteStartResolver(List.of(context -> List.of(injectedStart))));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient(), components);

        RouteSearchResult result =
                service.search(world, new BlockPosition(99, 99, 99), new BlockPosition(2, 63, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(injectedStart, result.route().orElseThrow().nodes().getFirst());
    }

    @Test
    void acceptsCustomSurfaceTraversalFeatureForStartAndExpansion() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(flatSurface(0, 2));
        SurfaceNode injectedStart = new SurfaceNode(new BlockPosition(0, 63, 0), 1, 1, 64.0);
        SurfaceNode injectedGoal = new SurfaceNode(new BlockPosition(2, 63, 0), 0, 0, 64.0);
        SurfaceTraversalFeature shortcut = new SurfaceTraversalFeature() {
            @Override
            public List<SurfaceRouteStartProvider> routeStartProviders() {
                return List.of(context -> List.of(injectedStart));
            }

            @Override
            public List<SurfaceConnectionProvider> connectionProviders() {
                return List.of((context, node, connections) ->
                        connections.add(new Connection<>(node, injectedGoal, 0.25)));
            }

            @Override
            public List<SurfaceTransitionProvider> transitionProviders() {
                return List.of(context -> java.util.Optional.of(MovementDecision.swim()));
            }

            @Override
            public List<SurfaceRouteStepProvider> routeStepProviders() {
                return List.of(new DefaultSurfaceRouteStepProvider());
            }
        };
        RouteSearchComponents components = RouteSearchComponents.standard()
                .withSurfaceTraversalFeatures(List.of(shortcut));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient(), components);

        RouteSearchResult result =
                service.search(world, new BlockPosition(99, 99, 99), new BlockPosition(2, 63, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertEquals(injectedStart, result.route().orElseThrow().nodes().getFirst());
        assertEquals(injectedGoal, result.route().orElseThrow().nodes().getLast());
        assertEquals(List.of(MovementAction.SWIM), result.route().orElseThrow().actions());
        assertEquals(List.of(new NavigationPoint(2.25, 64.0, 0.25)), result.route().orElseThrow().actionTargets());
    }

    @Test
    void findsRouteThatClimbsLadderColumn() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(climbColumn(new LadderBlockBehavior(HorizontalFacing.WEST)));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 65, 1));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertTrue(result.route().orElseThrow().actions().contains(MovementAction.CLIMB));
    }

    @Test
    void climbsAndDescendsLadderFromVisibleFacingSide() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(visibleFacingLadderColumn(HorizontalFacing.EAST, 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult climb =
                service.search(world, new BlockPosition(1, 64, 0), new BlockPosition(1, 83, 0));
        RouteSearchResult descend =
                service.search(world, new BlockPosition(1, 83, 0), new BlockPosition(1, 64, 0));

        assertEquals(PathfinderStatus.FOUND, climb.status());
        assertEquals(PathfinderStatus.FOUND, descend.status());
        assertTrue(climb.route().orElseThrow().actions().contains(MovementAction.CLIMB));
        assertTrue(descend.route().orElseThrow().actions().contains(MovementAction.CLIMB));
    }

    @Test
    void climbRouteKeepsEveryLadderBlockAsNavigationNode() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(climbColumn(new LadderBlockBehavior(HorizontalFacing.WEST)));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 65, 1));

        RoutePath route = result.route().orElseThrow();
        assertTrue(route.actions().contains(MovementAction.CLIMB));
        assertTrue(route.nodes().stream().anyMatch(node -> node.blockPosition().equals(new BlockPosition(1, 64, 0))));
        assertTrue(route.nodes().stream().anyMatch(node -> node.blockPosition().equals(new BlockPosition(1, 65, 0))));
    }

    @Test
    void doesNotClimbLadderFromNonFacingSide() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(nonFacingLadderColumn(new LadderBlockBehavior(HorizontalFacing.WEST)));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(1, 64, -1), new BlockPosition(1, 65, -1));

        assertEquals(PathfinderStatus.NOT_FOUND, result.status());
    }

    @Test
    void descendsLadderColumnAsClimbInsteadOfDrop() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(climbColumn(new LadderBlockBehavior(HorizontalFacing.WEST)));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(1, 66, 1), new BlockPosition(0, 64, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertTrue(route.actions().contains(MovementAction.CLIMB));
        assertFalse(route.actions().contains(MovementAction.DROP));
    }

    @Test
    void descendsLongLadderColumnAsClimbInsteadOfDrop() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(1, 83, 1), new BlockPosition(0, 64, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertTrue(route.actions().contains(MovementAction.CLIMB));
        assertFalse(route.actions().contains(MovementAction.DROP));
    }

    @Test
    void climbsLongLadderColumnAsClimbInsteadOfSmoothingShortcut() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 83, 1));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertEquals(20, route.actionCount(MovementAction.CLIMB));
        assertEquals(19, climbColumnNodeCount(route, 1, 0));
        assertFalse(route.actions().contains(MovementAction.DROP));
    }

    @Test
    void longLadderClimbTargetsTheClimbableFaceCenterForColumnSegments() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RoutePath route = service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 83, 1))
                .route()
                .orElseThrow();

        assertClimbColumnTargetsLadderFace(route);
    }

    @Test
    void climbTargetsComeFromTheClimbableBehaviorGeometry() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new CustomClimbGeometryBehavior(), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RoutePath route = service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 83, 1))
                .route()
                .orElseThrow();

        List<RouteStep> columnClimbSteps = climbColumnSteps(route);
        assertEquals(19, columnClimbSteps.size());
        for (RouteStep step : columnClimbSteps) {
            assertEquals(new NavigationPoint(1.42, step.to().floorY(), 0.37), step.targetPoint());
        }
    }

    @Test
    void climbsToLadderBlockTargetByResolvingNearbyLanding() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 82, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertEquals(20, route.actionCount(MovementAction.CLIMB));
        assertEquals(19, climbColumnNodeCount(route, 1, 0));
        assertEquals(new BlockPosition(1, 82, 1), route.nodes().getLast().blockPosition());
    }

    @Test
    void climbsToLadderBlockTargetWithoutNearbyLanding() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallFreeClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 82, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertTrue(route.actions().contains(MovementAction.CLIMB));
        assertEquals(new BlockPosition(1, 82, 0), route.nodes().getLast().blockPosition());
    }

    @Test
    void longClimbKeepsLandingPointsAndActionTargetsForBothDirections() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RoutePath climbUp = service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 83, 1))
                .route()
                .orElseThrow();
        RoutePath climbDown = service.search(world, new BlockPosition(1, 83, 1), new BlockPosition(0, 64, 0))
                .route()
                .orElseThrow();

        assertLongClimbUsesColumnNodes(climbUp);
        assertLongClimbUsesColumnNodes(climbDown);
    }

    @Test
    void longLadderDescentKeepsEachLadderBlockAsRouteNode() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(1, 83, 1), new BlockPosition(0, 64, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertEquals(20, route.actionCount(MovementAction.CLIMB));
        assertEquals(19, climbColumnNodeCount(route, 1, 0));
        assertFalse(route.actions().contains(MovementAction.DROP));
    }

    @Test
    void startsRouteFromInsideTallLadderColumnWhenClimbingUp() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(1, 70, 0), new BlockPosition(1, 83, 1));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertTrue(route.nodes().getFirst().blockPosition().equals(new BlockPosition(1, 70, 0)));
        assertTrue(route.actions().contains(MovementAction.CLIMB));
        assertFalse(route.actions().contains(MovementAction.DROP));
    }

    @Test
    void startsRouteFromInsideTallLadderColumnWhenClimbingDown() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(tallClimbColumn(new LadderBlockBehavior(HorizontalFacing.WEST), 64, 82));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(1, 76, 0), new BlockPosition(0, 64, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertTrue(route.nodes().getFirst().blockPosition().equals(new BlockPosition(1, 76, 0)));
        assertTrue(route.actions().contains(MovementAction.CLIMB));
        assertFalse(route.actions().contains(MovementAction.DROP));
    }

    @Test
    void findsConfiguredLongDropRoute() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(dropSurface(63, 55));
        RouteSearchSettings settings = RouteSearchSettings.standardClient().withMaxSafeFallDistance(10.0);
        RouteSearchService service = new RouteSearchService(settings);

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 55, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertTrue(result.route().orElseThrow().actions().contains(MovementAction.DROP));
    }

    @Test
    void rejectsLongDropWhenSettingsKeepDefaultSafeFallDistance() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(dropSurface(63, 55));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 55, 0));

        assertEquals(PathfinderStatus.NOT_FOUND, result.status());
    }

    @Test
    void swimsAcrossTopWaterSurfaceWhenSwimmingIsEnabled() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(surfaceWaterLane(0, 4, 63));
        RouteSearchService service = new RouteSearchService(swimmingSettings());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(4, 64, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertTrue(route.actions().contains(MovementAction.SWIM));
        assertTrue(route.points().stream().allMatch(point -> point.y() == 64.0));
    }

    @Test
    void swimSurfaceSearchIgnoresSubmergedWaterLevels() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(deepWaterLane(0, 4, 61, 63));
        RouteSearchService service = new RouteSearchService(swimmingSettings());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(4, 64, 0));

        assertEquals(PathfinderStatus.FOUND, result.status());
        RoutePath route = result.route().orElseThrow();
        assertTrue(route.actions().contains(MovementAction.SWIM));
        assertTrue(route.points().stream().allMatch(point -> point.y() == 64.0));
    }

    @Test
    void refusesWaterRouteWhenSwimmingIsDisabled() {
        TestSurfaceWorldLayer world = new TestSurfaceWorldLayer(surfaceWaterLane(0, 4, 63));
        RouteSearchService service =
                new RouteSearchService(RouteSearchSettings.standardClient().withSwimmingEnabled(false));

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(4, 64, 0));

        assertEquals(PathfinderStatus.NOT_FOUND, result.status());
    }

    @Test
    void standardClientCanSwimBecauseTheMinecraftPlayerCanSwim() {
        assertTrue(RouteSearchSettings.standardClient().movementProfile().capabilities().canSwim());
    }

    @Test
    void findsRouteThatClimbsVineColumn() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(climbColumn(new VineBlockBehavior(Set.of(HorizontalFacing.EAST), false)));
        RouteSearchService service = new RouteSearchService(RouteSearchSettings.standardClient());

        RouteSearchResult result =
                service.search(world, new BlockPosition(0, 64, 0), new BlockPosition(1, 65, 1));

        assertEquals(PathfinderStatus.FOUND, result.status());
        assertTrue(result.route().orElseThrow().actions().contains(MovementAction.CLIMB));
    }

    private static Graph<SurfaceNode> directSurfaceGraph(SurfaceNode goal) {
        return node -> node.sameSubcell(goal) ? List.of() : List.of(new Connection<>(node, goal, 0.25));
    }

    private static Map<BlockPosition, SurfaceBlock> flatSurface(int minX, int maxX) {
        return surfaceRectangle(minX, maxX, 0, 0, SurfaceBlock.solid(BlockShape.fullCube()));
    }

    private static Map<BlockPosition, SurfaceBlock> bottomSlabSurface(
            int minX,
            int maxX,
            int minZ,
            int maxZ) {
        return surfaceRectangle(minX, maxX, minZ, maxZ, bottomSlab());
    }

    private static Map<BlockPosition, SurfaceBlock> surfaceRectangle(
            int minX,
            int maxX,
            int minZ,
            int maxZ,
            SurfaceBlock block) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            addSurfaceColumn(blocks, x, minZ, maxZ, block);
        }
        return blocks;
    }

    private static void addSurfaceColumn(
            Map<BlockPosition, SurfaceBlock> blocks,
            int x,
            int minZ,
            int maxZ,
            SurfaceBlock block) {
        for (int z = minZ; z <= maxZ; z++) {
            blocks.put(new BlockPosition(x, 63, z), block);
        }
    }

    private static boolean hasVerticalAction(RoutePath route) {
        return route.actions().stream().anyMatch(RouteSearchServiceTest::isVerticalAction);
    }

    private static RouteSearchSettings swimmingSettings() {
        return RouteSearchSettings.standardClient().withSwimmingEnabled(true);
    }

    private static void assertLongClimbUsesColumnNodes(RoutePath route) {
        int climbIndex = route.actions().indexOf(MovementAction.CLIMB);
        assertTrue(climbIndex >= 0);
        assertEquals(19, climbColumnNodeCount(route, 1, 0));
        assertClimbColumnTargetsLadderFace(route);
    }

    private static void assertClimbColumnTargetsLadderFace(RoutePath route) {
        List<RouteStep> columnClimbSteps = climbColumnSteps(route);
        assertEquals(19, columnClimbSteps.size());
        for (RouteStep step : columnClimbSteps) {
            assertEquals(new NavigationPoint(1.3, step.to().floorY(), 0.5), step.targetPoint());
        }
    }

    private static List<RouteStep> climbColumnSteps(RoutePath route) {
        return route.steps().stream()
                .filter(step -> step.action() == MovementAction.CLIMB)
                .filter(step -> step.to().blockPosition().x() == 1 && step.to().blockPosition().z() == 0)
                .toList();
    }

    private static long climbColumnNodeCount(RoutePath route, int x, int z) {
        return route.nodes().stream()
                .map(SurfaceNode::blockPosition)
                .filter(position -> position.x() == x && position.z() == z)
                .count();
    }

    private static boolean isVerticalAction(MovementAction action) {
        return action == MovementAction.STEP_UP || action == MovementAction.JUMP || action == MovementAction.DROP;
    }

    private static Map<BlockPosition, SurfaceBlock> slabRingWithStairExit() {
        Map<BlockPosition, SurfaceBlock> blocks = flatSurface(-1, 2);
        blocks.put(new BlockPosition(0, 63, -1), fullBlock());
        blocks.put(new BlockPosition(1, 64, 0), stair(HorizontalFacing.WEST));
        blocks.put(new BlockPosition(0, 64, 1), bottomSlab());
        blocks.put(new BlockPosition(-1, 64, 0), bottomSlab());
        return blocks;
    }

    private static SurfaceBlock fullBlock() {
        return surface(BlockShape.fullCube(), new FullBlockBehavior());
    }

    private static SurfaceBlock bottomSlab() {
        return surface(BlockShape.bottomSlab(), new SlabBlockBehavior());
    }

    private static SurfaceBlock stair(HorizontalFacing facing) {
        return surface(stairShape(), new StairBlockBehavior(facing));
    }

    private static Map<BlockPosition, SurfaceBlock> climbColumn(BlockBehavior climbable) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        blocks.put(new BlockPosition(0, 63, 0), fullBlock());
        blocks.put(new BlockPosition(1, 64, 0), passable(BlockShape.empty(), climbable));
        blocks.put(new BlockPosition(1, 65, 0), passable(BlockShape.empty(), climbable));
        blocks.put(new BlockPosition(1, 65, 1), fullBlock());
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> nonFacingLadderColumn(BlockBehavior climbable) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        blocks.put(new BlockPosition(1, 63, -1), fullBlock());
        blocks.put(new BlockPosition(1, 64, 0), passable(BlockShape.empty(), climbable));
        blocks.put(new BlockPosition(1, 65, 0), passable(BlockShape.empty(), climbable));
        blocks.put(new BlockPosition(1, 65, -1), fullBlock());
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> tallClimbColumn(
            BlockBehavior climbable,
            int minY,
            int maxY) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        blocks.put(new BlockPosition(0, minY - 1, 0), fullBlock());
        for (int y = minY; y <= maxY; y++) {
            blocks.put(new BlockPosition(1, y, 0), passable(BlockShape.empty(), climbable));
        }
        blocks.put(new BlockPosition(1, maxY, 1), fullBlock());
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> tallFreeClimbColumn(
            BlockBehavior climbable,
            int minY,
            int maxY) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        blocks.put(new BlockPosition(0, minY - 1, 0), fullBlock());
        for (int y = minY; y <= maxY; y++) {
            blocks.put(new BlockPosition(1, y, 0), passable(BlockShape.empty(), climbable));
        }
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> visibleFacingLadderColumn(
            HorizontalFacing facing,
            int minY,
            int maxY) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        blocks.put(new BlockPosition(facing.xOffset(), minY - 1, facing.zOffset()), fullBlock());
        for (int y = minY; y <= maxY; y++) {
            blocks.put(new BlockPosition(0, y, 0), passable(BlockShape.empty(), new LadderBlockBehavior(facing)));
        }
        blocks.put(new BlockPosition(facing.xOffset(), maxY, facing.zOffset()), fullBlock());
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> dropSurface(int highY, int lowY) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        blocks.put(new BlockPosition(0, highY, 0), fullBlock());
        blocks.put(new BlockPosition(1, lowY, 0), fullBlock());
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> surfaceWaterLane(int minX, int maxX, int y) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            blocks.put(new BlockPosition(x, y, 0), water());
        }
        return blocks;
    }

    private static Map<BlockPosition, SurfaceBlock> deepWaterLane(int minX, int maxX, int minY, int maxY) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                blocks.put(new BlockPosition(x, y, 0), water());
            }
        }
        return blocks;
    }

    private static SurfaceBlock surface(BlockShape shape, BlockBehavior behavior) {
        return new SurfaceBlock(
                new dev.traveler.core.layer.BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID),
                shape,
                behavior);
    }

    private static SurfaceBlock passable(BlockShape shape, BlockBehavior behavior) {
        return new SurfaceBlock(
                new dev.traveler.core.layer.BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID),
                shape,
                behavior);
    }

    private static SurfaceBlock water() {
        return new SurfaceBlock(
                new dev.traveler.core.layer.BlockClassification(BlockPassability.PASSABLE, FluidHandling.ALLOW),
                BlockShape.empty(),
                new FluidBlockBehavior());
    }

    private static final class CustomClimbGeometryBehavior extends ClimbableBlockBehavior {
        @Override
        public BlockBehaviorKey key() {
            return BlockBehaviorKey.LADDER;
        }

        @Override
        public Set<HorizontalFacing> climbableFaces() {
            return Set.of(HorizontalFacing.WEST);
        }

        @Override
        protected ClimbSurfaceGeometry climbSurfaceGeometry(HorizontalFacing face) {
            return new ClimbSurfaceGeometry(0, 1, 0.42, 0.37);
        }
    }

    private static BlockShape stairShape() {
        return BlockShape.of(List.of(
                new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                new CollisionBox(0.0, 0.5, 0.0, 0.5, 1.0, 1.0)));
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
