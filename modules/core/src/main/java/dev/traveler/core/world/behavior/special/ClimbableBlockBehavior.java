package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;
import java.util.Set;

public abstract class ClimbableBlockBehavior implements BlockBehavior {
    @Override
    public final boolean supportsStanding(MovementCapabilities capabilities) {
        return false;
    }

    public final boolean supportsClimbing(MovementCapabilities capabilities) {
        return SurfaceMovementRules.supportsWalking(capabilities);
    }

    public abstract Set<HorizontalFacing> climbableFaces();

    public final boolean supportsClimbingFrom(HorizontalFacing face, MovementCapabilities capabilities) {
        return supportsClimbing(capabilities) && climbableFaces().contains(Objects.requireNonNull(face, "face"));
    }

    @Override
    public final boolean preservesRouteGeometry(MovementCapabilities capabilities) {
        return supportsClimbing(capabilities);
    }

    @Override
    public final MovementDecision evaluateMovement(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!supportsClimbing(safeContext.capabilities())) {
            return MovementDecision.blocked();
        }
        return MovementDecision.climb();
    }
}
