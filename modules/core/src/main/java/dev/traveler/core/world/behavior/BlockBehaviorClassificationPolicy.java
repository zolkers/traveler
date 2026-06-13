package dev.traveler.core.world.behavior;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import java.util.Objects;

public final class BlockBehaviorClassificationPolicy {
    public BlockClassification classify(BlockClassification base, BlockBehavior behavior) {
        BlockClassification safeBase = Objects.requireNonNull(base, "base");
        if (!isPartialWalkableSurface(Objects.requireNonNull(behavior, "behavior"))) {
            return safeBase;
        }
        return new BlockClassification(BlockPassability.WALKABLE, safeBase.fluidHandling());
    }

    private boolean isPartialWalkableSurface(BlockBehavior behavior) {
        if (behavior.key() == BlockBehaviorKey.SLAB || behavior.key() == BlockBehaviorKey.STAIR) {
            return true;
        }
        if (behavior instanceof WaterloggedBlockBehavior waterlogged) {
            return isPartialWalkableSurface(waterlogged.delegate());
        }
        return false;
    }
}
