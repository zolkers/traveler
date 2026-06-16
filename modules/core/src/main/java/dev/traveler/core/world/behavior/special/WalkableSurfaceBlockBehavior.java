package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.movement.MovementCapabilities;

abstract class WalkableSurfaceBlockBehavior implements BlockBehavior {
    @Override
    public final SupportSemantics supportSemantics(MovementCapabilities capabilities) {
        return SurfaceMovementRules.supportsWalking(capabilities)
                ? SupportSemantics.STANDABLE
                : SupportSemantics.NONE;
    }

    @Override
    public final BlockSemantics describe(SurfaceMovementContext context) {
        return SurfaceMovementRules.walkStepOrJumpSemantics(context);
    }
}
