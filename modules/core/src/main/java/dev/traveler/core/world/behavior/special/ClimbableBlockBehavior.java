package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.BlockBehavior;
import dev.traveler.core.world.behavior.context.HorizontalFacing;
import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.settings.TravelerSettings;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;
import java.util.Optional;
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

    public final Optional<ClimbSurfaceGeometry> climbSurface(
            HorizontalFacing face,
            MovementCapabilities capabilities) {
        HorizontalFacing safeFace = Objects.requireNonNull(face, "face");
        Objects.requireNonNull(capabilities, "capabilities");
        if (!supportsClimbingFrom(safeFace, capabilities)) {
            return Optional.empty();
        }
        return Optional.of(Objects.requireNonNull(climbSurfaceGeometry(safeFace), "climbSurfaceGeometry"));
    }

    protected ClimbSurfaceGeometry climbSurfaceGeometry(HorizontalFacing face) {
        return ClimbSurfaceGeometry.onFace(face, climbFaceInset());
    }

    protected double climbFaceInset() {
        return TravelerSettings.standard().get(TravelerSettings.CLIMB_FACE_INSET);
    }

    @Override
    public final boolean allowsRouteSmoothing(MovementCapabilities capabilities) {
        return !supportsClimbing(capabilities);
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
