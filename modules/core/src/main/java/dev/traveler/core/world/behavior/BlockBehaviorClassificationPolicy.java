package dev.traveler.core.world.behavior;

import dev.traveler.core.layer.BlockClassification;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import dev.traveler.core.world.block.BlockPassability;
import java.util.Objects;

public final class BlockBehaviorClassificationPolicy {
    public BlockClassification classify(BlockClassification base, BlockBehavior behavior) {
        BlockClassification safeBase = Objects.requireNonNull(base, "base");
        BlockBehavior safeBehavior = Objects.requireNonNull(behavior, "behavior");
        if (isPassableActionVolume(safeBehavior)) {
            return new BlockClassification(BlockPassability.PASSABLE, safeBase.fluidHandling());
        }
        if (!isPartialWalkableSurface(safeBehavior)) {
            return safeBase;
        }
        return new BlockClassification(BlockPassability.WALKABLE, safeBase.fluidHandling());
    }

    private boolean isPartialWalkableSurface(BlockBehavior behavior) {
        if (behavior.key() == BlockBehaviorKey.SLAB
                || behavior.key() == BlockBehaviorKey.STAIR
                || behavior.key() == BlockBehaviorKey.CARPET) {
            return true;
        }
        if (behavior instanceof WaterloggedBlockBehavior waterlogged) {
            return isPartialWalkableSurface(waterlogged.delegate());
        }
        return false;
    }

    private boolean isPassableActionVolume(BlockBehavior behavior) {
        if (behavior.key() == BlockBehaviorKey.LADDER || behavior.key() == BlockBehaviorKey.VINE) {
            return true;
        }
        if (behavior instanceof WaterloggedBlockBehavior waterlogged) {
            return isPassableActionVolume(waterlogged.delegate());
        }
        return false;
    }
}
