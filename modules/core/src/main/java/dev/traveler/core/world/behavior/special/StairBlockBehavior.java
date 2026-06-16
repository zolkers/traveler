package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.api.BlockSemantics;
import dev.traveler.core.world.behavior.api.SupportSemantics;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;

public final class StairBlockBehavior implements BlockBehavior {
    private final HorizontalFacing facing;

    public StairBlockBehavior(HorizontalFacing facing) {
        this.facing = Objects.requireNonNull(facing, "facing");
    }

    @Override
    public BlockBehaviorKey key() {
        return BlockBehaviorKey.STAIR;
    }

    public HorizontalFacing facing() {
        return facing;
    }

    @Override
    public SupportSemantics supportSemantics(MovementCapabilities capabilities) {
        return SurfaceMovementRules.supportsWalking(capabilities)
                ? SupportSemantics.STANDABLE
                : SupportSemantics.NONE;
    }

    @Override
    public BlockSemantics describe(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canWalk()) {
            return SurfaceMovementRules.blockedWalkableSemantics();
        }
        if (safeContext.floorDelta() <= 0.0) {
            return SurfaceMovementRules.walkStepOrJumpSemantics(safeContext);
        }
        return raisedMovementSemantics(safeContext);
    }

    private BlockSemantics raisedMovementSemantics(SurfaceMovementContext context) {
        if (context.direction().matches(facing.opposite())) {
            return SurfaceMovementRules.walkStepOrJumpSemantics(context);
        }
        if (context.direction().isPerpendicularTo(facing)) {
            return SurfaceMovementRules.walkOrJumpSemantics(context);
        }
        return SurfaceMovementRules.walkStepOrJumpSemantics(context);
    }
}
