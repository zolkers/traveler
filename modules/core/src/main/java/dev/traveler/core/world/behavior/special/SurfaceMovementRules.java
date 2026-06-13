package dev.traveler.core.world.behavior.special;

import dev.traveler.core.world.behavior.context.SurfaceMovementContext;
import dev.traveler.core.world.behavior.decision.MovementDecision;
import dev.traveler.core.world.movement.MovementCapabilities;
import java.util.Objects;

final class SurfaceMovementRules {
    private SurfaceMovementRules() {}

    static boolean supportsWalking(MovementCapabilities capabilities) {
        return Objects.requireNonNull(capabilities, "capabilities").canWalk();
    }

    static MovementDecision walkStepOrJump(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canWalk()) {
            return MovementDecision.blocked();
        }
        return upwardMovementDecision(safeContext, true);
    }

    static MovementDecision walkOrJump(SurfaceMovementContext context) {
        SurfaceMovementContext safeContext = Objects.requireNonNull(context, "context");
        if (!safeContext.capabilities().canWalk()) {
            return MovementDecision.blocked();
        }
        return upwardMovementDecision(safeContext, false);
    }

    private static MovementDecision upwardMovementDecision(
            SurfaceMovementContext context, boolean allowStepUp) {
        double delta = context.floorDelta();
        if (delta <= 0.0) {
            return safeFallDecision(context.capabilities(), delta);
        }
        if (allowStepUp && delta <= context.capabilities().maxStepUp()) {
            return MovementDecision.stepUp();
        }
        if (delta <= context.capabilities().maxJumpHeight()) {
            return MovementDecision.jump();
        }
        return MovementDecision.blocked();
    }

    private static MovementDecision safeFallDecision(MovementCapabilities capabilities, double delta) {
        if (-delta > capabilities.maxSafeFallDistance()) {
            return MovementDecision.blocked();
        }
        if (delta < 0.0) {
            return MovementDecision.drop();
        }
        return MovementDecision.walk();
    }
}
