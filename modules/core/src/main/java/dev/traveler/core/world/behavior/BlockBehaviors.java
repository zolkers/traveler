package dev.traveler.core.world.behavior;

import dev.traveler.core.world.behavior.special.ClimbableBlockBehavior;
import dev.traveler.core.world.behavior.special.WaterloggedBlockBehavior;
import java.util.Objects;
import java.util.Optional;

public final class BlockBehaviors {
    private BlockBehaviors() {}

    public static BlockBehavior dry(BlockBehavior behavior) {
        BlockBehavior safeBehavior = Objects.requireNonNull(behavior, "behavior");
        if (safeBehavior instanceof WaterloggedBlockBehavior waterlogged) {
            return dry(waterlogged.delegate());
        }
        return safeBehavior;
    }

    public static Optional<ClimbableBlockBehavior> climbable(BlockBehavior behavior) {
        BlockBehavior safeBehavior = dry(behavior);
        if (safeBehavior instanceof ClimbableBlockBehavior climbable) {
            return Optional.of(climbable);
        }
        return Optional.empty();
    }
}
