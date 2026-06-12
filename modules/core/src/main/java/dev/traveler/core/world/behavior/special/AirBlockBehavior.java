package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;

public final class AirBlockBehavior implements BlockBehavior {
    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.AIR;
    }

    @Override
    public boolean supportsStanding(MovementCapabilities capabilities) {
        return false;
    }

    @Override
    public MovementDecision evaluateMovement(SurfaceMovementContext context) {
        return MovementDecision.blocked();
    }
}
