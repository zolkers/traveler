package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;

abstract class ClimbableBlockBehavior implements BlockBehavior {
    @Override
    public final boolean supportsStanding(MovementCapabilities capabilities) {
        return false;
    }

    @Override
    public final MovementDecision evaluateMovement(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canWalk()) {
            return MovementDecision.blocked();
        }
        return MovementDecision.climb();
    }
}
