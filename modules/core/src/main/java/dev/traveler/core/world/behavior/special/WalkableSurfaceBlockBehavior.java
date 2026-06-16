package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;

abstract class WalkableSurfaceBlockBehavior implements BlockBehavior {
    @Override
    public final BlockSemantics describe(SurfaceMovementContext context) {
        return SurfaceMovementRules.walkStepOrJumpSemantics(context);
    }
}
