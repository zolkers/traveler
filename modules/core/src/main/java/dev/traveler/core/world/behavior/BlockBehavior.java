package dev.traveler.core.world.behavior;

import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;

public interface BlockBehavior {
    BlockBehaviorKey key();

    boolean supportsStanding(MovementCapabilities capabilities);

    MovementDecision evaluateMovement(SurfaceMovementContext context);

    default boolean allowsRouteSmoothing(MovementCapabilities capabilities) {
        return true;
    }

    default boolean preservesRouteGeometry(MovementCapabilities capabilities) {
        return !allowsRouteSmoothing(capabilities);
    }
}
