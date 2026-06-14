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
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.LadderBlockBehavior;
import dev.traveler.core.world.behavior.special.SlabBlockBehavior;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.behavior.special.VineBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import dev.traveler.core.world.movement.FluidHandling;
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
    void findsRouteThatClimbsVineColumn() {
        TestSurfaceWorldLayer world =
                new TestSurfaceWorldLayer(climbColumn(new VineBlockBehavior(Set.of(HorizontalFacing.WEST), false)));
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
