package dev.traveler.core.route;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.path.PathfinderStatus;
import dev.traveler.core.world.behavior.decision.MovementAction;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import java.util.HashMap;
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

    private static Map<BlockPosition, SurfaceBlock> flatSurface(int minX, int maxX) {
        Map<BlockPosition, SurfaceBlock> blocks = new HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            blocks.put(new BlockPosition(x, 63, 0), SurfaceBlock.solid(BlockShape.fullCube()));
        }
        return blocks;
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
