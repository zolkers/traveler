package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;

abstract class WalkableSurfaceBlockBehavior implements BlockBehavior {
    @Override
    public final boolean supportsStanding(MovementCapabilities capabilities) {
        return SurfaceMovementRules.supportsWalking(capabilities);
    }

    @Override
    public final MovementDecision evaluateMovement(SurfaceMovementContext context) {
        return SurfaceMovementRules.walkStepOrJump(context);
    }
}
