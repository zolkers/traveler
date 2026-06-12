package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.BlockBehaviorKey;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
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
    public boolean supportsStanding(MovementCapabilities capabilities) {
        return SurfaceMovementRules.supportsWalking(capabilities);
    }

    @Override
    public MovementDecision evaluateMovement(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canWalk()) {
            return MovementDecision.blocked();
        }
        if (safeContext.floorDelta() <= 0.0) {
            return SurfaceMovementRules.walkStepOrJump(safeContext);
        }
        return raisedMovementDecision(safeContext);
    }

    private MovementDecision raisedMovementDecision(SurfaceMovementContext context) {
        if (context.direction().matches(facing.opposite())) {
            return SurfaceMovementRules.walkStepOrJump(context);
        }
        if (context.direction().isPerpendicularTo(facing)) {
            return SurfaceMovementRules.walkOrJump(context);
        }
        return SurfaceMovementRules.walkStepOrJump(context);
    }
}
