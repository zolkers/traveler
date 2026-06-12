package dev.traveler.core.layer;

import dev.traveler.core.world.block.BlockPassability;
import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.BlockBehaviorRegistry;
import dev.traveler.core.world.geometry.BlockShape;
import dev.traveler.core.world.movement.FluidHandling;
import java.util.Objects;

public record SurfaceBlock(BlockClassification classification, BlockShape shape, BlockBehavior behavior) {
    private static final BlockBehaviorRegistry DEFAULT_BEHAVIORS = BlockBehaviorRegistry.defaults();
    private static final SurfaceBlock EMPTY = new SurfaceBlock(
            new BlockClassification(BlockPassability.PASSABLE, FluidHandling.AVOID),
            BlockShape.empty(),
            DEFAULT_BEHAVIORS.behavior(BlockBehaviorKey.AIR));

    public SurfaceBlock {
        Objects.requireNonNull(classification, "classification");
        Objects.requireNonNull(shape, "shape");
        Objects.requireNonNull(behavior, "behavior");
    }

    public static SurfaceBlock empty() {
        return EMPTY;
    }

    public static SurfaceBlock solid(BlockShape shape) {
        return new SurfaceBlock(
                new BlockClassification(BlockPassability.SOLID, FluidHandling.AVOID),
                shape,
                DEFAULT_BEHAVIORS.behavior(BlockBehaviorKey.FULL_BLOCK));
    }
}
