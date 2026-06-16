package dev.traveler.core.world.behavior;

import dev.traveler.core.layer.BlockClassification;
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
        return switch (BlockBehaviors.dry(behavior).key()) {
            case SLAB, STAIR, CARPET -> true;
            default -> false;
        };
    }

    private boolean isPassableActionVolume(BlockBehavior behavior) {
        return switch (BlockBehaviors.dry(behavior).key()) {
            case LADDER, VINE -> true;
            default -> false;
        };
    }
}
