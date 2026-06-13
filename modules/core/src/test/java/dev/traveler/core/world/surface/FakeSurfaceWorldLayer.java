package dev.traveler.core.world.surface;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.layer.SurfaceBlock;
import dev.traveler.core.layer.SurfaceWorldLayer;
import dev.traveler.core.world.block.BlockPosition;
import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.special.FullBlockBehavior;
import dev.traveler.core.world.behavior.special.SlabBlockBehavior;
import dev.traveler.core.world.behavior.special.StairBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.geometry.CollisionBox;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.List;
import java.util.Map;

record FakeSurfaceWorldLayer(Map<BlockPosition, SurfaceBlock> blocks) implements SurfaceWorldLayer {
    static SurfaceBlock fullBlock() {
        return surface(BlockShape.fullCube(), new FullBlockBehavior());
    }

    static SurfaceBlock bottomSlab() {
        return surface(BlockShape.bottomSlab(), new SlabBlockBehavior());
    }

    static SurfaceBlock waterloggedBottomSlab() {
        return surface(BlockShape.bottomSlab(), new WaterloggedBlockBehavior(new SlabBlockBehavior()));
    }

    static SurfaceBlock topSlab() {
        return surface(BlockShape.topSlab(), new SlabBlockBehavior());
    }

    static SurfaceBlock northFacingBottomStair() {
        return surface(
                BlockShape.of(List.of(
                        new CollisionBox(0.0, 0.0, 0.0, 1.0, 0.5, 1.0),
                        new CollisionBox(0.0, 0.5, 0.5, 1.0, 1.0, 1.0))),
                new StairBlockBehavior(HorizontalFacing.NORTH));
    }

    static SurfaceBlock solid(BlockShape shape) {
        return surface(shape, new FullBlockBehavior());
    }

    private static SurfaceBlock surface(BlockShape shape, BlockBehavior behavior) {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID),
                shape,
                behavior);
    }

    @Override
    public SurfaceBlock surfaceBlock(BlockPosition position) {
        return blocks.getOrDefault(position, SurfaceBlock.empty());
    }
}
