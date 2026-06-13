package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.SlabBlockBehavior;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private static Map<BlockPosition, SurfaceBlock> flatSurface(int minX, int maxX) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            blocks.put(new BlockPosition(x, 63, 0), SurfaceBlock.solid(BlockShape.fullCube()));
        }
        return blocks;
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

    private static SurfaceBlock surface(BlockShape shape, BlockBehavior behavior) {
        return new SurfaceBlock(
                new dev.traveler.core.layer.BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID),
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
