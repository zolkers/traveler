package dev.traveler.core.world.navigation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import dev.traveler.core.world.surface.SurfaceNode;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SurfaceSmoothingPolicyTest {
    private static final BlockBehaviorRegistry BEHAVIORS = BlockBehaviorRegistry.defaults();

    @Test
    void preservesNodesThatRepresentVerticalMovementActions() {
        SurfaceSmoothingPolicy policy = new SurfaceSmoothingPolicy(flatWorld());

        boolean required = policy.mustPreserve(nodeAt(0, 64.0), nodeAt(1, 65.0), nodeAt(2, 65.0));

        assertTrue(required);
    }

    @Test
    void preservesNodesThatEnterOrLeaveSpecialBlockBehaviors() {
        SurfaceWorldLayer world = new TestSurfaceWorldLayer(Map.of(
                supportAt(0), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(1), slab(),
                supportAt(2), SurfaceBlock.solid(BlockShape.fullCube())));
        SurfaceSmoothingPolicy policy = new SurfaceSmoothingPolicy(world);

        boolean required = policy.mustPreserve(nodeAt(0, 64.0), nodeAt(1, 64.0), nodeAt(2, 64.0));

        assertTrue(required);
    }

    @Test
    void allowsFlatFullBlockNodesToBeSmoothedAway() {
        SurfaceSmoothingPolicy policy = new SurfaceSmoothingPolicy(flatWorld());

        boolean required = policy.mustPreserve(nodeAt(0, 64.0), nodeAt(1, 64.0), nodeAt(2, 64.0));

        assertFalse(required);
    }

    private static SurfaceWorldLayer flatWorld() {
        return new TestSurfaceWorldLayer(Map.of(
                supportAt(0), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(1), SurfaceBlock.solid(BlockShape.fullCube()),
                supportAt(2), SurfaceBlock.solid(BlockShape.fullCube())));
    }

    private static SurfaceNode nodeAt(int x, double floorY) {
        return new SurfaceNode(supportAt(x), 1, 1, floorY);
    }

    private static BlockPosition supportAt(int x) {
        return new BlockPosition(x, 63, 0);
    }

    private static SurfaceBlock slab() {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID),
                BlockShape.topSlab(),
                BEHAVIORS.behavior(BlockBehaviorKey.SLAB));
    }

    private record TestSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
        @Override
        public SurfaceBlock surfaceBlock(BlockPosition position) {
            return blocks.getOrDefault(position, SurfaceBlock.empty());
        }
    }
}
