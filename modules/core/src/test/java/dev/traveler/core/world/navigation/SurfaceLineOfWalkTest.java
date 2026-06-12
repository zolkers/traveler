package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.MovementCapabilities;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceLineOfWalkTest {
    private static final MovementCapabilities PLAYER =
            new MovementCapabilities(true, false, false, false, 0.6, 1.25, 3.0);

    @Test
    void acceptsClearFlatSurfaceLines() {
        SurfaceLineOfWalk lineOfWalk = lineOfWalk(flatWorld(0, 4));

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertTrue(clear);
    }

    @Test
    void rejectsSurfaceLinesThroughBlockedBodySpace() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                supportAt(0), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(1), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(2), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(3), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(4), SurfaceBlock.solid(BlockShape.fullCube()),
                new BlockPosition(2, 64, 0), SurfaceBlock.solid(BlockShape.fullCube())));
        SurfaceLineOfWalk lineOfWalk = lineOfWalk(world);

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertFalse(clear);
    }

    @Test
    void rejectsSurfaceLinesWithoutIntermediateSupport() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                supportAt(0), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(4), SurfaceBlock.solid(BlockShape.fullCube())));
        SurfaceLineOfWalk lineOfWalk = lineOfWalk(world);

        boolean clear = lineOfWalk.hasLineOfWalk(nodeAt(0), nodeAt(4));

        assertFalse(clear);
    }

    private static SurfaceWorldLayer flatWorld(int minX, int maxX) {
        Map<BlockPosition, SurfaceBlock> blocks = new java.util.HashMap<>();
        for (int x = minX; x <= maxX; x++) {
            blocks.put(supportAt(x), SurfaceBlock.solid(BlockShape.fullCube()));
        }
        return new TestSurfaceWorldLayer(blocks);
    }

    private static SurfaceNode nodeAt(int x) {
        return new SurfaceNode(supportAt(x), 1, 1, 64.0);
    }

    private static SurfaceLineOfWalk lineOfWalk(SurfaceWorldLayer world) {
        return new SurfaceLineOfWalk(world, nodeAt(0), nodeAt(4), PLAYER, 8, 4);
    }

    private static BlockPosition supportAt(int x) {
        return new BlockPosition(x, 63, 0);
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
