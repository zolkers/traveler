package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.common.geometry.WorldPoint;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.EntityDimensions;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceBodyClearanceTest {
    private static final EntityDimensions PLAYER = new EntityDimensions(0.6, 1.8);

    @Test
    void acceptsPointWhosePlayerBodyOnlyTouchesWallBoundary() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                new BlockPosition(1, 64, 0), SurfaceBlock.solid(BlockShape.fullCube())));

        boolean clear = SurfaceBodyClearance.hasClearance(world, new WorldPoint(0.7, 64.0, 0.5), PLAYER);

        assertTrue(clear);
    }

    @Test
    void rejectsPointWhosePlayerBodyIntersectsWall() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                new BlockPosition(1, 64, 0), SurfaceBlock.solid(BlockShape.fullCube())));

        boolean clear = SurfaceBodyClearance.hasClearance(world, new WorldPoint(0.8, 64.0, 0.5), PLAYER);

        assertFalse(clear);
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
