package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;

public final class FluidBlockBehavior implements BlockBehavior {
    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.FLUID;
    }

    @Override
    public boolean supportsStanding(MovementCapabilities capabilities) {
        return false;
    }

    @Override
    public MovementDecision evaluateMovement(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (safeContext.capabilities().canSwim()) {
            return MovementDecision.swim();
        }
        return MovementDecision.blocked();
    }
}
